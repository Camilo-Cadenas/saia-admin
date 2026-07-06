package com.saia.model;

/**
 * Entidad que representa la tabla {@code aprendiz}.
 * num_doc → INT (nueva BD).
 */
public class Aprendiz {

    private int    idAprendiz;
    private int    numDoc;     // FK → persona.num_doc (INT)
    private int    idFicha;    // FK → ficha

    private Persona persona;
    private Ficha   ficha;
    private boolean cuentaActiva  = true;
    private String  siglasProg    = "";   // siglas del programa de formación
    private String  siglasCentro  = "";   // siglas del centro de formación

    public Aprendiz() {}

    public int getIdAprendiz() { return idAprendiz; }
    public void setIdAprendiz(int idAprendiz) { this.idAprendiz = idAprendiz; }

    public int getNumDoc() { return numDoc; }
    public void setNumDoc(int numDoc) { this.numDoc = numDoc; }

    public int getIdFicha() { return idFicha; }
    public void setIdFicha(int idFicha) { this.idFicha = idFicha; }

    public Persona getPersona() { return persona; }
    public void setPersona(Persona persona) { this.persona = persona; }

    public Ficha getFicha() { return ficha; }
    public void setFicha(Ficha ficha) { this.ficha = ficha; }

    public boolean isCuentaActiva() { return cuentaActiva; }
    public void setCuentaActiva(boolean cuentaActiva) { this.cuentaActiva = cuentaActiva; }

    public String getSiglasProg() { return siglasProg; }
    public void setSiglasProg(String siglasProg) { this.siglasProg = siglasProg != null ? siglasProg : ""; }

    public String getSiglasCentro() { return siglasCentro; }
    public void setSiglasCentro(String siglasCentro) { this.siglasCentro = siglasCentro != null ? siglasCentro : ""; }

    public String getNombreCompleto() {
        return persona != null ? persona.getNombreCompleto() : "";
    }

    @Override
    public String toString() {
        return "Aprendiz{id=" + idAprendiz + ", numDoc=" + numDoc + "}";
    }
}
