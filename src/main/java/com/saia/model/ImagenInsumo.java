package com.saia.model;

/**
 * Entidad que representa la tabla {@code imagen_insumo}.
 */
public class ImagenInsumo {

    private int idImagen;
    private int idInsumo;         // FK → insumo
    private String rutaImagen;    // VARCHAR(255)
    private int ordenImagen;      // TINYINT

    public ImagenInsumo() {}

    // ---- Getters y Setters ----

    public int getIdImagen() { return idImagen; }
    public void setIdImagen(int idImagen) { this.idImagen = idImagen; }

    public int getIdInsumo() { return idInsumo; }
    public void setIdInsumo(int idInsumo) { this.idInsumo = idInsumo; }

    public String getRutaImagen() { return rutaImagen; }
    public void setRutaImagen(String rutaImagen) { this.rutaImagen = rutaImagen; }

    public int getOrdenImagen() { return ordenImagen; }
    public void setOrdenImagen(int ordenImagen) { this.ordenImagen = ordenImagen; }

    @Override
    public String toString() {
        return "ImagenInsumo{idImagen=" + idImagen
                + ", rutaImagen='" + rutaImagen + "'}";
    }
}
