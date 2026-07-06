package com.saia.business;

import java.util.Optional;

import com.saia.data.AdministradorDAO;
import com.saia.data.CuentaDAO;
import com.saia.data.DataAccessException;
import com.saia.model.Administrador;
import com.saia.model.Cuenta;
import com.saia.util.PasswordUtil;
import com.saia.util.SessionManager;

/**
 * Servicio de autenticación.
 *
 * Flujo con la nueva estructura de BD:
 *  1. Validar formato email/contraseña.
 *  2. Buscar la cuenta por mail (tabla cuenta).
 *  3. Verificar estado de la cuenta (activa/bloqueada).
 *  4. Verificar password_hash (tabla cuenta).
 *  5. Verificar que el num_doc pertenece a un administrador.
 *  6. Iniciar sesión en SessionManager.
 */
public class AuthService {

    private final CuentaDAO        cuentaDAO;
    private final AdministradorDAO adminDAO;

    public AuthService() {
        this.cuentaDAO = new CuentaDAO();
        this.adminDAO  = new AdministradorDAO();
    }

    // ── Enum de resultados ────────────────────────────────────────────────────

    public enum AuthResult {
        SUCCESS,
        EMPTY_FIELDS,
        INVALID_EMAIL_FORMAT,
        PASSWORD_TOO_SHORT,
        INVALID_CREDENTIALS,  // mail no existe O contraseña incorrecta
        ACCOUNT_BLOCKED,      // cuenta existe pero estado = false
        NOT_ADMIN,            // la cuenta existe pero no es administrador
        DB_ERROR
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    public AuthResult login(String mail, String password) {

        // 1. Validaciones básicas
        if (isBlank(mail) || isBlank(password)) return AuthResult.EMPTY_FIELDS;
        if (!isValidEmail(mail))                return AuthResult.INVALID_EMAIL_FORMAT;
        if (password.length() < 6)              return AuthResult.PASSWORD_TOO_SHORT;

        String mailNorm = mail.trim().toLowerCase();

        try {
            // 2. Buscar cuenta por mail
            Optional<Cuenta> cuentaOpt = cuentaDAO.findByMail(mailNorm);
            if (cuentaOpt.isEmpty()) {
                System.err.println("[AuthService] Mail no encontrado en cuenta: " + mailNorm);
                return AuthResult.INVALID_CREDENTIALS;
            }

            Cuenta cuenta = cuentaOpt.get();

            // 3. Verificar estado
            if (!cuenta.isEstado()) {
                System.err.println("[AuthService] Cuenta bloqueada: " + mailNorm);
                return AuthResult.ACCOUNT_BLOCKED;
            }

            // 4. Verificar contraseña
            String storedHash = cuenta.getPasswordHash();
            if (storedHash == null || storedHash.isBlank()) {
                System.err.println("[AuthService] password_hash vacío en cuenta para: " + mailNorm);
                return AuthResult.INVALID_CREDENTIALS;
            }

            if (!PasswordUtil.verifyPassword(password, storedHash)) {
                System.err.println("[AuthService] Contraseña incorrecta para: " + mailNorm);
                return AuthResult.INVALID_CREDENTIALS;
            }

            // 5. Verificar que es administrador
            Optional<Administrador> adminOpt = adminDAO.findByNumDoc(cuenta.getNumDoc());
            if (adminOpt.isEmpty()) {
                System.err.println("[AuthService] num_doc=" + cuenta.getNumDoc()
                        + " no está en tabla administrador.");
                return AuthResult.NOT_ADMIN;
            }

            Administrador admin = adminOpt.get();
            System.out.println("[AuthService] Login exitoso: " + admin.getNombreCompleto()
                    + " (num_doc=" + admin.getNumDoc() + ")");

            // 6. Iniciar sesión
            SessionManager.getInstance().startSession(admin);
            return AuthResult.SUCCESS;

        } catch (DataAccessException e) {
            System.err.println("[AuthService] Error BD: " + e.getMessage());
            e.printStackTrace();
            return AuthResult.DB_ERROR;
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    public void logout() {
        SessionManager.getInstance().endSession();
    }

    // ── Mensajes ─────────────────────────────────────────────────────────────

    public String getMessageForResult(AuthResult result) {
        return switch (result) {
            case SUCCESS              -> "Autenticación exitosa.";
            case EMPTY_FIELDS         -> "Por favor completa todos los campos.";
            case INVALID_EMAIL_FORMAT -> "El formato del correo electrónico no es válido.";
            case PASSWORD_TOO_SHORT   -> "La contraseña debe tener al menos 6 caracteres.";
            case INVALID_CREDENTIALS  -> "Correo electrónico o contraseña incorrectos.";
            case ACCOUNT_BLOCKED      -> "Tu cuenta está bloqueada. Contacta al administrador.";
            case NOT_ADMIN            -> "Acceso denegado. Esta plataforma es solo para administradores.";
            case DB_ERROR             -> "No se pudo conectar a la base de datos.";
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isValidEmail(String email) {
        return email != null &&
               email.trim().matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
