package com.saia.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saia.db.ConnectionPool;

/**
 * DAO especializado en consultas de estadísticas para el dashboard de inicio.
 * Todas las queries son de sólo lectura y usan una única conexión por método.
 */
public class DashboardDAO {

    // ── Personal de seguridad ─────────────────────────────────────────────────
    public int countPersonalTotal() {
        return queryInt("SELECT COUNT(*) FROM personal_seguridad");
    }

    public int countPersonalActivo() {
        return queryInt(
            "SELECT COUNT(*) FROM personal_seguridad ps " +
            "INNER JOIN cuenta c ON ps.num_doc = c.num_doc " +
            "WHERE c.estado = TRUE");
    }

    public int countPersonalInactivo() {
        return queryInt(
            "SELECT COUNT(*) FROM personal_seguridad ps " +
            "INNER JOIN cuenta c ON ps.num_doc = c.num_doc " +
            "WHERE c.estado = FALSE");
    }

    // ── Aprendices ────────────────────────────────────────────────────────────
    public int countAprendicesTotal() {
        return queryInt("SELECT COUNT(*) FROM aprendiz");
    }

    public int countAprendicesActivos() {
        return queryInt(
            "SELECT COUNT(*) FROM aprendiz a " +
            "INNER JOIN cuenta c ON a.num_doc = c.num_doc " +
            "WHERE c.estado = TRUE");
    }

    public int countAprendicesInactivos() {
        return queryInt(
            "SELECT COUNT(*) FROM aprendiz a " +
            "INNER JOIN cuenta c ON a.num_doc = c.num_doc " +
            "WHERE c.estado = FALSE");
    }

    // ── Reportes ──────────────────────────────────────────────────────────────
    public int countReportesTotal() {
        return queryInt("SELECT COUNT(*) FROM reporte_rechazo");
    }

    public int countReportesHoy() {
        return queryInt(
            "SELECT COUNT(*) FROM reporte_rechazo " +
            "WHERE DATE(fecha_reporte) = CURDATE()");
    }

    public int countReportesMes() {
        return queryInt(
            "SELECT COUNT(*) FROM reporte_rechazo " +
            "WHERE MONTH(fecha_reporte) = MONTH(CURDATE()) " +
            "  AND YEAR(fecha_reporte)  = YEAR(CURDATE())");
    }

    // ── Usuarios bloqueados ───────────────────────────────────────────────────
    public int countUsuariosBloqueadosPersonal() {
        return queryInt(
            "SELECT COUNT(*) FROM personal_seguridad ps " +
            "INNER JOIN cuenta c ON ps.num_doc = c.num_doc " +
            "WHERE c.estado = FALSE");
    }

    public int countUsuariosBloqueadosAprendices() {
        return queryInt(
            "SELECT COUNT(*) FROM aprendiz a " +
            "INNER JOIN cuenta c ON a.num_doc = c.num_doc " +
            "WHERE c.estado = FALSE");
    }

    // ── Ingresos / Salidas hoy ────────────────────────────────────────────────
    public int countIngresosHoy() {
        return queryInt(
            "SELECT COUNT(*) FROM historial_ingreso " +
            "WHERE DATE(fecha_hora_ingreso) = CURDATE() " +
            "  AND estado_movimiento = 1");
    }

    public int countSalidasHoy() {
        return queryInt(
            "SELECT COUNT(*) FROM historial_ingreso " +
            "WHERE DATE(fecha_hora_salida) = CURDATE() " +
            "  AND estado_movimiento = 0");
    }

    // ── Reportes por tipo (este mes) ──────────────────────────────────────────
    /**
     * Retorna un mapa {motivo → cantidad} de reportes del mes actual.
     * Si no hay datos devuelve mapa vacío.
     */
    public Map<String, Integer> countReportesPorTipoMes() {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        String sql =
            "SELECT motivo, COUNT(*) AS total " +
            "FROM reporte_rechazo " +
            "WHERE MONTH(fecha_reporte) = MONTH(CURDATE()) " +
            "  AND YEAR(fecha_reporte)  = YEAR(CURDATE()) " +
            "GROUP BY motivo " +
            "ORDER BY total DESC";

        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String motivo = rs.getString("motivo");
                if (motivo == null || motivo.isBlank()) motivo = "Sin clasificar";
                mapa.put(motivo, rs.getInt("total"));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error contando reportes por tipo", e);
        }
        return mapa;
    }

    // ── Actividad reciente ────────────────────────────────────────────────────
    /**
     * Retorna las últimas N actividades del sistema desde la tabla
     * {@code auditoria_actividad}. Incluye creaciones y actualizaciones
     * con fecha y hora exactas.
     * Si la tabla no tiene datos, cae al modo legado (cuentas + reportes).
     */
    public List<ActividadItem> getActividadReciente(int limite) {
        List<ActividadItem> lista = new ArrayList<>();

        // ── Fuente principal: tabla auditoria_actividad ───────────────────────
        String sqlAuditoria =
            "SELECT a.descripcion AS desc_act, " +
            "       a.fecha_hora  AS momento, " +
            "       CONCAT(a.tipo_accion, ' · ', a.entidad) AS tipo " +
            "FROM auditoria_actividad a " +
            "ORDER BY a.fecha_hora DESC " +
            "LIMIT ?";

        // ── Fuente secundaria: creaciones de cuenta (legacy) ──────────────────
        String sqlCuentas =
            "SELECT CONCAT('Cuenta creada: ', p.nombres, ' ', p.p_ape) AS desc_act, " +
            "       c.fecha_creacion AS momento, " +
            "       r.nom_rol AS tipo " +
            "FROM cuenta c " +
            "INNER JOIN persona p ON c.num_doc = p.num_doc " +
            "INNER JOIN rol r ON c.id_rol = r.id_rol " +
            "ORDER BY c.fecha_creacion DESC " +
            "LIMIT ?";

        // ── Fuente terciaria: reportes de rechazo ─────────────────────────────
        String sqlReportes =
            "SELECT CONCAT('Reporte por: ', p.nombres, ' ', p.p_ape) AS desc_act, " +
            "       rr.fecha_reporte AS momento, " +
            "       CONCAT('Reporte · ', rr.motivo) AS tipo " +
            "FROM reporte_rechazo rr " +
            "INNER JOIN personal_seguridad ps ON rr.id_guarda = ps.id_guarda " +
            "INNER JOIN persona p ON ps.num_doc = p.num_doc " +
            "ORDER BY rr.fecha_reporte DESC " +
            "LIMIT ?";

        try (Connection cn = ConnectionPool.getInstance().getConnection()) {

            // Intentar leer de auditoría primero
            try (PreparedStatement ps = cn.prepareStatement(sqlAuditoria)) {
                ps.setInt(1, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Timestamp ts = rs.getTimestamp("momento");
                        lista.add(new ActividadItem(
                                rs.getString("desc_act"),
                                rs.getString("tipo"),
                                ts != null ? ts.toLocalDateTime() : null));
                    }
                }
            } catch (SQLException e) {
                // La tabla puede no existir todavía — no bloquear
                System.err.println("[DashboardDAO] Tabla auditoria no disponible: " + e.getMessage());
            }

            // Completar con cuentas creadas (legacy)
            try (PreparedStatement ps = cn.prepareStatement(sqlCuentas)) {
                ps.setInt(1, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Timestamp ts = rs.getTimestamp("momento");
                        lista.add(new ActividadItem(
                                rs.getString("desc_act"),
                                rs.getString("tipo"),
                                ts != null ? ts.toLocalDateTime() : null));
                    }
                }
            }

            // Completar con reportes
            try (PreparedStatement ps = cn.prepareStatement(sqlReportes)) {
                ps.setInt(1, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Timestamp ts = rs.getTimestamp("momento");
                        lista.add(new ActividadItem(
                                rs.getString("desc_act"),
                                rs.getString("tipo"),
                                ts != null ? ts.toLocalDateTime() : null));
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("[DashboardDAO] Error cargando actividad reciente: " + e.getMessage());
        }

        // Deduplicar por descripción+momento y ordenar por fecha desc
        lista.sort((a, b) -> {
            if (a.getMomento() == null) return 1;
            if (b.getMomento() == null) return -1;
            return b.getMomento().compareTo(a.getMomento());
        });

        // Eliminar duplicados (misma desc + mismo minuto)
        List<ActividadItem> dedup = new ArrayList<>();
        for (ActividadItem item : lista) {
            boolean found = false;
            for (ActividadItem d : dedup) {
                if (d.getDescripcion().equals(item.getDescripcion())) { found = true; break; }
            }
            if (!found) dedup.add(item);
            if (dedup.size() >= limite) break;
        }
        return dedup;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int queryInt(String sql) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[DashboardDAO] Error en consulta: " + sql + " | " + e.getMessage());
        }
        return 0;
    }

    // ── Clase interna para actividad reciente ─────────────────────────────────

    public static class ActividadItem {
        private final String descripcion;
        private final String tipo;
        private final java.time.LocalDateTime momento;

        public ActividadItem(String descripcion, String tipo, java.time.LocalDateTime momento) {
            this.descripcion = descripcion;
            this.tipo        = tipo;
            this.momento     = momento;
        }

        public String getDescripcion() { return descripcion; }
        public String getTipo()        { return tipo; }
        public java.time.LocalDateTime getMomento() { return momento; }
    }
}
