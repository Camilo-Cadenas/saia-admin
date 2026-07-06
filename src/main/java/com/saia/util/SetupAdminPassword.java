package com.saia.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.saia.db.ConnectionPool;

/**
 * Utilidad de configuración inicial.
 * Actualiza el password_hash en la tabla CUENTA (nueva estructura de BD)
 * para el administrador indicado.
 *
 * Uso:
 *   Ejecutar desde el fat-jar:
 *   java -cp target/saia-admin.jar com.saia.util.SetupAdminPassword
 */
public class SetupAdminPassword {

    private static final String MAIL     = "tec.sistemas.camilo@gmail.com";
    private static final String PASSWORD = "Admin1151$";

    public static void main(String[] args) {
        System.out.println("=== SAIA - Configuración de contraseña de administrador ===");
        System.out.println("Mail    : " + MAIL);
        System.out.println("Password: " + PASSWORD.replaceAll(".", "*"));

        String hash = PasswordUtil.hashPassword(PASSWORD);
        System.out.println("Hash    : " + hash);
        System.out.println("Verifica: " + PasswordUtil.verifyPassword(PASSWORD, hash));

        try (Connection cn = ConnectionPool.getInstance().getConnection()) {

            // ── 1. Verificar que existe en tabla cuenta ────────────────────────
            String sqlCheck = "SELECT id_cuenta, num_doc, estado FROM cuenta WHERE LOWER(mail) = LOWER(?)";
            int idCuenta = -1;
            int numDoc   = -1;
            boolean existe = false;

            try (PreparedStatement ps = cn.prepareStatement(sqlCheck)) {
                ps.setString(1, MAIL);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        idCuenta = rs.getInt("id_cuenta");
                        numDoc   = rs.getInt("num_doc");
                        existe   = true;
                        System.out.println("\nCuenta encontrada: id_cuenta=" + idCuenta
                                + ", num_doc=" + numDoc
                                + ", estado=" + rs.getBoolean("estado"));
                    }
                }
            }

            if (!existe) {
                // ── 2. Buscar la persona por email si no hay cuenta ────────────
                String sqlPer = "SELECT num_doc FROM persona WHERE LOWER(email) = LOWER(?)";
                try (PreparedStatement ps = cn.prepareStatement(sqlPer)) {
                    ps.setString(1, MAIL);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            numDoc = rs.getInt("num_doc");
                            System.out.println("Persona encontrada con num_doc=" + numDoc);
                        } else {
                            System.err.println("ERROR: No existe ningún registro con mail: " + MAIL);
                            System.err.println("Verifique que el email existe en tabla persona o cuenta.");
                            System.exit(1);
                        }
                    }
                }

                // Verificar si es administrador
                String sqlAdmin = "SELECT id_admin FROM administrador WHERE num_doc = ?";
                try (PreparedStatement ps = cn.prepareStatement(sqlAdmin)) {
                    ps.setInt(1, numDoc);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            System.err.println("ERROR: num_doc=" + numDoc + " no está en tabla administrador.");
                            System.exit(1);
                        }
                    }
                }

                // Crear cuenta (id_rol=1 para Administrador)
                String sqlInsert = "INSERT INTO cuenta (id_rol, num_doc, mail, password_hash, estado) VALUES (1, ?, ?, ?, TRUE)";
                try (PreparedStatement ps = cn.prepareStatement(sqlInsert)) {
                    ps.setInt   (1, numDoc);
                    ps.setString(2, MAIL.toLowerCase());
                    ps.setString(3, hash);
                    ps.executeUpdate();
                    System.out.println("Cuenta creada para num_doc=" + numDoc + " ✓");
                }
            } else {
                // ── 3. Actualizar password_hash en cuenta existente ────────────
                String sqlUpdate = "UPDATE cuenta SET password_hash = ?, estado = TRUE WHERE id_cuenta = ?";
                try (PreparedStatement ps = cn.prepareStatement(sqlUpdate)) {
                    ps.setString(1, hash);
                    ps.setInt   (2, idCuenta);
                    int rows = ps.executeUpdate();
                    System.out.println("password_hash actualizado: " + rows + " fila(s) ✓");
                }
            }

            // ── 4. Verificar estado final ──────────────────────────────────────
            String sqlFinal =
                "SELECT c.id_cuenta, c.mail, c.estado, r.nom_rol, " +
                "       p.nombres, p.p_ape " +
                "FROM cuenta c " +
                "INNER JOIN rol r ON c.id_rol = r.id_rol " +
                "INNER JOIN persona p ON c.num_doc = p.num_doc " +
                "WHERE LOWER(c.mail) = LOWER(?)";
            try (PreparedStatement ps = cn.prepareStatement(sqlFinal)) {
                ps.setString(1, MAIL);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("\n=== Estado final ===");
                        System.out.println("Nombre : " + rs.getString("nombres") + " " + rs.getString("p_ape"));
                        System.out.println("Mail   : " + rs.getString("mail"));
                        System.out.println("Rol    : " + rs.getString("nom_rol"));
                        System.out.println("Activo : " + rs.getBoolean("estado"));
                    }
                }
            }

            System.out.println("\n✓ Puede iniciar sesión con:");
            System.out.println("  Mail    : " + MAIL);
            System.out.println("  Password: " + PASSWORD);

        } catch (SQLException e) {
            System.err.println("\nERROR de base de datos: " + e.getMessage());
            e.printStackTrace();
        } finally {
            ConnectionPool.getInstance().shutdown();
        }
    }
}
