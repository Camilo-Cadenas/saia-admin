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
import com.saia.model.Administrador;
import com.saia.model.Persona;

/**
 * DAO para la tabla {@code administrador}.
 *
 * Cambios BD nueva:
 *  - num_doc es INT
 *  - El login usa la tabla cuenta (mail + password_hash),
 *    no el email de persona. Este DAO sigue haciendo JOIN con persona
 *    para obtener los datos de la sesión.
 */
public class AdministradorDAO {

    private static final String SQL_BASE =
            "SELECT a.id_admin, a.num_doc, " +
            "       p.tip_doc, p.nombres, p.p_ape, p.s_ape, p.tel, " +
            "       p.tip_sang, p.genero, p.fecha_nac, p.email " +
            "FROM administrador a " +
            "INNER JOIN persona p ON a.num_doc = p.num_doc ";

    // Busca por el mail de la tabla CUENTA (no el email de persona)
    private static final String SQL_FIND_BY_MAIL =
            SQL_BASE +
            "INNER JOIN cuenta c ON a.num_doc = c.num_doc " +
            "WHERE LOWER(c.mail) = LOWER(?)";

    private static final String SQL_FIND_BY_NUM_DOC =
            SQL_BASE + "WHERE a.num_doc = ?";

    private static final String SQL_FIND_BY_ID =
            SQL_BASE + "WHERE a.id_admin = ?";

    private static final String SQL_FIND_ALL =
            SQL_BASE + "ORDER BY p.p_ape, p.nombres";

    private static final String SQL_INSERT =
            "INSERT INTO administrador (num_doc) VALUES (?)";

    private static final String SQL_EXISTS =
            "SELECT COUNT(*) FROM administrador WHERE num_doc = ?";

    // ── Consultas ─────────────────────────────────────────────────────────────

    /**
     * Busca el administrador por el mail registrado en la tabla {@code cuenta}.
     * Este es el método usado durante el login.
     */
    public Optional<Administrador> findByMail(String mail) {
        if (mail == null || mail.isBlank()) return Optional.empty();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_MAIL)) {
            ps.setString(1, mail.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando administrador por mail: " + mail, e);
        }
        return Optional.empty();
    }

    public Optional<Administrador> findByNumDoc(int numDoc) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_NUM_DOC)) {
            ps.setInt(1, numDoc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando administrador numDoc=" + numDoc, e);
        }
        return Optional.empty();
    }

    public Optional<Administrador> findById(int idAdmin) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, idAdmin);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando administrador id=" + idAdmin, e);
        }
        return Optional.empty();
    }

    public List<Administrador> findAll() {
        List<Administrador> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DataAccessException("Error listando administradores", e);
        }
        return lista;
    }

    public void insert(int numDoc) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_INSERT)) {
            ps.setInt(1, numDoc);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error insertando administrador numDoc=" + numDoc, e);
        }
    }

    public boolean exists(int numDoc) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_EXISTS)) {
            ps.setInt(1, numDoc);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error verificando administrador numDoc=" + numDoc, e);
        }
    }

    // ── Mapeo ─────────────────────────────────────────────────────────────────

    private Administrador mapRow(ResultSet rs) throws SQLException {
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

        Administrador admin = new Administrador();
        admin.setIdAdmin(rs.getInt("id_admin"));
        admin.setNumDoc (rs.getInt("num_doc"));
        admin.setPersona(p);
        return admin;
    }
}
