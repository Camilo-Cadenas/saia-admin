package com.saia.model;

/**
 * Entidad que representa la tabla {@code programa_formacion}.
 */
public class ProgramaFormacion {

    private int idPrograma;
    private int idCentro;         // FK → centro_formacion
    private String siglasProg;    // VARCHAR(20)
    private String nomProg;       // VARCHAR(100)
    private String descProg;      // VARCHAR(255)

    /** Centro asociado (join opcional) */
    private CentroFormacion centro;

    public ProgramaFormacion() {}

    // ---- Getters y Setters ----

    public int getIdPrograma() { return idPrograma; }
    public void setIdPrograma(int idPrograma) { this.idPrograma = idPrograma; }

    public int getIdCentro() { return idCentro; }
    public void setIdCentro(int idCentro) { this.idCentro = idCentro; }

    public String getSiglasProg() { return siglasProg; }
    public void setSiglasProg(String siglasProg) { this.siglasProg = siglasProg; }

    public String getNomProg() { return nomProg; }
    public void setNomProg(String nomProg) { this.nomProg = nomProg; }

    public String getDescProg() { return descProg; }
    public void setDescProg(String descProg) { this.descProg = descProg; }

    public CentroFormacion getCentro() { return centro; }
    public void setCentro(CentroFormacion centro) { this.centro = centro; }

    @Override
    public String toString() {
        return "ProgramaFormacion{idPrograma=" + idPrograma
                + ", nomProg='" + nomProg + "'}";
    }
}
