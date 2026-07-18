package com.saia.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saia.db.ConnectionPool;
import com.saia.model.RegistroAuditoria;

/**
 * DAO para {@code auditoria_actividad}.
 * Consultas con filtros opcionales: fecha, accion, entidad, texto libre.
 */
public class AuditoriaDAO {

    // Entidad ENTIDAD_PERSONAL para guardas (nombre real en BD)
    public static final String ENTIDAD_PERSONAL = "Guarda";

    // Constantes de acción usadas por los servicios
    public static final String ACCION_CREAR       = "CREAR";
    public static final String ACCION_ACTUALIZAR  = "ACTUALIZAR";
    public static final String ACCION_BLOQUEAR    = "BLOQUEAR";
    public static final String ACCION_DESBLOQUEAR = "HABILITAR";
    public static final String ENTIDAD_APRENDIZ   = "Aprendiz";

    // ── Inserción ────────────────────────────────────────────────────────────

    /**
     * Registra una actividad de auditoría.
     * @param tipoAccion  constante ACCION_*
     * @param entidad     nombre de la entidad afectada
     * @param numDoc      num_doc de la persona afectada (puede ser null)
     * @param descripcion texto descriptivo
     * @param realizadoPor num_doc del admin que ejecutó la acción (puede ser null)
     */
    public void registrar(String tipoAccion, String entidad,
                          int numDoc, String descripcion, Integer realizadoPor) {
        String sql =
            "INSERT INTO auditoria_actividad " +
            "(tipo_accion, entidad, num_doc, descripcion, realizado_por) " +
            "VALUES (?, ?, ?, ?, ?)";
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString (1, tipoAccion);
            ps.setString (2, entidad);
            ps.setInt    (3, numDoc);
            ps.setString (4, descripcion != null ? descripcion : "");
            if (realizadoPor != null) ps.setInt(5, realizadoPor);
            else ps.setNull(5, java.sql.Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AuditoriaDAO] registrar: " + e.getMessage());
        }
    }

    // ── Consulta principal ────────────────────────────────────────────────────

    /**
     * Lista todos los registros con filtros opcionales.
     * @param desde      null = sin límite inferior
     * @param hasta      null = sin límite superior
     * @param accion     null = todas las acciones
     * @param entidad    null = todas las entidades
     * @param textoBusca null = sin búsqueda libre
     * @param limite     0 = sin límite
     * @param offset     página * tamaño
     */
    public List<RegistroAuditoria> findAll(
            LocalDate desde, LocalDate hasta,
            String accion, String entidad,
            String textoBusca,
            int limite, int offset) {

        StringBuilder sql = new StringBuilder(
            "SELECT aa.id_actividad, aa.tipo_accion, aa.entidad, " +
            "       aa.num_doc, aa.descripcion, aa.realizado_por, aa.fecha_hora, " +
            "       IFNULL(CONCAT(p.nombres,' ',p.p_ape), CAST(aa.realizado_por AS CHAR)) AS usuario " +
            "FROM auditoria_actividad aa " +
            "LEFT JOIN persona p ON aa.realizado_por = p.num_doc " +
            "WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (desde != null) {
            sql.append(" AND DATE(aa.fecha_hora) >= ? ");
            params.add(java.sql.Date.valueOf(desde));
        }
        if (hasta != null) {
            sql.append(" AND DATE(aa.fecha_hora) <= ? ");
            params.add(java.sql.Date.valueOf(hasta));
        }
        if (accion != null && !accion.isBlank() && !"Todas".equals(accion)) {
            sql.append(" AND aa.tipo_accion = ? ");
            params.add(accion);
        }
        if (entidad != null && !entidad.isBlank() && !"Todas".equals(entidad)) {
            sql.append(" AND aa.entidad = ? ");
            params.add(entidad);
        }
        if (textoBusca != null && !textoBusca.isBlank()) {
            sql.append(" AND (aa.descripcion LIKE ? " +
                       "  OR aa.entidad LIKE ? " +
                       "  OR aa.tipo_accion LIKE ? " +
                       "  OR IFNULL(CONCAT(p.nombres,' ',p.p_ape),'') LIKE ?) ");
            String like = "%" + textoBusca.trim() + "%";
            params.add(like); params.add(like); params.add(like); params.add(like);
        }

        sql.append(" ORDER BY aa.fecha_hora DESC ");

        if (limite > 0) {
            sql.append(" LIMIT ? OFFSET ? ");
            params.add(limite);
            params.add(offset);
        }

        List<RegistroAuditoria> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++)
                ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[AuditoriaDAO] " + e.getMessage());
        }
        return lista;
    }

    /** Cuenta total de registros con los mismos filtros (para paginación). */
    public int count(LocalDate desde, LocalDate hasta,
                     String accion, String entidad, String textoBusca) {

        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM auditoria_actividad aa " +
            "LEFT JOIN persona p ON aa.realizado_por = p.num_doc " +
            "WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (desde != null) { sql.append(" AND DATE(aa.fecha_hora) >= ? "); params.add(java.sql.Date.valueOf(desde)); }
        if (hasta != null) { sql.append(" AND DATE(aa.fecha_hora) <= ? "); params.add(java.sql.Date.valueOf(hasta)); }
        if (accion != null && !accion.isBlank() && !"Todas".equals(accion)) { sql.append(" AND aa.tipo_accion = ? "); params.add(accion); }
        if (entidad != null && !entidad.isBlank() && !"Todas".equals(entidad)) { sql.append(" AND aa.entidad = ? "); params.add(entidad); }
        if (textoBusca != null && !textoBusca.isBlank()) {
            String like = "%" + textoBusca.trim() + "%";
            sql.append(" AND (aa.descripcion LIKE ? OR aa.entidad LIKE ? OR aa.tipo_accion LIKE ? " +
                       "  OR IFNULL(CONCAT(p.nombres,' ',p.p_ape),'') LIKE ?) ");
            params.add(like); params.add(like); params.add(like); params.add(like);
        }

        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { System.err.println("[AuditoriaDAO] count: " + e.getMessage()); }
        return 0;
    }

    /** Lista de acciones distintas para el ComboBox. */
    public List<String> getAccionesDistintas() {
        List<String> list = new ArrayList<>();
        list.add("Todas");
        String sql = "SELECT DISTINCT tipo_accion FROM auditoria_actividad ORDER BY tipo_accion";
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String v = rs.getString(1);
                if (v != null) list.add(v);
            }
        } catch (SQLException e) { /* silencioso */ }
        return list;
    }

    /** Lista de entidades distintas para el ComboBox. */
    public List<String> getEntidadesDistintas() {
        List<String> list = new ArrayList<>();
        list.add("Todas");
        String sql = "SELECT DISTINCT entidad FROM auditoria_actividad ORDER BY entidad";
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String v = rs.getString(1);
                if (v != null) list.add(v);
            }
        } catch (SQLException e) { /* silencioso */ }
        return list;
    }

    // ── Mapeo ─────────────────────────────────────────────────────────────────

    private RegistroAuditoria mapRow(ResultSet rs) throws SQLException {
        RegistroAuditoria r = new RegistroAuditoria();
        r.setIdActividad   (rs.getInt   ("id_actividad"));
        r.setAccion        (RegistroAuditoria.Accion.fromBD(rs.getString("tipo_accion")));
        r.setEntidad       (rs.getString("entidad"));
        r.setNumDocAfectado(rs.getInt   ("num_doc"));
        r.setDescripcion   (rs.getString("descripcion"));
        r.setNumDocUsuario (rs.getInt   ("realizado_por"));
        r.setUsuario       (rs.getString("usuario"));

        // Módulo derivado de entidad
        r.setModulo(derivarModulo(r.getEntidad()));

        Timestamp ts = rs.getTimestamp("fecha_hora");
        if (ts != null) r.setFechaHora(ts.toLocalDateTime());
        return r;
    }

    private String derivarModulo(String entidad) {
        if (entidad == null) return "—";
        return switch (entidad.toLowerCase()) {
            case "aprendiz", "aprendices" -> "Aprendices";
            case "guarda", "personal_seguridad", "personal de seguridad" -> "Personal Seguridad";
            case "administrador" -> "Administradores";
            case "cuenta" -> "Cuentas";
            case "historial_ingreso", "historial" -> "Historial Ingresos";
            case "reporte_rechazo", "reporte" -> "Reportes";
            default -> entidad;
        };
    }
}
