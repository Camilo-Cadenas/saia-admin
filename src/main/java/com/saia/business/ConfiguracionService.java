package com.saia.business;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Optional;

import com.saia.data.ConfiguracionDAO;
import com.saia.data.ConfiguracionDAO.PerfilAdmin;
import com.saia.model.Persona;
import com.saia.util.PasswordUtil;

/**
 * Servicio para el módulo Configuración del Sistema.
 * Gestiona la edición del perfil del administrador autenticado.
 */
public class ConfiguracionService {

    private static final long  MAX_FOTO_BYTES  = 2 * 1024 * 1024; // 2 MB
    private static final String UPLOAD_DIR     = "uploads/perfiles/";

    private final ConfiguracionDAO dao = new ConfiguracionDAO();

    public enum GuardadoResult {
        OK, MAIL_DUPLICADO, MAIL_INVALIDO, PASSWORD_INCORRECTA,
        FECHA_INVALIDA, CAMPO_REQUERIDO, FOTO_INVALIDA, ERROR_BD
    }

    // ── Carga del perfil ──────────────────────────────────────────────────────

    public Optional<PerfilAdmin> cargarPerfil(int numDoc) {
        return dao.findPerfil(numDoc);
    }

    // ── Guardar datos personales ──────────────────────────────────────────────

    public GuardadoResult guardarDatosPersonales(int numDoc,
            String tipDoc, String nombres, String pApe, String sApe,
            String tel, String tipSang, String genero, LocalDate fechaNac,
            String email) {

        if (esVacio(tipDoc) || esVacio(nombres) || esVacio(pApe))
            return GuardadoResult.CAMPO_REQUERIDO;

        if (fechaNac != null) {
            if (fechaNac.isAfter(LocalDate.now()))
                return GuardadoResult.FECHA_INVALIDA;
            if (LocalDate.now().getYear() - fechaNac.getYear() < 18)
                return GuardadoResult.FECHA_INVALIDA;
        }

        Persona p = new Persona();
        p.setNumDoc (numDoc);
        p.setTipDoc (tipDoc.trim());
        p.setNombres(nombres.trim());
        p.setPApe   (pApe.trim());
        p.setSApe   (sApe != null && !sApe.isBlank() ? sApe.trim() : null);
        p.setTel    (tel   != null && !tel.isBlank()  ? tel.trim()  : null);
        p.setTipSang(tipSang != null && !tipSang.isBlank() ? tipSang : null);
        p.setGenero (genero  != null && !genero.isBlank()  ? genero  : null);
        p.setFechaNac(fechaNac);
        p.setEmail  (email != null ? email.trim() : null);

        return dao.updatePersona(p) ? GuardadoResult.OK : GuardadoResult.ERROR_BD;
    }

    // ── Guardar mail ──────────────────────────────────────────────────────────

    public GuardadoResult guardarMail(int numDoc, String newMail) {
        if (esVacio(newMail)) return GuardadoResult.CAMPO_REQUERIDO;
        if (!newMail.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$"))
            return GuardadoResult.MAIL_INVALIDO;
        if (dao.mailExiste(numDoc, newMail))
            return GuardadoResult.MAIL_DUPLICADO;
        return dao.updateMail(numDoc, newMail) ? GuardadoResult.OK : GuardadoResult.ERROR_BD;
    }

    // ── Cambiar contraseña ────────────────────────────────────────────────────

    public GuardadoResult cambiarPassword(int numDoc,
            String passwordActual, String passwordNueva) {
        if (esVacio(passwordActual) || esVacio(passwordNueva))
            return GuardadoResult.CAMPO_REQUERIDO;

        // Verificar contraseña actual
        String hashActual = dao.getPasswordHash(numDoc);
        if (!PasswordUtil.hashPassword(passwordActual).equals(hashActual))
            return GuardadoResult.PASSWORD_INCORRECTA;

        String newHash = PasswordUtil.hashPassword(passwordNueva);
        return dao.updatePassword(numDoc, newHash) ? GuardadoResult.OK : GuardadoResult.ERROR_BD;
    }

    // ── Foto de perfil ────────────────────────────────────────────────────────

    /**
     * Guarda la imagen de perfil en disco y actualiza la BD.
     * @param numDoc       identificador del admin
     * @param nombreArchivo nombre original del archivo (para validar extensión)
     * @param datos        bytes del archivo
     * @param fotoAnterior ruta anterior a borrar (puede ser null)
     * @return ruta relativa guardada, o null si hubo error
     */
    public String guardarFotoPerfil(int numDoc, String nombreArchivo,
                                     byte[] datos, String fotoAnterior) {
        if (datos == null || datos.length == 0)  return null;
        if (datos.length > MAX_FOTO_BYTES)        return null;

        String ext = extension(nombreArchivo).toLowerCase();
        if (!ext.equals("jpg") && !ext.equals("jpeg")
                && !ext.equals("png") && !ext.equals("webp"))
            return null;

        // Nombre único: numDoc_timestamp.ext
        String nombreUnico = numDoc + "_" + System.currentTimeMillis() + "." + ext;
        Path dir  = Paths.get(UPLOAD_DIR);
        Path dest = dir.resolve(nombreUnico);

        try {
            Files.createDirectories(dir);
            Files.write(dest, datos);

            // Actualizar BD
            if (!dao.updateFotoPerfil(numDoc, UPLOAD_DIR + nombreUnico)) {
                Files.deleteIfExists(dest);
                return null;
            }

            // Borrar foto anterior si existe
            if (fotoAnterior != null && !fotoAnterior.isBlank()) {
                try { Files.deleteIfExists(Paths.get(fotoAnterior)); }
                catch (Exception ignored) {}
            }

            return UPLOAD_DIR + nombreUnico;
        } catch (IOException e) {
            System.err.println("[ConfiguracionService] guardarFotoPerfil: " + e.getMessage());
            return null;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean esVacio(String s) {
        return s == null || s.isBlank();
    }

    private static String extension(String nombre) {
        if (nombre == null || !nombre.contains(".")) return "";
        return nombre.substring(nombre.lastIndexOf('.') + 1);
    }
}
