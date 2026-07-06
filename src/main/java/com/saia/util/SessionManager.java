package com.saia.util;

import com.saia.model.Administrador;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gestor de sesión en memoria para el administrador autenticado.
 *
 * <p>Singleton thread-safe. Mantiene la sesión activa hasta que:
 * <ul>
 *   <li>El usuario hace clic en "Cerrar sesión".</li>
 *   <li>La aplicación se cierra (el shutdown hook llama a {@link #endSession()}).</li>
 * </ul>
 *
 * <p>Uso típico:
 * <pre>
 *   // Al hacer login exitoso:
 *   SessionManager.getInstance().startSession(admin);
 *
 *   // Para leer el nombre del admin logueado en cualquier parte:
 *   String nombre = SessionManager.getInstance().getAdmin().getNombreCompleto();
 *
 *   // Al cerrar sesión:
 *   SessionManager.getInstance().endSession();
 * </pre>
 */
public final class SessionManager {

    private static volatile SessionManager instance;

    private Administrador currentAdmin;
    private LocalDateTime loginTime;

    private SessionManager() {}

    // -------------------------------------------------------------------------
    //  Singleton
    // -------------------------------------------------------------------------

    /**
     * Retorna la instancia única del SessionManager.
     * Implementación double-checked locking para thread safety.
     */
    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) instance = new SessionManager();
            }
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    //  Ciclo de vida de la sesión
    // -------------------------------------------------------------------------

    /**
     * Inicia una sesión para el administrador autenticado.
     *
     * @param admin administrador que acaba de autenticarse
     */
    public synchronized void startSession(Administrador admin) {
        if (admin == null) throw new IllegalArgumentException("El administrador no puede ser null");
        this.currentAdmin = admin;
        this.loginTime    = LocalDateTime.now();
        System.out.println("[SessionManager] Sesión iniciada para: "
                + admin.getNombreCompleto()
                + " (" + admin.getEmail() + ")"
                + " a las " + loginTime.format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")));
    }

    /**
     * Cierra la sesión actual, eliminando los datos del usuario en memoria.
     */
    public synchronized void endSession() {
        if (currentAdmin != null) {
            System.out.println("[SessionManager] Sesión cerrada para: "
                    + currentAdmin.getNombreCompleto()
                    + " | Duración: " + getSessionDurationFormatted());
        }
        this.currentAdmin = null;
        this.loginTime    = null;
    }

    // -------------------------------------------------------------------------
    //  Consultas de estado
    // -------------------------------------------------------------------------

    /**
     * Verifica si hay una sesión activa.
     *
     * @return true si hay un administrador autenticado
     */
    public boolean isSessionActive() {
        return currentAdmin != null;
    }

    /**
     * Retorna el administrador actualmente autenticado.
     *
     * @return administrador en sesión, o {@code null} si no hay sesión
     */
    public Administrador getAdmin() {
        return currentAdmin;
    }

    /**
     * Retorna el nombre completo del administrador en sesión.
     *
     * @return nombre completo, o cadena vacía si no hay sesión
     */
    public String getAdminNombre() {
        return currentAdmin != null ? currentAdmin.getNombreCompleto() : "";
    }

    /**
     * Retorna el email del administrador en sesión.
     *
     * @return email, o cadena vacía si no hay sesión
     */
    public String getAdminEmail() {
        return currentAdmin != null ? currentAdmin.getEmail() : "";
    }

    /**
     * Retorna el id_admin del administrador en sesión.
     *
     * @return id_admin, o -1 si no hay sesión
     */
    public int getAdminId() {
        return currentAdmin != null ? currentAdmin.getIdAdmin() : -1;
    }

    /**
     * Retorna el momento en que se inició la sesión.
     *
     * @return LocalDateTime del login, o null si no hay sesión
     */
    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    /**
     * Retorna la duración de la sesión actual en milisegundos.
     *
     * @return milisegundos desde el inicio de sesión, o 0 si no hay sesión
     */
    public long getSessionDurationMs() {
        if (loginTime == null) return 0L;
        return java.time.Duration.between(loginTime, LocalDateTime.now()).toMillis();
    }

    /**
     * Retorna la duración de la sesión en formato legible "HH:mm:ss".
     */
    public String getSessionDurationFormatted() {
        long ms = getSessionDurationMs();
        long seconds = ms / 1000;
        long hours   = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs    = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
}
