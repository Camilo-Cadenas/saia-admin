package com.saia.business;

import java.util.List;
import java.util.Optional;

import com.saia.data.AprendizDAO;
import com.saia.data.AuditoriaDAO;
import com.saia.data.CuentaDAO;
import com.saia.data.DataAccessException;
import com.saia.model.Aprendiz;
import com.saia.util.SessionManager;

/**
 * Servicio de negocio para el módulo de Aprendices.
 * Gestiona bloqueo y desbloqueo de cuentas.
 */
public class AprendizService {

    public enum EstadoResult { OK, NO_ENCONTRADO, ERROR_BD }

    private final AprendizDAO  aprendizDAO;
    private final CuentaDAO    cuentaDAO;
    private final AuditoriaDAO auditoriaDAO;

    public AprendizService() {
        this.aprendizDAO  = new AprendizDAO();
        this.cuentaDAO    = new CuentaDAO();
        this.auditoriaDAO = new AuditoriaDAO();
    }

    // ── Listado ───────────────────────────────────────────────────────────────

    public List<Aprendiz> listarTodos() {
        return aprendizDAO.findAll();
    }

    // ── Bloquear ──────────────────────────────────────────────────────────────

    /**
     * Bloquea la cuenta de un aprendiz: pone estado=FALSE en {@code cuenta}.
     */
    public EstadoResult bloquear(int numDoc) {
        return cambiarEstado(numDoc, false);
    }

    // ── Desbloquear ───────────────────────────────────────────────────────────

    /**
     * Desbloquea la cuenta de un aprendiz: pone estado=TRUE en {@code cuenta}.
     */
    public EstadoResult desbloquear(int numDoc) {
        return cambiarEstado(numDoc, true);
    }

    // ── Lógica interna ────────────────────────────────────────────────────────

    private EstadoResult cambiarEstado(int numDoc, boolean activo) {
        try {
            Optional<Aprendiz> opt = aprendizDAO.findByNumDoc(numDoc);
            if (opt.isEmpty()) return EstadoResult.NO_ENCONTRADO;

            // Actualizar estado en tabla cuenta
            cuentaDAO.updateEstado(numDoc, activo);

            // Auditoría
            Aprendiz a = opt.get();
            String accion = activo ? AuditoriaDAO.ACCION_DESBLOQUEAR : AuditoriaDAO.ACCION_BLOQUEAR;
            String desc   = (activo ? "Se habilitó" : "Se bloqueó")
                    + " la cuenta del aprendiz " + a.getNombreCompleto()
                    + " (N° doc: " + numDoc + ")";
            Integer adminDoc = null;
            if (SessionManager.getInstance().isSessionActive()) {
                adminDoc = SessionManager.getInstance().getAdmin().getNumDoc();
            }
            auditoriaDAO.registrar(accion, AuditoriaDAO.ENTIDAD_APRENDIZ,
                    numDoc, desc, adminDoc);

            return EstadoResult.OK;

        } catch (DataAccessException e) {
            System.err.println("[AprendizService] Error cambiarEstado: " + e.getMessage());
            return EstadoResult.ERROR_BD;
        }
    }
}
