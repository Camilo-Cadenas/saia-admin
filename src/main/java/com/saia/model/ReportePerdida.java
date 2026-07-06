package com.saia.model;

import java.time.LocalDateTime;

/**
 * Entidad que representa la tabla {@code reporte_perdida}.
 */
public class ReportePerdida {

    private int idReporte;
    private int idAprendiz;      // FK → aprendiz
    private int idInsumo;        // FK → insumo
    private String descripcion;  // TEXT
    private LocalDateTime fechaReporte;

    /** Aprendiz e insumo asociados */
    private Aprendiz aprendiz;
    private Insumo   insumo;

    public ReportePerdida() {}

    // ---- Getters y Setters ----

    public int getIdReporte() { return idReporte; }
    public void setIdReporte(int idReporte) { this.idReporte = idReporte; }

    public int getIdAprendiz() { return idAprendiz; }
    public void setIdAprendiz(int idAprendiz) { this.idAprendiz = idAprendiz; }

    public int getIdInsumo() { return idInsumo; }
    public void setIdInsumo(int idInsumo) { this.idInsumo = idInsumo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDateTime getFechaReporte() { return fechaReporte; }
    public void setFechaReporte(LocalDateTime fechaReporte) { this.fechaReporte = fechaReporte; }

    public Aprendiz getAprendiz() { return aprendiz; }
    public void setAprendiz(Aprendiz aprendiz) { this.aprendiz = aprendiz; }

    public Insumo getInsumo() { return insumo; }
    public void setInsumo(Insumo insumo) { this.insumo = insumo; }

    @Override
    public String toString() {
        return "ReportePerdida{idReporte=" + idReporte
                + ", idAprendiz=" + idAprendiz + "}";
    }
}
