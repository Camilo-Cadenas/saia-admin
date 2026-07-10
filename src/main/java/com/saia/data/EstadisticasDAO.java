package com.saia.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saia.db.ConnectionPool;

/**
 * DAO especializado para el panel de estadísticas de ingresos/salidas.
 */
public class EstadisticasDAO {

    public int totalIngresos(LocalDate desde, LocalDate hasta) {
        return queryInt("SELECT COUNT(*) FROM historial_ingreso WHERE estado_movimiento=1 AND DATE(fecha_hora_ingreso) BETWEEN ? AND ?", desde, hasta);
    }

    public int promedioDiario(LocalDate desde, LocalDate hasta) {
        int total = totalIngresos(desde, hasta);
        long dias = desde.until(hasta, java.time.temporal.ChronoUnit.DAYS) + 1;
        return dias > 0 ? (int)(total / dias) : 0;
    }

    public String horaPico(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT HOUR(fecha_hora_ingreso) AS hora, COUNT(*) AS cnt FROM historial_ingreso WHERE estado_movimiento=1 AND DATE(fecha_hora_ingreso) BETWEEN ? AND ? GROUP BY hora ORDER BY cnt DESC LIMIT 1";
        try (Connection cn = ConnectionPool.getInstance().getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(desde)); ps.setDate(2, java.sql.Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return String.format("%02d:00", rs.getInt("hora")); }
        } catch (SQLException e) { System.err.println("[EstadisticasDAO] " + e.getMessage()); }
        return "—";
    }

    public static class PuntoDia {
        public final String fecha, horaPromedio;
        public final int total;
        public PuntoDia(String f, int t, String h) { fecha=f; total=t; horaPromedio=h; }
    }

    public List<PuntoDia> ingresosPorDia(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT DATE(fecha_hora_ingreso) AS dia, COUNT(*) AS total, TIME_FORMAT(SEC_TO_TIME(AVG(TIME_TO_SEC(TIME(fecha_hora_ingreso)))),'%H:%i') AS hora_prom FROM historial_ingreso WHERE estado_movimiento=1 AND DATE(fecha_hora_ingreso) BETWEEN ? AND ? GROUP BY dia ORDER BY dia";
        List<PuntoDia> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(desde)); ps.setDate(2, java.sql.Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date d = rs.getDate("dia");
                    String f = d != null ? d.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")) : "";
                    lista.add(new PuntoDia(f, rs.getInt("total"), rs.getString("hora_prom")));
                }
            }
        } catch (SQLException e) { System.err.println("[EstadisticasDAO] " + e.getMessage()); }
        return lista;
    }

    public static class ColorRGB { public final int r,g,b; public ColorRGB(int r,int g,int b){this.r=r;this.g=g;this.b=b;} }
    private static ColorRGB rgb(int r,int g,int b){return new ColorRGB(r,g,b);}

    public static class FranjaHoraria {
        public final String label; public final int total; public final ColorRGB color;
        public FranjaHoraria(String l,int t,ColorRGB c){label=l;total=t;color=c;}
    }

    public List<FranjaHoraria> distribucionFranja(LocalDate desde, LocalDate hasta) {
        int[][] fr={{6,9},{9,12},{12,15},{15,18},{18,21}};
        ColorRGB[] col={rgb(0x2E,0x7D,0x32),rgb(0x15,0x65,0xC0),rgb(0xFF,0x8F,0x00),rgb(0x7B,0x1F,0xA2),rgb(0xC6,0x28,0x28)};
        String sql="SELECT COUNT(*) FROM historial_ingreso WHERE estado_movimiento=1 AND DATE(fecha_hora_ingreso) BETWEEN ? AND ? AND HOUR(fecha_hora_ingreso)>=? AND HOUR(fecha_hora_ingreso)<?";
        List<FranjaHoraria> lista=new ArrayList<>();
        try (Connection cn=ConnectionPool.getInstance().getConnection()) {
            for (int i=0;i<fr.length;i++) {
                try (PreparedStatement ps=cn.prepareStatement(sql)) {
                    ps.setDate(1,java.sql.Date.valueOf(desde)); ps.setDate(2,java.sql.Date.valueOf(hasta)); ps.setInt(3,fr[i][0]); ps.setInt(4,fr[i][1]);
                    try (ResultSet rs=ps.executeQuery()) { int cnt=rs.next()?rs.getInt(1):0; lista.add(new FranjaHoraria(String.format("%02d:00 - %02d:00",fr[i][0],fr[i][1]),cnt,col[i])); }
                }
            }
        } catch (SQLException e) { System.err.println("[EstadisticasDAO] "+e.getMessage()); }
        return lista;
    }

    public static class InfoPeriodo {
        public final int diasTotales, maxIngresos, minIngresos;
        public final String diaMasIngresos, diaMenosIngresos;
        public InfoPeriodo(int dt,String dmax,int max,String dmin,int min){diasTotales=dt;diaMasIngresos=dmax;maxIngresos=max;diaMenosIngresos=dmin;minIngresos=min;}
    }

    public InfoPeriodo infoPeriodo(LocalDate desde, LocalDate hasta) {
        String fmtMes="dd 'de' MMMM";
        java.util.Locale loc=java.util.Locale.forLanguageTag("es-CO");
        int dt=(int)(desde.until(hasta,java.time.temporal.ChronoUnit.DAYS)+1);
        String dmax="—",dmin="—"; int max=0,min=0;
        try (Connection cn=ConnectionPool.getInstance().getConnection()) {
            String q1="SELECT DATE(fecha_hora_ingreso) AS dia,COUNT(*) AS cnt FROM historial_ingreso WHERE estado_movimiento=1 AND DATE(fecha_hora_ingreso) BETWEEN ? AND ? GROUP BY dia ORDER BY cnt DESC LIMIT 1";
            try (PreparedStatement ps=cn.prepareStatement(q1)){ps.setDate(1,java.sql.Date.valueOf(desde));ps.setDate(2,java.sql.Date.valueOf(hasta));try(ResultSet rs=ps.executeQuery()){if(rs.next()){java.sql.Date d=rs.getDate("dia");if(d!=null)dmax=d.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern(fmtMes,loc));max=rs.getInt("cnt");}}}
            String q2="SELECT DATE(fecha_hora_ingreso) AS dia,COUNT(*) AS cnt FROM historial_ingreso WHERE estado_movimiento=1 AND DATE(fecha_hora_ingreso) BETWEEN ? AND ? GROUP BY dia ORDER BY cnt ASC LIMIT 1";
            try (PreparedStatement ps=cn.prepareStatement(q2)){ps.setDate(1,java.sql.Date.valueOf(desde));ps.setDate(2,java.sql.Date.valueOf(hasta));try(ResultSet rs=ps.executeQuery()){if(rs.next()){java.sql.Date d=rs.getDate("dia");if(d!=null)dmin=d.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern(fmtMes,loc));min=rs.getInt("cnt");}}}
        } catch (SQLException e) { System.err.println("[EstadisticasDAO] "+e.getMessage()); }
        return new InfoPeriodo(dt,dmax,max,dmin,min);
    }

    private int queryInt(String sql, LocalDate d1, LocalDate d2) {
        try (Connection cn=ConnectionPool.getInstance().getConnection(); PreparedStatement ps=cn.prepareStatement(sql)) {
            ps.setDate(1,java.sql.Date.valueOf(d1)); ps.setDate(2,java.sql.Date.valueOf(d2));
            try (ResultSet rs=ps.executeQuery()){if(rs.next())return rs.getInt(1);}
        } catch (SQLException e) { System.err.println("[EstadisticasDAO] "+e.getMessage()); }
        return 0;
    }
}
