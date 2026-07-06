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
 * DAO para la tabla {@code aprendiz}.
 * Hace JOIN con {@code persona} y LEFT JOIN con {@code cuenta}
 * para incluir el estado de la cuenta en cada registro.
 */
public class AprendizDAO {

    private static final String SQL_BASE =
            "SELECT a.id_aprendiz, a.num_doc, a.id_ficha, " +
            "       p.tip_doc, p.nombres, p.p_ape, p.s_ape, " +
            "       p.tel, p.tip_sang, p.genero, p.fecha_nac, p.email, " +
            "       f.id_ficha AS num_ficha, " +
            "       COALESCE(f.nom_ficha, '') AS nom_ficha, " +
            "       cf.siglas_centro, " +
            "       COALESCE(c.estado, TRUE) AS cuenta_activa " +
            "FROM aprendiz a " +
            "INNER JOIN persona           p  ON a.num_doc    = p.num_doc " +
            "LEFT  JOIN ficha             f  ON a.id_ficha   = f.id_ficha " +
            "LEFT  JOIN programa_formacion pf ON f.id_programa = pf.id_programa " +
            "LEFT  JOIN centro_formacion   cf ON pf.id_centro  = cf.id_centro " +
            "LEFT  JOIN cuenta             c  ON a.num_doc    = c.num_doc ";

    private static final String SQL_FIND_ALL =
            SQL_BASE + "ORDER BY p.p_ape, p.nombres";

    private static final String SQL_FIND_BY_NUM_DOC =
            SQL_BASE + "WHERE a.num_doc = ?";

    private static final String SQL_FIND_BY_ID =
            SQL_BASE + "WHERE a.id_aprendiz = ?";

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

    public Optional<Aprendiz> findById(int id) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando aprendiz id=" + id, e);
        }
        return Optional.empty();
    }

    // ── Mapeo ─────────────────────────────────────────────────────────────────

    private Aprendiz mapRow(ResultSet rs) throws SQLException {
        Persona p = new Persona();
        p.setNumDoc (rs.getInt   ("num_doc"));
        p.setTipDoc (rs.getString("tip_doc"));
        p.setNombres(rs.getString("nombres"));
        p.setPApe   (rs.getString("p_ape"));
        p.setSApe   (rs.getString("s_ape"));
        p.setTel    (rs.getString("tel"));
        p.setTipSang(rs.getString("tip_sang"));
        p.setGenero (rs.getString("genero"));
        Date fn = rs.getDate("fecha_nac");
        if (fn != null) p.setFechaNac(fn.toLocalDate());
        p.setEmail  (rs.getString("email"));

        Aprendiz a = new Aprendiz();
        a.setIdAprendiz  (rs.getInt    ("id_aprendiz"));
        a.setNumDoc      (rs.getInt    ("num_doc"));
        a.setIdFicha     (rs.getInt    ("id_ficha"));
        a.setPersona     (p);
        a.setCuentaActiva(rs.getBoolean("cuenta_activa"));
        a.setSiglasProg  (rs.getString ("nom_ficha"));
        a.setSiglasCentro(rs.getString ("siglas_centro"));
        return a;
    }
}
