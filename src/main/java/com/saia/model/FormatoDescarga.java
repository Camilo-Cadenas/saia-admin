package com.saia.model;

/** Formatos de exportación disponibles. */
public enum FormatoDescarga {

    EXCEL(
        "Excel",
        ".xlsx",
        "\uD83D\uDCF0",
        "Compatible con Microsoft Excel y LibreOffice"
    ),
    PDF(
        "PDF",
        ".pdf",
        "\uD83D\uDCC4",
        "Documento portable, ideal para imprimir"
    ),
    CSV(
        "CSV",
        ".csv",
        "\uD83D\uDDC2",
        "Texto separado por comas, para análisis de datos"
    );

    public final String titulo;
    public final String extension;
    public final String icono;
    public final String descripcion;

    FormatoDescarga(String titulo, String extension, String icono, String descripcion) {
        this.titulo      = titulo;
        this.extension   = extension;
        this.icono       = icono;
        this.descripcion = descripcion;
    }
}
