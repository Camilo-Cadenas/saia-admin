package com.saia.data;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.saia.db.ConnectionPool;
import com.saia.model.HistorialIngreso;

/**
 * DAO para la tabla {@code historial_ingreso}.
 * BD nueva: usa num_doc (no id_aprendiz) para referenciar al aprendiz.
 * estado_movimiento: 1 = Ingreso, 0 = Salida (TINYINT).
 */
public class HistorialIngresoDAO {

    private static final String SQL_BASE =
            "SELECT hi.id_ingreso, hi.num_doc, hi.id_guarda, " +
            "       hi.fecha_hora_ingreso, hi.fecha_hora_salida, " +
            "       hi.estado_movimiento, hi.observacion " +
            "FROM historial_ingreso hi ";

    private static final String SQL_FIND_BY_ID =
            SQL_BASE + "WHERE hi.id_ingreso = ?";

    private static final String SQL_FIND_BY_NUM_DOC =
            SQL_BASE + "WHERE hi.num_doc = ? ORDER BY hi.fecha_hora_ingreso DESC";

    private static final String SQL_FIND_BY_GUARDA =
            SQL_BASE + "WHERE hi.id_guarda = ? ORDER BY hi.fecha_hora_ingreso DESC";

    private static final String SQL_FIND_BY_RANGO =
            SQL_BASE +
            "WHERE DATE(hi.fecha_hora_ingreso) BETWEEN ? AND ? " +
            "ORDER BY hi.fecha_hora_ingreso DESC";

    private static final String SQL_FIND_BY_RANGO_GUARDA =
            SQL_BASE +
            "WHERE hi.id_guarda = ? " +
            "  AND DATE(hi.fecha_hora_ingreso) BETWEEN ? AND ? " +
            "ORDER BY hi.fecha_hora_ingreso DESC";

    private static final String SQL_INSERT =
            "INSERT INTO historial_ingreso " +
            "(num_doc, id_guarda, fecha_hora_ingreso, fecha_hora_salida, " +
            " estado_movimiento, observacion) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE_SALIDA =
            "UPDATE historial_ingreso SET fecha_hora_salida = ? WHERE id_ingreso = ?";

    // -------------------------------------------------------------------------

    public Optional<HistorialIngreso> findById(int id) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando historial_ingreso id=" + id, e);
        }
        return Optional.empty();
    }

    /** Busca historial por num_doc del aprendiz. */
    public List<HistorialIngreso> findByNumDoc(int numDoc) {
        List<HistorialIngreso> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_NUM_DOC)) {

            ps.setInt(1, numDoc);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando historial por numDoc=" + numDoc, e);
        }
        return lista;
    }

    public List<HistorialIngreso> findByGuarda(int idGuarda) {
        List<HistorialIngreso> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_GUARDA)) {

            ps.setInt(1, idGuarda);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando historial por guarda=" + idGuarda, e);
        }
        return lista;
    }

    public List<HistorialIngreso> findByRango(LocalDate desde, LocalDate hasta) {
        List<HistorialIngreso> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_RANGO)) {

            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando historial por rango", e);
        }
        return lista;
    }

    public List<HistorialIngreso> findByGuardaYRango(int idGuarda, LocalDate desde, LocalDate hasta) {
        List<HistorialIngreso> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_RANGO_GUARDA)) {

            ps.setInt (1, idGuarda);
            ps.setDate(2, Date.valueOf(desde));
            ps.setDate(3, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando historial por guarda y rango", e);
        }
        return lista;
    }

    public int insert(HistorialIngreso hi) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_INSERT,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt      (1, hi.getNumDoc());
            ps.setInt      (2, hi.getIdGuarda());
            ps.setTimestamp(3, hi.getFechaHoraIngreso() != null
                    ? Timestamp.valueOf(hi.getFechaHoraIngreso()) : null);
            ps.setTimestamp(4, hi.getFechaHoraSalida() != null
                    ? Timestamp.valueOf(hi.getFechaHoraSalida()) : null);
            ps.setInt      (5, hi.getEstadoMovimiento());
            ps.setString   (6, hi.getObservacion());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error insertando historial_ingreso", e);
        }
        return -1;
    }

    public void updateSalida(int idIngreso, java.time.LocalDateTime salida) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_UPDATE_SALIDA)) {

            ps.setTimestamp(1, Timestamp.valueOf(salida));
            ps.setInt      (2, idIngreso);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error actualizando salida id=" + idIngreso, e);
        }
    }

    // -------------------------------------------------------------------------
    //  Mapeo
    // -------------------------------------------------------------------------

    private HistorialIngreso mapRow(ResultSet rs) throws SQLException {
        HistorialIngreso hi = new HistorialIngreso();
        hi.setIdIngreso       (rs.getInt("id_ingreso"));
        hi.setNumDoc          (rs.getInt("num_doc"));
        hi.setIdGuarda        (rs.getInt("id_guarda"));
        Timestamp ingreso = rs.getTimestamp("fecha_hora_ingreso");
        if (ingreso != null) hi.setFechaHoraIngreso(ingreso.toLocalDateTime());
        Timestamp salida = rs.getTimestamp("fecha_hora_salida");
        if (salida != null) hi.setFechaHoraSalida(salida.toLocalDateTime());
        hi.setEstadoMovimiento(rs.getInt("estado_movimiento"));
        hi.setObservacion     (rs.getString("observacion"));
        return hi;
    }
}
