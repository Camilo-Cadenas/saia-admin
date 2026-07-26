package com.saia.data;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import com.saia.db.ConnectionPool;
import com.saia.model.Cuenta;
import com.saia.model.Persona;

/**
 * DAO para el módulo de Configuración del Sistema (perfil del administrador).
 * Trabaja con las tablas: persona, administrador, cuenta.
 */
public class ConfiguracionDAO {

    // ── SELECT perfil completo ────────────────────────────────────────────────

    private static final String SQL_PERFIL =
        "SELECT p.num_doc, p.tip_doc, p.nombres, p.p_ape, p.s_ape, " +
        "       p.tel, p.tip_sang, p.genero, p.fecha_nac, p.email, " +
        "       p.foto_perfil, " +
        "       c.id_cuenta, c.mail, c.estado, c.fecha_creacion " +
        "FROM administrador a " +
        "INNER JOIN persona p ON a.num_doc = p.num_doc " +
        "INNER JOIN cuenta  c ON a.num_doc = c.num_doc " +
        "WHERE a.num_doc = ?";

    /**
     * Carga el perfil completo del administrador autenticado.
     * @param numDoc num_doc del administrador en sesión
     */
    public Optional<PerfilAdmin> findPerfil(int numDoc) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_PERFIL)) {
            ps.setInt(1, numDoc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Persona p = new Persona();
                    p.setNumDoc  (rs.getInt   ("num_doc"));
                    p.setTipDoc  (rs.getString("tip_doc"));
                    p.setNombres (rs.getString("nombres"));
                    p.setPApe    (rs.getString("p_ape"));
                    p.setSApe    (rs.getString("s_ape"));
                    p.setTel     (rs.getString("tel"));
                    p.setTipSang (rs.getString("tip_sang"));
                    p.setGenero  (rs.getString("genero"));
                    Date fn = rs.getDate("fecha_nac");
                    if (fn != null) p.setFechaNac(fn.toLocalDate());
                    p.setEmail   (rs.getString("email"));

                    Cuenta c = new Cuenta();
                    c.setIdCuenta(rs.getInt   ("id_cuenta"));
                    c.setMail    (rs.getString("mail"));
                    c.setEstado  (rs.getBoolean("estado"));

                    return Optional.of(new PerfilAdmin(p,
                        rs.getString("foto_perfil"), c));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ConfiguracionDAO] findPerfil: " + e.getMessage());
        }
        return Optional.empty();
    }

    // ── UPDATE datos personales ───────────────────────────────────────────────

    private static final String SQL_UPDATE_PERSONA =
        "UPDATE persona SET tip_doc=?, nombres=?, p_ape=?, s_ape=?, " +
        "tel=?, tip_sang=?, genero=?, fecha_nac=?, email=? " +
        "WHERE num_doc=?";

    public boolean updatePersona(Persona p) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_UPDATE_PERSONA)) {
            ps.setString(1, p.getTipDoc());
            ps.setString(2, p.getNombres());
            ps.setString(3, p.getPApe());
            ps.setString(4, p.getSApe());
            ps.setString(5, p.getTel());
            ps.setString(6, p.getTipSang());
            ps.setString(7, p.getGenero());
            if (p.getFechaNac() != null)
                ps.setDate(8, Date.valueOf(p.getFechaNac()));
            else
                ps.setNull(8, java.sql.Types.DATE);
            ps.setString(9,  p.getEmail());
            ps.setInt   (10, p.getNumDoc());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ConfiguracionDAO] updatePersona: " + e.getMessage());
            return false;
        }
    }

    // ── UPDATE foto de perfil ─────────────────────────────────────────────────

    public boolean updateFotoPerfil(int numDoc, String rutaFoto) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 "UPDATE persona SET foto_perfil=? WHERE num_doc=?")) {
            ps.setString(1, rutaFoto);
            ps.setInt   (2, numDoc);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ConfiguracionDAO] updateFotoPerfil: " + e.getMessage());
            return false;
        }
    }

    // ── UPDATE mail de cuenta ─────────────────────────────────────────────────

    public boolean updateMail(int numDoc, String newMail) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 "UPDATE cuenta SET mail=? WHERE num_doc=?")) {
            ps.setString(1, newMail.trim().toLowerCase());
            ps.setInt   (2, numDoc);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ConfiguracionDAO] updateMail: " + e.getMessage());
            return false;
        }
    }

    /** Verifica si otro usuario ya usa ese mail (excluyendo el propio numDoc). */
    public boolean mailExiste(int numDoc, String mail) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 "SELECT COUNT(*) FROM cuenta WHERE LOWER(mail)=LOWER(?) AND num_doc<>?")) {
            ps.setString(1, mail.trim());
            ps.setInt   (2, numDoc);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) { return false; }
    }

    // ── UPDATE contraseña ─────────────────────────────────────────────────────

    public boolean updatePassword(int numDoc, String newHash) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 "UPDATE cuenta SET password=? WHERE num_doc=?")) {
            ps.setString(1, newHash);
            ps.setInt   (2, numDoc);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ConfiguracionDAO] updatePassword: " + e.getMessage());
            return false;
        }
    }

    /** Obtiene el hash de contraseña actual para verificación. */
    public String getPasswordHash(int numDoc) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 "SELECT password FROM cuenta WHERE num_doc=?")) {
            ps.setInt(1, numDoc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("password");
            }
        } catch (SQLException e) { /* ignorar */ }
        return "";
    }

    // ── Modelo interno ────────────────────────────────────────────────────────

    /** DTO con todos los datos del perfil del administrador. */
    public record PerfilAdmin(Persona persona, String fotoPerfil, Cuenta cuenta) {}
}
