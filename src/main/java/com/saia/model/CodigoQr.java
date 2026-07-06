package com.saia.model;

import java.time.LocalDateTime;

/**
 * Entidad que representa la tabla {@code codigo_qr}.
 */
public class CodigoQr {

    private int idQr;
    private int idAprendiz;          // FK → aprendiz UNIQUE
    private String codigo;            // VARCHAR(255) UNIQUE
    private boolean estadoQr;
    private LocalDateTime fechaGeneracion;

    /** Aprendiz asociado */
    private Aprendiz aprendiz;

    public CodigoQr() {}

    // ---- Getters y Setters ----

    public int getIdQr() { return idQr; }
    public void setIdQr(int idQr) { this.idQr = idQr; }

    public int getIdAprendiz() { return idAprendiz; }
    public void setIdAprendiz(int idAprendiz) { this.idAprendiz = idAprendiz; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public boolean isEstadoQr() { return estadoQr; }
    public void setEstadoQr(boolean estadoQr) { this.estadoQr = estadoQr; }

    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }

    public Aprendiz getAprendiz() { return aprendiz; }
    public void setAprendiz(Aprendiz aprendiz) { this.aprendiz = aprendiz; }

    @Override
    public String toString() {
        return "CodigoQr{idQr=" + idQr
                + ", idAprendiz=" + idAprendiz
                + ", estadoQr=" + estadoQr + "}";
    }
}
