package com.saia.presentation.home;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import com.saia.business.PersonalSeguridadService.EditarResult;
import com.saia.model.Persona;
import com.saia.model.PersonalSeguridad;
import static com.saia.presentation.home.PersonalSeguridadPanel.BORDER_C;
import static com.saia.presentation.home.PersonalSeguridadPanel.BTN_CANCEL;
import static com.saia.presentation.home.PersonalSeguridadPanel.NAVY;
import static com.saia.presentation.home.PersonalSeguridadPanel.TEXT_DARK;

/**
 * Sub-panel EDITAR: carga los datos de un guardia existente y permite modificarlos.
 */
class EditarPanel extends BaseFormPanel {

    private int numDocActual = 0;

    EditarPanel(PersonalSeguridadPanel parent) {
        super(parent);
        buildUI();
    }

    private void buildUI() {
        add(buildEncabezado(), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(1, 2, 18, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(14, 0, 14, 0));
        body.add(buildPersonalCard(false));  // num_doc NO editable
        body.add(buildGuardaCard());
        add(body, BorderLayout.CENTER);

        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildEncabezado() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 10, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JButton btnVolver = actionBtn("\u2190  Volver al listado", BTN_CANCEL, Color.WHITE, 170);
        btnVolver.addActionListener(e -> parent.mostrar("LISTA"));

        JLabel titulo = new JLabel("\u270E  Editar Personal de Seguridad");
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

        JButton btnGuardar  = actionBtn("\uD83D\uDCBE  GUARDAR CAMBIOS", NAVY,       Color.WHITE, 165);
        JButton btnCancelar = actionBtn("\u2715  CANCELAR",              BTN_CANCEL, Color.WHITE, 120);

        btnGuardar.addActionListener(e -> guardar());
        btnCancelar.addActionListener(e -> parent.mostrar("LISTA"));

        p.add(btnGuardar);
        p.add(btnCancelar);
        return p;
    }

    /** Carga los datos del guardia en los campos del formulario. */
    void cargar(PersonalSeguridad g) {
        numDocActual = g.getNumDoc();
        Persona p = g.getPersona();
        if (p == null) return;

        // Tipo de documento
        String td = p.getTipDoc() != null ? p.getTipDoc() : "Cédula de Ciudadanía";
        for (int i = 0; i < cmbTipDoc.getItemCount(); i++) {
            if (cmbTipDoc.getItemAt(i).equals(td)) { cmbTipDoc.setSelectedIndex(i); break; }
        }

        // num_doc (solo lectura)
        txtNumDoc.setText(String.valueOf(g.getNumDoc()));
        txtNumDoc.setForeground(new Color(0x333333));

        // Datos personales
        setField(txtNombres,   p.getNombres(), "Ej: Carlos Andrés");
        String apes = (p.getPApe() != null ? p.getPApe() : "")
                    + (p.getSApe() != null && !p.getSApe().isBlank() ? " " + p.getSApe() : "");
        setField(txtApellidos, apes.trim(), "Ej: López Martínez");
        setField(txtEmail,     p.getEmail(), "ejemplo@correo.com");
        setField(txtTelefono,  p.getTel()  != null ? p.getTel() : "", "Ej: 3001234567");

        // Fecha de nacimiento
        if (p.getFechaNac() != null) {
            fechaSeleccionada = p.getFechaNac();
            txtFechaNac.setText(fechaSeleccionada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            txtFechaNac.setForeground(new Color(0x333333));
        } else {
            fechaSeleccionada = null;
            txtFechaNac.setText("dd/mm/aaaa");
            txtFechaNac.setForeground(new Color(0xAAAAAA));
        }

        // Tipo de sangre
        selectCombo(cmbTipSang, p.getTipSang());

        // Género
        selectCombo(cmbGenero, p.getGenero());

        // Datos de guardia
        setField(txtEmpresa, g.getEmpresaSeg() != null ? g.getEmpresaSeg() : "",
                "Ej: Seguridad Total S.A.S");

        // Turno
        cmbTurno.setSelectedIndex(0);
        if (g.getTurno() != null) {
            for (int i = 0; i < cmbTurno.getItemCount(); i++) {
                if (cmbTurno.getItemAt(i).equals(g.getTurno())) {
                    cmbTurno.setSelectedIndex(i); break;
                }
            }
        }

        // Estado real desde la BD (consultar tabla cuenta)
        try {
            com.saia.data.CuentaDAO cuentaDAO = new com.saia.data.CuentaDAO();
            cuentaDAO.findByNumDoc(g.getNumDoc()).ifPresent(cuenta ->
                cmbEstado.setSelectedItem(cuenta.isEstado() ? "Activo" : "Inactivo")
            );
        } catch (Exception ex) {
            cmbEstado.setSelectedIndex(0); // fallback Activo
        }
    }

    private void setField(JTextField f, String value, String placeholder) {
        if (value != null && !value.isBlank()) {
            f.setText(value);
            f.setForeground(new Color(0x333333));
        } else {
            f.setText(placeholder);
            f.setForeground(new Color(0xAAAAAA));
        }
        f.setBorder(new LineBorder(BORDER_C, 1, true));
    }

    /** Selecciona el item del combo que coincida con el valor dado. */
    private void selectCombo(JComboBox<String> cmb, String value) {
        cmb.setSelectedIndex(0); // por defecto "-- Seleccione --"
        if (value != null && !value.isBlank()) {
            for (int i = 0; i < cmb.getItemCount(); i++) {
                if (cmb.getItemAt(i).equals(value)) {
                    cmb.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    private void guardar() {
        // Validar campos obligatorios
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
            showError("El formato del correo no es válido.\nEjemplo: usuario@dominio.com"); return;
        }
        clearError(txtEmail);

        // Construir objetos
        Persona persona = new Persona();
        persona.setNumDoc(numDocActual);
        persona.setTipDoc((String) cmbTipDoc.getSelectedItem());
        persona.setNombres(nombres);
        String[] apes = apelRaw.split("\\s+", 2);
        persona.setPApe(apes[0]);
        persona.setSApe(apes.length > 1 ? apes[1] : null);
        persona.setEmail(email);
        String tel = val(txtTelefono, "Ej: 3001234567");
        if (!tel.isEmpty()) persona.setTel(tel);
        if (fechaSeleccionada != null) persona.setFechaNac(fechaSeleccionada);

        // Tipo de sangre y género desde los combos
        String tipSang = (String) cmbTipSang.getSelectedItem();
        if (tipSang != null && !tipSang.equals("-- Seleccione --")) persona.setTipSang(tipSang);

        String genero = (String) cmbGenero.getSelectedItem();
        if (genero != null && !genero.equals("-- Seleccione --")) persona.setGenero(genero);

        PersonalSeguridad guardia = new PersonalSeguridad();
        guardia.setNumDoc(numDocActual);
        String turno = (String) cmbTurno.getSelectedItem();
        guardia.setTurno("-- Seleccione turno --".equals(turno) ? null : turno);
        String empresa = val(txtEmpresa, "Ej: Seguridad Total S.A.S");
        guardia.setEmpresaSeg(empresa.isEmpty() ? null : empresa);

        // Estado de la cuenta
        boolean activo = !"Inactivo".equals(cmbEstado.getSelectedItem());

        JButton btnGuardar = (JButton)((JPanel) getComponent(2)).getComponent(0);
        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        SwingWorker<EditarResult, Void> w = new SwingWorker<>() {
            @Override protected EditarResult doInBackground() {
                return parent.getService().editar(persona, guardia, activo);
            }
            @Override protected void done() {
                btnGuardar.setEnabled(true);
                btnGuardar.setText("\uD83D\uDCBE  GUARDAR CAMBIOS");
                try {
                    switch (get()) {
                        case OK -> {
                            JOptionPane.showMessageDialog(EditarPanel.this,
                                "<html>Los datos de <b>" + persona.getNombres() + " "
                                + persona.getPApe() + "</b> fueron actualizados correctamente.</html>",
                                "Edición exitosa", JOptionPane.INFORMATION_MESSAGE);
                            parent.mostrar("LISTA");
                        }
                        case EMAIL_DUPLICADO -> showError("El correo electrónico ya está en uso por otro usuario.");
                        case NO_ENCONTRADO   -> showError("No se encontró el guardia en la base de datos.");
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

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error de validación", JOptionPane.WARNING_MESSAGE);
    }
}
