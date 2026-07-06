package com.saia.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.saia.db.ConnectionPool;

/**
 * DAO para la tabla {@code auditoria_actividad}.
 * Registra y consulta todas las acciones realizadas en el sistema.
 */
public class AuditoriaDAO {

    // Tipos de acción estándar
    public static final String ACCION_CREAR      = "CREAR";
    public static final String ACCION_ACTUALIZAR = "ACTUALIZAR";
    public static final String ACCION_BLOQUEAR   = "BLOQUEAR";
    public static final String ACCION_DESBLOQUEAR= "DESBLOQUEAR";
    public static final String ACCION_ELIMINAR   = "ELIMINAR";

    // Entidades estándar
    public static final String ENTIDAD_PERSONAL  = "Personal de Seguridad";
    public static final String ENTIDAD_APRENDIZ  = "Aprendiz";
    public static final String ENTIDAD_CUENTA    = "Cuenta";
    public static final String ENTIDAD_REPORTE   = "Reporte";

    private static final String SQL_INSERT =
        "INSERT INTO auditoria_actividad " +
        "(tipo_accion, entidad, num_doc, descripcion, realizado_por, fecha_hora) " +
        "VALUES (?, ?, ?, ?, ?, NOW())";

    private static final String SQL_SELECT_RECIENTES =
        "SELECT id_actividad, tipo_accion, entidad, num_doc, " +
        "       descripcion, realizado_por, fecha_hora " +
        "FROM auditoria_actividad " +
        "ORDER BY fecha_hora DESC " +
        "LIMIT ?";

    // ── Escritura ─────────────────────────────────────────────────────────────

    /**
     * Registra una actividad de auditoría.
     *
     * @param tipoAccion   tipo de acción (CREAR, ACTUALIZAR, etc.)
     * @param entidad      nombre de la entidad afectada
     * @param numDoc       num_doc del registro afectado (puede ser null)
     * @param descripcion  texto descriptivo legible
     * @param realizadoPor num_doc del administrador que ejecutó la acción
     */
    public void registrar(String tipoAccion, String entidad, Integer numDoc,
                          String descripcion, Integer realizadoPor) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, tipoAccion);
            ps.setString(2, entidad);
            if (numDoc != null) ps.setInt(3, numDoc); else ps.setNull(3, Types.INTEGER);
            ps.setString(4, descripcion);
            if (realizadoPor != null) ps.setInt(5, realizadoPor); else ps.setNull(5, Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException e) {
            // La auditoría no debe bloquear la operación principal
            System.err.println("[AuditoriaDAO] Error registrando actividad: " + e.getMessage());
        }
    }

    // ── Consulta ──────────────────────────────────────────────────────────────

    /**
     * Retorna las últimas N actividades ordenadas por fecha descendente.
     */
    public List<ActividadAuditoria> getRecientes(int limite) {
        List<ActividadAuditoria> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_SELECT_RECIENTES)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("fecha_hora");
                    lista.add(new ActividadAuditoria(
                        rs.getInt("id_actividad"),
                        rs.getString("tipo_accion"),
                        rs.getString("entidad"),
                        rs.getObject("num_doc", Integer.class),
                        rs.getString("descripcion"),
                        rs.getObject("realizado_por", Integer.class),
                        ts != null ? ts.toLocalDateTime() : null
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[AuditoriaDAO] Error leyendo actividades: " + e.getMessage());
        }
        return lista;
    }

    // ── Modelo interno ────────────────────────────────────────────────────────

    public static class ActividadAuditoria {
        public final int           idActividad;
        public final String        tipoAccion;
        public final String        entidad;
        public final Integer       numDoc;
        public final String        descripcion;
        public final Integer       realizadoPor;
        public final LocalDateTime fechaHora;

        public ActividadAuditoria(int idActividad, String tipoAccion, String entidad,
                                  Integer numDoc, String descripcion,
                                  Integer realizadoPor, LocalDateTime fechaHora) {
            this.idActividad  = idActividad;
            this.tipoAccion   = tipoAccion;
            this.entidad      = entidad;
            this.numDoc       = numDoc;
            this.descripcion  = descripcion;
            this.realizadoPor = realizadoPor;
            this.fechaHora    = fechaHora;
        }
    }
}
