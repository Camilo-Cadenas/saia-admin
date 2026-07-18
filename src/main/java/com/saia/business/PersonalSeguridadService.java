package com.saia.business;

import java.util.List;
import java.util.Optional;

import com.saia.data.AuditoriaDAO;
import com.saia.data.CuentaDAO;
import com.saia.data.DataAccessException;
import com.saia.data.PersonaDAO;
import com.saia.data.PersonalSeguridadDAO;
import com.saia.model.Persona;
import com.saia.model.PersonalSeguridad;
import com.saia.util.PasswordUtil;
import com.saia.util.SessionManager;

/**
 * Servicio de negocio para el módulo de Personal de Seguridad.
 *
 * Registro mínimo (campos obligatorios):
 *   persona  → num_doc, tip_doc, nombres, p_ape, email
 *   personal_seguridad → num_doc (turno/empresa se editan después)
 *   cuenta   → id_rol=2, num_doc, mail=email, password_hash=hash(Temp+numDoc)
 */
public class PersonalSeguridadService {

    /** id_rol = 3 para Guarda (antes Personal de Seguridad) */
    private static final int ID_ROL_PERSONAL = 3;

    public enum RegistroResult {
        OK,
        DOC_DUPLICADO,
        EMAIL_DUPLICADO,
        CAMPO_REQUERIDO,
        ERROR_BD
    }

    public enum EditarResult {
        OK,
        NO_ENCONTRADO,
        EMAIL_DUPLICADO,
        CAMPO_REQUERIDO,
        ERROR_BD
    }

    private final PersonaDAO           personaDAO;
    private final PersonalSeguridadDAO guardaDAO;
    private final CuentaDAO            cuentaDAO;
    private final AuditoriaDAO         auditoriaDAO;

    public PersonalSeguridadService() {
        this.personaDAO   = new PersonaDAO();
        this.guardaDAO    = new PersonalSeguridadDAO();
        this.cuentaDAO    = new CuentaDAO();
        this.auditoriaDAO = new AuditoriaDAO();
    }

    // ── Listado ───────────────────────────────────────────────────────────────

    public List<PersonalSeguridad> listarTodos() {
        return guardaDAO.findAll();
    }

    // ── Registro ──────────────────────────────────────────────────────────────

    /**
     * Registra un nuevo guardia con todos los datos del formulario.
     *
     * @param persona datos personales (obligatorios: num_doc, tipDoc, nombres, pApe, email)
     * @param guardia datos de personal_seguridad (turno y empresa opcionales)
     * @param activo  true = cuenta activa, false = cuenta inactiva
     * @return resultado de la operación
     */
    public RegistroResult registrar(Persona persona, PersonalSeguridad guardia, boolean activo) {

        // 1. Validar obligatorios
        if (isBlank(persona.getNombres())
                || isBlank(persona.getPApe())
                || isBlank(persona.getEmail())
                || isBlank(persona.getTipDoc())
                || persona.getNumDoc() == 0) {
            return RegistroResult.CAMPO_REQUERIDO;
        }

        try {
            // 2. Verificar documento duplicado en persona
            if (personaDAO.existsByNumDoc(persona.getNumDoc())) {
                return RegistroResult.DOC_DUPLICADO;
            }

            // 3. Verificar email duplicado en cuenta
            Optional<com.saia.model.Cuenta> cuentaExiste =
                    cuentaDAO.findByMail(persona.getEmail());
            if (cuentaExiste.isPresent()) {
                return RegistroResult.EMAIL_DUPLICADO;
            }

            // Normalizar email
            String emailNorm = persona.getEmail().trim().toLowerCase();
            persona.setEmail(emailNorm);

            // Contraseña temporal: Temp + num_doc
            String passTemp  = "Temp" + persona.getNumDoc();
            String hashTemp  = PasswordUtil.hashPassword(passTemp);

            // 4. INSERT persona
            personaDAO.insert(persona);

            // 5. INSERT personal_seguridad con turno y empresa si vienen
            guardaDAO.insert(
                persona.getNumDoc(),
                guardia != null ? guardia.getTurno()      : null,
                guardia != null ? guardia.getEmpresaSeg() : null
            );

            // 6. INSERT cuenta con el estado elegido por el usuario
            cuentaDAO.insert(ID_ROL_PERSONAL, persona.getNumDoc(), emailNorm, hashTemp, activo);

            // 7. Registrar en auditoría
            String desc = "Se creó la cuenta de " + persona.getNombreCompleto()
                    + " (N° doc: " + persona.getNumDoc() + ")";
            Integer adminDoc = null;
            if (SessionManager.getInstance().isSessionActive()) {
                adminDoc = SessionManager.getInstance().getAdmin().getNumDoc();
            }
            auditoriaDAO.registrar(
                AuditoriaDAO.ACCION_CREAR,
                AuditoriaDAO.ENTIDAD_PERSONAL,
                persona.getNumDoc(),
                desc,
                adminDoc
            );

            System.out.println("[PersonalSeguridadService] Registrado: "
                    + persona.getNombreCompleto()
                    + " | contraseña temporal: " + passTemp);

            return RegistroResult.OK;

        } catch (DataAccessException e) {
            System.err.println("[PersonalSeguridadService] Error BD: " + e.getMessage());
            return RegistroResult.ERROR_BD;
        }
    }

    // ── Bloquear / Desbloquear ────────────────────────────────────────────────

    public enum BloqueoResult { OK, NO_ENCONTRADO, ERROR_BD }

    public BloqueoResult bloquear(int numDoc) { return cambiarEstado(numDoc, false); }
    public BloqueoResult desbloquear(int numDoc) { return cambiarEstado(numDoc, true); }

    private BloqueoResult cambiarEstado(int numDoc, boolean activo) {
        try {
            if (!guardaDAO.exists(numDoc)) return BloqueoResult.NO_ENCONTRADO;
            cuentaDAO.updateEstado(numDoc, activo);

            String accion = activo ? AuditoriaDAO.ACCION_DESBLOQUEAR : AuditoriaDAO.ACCION_BLOQUEAR;
            String desc   = (activo ? "Se habilitó" : "Se bloqueó")
                    + " la cuenta del guardia N° doc: " + numDoc;
            Integer adminDoc = null;
            if (SessionManager.getInstance().isSessionActive())
                adminDoc = SessionManager.getInstance().getAdmin().getNumDoc();
            auditoriaDAO.registrar(accion, AuditoriaDAO.ENTIDAD_PERSONAL, numDoc, desc, adminDoc);
            return BloqueoResult.OK;
        } catch (DataAccessException e) {
            System.err.println("[PersonalSeguridadService] Error bloqueo: " + e.getMessage());
            return BloqueoResult.ERROR_BD;
        }
    }

    // ── Búsqueda ──────────────────────────────────────────────────────────────

    public Optional<PersonalSeguridad> buscarPorNumDoc(int numDoc) {
        return guardaDAO.findByNumDoc(numDoc);
    }

    // ── Edición ───────────────────────────────────────────────────────────────

    /**
     * Actualiza los datos de un guardia existente.
     *
     * @param persona datos personales actualizados
     * @param guardia datos de personal_seguridad actualizados
     * @param activo  true = cuenta activa, false = cuenta inactiva
     * @return resultado de la operación
     */
    public EditarResult editar(Persona persona, PersonalSeguridad guardia, boolean activo) {
        if (isBlank(persona.getNombres())
                || isBlank(persona.getPApe())
                || isBlank(persona.getEmail())) {
            return EditarResult.CAMPO_REQUERIDO;
        }
        try {
            // Verificar que existe
            if (!guardaDAO.exists(persona.getNumDoc())) {
                return EditarResult.NO_ENCONTRADO;
            }
            // Verificar email duplicado en otro registro
            if (personaDAO.emailExistsForOther(persona.getEmail(), persona.getNumDoc())) {
                return EditarResult.EMAIL_DUPLICADO;
            }
            persona.setEmail(persona.getEmail().trim().toLowerCase());
            // Actualizar persona
            personaDAO.update(persona);
            // Actualizar personal_seguridad
            guardia.setNumDoc(persona.getNumDoc());
            guardaDAO.update(guardia);
            // Actualizar estado de la cuenta
            cuentaDAO.updateEstado(persona.getNumDoc(), activo);

            // Registrar en auditoría
            String desc = "Se actualizaron los datos de " + persona.getNombreCompleto()
                    + " (N° doc: " + persona.getNumDoc() + ")";
            Integer adminDoc = null;
            if (SessionManager.getInstance().isSessionActive()) {
                adminDoc = SessionManager.getInstance().getAdmin().getNumDoc();
            }
            auditoriaDAO.registrar(
                AuditoriaDAO.ACCION_ACTUALIZAR,
                AuditoriaDAO.ENTIDAD_PERSONAL,
                persona.getNumDoc(),
                desc,
                adminDoc
            );

            return EditarResult.OK;
        } catch (DataAccessException e) {
            System.err.println("[PersonalSeguridadService] Error editar: " + e.getMessage());
            return EditarResult.ERROR_BD;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
