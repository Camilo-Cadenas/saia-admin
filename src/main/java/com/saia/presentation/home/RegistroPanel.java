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
        JButton btnVolver = actionBtn("\u2190  Volver al listado", BTN_CANCEL, Color.WHITE, 170);
        btnVolver.addActionListener(e -> parent.mostrar("LISTA"));

        JLabel titulo = new JLabel("\uD83D\uDC6E  Nuevo Personal de Seguridad");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titulo.setForeground(TEXT_DARK);

        left.add(btnVolver);
        left.add(titulo);
        p.add(left, BorderLayout.WEST);
        return p;
    }

    private JPanel buildFooter() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 4, 0));

        JButton btnGuardar  = actionBtn("\uD83D\uDCBE  GUARDAR", NAVY,       Color.WHITE, 140);
        JButton btnLimpiar  = actionBtn("\uD83E\uDDF9  LIMPIAR", new Color(0x4A6FA5), Color.WHITE, 130);
        JButton btnCancelar = actionBtn("\u2715  CANCELAR",      BTN_CANCEL, Color.WHITE, 130);

        btnGuardar.addActionListener(e -> guardar());
        btnLimpiar.addActionListener(e -> limpiar());
        btnCancelar.addActionListener(e -> { limpiar(); parent.mostrar("LISTA"); });

        p.add(btnGuardar);
        p.add(btnLimpiar);
        p.add(btnCancelar);
        return p;
    }

    private void guardar() {
        // ── Validaciones ──────────────────────────────────────────────────────
        String tipDoc = (String) cmbTipDoc.getSelectedItem();

        String numDocStr = txtNumDoc.getText().trim();
        if (numDocStr.isEmpty() || numDocStr.equals("Ej: 1151940954")) {
            markError(txtNumDoc); showError("El número de documento es obligatorio."); return;
        }
        int numDoc;
        try { numDoc = Integer.parseInt(numDocStr); }
        catch (NumberFormatException ex) {
            markError(txtNumDoc); showError("El número de documento debe ser numérico."); return;
        }
        clearError(txtNumDoc);

        String nombres = val(txtNombres, "Ej: Carlos Andrés");
        if (nombres.isEmpty()) { markError(txtNombres); showError("Los nombres son obligatorios."); return; }
        clearError(txtNombres);

        String apelRaw = val(txtApellidos, "Ej: López Martínez");
        if (apelRaw.isEmpty()) { markError(txtApellidos); showError("El apellido es obligatorio."); return; }
        clearError(txtApellidos);

        String email = val(txtEmail, "ejemplo@correo.com");
        if (email.isEmpty()) { markError(txtEmail); showError("El correo electrónico es obligatorio."); return; }
        if (!validateEmail(email)) {
            markError(txtEmail);
            showError("El correo electrónico no tiene un formato válido.\nEjemplo: usuario@dominio.com"); return;
        }
        clearError(txtEmail);

        // ── Construir Persona con TODOS los campos ────────────────────────────
        Persona persona = new Persona();
        persona.setTipDoc(tipDoc);
        persona.setNumDoc(numDoc);
        persona.setNombres(nombres);
        String[] apes = apelRaw.split("\\s+", 2);
        persona.setPApe(apes[0]);
        persona.setSApe(apes.length > 1 ? apes[1] : null);
        persona.setEmail(email);

        // Teléfono
        String tel = val(txtTelefono, "Ej: 3001234567");
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
        String empresa = val(txtEmpresa, "Ej: Seguridad Total S.A.S");
        guardia.setEmpresaSeg(empresa.isEmpty() ? null : empresa);

        // Estado de la cuenta (Activo/Inactivo)
        boolean activo = !"Inactivo".equals(cmbEstado.getSelectedItem());

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
                btnGuardar.setText("\uD83D\uDCBE  GUARDAR");
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
        resetField(txtNumDoc,    "Ej: 1151940954");
        resetField(txtNombres,   "Ej: Carlos Andrés");
        resetField(txtApellidos, "Ej: López Martínez");
        resetField(txtEmail,     "ejemplo@correo.com");
        resetField(txtTelefono,  "Ej: 3001234567");
        resetField(txtFechaNac,  "dd/mm/aaaa");
        fechaSeleccionada = null;
        cmbTipSang.setSelectedIndex(0);
        cmbGenero.setSelectedIndex(0);
        resetField(txtEmpresa,   "Ej: Seguridad Total S.A.S");
        cmbTurno.setSelectedIndex(0);
        cmbEstado.setSelectedIndex(0);
    }

    private void resetField(JTextField f, String ph) {
        if (f == null) return;
        f.setText(ph);
        f.setForeground(new java.awt.Color(0xAAAAAA));
        f.setBorder(new javax.swing.border.LineBorder(BORDER_C, 1, true));
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error de validación", JOptionPane.WARNING_MESSAGE);
    }
}
