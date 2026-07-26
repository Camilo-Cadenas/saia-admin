package com.saia.presentation.home;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.saia.business.ConfiguracionService;
import com.saia.business.ConfiguracionService.GuardadoResult;
import com.saia.data.ConfiguracionDAO.PerfilAdmin;
import com.saia.model.Persona;
import com.saia.presentation.IconUtil;
import com.saia.presentation.UITheme;
import com.saia.util.SessionManager;

/**
 * Panel "Configuración del Sistema" — Edición del perfil del administrador.
 * Secciones:
 *   1. Foto de perfil (avatar + selector de imagen)
 *   2. Datos personales (tip_doc, nombres, apellidos, tel, tip_sang, genero, fecha_nac)
 *   3. Cuenta y seguridad (mail + cambio de contraseña)
 */
public class ConfiguracionPanel extends JPanel {

    // ── Paleta ────────────────────────────────────────────────────────────────
    private static final Color BG     = UITheme.BG_SECONDARY;
    private static final Color CARD   = UITheme.BG_WHITE;
    private static final Color BORDER = UITheme.BORDER;
    private static final Color TXT_D  = UITheme.TEXT_PRIMARY;
    private static final Color TXT_G  = UITheme.TEXT_SECONDARY;
    private static final Color GREEN  = UITheme.PRIMARY;
    private static final Color RED    = UITheme.ERROR;

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ConfiguracionService service = new ConfiguracionService();

    // ── Estado ────────────────────────────────────────────────────────────────
    private PerfilAdmin perfilActual;

    // ── Sección foto ──────────────────────────────────────────────────────────
    private JLabel lblAvatar;
    private JLabel lblFotoInfo;

    // ── Sección datos personales ──────────────────────────────────────────────
    private JComboBox<String> cmbTipDoc;
    private JTextField txtNumDoc, txtNombres, txtApellidos;
    private JTextField txtTelefono, txtEmail;
    private JTextField txtFechaNac;
    private LocalDate  fechaSeleccionada;
    private JComboBox<String> cmbTipSang, cmbGenero;

    // ── Sección cuenta ────────────────────────────────────────────────────────
    private JTextField     txtMail;
    private JPasswordField txtPassActual, txtPassNueva, txtPassConfirm;

    // ── Feedback ──────────────────────────────────────────────────────────────
    private JLabel lblStatusPersonal, lblStatusCuenta, lblStatusFoto;

    public ConfiguracionPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(16, 24, 16, 24));
        buildUI();
    }

    @Override public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::cargarPerfil);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BUILD UI
    // ═════════════════════════════════════════════════════════════════════════
    private void buildUI() {
        // Encabezado
        JPanel head = new JPanel(new BorderLayout(0, 4));
        head.setOpaque(false);
        head.setBorder(new EmptyBorder(0, 0, 16, 0));
        JLabel titulo = new JLabel("  Configuración del Sistema");
        titulo.setFont(UITheme.FONT_PAGE_TITLE);
        titulo.setForeground(TXT_D);
        titulo.setIcon(IconUtil.navConfig());
        JLabel sub = new JLabel("Administra tu perfil, foto y credenciales de acceso.");
        sub.setFont(UITheme.FONT_BODY);
        sub.setForeground(TXT_G);
        head.add(titulo, BorderLayout.NORTH);
        head.add(sub,    BorderLayout.CENTER);
        add(head, BorderLayout.NORTH);

        // Contenido scrollable
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setOpaque(false);
        root.add(buildFotoCard());
        root.add(vgap(14));
        root.add(buildPersonalCard());
        root.add(vgap(14));
        root.add(buildCuentaCard());
        root.add(vgap(10));

        JScrollPane scroll = new JScrollPane(root);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
    }

    // ── Sección 1: Foto de perfil ─────────────────────────────────────────────
    private JPanel buildFotoCard() {
        JPanel c = card();
        c.setLayout(new BorderLayout(20, 0));
        c.setBorder(new EmptyBorder(20, 24, 20, 24));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        // Avatar circular
        lblAvatar = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.PRIMARY_PALE);
                g2.fillOval(0, 0, getWidth(), getHeight());
                if (getIcon() != null) {
                    // Clip circular para la imagen
                    g2.setClip(new Ellipse2D.Float(0, 0, getWidth(), getHeight()));
                    getIcon().paintIcon(this, g2, 0, 0);
                } else {
                    // Avatar por defecto
                    g2.setColor(GREEN);
                    int cx = getWidth()/2, cy = getHeight()/2;
                    g2.fillOval(cx-18, cy-28, 36, 36);
                    g2.fillArc (cx-26, cy+10, 52, 36, 0, 180);
                }
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(2f));
                g2.setClip(null);
                g2.drawOval(1, 1, getWidth()-3, getHeight()-3);
                g2.dispose();
            }
        };
        lblAvatar.setPreferredSize(new Dimension(90, 90));
        lblAvatar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblAvatar.setToolTipText("Haz clic para cambiar la foto");
        lblAvatar.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { seleccionarFoto(); }
        });

        JPanel infoFoto = new JPanel();
        infoFoto.setLayout(new BoxLayout(infoFoto, BoxLayout.Y_AXIS));
        infoFoto.setOpaque(false);
        JLabel lFoto = new JLabel("Foto de perfil");
        lFoto.setFont(UITheme.FONT_SECTION);
        lFoto.setForeground(TXT_D);
        lblFotoInfo = new JLabel("JPG, PNG o WebP · máx. 2 MB");
        lblFotoInfo.setFont(UITheme.FONT_CAPTION);
        lblFotoInfo.setForeground(TXT_G);
        lblStatusFoto = new JLabel(" ");
        lblStatusFoto.setFont(UITheme.FONT_CAPTION);

        JButton btnSubir = UITheme.primaryButton("  Cambiar foto", 140, 36);
        btnSubir.setIcon(IconUtil.icon(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.UPLOAD, 12, Color.WHITE));
        btnSubir.addActionListener(e -> seleccionarFoto());

        infoFoto.add(lFoto);
        infoFoto.add(Box.createVerticalStrut(4));
        infoFoto.add(lblFotoInfo);
        infoFoto.add(Box.createVerticalStrut(6));
        infoFoto.add(lblStatusFoto);
        infoFoto.add(Box.createVerticalStrut(8));
        infoFoto.add(btnSubir);

        c.add(lblAvatar, BorderLayout.WEST);
        c.add(infoFoto,  BorderLayout.CENTER);
        return c;
    }

    // ── Sección 2: Datos personales ───────────────────────────────────────────
    private JPanel buildPersonalCard() {
        JPanel c = card();
        c.setLayout(new BorderLayout(0, 14));
        c.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Título
        JLabel t = new JLabel("  Datos personales");
        t.setFont(UITheme.FONT_SECTION);
        t.setForeground(TXT_D);
        t.setIcon(IconUtil.icon(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.USER, 14, GREEN));
        c.add(t, BorderLayout.NORTH);

        // Grid 2 columnas
        JPanel grid = new JPanel(new GridLayout(0, 2, 16, 10));
        grid.setOpaque(false);

        // Tipo documento (no editable visualmente pero presente)
        cmbTipDoc = combo(new String[]{"Cédula de Ciudadanía","Tarjeta de Identidad","Cédula de Extranjería","Pasaporte"});
        grid.add(labeledField("Tipo de documento: *", cmbTipDoc));

        // N° documento (solo lectura)
        txtNumDoc = field("—"); txtNumDoc.setEditable(false);
        txtNumDoc.setBackground(new Color(0xF5F5F5));
        grid.add(labeledField("N° documento:", txtNumDoc));

        txtNombres   = field("Ingresa tus nombres");
        txtApellidos = field("Primer y segundo apellido");
        txtTelefono  = field("Ej: 3001234567"); applyNumericFilter(txtTelefono);
        txtEmail     = field("correo@ejemplo.com");

        cmbTipSang = combo(new String[]{"-- Seleccione --","A+","A-","B+","B-","AB+","AB-","O+","O-"});
        cmbGenero  = combo(new String[]{"-- Seleccione --","Masculino","Femenino","No binario","Género fluido","Prefiero no decirlo"});

        grid.add(labeledField("Nombres: *",             txtNombres));
        grid.add(labeledField("Apellidos: *",           txtApellidos));
        grid.add(labeledField("Teléfono:",              txtTelefono));
        grid.add(labeledField("Correo personal:",       txtEmail));
        grid.add(labeledField("Tipo de sangre:",        cmbTipSang));
        grid.add(labeledField("Género:",                cmbGenero));
        grid.add(labeledField("Fecha de nacimiento:",   buildDatePickerRow()));
        grid.add(new JPanel()); // espacio

        c.add(grid, BorderLayout.CENTER);

        // Footer con botón guardar + status
        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        lblStatusPersonal = new JLabel(" ");
        lblStatusPersonal.setFont(UITheme.FONT_BODY);
        JButton btnGuardar = UITheme.primaryButton("  Guardar datos", 150, 38);
        btnGuardar.setIcon(IconUtil.save());
        btnGuardar.addActionListener(e -> guardarDatosPersonales());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(btnGuardar);
        foot.add(lblStatusPersonal, BorderLayout.WEST);
        foot.add(btnRow,            BorderLayout.EAST);
        c.add(foot, BorderLayout.SOUTH);
        return c;
    }

    // ── Sección 3: Cuenta y seguridad ─────────────────────────────────────────
    private JPanel buildCuentaCard() {
        JPanel c = card();
        c.setLayout(new BorderLayout(0, 14));
        c.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel t = new JLabel("  Cuenta y seguridad");
        t.setFont(UITheme.FONT_SECTION);
        t.setForeground(TXT_D);
        t.setIcon(IconUtil.icon(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.SHIELD_ALT, 14, GREEN));
        c.add(t, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        // Mail
        txtMail = field("correo@login.com");
        JPanel mailRow = new JPanel(new GridLayout(1, 2, 16, 0));
        mailRow.setOpaque(false);
        mailRow.add(labeledField("Correo de inicio de sesión: *", txtMail));
        mailRow.add(new JPanel()); // espacio
        body.add(mailRow);
        body.add(vgap(14));

        // Separador cambio contraseña
        JLabel lPass = new JLabel("Cambiar contraseña  (opcional)");
        lPass.setFont(UITheme.FONT_LABEL);
        lPass.setForeground(TXT_G);
        body.add(lPass);
        body.add(vgap(8));

        JPanel passGrid = new JPanel(new GridLayout(1, 3, 16, 0));
        passGrid.setOpaque(false);
        txtPassActual  = passField();
        txtPassNueva   = passField();
        txtPassConfirm = passField();
        passGrid.add(labeledField("Contraseña actual:",  txtPassActual));
        passGrid.add(labeledField("Nueva contraseña:",   txtPassNueva));
        passGrid.add(labeledField("Confirmar:",          txtPassConfirm));
        body.add(passGrid);

        c.add(body, BorderLayout.CENTER);

        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        lblStatusCuenta = new JLabel(" ");
        lblStatusCuenta.setFont(UITheme.FONT_BODY);
        JButton btnGuardar = UITheme.primaryButton("  Guardar cuenta", 150, 38);
        btnGuardar.setIcon(IconUtil.save());
        btnGuardar.addActionListener(e -> guardarCuenta());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(btnGuardar);
        foot.add(lblStatusCuenta, BorderLayout.WEST);
        foot.add(btnRow,          BorderLayout.EAST);
        c.add(foot, BorderLayout.SOUTH);
        return c;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // LÓGICA
    // ═════════════════════════════════════════════════════════════════════════

    private void cargarPerfil() {
        int numDoc = SessionManager.getInstance().getAdmin().getNumDoc();
        new SwingWorker<Optional<PerfilAdmin>, Void>() {
            @Override protected Optional<PerfilAdmin> doInBackground() {
                return service.cargarPerfil(numDoc);
            }
            @Override protected void done() {
                try {
                    Optional<PerfilAdmin> opt = get();
                    if (opt.isEmpty()) return;
                    perfilActual = opt.get();
                    poblarFormulario(perfilActual);
                } catch (InterruptedException | ExecutionException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void poblarFormulario(PerfilAdmin p) {
        Persona per = p.persona();

        // Tipo documento
        String td = per.getTipDoc() != null ? per.getTipDoc() : "Cédula de Ciudadanía";
        for (int i = 0; i < cmbTipDoc.getItemCount(); i++) {
            if (cmbTipDoc.getItemAt(i).equals(td)) { cmbTipDoc.setSelectedIndex(i); break; }
        }

        txtNumDoc.setText(String.valueOf(per.getNumDoc()));
        setFieldValue(txtNombres,   per.getNombres());
        setFieldValue(txtApellidos, joinApes(per));
        setFieldValue(txtTelefono,  per.getTel());
        setFieldValue(txtEmail,     per.getEmail());

        // Tipo sangre
        selectCombo(cmbTipSang, per.getTipSang());
        // Género
        selectCombo(cmbGenero, per.getGenero());

        // Fecha nacimiento
        if (per.getFechaNac() != null) {
            fechaSeleccionada = per.getFechaNac();
            txtFechaNac.setText(fechaSeleccionada.format(FMT_DATE));
            txtFechaNac.setForeground(TXT_D);
        }

        // Mail
        setFieldValue(txtMail, p.cuenta() != null ? p.cuenta().getMail() : "");

        // Foto
        cargarFotoAvatar(p.fotoPerfil());
    }

    private void cargarFotoAvatar(String ruta) {
        if (ruta == null || ruta.isBlank()) { lblAvatar.setIcon(null); return; }
        try {
            File f = new File(ruta);
            if (!f.exists()) { lblAvatar.setIcon(null); return; }
            BufferedImage img = ImageIO.read(f);
            if (img == null) { lblAvatar.setIcon(null); return; }
            Image scaled = img.getScaledInstance(90, 90, Image.SCALE_SMOOTH);
            lblAvatar.setIcon(new ImageIcon(scaled));
        } catch (java.io.IOException e) {
            lblAvatar.setIcon(null);
        }
        lblAvatar.repaint();
    }

    // ── Selección de foto ─────────────────────────────────────────────────────
    private void seleccionarFoto() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar foto de perfil");
        fc.setFileFilter(new FileNameExtensionFilter(
            "Imágenes (JPG, PNG, WebP)", "jpg","jpeg","png","webp"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File archivo = fc.getSelectedFile();
        if (archivo.length() > 2 * 1024 * 1024) {
            mostrarStatus(lblStatusFoto, "❌  La imagen supera los 2 MB.", false);
            return;
        }

        // Preview inmediato
        try {
            BufferedImage img = ImageIO.read(archivo);
            if (img == null) { mostrarStatus(lblStatusFoto, "❌  Formato no válido.", false); return; }
            Image scaled = img.getScaledInstance(90, 90, Image.SCALE_SMOOTH);
            lblAvatar.setIcon(new ImageIcon(scaled));
            lblAvatar.repaint();
            mostrarStatus(lblStatusFoto,
                "✓  " + archivo.getName() + " — haz clic en «Guardar datos» para confirmar", true);
            // Subir foto inmediatamente
            subirFoto(archivo);
        } catch (java.io.IOException e) {
            mostrarStatus(lblStatusFoto, "❌  Error al leer la imagen.", false);
        }
    }

    private void subirFoto(File archivo) {
        int numDoc = perfilActual.persona().getNumDoc();
        String fotoAnterior = perfilActual.fotoPerfil();
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                byte[] datos = Files.readAllBytes(archivo.toPath());
                return service.guardarFotoPerfil(numDoc, archivo.getName(), datos, fotoAnterior);
            }
            @Override protected void done() {
                try {
                    String ruta = get();
                    if (ruta != null) {
                        if (perfilActual != null) {
                            perfilActual = new com.saia.data.ConfiguracionDAO.PerfilAdmin(
                                perfilActual.persona(), ruta, perfilActual.cuenta());
                        }
                        mostrarStatus(lblStatusFoto, "✓  Foto actualizada correctamente.", true);
                    } else {
                        mostrarStatus(lblStatusFoto, "❌  No se pudo guardar la foto.", false);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    mostrarStatus(lblStatusFoto, "❌  Error: " + ex.getMessage(), false);
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    // ── Guardar datos personales ──────────────────────────────────────────────
    private void guardarDatosPersonales() {
        if (perfilActual == null) return;
        int numDoc = perfilActual.persona().getNumDoc();

        String tipDoc    = (String) cmbTipDoc.getSelectedItem();
        String nombres   = txtNombres.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String[] apes    = apellidos.split("\\s+", 2);
        final String pApe  = apes.length > 0 ? apes[0] : "";
        final String sApe  = apes.length > 1 ? apes[1] : null;
        String tel     = txtTelefono.getText().trim();
        String email   = txtEmail.getText().trim();
        String tipSang = selectedCombo(cmbTipSang);
        String genero  = selectedCombo(cmbGenero);
        final LocalDate fechaNac = fechaSeleccionada;

        new SwingWorker<GuardadoResult, Void>() {
            @Override protected GuardadoResult doInBackground() {
                return service.guardarDatosPersonales(numDoc, tipDoc, nombres,
                    pApe, sApe, tel, tipSang, genero, fechaNac, email);
            }
            @Override protected void done() {
                try {
                    switch (get()) {
                        case OK -> {
                            mostrarStatus(lblStatusPersonal, "✓  Datos actualizados correctamente.", true);
                            // Refrescar nombre en sesión si cambió
                            SessionManager.getInstance().refreshAdmin();
                        }
                        case CAMPO_REQUERIDO -> mostrarStatus(lblStatusPersonal, "❌  Completa los campos obligatorios (*).", false);
                        case FECHA_INVALIDA  -> mostrarStatus(lblStatusPersonal, "❌  Fecha de nacimiento inválida (debe ser mayor de 18 años).", false);
                        default              -> mostrarStatus(lblStatusPersonal, "❌  Error al guardar. Intenta nuevamente.", false);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    // ── Guardar cuenta ────────────────────────────────────────────────────────
    private void guardarCuenta() {
        if (perfilActual == null) return;
        int numDoc = perfilActual.persona().getNumDoc();
        String mail       = txtMail.getText().trim();
        String passActual = new String(txtPassActual.getPassword());
        String passNueva  = new String(txtPassNueva.getPassword());
        String passConf   = new String(txtPassConfirm.getPassword());

        // Validar contraseña si se ingresó alguna
        if (!passNueva.isBlank()) {
            if (!passNueva.equals(passConf)) {
                mostrarStatus(lblStatusCuenta, "❌  La nueva contraseña y su confirmación no coinciden.", false);
                return;
            }
        }

        new SwingWorker<Void, Void>() {
            GuardadoResult mailResult, passResult = null;
            @Override protected Void doInBackground() {
                mailResult = service.guardarMail(numDoc, mail);
                if (!passNueva.isBlank()) {
                    passResult = service.cambiarPassword(numDoc, passActual, passNueva);
                }
                return null;
            }
            @Override protected void done() {
                // Mail
                if (mailResult != GuardadoResult.OK) {
                    String msg = switch (mailResult) {
                        case MAIL_DUPLICADO -> "❌  Ese correo ya está en uso por otro usuario.";
                        case MAIL_INVALIDO  -> "❌  El formato del correo no es válido.";
                        case CAMPO_REQUERIDO-> "❌  El correo de inicio de sesión es obligatorio.";
                        default             -> "❌  Error al actualizar el correo.";
                    };
                    mostrarStatus(lblStatusCuenta, msg, false); return;
                }
                // Contraseña
                if (passResult != null && passResult != GuardadoResult.OK) {
                    String msg = passResult == GuardadoResult.PASSWORD_INCORRECTA
                        ? "❌  La contraseña actual es incorrecta."
                        : "❌  Error al cambiar la contraseña.";
                    mostrarStatus(lblStatusCuenta, msg, false); return;
                }
                // Éxito
                mostrarStatus(lblStatusCuenta, "✓  Cuenta actualizada correctamente.", true);
                txtPassActual.setText("");
                txtPassNueva.setText("");
                txtPassConfirm.setText("");
            }
        }.execute();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPERS UI
    // ═════════════════════════════════════════════════════════════════════════

    private JPanel card() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0,0,0,10));
                g2.fill(new RoundRectangle2D.Float(2,3,getWidth()-3,getHeight()-3,12,12));
                g2.setColor(CARD);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth()-2,getHeight()-2,12,12));
                g2.setColor(BORDER); g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f,0.5f,getWidth()-3,getHeight()-3,12,12));
                g2.dispose(); super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
    }

    private JPanel labeledField(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_CAPTION);
        lbl.setForeground(TXT_G);
        p.add(lbl,  BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private JTextField field(String ph) {
        JTextField f = new JTextField();
        f.setFont(UITheme.FONT_BODY);
        f.setForeground(TXT_G);
        f.setText(ph);
        f.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(4, 8, 4, 8)));
        f.setPreferredSize(new Dimension(0, 34));
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (f.getText().equals(ph)) { f.setText(""); f.setForeground(TXT_D); }
                f.setBorder(new CompoundBorder(new LineBorder(GREEN, 2, true), new EmptyBorder(3, 7, 3, 7)));
            }
            @Override public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) { f.setText(ph); f.setForeground(TXT_G); }
                f.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(4, 8, 4, 8)));
            }
        });
        return f;
    }

    private JPasswordField passField() {
        JPasswordField f = new JPasswordField();
        f.setFont(UITheme.FONT_BODY);
        f.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(4, 8, 4, 8)));
        f.setPreferredSize(new Dimension(0, 34));
        return f;
    }

    private JComboBox<String> combo(String[] items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(UITheme.FONT_BODY);
        c.setBackground(Color.WHITE);
        c.setPreferredSize(new Dimension(0, 34));
        c.setFocusable(false);
        return c;
    }

    private JPanel buildDatePickerRow() {
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.setOpaque(false);
        txtFechaNac = new JTextField("dd/mm/aaaa");
        txtFechaNac.setFont(UITheme.FONT_BODY);
        txtFechaNac.setEditable(false);
        txtFechaNac.setForeground(TXT_G);
        txtFechaNac.setBackground(Color.WHITE);
        txtFechaNac.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(4, 8, 4, 8)));
        txtFechaNac.setPreferredSize(new Dimension(0, 34));

        JButton btnCal = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GREEN); g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,6,6);
                g2.dispose(); super.paintComponent(g);
            }
        };
        btnCal.setIcon(IconUtil.calendar());
        btnCal.setOpaque(false); btnCal.setContentAreaFilled(false);
        btnCal.setBorderPainted(false); btnCal.setFocusPainted(false);
        btnCal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCal.setPreferredSize(new Dimension(34, 34));
        btnCal.addActionListener(e -> showCalendario());
        p.add(txtFechaNac, BorderLayout.CENTER);
        p.add(btnCal,      BorderLayout.EAST);
        return p;
    }

    private void showCalendario() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Seleccionar fecha", true);
        dlg.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        dlg.getRootPane().setBorder(new EmptyBorder(10, 14, 10, 14));
        dlg.setResizable(false);

        javax.swing.SpinnerDateModel m = new javax.swing.SpinnerDateModel();
        if (fechaSeleccionada != null) {
            Calendar cal = Calendar.getInstance();
            cal.set(fechaSeleccionada.getYear(), fechaSeleccionada.getMonthValue()-1, fechaSeleccionada.getDayOfMonth());
            m.setValue(cal.getTime());
        }
        JSpinner sp = new JSpinner(m);
        sp.setEditor(new JSpinner.DateEditor(sp, "dd/MM/yyyy"));
        sp.setPreferredSize(new Dimension(150, 34));

        JButton ok = new JButton("Aceptar");
        ok.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        ok.setBackground(GREEN); ok.setForeground(Color.BLACK); ok.setFocusPainted(false);
        ok.addActionListener(ev -> {
            java.util.Date d = (java.util.Date) sp.getValue();
            Calendar cal = Calendar.getInstance(); cal.setTime(d);
            fechaSeleccionada = LocalDate.of(cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH));
            txtFechaNac.setText(fechaSeleccionada.format(FMT_DATE));
            txtFechaNac.setForeground(TXT_D);
            dlg.dispose();
        });

        dlg.add(new JLabel("Fecha:")); dlg.add(sp); dlg.add(ok);
        dlg.pack(); dlg.setLocationRelativeTo(this); dlg.setVisible(true);
    }

    // ── Helpers lógica ────────────────────────────────────────────────────────
    private void setFieldValue(JTextField f, String v) {
        if (v != null && !v.isBlank()) { f.setText(v); f.setForeground(TXT_D); }
    }

    private void selectCombo(JComboBox<String> c, String v) {
        c.setSelectedIndex(0);
        if (v == null || v.isBlank()) return;
        for (int i = 0; i < c.getItemCount(); i++) {
            if (c.getItemAt(i).equals(v)) { c.setSelectedIndex(i); return; }
        }
    }

    private String selectedCombo(JComboBox<String> c) {
        String v = (String) c.getSelectedItem();
        return (v == null || v.startsWith("--")) ? null : v;
    }

    private String joinApes(Persona p) {
        String a = p.getPApe() != null ? p.getPApe() : "";
        if (p.getSApe() != null && !p.getSApe().isBlank()) a += " " + p.getSApe();
        return a.trim();
    }

    private void applyNumericFilter(JTextField f) {
        ((javax.swing.text.AbstractDocument) f.getDocument())
            .setDocumentFilter(new javax.swing.text.DocumentFilter() {
                @Override public void insertString(FilterBypass fb, int o, String s,
                        javax.swing.text.AttributeSet a) throws javax.swing.text.BadLocationException {
                    if (s != null && s.matches("\\d+")) super.insertString(fb, o, s, a);
                }
                @Override public void replace(FilterBypass fb, int o, int l, String s,
                        javax.swing.text.AttributeSet a) throws javax.swing.text.BadLocationException {
                    if (s != null && s.matches("\\d*")) super.replace(fb, o, l, s, a);
                }
            });
    }

    private void mostrarStatus(JLabel lbl, String msg, boolean ok) {
        lbl.setText(msg);
        lbl.setForeground(ok ? GREEN : RED);
        // Limpiar después de 5 segundos
        Timer t = new Timer(5000, e -> lbl.setText(" "));
        t.setRepeats(false); t.start();
    }

    private static Component vgap(int h) { return Box.createVerticalStrut(h); }
}
