package com.saia.model;

import java.time.LocalDateTime;

/**
 * Entidad que representa la tabla {@code insumo}.
 */
public class Insumo {

    private int idInsumo;
    private int idAprendiz;         // FK → aprendiz
    private String nomInsumo;       // VARCHAR(100)
    private boolean estado;
    private String marca;           // VARCHAR(100)
    private String numSerie;        // VARCHAR(100) UNIQUE
    private String descInsumo;      // TEXT
    private LocalDateTime fechaRegistro;

    /** Imágenes asociadas (cargadas bajo demanda) */
    private java.util.List<ImagenInsumo> imagenes;

    public Insumo() {}

    // ---- Getters y Setters ----

    public int getIdInsumo() { return idInsumo; }
    public void setIdInsumo(int idInsumo) { this.idInsumo = idInsumo; }

    public int getIdAprendiz() { return idAprendiz; }
    public void setIdAprendiz(int idAprendiz) { this.idAprendiz = idAprendiz; }

    public String getNomInsumo() { return nomInsumo; }
    public void setNomInsumo(String nomInsumo) { this.nomInsumo = nomInsumo; }

    public boolean isEstado() { return estado; }
    public void setEstado(boolean estado) { this.estado = estado; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getNumSerie() { return numSerie; }
    public void setNumSerie(String numSerie) { this.numSerie = numSerie; }

    public String getDescInsumo() { return descInsumo; }
    public void setDescInsumo(String descInsumo) { this.descInsumo = descInsumo; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public java.util.List<ImagenInsumo> getImagenes() { return imagenes; }
    public void setImagenes(java.util.List<ImagenInsumo> imagenes) { this.imagenes = imagenes; }

    @Override
    public String toString() {
        return "Insumo{idInsumo=" + idInsumo
                + ", nomInsumo='" + nomInsumo + "'}";
    }
}
