package com.saia.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilidad para manejo de contraseñas usando SHA-256 con salt fijo.
 *
 * Para crear un administrador directamente en MySQL usar la función equivalente:
 *
 *   SHA2(CONCAT('SAIA_SENA_2025_SALT', 'tuPassword', 'SAIA_SENA_2025_SALT'), 256)
 *
 * Ejemplo para la contraseña 'Admin1151$':
 *   UPDATE cuenta
 *   SET password_hash = SHA2(CONCAT('SAIA_SENA_2025_SALT','Admin1151$','SAIA_SENA_2025_SALT'), 256)
 *   WHERE mail = 'correo@ejemplo.com';
 */
public class PasswordUtil {

    /** Salt fijo aplicado antes y después de la contraseña. */
    public static final String SALT = "SAIA_SENA_2025_SALT";

    private PasswordUtil() {}

    /**
     * Genera el hash SHA-256 con salt.
     * Equivalente MySQL: SHA2(CONCAT(SALT, password, SALT), 256)
     *
     * @param plainPassword contraseña en texto plano
     * @return hash hexadecimal en minúsculas (64 caracteres)
     */
    public static String hashPassword(String plainPassword) {
        try {
            String salted = SALT + plainPassword + SALT;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(salted.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al calcular hash SHA-256", e);
        }
    }

    /**
     * Verifica si una contraseña coincide con su hash almacenado.
     *
     * @param plainPassword contraseña en texto plano
     * @param storedHash    hash almacenado en BD (64 chars hex)
     * @return true si coinciden
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) return false;
        return hashPassword(plainPassword).equalsIgnoreCase(storedHash.trim());
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
