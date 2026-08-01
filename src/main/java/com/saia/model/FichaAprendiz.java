package com.saia.model;

import java.time.LocalDateTime;

/**
 * Modelo para la relación entre Fichas de Formación y Aprendices.
 * 
 * <p>Representa la asignación de un aprendiz a una ficha formativa.
 * Cada ficha puede tener un máximo de 30 aprendices activos.</p>
 * 
 * <p><b>Tabla:</b> ficha_aprendiz</p>
 * 
 * @author SAIA Team
 * @version 1.0
 */
public class FichaAprendiz {
    
    /**
     * Estados posibles de la relación ficha-aprendiz
     */
    public enum Estado {
        ACTIVO("ACTIVO"),
        INACTIVO("INACTIVO"),
        RETIRADO("RETIRADO");
        
        private final String value;
        
        Estado(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static Estado fromString(String value) {
            for (Estado estado : Estado.values()) {
                if (estado.value.equalsIgnoreCase(value)) {
                    return estado;
                }
            }
            return ACTIVO; // Default
        }
    }
    
    // ── Campos de la tabla ────────────────────────────────────────────────────
    
    private int idFichaAprendiz;
    private int idFicha;
    private int idCuenta;
    private LocalDateTime fechaAsignacion;
    private Estado estado;
    
    // ── Información extendida (JOIN con otras tablas) ─────────────────────────
    
    private String nomFicha;
    private String jornada;
    private int numDoc;
    private String nombres;
    private String apellidos;
    private String email;
    private String programa;
    private String centro;
    private int totalAprendicesActivos; // Total en la ficha
    
    // ── Constructores ─────────────────────────────────────────────────────────
    
    public FichaAprendiz() {
        this.estado = Estado.ACTIVO;
    }
    
    public FichaAprendiz(int idFicha, int idCuenta) {
        this.idFicha = idFicha;
        this.idCuenta = idCuenta;
        this.estado = Estado.ACTIVO;
    }
    
    // ── Getters y Setters ─────────────────────────────────────────────────────
    
    public int getIdFichaAprendiz() {
        return idFichaAprendiz;
    }
    
    public void setIdFichaAprendiz(int idFichaAprendiz) {
        this.idFichaAprendiz = idFichaAprendiz;
    }
    
    public int getIdFicha() {
        return idFicha;
    }
    
    public void setIdFicha(int idFicha) {
        this.idFicha = idFicha;
    }
    
    public int getIdCuenta() {
        return idCuenta;
    }
    
    public void setIdCuenta(int idCuenta) {
        this.idCuenta = idCuenta;
    }
    
    public LocalDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }
    
    public void setFechaAsignacion(LocalDateTime fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }
    
    public Estado getEstado() {
        return estado;
    }
    
    public void setEstado(Estado estado) {
        this.estado = estado;
    }
    
    public void setEstado(String estado) {
        this.estado = Estado.fromString(estado);
    }
    
    // ── Getters y Setters de información extendida ────────────────────────────
    
    public String getNomFicha() {
        return nomFicha;
    }
    
    public void setNomFicha(String nomFicha) {
        this.nomFicha = nomFicha;
    }
    
    public String getJornada() {
        return jornada;
    }
    
    public void setJornada(String jornada) {
        this.jornada = jornada;
    }
    
    public int getNumDoc() {
        return numDoc;
    }
    
    public void setNumDoc(int numDoc) {
        this.numDoc = numDoc;
    }
    
    public String getNombres() {
        return nombres;
    }
    
    public void setNombres(String nombres) {
        this.nombres = nombres;
    }
    
    public String getApellidos() {
        return apellidos;
    }
    
    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPrograma() {
        return programa;
    }
    
    public void setPrograma(String programa) {
        this.programa = programa;
    }
    
    public String getCentro() {
        return centro;
    }
    
    public void setCentro(String centro) {
        this.centro = centro;
    }
    
    public int getTotalAprendicesActivos() {
        return totalAprendicesActivos;
    }
    
    public void setTotalAprendicesActivos(int totalAprendicesActivos) {
        this.totalAprendicesActivos = totalAprendicesActivos;
    }
    
    // ── Métodos de utilidad ───────────────────────────────────────────────────
    
    /**
     * @return Nombre completo del aprendiz
     */
    public String getNombreCompleto() {
        return (nombres != null ? nombres : "") + " " + (apellidos != null ? apellidos : "");
    }
    
    /**
     * @return true si la relación está activa
     */
    public boolean isActivo() {
        return estado == Estado.ACTIVO;
    }
    
    /**
     * @return Cupos disponibles en la ficha (máximo 30)
     */
    public int getCuposDisponibles() {
        return 30 - totalAprendicesActivos;
    }
    
    /**
     * @return true si la ficha está llena (30 aprendices)
     */
    public boolean isFichaLlena() {
        return totalAprendicesActivos >= 30;
    }
    
    @Override
    public String toString() {
        return "FichaAprendiz{" +
                "idFichaAprendiz=" + idFichaAprendiz +
                ", idFicha=" + idFicha +
                ", idCuenta=" + idCuenta +
                ", estado=" + estado +
                ", nomFicha='" + nomFicha + '\'' +
                ", numDoc=" + numDoc +
                ", nombreCompleto='" + getNombreCompleto() + '\'' +
                ", totalAprendicesActivos=" + totalAprendicesActivos +
                '}';
    }
}
