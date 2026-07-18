package com.saia.data;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saia.db.ConnectionPool;
import com.saia.model.FiltrosReporte;
import com.saia.model.TipoReporte;

/**
 * DAO para el módulo Generador de Reportes.
 * Devuelve los datos como List<Map<String,Object>> para poder
 * renderizarlos en tabla o exportarlos a cualquier formato.
 */
public class GeneradorReportesDAO {

    /**
     * Cabeceras de columna para cada tipo de reporte.
     */
    public static List<String> getColumnas(TipoReporte tipo) {
        return switch (tipo) {
            case HISTORIAL_INGRESOS -> List.of(
                "N° Doc", "Nombres", "Apellidos", "Fecha Ingreso",
                "Hora Ingreso", "Fecha Salida", "Hora Salida", "Movimiento", "Observación");
            case APRENDICES -> List.of(
                "Tipo Doc", "N° Doc", "Nombres", "Apellidos",
                "Correo", "N° Ficha", "Programa", "Centro", "Estado");
            case RESUMEN_GENERAL -> List.of(
                "Fecha", "Total Ingresos", "Total Salidas", "Neto");
            case ESCANEOS_QR -> List.of(
                "Fecha", "Hora", "N° Doc", "Nombres", "Movimiento");
            case PERSONAL_SEGURIDAD -> List.of(
                "Tipo Doc", "N° Doc", "Nombres", "Apellidos",
                "Correo", "Teléfono", "Turno", "Empresa", "Estado");
        };
    }

    /**
     * Ejecuta la consulta según tipo de reporte y filtros.
     */
    public List<Map<String, Object>> getData(TipoReporte tipo, FiltrosReporte f) {
        return switch (tipo) {
            case HISTORIAL_INGRESOS  -> getHistorialIngresos(f);
            case APRENDICES          -> getAprendices(f);
            case RESUMEN_GENERAL     -> getResumenGeneral(f);
            case ESCANEOS_QR         -> getEscaneos(f);
            case PERSONAL_SEGURIDAD  -> getPersonalSeguridad(f);
        };
    }

    // ── Historial de Ingresos ─────────────────────────────────────────────────

    private List<Map<String, Object>> getHistorialIngresos(FiltrosReporte f) {
        StringBuilder sql = new StringBuilder(
            "SELECT hi.num_doc, p.nombres, p.p_ape, " +
            "       DATE(hi.fecha_hora_ingreso) AS fecha_ing, " +
            "       TIME(hi.fecha_hora_ingreso) AS hora_ing, " +
            "       DATE(hi.fecha_hora_salida)  AS fecha_sal, " +
            "       TIME(hi.fecha_hora_salida)  AS hora_sal, " +
            "       CASE hi.estado_movimiento WHEN 1 THEN 'Ingreso' ELSE 'Salida' END AS movimiento, " +
            "       hi.observacion " +
            "FROM historial_ingreso hi " +
            "INNER JOIN persona p ON hi.num_doc = p.num_doc " +
            "WHERE 1=1 ");
        if (f.getFechaInicio() != null)
            sql.append(" AND DATE(hi.fecha_hora_ingreso) >= '").append(f.getFechaInicio()).append("'");
        if (f.getFechaFin() != null)
            sql.append(" AND DATE(hi.fecha_hora_ingreso) <= '").append(f.getFechaFin()).append("'");
        sql.append(" ORDER BY hi.fecha_hora_ingreso DESC");

        return query(sql.toString(),
            "num_doc","nombres","p_ape",
            "fecha_ing","hora_ing","fecha_sal","hora_sal","movimiento","observacion");
    }

    // ── Aprendices ────────────────────────────────────────────────────────────

    private List<Map<String, Object>> getAprendices(FiltrosReporte f) {
        StringBuilder sql = new StringBuilder(
            "SELECT p.tip_doc, p.num_doc, p.nombres, p.p_ape, p.email, " +
            "       fi.num_ficha, fi.nom_ficha, fi.siglas_centro, " +
            "       CASE c.estado WHEN 1 THEN 'Activo' ELSE 'Inactivo' END AS estado " +
            "FROM aprendiz a " +
            "INNER JOIN persona p ON a.num_doc = p.num_doc " +
            "LEFT  JOIN ficha   fi ON a.id_ficha = fi.id_ficha " +
            "LEFT  JOIN cuenta  c  ON a.num_doc  = c.num_doc " +
            "WHERE 1=1 ");
        if (notNull(f.getEstadoAprendiz()))
            sql.append(" AND c.estado = ").append("Activo".equals(f.getEstadoAprendiz()) ? "1" : "0");
        sql.append(" ORDER BY p.p_ape, p.nombres");

        return query(sql.toString(),
            "tip_doc","num_doc","nombres","p_ape","email",
            "num_ficha","nom_ficha","siglas_centro","estado");
    }

    // ── Resumen General ───────────────────────────────────────────────────────

    private List<Map<String, Object>> getResumenGeneral(FiltrosReporte f) {
        StringBuilder sql = new StringBuilder(
            "SELECT DATE(fecha_hora_ingreso) AS fecha, " +
            "       SUM(CASE estado_movimiento WHEN 1 THEN 1 ELSE 0 END) AS ingresos, " +
            "       SUM(CASE estado_movimiento WHEN 0 THEN 1 ELSE 0 END) AS salidas, " +
            "       SUM(CASE estado_movimiento WHEN 1 THEN 1 ELSE -1 END) AS neto " +
            "FROM historial_ingreso WHERE 1=1 ");
        if (f.getFechaInicio() != null)
            sql.append(" AND DATE(fecha_hora_ingreso) >= '").append(f.getFechaInicio()).append("'");
        if (f.getFechaFin() != null)
            sql.append(" AND DATE(fecha_hora_ingreso) <= '").append(f.getFechaFin()).append("'");
        sql.append(" GROUP BY fecha ORDER BY fecha DESC");

        return query(sql.toString(), "fecha","ingresos","salidas","neto");
    }

    // ── Escaneos QR ───────────────────────────────────────────────────────────

    private List<Map<String, Object>> getEscaneos(FiltrosReporte f) {
        StringBuilder sql = new StringBuilder(
            "SELECT DATE(hi.fecha_hora_ingreso) AS fecha, " +
            "       TIME(hi.fecha_hora_ingreso) AS hora, " +
            "       hi.num_doc, p.nombres, " +
            "       CASE hi.estado_movimiento WHEN 1 THEN 'Ingreso' ELSE 'Salida' END AS movimiento " +
            "FROM historial_ingreso hi " +
            "INNER JOIN persona p ON hi.num_doc = p.num_doc " +
            "WHERE 1=1 ");
        if (f.getFechaInicio() != null)
            sql.append(" AND DATE(hi.fecha_hora_ingreso) >= '").append(f.getFechaInicio()).append("'");
        if (f.getFechaFin() != null)
            sql.append(" AND DATE(hi.fecha_hora_ingreso) <= '").append(f.getFechaFin()).append("'");
        sql.append(" ORDER BY hi.fecha_hora_ingreso DESC");

        return query(sql.toString(), "fecha","hora","num_doc","nombres","movimiento");
    }

    // ── Personal de Seguridad ─────────────────────────────────────────────────

    private List<Map<String, Object>> getPersonalSeguridad(FiltrosReporte f) {
        StringBuilder sql = new StringBuilder(
            "SELECT p.tip_doc, p.num_doc, p.nombres, p.p_ape, p.email, p.tel, " +
            "       ps.turno, ps.empresa_seg, " +
            "       CASE c.estado WHEN 1 THEN 'Activo' ELSE 'Inactivo' END AS estado " +
            "FROM personal_seguridad ps " +
            "INNER JOIN persona p ON ps.num_doc = p.num_doc " +
            "LEFT  JOIN cuenta  c ON ps.num_doc = c.num_doc " +
            "WHERE 1=1 ");
        if (notNull(f.getGuarda()))
            sql.append(" AND p.num_doc = ").append(f.getGuarda().replaceAll("[^0-9]",""));
        sql.append(" ORDER BY p.p_ape, p.nombres");

        return query(sql.toString(),
            "tip_doc","num_doc","nombres","p_ape","email",
            "tel","turno","empresa_seg","estado");
    }

    // ── Listas para ComboBox ──────────────────────────────────────────────────

    public List<String> getProgramas() {
        List<String> list = new ArrayList<>();
        list.add("Todos");
        String sql = "SELECT DISTINCT nom_ficha FROM ficha ORDER BY nom_ficha";
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString(1));
        } catch (SQLException e) { /* silencioso */ }
        return list;
    }

    public List<String> getCentros() {
        List<String> list = new ArrayList<>();
        list.add("Todos");
        String sql = "SELECT DISTINCT siglas_centro FROM ficha WHERE siglas_centro IS NOT NULL ORDER BY siglas_centro";
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString(1));
        } catch (SQLException e) { /* silencioso */ }
        return list;
    }

    public List<String> getGuardas() {
        List<String> list = new ArrayList<>();
        list.add("Todos");
        String sql =
            "SELECT CONCAT(p.nombres,' ',p.p_ape,' - ',ps.num_doc) " +
            "FROM personal_seguridad ps INNER JOIN persona p ON ps.num_doc=p.num_doc " +
            "ORDER BY p.p_ape";
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString(1));
        } catch (SQLException e) { /* silencioso */ }
        return list;
    }

    // ── Helper genérico ───────────────────────────────────────────────────────

    private List<Map<String, Object>> query(String sql, String... cols) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String col : cols) {
                    Object v = rs.getObject(col);
                    row.put(col, v != null ? v.toString() : "");
                }
                rows.add(row);
            }
        } catch (SQLException e) {
            System.err.println("[GeneradorReportesDAO] " + e.getMessage());
        }
        return rows;
    }

    private boolean notNull(String v) {
        return v != null && !v.isBlank() && !"Todos".equals(v);
    }
}
