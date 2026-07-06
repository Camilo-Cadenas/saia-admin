package com.saia.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.saia.db.ConnectionPool;
import com.saia.model.Cuenta;
import com.saia.model.Persona;
import com.saia.model.Rol;

/**
 * DAO para la tabla {@code cuenta}.
 *
 * Cambios BD nueva:
 *  - num_doc es INT
 *  - Agrega columnas mail y password_hash
 */
public class CuentaDAO {

    private static final String SQL_BASE =
            "SELECT c.id_cuenta, c.id_rol, c.num_doc, c.mail, c.password_hash, " +
            "       c.estado, c.fecha_creacion, r.nom_rol, " +
            "       p.nombres, p.p_ape, p.s_ape, p.email " +
            "FROM cuenta c " +
            "INNER JOIN rol     r ON c.id_rol  = r.id_rol " +
            "INNER JOIN persona p ON c.num_doc = p.num_doc ";

    private static final String SQL_FIND_BY_NUM_DOC =
            SQL_BASE + "WHERE c.num_doc = ?";

    private static final String SQL_FIND_BY_MAIL =
            SQL_BASE + "WHERE LOWER(c.mail) = LOWER(?)";

    private static final String SQL_FIND_ALL =
            SQL_BASE + "ORDER BY p.p_ape, p.nombres";

    private static final String SQL_FIND_ACTIVE =
            SQL_BASE + "WHERE c.estado = TRUE ORDER BY p.p_ape, p.nombres";

    // Inserción con estado explícito
    private static final String SQL_INSERT =
            "INSERT INTO cuenta (id_rol, num_doc, mail, password_hash, estado) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE_ESTADO =
            "UPDATE cuenta SET estado = ? WHERE num_doc = ?";

    private static final String SQL_UPDATE_PASSWORD =
            "UPDATE cuenta SET password_hash = ? WHERE num_doc = ?";

    private static final String SQL_EXISTS =
            "SELECT COUNT(*) FROM cuenta WHERE num_doc = ?";

    // ── Consultas ─────────────────────────────────────────────────────────────

    public Optional<Cuenta> findByNumDoc(int numDoc) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_NUM_DOC)) {
            ps.setInt(1, numDoc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando cuenta numDoc=" + numDoc, e);
        }
        return Optional.empty();
    }

    /**
     * Busca la cuenta por mail (usado en el login del administrador).
     */
    public Optional<Cuenta> findByMail(String mail) {
        if (mail == null || mail.isBlank()) return Optional.empty();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_MAIL)) {
            ps.setString(1, mail.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando cuenta por mail: " + mail, e);
        }
        return Optional.empty();
    }

    public List<Cuenta> findAll() {
        List<Cuenta> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DataAccessException("Error listando cuentas", e);
        }
        return lista;
    }

    public List<Cuenta> findAllActive() {
        List<Cuenta> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_ACTIVE);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DataAccessException("Error listando cuentas activas", e);
        }
        return lista;
    }

    public boolean exists(int numDoc) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_EXISTS)) {
            ps.setInt(1, numDoc);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error verificando cuenta numDoc=" + numDoc, e);
        }
    }

    // ── Escritura ─────────────────────────────────────────────────────────────

    /**
     * Crea una cuenta nueva con el estado indicado.
     *
     * @param idRol        id del rol
     * @param numDoc       num_doc de la persona (INT)
     * @param mail         correo para login
     * @param passwordHash hash de la contraseña temporal
     * @param estado       true = activo, false = inactivo
     */
    public void insert(int idRol, int numDoc, String mail, String passwordHash, boolean estado) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_INSERT)) {
            ps.setInt    (1, idRol);
            ps.setInt    (2, numDoc);
            ps.setString (3, mail != null ? mail.trim().toLowerCase() : null);
            ps.setString (4, passwordHash);
            ps.setBoolean(5, estado);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error insertando cuenta numDoc=" + numDoc, e);
        }
    }

    /** @deprecated usar {@link #insert(int, int, String, String, boolean)} */
    @Deprecated
    public void insert(int idRol, int numDoc, String mail, String passwordHash) {
        insert(idRol, numDoc, mail, passwordHash, true);
    }

    public void updateEstado(int numDoc, boolean estado) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_UPDATE_ESTADO)) {
            ps.setBoolean(1, estado);
            ps.setInt    (2, numDoc);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error actualizando estado cuenta numDoc=" + numDoc, e);
        }
    }

    public void updatePassword(int numDoc, String newHash) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_UPDATE_PASSWORD)) {
            ps.setString(1, newHash);
            ps.setInt   (2, numDoc);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error actualizando contraseña numDoc=" + numDoc, e);
        }
    }

    // ── Mapeo ─────────────────────────────────────────────────────────────────

    private Cuenta mapRow(ResultSet rs) throws SQLException {
        Cuenta c = new Cuenta();
        c.setIdCuenta    (rs.getInt    ("id_cuenta"));
        c.setIdRol       (rs.getInt    ("id_rol"));
        c.setNumDoc      (rs.getInt    ("num_doc"));
        c.setMail        (rs.getString ("mail"));
        c.setPasswordHash(rs.getString ("password_hash"));
        c.setEstado      (rs.getBoolean("estado"));

        Timestamp ts = rs.getTimestamp("fecha_creacion");
        if (ts != null) c.setFechaCreacion(ts.toLocalDateTime());

        Rol rol = new Rol(rs.getInt("id_rol"), rs.getString("nom_rol"));
        c.setRol(rol);

        Persona p = new Persona();
        p.setNumDoc (rs.getInt   ("num_doc"));
        p.setNombres(rs.getString("nombres"));
        p.setPApe   (rs.getString("p_ape"));
        p.setSApe   (rs.getString("s_ape"));
        p.setEmail  (rs.getString("email"));
        c.setPersona(p);

        return c;
    }
}
