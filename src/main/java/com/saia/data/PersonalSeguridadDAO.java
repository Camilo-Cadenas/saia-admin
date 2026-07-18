package com.saia.data;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.saia.db.ConnectionPool;
import com.saia.model.Persona;
import com.saia.model.PersonalSeguridad;

/**
 * DAO para la tabla {@code personal_seguridad}.
 * num_doc → INT (nueva BD).
 */
public class PersonalSeguridadDAO {

    private static final String SQL_BASE =
            "SELECT ps.id_guarda, ps.num_doc, ps.turno, ps.empresa_seg, " +
            "       ps.inicio_turno, ps.finalizacion_turno, " +
            "       p.tip_doc, p.nombres, p.p_ape, p.s_ape, p.tel, " +
            "       p.tip_sang, p.genero, p.fecha_nac, p.email, " +
            "       COALESCE(c.estado, TRUE) AS cuenta_activa " +
            "FROM personal_seguridad ps " +
            "INNER JOIN persona p ON ps.num_doc = p.num_doc " +
            "LEFT  JOIN cuenta  c ON ps.num_doc = c.num_doc ";

    private static final String SQL_FIND_ALL        = SQL_BASE + "ORDER BY p.p_ape, p.nombres";
    private static final String SQL_FIND_BY_ID      = SQL_BASE + "WHERE ps.id_guarda = ?";
    private static final String SQL_FIND_BY_NUM_DOC = SQL_BASE + "WHERE ps.num_doc = ?";

    // INSERT completo — turno y empresa_seg son opcionales (pueden ser NULL)
    private static final String SQL_INSERT =
            "INSERT INTO personal_seguridad (num_doc, turno, empresa_seg) VALUES (?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE personal_seguridad SET turno = ?, empresa_seg = ? WHERE num_doc = ?";

    private static final String SQL_DELETE =
            "DELETE FROM personal_seguridad WHERE num_doc = ?";

    private static final String SQL_EXISTS =
            "SELECT COUNT(*) FROM personal_seguridad WHERE num_doc = ?";

    // ── Consultas ─────────────────────────────────────────────────────────────

    public List<PersonalSeguridad> findAll() {
        List<PersonalSeguridad> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DataAccessException("Error listando personal de seguridad", e);
        }
        return lista;
    }

    public Optional<PersonalSeguridad> findById(int idGuarda) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, idGuarda);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando guardia id=" + idGuarda, e);
        }
        return Optional.empty();
    }

    public Optional<PersonalSeguridad> findByNumDoc(int numDoc) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_NUM_DOC)) {
            ps.setInt(1, numDoc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando guardia numDoc=" + numDoc, e);
        }
        return Optional.empty();
    }

    public boolean exists(int numDoc) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_EXISTS)) {
            ps.setInt(1, numDoc);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error verificando guardia numDoc=" + numDoc, e);
        }
    }

    // ── Escritura ─────────────────────────────────────────────────────────────

    /** Inserta num_doc con turno y empresa opcionales (pueden ser null). */
    public void insert(int numDoc, String turno, String empresaSeg) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_INSERT)) {
            ps.setInt   (1, numDoc);
            ps.setString(2, (turno      != null && !turno.isBlank())      ? turno      : null);
            ps.setString(3, (empresaSeg != null && !empresaSeg.isBlank()) ? empresaSeg : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error insertando personal_seguridad numDoc=" + numDoc, e);
        }
    }

    /** @deprecated usar {@link #insert(int, String, String)} */
    @Deprecated
    public void insert(int numDoc) {
        insert(numDoc, null, null);
    }

    public void update(PersonalSeguridad g) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, g.getTurno());
            ps.setString(2, g.getEmpresaSeg());
            ps.setInt   (3, g.getNumDoc());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error actualizando guardia numDoc=" + g.getNumDoc(), e);
        }
    }

    public void delete(int numDoc) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, numDoc);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error eliminando guardia numDoc=" + numDoc, e);
        }
    }

    // ── Mapeo ─────────────────────────────────────────────────────────────────

    private PersonalSeguridad mapRow(ResultSet rs) throws SQLException {
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

        PersonalSeguridad g = new PersonalSeguridad();
        g.setIdGuarda  (rs.getInt   ("id_guarda"));
        g.setNumDoc    (rs.getInt   ("num_doc"));
        g.setTurno     (rs.getString("turno"));
        g.setEmpresaSeg(rs.getString("empresa_seg"));

        Timestamp ini = rs.getTimestamp("inicio_turno");
        if (ini != null) g.setInicioTurno(ini.toLocalDateTime());
        Timestamp fin = rs.getTimestamp("finalizacion_turno");
        if (fin != null) g.setFinalizacionTurno(fin.toLocalDateTime());

        g.setPersona   (p);
        g.setCuentaActiva(rs.getBoolean("cuenta_activa"));
        return g;
    }
}
