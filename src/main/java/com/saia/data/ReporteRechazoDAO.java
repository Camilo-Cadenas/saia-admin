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
import com.saia.model.ReporteRechazo;

/**
 * DAO para la tabla {@code reporte_rechazo}.
 * BD nueva: la tabla usa num_doc (no id_aprendiz) para referenciar al aprendiz.
 */
public class ReporteRechazoDAO {

    private static final String SQL_BASE =
            "SELECT rr.id_reporte, rr.num_doc, rr.id_guarda, " +
            "       rr.motivo, rr.descripcion, rr.fecha_reporte " +
            "FROM reporte_rechazo rr ";

    private static final String SQL_FIND_BY_ID =
            SQL_BASE + "WHERE rr.id_reporte = ?";

    private static final String SQL_FIND_BY_NUM_DOC =
            SQL_BASE + "WHERE rr.num_doc = ? ORDER BY rr.fecha_reporte DESC";

    private static final String SQL_FIND_BY_RANGO =
            SQL_BASE +
            "WHERE DATE(rr.fecha_reporte) BETWEEN ? AND ? " +
            "ORDER BY rr.fecha_reporte DESC";

    private static final String SQL_FIND_ALL =
            SQL_BASE + "ORDER BY rr.fecha_reporte DESC";

    private static final String SQL_INSERT =
            "INSERT INTO reporte_rechazo " +
            "(num_doc, id_guarda, motivo, descripcion) " +
            "VALUES (?, ?, ?, ?)";

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

    /** Busca reportes por num_doc del aprendiz. */
    public List<ReporteRechazo> findByNumDoc(int numDoc) {
        List<ReporteRechazo> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_NUM_DOC)) {

            ps.setInt(1, numDoc);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error buscando rechazos por numDoc=" + numDoc, e);
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

            ps.setLong  (1, r.getNumDoc());
            ps.setInt   (2, r.getIdGuarda());
            ps.setString(3, r.getMotivo());
            ps.setString(4, r.getDescripcion());
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
        r.setNumDoc     (rs.getLong  ("num_doc"));
        r.setIdGuarda   (rs.getInt   ("id_guarda"));
        r.setMotivo     (rs.getString("motivo"));
        r.setDescripcion(rs.getString("descripcion"));
        Timestamp ts = rs.getTimestamp("fecha_reporte");
        if (ts != null) r.setFechaReporte(ts.toLocalDateTime());
        return r;
    }
}
