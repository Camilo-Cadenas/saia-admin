package com.saia.business;

import java.util.List;

import com.saia.data.DataAccessException;
import com.saia.data.FichaAprendizDAO;
import com.saia.data.FichaAprendizDAO.AprendizSinFicha;
import com.saia.data.FichaAprendizDAO.FichaConCupos;
import com.saia.model.FichaAprendiz;

/**
 * Servicio de negocio para gestionar la relación entre fichas y aprendices.
 * 
 * <p>Proporciona operaciones de alto nivel con validación de reglas de negocio.</p>
 * 
 * @author SAIA Team
 * @version 1.0
 */
public class FichaAprendizService {
    
    private final FichaAprendizDAO dao = new FichaAprendizDAO();
    
    // ── Resultados de operaciones ─────────────────────────────────────────────
    
    public enum AsignacionResult {
        OK,
        FICHA_LLENA,
        YA_ASIGNADO,
        NO_ES_APRENDIZ,
        ERROR_BD
    }
    
    public enum RetiroResult {
        OK,
        NO_ASIGNADO,
        ERROR_BD
    }
    
    public enum TransferenciaResult {
        OK,
        FICHA_DESTINO_LLENA,
        NO_ASIGNADO,
        ERROR_BD
    }
    
    // ── Consultas ─────────────────────────────────────────────────────────────
    
    /**
     * Obtiene todos los aprendices de una ficha.
     * 
     * @param idFicha ID de la ficha
     * @return Lista de aprendices asignados
     */
    public List<FichaAprendiz> obtenerAprendicesDeFicha(int idFicha) {
        return dao.findByFicha(idFicha);
    }
    
    /**
     * Obtiene la ficha activa de un aprendiz.
     * 
     * @param idCuenta ID de la cuenta del aprendiz
     * @return FichaAprendiz o null si no está asignado
     */
    public FichaAprendiz obtenerFichaDeAprendiz(int idCuenta) {
        return dao.findByCuenta(idCuenta);
    }
    
    /**
     * Obtiene aprendices que no están asignados a ninguna ficha.
     * 
     * @return Lista de aprendices sin ficha
     */
    public List<AprendizSinFicha> obtenerAprendicesSinFicha() {
        return dao.findAprendicesSinFicha();
    }
    
    /**
     * Obtiene fichas que tienen cupos disponibles.
     * 
     * @return Lista de fichas con cupos
     */
    public List<FichaConCupos> obtenerFichasConCupos() {
        return dao.findFichasConCupos();
    }
    
    /**
     * Verifica cuántos cupos tiene disponibles una ficha.
     * 
     * @param idFicha ID de la ficha
     * @return Número de cupos disponibles (0-30)
     */
    public int getCuposDisponibles(int idFicha) {
        int activos = dao.countActivosEnFicha(idFicha);
        return 30 - activos;
    }
    
    /**
     * Verifica si una ficha está llena (30 aprendices).
     * 
     * @param idFicha ID de la ficha
     * @return true si está llena
     */
    public boolean isFichaLlena(int idFicha) {
        return dao.countActivosEnFicha(idFicha) >= 30;
    }
    
    // ── Operaciones ───────────────────────────────────────────────────────────
    
    /**
     * Asigna un aprendiz a una ficha.
     * Valida que:
     * - La ficha no esté llena (máximo 30)
     * - El aprendiz no esté ya asignado a otra ficha
     * - La cuenta sea de un aprendiz (rol=1)
     * 
     * @param idFicha ID de la ficha
     * @param idCuenta ID de la cuenta del aprendiz
     * @return Resultado de la operación
     */
    public AsignacionResult asignarAprendizAFicha(int idFicha, int idCuenta) {
        try {
            dao.asignarAprendiz(idFicha, idCuenta);
            System.out.println("[FichaAprendizService] Aprendiz " + idCuenta + 
                             " asignado a ficha " + idFicha);
            return AsignacionResult.OK;
        } catch (DataAccessException e) {
            String msg = e.getMessage();
            System.err.println("[FichaAprendizService] Error asignando: " + msg);
            
            if (msg.contains("límite") || msg.contains("llena")) {
                return AsignacionResult.FICHA_LLENA;
            } else if (msg.contains("ya está asignado")) {
                return AsignacionResult.YA_ASIGNADO;
            } else if (msg.contains("aprendices")) {
                return AsignacionResult.NO_ES_APRENDIZ;
            }
            return AsignacionResult.ERROR_BD;
        }
    }
    
    /**
     * Retira un aprendiz de su ficha activa.
     * 
     * @param idCuenta ID de la cuenta del aprendiz
     * @return Resultado de la operación
     */
    public RetiroResult retirarAprendizDeFicha(int idCuenta) {
        try {
            dao.retirarAprendiz(idCuenta);
            System.out.println("[FichaAprendizService] Aprendiz " + idCuenta + " retirado de ficha");
            return RetiroResult.OK;
        } catch (DataAccessException e) {
            System.err.println("[FichaAprendizService] Error retirando: " + e.getMessage());
            if (e.getMessage().contains("no está asignado")) {
                return RetiroResult.NO_ASIGNADO;
            }
            return RetiroResult.ERROR_BD;
        }
    }
    
    /**
     * Transfiere un aprendiz de una ficha a otra.
     * Valida que la ficha destino tenga cupos disponibles.
     * 
     * @param idCuenta ID de la cuenta del aprendiz
     * @param idFichaNueva ID de la ficha destino
     * @return Resultado de la operación
     */
    public TransferenciaResult transferirAprendiz(int idCuenta, int idFichaNueva) {
        try {
            dao.transferirAprendiz(idCuenta, idFichaNueva);
            System.out.println("[FichaAprendizService] Aprendiz " + idCuenta + 
                             " transferido a ficha " + idFichaNueva);
            return TransferenciaResult.OK;
        } catch (DataAccessException e) {
            String msg = e.getMessage();
            System.err.println("[FichaAprendizService] Error transfiriendo: " + msg);
            
            if (msg.contains("límite") || msg.contains("llena")) {
                return TransferenciaResult.FICHA_DESTINO_LLENA;
            } else if (msg.contains("no está asignado")) {
                return TransferenciaResult.NO_ASIGNADO;
            }
            return TransferenciaResult.ERROR_BD;
        }
    }
    
    // ── Validaciones ──────────────────────────────────────────────────────────
    
    /**
     * Valida si se puede asignar un aprendiz a una ficha.
     * 
     * @param idFicha ID de la ficha
     * @param idCuenta ID de la cuenta del aprendiz
     * @return Mensaje de error o null si es válido
     */
    public String validarAsignacion(int idFicha, int idCuenta) {
        // Verificar si la ficha está llena
        if (isFichaLlena(idFicha)) {
            return "La ficha ha alcanzado el límite de 30 aprendices";
        }
        
        // Verificar si el aprendiz ya está asignado
        FichaAprendiz fichaActual = dao.findByCuenta(idCuenta);
        if (fichaActual != null) {
            return "El aprendiz ya está asignado a la ficha: " + fichaActual.getNomFicha();
        }
        
        return null; // Válido
    }
    
    /**
     * Obtiene estadísticas de ocupación de una ficha.
     * 
     * @param idFicha ID de la ficha
     * @return Texto descriptivo de la ocupación
     */
    public String getEstadisticasFicha(int idFicha) {
        int activos = dao.countActivosEnFicha(idFicha);
        int cupos = 30 - activos;
        double porcentaje = (activos * 100.0) / 30.0;
        
        return String.format("%d/%d aprendices (%.0f%% ocupado) - %d cupos disponibles",
                           activos, 30, porcentaje, cupos);
    }
}
