package com.saia.business;

import java.util.List;
import java.util.Map;

/**
 * DTO inmutable que agrupa todas las estadísticas necesarias para el panel de inicio.
 */
public final class DashboardStats {

    // Personal de seguridad
    public final int personalTotal;
    public final int personalActivo;
    public final int personalInactivo;

    // Aprendices
    public final int aprendicesTotal;
    public final int aprendicesActivos;
    public final int aprendicesInactivos;

    // Reportes
    public final int reportesTotal;
    public final int reportesHoy;
    public final int reportesMes;

    // Bloqueados
    public final int bloqueadosPersonal;
    public final int bloqueadosAprendices;

    // Ingresos / Salidas hoy
    public final int ingresosHoy;
    public final int salidasHoy;

    // Reportes por tipo (mes actual)
    public final Map<String, Integer> reportesPorTipo;

    // Actividad reciente
    public final List<com.saia.data.DashboardDAO.ActividadItem> actividadReciente;

    public DashboardStats(
            int personalTotal, int personalActivo, int personalInactivo,
            int aprendicesTotal, int aprendicesActivos, int aprendicesInactivos,
            int reportesTotal, int reportesHoy, int reportesMes,
            int bloqueadosPersonal, int bloqueadosAprendices,
            int ingresosHoy, int salidasHoy,
            Map<String, Integer> reportesPorTipo,
            List<com.saia.data.DashboardDAO.ActividadItem> actividadReciente) {

        this.personalTotal      = personalTotal;
        this.personalActivo     = personalActivo;
        this.personalInactivo   = personalInactivo;
        this.aprendicesTotal    = aprendicesTotal;
        this.aprendicesActivos  = aprendicesActivos;
        this.aprendicesInactivos = aprendicesInactivos;
        this.reportesTotal      = reportesTotal;
        this.reportesHoy        = reportesHoy;
        this.reportesMes        = reportesMes;
        this.bloqueadosPersonal   = bloqueadosPersonal;
        this.bloqueadosAprendices = bloqueadosAprendices;
        this.ingresosHoy        = ingresosHoy;
        this.salidasHoy         = salidasHoy;
        this.reportesPorTipo    = reportesPorTipo;
        this.actividadReciente  = actividadReciente;
    }

    public int getBloqueadosTotal() {
        return bloqueadosPersonal + bloqueadosAprendices;
    }
}
