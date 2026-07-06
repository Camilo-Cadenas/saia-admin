package com.saia.business;

import com.saia.data.DashboardDAO;

/**
 * Servicio de negocio para el dashboard. Coordina la carga de estadísticas
 * desde {@link DashboardDAO} y construye el {@link DashboardStats}.
 */
public class DashboardService {

    private final DashboardDAO dao;

    public DashboardService() {
        this.dao = new DashboardDAO();
    }

    /**
     * Carga todas las estadísticas del dashboard en una sola llamada.
     * Se ejecuta en un {@code SwingWorker} desde la capa de presentación.
     *
     * @return stats listo para renderizar
     */
    public DashboardStats loadStats() {
        return new DashboardStats(
                dao.countPersonalTotal(),
                dao.countPersonalActivo(),
                dao.countPersonalInactivo(),
                dao.countAprendicesTotal(),
                dao.countAprendicesActivos(),
                dao.countAprendicesInactivos(),
                dao.countReportesTotal(),
                dao.countReportesHoy(),
                dao.countReportesMes(),
                dao.countUsuariosBloqueadosPersonal(),
                dao.countUsuariosBloqueadosAprendices(),
                dao.countIngresosHoy(),
                dao.countSalidasHoy(),
                dao.countReportesPorTipoMes(),
                dao.getActividadReciente(5)
        );
    }
}
