package com.saia.model;

import java.time.LocalDateTime;

/**
 * Entidad que representa la tabla {@code reporte_rechazo}.
 * BD nueva: usa num_doc directamente (no id_aprendiz).
 */
public class ReporteRechazo {

    private int idReporte;
    private long numDoc;           // num_doc FK → persona (aprendiz)
    private int idGuarda;
    private String motivo;
    private String descripcion;
    private LocalDateTime fechaReporte;

    /** Objetos asociados opcionales */
    private Aprendiz          aprendiz;
    private PersonalSeguridad guarda;

    public ReporteRechazo() {}

    public int getIdReporte() { return idReporte; }
    public void setIdReporte(int idReporte) { this.idReporte = idReporte; }

    public long getNumDoc() { return numDoc; }
    public void setNumDoc(long numDoc) { this.numDoc = numDoc; }

    /** @deprecated La BD nueva no tiene id_aprendiz. Usar getNumDoc(). */
    @Deprecated
    public int getIdAprendiz() { return (int) numDoc; }
    /** @deprecated Usar setNumDoc() */
    @Deprecated
    public void setIdAprendiz(int idAprendiz) { this.numDoc = idAprendiz; }

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
        return "ReporteRechazo{idReporte=" + idReporte + ", numDoc=" + numDoc + "}";
    }
}
