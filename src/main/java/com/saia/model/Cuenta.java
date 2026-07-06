package com.saia.model;

import java.time.LocalDateTime;

/**
 * Entidad que representa la tabla {@code cuenta}.
 *
 * Cambios respecto a la BD nueva:
 *  - num_doc es INT (no BIGINT)
 *  - Agrega campo mail VARCHAR(155)
 *  - Agrega campo password_hash VARCHAR(255)  ← movido desde persona
 *  - estado BOOLEAN DEFAULT TRUE
 *  - fecha_creacion TIMESTAMP DEFAULT NOW
 */
public class Cuenta {

    private int    idCuenta;
    private int    idRol;             // FK → rol
    private int    numDoc;            // FK → persona.num_doc UNIQUE  (INT)
    private String mail;              // VARCHAR(155)
    private String passwordHash;      // VARCHAR(255)  ← ahora aquí, no en persona
    private boolean estado;           // DEFAULT TRUE
    private LocalDateTime fechaCreacion;

    /** Joins opcionales */
    private Rol     rol;
    private Persona persona;

    public Cuenta() {}

    // ── Getters y Setters ────────────────────────────────────────────────────

    public int getIdCuenta() { return idCuenta; }
    public void setIdCuenta(int idCuenta) { this.idCuenta = idCuenta; }

    public int getIdRol() { return idRol; }
    public void setIdRol(int idRol) { this.idRol = idRol; }

    public int getNumDoc() { return numDoc; }
    public void setNumDoc(int numDoc) { this.numDoc = numDoc; }

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isEstado() { return estado; }
    public void setEstado(boolean estado) { this.estado = estado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

    public Persona getPersona() { return persona; }
    public void setPersona(Persona persona) { this.persona = persona; }

    @Override
    public String toString() {
        return "Cuenta{idCuenta=" + idCuenta + ", numDoc=" + numDoc
                + ", mail='" + mail + "', estado=" + estado + "}";
    }
}
