package com.saia.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.saia.db.ConnectionPool;

/**
 * DAO para consultar usuarios bloqueados (estado = 0 en tabla cuenta).
 * Unifica aprendices y personal de seguridad en una sola lista.
 */
public class UsuarioBloqueadoDAO {

    /**
     * Modelo ligero que representa un usuario bloqueado.
     */
    public static class UsuarioBloqueado {
        public final int           numDoc;
        public final String        tipDoc;
        public final String        nombres;
        public final String        pApe;
        public final String        sApe;
        public final String        email;
        public final String        tel;
        public final String        rol;      // "Personal de Seguridad" | "Aprendiz"
        public final LocalDateTime fechaCreacion;

        public UsuarioBloqueado(int numDoc, String tipDoc, String nombres, String pApe,
                                String sApe, String email, String tel, String rol,
                                LocalDateTime fechaCreacion) {
            this.numDoc       = numDoc;
            this.tipDoc       = tipDoc;
            this.nombres      = nombres != null ? nombres : "";
            this.pApe         = pApe    != null ? pApe    : "";
            this.sApe         = sApe    != null ? sApe    : "";
            this.email        = email   != null ? email   : "";
            this.tel          = tel     != null ? tel     : "";
            this.rol          = rol;
            this.fechaCreacion = fechaCreacion;
        }

        public String getNombreCompleto() {
            String n = nombres + " " + pApe;
            if (sApe != null && !sApe.isBlank()) n += " " + sApe;
            return n.trim();
        }
    }

    // ── Consulta principal ────────────────────────────────────────────────────

    private static final String SQL_BLOQUEADOS =
            "SELECT p.num_doc, p.tip_doc, p.nombres, p.p_ape, p.s_ape, " +
            "       p.email, p.tel, r.nom_rol, c.fecha_creacion " +
            "FROM cuenta c " +
            "INNER JOIN persona p ON c.num_doc = p.num_doc " +
            "INNER JOIN rol     r ON c.id_rol  = r.id_rol " +
            "WHERE c.estado = FALSE " +
            "  AND r.nom_rol IN ('Guarda', 'Aprendiz') " +
            "ORDER BY r.nom_rol, p.p_ape, p.nombres";

    /**
     * Retorna todos los usuarios bloqueados (aprendices + personal de seguridad).
     */
    public List<UsuarioBloqueado> findAllBloqueados() {
        List<UsuarioBloqueado> lista = new ArrayList<>();
        try (Connection cn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL_BLOQUEADOS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[UsuarioBloqueadoDAO] Error: " + e.getMessage());
        }
        return lista;
    }

    private UsuarioBloqueado mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("fecha_creacion");
        return new UsuarioBloqueado(
            rs.getInt   ("num_doc"),
            rs.getString("tip_doc"),
            rs.getString("nombres"),
            rs.getString("p_ape"),
            rs.getString("s_ape"),
            rs.getString("email"),
            rs.getString("tel"),
            rs.getString("nom_rol"),
            ts != null ? ts.toLocalDateTime() : null
        );
    }
}
