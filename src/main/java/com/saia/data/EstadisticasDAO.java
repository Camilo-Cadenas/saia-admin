package com.saia.data;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.saia.db.ConnectionPool;

/**
 * DAO para estadísticas de ingresos/salidas.
 *
 * Estructura REAL de la BD:
 *   historial_ingreso: cada fila = una visita completa.
 *     - fecha_hora_ingreso : cuando entró
 *     - fecha_hora_salida  : cuando salió (puede ser NULL)
 *     - estado_movimiento  : todos los registros actuales = 0
 *     → Se cuentan TODAS las filas como "ingresos".
 *     → Las "salidas" son filas que tienen fecha_hora_salida IS NOT NULL.
 */
public class EstadisticasDAO {

    private static final Locale LOCALE = Locale.forLanguageTag("es-CO");

    // ── Filtro base — NO filtrar por estado_movimiento ────────────────────────
    // Cada fila en historial_ingreso representa una visita (ingreso).
    private static final String RANGO_INGRESO =
        "DATE(fecha_hora_ingreso) BETWEEN ? AND ?";

    // ── Totales ───────────────────────────────────────────────────────────────

    /** Total de ingresos = total de filas en el rango. */
    public int totalIngresos(LocalDate desde, LocalDate hasta) {
        return queryInt(
            "SELECT COUNT(*) FROM historial_ingreso WHERE " + RANGO_INGRESO,
            desde, hasta);
    }

    /** Total de salidas = filas con fecha_hora_salida registrada en el rango. */
    public int totalSalidas(LocalDate desde, LocalDate hasta) {
        return queryInt(
            "SELECT COUNT(*) FROM historial_ingreso " +
            "WHERE fecha_hora_salida IS NOT NULL AND " + RANGO_INGRESO,
            desde, hasta);
    }

    public int promedioDiario(LocalDate desde, LocalDate hasta) {
        int total = totalIngresos(desde, hasta);
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        return dias > 0 ? (int) Math.round((double) total / dias) : 0;
    }

    /** Hora pico: la hora del día con más ingresos. */
    public String horaPico(LocalDate desde, LocalDate hasta) {
        String sql =
            "SELECT HOUR(fecha_hora_ingreso) AS hora, COUNT(*) AS cnt " +
            "FROM historial_ingreso " +
            "WHERE " + RANGO_INGRESO +
            " GROUP BY hora ORDER BY cnt DESC LIMIT 1";
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return String.format("%02d:00", rs.getInt("hora"));
            }
        } catch (SQLException e) { System.err.println("[EstadisticasDAO] horaPico: " + e.getMessage()); }
        return "—";
    }

    /**
     * Variación porcentual de ingresos vs. período anterior del mismo tamaño.
     */
    public String variacionVsAnterior(LocalDate desde, LocalDate hasta) {
        long dias      = ChronoUnit.DAYS.between(desde, hasta) + 1;
        LocalDate antHasta = desde.minusDays(1);
        LocalDate antDesde = antHasta.minusDays(dias - 1);
        int actual   = totalIngresos(desde, hasta);
        int anterior = totalIngresos(antDesde, antHasta);
        if (anterior == 0) return actual > 0 ? "+100%" : "N/A";
        int pct = (int)(((double)(actual - anterior) / anterior) * 100);
        return (pct >= 0 ? "+" : "") + pct + "%";
    }

    /** % de ingresos que tienen salida registrada (consistencia). */
    public String porcentajeValidos(LocalDate desde, LocalDate hasta) {
        int total = totalIngresos(desde, hasta);
        if (total == 0) return "—";
        int conSalida = totalSalidas(desde, hasta);
        int pct = (int)((double) conSalida / total * 100);
        return pct + "%";
    }

    // ── Ingresos por día ──────────────────────────────────────────────────────

    public static class PuntoDia {
        public final String fecha;
        public final int    total;
        public final String horaPromedio;
        public final double horaDecimal;
        public PuntoDia(String f, int t, String h, double hd) {
            fecha = f; total = t; horaPromedio = h; horaDecimal = hd;
        }
    }

    public List<PuntoDia> ingresosPorDia(LocalDate desde, LocalDate hasta) {
        String sql =
            "SELECT DATE(fecha_hora_ingreso) AS dia, " +
            "       COUNT(*) AS total, " +
            "       TIME_FORMAT(SEC_TO_TIME(AVG(TIME_TO_SEC(TIME(fecha_hora_ingreso)))),'%H:%i') AS hora_prom, " +
            "       AVG(HOUR(fecha_hora_ingreso) + MINUTE(fecha_hora_ingreso)/60.0) AS hora_dec " +
            "FROM historial_ingreso " +
            "WHERE " + RANGO_INGRESO +
            " GROUP BY dia ORDER BY dia";
        List<PuntoDia> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date d = rs.getDate("dia");
                    String f = d != null
                        ? d.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM"))
                        : "";
                    String hp = rs.getString("hora_prom");
                    double hd = rs.getDouble("hora_dec");
                    lista.add(new PuntoDia(f, rs.getInt("total"),
                            hp != null ? hp : "—", hd));
                }
            }
        } catch (SQLException e) { System.err.println("[EstadisticasDAO] ingresosPorDia: " + e.getMessage()); }
        return lista;
    }

    // ── Distribución por franja horaria ───────────────────────────────────────

    public static class ColorRGB {
        public final int r, g, b;
        public ColorRGB(int r, int g, int b) { this.r = r; this.g = g; this.b = b; }
    }

    public static class FranjaHoraria {
        public final String   label;
        public final String   rango;
        public final int      total;
        public final int      porcentaje;
        public final ColorRGB color;
        public FranjaHoraria(String l, String r, int t, int p, ColorRGB c) {
            label = l; rango = r; total = t; porcentaje = p; color = c;
        }
    }

    public List<FranjaHoraria> distribucionFranja(LocalDate desde, LocalDate hasta) {
        int[][]    rangos  = {{5,9},{9,12},{12,15},{15,18},{18,24}};
        String[]   labels  = {"Mañana","Media mañana","Mediodía","Tarde","Noche"};
        ColorRGB[] colores = {
            new ColorRGB(0x2E,0x7D,0x32),
            new ColorRGB(0x15,0x65,0xC0),
            new ColorRGB(0xFF,0x8F,0x00),
            new ColorRGB(0x7B,0x1F,0xA2),
            new ColorRGB(0xC6,0x28,0x28)
        };

        String sql =
            "SELECT COUNT(*) FROM historial_ingreso " +
            "WHERE " + RANGO_INGRESO +
            "  AND HOUR(fecha_hora_ingreso) >= ? " +
            "  AND HOUR(fecha_hora_ingreso) < ?";

        int total = totalIngresos(desde, hasta);
        List<FranjaHoraria> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection()) {
            for (int i = 0; i < rangos.length; i++) {
                try (PreparedStatement ps = cn.prepareStatement(sql)) {
                    ps.setDate(1, Date.valueOf(desde));
                    ps.setDate(2, Date.valueOf(hasta));
                    ps.setInt (3, rangos[i][0]);
                    ps.setInt (4, rangos[i][1]);
                    try (ResultSet rs = ps.executeQuery()) {
                        int cnt = rs.next() ? rs.getInt(1) : 0;
                        int pct = total > 0 ? (int)((double) cnt / total * 100) : 0;
                        String r = String.format("%02d:00 – %02d:00", rangos[i][0], rangos[i][1]);
                        lista.add(new FranjaHoraria(labels[i], r, cnt, pct, colores[i]));
                    }
                }
            }
        } catch (SQLException e) { System.err.println("[EstadisticasDAO] franja: " + e.getMessage()); }
        return lista;
    }

    // ── Información del período ───────────────────────────────────────────────

    public static class InfoPeriodo {
        public final int    diasTotales;
        public final int    maxIngresos;
        public final int    minIngresos;
        public final String diaMasIngresos;
        public final String diaMenosIngresos;
        public InfoPeriodo(int dt, String dmax, int max, String dmin, int min) {
            diasTotales      = dt;
            diaMasIngresos   = dmax;
            maxIngresos      = max;
            diaMenosIngresos = dmin;
            minIngresos      = min;
        }
    }

    public InfoPeriodo infoPeriodo(LocalDate desde, LocalDate hasta) {
        int dt = (int)(ChronoUnit.DAYS.between(desde, hasta) + 1);
        String dmax = "—", dmin = "—"; int max = 0, min = 0;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd 'de' MMMM", LOCALE);

        String qBase =
            "SELECT DATE(fecha_hora_ingreso) AS dia, COUNT(*) AS cnt " +
            "FROM historial_ingreso " +
            "WHERE " + RANGO_INGRESO +
            " GROUP BY dia ";

        try (Connection cn = ConnectionPool.getInstance().getConnection()) {
            // Día con más ingresos
            try (PreparedStatement ps = cn.prepareStatement(qBase + "ORDER BY cnt DESC LIMIT 1")) {
                ps.setDate(1, Date.valueOf(desde));
                ps.setDate(2, Date.valueOf(hasta));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        java.sql.Date d = rs.getDate("dia");
                        if (d != null) dmax = d.toLocalDate().format(fmt);
                        max = rs.getInt("cnt");
                    }
                }
            }
            // Día con menos ingresos
            try (PreparedStatement ps = cn.prepareStatement(qBase + "ORDER BY cnt ASC LIMIT 1")) {
                ps.setDate(1, Date.valueOf(desde));
                ps.setDate(2, Date.valueOf(hasta));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        java.sql.Date d = rs.getDate("dia");
                        if (d != null) dmin = d.toLocalDate().format(fmt);
                        min = rs.getInt("cnt");
                    }
                }
            }
        } catch (SQLException e) { System.err.println("[EstadisticasDAO] infoPeriodo: " + e.getMessage()); }
        return new InfoPeriodo(dt, dmax, max, dmin, min);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private int queryInt(String sql, LocalDate d1, LocalDate d2) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(d1));
            ps.setDate(2, Date.valueOf(d2));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { System.err.println("[EstadisticasDAO] queryInt: " + e.getMessage()); }
        return 0;
    }
}
