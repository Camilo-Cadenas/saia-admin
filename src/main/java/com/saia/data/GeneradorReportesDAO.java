package com.saia.data;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saia.db.ConnectionPool;
import com.saia.model.FiltrosReporte;
import com.saia.model.TipoReporte;

/**
 * DAO para el módulo Generador de Reportes.
 * Devuelve datos como List<Map<String,Object>> para tabla y exportación.
 *
 * Estructura real de la BD:
 *   ficha         → id_ficha, id_programa, nom_ficha, fecha_inicio, fecha_fin, jornada
 *   programa_formacion → id_programa, id_centro, siglas_prog, nom_prog, nivel_formacion
 *   centro_formacion   → id_centro, nom_centro, siglas_centro
 */
public class GeneradorReportesDAO {

    // ── Columnas para cada tipo ───────────────────────────────────────────────

    public static List<String> getColumnas(TipoReporte tipo) {
        return switch (tipo) {
            case HISTORIAL_INGRESOS -> List.of(
                "N° Doc", "Tipo Doc", "Nombres", "Apellidos",
                "Programa", "Centro Formación", "Jornada",
                "Fecha Ingreso", "Hora Ingreso",
                "Fecha Salida", "Hora Salida",
                "Tipo Movimiento", "Observación");
            case APRENDICES -> List.of(
                "Tipo Doc", "N° Doc", "Nombres", "Apellidos",
                "Correo", "Teléfono",
                "N° Ficha", "Nombre Ficha", "Jornada",
                "Programa", "Siglas Programa", "Nivel Formación",
                "Centro", "Siglas Centro",
                "Fecha Inicio Ficha", "Fecha Fin Ficha",
                "Estado Cuenta");
            case RESUMEN_GENERAL -> List.of(
                "Fecha", "Día Semana",
                "Total Ingresos", "Total Salidas", "Diferencia Neta",
                "Primera Entrada", "Última Entrada");
            case ESCANEOS_QR -> List.of(
                "Fecha", "Hora",
                "N° Doc", "Tipo Doc", "Nombres", "Apellidos",
                "Programa", "N° Ficha",
                "Tipo Movimiento", "Observación");
            case PERSONAL_SEGURIDAD -> List.of(
                "Tipo Doc", "N° Doc", "Nombres", "Apellidos",
                "Correo", "Teléfono",
                "Tipo Sangre", "Fecha Nacimiento",
                "Turno", "Empresa Seguridad",
                "Estado Cuenta");
        };
    }

    // ── Dispatcher principal ─────────────────────────────────────────────────

    public List<Map<String, Object>> getData(TipoReporte tipo, FiltrosReporte f) {
        return switch (tipo) {
            case HISTORIAL_INGRESOS  -> getHistorialIngresos(f);
            case APRENDICES          -> getAprendices(f);
            case RESUMEN_GENERAL     -> getResumenGeneral(f);
            case ESCANEOS_QR         -> getEscaneos(f);
            case PERSONAL_SEGURIDAD  -> getPersonalSeguridad(f);
        };
    }

    // ── 1. Historial de Ingresos ──────────────────────────────────────────────

    private List<Map<String, Object>> getHistorialIngresos(FiltrosReporte f) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT
                hi.num_doc,
                p.tip_doc,
                p.nombres,
                CONCAT(p.p_ape, IFNULL(CONCAT(' ', p.s_ape), ''))      AS apellidos,
                pf.nom_prog                                              AS programa,
                cf.nom_centro                                            AS centro,
                f.jornada,
                DATE(hi.fecha_hora_ingreso)                              AS fecha_ing,
                TIME_FORMAT(hi.fecha_hora_ingreso, '%H:%i:%s')           AS hora_ing,
                DATE(hi.fecha_hora_salida)                               AS fecha_sal,
                TIME_FORMAT(hi.fecha_hora_salida,  '%H:%i:%s')           AS hora_sal,
                CASE WHEN hi.fecha_hora_salida IS NOT NULL THEN 'Completo' ELSE 'Sin salida'
                     END                                                      AS movimiento,
                IFNULL(hi.observacion, '')                               AS observacion
            FROM historial_ingreso hi
            INNER JOIN persona          p   ON hi.num_doc      = p.num_doc
            LEFT  JOIN aprendiz         a   ON hi.num_doc      = a.num_doc
            LEFT  JOIN ficha            f   ON a.id_ficha      = f.id_ficha
            LEFT  JOIN programa_formacion pf ON f.id_programa  = pf.id_programa
            LEFT  JOIN centro_formacion  cf ON pf.id_centro    = cf.id_centro
            WHERE 1=1
            """);

        appendFechaFiltros(sql, params, f, "hi.fecha_hora_ingreso");

        if (notNull(f.getSedeCentro())) {
            sql.append(" AND cf.siglas_centro = ? ");
            params.add(f.getSedeCentro());
        }
        if (notNull(f.getJornada())) {
            sql.append(" AND f.jornada = ? ");
            params.add(f.getJornada().toUpperCase());
        }
        sql.append(" ORDER BY hi.fecha_hora_ingreso DESC");

        return query(sql.toString(), params,
            "num_doc","tip_doc","nombres","apellidos",
            "programa","centro","jornada",
            "fecha_ing","hora_ing","fecha_sal","hora_sal",
            "movimiento","observacion");
    }

    // ── 2. Aprendices ──────────────────────────────────────────────────────────

    private List<Map<String, Object>> getAprendices(FiltrosReporte f) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT
                p.tip_doc,
                p.num_doc,
                p.nombres,
                CONCAT(p.p_ape, IFNULL(CONCAT(' ', p.s_ape), ''))     AS apellidos,
                IFNULL(p.email, '')                                    AS email,
                IFNULL(p.tel,   '')                                    AS tel,
                f.nom_ficha                                            AS nom_ficha,
                fi.nom_ficha                                           AS nom_ficha2,
                f.jornada,
                pf.nom_prog                                            AS programa,
                pf.siglas_prog,
                pf.nivel_formacion,
                cf.nom_centro                                          AS centro,
                cf.siglas_centro,
                DATE_FORMAT(f.fecha_inicio, '%d/%m/%Y')                AS fecha_inicio,
                DATE_FORMAT(f.fecha_fin,    '%d/%m/%Y')                AS fecha_fin,
                CASE c.estado WHEN 1 THEN 'Activo' ELSE 'Inactivo' END AS estado
            FROM aprendiz a
            INNER JOIN persona          p   ON a.num_doc     = p.num_doc
            LEFT  JOIN ficha            f   ON a.id_ficha    = f.id_ficha
            LEFT  JOIN ficha            fi  ON a.id_ficha    = fi.id_ficha
            LEFT  JOIN programa_formacion pf ON f.id_programa = pf.id_programa
            LEFT  JOIN centro_formacion  cf ON pf.id_centro   = cf.id_centro
            LEFT  JOIN cuenta            c  ON a.num_doc      = c.num_doc
            WHERE 1=1
            """);

        if (notNull(f.getEstadoAprendiz())) {
            sql.append(" AND c.estado = ? ");
            params.add("Activo".equals(f.getEstadoAprendiz()) ? 1 : 0);
        }
        if (notNull(f.getSedeCentro())) {
            sql.append(" AND cf.siglas_centro = ? ");
            params.add(f.getSedeCentro());
        }
        if (notNull(f.getPrograma())) {
            sql.append(" AND pf.siglas_prog = ? ");
            params.add(f.getPrograma());
        }
        if (notNull(f.getJornada())) {
            sql.append(" AND f.jornada = ? ");
            params.add(f.getJornada().toUpperCase());
        }
        sql.append(" ORDER BY p.p_ape, p.nombres");

        return query(sql.toString(), params,
            "tip_doc","num_doc","nombres","apellidos",
            "email","tel",
            "nom_ficha","nom_ficha","jornada",
            "programa","siglas_prog","nivel_formacion",
            "centro","siglas_centro",
            "fecha_inicio","fecha_fin","estado");
    }

    // ── 3. Resumen General ────────────────────────────────────────────────────

    private List<Map<String, Object>> getResumenGeneral(FiltrosReporte f) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT
                DATE(fecha_hora_ingreso)                                        AS fecha,
                DAYNAME(fecha_hora_ingreso)                                     AS dia_semana,
                COUNT(*)                                                        AS ingresos,
                SUM(CASE WHEN fecha_hora_salida IS NOT NULL THEN 1 ELSE 0 END) AS salidas,
                COUNT(*) - SUM(CASE WHEN fecha_hora_salida IS NOT NULL THEN 1 ELSE 0 END) AS neto,
                MIN(TIME_FORMAT(fecha_hora_ingreso,'%H:%i'))                    AS primera_entrada,
                MAX(TIME_FORMAT(fecha_hora_ingreso,'%H:%i'))                    AS ultima_entrada
            FROM historial_ingreso
            WHERE 1=1
            """);

        appendFechaFiltros(sql, params, f, "fecha_hora_ingreso");
        sql.append(" GROUP BY DATE(fecha_hora_ingreso) ORDER BY fecha DESC");

        return query(sql.toString(), params,
            "fecha","dia_semana","ingresos","salidas","neto",
            "primera_entrada","ultima_entrada");
    }

    // ── 4. Escaneos QR ────────────────────────────────────────────────────────

    private List<Map<String, Object>> getEscaneos(FiltrosReporte f) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT
                DATE(hi.fecha_hora_ingreso)                            AS fecha,
                TIME_FORMAT(hi.fecha_hora_ingreso, '%H:%i:%s')         AS hora,
                hi.num_doc,
                p.tip_doc,
                p.nombres,
                CONCAT(p.p_ape, IFNULL(CONCAT(' ', p.s_ape), ''))     AS apellidos,
                IFNULL(pf.nom_prog, '')                                AS programa,
                IFNULL(f.nom_ficha, '')                                AS nom_ficha,
                CASE WHEN hi.fecha_hora_salida IS NOT NULL THEN 'Completo' ELSE 'Sin salida'
                     END                                                       AS movimiento,
                IFNULL(hi.observacion, '')                             AS observacion
            FROM historial_ingreso hi
            INNER JOIN persona          p   ON hi.num_doc      = p.num_doc
            LEFT  JOIN aprendiz         a   ON hi.num_doc      = a.num_doc
            LEFT  JOIN ficha            f   ON a.id_ficha      = f.id_ficha
            LEFT  JOIN programa_formacion pf ON f.id_programa  = pf.id_programa
            WHERE 1=1
            """);

        appendFechaFiltros(sql, params, f, "hi.fecha_hora_ingreso");
        sql.append(" ORDER BY hi.fecha_hora_ingreso DESC");

        return query(sql.toString(), params,
            "fecha","hora","num_doc","tip_doc","nombres","apellidos",
            "programa","nom_ficha","movimiento","observacion");
    }

    // ── 5. Personal de Seguridad ──────────────────────────────────────────────

    private List<Map<String, Object>> getPersonalSeguridad(FiltrosReporte f) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT
                p.tip_doc,
                p.num_doc,
                p.nombres,
                CONCAT(p.p_ape, IFNULL(CONCAT(' ', p.s_ape), ''))     AS apellidos,
                IFNULL(p.email,    '')                                 AS email,
                IFNULL(p.tel,      '')                                 AS tel,
                IFNULL(p.tip_sang, '')                                 AS tip_sang,
                DATE_FORMAT(p.fecha_nac, '%d/%m/%Y')                   AS fecha_nac,
                IFNULL(ps.turno,       '')                             AS turno,
                IFNULL(ps.empresa_seg, '')                             AS empresa_seg,
                CASE c.estado WHEN 1 THEN 'Activo' ELSE 'Inactivo' END AS estado
            FROM personal_seguridad ps
            INNER JOIN persona p ON ps.num_doc = p.num_doc
            LEFT  JOIN cuenta  c ON ps.num_doc = c.num_doc
            WHERE 1=1
            """);

        if (notNull(f.getGuarda())) {
            // El valor del combo es "Nombre - numDoc", extraemos el numDoc al final
            String g = f.getGuarda();
            String numStr = g.contains("-") ? g.substring(g.lastIndexOf("-") + 1).trim() : g.trim();
            if (numStr.matches("\\d+")) {
                sql.append(" AND ps.num_doc = ? ");
                params.add(Integer.parseInt(numStr));
            }
        }
        sql.append(" ORDER BY p.p_ape, p.nombres");

        return query(sql.toString(), params,
            "tip_doc","num_doc","nombres","apellidos",
            "email","tel","tip_sang","fecha_nac",
            "turno","empresa_seg","estado");
    }

    // ── Listas para ComboBox ──────────────────────────────────────────────────

    /** Programas desde programa_formacion (siglas_prog + nom_prog). */
    public List<String> getProgramas() {
        List<String> list = new ArrayList<>();
        list.add("Todos");
        String sql =
            "SELECT CONCAT(siglas_prog, ' - ', nom_prog) AS label, siglas_prog " +
            "FROM programa_formacion ORDER BY nom_prog";
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString("label"));
        } catch (SQLException e) { System.err.println("[GeneradorDAO] getProgramas: " + e.getMessage()); }
        return list;
    }

    /** Centros desde centro_formacion. */
    public List<String> getCentros() {
        List<String> list = new ArrayList<>();
        list.add("Todos");
        String sql =
            "SELECT CONCAT(siglas_centro, ' - ', nom_centro) AS label, siglas_centro " +
            "FROM centro_formacion ORDER BY nom_centro";
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString("label"));
        } catch (SQLException e) { System.err.println("[GeneradorDAO] getCentros: " + e.getMessage()); }
        return list;
    }

    /** Jornadas distintas desde ficha. */
    public List<String> getJornadas() {
        List<String> list = new ArrayList<>();
        list.add("Todos");
        String sql = "SELECT DISTINCT jornada FROM ficha WHERE jornada IS NOT NULL ORDER BY jornada";
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String j = rs.getString(1);
                if (j != null) list.add(capitalize(j));
            }
        } catch (SQLException e) { System.err.println("[GeneradorDAO] getJornadas: " + e.getMessage()); }
        return list;
    }

    /** Guardas (nombre + num_doc). */
    public List<String> getGuardas() {
        List<String> list = new ArrayList<>();
        list.add("Todos");
        String sql =
            "SELECT CONCAT(p.nombres,' ',p.p_ape,' - ',ps.num_doc) AS label " +
            "FROM personal_seguridad ps " +
            "INNER JOIN persona p ON ps.num_doc = p.num_doc " +
            "ORDER BY p.p_ape";
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString("label"));
        } catch (SQLException e) { System.err.println("[GeneradorDAO] getGuardas: " + e.getMessage()); }
        return list;
    }

    // ── Helpers internos ──────────────────────────────────────────────────────

    /** Agrega cláusulas de fecha al WHERE usando el campo indicado. */
    private void appendFechaFiltros(StringBuilder sql, List<Object> params,
                                     FiltrosReporte f, String campo) {
        if (f.getFechaInicio() != null) {
            sql.append(" AND DATE(").append(campo).append(") >= ? ");
            params.add(Date.valueOf(f.getFechaInicio()));
        }
        if (f.getFechaFin() != null) {
            sql.append(" AND DATE(").append(campo).append(") <= ? ");
            params.add(Date.valueOf(f.getFechaFin()));
        }
    }

    /** Ejecuta la query y construye la lista de mapas con las columnas indicadas. */
    private List<Map<String, Object>> query(String sql, List<Object> params, String... cols) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (String col : cols) {
                        Object v = rs.getObject(col);
                        row.put(col, v != null ? v.toString() : "");
                    }
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("[GeneradorReportesDAO] " + e.getMessage());
        }
        return rows;
    }

    private boolean notNull(String v) {
        return v != null && !v.isBlank() && !"Todos".equals(v) && !"Todas".equals(v);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
