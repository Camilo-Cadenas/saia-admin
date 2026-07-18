package com.saia.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * POJO que representa un registro de la tabla {@code auditoria_actividad}.
 */
public class RegistroAuditoria {

    public enum Accion {
        CREACION   ("Creación",     0x059669, 0xD1FAE5),  // verde
        MODIFICACION("Modificación", 0xD97706, 0xFEF3C7), // amarillo-naranja
        ELIMINACION("Eliminación",  0xDC2626, 0xFEE2E2),  // rojo
        BLOQUEAR   ("Bloquear",     0x7C3AED, 0xEDE9FE),  // morado
        HABILITAR  ("Habilitar",    0x0891B2, 0xCFFAFE),  // cyan
        OTRO       ("Otro",         0x6B7280, 0xF3F4F6);  // gris

        public final String label;
        public final int    colorFg;
        public final int    colorBg;

        Accion(String label, int fg, int bg) {
            this.label   = label;
            this.colorFg = fg;
            this.colorBg = bg;
        }

        /** Mapea el tipo_accion de BD al enum. */
        public static Accion fromBD(String tipo) {
            if (tipo == null) return OTRO;
            return switch (tipo.toUpperCase().trim()) {
                case "CREAR", "CREACION", "INSERT", "REGISTRO" -> CREACION;
                case "ACTUALIZAR", "MODIFICAR", "UPDATE", "MODIFICACION" -> MODIFICACION;
                case "ELIMINAR", "ELIMINACION", "DELETE" -> ELIMINACION;
                case "BLOQUEAR", "BLOQUEO" -> BLOQUEAR;
                case "HABILITAR", "DESBLOQUEAR" -> HABILITAR;
                default -> OTRO;
            };
        }
    }

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA  = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FMT_TS    = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private int           idActividad;
    private LocalDateTime fechaHora;
    private String        usuario;         // nombre del que realizó
    private int           numDocUsuario;   // realizado_por
    private Accion        accion;
    private String        entidad;
    private String        descripcion;
    private String        modulo;          // derivado de entidad
    private int           numDocAfectado;  // num_doc de la fila afectada

    public RegistroAuditoria() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public int getIdActividad()             { return idActividad; }
    public void setIdActividad(int v)       { this.idActividad = v; }

    public LocalDateTime getFechaHora()         { return fechaHora; }
    public void setFechaHora(LocalDateTime v)   { this.fechaHora = v; }

    public String getUsuario()              { return usuario != null ? usuario : "—"; }
    public void setUsuario(String v)        { this.usuario = v; }

    public int getNumDocUsuario()           { return numDocUsuario; }
    public void setNumDocUsuario(int v)     { this.numDocUsuario = v; }

    public Accion getAccion()               { return accion != null ? accion : Accion.OTRO; }
    public void setAccion(Accion v)         { this.accion = v; }

    public String getEntidad()              { return entidad != null ? entidad : "—"; }
    public void setEntidad(String v)        { this.entidad = v; }

    public String getDescripcion()          { return descripcion != null ? descripcion : "—"; }
    public void setDescripcion(String v)    { this.descripcion = v; }

    public String getModulo()               { return modulo != null ? modulo : entidad; }
    public void setModulo(String v)         { this.modulo = v; }

    public int getNumDocAfectado()          { return numDocAfectado; }
    public void setNumDocAfectado(int v)    { this.numDocAfectado = v; }

    // ── Helpers formato ───────────────────────────────────────────────────────

    public String getFechaStr() {
        return fechaHora != null ? fechaHora.format(FMT_FECHA) : "—";
    }
    public String getHoraStr() {
        return fechaHora != null ? fechaHora.format(FMT_HORA) : "—";
    }
    public String getFechaHoraStr() {
        return fechaHora != null ? fechaHora.format(FMT_TS) : "—";
    }

    @Override
    public String toString() {
        return "RegistroAuditoria{id=" + idActividad + ", accion=" + accion + ", entidad=" + entidad + "}";
    }
}
