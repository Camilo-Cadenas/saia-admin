package com.saia.data;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.saia.db.ConnectionPool;

/**
 * DAO para consultas del módulo de Reportes.
 * Cubre reporte_rechazo e historial_ingreso con filtros por período.
 */
public class ReporteConsultaDAO {

    // ── Modelo de fila de reporte ─────────────────────────────────────────────

    public static class FilaReporte {
        public final int           idReporte;
        public final String        tipDocGuarda;
        public final int           numDocGuarda;
        public final String        nombreGuarda;
        public final String        tipDocAprendiz;
        public final int           numDocAprendiz;
        public final String        nombreAprendiz;
        public final String        motivo;
        public final String        descripcion;
        public final LocalDateTime fechaReporte;

        public FilaReporte(int id, String tipDocG, int numDocG, String nomG,
                           String tipDocA, int numDocA, String nomA,
                           String motivo, String desc, LocalDateTime fecha) {
            this.idReporte      = id;
            this.tipDocGuarda   = tipDocG   != null ? tipDocG  : "";
            this.numDocGuarda   = numDocG;
            this.nombreGuarda   = nomG      != null ? nomG     : "";
            this.tipDocAprendiz = tipDocA   != null ? tipDocA  : "";
            this.numDocAprendiz = numDocA;
            this.nombreAprendiz = nomA      != null ? nomA     : "";
            this.motivo         = motivo    != null ? motivo   : "";
            this.descripcion    = desc      != null ? desc     : "";
            this.fechaReporte   = fecha;
        }
    }

    // ── Modelo de estadística de ingresos/salidas ─────────────────────────────

    public static class EstadisticaPunto {
        public final String etiqueta;  // fecha o período
        public final int    ingresos;
        public final int    salidas;
        public EstadisticaPunto(String etiqueta, int ingresos, int salidas) {
            this.etiqueta = etiqueta;
            this.ingresos = ingresos;
            this.salidas  = salidas;
        }
    }

    public static class ResumenPeriodo {
        public final int totalIngresos;
        public final int totalSalidas;
        public final int promedioDiarioIngresos;
        public final int promedioDiarioSalidas;
        public final int diferenciaNeta;
        public ResumenPeriodo(int ti, int ts, int pdi, int pds) {
            this.totalIngresos          = ti;
            this.totalSalidas           = ts;
            this.promedioDiarioIngresos = pdi;
            this.promedioDiarioSalidas  = pds;
            this.diferenciaNeta         = ti - ts;
        }
    }

    // ── Contadores para tarjetas de estadísticas ──────────────────────────────

    public int countTotalReportes(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT COUNT(*) FROM reporte_rechazo " +
                     "WHERE fecha_reporte BETWEEN ? AND ?";
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { System.err.println("[ReporteConsultaDAO] " + e.getMessage()); }
        return 0;
    }

    public int countReportesHoy() {
        String sql = "SELECT COUNT(*) FROM reporte_rechazo WHERE DATE(fecha_reporte) = CURDATE()";
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        catch (SQLException e) { System.err.println("[ReporteConsultaDAO] " + e.getMessage()); }
        return 0;
    }

    public int countGuardasActivos() {
        String sql = "SELECT COUNT(*) FROM personal_seguridad ps " +
                     "INNER JOIN cuenta c ON ps.num_doc = c.num_doc WHERE c.estado = TRUE";
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        catch (SQLException e) { System.err.println("[ReporteConsultaDAO] " + e.getMessage()); }
        return 0;
    }

    public int countAprendicesReportados(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT COUNT(DISTINCT num_doc) FROM reporte_rechazo " +
                     "WHERE fecha_reporte BETWEEN ? AND ?";
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { System.err.println("[ReporteConsultaDAO] " + e.getMessage()); }
        return 0;
    }

    // ── Consulta de reportes de rechazo con filtros ───────────────────────────

    private static final String SQL_REPORTES =
        "SELECT rr.id_reporte, " +
        "       pg.tip_doc  AS tip_doc_g, ps.num_doc AS num_doc_g, " +
        "       CONCAT(pg.nombres,' ',pg.p_ape) AS nombre_guarda, " +
        "       pa.tip_doc  AS tip_doc_a, rr.num_doc AS num_doc_a, " +
        "       CONCAT(pa.nombres,' ',pa.p_ape) AS nombre_aprendiz, " +
        "       rr.motivo, rr.descripcion, rr.fecha_reporte " +
        "FROM reporte_rechazo rr " +
        "INNER JOIN personal_seguridad ps ON rr.id_guarda = ps.id_guarda " +
        "INNER JOIN persona             pg ON ps.num_doc   = pg.num_doc " +
        "INNER JOIN persona             pa ON rr.num_doc   = pa.num_doc " +
        "WHERE rr.fecha_reporte BETWEEN ? AND ? ";

    /**
     * Lista reportes de rechazo en un rango de fechas.
     * @param desde inicio del rango (inclusive)
     * @param hasta fin del rango (inclusive, se agrega 1 día)
     * @param idGuarda  -1 = todos
     * @param numDocAprendiz -1 = todos
     */
    public List<FilaReporte> findReportes(LocalDate desde, LocalDate hasta,
                                          int idGuarda, int numDocAprendiz) {
        List<FilaReporte> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SQL_REPORTES);
        if (idGuarda        > 0) sql.append(" AND rr.id_guarda = ").append(idGuarda);
        if (numDocAprendiz  > 0) sql.append(" AND rr.num_doc   = ").append(numDocAprendiz);
        sql.append(" ORDER BY rr.fecha_reporte DESC");

        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("fecha_reporte");
                    lista.add(new FilaReporte(
                        rs.getInt("id_reporte"),
                        rs.getString("tip_doc_g"),  rs.getInt("num_doc_g"),
                        rs.getString("nombre_guarda"),
                        rs.getString("tip_doc_a"),  rs.getInt("num_doc_a"),
                        rs.getString("nombre_aprendiz"),
                        rs.getString("motivo"),
                        rs.getString("descripcion"),
                        ts != null ? ts.toLocalDateTime() : null
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ReporteConsultaDAO] Error reportes: " + e.getMessage());
        }
        return lista;
    }

    // ── Estadísticas de ingresos/salidas ──────────────────────────────────────

    /**
     * Retorna estadísticas agrupadas según el tipo de vista.
     * estado_movimiento: 1 = Ingreso, 0 = Salida
     * Las salidas usan fecha_hora_salida; los ingresos usan fecha_hora_ingreso.
     */
    public List<EstadisticaPunto> getEstadisticasPorPeriodo(
            LocalDate desde, LocalDate hasta, String tipo) {

        String groupExpr, labelExpr;
        switch (tipo) {
            case "Mensual" -> {
                groupExpr = "DATE_FORMAT(fecha_hora_ingreso, '%Y-%m')";
                labelExpr = "DATE_FORMAT(fecha_hora_ingreso, '%m/%Y')";
            }
            case "Anual" -> {
                groupExpr = "YEAR(fecha_hora_ingreso)";
                labelExpr = "CAST(YEAR(fecha_hora_ingreso) AS CHAR)";
            }
            default -> {   // Diaria / Semanal
                groupExpr = "DATE(fecha_hora_ingreso)";
                labelExpr = "DATE_FORMAT(fecha_hora_ingreso, '%d/%m')";
            }
        }

        // Ingresos: estado_movimiento=1, agrupados por fecha_hora_ingreso
        // Salidas:  estado_movimiento=0, agrupados por fecha_hora_salida (misma expresión)
        String sql =
            "SELECT " + labelExpr + " AS etiqueta, " +
            "       SUM(CASE WHEN estado_movimiento = 1 THEN 1 ELSE 0 END) AS ingresos, " +
            "       SUM(CASE WHEN estado_movimiento = 0 THEN 1 ELSE 0 END) AS salidas " +
            "FROM historial_ingreso " +
            "WHERE DATE(fecha_hora_ingreso) BETWEEN ? AND ? " +
            "GROUP BY " + groupExpr + " ORDER BY " + groupExpr;

        List<EstadisticaPunto> pts = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    pts.add(new EstadisticaPunto(
                        rs.getString("etiqueta"),
                        rs.getInt("ingresos"),
                        rs.getInt("salidas")));
            }
        } catch (SQLException e) {
            System.err.println("[ReporteConsultaDAO] Error estadísticas por período: " + e.getMessage());
        }
        return pts;
    }

    /**
     * Retorna puntos de estadística agrupados por día para un rango.
     * estado_movimiento: 1 = Ingreso, 0 = Salida
     */
    public List<EstadisticaPunto> getEstadisticasPorDia(LocalDate desde, LocalDate hasta) {
        List<EstadisticaPunto> pts = new ArrayList<>();
        String sql =
            "SELECT DATE(fecha_hora_ingreso) AS dia, " +
            "       SUM(CASE WHEN estado_movimiento = 1 THEN 1 ELSE 0 END) AS ingresos, " +
            "       SUM(CASE WHEN estado_movimiento = 0 THEN 1 ELSE 0 END) AS salidas " +
            "FROM historial_ingreso " +
            "WHERE DATE(fecha_hora_ingreso) BETWEEN ? AND ? " +
            "GROUP BY dia ORDER BY dia";
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    pts.add(new EstadisticaPunto(
                        rs.getString("dia"),
                        rs.getInt("ingresos"),
                        rs.getInt("salidas")));
            }
        } catch (SQLException e) {
            System.err.println("[ReporteConsultaDAO] Error estadísticas: " + e.getMessage());
        }
        return pts;
    }

    /** Totales de ingresos/salidas para un rango de fechas. */
    public ResumenPeriodo getResumen(LocalDate desde, LocalDate hasta) {
        // Ingresos: estado=1 por fecha_hora_ingreso
        // Salidas:  estado=0 por fecha_hora_salida
        String sql =
            "SELECT " +
            "  SUM(CASE WHEN estado_movimiento = 1 THEN 1 ELSE 0 END) AS total_i, " +
            "  SUM(CASE WHEN estado_movimiento = 0 " +
            "           AND DATE(fecha_hora_salida) BETWEEN ? AND ? THEN 1 ELSE 0 END) AS total_s, " +
            "  COUNT(DISTINCT DATE(fecha_hora_ingreso)) AS dias " +
            "FROM historial_ingreso " +
            "WHERE DATE(fecha_hora_ingreso) BETWEEN ? AND ?";
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            ps.setDate(3, Date.valueOf(desde));
            ps.setDate(4, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int ti   = rs.getInt("total_i");
                    int ts2  = rs.getInt("total_s");
                    int dias = rs.getInt("dias");
                    if (dias < 1) dias = 1;
                    return new ResumenPeriodo(ti, ts2, ti / dias, ts2 / dias);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ReporteConsultaDAO] Error resumen: " + e.getMessage());
        }
        return new ResumenPeriodo(0, 0, 0, 0);
    }

    // ── Listas para combos de filtro ──────────────────────────────────────────

    public List<int[]> getGuardas() {
        // Retorna {id_guarda, num_doc, nombre}
        List<int[]> lista = new ArrayList<>();
        String sql = "SELECT ps.id_guarda, ps.num_doc, p.nombres, p.p_ape " +
                     "FROM personal_seguridad ps INNER JOIN persona p ON ps.num_doc=p.num_doc " +
                     "ORDER BY p.p_ape";
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                lista.add(new int[]{rs.getInt("id_guarda"), rs.getInt("num_doc")});
        } catch (SQLException e) { /* silencioso */ }
        return lista;
    }
}
