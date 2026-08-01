package com.saia.data;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.saia.db.ConnectionPool;
import com.saia.model.Aprendiz;
import com.saia.model.Persona;

/**
 * DAO para consultas de aprendices.
 * ESTRUCTURA NUEVA: Los aprendices son personas con id_rol=1 (Aprendiz) en la tabla cuenta.
 * Ya NO existe la tabla aprendiz independiente.
 * 
 * Hace JOIN con persona y cuenta para obtener datos completos del aprendiz.
 */
public class AprendizDAO {

    // ── NUEVA ESTRUCTURA: Aprendices identificados por rol ────────────────────
    
    private static final String SQL_BASE =
            "SELECT c.num_doc, c.id_cuenta, c.estado AS cuenta_activa, " +
            "       p.tip_doc, p.nombres, p.p_ape, p.s_ape, " +
            "       p.tel, p.tip_sang, p.genero, p.fecha_nac, p.email, p.foto_perfil " +
            // "       f.id_ficha AS num_ficha, " +  // Comentado: no hay relación directa ahora
            // "       COALESCE(f.nom_ficha, '') AS nom_ficha, " +
            // "       cf.siglas_centro " +
            "FROM cuenta c " +
            "INNER JOIN persona p ON c.num_doc = p.num_doc " +
            "INNER JOIN rol r ON c.id_rol = r.id_rol " +
            "WHERE r.nom_rol = 'Aprendiz' ";

    private static final String SQL_FIND_ALL =
            SQL_BASE + "ORDER BY p.p_ape, p.nombres";

    private static final String SQL_FIND_BY_NUM_DOC =
            SQL_BASE + "AND c.num_doc = ?";

    private static final String SQL_FIND_BY_ID_CUENTA =
            SQL_BASE + "AND c.id_cuenta = ?";

    /* ══════════════════════════════════════════════════════════════════════════
     * CÓDIGO ANTIGUO (COMENTADO) - Ya no se usa la tabla aprendiz
     * ══════════════════════════════════════════════════════════════════════════
     * 
     * private static final String SQL_BASE_OLD =
     *         "SELECT a.id_aprendiz, a.num_doc, a.id_ficha, " +
     *         "       p.tip_doc, p.nombres, p.p_ape, p.s_ape, " +
     *         "       p.tel, p.tip_sang, p.genero, p.fecha_nac, p.email, " +
     *         "       f.id_ficha AS num_ficha, " +
     *         "       COALESCE(f.nom_ficha, '') AS nom_ficha, " +
     *         "       cf.siglas_centro, " +
     *         "       COALESCE(c.estado, TRUE) AS cuenta_activa " +
     *         "FROM aprendiz a " +
     *         "INNER JOIN persona           p  ON a.num_doc    = p.num_doc " +
     *         "LEFT  JOIN ficha             f  ON a.id_ficha   = f.id_ficha " +
     *         "LEFT  JOIN programa_formacion pf ON f.id_programa = pf.id_programa " +
     *         "LEFT  JOIN centro_formacion   cf ON pf.id_centro  = cf.id_centro " +
     *         "LEFT  JOIN cuenta             c  ON a.num_doc    = c.num_doc ";
     *
     * ══════════════════════════════════════════════════════════════════════════
     */

    // ── Consultas ─────────────────────────────────────────────────────────────

    public List<Aprendiz> findAll() {
        List<Aprendiz> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DataAccessException("Error listando aprendices", e);
        }
        return lista;
    }

    public Optional<Aprendiz> findByNumDoc(int numDoc) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_NUM_DOC)) {
            ps.setInt(1, numDoc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando aprendiz numDoc=" + numDoc, e);
        }
        return Optional.empty();
    }

    /**
     * Busca un aprendiz por ID de cuenta (antes se usaba id_aprendiz).
     * @param idCuenta ID de la cuenta del aprendiz
     */
    public Optional<Aprendiz> findByIdCuenta(int idCuenta) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_ID_CUENTA)) {
            ps.setInt(1, idCuenta);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando aprendiz idCuenta=" + idCuenta, e);
        }
        return Optional.empty();
    }

    /**
     * @deprecated Ya no existe id_aprendiz. Usar findByIdCuenta() o findByNumDoc()
     */
    @Deprecated
    public Optional<Aprendiz> findById(int id) {
        System.err.println("[AprendizDAO] ADVERTENCIA: findById() está deprecado. Use findByIdCuenta() o findByNumDoc()");
        return findByIdCuenta(id);
    }

    // ── Mapeo ─────────────────────────────────────────────────────────────────

    private Aprendiz mapRow(ResultSet rs) throws SQLException {
        Persona p = new Persona();
        p.setNumDoc   (rs.getInt   ("num_doc"));
        p.setTipDoc   (rs.getString("tip_doc"));
        p.setNombres  (rs.getString("nombres"));
        p.setPApe     (rs.getString("p_ape"));
        p.setSApe     (rs.getString("s_ape"));
        p.setTel      (rs.getString("tel"));
        p.setTipSang  (rs.getString("tip_sang"));
        p.setGenero   (rs.getString("genero"));
        p.setEmail    (rs.getString("email"));
        p.setFotoPerfil(rs.getString("foto_perfil"));
        
        Date fn = rs.getDate("fecha_nac");
        if (fn != null) p.setFechaNac(fn.toLocalDate());

        Aprendiz a = new Aprendiz();
        // NUEVA ESTRUCTURA: Usamos id_cuenta como identificador
        a.setIdAprendiz  (rs.getInt    ("id_cuenta")); // Ahora es id_cuenta
        a.setNumDoc      (rs.getInt    ("num_doc"));
        a.setIdFicha     (0); // Ya no hay relación directa con ficha
        a.setPersona     (p);
        a.setCuentaActiva(rs.getBoolean("cuenta_activa"));
        
        // Campos de ficha y centro ya no están disponibles en la nueva estructura
        a.setSiglasProg  (""); // No disponible sin relación con ficha
        a.setSiglasCentro(""); // No disponible sin relación con centro
        
        return a;
    }
    
    /* ══════════════════════════════════════════════════════════════════════════
     * CÓDIGO DE MAPEO ANTIGUO (COMENTADO)
     * ══════════════════════════════════════════════════════════════════════════
     * 
     * private Aprendiz mapRow(ResultSet rs) throws SQLException {
     *     Persona p = new Persona();
     *     p.setNumDoc (rs.getInt   ("num_doc"));
     *     p.setTipDoc (rs.getString("tip_doc"));
     *     p.setNombres(rs.getString("nombres"));
     *     p.setPApe   (rs.getString("p_ape"));
     *     p.setSApe   (rs.getString("s_ape"));
     *     p.setTel    (rs.getString("tel"));
     *     p.setTipSang(rs.getString("tip_sang"));
     *     p.setGenero (rs.getString("genero"));
     *     Date fn = rs.getDate("fecha_nac");
     *     if (fn != null) p.setFechaNac(fn.toLocalDate());
     *     p.setEmail  (rs.getString("email"));
     *
     *     Aprendiz a = new Aprendiz();
     *     a.setIdAprendiz  (rs.getInt    ("id_aprendiz"));
     *     a.setNumDoc      (rs.getInt    ("num_doc"));
     *     a.setIdFicha     (rs.getInt    ("id_ficha"));
     *     a.setPersona     (p);
     *     a.setCuentaActiva(rs.getBoolean("cuenta_activa"));
     *     a.setSiglasProg  (rs.getString ("nom_ficha"));
     *     a.setSiglasCentro(rs.getString ("siglas_centro"));
     *     return a;
     * }
     *
     * ══════════════════════════════════════════════════════════════════════════
     */
}
