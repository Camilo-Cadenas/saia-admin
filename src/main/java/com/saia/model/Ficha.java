package com.saia.model;

import java.time.LocalDate;

/**
 * Entidad que representa la tabla {@code ficha}.
 */
public class Ficha {

    private int idFicha;
    private int idPrograma;       // FK → programa_formacion
    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    /** Programa asociado (join opcional) */
    private ProgramaFormacion programa;

    public Ficha() {}

    // ---- Getters y Setters ----

    public int getIdFicha() { return idFicha; }
    public void setIdFicha(int idFicha) { this.idFicha = idFicha; }

    public int getIdPrograma() { return idPrograma; }
    public void setIdPrograma(int idPrograma) { this.idPrograma = idPrograma; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public ProgramaFormacion getPrograma() { return programa; }
    public void setPrograma(ProgramaFormacion programa) { this.programa = programa; }

    @Override
    public String toString() {
        return "Ficha{idFicha=" + idFicha
                + ", idPrograma=" + idPrograma + "}";
    }
}
