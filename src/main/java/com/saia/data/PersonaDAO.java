package com.saia.data;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import com.saia.db.ConnectionPool;
import com.saia.model.Persona;

/**
 * DAO para la tabla {@code persona}.
 *
 * Cambios respecto a la BD nueva:
 *  - num_doc es INT
 *  - sexo → genero
 *  - password_hash eliminado de persona (ahora en cuenta)
 */
public class PersonaDAO {

    private static final String COLS =
            "num_doc, tip_doc, nombres, p_ape, s_ape, tel, tip_sang, genero, fecha_nac, email";

    private static final String SQL_FIND_BY_EMAIL =
            "SELECT " + COLS + " FROM persona WHERE LOWER(email) = LOWER(?)";

    private static final String SQL_FIND_BY_NUM_DOC =
            "SELECT " + COLS + " FROM persona WHERE num_doc = ?";

    private static final String SQL_EXISTS_NUM_DOC =
            "SELECT COUNT(*) FROM persona WHERE num_doc = ?";

    private static final String SQL_EXISTS_EMAIL =
            "SELECT COUNT(*) FROM persona WHERE LOWER(email) = LOWER(?) AND num_doc <> ?";

    // INSERT completo — incluye todos los campos del formulario de registro
    private static final String SQL_INSERT =
            "INSERT INTO persona (num_doc, tip_doc, nombres, p_ape, s_ape, " +
            "                     tel, tip_sang, genero, fecha_nac, email) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    // Update completo (para edición de perfil)
    private static final String SQL_UPDATE =
            "UPDATE persona SET tip_doc=?, nombres=?, p_ape=?, s_ape=?, tel=?, " +
            "       tip_sang=?, genero=?, fecha_nac=?, email=? " +
            "WHERE num_doc = ?";

    // ── Consultas ─────────────────────────────────────────────────────────────

    public Optional<Persona> findByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_EMAIL)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando persona por email: " + email, e);
        }
        return Optional.empty();
    }

    public Optional<Persona> findByNumDoc(int numDoc) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_NUM_DOC)) {
            ps.setInt(1, numDoc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando persona numDoc=" + numDoc, e);
        }
        return Optional.empty();
    }

    public boolean existsByNumDoc(int numDoc) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_EXISTS_NUM_DOC)) {
            ps.setInt(1, numDoc);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error verificando numDoc=" + numDoc, e);
        }
    }

    public boolean emailExistsForOther(String email, int excludeDoc) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_EXISTS_EMAIL)) {
            ps.setString(1, email.trim());
            ps.setInt   (2, excludeDoc);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error verificando email único", e);
        }
    }

    // ── Escritura ─────────────────────────────────────────────────────────────

    /**
     * Inserta los campos obligatorios de persona para el registro de personal.
     * Los datos opcionales (tel, tip_sang, genero, fecha_nac) se actualizan luego.
     */
    public void insert(Persona p) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_INSERT)) {
            ps.setInt   (1,  p.getNumDoc());
            ps.setString(2,  p.getTipDoc());
            ps.setString(3,  p.getNombres());
            ps.setString(4,  p.getPApe());
            ps.setString(5,  nullIfBlank(p.getSApe()));
            ps.setString(6,  nullIfBlank(p.getTel()));
            ps.setString(7,  nullIfBlank(p.getTipSang()));
            ps.setString(8,  nullIfBlank(p.getGenero()));
            ps.setDate  (9,  p.getFechaNac() != null ? Date.valueOf(p.getFechaNac()) : null);
            ps.setString(10, p.getEmail() != null ? p.getEmail().trim().toLowerCase() : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error insertando persona numDoc=" + p.getNumDoc(), e);
        }
    }

    /**
     * Actualiza todos los campos editables de una persona (para edición de perfil).
     */
    public void update(Persona p) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, p.getTipDoc());
            ps.setString(2, p.getNombres());
            ps.setString(3, p.getPApe());
            ps.setString(4, nullIfBlank(p.getSApe()));
            ps.setString(5, nullIfBlank(p.getTel()));
            ps.setString(6, nullIfBlank(p.getTipSang()));
            ps.setString(7, nullIfBlank(p.getGenero()));
            ps.setDate  (8, p.getFechaNac() != null ? Date.valueOf(p.getFechaNac()) : null);
            ps.setString(9, p.getEmail() != null ? p.getEmail().trim().toLowerCase() : null);
            ps.setInt   (10, p.getNumDoc());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error actualizando persona numDoc=" + p.getNumDoc(), e);
        }
    }

    // ── Mapeo ─────────────────────────────────────────────────────────────────

    private Persona mapRow(ResultSet rs) throws SQLException {
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
        return p;
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
