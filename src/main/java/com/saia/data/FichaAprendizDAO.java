package com.saia.data;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.saia.db.ConnectionPool;
import com.saia.model.FichaAprendiz;
import com.saia.model.FichaAprendiz.Estado;

/**
 * DAO para gestionar la relación entre fichas y aprendices.
 * 
 * <p>Permite asignar aprendices a fichas con validación del límite de 30.</p>
 * 
 * @author SAIA Team
 * @version 1.0
 */
public class FichaAprendizDAO {
    
    // ── Consultas SQL ─────────────────────────────────────────────────────────
    
    private static final String SQL_SELECT_BY_FICHA =
            "SELECT * FROM vista_ficha_aprendices WHERE id_ficha = ? ORDER BY apellidos, nombres";
    
    private static final String SQL_SELECT_BY_CUENTA =
            "SELECT * FROM vista_ficha_aprendices WHERE id_cuenta = ? AND estado_en_ficha = 'ACTIVO'";
    
    private static final String SQL_UPDATE_ESTADO =
            "UPDATE ficha_aprendiz SET estado = ? WHERE id_ficha_aprendiz = ?";
    
    private static final String SQL_COUNT_ACTIVOS_EN_FICHA =
            "SELECT COUNT(*) FROM ficha_aprendiz WHERE id_ficha = ? AND estado = 'ACTIVO'";
    
    private static final String SQL_APRENDICES_SIN_FICHA =
            "SELECT c.id_cuenta, p.num_doc, p.nombres, " +
            "       CONCAT(p.p_ape, IFNULL(CONCAT(' ', p.s_ape), '')) AS apellidos, " +
            "       p.email " +
            "FROM cuenta c " +
            "INNER JOIN persona p ON c.num_doc = p.num_doc " +
            "WHERE c.id_rol = 1 " +  // Aprendices
            "  AND c.estado = TRUE " +  // Activos
            "  AND NOT EXISTS ( " +
            "      SELECT 1 FROM ficha_aprendiz fa " +
            "      WHERE fa.id_cuenta = c.id_cuenta AND fa.estado = 'ACTIVO' " +
            "  ) " +
            "ORDER BY p.p_ape, p.nombres";
    
    private static final String SQL_FICHAS_CON_CUPOS =
            "SELECT f.id_ficha, f.nom_ficha, f.jornada, " +
            "       pf.nom_prog AS programa, " +
            "       COUNT(fa.id_cuenta) AS total_aprendices, " +
            "       30 - COUNT(fa.id_cuenta) AS cupos_disponibles " +
            "FROM ficha f " +
            "LEFT JOIN ficha_aprendiz fa ON f.id_ficha = fa.id_ficha AND fa.estado = 'ACTIVO' " +
            "LEFT JOIN programa_formacion pf ON f.id_programa = pf.id_programa " +
            "GROUP BY f.id_ficha, f.nom_ficha, f.jornada, pf.nom_prog " +
            "HAVING cupos_disponibles > 0 " +
            "ORDER BY cupos_disponibles DESC, f.nom_ficha";
    
    // ── Métodos de consulta ───────────────────────────────────────────────────
    
    /**
     * Obtiene todos los aprendices asignados a una ficha.
     * 
     * @param idFicha ID de la ficha
     * @return Lista de relaciones ficha-aprendiz
     */
    public List<FichaAprendiz> findByFicha(int idFicha) {
        List<FichaAprendiz> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_SELECT_BY_FICHA)) {
            ps.setInt(1, idFicha);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error obteniendo aprendices de ficha " + idFicha, e);
        }
        return lista;
    }
    
    /**
     * Obtiene la ficha activa de un aprendiz.
     * 
     * @param idCuenta ID de la cuenta del aprendiz
     * @return FichaAprendiz o null si no está asignado
     */
    public FichaAprendiz findByCuenta(int idCuenta) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_SELECT_BY_CUENTA)) {
            ps.setInt(1, idCuenta);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error obteniendo ficha de cuenta " + idCuenta, e);
        }
        return null;
    }
    
    /**
     * Cuenta cuántos aprendices activos tiene una ficha.
     * 
     * @param idFicha ID de la ficha
     * @return Número de aprendices activos
     */
    public int countActivosEnFicha(int idFicha) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_COUNT_ACTIVOS_EN_FICHA)) {
            ps.setInt(1, idFicha);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error contando aprendices en ficha " + idFicha, e);
        }
        return 0;
    }
    
    /**
     * Obtiene aprendices que no están asignados a ninguna ficha.
     * 
     * @return Lista de información básica de aprendices sin ficha
     */
    public List<AprendizSinFicha> findAprendicesSinFicha() {
        List<AprendizSinFicha> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_APRENDICES_SIN_FICHA);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AprendizSinFicha asf = new AprendizSinFicha();
                asf.idCuenta = rs.getInt("id_cuenta");
                asf.numDoc = rs.getInt("num_doc");
                asf.nombres = rs.getString("nombres");
                asf.apellidos = rs.getString("apellidos");
                asf.email = rs.getString("email");
                lista.add(asf);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error obteniendo aprendices sin ficha", e);
        }
        return lista;
    }
    
    /**
     * Obtiene fichas que tienen cupos disponibles.
     * 
     * @return Lista de información de fichas con cupos
     */
    public List<FichaConCupos> findFichasConCupos() {
        List<FichaConCupos> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_FICHAS_CON_CUPOS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FichaConCupos fcc = new FichaConCupos();
                fcc.idFicha = rs.getInt("id_ficha");
                fcc.nomFicha = rs.getString("nom_ficha");
                fcc.jornada = rs.getString("jornada");
                fcc.programa = rs.getString("programa");
                fcc.totalAprendices = rs.getInt("total_aprendices");
                fcc.cuposDisponibles = rs.getInt("cupos_disponibles");
                lista.add(fcc);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error obteniendo fichas con cupos", e);
        }
        return lista;
    }
    
    // ── Métodos de escritura ──────────────────────────────────────────────────
    
    /**
     * Asigna un aprendiz a una ficha usando el procedimiento almacenado.
     * Valida automáticamente el límite de 30 aprendices.
     * 
     * @param idFicha ID de la ficha
     * @param idCuenta ID de la cuenta del aprendiz
     * @return true si se asignó correctamente
     * @throws DataAccessException si la ficha está llena o el aprendiz ya está asignado
     */
    public boolean asignarAprendiz(int idFicha, int idCuenta) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             CallableStatement cs = cn.prepareCall("{CALL sp_asignar_aprendiz_ficha(?, ?)}")) {
            cs.setInt(1, idFicha);
            cs.setInt(2, idCuenta);
            cs.execute();
            return true;
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg.contains("límite")) {
                throw new DataAccessException("La ficha ha alcanzado el límite de 30 aprendices", e);
            } else if (msg.contains("ya está asignado")) {
                throw new DataAccessException("El aprendiz ya está asignado a otra ficha", e);
            } else if (msg.contains("Solo se pueden asignar")) {
                throw new DataAccessException("Solo se pueden asignar cuentas de aprendices (rol=1)", e);
            }
            throw new DataAccessException("Error asignando aprendiz a ficha", e);
        }
    }
    
    /**
     * Retira un aprendiz de su ficha activa.
     * 
     * @param idCuenta ID de la cuenta del aprendiz
     * @return true si se retiró correctamente
     */
    public boolean retirarAprendiz(int idCuenta) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             CallableStatement cs = cn.prepareCall("{CALL sp_retirar_aprendiz_ficha(?)}")) {
            cs.setInt(1, idCuenta);
            cs.execute();
            return true;
        } catch (SQLException e) {
            throw new DataAccessException("Error retirando aprendiz", e);
        }
    }
    
    /**
     * Transfiere un aprendiz de una ficha a otra.
     * 
     * @param idCuenta ID de la cuenta del aprendiz
     * @param idFichaNueva ID de la ficha destino
     * @return true si se transfirió correctamente
     */
    public boolean transferirAprendiz(int idCuenta, int idFichaNueva) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             CallableStatement cs = cn.prepareCall("{CALL sp_transferir_aprendiz_ficha(?, ?)}")) {
            cs.setInt(1, idCuenta);
            cs.setInt(2, idFichaNueva);
            cs.execute();
            return true;
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg.contains("límite")) {
                throw new DataAccessException("La ficha destino ha alcanzado el límite de 30 aprendices", e);
            }
            throw new DataAccessException("Error transfiriendo aprendiz", e);
        }
    }
    
    /**
     * Cambia el estado de una relación ficha-aprendiz.
     * 
     * @param idFichaAprendiz ID de la relación
     * @param nuevoEstado Nuevo estado
     * @return true si se actualizó correctamente
     */
    public boolean updateEstado(int idFichaAprendiz, Estado nuevoEstado) {
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_UPDATE_ESTADO)) {
            ps.setString(1, nuevoEstado.getValue());
            ps.setInt(2, idFichaAprendiz);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error actualizando estado", e);
        }
    }
    
    // ── Mapeo ─────────────────────────────────────────────────────────────────
    
    private FichaAprendiz mapRow(ResultSet rs) throws SQLException {
        FichaAprendiz fa = new FichaAprendiz();
        fa.setIdFichaAprendiz(rs.getInt("id_ficha_aprendiz"));
        fa.setIdFicha(rs.getInt("id_ficha"));
        fa.setIdCuenta(rs.getInt("id_cuenta"));
        
        Timestamp ts = rs.getTimestamp("fecha_asignacion");
        if (ts != null) fa.setFechaAsignacion(ts.toLocalDateTime());
        
        fa.setEstado(rs.getString("estado_en_ficha"));
        
        // Información extendida
        fa.setNomFicha(rs.getString("nom_ficha"));
        fa.setJornada(rs.getString("jornada"));
        fa.setNumDoc(rs.getInt("num_doc"));
        fa.setNombres(rs.getString("nombres"));
        fa.setApellidos(rs.getString("apellidos"));
        fa.setEmail(rs.getString("email"));
        fa.setPrograma(rs.getString("programa"));
        fa.setCentro(rs.getString("centro"));
        fa.setTotalAprendicesActivos(rs.getInt("total_aprendices_activos"));
        
        return fa;
    }
    
    // ── Clases auxiliares ─────────────────────────────────────────────────────
    
    /**
     * Información básica de un aprendiz sin ficha asignada.
     */
    public static class AprendizSinFicha {
        public int idCuenta;
        public int numDoc;
        public String nombres;
        public String apellidos;
        public String email;
        
        public String getNombreCompleto() {
            return nombres + " " + apellidos;
        }
        
        @Override
        public String toString() {
            return getNombreCompleto() + " (" + numDoc + ")";
        }
    }
    
    /**
     * Información de una ficha con cupos disponibles.
     */
    public static class FichaConCupos {
        public int idFicha;
        public String nomFicha;
        public String jornada;
        public String programa;
        public int totalAprendices;
        public int cuposDisponibles;
        
        @Override
        public String toString() {
            return nomFicha + " - " + programa + " (" + cuposDisponibles + " cupos)";
        }
    }
}
