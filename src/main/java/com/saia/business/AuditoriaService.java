package com.saia.business;

import java.time.LocalDate;
import java.util.List;

import com.saia.data.AuditoriaDAO;
import com.saia.model.RegistroAuditoria;

/**
 * Servicio para el módulo Historial de Auditoría.
 * Encapsula la lógica de paginación y filtrado sobre AuditoriaDAO.
 */
public class AuditoriaService {

    private final AuditoriaDAO dao = new AuditoriaDAO();

    public static final int PAGE_SIZE = 12; // registros por página

    /** Resultado paginado. */
    public record PaginaAuditoria(
        List<RegistroAuditoria> registros,
        int totalRegistros,
        int paginaActual,
        int totalPaginas
    ) {}

    /**
     * Consulta paginada con filtros.
     * @param pagina  0-indexed
     */
    public PaginaAuditoria buscar(
            LocalDate desde, LocalDate hasta,
            String accion, String entidad,
            String textoBusca, int pagina) {

        int total  = dao.count(desde, hasta, accion, entidad, textoBusca);
        int pages  = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        int pag    = Math.max(0, Math.min(pagina, pages - 1));
        int offset = pag * PAGE_SIZE;

        List<RegistroAuditoria> registros =
            dao.findAll(desde, hasta, accion, entidad, textoBusca, PAGE_SIZE, offset);

        return new PaginaAuditoria(registros, total, pag, pages);
    }

    /**
     * Consulta todos los registros sin paginación.
     */
    public List<RegistroAuditoria> buscarTodos(
            LocalDate desde, LocalDate hasta,
            String accion, String entidad,
            String textoBusca) {
        // Límite alto para obtener todos los registros (ajustar según necesidades)
        return dao.findAll(desde, hasta, accion, entidad, textoBusca, 10000, 0);
    }

    public List<String> getAcciones()  { return dao.getAccionesDistintas(); }
    public List<String> getEntidades() { return dao.getEntidadesDistintas(); }
    
    /**
     * Obtiene el nombre completo de una persona desde la base de datos.
     * @param numDoc número de documento
     * @return nombre completo o "—" si no se encuentra
     */
    public String getNombrePersona(int numDoc) {
        return dao.getNombrePersona(numDoc);
    }
}
