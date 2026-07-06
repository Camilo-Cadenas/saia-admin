package com.saia.model;

import java.time.LocalDateTime;

/**
 * Entidad que representa la tabla {@code historial_ingreso}.
 */
public class HistorialIngreso {

    private int idIngreso;
    private int idAprendiz;
    private int idGuarda;
    private LocalDateTime fechaHoraIngreso;
    private LocalDateTime fechaHoraSalida;
    private String estadoMovimiento;   // VARCHAR(20)
    private String observacion;        // TEXT

    /** Aprendiz y guardia asociados (joins opcionales) */
    private Aprendiz       aprendiz;
    private PersonalSeguridad guarda;

    public HistorialIngreso() {}

    // ---- Getters y Setters ----

    public int getIdIngreso() { return idIngreso; }
    public void setIdIngreso(int idIngreso) { this.idIngreso = idIngreso; }

    public int getIdAprendiz() { return idAprendiz; }
    public void setIdAprendiz(int idAprendiz) { this.idAprendiz = idAprendiz; }

    public int getIdGuarda() { return idGuarda; }
    public void setIdGuarda(int idGuarda) { this.idGuarda = idGuarda; }

    public LocalDateTime getFechaHoraIngreso() { return fechaHoraIngreso; }
    public void setFechaHoraIngreso(LocalDateTime fechaHoraIngreso) { this.fechaHoraIngreso = fechaHoraIngreso; }

    public LocalDateTime getFechaHoraSalida() { return fechaHoraSalida; }
    public void setFechaHoraSalida(LocalDateTime fechaHoraSalida) { this.fechaHoraSalida = fechaHoraSalida; }

    public String getEstadoMovimiento() { return estadoMovimiento; }
    public void setEstadoMovimiento(String estadoMovimiento) { this.estadoMovimiento = estadoMovimiento; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public Aprendiz getAprendiz() { return aprendiz; }
    public void setAprendiz(Aprendiz aprendiz) { this.aprendiz = aprendiz; }

    public PersonalSeguridad getGuarda() { return guarda; }
    public void setGuarda(PersonalSeguridad guarda) { this.guarda = guarda; }

    @Override
    public String toString() {
        return "HistorialIngreso{idIngreso=" + idIngreso
                + ", idAprendiz=" + idAprendiz
                + ", estado='" + estadoMovimiento + "'}";
    }
}
