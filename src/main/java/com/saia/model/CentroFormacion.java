package com.saia.model;

/**
 * Entidad que representa la tabla {@code centro_formacion}.
 *
 * Campos nuevos: nom_centro VARCHAR(50)
 */
public class CentroFormacion {

    private int    idCentro;
    private String nomCentro;     // VARCHAR(50)   ← nuevo
    private String siglasCentro;  // VARCHAR(20)
    private String descCentro;    // VARCHAR(255)

    public CentroFormacion() {}

    public int getIdCentro() { return idCentro; }
    public void setIdCentro(int idCentro) { this.idCentro = idCentro; }

    public String getNomCentro() { return nomCentro; }
    public void setNomCentro(String nomCentro) { this.nomCentro = nomCentro; }

    public String getSiglasCentro() { return siglasCentro; }
    public void setSiglasCentro(String siglasCentro) { this.siglasCentro = siglasCentro; }

    public String getDescCentro() { return descCentro; }
    public void setDescCentro(String descCentro) { this.descCentro = descCentro; }

    @Override
    public String toString() {
        return "CentroFormacion{idCentro=" + idCentro + ", nom='" + nomCentro + "'}";
    }
}
