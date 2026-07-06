package com.saia.model;

import java.time.LocalDateTime;

/**
 * Entidad que representa la tabla {@code reporte_rechazo}.
 */
public class ReporteRechazo {

    private int idReporte;
    private int idAprendiz;
    private long numDoc;              // num_doc FK → persona
    private int idGuarda;
    private String motivo;            // VARCHAR(255)
    private String descripcion;       // TEXT
    private LocalDateTime fechaReporte;

    /** Aprendiz y guardia asociados */
    private Aprendiz          aprendiz;
    private PersonalSeguridad guarda;

    public ReporteRechazo() {}

    // ---- Getters y Setters ----

    public int getIdReporte() { return idReporte; }
    public void setIdReporte(int idReporte) { this.idReporte = idReporte; }

    public int getIdAprendiz() { return idAprendiz; }
    public void setIdAprendiz(int idAprendiz) { this.idAprendiz = idAprendiz; }

    public long getNumDoc() { return numDoc; }
    public void setNumDoc(long numDoc) { this.numDoc = numDoc; }

    public int getIdGuarda() { return idGuarda; }
    public void setIdGuarda(int idGuarda) { this.idGuarda = idGuarda; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDateTime getFechaReporte() { return fechaReporte; }
    public void setFechaReporte(LocalDateTime fechaReporte) { this.fechaReporte = fechaReporte; }

    public Aprendiz getAprendiz() { return aprendiz; }
    public void setAprendiz(Aprendiz aprendiz) { this.aprendiz = aprendiz; }

    public PersonalSeguridad getGuarda() { return guarda; }
    public void setGuarda(PersonalSeguridad guarda) { this.guarda = guarda; }

    @Override
    public String toString() {
        return "ReporteRechazo{idReporte=" + idReporte
                + ", idAprendiz=" + idAprendiz + "}";
    }
}
