package com.saia.data;

import com.saia.db.ConnectionPool;
import com.saia.model.ReporteRechazo;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para la tabla {@code reporte_rechazo}.
 */
public class ReporteRechazoDAO {

    private static final String SQL_BASE =
            "SELECT rr.id_reporte, rr.id_aprendiz, rr.num_doc, rr.id_guarda, " +
            "       rr.motivo, rr.descripcion, rr.fecha_reporte " +
            "FROM reporte_rechazo rr ";

    private static final String SQL_FIND_BY_ID =
            SQL_BASE + "WHERE rr.id_reporte = ?";

    private static final String SQL_FIND_BY_APRENDIZ =
            SQL_BASE + "WHERE rr.id_aprendiz = ? ORDER BY rr.fecha_reporte DESC";

    private static final String SQL_FIND_BY_RANGO =
            SQL_BASE +
            "WHERE DATE(rr.fecha_reporte) BETWEEN ? AND ? " +
            "ORDER BY rr.fecha_reporte DESC";

    private static final String SQL_FIND_ALL =
            SQL_BASE + "ORDER BY rr.fecha_reporte DESC";

    private static final String SQL_INSERT =
            "INSERT INTO reporte_rechazo " +
            "(id_aprendiz, num_doc, id_guarda, motivo, descripcion, fecha_reporte) " +
            "VALUES (?, ?, ?, ?, ?, NOW())";

    // -------------------------------------------------------------------------

    public Optional<ReporteRechazo> findById(int id) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando reporte_rechazo id=" + id, e);
        }
        return Optional.empty();
    }

    public List<ReporteRechazo> findByAprendiz(int idAprendiz) {
        List<ReporteRechazo> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_APRENDIZ)) {

            ps.setInt(1, idAprendiz);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando rechazos por aprendiz=" + idAprendiz, e);
        }
        return lista;
    }

    public List<ReporteRechazo> findByRango(LocalDate desde, LocalDate hasta) {
        List<ReporteRechazo> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_RANGO)) {

            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando rechazos por rango", e);
        }
        return lista;
    }

    public List<ReporteRechazo> findAll() {
        List<ReporteRechazo> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) lista.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DataAccessException("Error listando reporte_rechazo", e);
        }
        return lista;
    }

    public int insert(ReporteRechazo r) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_INSERT,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt   (1, r.getIdAprendiz());
            ps.setLong  (2, r.getNumDoc());
            ps.setInt   (3, r.getIdGuarda());
            ps.setString(4, r.getMotivo());
            ps.setString(5, r.getDescripcion());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error insertando reporte_rechazo", e);
        }
        return -1;
    }

    // -------------------------------------------------------------------------

    private ReporteRechazo mapRow(ResultSet rs) throws SQLException {
        ReporteRechazo r = new ReporteRechazo();
        r.setIdReporte  (rs.getInt   ("id_reporte"));
        r.setIdAprendiz (rs.getInt   ("id_aprendiz"));
        r.setNumDoc     (rs.getLong  ("num_doc"));
        r.setIdGuarda   (rs.getInt   ("id_guarda"));
        r.setMotivo     (rs.getString("motivo"));
        r.setDescripcion(rs.getString("descripcion"));
        Timestamp ts = rs.getTimestamp("fecha_reporte");
        if (ts != null) r.setFechaReporte(ts.toLocalDateTime());
        return r;
    }
}
