package com.saia.presentation.home;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import com.saia.business.PersonalSeguridadService.RegistroResult;
import com.saia.model.Persona;
import com.saia.model.PersonalSeguridad;
import static com.saia.presentation.home.PersonalSeguridadPanel.BORDER_C;
import static com.saia.presentation.home.PersonalSeguridadPanel.BTN_CANCEL;
import static com.saia.presentation.home.PersonalSeguridadPanel.NAVY;
import static com.saia.presentation.home.PersonalSeguridadPanel.TEXT_DARK;

/**
 * Sub-panel REGISTRO: formulario de alta de nuevo personal de seguridad.
 */
class RegistroPanel extends BaseFormPanel {

    RegistroPanel(PersonalSeguridadPanel parent) {
        super(parent);
        buildUI();
    }

    private void buildUI() {
        // Encabezado
        add(buildEncabezado(), BorderLayout.NORTH);

        // Dos tarjetas en columnas
        JPanel body = new JPanel(new GridLayout(1, 2, 18, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(14, 0, 14, 0));
        body.add(buildPersonalCard(true));   // num_doc editable
        body.add(buildGuardaCard());
        add(body, BorderLayout.CENTER);

        // Botones
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildEncabezado() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 10, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        // Botón volver
        JButton btnVolver = actionBtn("  Volver al listado", BTN_CANCEL, Color.WHITE, 170);
        btnVolver.setIcon(com.saia.presentation.IconUtil.back());
        btnVolver.addActionListener(e -> parent.mostrar("LISTA"));

        JLabel titulo = new JLabel("  Nuevo Personal de Seguridad");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titulo.setForeground(TEXT_DARK);
        titulo.setIcon(com.saia.presentation.IconUtil.icon(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.USER_SHIELD,
            18, com.saia.presentation.UITheme.PRIMARY));

        left.add(btnVolver);
        left.add(titulo);
        p.add(left, BorderLayout.WEST);
        return p;
    }

    private JPanel buildFooter() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 4, 0));

        JButton btnGuardar  = actionBtn("  GUARDAR",   NAVY,                  Color.WHITE, 140);
        JButton btnLimpiar  = actionBtn("  LIMPIAR",   new Color(0x4A6FA5),   Color.WHITE, 130);
        JButton btnCancelar = actionBtn("  CANCELAR",  BTN_CANCEL,            Color.WHITE, 130);
        btnGuardar.setIcon(com.saia.presentation.IconUtil.save());
        btnLimpiar.setIcon(com.saia.presentation.IconUtil.refresh());
        btnCancelar.setIcon(com.saia.presentation.IconUtil.icon(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.TIMES_CIRCLE, 14, Color.WHITE));

        btnGuardar.addActionListener(e -> guardar());
        btnLimpiar.addActionListener(e -> limpiar());
        btnCancelar.addActionListener(e -> { limpiar(); parent.mostrar("LISTA"); });

        p.add(btnGuardar);
        p.add(btnLimpiar);
        p.add(btnCancelar);
        return p;
    }

    private void guardar() {
        // ── Validaciones de Seguridad ─────────────────────────────────────────
        String tipDoc = (String) cmbTipDoc.getSelectedItem();

        // Validar número de documento
        String numDocStr = txtNumDoc.getText().trim();
        if (numDocStr.isEmpty() || numDocStr.equals("0123456789")) {
            markError(txtNumDoc); 
            showError("El número de documento es obligatorio."); 
            return;
        }
        
        // Validación de seguridad: verificar formato y rango
        if (!InputValidator.isValidNumDoc(numDocStr)) {
            markError(txtNumDoc);
            showError("El número de documento debe ser un número válido.\n" +
                     "Máximo: " + InputValidator.MAX_INT_VALUE);
            return;
        }
        
        int numDoc;
        try { 
            numDoc = Integer.parseInt(numDocStr); 
        } catch (NumberFormatException ex) {
            markError(txtNumDoc); 
            showError(InputValidator.getNumericErrorMessage("Número de documento")); 
            return;
        }
        clearError(txtNumDoc);

        // Validar nombres
        String nombres = val(txtNombres, "Personal Seguridad");
        if (nombres.isEmpty()) { 
            markError(txtNombres); 
            showError("Los nombres son obligatorios."); 
            return; 
        }
        if (!InputValidator.isValidLength(nombres, InputValidator.MAX_NOMBRES)) {
            markError(txtNombres);
            showError(InputValidator.getLengthErrorMessage("Nombres", InputValidator.MAX_NOMBRES));
            return;
        }
        clearError(txtNombres);

        // Validar apellidos
        String apelRaw = val(txtApellidos, "Personal Seguridad");
        if (apelRaw.isEmpty()) { 
            markError(txtApellidos); 
            showError("El apellido es obligatorio."); 
            return; 
        }
        if (!InputValidator.isValidLength(apelRaw, InputValidator.MAX_APELLIDOS)) {
            markError(txtApellidos);
            showError(InputValidator.getLengthErrorMessage("Apellidos", InputValidator.MAX_APELLIDOS));
            return;
        }
        clearError(txtApellidos);

        // Validar email
        String email = val(txtEmail, "ejemplo@correo.com");
        if (email.isEmpty()) { 
            markError(txtEmail); 
            showError("El correo electrónico es obligatorio."); 
            return; 
        }
        if (!InputValidator.isValidEmail(email)) {
            markError(txtEmail);
            showError("El correo electrónico no tiene un formato válido.\n" +
                     "Ejemplo: usuario@dominio.com\n" +
                     "Máximo: " + InputValidator.MAX_EMAIL + " caracteres");
            return;
        }
        clearError(txtEmail);

        // Validar teléfono (opcional)
        String tel = val(txtTelefono, "3001234567");
        if (!tel.isEmpty() && !InputValidator.isValidTelefono(tel)) {
            markError(txtTelefono);
            showError("El teléfono debe contener entre 7 y 10 dígitos numéricos.");
            return;
        }
        clearError(txtTelefono);

        // ── Construir Persona con TODOS los campos ────────────────────────────
        Persona persona = new Persona();
        persona.setTipDoc(tipDoc);
        persona.setNumDoc(numDoc);
        
        // Sanitizar datos antes de guardar (capa adicional de seguridad)
        persona.setNombres(InputValidator.sanitize(nombres));
        
        String[] apes = apelRaw.split("\\s+", 2);
        persona.setPApe(InputValidator.sanitize(apes[0]));
        persona.setSApe(apes.length > 1 ? InputValidator.sanitize(apes[1]) : null);
        
        persona.setEmail(email.toLowerCase().trim()); // Email siempre en minúsculas

        // Teléfono
        if (!tel.isEmpty()) persona.setTel(tel);

        // Fecha de nacimiento
        if (fechaSeleccionada != null) persona.setFechaNac(fechaSeleccionada);

        // Tipo de sangre
        String tipSang = (String) cmbTipSang.getSelectedItem();
        if (tipSang != null && !tipSang.equals("-- Seleccione --")) persona.setTipSang(tipSang);

        // Género
        String genero = (String) cmbGenero.getSelectedItem();
        if (genero != null && !genero.equals("-- Seleccione --")) persona.setGenero(genero);

        // ── Construir PersonalSeguridad (turno y empresa) ─────────────────────
        PersonalSeguridad guardia = new PersonalSeguridad();
        String turnoSel = (String) cmbTurno.getSelectedItem();
        guardia.setTurno("-- Seleccione turno --".equals(turnoSel) ? null : turnoSel);
        
        String empresa = val(txtEmpresa, "Empresa Seguridad");
        if (!empresa.isEmpty()) {
            if (!InputValidator.isValidLength(empresa, InputValidator.MAX_EMPRESA)) {
                markError(txtEmpresa);
                showError(InputValidator.getLengthErrorMessage("Empresa de Seguridad", InputValidator.MAX_EMPRESA));
                return;
            }
            guardia.setEmpresaSeg(InputValidator.sanitize(empresa));
        } else {
            guardia.setEmpresaSeg(null);
        }
        clearError(txtEmpresa);

        // Estado de la cuenta (Activo/Inactivo)
        boolean activo = !"Inactivo".equals(cmbEstado.getSelectedItem());

        // ── Guardar foto de perfil si se seleccionó ───────────────────────────
        String rutaFotoGuardada = null;
        if (fotoBytes != null && fotoNombreArchivo != null) {
            rutaFotoGuardada = guardarArchivoFoto(numDoc, fotoNombreArchivo, fotoBytes);
            
            if (rutaFotoGuardada == null) {
                showError("Error al guardar la foto de perfil. Verifique el formato y tamaño.");
                return;
            }
            
            // Asignar ruta de foto a la persona
            persona.setFotoPerfil(rutaFotoGuardada);
        }

        // ── Guardar en hilo separado ──────────────────────────────────────────
        JButton btnGuardar = (JButton)((JPanel) getComponent(2)).getComponent(0);
        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        final int numDocFinal = numDoc;
        SwingWorker<RegistroResult, Void> w = new SwingWorker<>() {
            @Override protected RegistroResult doInBackground() {
                return parent.getService().registrar(persona, guardia, activo);
            }
            @Override protected void done() {
                btnGuardar.setEnabled(true);
                btnGuardar.setText("  GUARDAR");
                try {
                    switch (get()) {
                        case OK -> {
                            JOptionPane.showMessageDialog(RegistroPanel.this,
                                "<html><b>" + persona.getNombres() + " " + persona.getPApe()
                                + "</b> registrado correctamente.<br><br>"
                                + "Contraseña temporal: <b>Temp" + numDocFinal + "</b></html>",
                                "Registro exitoso", JOptionPane.INFORMATION_MESSAGE);
                            limpiar();
                            parent.mostrar("LISTA");
                        }
                        case DOC_DUPLICADO   -> showError("Ya existe un usuario con ese número de documento.");
                        case EMAIL_DUPLICADO -> showError("El correo electrónico ya está en uso.");
                        case CAMPO_REQUERIDO -> showError("Complete todos los campos obligatorios (*).");
                        case ERROR_BD        -> showError("Error al guardar. Intente nuevamente.");
                    }
                } catch (InterruptedException | java.util.concurrent.ExecutionException ex) {
                    showError("Error inesperado: " + ex.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        };
        w.execute();
    }

    private void limpiar() {
        cmbTipDoc.setSelectedIndex(0);
        resetField(txtNumDoc,    "0123456789");
        resetField(txtNombres,   "Personal Seguridad");
        resetField(txtApellidos, "Personal Seguridad");
        resetField(txtEmail,     "ejemplo@correo.com");
        resetField(txtTelefono,  "3001234567");
        resetField(txtFechaNac,  "dd/mm/aaaa");
        fechaSeleccionada = null;
        cmbTipSang.setSelectedIndex(0);
        cmbGenero.setSelectedIndex(0);
        resetField(txtEmpresa,   "Empresa Seguridad");
        cmbTurno.setSelectedIndex(0);
        cmbEstado.setSelectedIndex(0);
        
        // Limpiar foto de perfil
        eliminarFoto();
    }

    /**
     * Restablece un campo de texto a su estado inicial con placeholder.
     * 
     * @param f Campo de texto
     * @param ph Texto del placeholder
     */
    private void resetField(JTextField f, String ph) {
        if (f == null) return;
        f.setText(ph);
        f.setForeground(new java.awt.Color(0xAAAAAA));
        f.setBorder(new javax.swing.border.LineBorder(BORDER_C, 1, true));
        // Asegurar que el caret no esté visible cuando está el placeholder
        f.getCaret().setVisible(false);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error de validación", JOptionPane.WARNING_MESSAGE);
    }
    
    /**
     * Guarda el archivo de foto en disco y retorna la ruta relativa.
     * 
     * @param numDoc Número de documento del personal
     * @param nombreArchivo Nombre original del archivo
     * @param datos Bytes del archivo
     * @return Ruta relativa del archivo guardado, o null si hubo error
     */
    private String guardarArchivoFoto(int numDoc, String nombreArchivo, byte[] datos) {
        if (datos == null || datos.length == 0) return null;
        
        // Validar tamaño máximo (2 MB)
        if (datos.length > 2 * 1024 * 1024) return null;
        
        // Validar extensión
        String ext = obtenerExtension(nombreArchivo).toLowerCase();
        if (!ext.equals("jpg") && !ext.equals("jpeg") 
                && !ext.equals("png") && !ext.equals("webp")) {
            return null;
        }
        
        // Crear directorio si no existe
        String uploadDir = "uploads/perfiles/";
        java.nio.file.Path dir = java.nio.file.Paths.get(uploadDir);
        try {
            java.nio.file.Files.createDirectories(dir);
        } catch (java.io.IOException e) {
            System.err.println("Error creando directorio: " + e.getMessage());
            return null;
        }
        
        // Nombre único: numDoc_timestamp.ext
        String nombreUnico = numDoc + "_" + System.currentTimeMillis() + "." + ext;
        java.nio.file.Path destino = dir.resolve(nombreUnico);
        
        try {
            java.nio.file.Files.write(destino, datos);
            return uploadDir + nombreUnico;
        } catch (java.io.IOException e) {
            System.err.println("Error guardando foto: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Obtiene la extensión de un archivo.
     * 
     * @param nombreArchivo Nombre del archivo
     * @return Extensión sin el punto, o cadena vacía si no tiene
     */
    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) return "";
        return nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1);
    }
}
