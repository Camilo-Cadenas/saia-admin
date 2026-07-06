package com.saia.business;

import java.util.List;

import com.saia.data.AuditoriaDAO;
import com.saia.data.CuentaDAO;
import com.saia.data.DataAccessException;
import com.saia.data.UsuarioBloqueadoDAO;
import com.saia.data.UsuarioBloqueadoDAO.UsuarioBloqueado;
import com.saia.util.SessionManager;

/**
 * Servicio para el panel de Bloqueo de Usuarios.
 * Gestiona la consulta de bloqueados y la habilitación de cuentas.
 */
public class BloqueoService {

    public enum HabilitarResult { OK, NO_ENCONTRADO, ERROR_BD }

    private final UsuarioBloqueadoDAO bloqueadoDAO;
    private final CuentaDAO           cuentaDAO;
    private final AuditoriaDAO        auditoriaDAO;

    public BloqueoService() {
        this.bloqueadoDAO = new UsuarioBloqueadoDAO();
        this.cuentaDAO    = new CuentaDAO();
        this.auditoriaDAO = new AuditoriaDAO();
    }

    public List<UsuarioBloqueado> listarBloqueados() {
        return bloqueadoDAO.findAllBloqueados();
    }

    /**
     * Habilita la cuenta de un usuario bloqueado.
     */
    public HabilitarResult habilitar(int numDoc, String rol) {
        try {
            if (!cuentaDAO.exists(numDoc)) return HabilitarResult.NO_ENCONTRADO;

            cuentaDAO.updateEstado(numDoc, true);

            String desc = "Se habilitó la cuenta de " + rol + " N° doc: " + numDoc;
            Integer adminDoc = null;
            if (SessionManager.getInstance().isSessionActive())
                adminDoc = SessionManager.getInstance().getAdmin().getNumDoc();

            auditoriaDAO.registrar(
                AuditoriaDAO.ACCION_DESBLOQUEAR,
                rol,
                numDoc, desc, adminDoc);

            return HabilitarResult.OK;
        } catch (DataAccessException e) {
            System.err.println("[BloqueoService] Error: " + e.getMessage());
            return HabilitarResult.ERROR_BD;
        }
    }
}
