package com.saia.model;

/** Tipos de reporte disponibles en el generador. */
public enum TipoReporte {

    HISTORIAL_INGRESOS(
        "Historial de ingresos",
        "Registros de entrada y salida del SENA",
        "\uD83D\uDCC5",
        0x059669   // verde
    ),
    APRENDICES(
        "Aprendices",
        "Listado completo de aprendices registrados",
        "\uD83C\uDF93",
        0x7C3AED   // morado
    ),
    RESUMEN_GENERAL(
        "Resumen general",
        "Estadísticas consolidadas del período",
        "\uD83D\uDCCA",
        0x0D9488   // teal
    ),
    ESCANEOS_QR(
        "Escaneos QR",
        "Detalle de escaneos por dispositivo",
        "\uD83D\uDCF1",
        0xEA580C   // naranja
    ),
    PERSONAL_SEGURIDAD(
        "Personal de seguridad",
        "Guardas y turnos registrados",
        "\uD83D\uDC6E",
        0x2563EB   // azul
    );

    public final String titulo;
    public final String descripcion;
    public final String icono;
    public final int    colorHex;

    TipoReporte(String titulo, String descripcion, String icono, int colorHex) {
        this.titulo      = titulo;
        this.descripcion = descripcion;
        this.icono       = icono;
        this.colorHex    = colorHex;
    }
}
