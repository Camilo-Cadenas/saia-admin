package com.saia.presentation.home;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import com.saia.business.AuthService;
import com.saia.business.ConfiguracionService;
import com.saia.data.ConfiguracionDAO.PerfilAdmin;
import com.saia.presentation.IconUtil;
import com.saia.presentation.UITheme;
import com.saia.presentation.components.SidebarButton;
import com.saia.util.SessionManager;

/**
 * Ventana principal del panel de administración SAIA.
 *
 * Estructura:
 *  ┌─────────────────────────────────────────────────────┐
 *  │  TOPBAR (verde oscuro): logo | título | campana | usuario │
 *  ├──────────────────┬──────────────────────────────────┤
 *  │  SIDEBAR (blanco)│  CONTENT (panel dinámico)        │
 *  │  • Inicio        │                                   │
 *  │  • Personal Seg. │   ← cardLayout / swap             │
 *  │  • Aprendices    │                                   │
 *  │  • ...           │                                   │
 *  │  [Cerrar sesión] │                                   │
 *  ├──────────────────┴──────────────────────────────────┤
 *  │  FOOTER                                             │
 *  └─────────────────────────────────────────────────────┘
 */
public class HomeFrame extends JFrame {

    // ── Colores ───────────────────────────────────────────────────────────────
    private static final Color SIDEBAR_BG  = Color.WHITE;
    private static final Color BORDER_C    = UITheme.BORDER;
    private static final Color TEXT_GRAY   = UITheme.TEXT_SECONDARY;
    private static final Color SECT_LABEL  = UITheme.TEXT_LIGHT;

    // ── Navegación ────────────────────────────────────────────────────────────
    private static final String PAGE_INICIO      = "INICIO";
    private static final String PAGE_PERSONAL    = "PERSONAL";
    private static final String PAGE_APRENDICES  = "APRENDICES";
    private static final String PAGE_BLOQUEO     = "BLOQUEO";
    private static final String PAGE_REPORTES    = "REPORTES";
    private static final String PAGE_ESTADISTICAS= "ESTADISTICAS";
    private static final String PAGE_DESCARGA    = "DESCARGA";
    private static final String PAGE_AUDITORIA   = "AUDITORIA";
    private static final String PAGE_CONFIG      = "CONFIG";

    private final CardLayout  cardLayout  = new CardLayout();
    private final JPanel      contentArea = new JPanel(cardLayout);

    // Paneles de contenido (lazy: se crean al primera vez)
    private InicioPanel            inicioPanel;
    private PersonalSeguridadPanel personalPanel;
    private AprendicesPanel        aprendicesPanel;
    private BloqueoPanel           bloqueoPanel;
    private ReportesPanel          reportesPanel;
    private EstadisticasPanel      estadisticasPanel;
    private DescargaReportesPanel  descargaPanel;
    private HistorialAuditoriaPanel auditoriaPanel;
    private ConfiguracionPanel      configuracionPanel;

    // Sidebar buttons (para manejar el estado activo)
    private SidebarButton btnInicio;
    private SidebarButton btnPersonal;
    private SidebarButton btnAprendices;
    private SidebarButton btnBloqueo;
    private SidebarButton btnReportes;
    private SidebarButton btnDescargar;
    private SidebarButton btnAuditoria;
    private SidebarButton btnEstadisticas;
    private SidebarButton btnConfig;
    private final AuthService authService = new AuthService();
    private final ConfiguracionService configService = new ConfiguracionService();
    
    // Panel de avatar en la barra superior
    private JPanel userAvatarPanel;

    // ── Constructor ───────────────────────────────────────────────────────────
    public HomeFrame() {
        initFrame();
        buildUI();
        cargarFotoUsuario(); // Cargar foto del usuario
        navigate(PAGE_INICIO);
        setVisible(true);
    }

    private void initFrame() {
        setTitle("SAIA - Sistema de Autogestión de Aprendices SENA");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 680));
        setLayout(new BorderLayout());
        
        // Configurar pantalla completa
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });
    }

    // ── Construcción de la UI ─────────────────────────────────────────────────

    private void buildUI() {
        add(buildTopBar(),  BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(contentArea,    BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);
    }

    // ── TopBar ────────────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, UITheme.TOPBAR_START,
                        getWidth(), 0, UITheme.TOPBAR_END);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 60));
        bar.setBorder(new EmptyBorder(0, 0, 0, 0));

        // ── Izquierda: logo + nombre ──────────────────────────────────────────
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);

        // Logo SENA (icono dibujado)
        JPanel logo = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255,255,255,40));
                g2.fillRoundRect(6, 6, 48, 48, 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("SENA", 30 - fm.stringWidth("SENA") / 2, 35);
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(16, 38, 44, 38);
                g2.drawLine(18, 42, 42, 42);
                g2.dispose();
            }
        };
        logo.setOpaque(false);
        logo.setPreferredSize(new Dimension(60, 60));

        JPanel brandTxt = new JPanel();
        brandTxt.setLayout(new BoxLayout(brandTxt, BoxLayout.Y_AXIS));
        brandTxt.setOpaque(false);
        brandTxt.setBorder(new EmptyBorder(14, 0, 0, 0));

        JLabel lSaia = new JLabel("SAIA");
        lSaia.setFont(UITheme.FONT_PAGE_TITLE);
        lSaia.setForeground(Color.WHITE);

        JLabel lSub  = new JLabel("Sistema de Autogestión");
        lSub.setFont(UITheme.FONT_CAPTION);
        lSub.setForeground(new Color(255,255,255,180));

        JLabel lSub2 = new JLabel("de Aprendices SENA");
        lSub2.setFont(UITheme.FONT_CAPTION);
        lSub2.setForeground(new Color(255,255,255,180));

        brandTxt.add(lSaia);
        brandTxt.add(lSub);
        brandTxt.add(lSub2);

        left.add(logo);
        left.add(brandTxt);

        // ── Centro: nombre sección activa ─────────────────────────────────────
        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 18));
        center.setOpaque(false);
        
        JLabel pageLabel = new JLabel("  Inicio");
        pageLabel.setFont(UITheme.FONT_SECTION);
        pageLabel.setForeground(Color.WHITE);
        pageLabel.setIcon(IconUtil.icon(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.HOME, 16, Color.WHITE));
        pageLabel.setName("pageLabel");
        
        center.add(pageLabel);

        // ── Derecha: perfil de usuario ───────────────────────────────────────
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        right.setOpaque(false);

        JPanel userChip = buildUserChip();

        right.add(userChip);
        right.setBorder(new EmptyBorder(0, 0, 0, 16));

        bar.add(left,   BorderLayout.WEST);
        bar.add(center, BorderLayout.CENTER);
        bar.add(right,  BorderLayout.EAST);
        return bar;
    }

    private JPanel buildUserChip() {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        chip.setOpaque(false);
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Avatar con foto del usuario
        userAvatarPanel = new JPanel() {
            private ImageIcon userPhoto = null;
            
            @SuppressWarnings("unused") // Usado mediante reflexión en cargarFotoUsuario()
            public void actualizarFoto(ImageIcon icon) {
                this.userPhoto = icon;
                repaint();
            }
            
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (userPhoto != null) {
                    // Dibujar foto circular
                    g2.setClip(new Ellipse2D.Float(0, 0, 32, 32));
                    g2.drawImage(userPhoto.getImage(), 0, 0, 32, 32, null);
                    g2.setClip(null);
                    // Borde blanco
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawOval(0, 0, 32, 32);
                } else {
                    // Ícono genérico si no hay foto
                    g2.setColor(new Color(255,255,255,50));
                    g2.fillOval(0, 0, 32, 32);
                    g2.setColor(Color.WHITE);
                    g2.fillOval(10, 4,  12, 12);
                    g2.fillArc (7,  18, 18, 14, 0, 180);
                }
                g2.dispose();
            }
        };
        userAvatarPanel.setOpaque(false);
        userAvatarPanel.setPreferredSize(new Dimension(32, 32));

        String nombre = SessionManager.getInstance().getAdminNombre();
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel nameLbl = new JLabel(nombre.isEmpty() ? "Administrador" : nombre);
        nameLbl.setFont(UITheme.FONT_LABEL);
        nameLbl.setForeground(Color.WHITE);

        JLabel roleLbl = new JLabel("Rol: Administrador");
        roleLbl.setFont(UITheme.FONT_CAPTION);
        roleLbl.setForeground(new Color(255,255,255,180));

        info.add(nameLbl);
        info.add(roleLbl);

        chip.add(userAvatarPanel);
        chip.add(info);
        return chip;
    }
    
    /**
     * Carga la foto del usuario desde el perfil en la base de datos para mostrarla en la barra superior.
     */
    private void cargarFotoUsuario() {
        int numDoc = SessionManager.getInstance().getAdmin().getNumDoc();
        
        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() {
                Optional<PerfilAdmin> perfilOpt = configService.cargarPerfil(numDoc);
                if (perfilOpt.isEmpty()) return null;
                
                String rutaFoto = perfilOpt.get().fotoPerfil();
                if (rutaFoto == null || rutaFoto.isBlank()) return null;
                
                try {
                    File f = new File(rutaFoto);
                    if (!f.exists()) return null;
                    
                    BufferedImage img = ImageIO.read(f);
                    if (img == null) return null;
                    
                    Image scaled = img.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaled);
                } catch (java.io.IOException e) {
                    System.err.println("[HomeFrame] Error cargando foto: " + e.getMessage());
                    return null;
                }
            }
            
            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null && userAvatarPanel != null) {
                        // Llamar al método actualizarFoto usando reflection
                        try {
                            userAvatarPanel.getClass()
                                .getMethod("actualizarFoto", ImageIcon.class)
                                .invoke(userAvatarPanel, icon);
                        } catch (ReflectiveOperationException e) {
                            System.err.println("[HomeFrame] Error actualizando avatar: " + e.getMessage());
                        }
                    }
                } catch (InterruptedException | java.util.concurrent.ExecutionException ex) {
                    System.err.println("[HomeFrame] Error obteniendo foto: " + ex.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                // Borde derecho sutil
                g2.setColor(BORDER_C);
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
                g2.dispose();
            }
        };
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setBorder(new EmptyBorder(12, 0, 12, 0));

        // ── INICIO ────────────────────────────────────────────────────────────
        btnInicio = navBtn("  Inicio",
            IconUtil.navHome(), IconUtil.navHomeActive());
        btnInicio.addActionListener(e -> navigate(PAGE_INICIO));
        sidebar.add(btnInicio);
        sidebar.add(Box.createVerticalStrut(4));

        // ── GESTIÓN DE USUARIOS ───────────────────────────────────────────────
        sidebar.add(sectionLabel("GESTIÓN DE USUARIOS"));
        sidebar.add(Box.createVerticalStrut(2));

        btnPersonal = navBtn("  Personal de Seguridad",
            IconUtil.navGuards(), IconUtil.navGuardsActive());
        btnPersonal.addActionListener(e -> navigate(PAGE_PERSONAL));
        sidebar.add(btnPersonal);

        btnAprendices = navBtn("  Aprendices",
            IconUtil.navStudents(), IconUtil.navStudentsActive());
        btnAprendices.addActionListener(e -> navigate(PAGE_APRENDICES));
        sidebar.add(btnAprendices);

        btnBloqueo = navBtn("  Usuarios Bloqueados",
            IconUtil.navBlocked(), IconUtil.navBlockedActive());
        btnBloqueo.addActionListener(e -> navigate(PAGE_BLOQUEO));
        sidebar.add(btnBloqueo);

        // ── REPORTES ─────────────────────────────────────────────────────────
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(sectionLabel("REPORTES"));
        sidebar.add(Box.createVerticalStrut(2));

        btnReportes = navBtn("  Gestión de Reportes",
            IconUtil.navReports(), IconUtil.navReportsActive());
        btnReportes.addActionListener(e -> navigate(PAGE_REPORTES));
        sidebar.add(btnReportes);

        btnDescargar = navBtn("  Descargar Reportes",
            IconUtil.navDownload(), IconUtil.navDownloadActive());
        btnDescargar.addActionListener(e -> navigate(PAGE_DESCARGA));
        sidebar.add(btnDescargar);

        // ── AUDITORÍA ────────────────────────────────────────────────────────
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(sectionLabel("AUDITORÍA"));
        sidebar.add(Box.createVerticalStrut(2));

        btnAuditoria = navBtn("  Historial de Auditoría",
            IconUtil.navAudit(), IconUtil.navAuditActive());
        btnAuditoria.addActionListener(e -> navigate(PAGE_AUDITORIA));
        sidebar.add(btnAuditoria);

        // Estadísticas de Ingresos/Salidas bajo Auditoría
        btnEstadisticas = navBtn("  Estadísticas Ingr./Salidas",
            IconUtil.navStats(), IconUtil.navStatsActive());
        btnEstadisticas.addActionListener(e -> navigate(PAGE_ESTADISTICAS));
        sidebar.add(btnEstadisticas);

        // ── CONFIGURACIÓN ────────────────────────────────────────────────────
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(sectionLabel("CONFIGURACIÓN"));
        sidebar.add(Box.createVerticalStrut(2));

        btnConfig = navBtn("  Configuración del Sistema",
            IconUtil.navConfig(), IconUtil.navConfigActive());
        btnConfig.addActionListener(e -> navigate(PAGE_CONFIG));
        sidebar.add(btnConfig);

        // Espacio flexible
        sidebar.add(Box.createVerticalGlue());

        // ── CERRAR SESIÓN ────────────────────────────────────────────────────
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_C);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sidebar.add(sep);
        sidebar.add(Box.createVerticalStrut(8));

        JButton btnLogout = new JButton("\u2192  Cerrar Sesión") {
            boolean hov = false;
            { addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { hov = true;  repaint(); }
                @Override public void mouseExited (java.awt.event.MouseEvent e) { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                if (hov) {
                    g.setColor(new Color(0xFFF3F3));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
                super.paintComponent(g);
            }
        };
        btnLogout.setFont(UITheme.FONT_LABEL);
        btnLogout.setForeground(UITheme.ERROR);
        btnLogout.setOpaque(false);
        btnLogout.setContentAreaFilled(false);
        btnLogout.setBorderPainted(false);
        btnLogout.setFocusPainted(false);
        btnLogout.setHorizontalAlignment(SwingConstants.LEFT);
        btnLogout.setBorder(new EmptyBorder(0, 18, 0, 14));
        btnLogout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> logout());
        sidebar.add(btnLogout);

        return sidebar;
    }

    /** Crea un botón de sidebar con iconos Ikonli para estado normal y activo. */
    private SidebarButton navBtn(String text,
            org.kordamp.ikonli.swing.FontIcon normalIcon,
            org.kordamp.ikonli.swing.FontIcon activeIcon) {
        SidebarButton btn = new SidebarButton(text, normalIcon, activeIcon);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return btn;
    }

    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_SIDEBAR_SEC);
        lbl.setForeground(SECT_LABEL);
        lbl.setBorder(new EmptyBorder(4, 18, 2, 14));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    // ── Footer ────────────────────────────────────────────────────────────────

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(BORDER_C);
                g.drawLine(0, 0, getWidth(), 0);
            }
        };
        footer.setBackground(Color.WHITE);
        footer.setPreferredSize(new Dimension(0, 28));

        JLabel copy = new JLabel("© 2026 - S.A.I.A. Todos los derechos reservados.",
                SwingConstants.CENTER);
        copy.setFont(UITheme.FONT_CAPTION);
        copy.setForeground(TEXT_GRAY);

        JLabel ver = new JLabel("Versión 1.0.0  ");
        ver.setFont(UITheme.FONT_CAPTION);
        ver.setForeground(TEXT_GRAY);

        footer.add(copy, BorderLayout.CENTER);
        footer.add(ver,  BorderLayout.EAST);
        return footer;
    }

    // ── Navegación ────────────────────────────────────────────────────────────

    /**
     * Navega al panel indicado: crea el panel si es la primera vez
     * (lazy init) y actualiza el estado activo en el sidebar.
     */
    private void navigate(String page) {
        // Registrar panel si no existe
        switch (page) {
            case PAGE_INICIO -> {
                if (inicioPanel == null) {
                    inicioPanel = new InicioPanel();
                    contentArea.add(inicioPanel, PAGE_INICIO);
                }
            }
            case PAGE_PERSONAL -> {
                if (personalPanel == null) {
                    personalPanel = new PersonalSeguridadPanel();
                    contentArea.add(personalPanel, PAGE_PERSONAL);
                }
            }
            case PAGE_APRENDICES -> {
                if (aprendicesPanel == null) {
                    aprendicesPanel = new AprendicesPanel();
                    contentArea.add(aprendicesPanel, PAGE_APRENDICES);
                }
            }
            case PAGE_BLOQUEO -> {
                if (bloqueoPanel == null) {
                    bloqueoPanel = new BloqueoPanel();
                    contentArea.add(bloqueoPanel, PAGE_BLOQUEO);
                }
            }
            case PAGE_REPORTES -> {
                if (reportesPanel == null) {
                    reportesPanel = new ReportesPanel();
                    contentArea.add(reportesPanel, PAGE_REPORTES);
                }
            }
            case PAGE_ESTADISTICAS -> {
                if (estadisticasPanel == null) {
                    estadisticasPanel = new EstadisticasPanel();
                    contentArea.add(estadisticasPanel, PAGE_ESTADISTICAS);
                }
            }
            case PAGE_DESCARGA -> {
                if (descargaPanel == null) {
                    descargaPanel = new DescargaReportesPanel();
                    contentArea.add(descargaPanel, PAGE_DESCARGA);
                }
            }
            case PAGE_AUDITORIA -> {
                if (auditoriaPanel == null) {
                    auditoriaPanel = new HistorialAuditoriaPanel();
                    contentArea.add(auditoriaPanel, PAGE_AUDITORIA);
                }
            }
            case PAGE_CONFIG -> {
                if (configuracionPanel == null) {
                    configuracionPanel = new ConfiguracionPanel();
                    contentArea.add(configuracionPanel, PAGE_CONFIG);
                }
            }
        }

        cardLayout.show(contentArea, page);
        updateActiveButton(page);
        updatePageTitle(page);

        // Recargar estadísticas al volver a Inicio
        if (PAGE_INICIO.equals(page) && inicioPanel != null) {
            inicioPanel.loadDataAsync();
        }
    }

    private void updateActiveButton(String page) {
        SidebarButton[] all = {btnInicio, btnPersonal, btnAprendices,
                btnBloqueo, btnReportes, btnDescargar, btnAuditoria,
                btnEstadisticas, btnConfig};
        for (SidebarButton b : all) {
            if (b != null) b.setActive(false);
        }
        switch (page) {
            case PAGE_INICIO       -> { if (btnInicio        != null) btnInicio.setActive(true); }
            case PAGE_PERSONAL     -> { if (btnPersonal      != null) btnPersonal.setActive(true); }
            case PAGE_APRENDICES   -> { if (btnAprendices    != null) btnAprendices.setActive(true); }
            case PAGE_BLOQUEO      -> { if (btnBloqueo       != null) btnBloqueo.setActive(true); }
            case PAGE_REPORTES     -> { if (btnReportes      != null) btnReportes.setActive(true); }
            case PAGE_DESCARGA     -> { if (btnDescargar     != null) btnDescargar.setActive(true); }
            case PAGE_ESTADISTICAS -> { if (btnEstadisticas  != null) btnEstadisticas.setActive(true); }
            case PAGE_AUDITORIA    -> { if (btnAuditoria     != null) btnAuditoria.setActive(true); }
            case PAGE_CONFIG       -> { if (btnConfig        != null) btnConfig.setActive(true); }
        }
    }

    private void updatePageTitle(String page) {
        // Buscar el label de título en el topbar
        findPageLabel(getContentPane(), page);
    }

    private void findPageLabel(Container c, String page) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JLabel lbl && "pageLabel".equals(lbl.getName())) {
                switch (page) {
                    case PAGE_INICIO       -> {
                        lbl.setText("  Inicio");
                        lbl.setIcon(IconUtil.icon(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.HOME, 16, Color.WHITE));
                    }
                    case PAGE_PERSONAL     -> {
                        lbl.setText("  Personal de Seguridad");
                        lbl.setIcon(IconUtil.icon(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.USER_SHIELD, 16, Color.WHITE));
                    }
                    case PAGE_APRENDICES   -> {
                        lbl.setText("  Aprendices");
                        lbl.setIcon(IconUtil.icon(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.USER_GRADUATE, 16, Color.WHITE));
                    }
                    case PAGE_BLOQUEO      -> {
                        lbl.setText("  Bloqueo de Usuarios");
                        lbl.setIcon(IconUtil.icon(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.USER_SLASH, 16, Color.WHITE));
                    }
                    case PAGE_REPORTES     -> {
                        lbl.setText("  Gestión de Reportes");
                        lbl.setIcon(IconUtil.icon(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.FILE_ALT, 16, Color.WHITE));
                    }
                    case PAGE_ESTADISTICAS -> {
                        lbl.setText("  Estadísticas de Ingresos/Salidas");
                        lbl.setIcon(IconUtil.icon(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.CHART_BAR, 16, Color.WHITE));
                    }
                    case PAGE_DESCARGA     -> {
                        lbl.setText("  Descarga de Reportes");
                        lbl.setIcon(IconUtil.icon(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.FILE_DOWNLOAD, 16, Color.WHITE));
                    }
                    case PAGE_AUDITORIA    -> {
                        lbl.setText("  Historial de Auditoría");
                        lbl.setIcon(IconUtil.icon(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.HISTORY, 16, Color.WHITE));
                    }
                    case PAGE_CONFIG       -> {
                        lbl.setText("  Configuración del Sistema");
                        lbl.setIcon(IconUtil.icon(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.COG, 16, Color.WHITE));
                    }
                    default                -> {
                        lbl.setText(page);
                        lbl.setIcon(null);
                    }
                }
                return;
            }
            if (comp instanceof Container cont) findPageLabel(cont, page);
        }
    }

    // ── Acciones globales ─────────────────────────────────────────────────────

    private void logout() {
        int r = JOptionPane.showConfirmDialog(this,
                "¿Está seguro que desea cerrar sesión?",
                "Cerrar sesión", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (r == JOptionPane.YES_OPTION) {
            authService.logout();
            dispose();
            SwingUtilities.invokeLater(com.saia.presentation.login.LoginFrame::new);
        }
    }

    private void confirmExit() {
        int r = JOptionPane.showConfirmDialog(this,
                "¿Desea salir de SAIA?",
                "Salir", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (r == JOptionPane.YES_OPTION) {
            if (SessionManager.getInstance().isSessionActive())
                authService.logout();
            System.exit(0);
        }
    }
}
