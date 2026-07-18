package com.saia.model;

import java.time.LocalDate;

/**
 * POJO con todos los filtros seleccionados en el generador de reportes.
 */
public class FiltrosReporte {

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String    programa;       // null = todos
    private String    estadoAprendiz; // null = todos
    private String    sedeCentro;     // null = todos
    private String    jornada;        // null = todos
    private String    guarda;         // null = todos

    public FiltrosReporte() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate v) { this.fechaInicio = v; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate v) { this.fechaFin = v; }

    public String getPrograma() { return programa; }
    public void setPrograma(String v) { this.programa = v; }

    public String getEstadoAprendiz() { return estadoAprendiz; }
    public void setEstadoAprendiz(String v) { this.estadoAprendiz = v; }

    public String getSedeCentro() { return sedeCentro; }
    public void setSedeCentro(String v) { this.sedeCentro = v; }

    public String getJornada() { return jornada; }
    public void setJornada(String v) { this.jornada = v; }

    public String getGuarda() { return guarda; }
    public void setGuarda(String v) { this.guarda = v; }

    /** Valida que fechaInicio <= fechaFin (ambas pueden ser null). */
    public boolean isFechasValidas() {
        if (fechaInicio == null || fechaFin == null) return true;
        return !fechaInicio.isAfter(fechaFin);
    }

    @Override
    public String toString() {
        return "FiltrosReporte{" +
               "desde=" + fechaInicio +
               ", hasta=" + fechaFin +
               ", programa='" + programa + '\'' +
               ", estado='" + estadoAprendiz + '\'' +
               '}';
    }
}
