package com.saia.model;

/**
 * Entidad que representa la tabla {@code administrador}.
 * num_doc → INT (nueva BD).
 */
public class Administrador {

    private int idAdmin;   // PK generado
    private int numDoc;    // FK → persona.num_doc UNIQUE  (INT)

    private Persona persona;

    public Administrador() {}

    public int getIdAdmin() { return idAdmin; }
    public void setIdAdmin(int idAdmin) { this.idAdmin = idAdmin; }

    public int getNumDoc() { return numDoc; }
    public void setNumDoc(int numDoc) { this.numDoc = numDoc; }

    public Persona getPersona() { return persona; }
    public void setPersona(Persona persona) { this.persona = persona; }

    // ── Conveniencia ─────────────────────────────────────────────────────────

    public String getNombreCompleto() {
        return persona != null ? persona.getNombreCompleto() : "";
    }

    public String getEmail() {
        return persona != null ? persona.getEmail() : "";
    }

    // El password_hash ahora está en la tabla cuenta, no en persona.
    // Se accede a través de la cuenta del administrador.

    @Override
    public String toString() {
        return "Administrador{idAdmin=" + idAdmin + ", numDoc=" + numDoc
                + ", nombre='" + getNombreCompleto() + "'}";
    }
}
