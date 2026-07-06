package com.saia.presentation.home;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import com.saia.model.Persona;
import com.saia.model.PersonalSeguridad;
import static com.saia.presentation.home.PersonalSeguridadPanel.BG_PAGE;
import static com.saia.presentation.home.PersonalSeguridadPanel.BORDER_C;
import static com.saia.presentation.home.PersonalSeguridadPanel.BORDER_ERR;
import static com.saia.presentation.home.PersonalSeguridadPanel.CARD_BG;
import static com.saia.presentation.home.PersonalSeguridadPanel.NAVY;
import static com.saia.presentation.home.PersonalSeguridadPanel.NAVY_LIGHT;
import static com.saia.presentation.home.PersonalSeguridadPanel.TEXT_DARK;

/**
 * Clase base con los componentes y helpers compartidos entre
 * {@link RegistroPanel} y {@link EditarPanel}.
 */
abstract class BaseFormPanel extends JPanel {

    protected final PersonalSeguridadPanel parent;

    // Campos personales
    protected JComboBox<String> cmbTipDoc;
    protected JTextField        txtNumDoc;
    protected JTextField        txtNombres;
    protected JTextField        txtApellidos;
    protected JTextField        txtEmail;
    protected JTextField        txtTelefono;
    protected JTextField        txtFechaNac;
    protected LocalDate         fechaSeleccionada;
    protected JComboBox<String> cmbTipSang;
    protected JComboBox<String> cmbGenero;

    // Opciones estáticas
    static final String[] TIPOS_SANGRE = {
        "-- Seleccione --", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
    };
    static final String[] GENEROS = {
        "-- Seleccione --", "Masculino", "Femenino", "No binario",
        "Género fluido", "Prefiero no decirlo"
    };

    // Campos de guardia
    protected JTextField        txtEmpresa;
    protected JComboBox<String> cmbTurno;
    protected JComboBox<String> cmbEstado;

    protected BaseFormPanel(PersonalSeguridadPanel parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        setBackground(BG_PAGE);
        setBorder(new EmptyBorder(20, 24, 20, 24));
    }

    // ── Tarjeta Datos Personales ──────────────────────────────────────────────
    protected JPanel buildPersonalCard(boolean numDocEditable) {
        JPanel card = formCard();
        card.setLayout(new BorderLayout());
        card.add(sectionTitle("\uD83D\uDC64  DATOS PERSONALES"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(14, 18, 14, 18));
        GridBagConstraints gc = defaultGbc();

        cmbTipDoc = new JComboBox<>(new String[]{
            "Cédula de Ciudadanía","Tarjeta de Identidad","Cédula de Extranjería","Pasaporte","NIT"
        });
        styleCombo(cmbTipDoc);
        addRow(form, gc, "Tipo de Documento: *", cmbTipDoc, 0);

        txtNumDoc = field("Ej: 1151940954");
        if (numDocEditable) applyNumericFilter(txtNumDoc);
        else { txtNumDoc.setEditable(false); txtNumDoc.setBackground(new Color(0xF5F5F5)); }
        addRow(form, gc, "N° Documento: *", txtNumDoc, 1);

        txtNombres   = field("Ej: Carlos Andrés");
        txtApellidos = field("Ej: López Martínez");
        txtEmail     = field("ejemplo@correo.com");
        txtTelefono  = field("Ej: 3001234567");
        applyNumericFilter(txtTelefono);

        addRow(form, gc, "Nombres: *",           txtNombres,   2);
        addRow(form, gc, "Apellidos: *",          txtApellidos, 3);
        addRow(form, gc, "Correo Electrónico: *", txtEmail,     4);
        addRow(form, gc, "Teléfono:",             txtTelefono,  5);
        addRow(form, gc, "Fecha de Nacimiento:",  buildDatePickerRow(), 6);

        // Tipo de sangre
        cmbTipSang = new JComboBox<>(TIPOS_SANGRE);
        styleCombo(cmbTipSang);
        addRow(form, gc, "Tipo de Sangre:",       cmbTipSang,   7);

        // Género
        cmbGenero = new JComboBox<>(GENEROS);
        styleCombo(cmbGenero);
        addRow(form, gc, "Género:",               cmbGenero,    8);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    // ── Tarjeta Datos del Guardia ─────────────────────────────────────────────
    protected JPanel buildGuardaCard() {
        JPanel card = formCard();
        card.setLayout(new BorderLayout());
        card.add(sectionTitle("\uD83D\uDEE1  DATOS DEL PERSONAL DE SEGURIDAD"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(14, 18, 14, 18));
        GridBagConstraints gc = defaultGbc();

        txtEmpresa = field("Ej: Seguridad Total S.A.S");
        cmbTurno   = new JComboBox<>(new String[]{
            "-- Seleccione turno --","Mañana","Tarde","Noche","Rotativo"
        });
        cmbEstado  = new JComboBox<>(new String[]{"Activo","Inactivo"});
        styleCombo(cmbTurno);
        styleCombo(cmbEstado);

        addRow(form, gc, "Empresa de Seguridad:", txtEmpresa, 0);
        addRow(form, gc, "Turno:",                cmbTurno,   1);
        addRow(form, gc, "Estado:",               cmbEstado,  2);

        GridBagConstraints fill = (GridBagConstraints) gc.clone();
        fill.gridy = 3; fill.weighty = 1.0; fill.gridwidth = 2;
        form.add(Box.createGlue(), fill);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    // ── Date picker ───────────────────────────────────────────────────────────
    private JPanel buildDatePickerRow() {
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.setOpaque(false);

        txtFechaNac = new JTextField("dd/mm/aaaa");
        txtFechaNac.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtFechaNac.setEditable(false);
        txtFechaNac.setForeground(new Color(0xAAAAAA));
        txtFechaNac.setBackground(Color.WHITE);
        txtFechaNac.setBorder(new LineBorder(BORDER_C, 1, true));
        txtFechaNac.setPreferredSize(new Dimension(0, 34));

        JButton btnCal = new JButton("\uD83D\uDCC5") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(NAVY);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,6,6));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnCal.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        btnCal.setForeground(Color.WHITE);
        btnCal.setOpaque(false); btnCal.setContentAreaFilled(false);
        btnCal.setBorderPainted(false); btnCal.setFocusPainted(false);
        btnCal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCal.setPreferredSize(new Dimension(34, 34));
        btnCal.addActionListener(e -> showCalendarPopup(btnCal));

        p.add(txtFechaNac, BorderLayout.CENTER);
        p.add(btnCal,      BorderLayout.EAST);
        return p;
    }

    protected void showCalendarPopup(Component owner) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Seleccionar fecha", true);
        dlg.setLayout(new BorderLayout(8, 8));
        dlg.getRootPane().setBorder(new EmptyBorder(14, 18, 14, 18));
        dlg.setResizable(false);

        SpinnerDateModel model = new SpinnerDateModel();
        if (fechaSeleccionada != null) {
            Calendar cal = Calendar.getInstance();
            cal.set(fechaSeleccionada.getYear(), fechaSeleccionada.getMonthValue()-1,
                    fechaSeleccionada.getDayOfMonth());
            model.setValue(cal.getTime());
        }
        JSpinner spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd/MM/yyyy"));
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        spinner.setPreferredSize(new Dimension(160, 34));

        JLabel lbl = new JLabel("Fecha de nacimiento:");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JButton ok     = new JButton("Aceptar");
        JButton cancel = new JButton("Cancelar");
        ok.setFont(new Font("Segoe UI", Font.BOLD, 12));
        ok.setBackground(NAVY); ok.setForeground(Color.WHITE);
        ok.setFocusPainted(false);
        cancel.setFocusPainted(false);

        ok.addActionListener(ev -> {
            java.util.Date d = (java.util.Date) spinner.getValue();
            Calendar cal = Calendar.getInstance(); cal.setTime(d);
            fechaSeleccionada = LocalDate.of(cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH));
            txtFechaNac.setText(fechaSeleccionada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            txtFechaNac.setForeground(new Color(0x333333));
            dlg.dispose();
        });
        cancel.addActionListener(ev -> dlg.dispose());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false); top.add(lbl); top.add(spinner);
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bot.setOpaque(false); bot.add(cancel); bot.add(ok);

        dlg.add(top, BorderLayout.CENTER);
        dlg.add(bot, BorderLayout.SOUTH);
        dlg.pack(); dlg.setLocationRelativeTo(owner); dlg.setVisible(true);
    }

    // ── Leer campos ───────────────────────────────────────────────────────────
    protected Persona leerPersona(boolean numDocEditable) {
        Persona p = new Persona();
        p.setTipDoc((String) cmbTipDoc.getSelectedItem());
        if (numDocEditable) {
            String nd = txtNumDoc.getText().trim();
            if (!nd.isEmpty()) p.setNumDoc(Integer.parseInt(nd));
        } else {
            String nd = txtNumDoc.getText().trim();
            if (!nd.isEmpty()) p.setNumDoc(Integer.parseInt(nd));
        }
        p.setNombres(val(txtNombres, "Ej: Carlos Andrés"));
        String apes = val(txtApellidos, "Ej: López Martínez");
        String[] parts = apes.split("\\s+", 2);
        p.setPApe(parts.length > 0 ? parts[0] : "");
        p.setSApe(parts.length > 1 ? parts[1] : null);
        p.setEmail(val(txtEmail, "ejemplo@correo.com"));
        String tel = val(txtTelefono, "Ej: 3001234567");
        if (!tel.isEmpty()) p.setTel(tel);
        if (fechaSeleccionada != null) p.setFechaNac(fechaSeleccionada);

        // Tipo de sangre
        String ts = (String) cmbTipSang.getSelectedItem();
        if (ts != null && !ts.equals("-- Seleccione --")) p.setTipSang(ts);

        // Género
        String gen = (String) cmbGenero.getSelectedItem();
        if (gen != null && !gen.equals("-- Seleccione --")) p.setGenero(gen);

        return p;
    }

    protected PersonalSeguridad leerGuardia() {
        PersonalSeguridad g = new PersonalSeguridad();
        String turno = (String) cmbTurno.getSelectedItem();
        g.setTurno("-- Seleccione turno --".equals(turno) ? null : turno);
        g.setEmpresaSeg(val(txtEmpresa, "Ej: Seguridad Total S.A.S"));
        return g;
    }

    protected String val(JTextField f, String ph) {
        String v = f.getText().trim();
        return v.equals(ph) ? "" : v;
    }

    protected void markError(JTextField f) { f.setBorder(new LineBorder(BORDER_ERR, 2, true)); }
    protected void clearError(JTextField f){ f.setBorder(new LineBorder(BORDER_C,   1, true)); }

    protected boolean validateEmail(String e) {
        return e.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────
    protected JPanel formCard() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0,0,0,14));
                g2.fill(new RoundRectangle2D.Float(2,3,getWidth()-3,getHeight()-3,10,10));
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth()-2,getHeight()-2,10,10));
                g2.setColor(BORDER_C); g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f,0.5f,getWidth()-3,getHeight()-3,10,10));
                g2.dispose(); super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
    }

    protected JPanel sectionTitle(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(NAVY);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),10,10));
                g2.fillRect(0, getHeight()/2, getWidth(), getHeight()/2);
                g2.dispose(); super.paintComponent(g);
            }
        };
        p.setOpaque(false); p.setPreferredSize(new Dimension(0, 40));
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(Color.WHITE);
        p.add(lbl);
        return p;
    }

    protected JTextField field(String ph) {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setText(ph); f.setForeground(new Color(0xAAAAAA));
        f.setBorder(new LineBorder(BORDER_C, 1, true));
        f.setPreferredSize(new Dimension(0, 34));
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (f.getText().equals(ph)) { f.setText(""); f.setForeground(new Color(0x333333)); }
                f.setBorder(new LineBorder(NAVY_LIGHT, 2, true));
            }
            @Override public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) { f.setText(ph); f.setForeground(new Color(0xAAAAAA)); }
                f.setBorder(new LineBorder(BORDER_C, 1, true));
            }
        });
        return f;
    }

    protected void styleCombo(JComboBox<String> c) {
        c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        c.setBackground(Color.WHITE);
        c.setBorder(new LineBorder(BORDER_C, 1, true));
        c.setPreferredSize(new Dimension(0, 34));
        c.setFocusable(false);
    }

    protected JButton actionBtn(String text, Color bg, Color fg, int w) {
        JButton btn = new JButton(text) {
            boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hov=true; repaint(); }
                @Override public void mouseExited (MouseEvent e) { hov=false;repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? bg.darker() : bg);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,8,8));
                g2.dispose(); super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(fg); btn.setOpaque(false);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(w, 38));
        return btn;
    }

    protected void addRow(JPanel form, GridBagConstraints gc, String label, Component comp, int row) {
        gc.gridy = row; gc.gridx = 0; gc.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(TEXT_DARK);
        form.add(lbl, gc);
        gc.gridx = 1; gc.weightx = 1.0;
        form.add(comp, gc);
    }

    protected GridBagConstraints defaultGbc() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(5, 6, 5, 6);
        gc.weightx = 0; gc.weighty = 0;
        return gc;
    }

    protected void applyNumericFilter(JTextField f) {
        ((AbstractDocument) f.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void insertString(FilterBypass fb, int off, String s, AttributeSet a)
                    throws BadLocationException {
                if (s != null && s.matches("\\d+")) super.insertString(fb, off, s, a);
            }
            @Override public void replace(FilterBypass fb, int off, int len, String s, AttributeSet a)
                    throws BadLocationException {
                if (s != null && s.matches("\\d*")) super.replace(fb, off, len, s, a);
            }
        });
    }
}
