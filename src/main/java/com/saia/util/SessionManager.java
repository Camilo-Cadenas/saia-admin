package com.saia.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.saia.model.Administrador;

/**
 * Gestor de sesión en memoria para el administrador autenticado.
 */
public final class SessionManager {

    private static volatile SessionManager instance;

    private Administrador currentAdmin;
    private LocalDateTime loginTime;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) instance = new SessionManager();
            }
        }
        return instance;
    }

    public synchronized void startSession(Administrador admin) {
        if (admin == null) throw new IllegalArgumentException("El administrador no puede ser null");
        this.currentAdmin = admin;
        this.loginTime    = LocalDateTime.now();
        System.out.println("[SessionManager] Sesión iniciada para: "
                + admin.getNombreCompleto()
                + " (" + admin.getEmail() + ")"
                + " a las " + loginTime.format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")));
    }

    public synchronized void endSession() {
        if (currentAdmin != null) {
            System.out.println("[SessionManager] Sesión cerrada para: "
                    + currentAdmin.getNombreCompleto()
                    + " | Duración: " + getSessionDurationFormatted());
        }
        this.currentAdmin = null;
        this.loginTime    = null;
    }

    public boolean isSessionActive() { return currentAdmin != null; }

    public Administrador getAdmin() { return currentAdmin; }

    public String getAdminNombre() {
        return currentAdmin != null ? currentAdmin.getNombreCompleto() : "";
    }

    public String getAdminEmail() {
        return currentAdmin != null ? currentAdmin.getEmail() : "";
    }

    public int getAdminId() {
        return currentAdmin != null ? currentAdmin.getIdAdmin() : -1;
    }

    public LocalDateTime getLoginTime() { return loginTime; }

    public long getSessionDurationMs() {
        if (loginTime == null) return 0L;
        return java.time.Duration.between(loginTime, LocalDateTime.now()).toMillis();
    }

    public String getSessionDurationFormatted() {
        long ms      = getSessionDurationMs();
        long seconds = ms / 1000;
        long hours   = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs    = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    /**
     * Recarga los datos del administrador desde la BD.
     * Llama tras editar el perfil para refrescar el nombre en el topbar.
     */
    public synchronized void refreshAdmin() {
        if (currentAdmin == null) return;
        try {
            com.saia.data.AdministradorDAO dao = new com.saia.data.AdministradorDAO();
            dao.findByNumDoc(currentAdmin.getNumDoc()).ifPresent(a -> {
                this.currentAdmin = a;
                System.out.println("[SessionManager] Perfil refrescado: " + a.getNombreCompleto());
            });
        } catch (Exception e) {
            System.err.println("[SessionManager] refreshAdmin: " + e.getMessage());
        }
    }
}
