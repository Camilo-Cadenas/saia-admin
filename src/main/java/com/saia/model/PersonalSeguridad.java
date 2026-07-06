package com.saia.model;

/**
 * Entidad que representa la tabla {@code personal_seguridad}.
 * num_doc → INT (nueva BD).
 */
public class PersonalSeguridad {

    private int    idGuarda;
    private int    numDoc;
    private String turno;
    private String empresaSeg;
    private Persona persona;
    private boolean cuentaActiva = true; // estado leído de tabla cuenta

    public PersonalSeguridad() {}

    public int getIdGuarda() { return idGuarda; }
    public void setIdGuarda(int idGuarda) { this.idGuarda = idGuarda; }

    public int getNumDoc() { return numDoc; }
    public void setNumDoc(int numDoc) { this.numDoc = numDoc; }

    public String getTurno() { return turno; }
    public void setTurno(String turno) { this.turno = turno; }

    public String getEmpresaSeg() { return empresaSeg; }
    public void setEmpresaSeg(String empresaSeg) { this.empresaSeg = empresaSeg; }

    public Persona getPersona() { return persona; }
    public void setPersona(Persona persona) { this.persona = persona; }

    public boolean isCuentaActiva() { return cuentaActiva; }
    public void setCuentaActiva(boolean cuentaActiva) { this.cuentaActiva = cuentaActiva; }

    public String getNombreCompleto() {
        return persona != null ? persona.getNombreCompleto() : "";
    }

    @Override
    public String toString() {
        return "PersonalSeguridad{idGuarda=" + idGuarda + ", numDoc=" + numDoc
                + ", turno='" + turno + "'}";
    }
}
