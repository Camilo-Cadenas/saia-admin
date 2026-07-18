package com.saia.model;

import java.time.LocalDateTime;

/**
 * Entidad que representa la tabla {@code historial_ingreso}.
 * BD nueva: usa num_doc (no id_aprendiz). estado_movimiento: 1=Ingreso, 0=Salida (int/TINYINT).
 */
public class HistorialIngreso {

    private int idIngreso;
    private int numDoc;            // num_doc FK → persona (reemplaza id_aprendiz)
    private int idGuarda;
    private LocalDateTime fechaHoraIngreso;
    private LocalDateTime fechaHoraSalida;
    private int estadoMovimiento;  // 1 = Ingreso, 0 = Salida
    private String observacion;

    /** Objetos asociados opcionales (joins) */
    private Aprendiz          aprendiz;
    private PersonalSeguridad guarda;

    public HistorialIngreso() {}

    public int getIdIngreso() { return idIngreso; }
    public void setIdIngreso(int idIngreso) { this.idIngreso = idIngreso; }

    public int getNumDoc() { return numDoc; }
    public void setNumDoc(int numDoc) { this.numDoc = numDoc; }

    /** @deprecated Usar getNumDoc() — la BD nueva no tiene id_aprendiz */
    @Deprecated
    public int getIdAprendiz() { return numDoc; }
    /** @deprecated Usar setNumDoc() */
    @Deprecated
    public void setIdAprendiz(int idAprendiz) { this.numDoc = idAprendiz; }

    public int getIdGuarda() { return idGuarda; }
    public void setIdGuarda(int idGuarda) { this.idGuarda = idGuarda; }

    public LocalDateTime getFechaHoraIngreso() { return fechaHoraIngreso; }
    public void setFechaHoraIngreso(LocalDateTime fechaHoraIngreso) { this.fechaHoraIngreso = fechaHoraIngreso; }

    public LocalDateTime getFechaHoraSalida() { return fechaHoraSalida; }
    public void setFechaHoraSalida(LocalDateTime fechaHoraSalida) { this.fechaHoraSalida = fechaHoraSalida; }

    /** 1 = Ingreso, 0 = Salida */
    public int getEstadoMovimiento() { return estadoMovimiento; }
    public void setEstadoMovimiento(int estadoMovimiento) { this.estadoMovimiento = estadoMovimiento; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public Aprendiz getAprendiz() { return aprendiz; }
    public void setAprendiz(Aprendiz aprendiz) { this.aprendiz = aprendiz; }

    public PersonalSeguridad getGuarda() { return guarda; }
    public void setGuarda(PersonalSeguridad guarda) { this.guarda = guarda; }

    @Override
    public String toString() {
        return "HistorialIngreso{idIngreso=" + idIngreso
                + ", numDoc=" + numDoc
                + ", estado=" + estadoMovimiento + "}";
    }
}
