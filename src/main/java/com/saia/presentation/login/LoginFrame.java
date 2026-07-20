package com.saia.presentation.login;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import com.saia.business.AuthService;
import com.saia.business.AuthService.AuthResult;
import com.saia.presentation.UITheme;
import com.saia.presentation.components.RoundedButton;
import com.saia.presentation.components.RoundedPasswordField;
import com.saia.presentation.components.RoundedTextField;

/**
 * Pantalla de inicio de sesión del sistema SAIA.
 * Panel izquierdo: branding SENA. Panel derecho: formulario de login.
 */
public class LoginFrame extends JFrame {

    // Colores corporativos — ahora referenciados desde UITheme
    private static final Color COLOR_GREEN_DARK  = UITheme.TOPBAR_START;
    private static final Color COLOR_GREEN_MID   = UITheme.PRIMARY;
    private static final Color COLOR_GREEN_LIGHT = UITheme.SECONDARY;
    private static final Color COLOR_GREEN_PALE  = UITheme.PRIMARY_PALE;
    private static final Color COLOR_GRAY_TEXT   = UITheme.TEXT_SECONDARY;
    private static final Color COLOR_BORDER      = UITheme.BORDER;
    private static final Color COLOR_WHITE       = UITheme.BG_WHITE;

    private final AuthService authService = new AuthService();

    private RoundedTextField emailField;
    private RoundedPasswordField passwordField;
    private JCheckBox rememberMeCheck;
    private JLabel togglePasswordLabel;
    private RoundedButton loginButton;
    private RoundedButton backButton;
    private JLabel errorLabel;

    public LoginFrame() {
        initFrame();
        initComponents();
        setVisible(true);
    }

    private void initFrame() {
        setTitle("SAIA - Iniciar Sesión");
        setSize(1020, 600);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
    }

    private void initComponents() {
        JPanel leftPanel  = buildLeftPanel();
        JPanel rightPanel = buildRightPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerSize(0);
        splitPane.setEnabled(false);
        splitPane.setDividerLocation(330);
        splitPane.setBorder(null);
        add(splitPane, BorderLayout.CENTER);
    }

    // =========================================================
    //  PANEL IZQUIERDO  (branding SENA)
    // =========================================================
    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fondo degradado sutil
                GradientPaint gp = new GradientPaint(0, 0, COLOR_GREEN_DARK,
                        0, getHeight(), new Color(0x0D3B14));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Círculo decorativo grande (fondo)
                g2.setColor(new Color(255, 255, 255, 15));
                g2.fillOval(-60, -60, 280, 280);

                // Círculo decorativo pequeño
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillOval(getWidth() - 100, getHeight() - 120, 200, 200);

                // Línea decorativa horizontal
                g2.setColor(COLOR_GREEN_LIGHT);
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(30, 210, getWidth() - 30, 210);

                // Ilustración edificio
                drawBuilding(g2, getWidth(), getHeight());
            }
        };
        panel.setLayout(null);
        panel.setPreferredSize(new Dimension(330, 600));
        panel.setMinimumSize(new Dimension(330, 600));

        // ----- Logo SENA -----
        JLabel logoLabel = new JLabel("\uD83C\uDFEB", SwingConstants.CENTER);
        logoLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 42));
        logoLabel.setForeground(COLOR_WHITE);
        logoLabel.setBounds(0, 40, 330, 55);
        panel.add(logoLabel);

        // ----- Título SAIA -----
        JLabel titleLabel = new JLabel("SAIA", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 42));
        titleLabel.setForeground(COLOR_WHITE);
        titleLabel.setBounds(0, 90, 330, 55);
        panel.add(titleLabel);

        // ----- Subtítulo -----
        JLabel subtitleLabel = new JLabel("<html><center>Sistema de Autogestión<br>de Aprendices SENA</center></html>",
                SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(255, 255, 255, 200));
        subtitleLabel.setBounds(20, 145, 290, 55);
        panel.add(subtitleLabel);

        // ----- Badge plataforma administrativa -----
        JPanel badgePanel = buildBadgePanel();
        badgePanel.setBounds(20, 225, 290, 70);
        panel.add(badgePanel);

        return panel;
    }

    private JPanel buildBadgePanel() {
        JPanel badge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 25));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(new Color(255, 255, 255, 60));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);
        badge.setLayout(new BoxLayout(badge, BoxLayout.Y_AXIS));
        badge.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel line1 = new JLabel("\u2714  Plataforma administrativa");
        line1.setFont(new Font("Segoe UI", Font.BOLD, 12));
        line1.setForeground(COLOR_WHITE);
        line1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel line2 = new JLabel("Acceso exclusivo para administradores");
        line2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        line2.setForeground(new Color(255, 255, 255, 180));
        line2.setAlignmentX(Component.LEFT_ALIGNMENT);

        badge.add(line1);
        badge.add(Box.createVerticalStrut(4));
        badge.add(line2);
        return badge;
    }

    /** Dibuja la ilustración de un edificio con Graphics2D en la parte inferior del panel izquierdo. */
    private void drawBuilding(Graphics2D g2, int panelW, int panelH) {
        g2.setColor(new Color(255, 255, 255, 55));
        g2.setStroke(new BasicStroke(1.5f));

        int baseY    = panelH - 20;
        int buildingX = 20;

        // --- Edificio principal (centro) ---
        int bW = 110, bH = 140;
        int bX = (panelW - bW) / 2;
        int bY = baseY - bH;
        g2.drawRect(bX, bY, bW, bH);

        // Ventanas edificio principal (4 filas x 3 col)
        int wW = 18, wH = 14, wGapX = 10, wGapY = 12;
        int startX = bX + 12, startY = bY + 14;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) {
                int wx = startX + col * (wW + wGapX);
                int wy = startY + row * (wH + wGapY);
                g2.setColor(new Color(255, 255, 255, 80));
                g2.fillRect(wx, wy, wW, wH);
                g2.setColor(new Color(255, 255, 255, 120));
                g2.drawRect(wx, wy, wW, wH);
            }
        }
        // Puerta principal
        g2.setColor(new Color(255, 255, 255, 55));
        g2.drawRect(bX + (bW / 2) - 12, baseY - 30, 24, 30);

        // --- Edificio izquierdo ---
        int lW = 55, lH = 90;
        int lX = buildingX;
        int lY = baseY - lH;
        g2.setColor(new Color(255, 255, 255, 40));
        g2.drawRect(lX, lY, lW, lH);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 2; col++) {
                g2.drawRect(lX + 8 + col * 22, lY + 12 + row * 22, 14, 12);
            }
        }

        // --- Edificio derecho ---
        int rW = 55, rH = 105;
        int rX = panelW - buildingX - rW;
        int rY = baseY - rH;
        g2.setColor(new Color(255, 255, 255, 40));
        g2.drawRect(rX, rY, rW, rH);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 2; col++) {
                g2.drawRect(rX + 8 + col * 22, rY + 12 + row * 24, 14, 12);
            }
        }

        // Línea de suelo
        g2.setColor(new Color(255, 255, 255, 80));
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(buildingX - 5, baseY, panelW - buildingX + 5, baseY);
    }

    // =========================================================
    //  PANEL DERECHO  (formulario de login)
    // =========================================================
    private JPanel buildRightPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(COLOR_WHITE);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(COLOR_WHITE);
        form.setBorder(new EmptyBorder(30, 50, 30, 50));
        form.setMaximumSize(new Dimension(460, 600));

        // Avatar circular
        form.add(buildAvatarPanel());
        form.add(Box.createVerticalStrut(16));

        // Título
        JLabel titleLabel = new JLabel("Acceso Administrador", SwingConstants.CENTER);
        titleLabel.setFont(UITheme.FONT_PAGE_TITLE);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(titleLabel);
        form.add(Box.createVerticalStrut(6));

        // Subtítulo
        JLabel subtitleLabel = new JLabel("Ingresa tus credenciales para continuar", SwingConstants.CENTER);
        subtitleLabel.setFont(UITheme.FONT_BODY);
        subtitleLabel.setForeground(UITheme.TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(subtitleLabel);
        form.add(Box.createVerticalStrut(28));

        // Campo correo
        form.add(buildFieldLabel("Correo electrónico"));
        form.add(Box.createVerticalStrut(6));
        form.add(buildEmailFieldPanel());
        form.add(Box.createVerticalStrut(16));

        // Campo contraseña
        form.add(buildFieldLabel("Contraseña"));
        form.add(Box.createVerticalStrut(6));
        form.add(buildPasswordFieldPanel());
        form.add(Box.createVerticalStrut(12));

        // Recordarme + Olvidé contraseña
        form.add(buildRememberRow());
        form.add(Box.createVerticalStrut(10));

        // Label de error
        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        errorLabel.setForeground(new Color(0xC62828));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(errorLabel);
        form.add(Box.createVerticalStrut(6));

        // Botón Iniciar sesión
        loginButton = buildLoginButton();
        form.add(loginButton);
        form.add(Box.createVerticalStrut(14));

        // Separador "o"
        form.add(buildSeparator());
        form.add(Box.createVerticalStrut(14));

        // Botón Volver al sistema
        backButton = buildBackButton();
        form.add(backButton);
        form.add(Box.createVerticalStrut(20));

        // Footer
        form.add(buildFooter());

        outer.add(form);
        return outer;
    }

    private JPanel buildAvatarPanel() {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrapper.setOpaque(false);

        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Círculo fondo verde muy claro
                g2.setColor(COLOR_GREEN_PALE);
                g2.fillOval(0, 0, getWidth(), getHeight());
                // Ícono persona (cabeza + cuerpo)
                g2.setColor(COLOR_GREEN_MID);
                int cx = getWidth() / 2, cy = getHeight() / 2;
                // Cabeza
                g2.fillOval(cx - 12, cy - 22, 24, 24);
                // Cuerpo (arco)
                g2.fillArc(cx - 18, cy + 4, 36, 28, 0, 180);
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(72, 72));
        wrapper.add(avatar);
        return wrapper;
    }

    private JLabel buildFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_LABEL);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel buildEmailFieldPanel() {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                // Pintado por el RoundedTextField hijo
                super.paintComponent(g);
            }
        };
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        // Ícono sobre en la izquierda
        JLabel iconLabel = buildIconLabel("\u2709", 16);
        iconLabel.setBorder(new EmptyBorder(0, 12, 0, 0));

        emailField = new RoundedTextField("ejemplo@correo.com", 20, 12);
        emailField.setBorder(new EmptyBorder(10, 8, 10, 14));

        // Acción Enter
        emailField.addActionListener(e -> performLogin());

        // Contenedor con borde redondeado que engloba ícono + campo
        JPanel fieldContainer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(COLOR_BORDER);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        fieldContainer.setOpaque(false);
        fieldContainer.add(iconLabel, BorderLayout.WEST);
        fieldContainer.add(emailField, BorderLayout.CENTER);

        wrapper.add(fieldContainer, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildPasswordFieldPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        // Ícono candado izquierdo
        JLabel lockIcon = buildIconLabel("\uD83D\uDD12", 16);
        lockIcon.setBorder(new EmptyBorder(0, 12, 0, 0));

        passwordField = new RoundedPasswordField(20, 12);
        passwordField.setBorder(new EmptyBorder(10, 8, 10, 8));
        passwordField.addActionListener(e -> performLogin());

        // Ícono ojo (toggle visibilidad) derecho
        togglePasswordLabel = new JLabel("\uD83D\uDC41");
        togglePasswordLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        togglePasswordLabel.setBorder(new EmptyBorder(0, 0, 0, 12));
        togglePasswordLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        togglePasswordLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                passwordField.togglePasswordVisibility();
                // Actualizar ícono
                if (passwordField.isPasswordVisible()) {
                    togglePasswordLabel.setText("\uD83D\uDEAB"); // ojo tachado (aproximación Unicode)
                    togglePasswordLabel.setToolTipText("Ocultar contraseña");
                } else {
                    togglePasswordLabel.setText("\uD83D\uDC41");
                    togglePasswordLabel.setToolTipText("Mostrar contraseña");
                }
            }
        });
        togglePasswordLabel.setToolTipText("Mostrar contraseña");

        JPanel fieldContainer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(COLOR_BORDER);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        fieldContainer.setOpaque(false);
        fieldContainer.add(lockIcon, BorderLayout.WEST);
        fieldContainer.add(passwordField, BorderLayout.CENTER);
        fieldContainer.add(togglePasswordLabel, BorderLayout.EAST);

        wrapper.add(fieldContainer, BorderLayout.CENTER);
        return wrapper;
    }

    private JLabel buildIconLabel(String unicode, int size) {
        JLabel lbl = new JLabel(unicode);
        lbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, size));
        lbl.setForeground(COLOR_GRAY_TEXT);
        return lbl;
    }

    private JPanel buildRememberRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        rememberMeCheck = new JCheckBox("Recordarme");
        rememberMeCheck.setFont(UITheme.FONT_BODY);
        rememberMeCheck.setForeground(UITheme.TEXT_PRIMARY);
        rememberMeCheck.setOpaque(false);
        rememberMeCheck.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel forgotLabel = new JLabel("\u00BFOlvidaste tu contraseña?");
        forgotLabel.setFont(UITheme.FONT_BODY);
        forgotLabel.setForeground(UITheme.PRIMARY);
        forgotLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(LoginFrame.this,
                        "Contacta al administrador del sistema para\nrestablecer tu contraseña.",
                        "Recuperar contraseña",
                        JOptionPane.INFORMATION_MESSAGE);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                forgotLabel.setForeground(UITheme.PRIMARY_DARK);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                forgotLabel.setForeground(UITheme.PRIMARY);
            }
        });

        row.add(rememberMeCheck, BorderLayout.WEST);
        row.add(forgotLabel, BorderLayout.EAST);
        return row;
    }

    private RoundedButton buildLoginButton() {
        RoundedButton btn = new RoundedButton("\uD83D\uDD12  Iniciar sesión",
                UITheme.PRIMARY, UITheme.PRIMARY_DARK,
                UITheme.SECONDARY_DARK, Color.WHITE, 28);
        btn.setFont(UITheme.FONT_SECTION);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btn.setPreferredSize(new Dimension(360, 50));
        btn.addActionListener(e -> performLogin());
        return btn;
    }

    private JPanel buildSeparator() {
        JPanel sep = new JPanel(new BorderLayout(8, 0));
        sep.setOpaque(false);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JSeparator left  = new JSeparator();
        JSeparator right = new JSeparator();
        left.setForeground(COLOR_BORDER);
        right.setForeground(COLOR_BORDER);

        JLabel orLabel = new JLabel("o", SwingConstants.CENTER);
        orLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        orLabel.setForeground(COLOR_GRAY_TEXT);

        sep.add(left,    BorderLayout.WEST);
        sep.add(orLabel, BorderLayout.CENTER);
        sep.add(right,   BorderLayout.EAST);

        // Igualar tamaño de separadores
        left.setPreferredSize(new Dimension(0, 2));
        right.setPreferredSize(new Dimension(0, 2));
        return sep;
    }

    private RoundedButton buildBackButton() {
        RoundedButton btn = new RoundedButton("\uD83D\uDEE1  Volver al sistema",
                COLOR_WHITE, new Color(0xF5F5F5), new Color(0xE0E0E0), new Color(0x444444), 28) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Fondo blanco con borde gris
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 2, getHeight() - 2, 28, 28));
                g2.setColor(COLOR_BORDER);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 3, getHeight() - 3, 28, 28));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setForeground(new Color(0x444444));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btn.setPreferredSize(new Dimension(360, 46));
        btn.addActionListener(e ->
                JOptionPane.showMessageDialog(this,
                        "Función no disponible en esta versión.",
                        "Volver al sistema", JOptionPane.INFORMATION_MESSAGE));
        return btn;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Separador
        JSeparator sep = new JSeparator();
        sep.setForeground(COLOR_BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.add(sep);
        footer.add(Box.createVerticalStrut(12));

        // Línea 1: shield + texto
        JPanel secRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        secRow.setOpaque(false);
        JLabel shieldIcon = new JLabel("\uD83D\uDEE1");
        shieldIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        shieldIcon.setForeground(COLOR_GREEN_MID);
        JLabel secText = new JLabel("Acceso seguro y confidencial");
        secText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        secText.setForeground(COLOR_GRAY_TEXT);
        secRow.add(shieldIcon);
        secRow.add(secText);
        footer.add(secRow);
        footer.add(Box.createVerticalStrut(4));

        // Línea 2: nombre sistema
        JLabel sysLabel = new JLabel("SAIA - Sistema de Autogestión de Aprendices SENA",
                SwingConstants.CENTER);
        sysLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sysLabel.setForeground(new Color(0x999999));
        sysLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.add(sysLabel);
        footer.add(Box.createVerticalStrut(3));

        // Línea 3: copyright
        JLabel copyLabel = new JLabel("\u00A9 2025 SENA. Todos los derechos reservados.",
                SwingConstants.CENTER);
        copyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        copyLabel.setForeground(new Color(0xBBBBBB));
        copyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.add(copyLabel);

        return footer;
    }

    // =========================================================
    //  LÓGICA DE LOGIN
    // =========================================================
    private void performLogin() {
        String email    = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        // Limpiar error previo
        errorLabel.setText(" ");

        // Deshabilitar botón mientras procesa
        loginButton.setEnabled(false);
        loginButton.setText("\uD83D\uDD12  Verificando...");

        // Procesar en hilo separado para no bloquear la UI
        SwingWorker<AuthResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AuthResult doInBackground() throws Exception {
                Thread.sleep(400); // Efecto visual mínimo
                return authService.login(email, password);
            }

            @Override
            protected void done() {
                try {
                    AuthResult result = get();
                    handleAuthResult(result);
                } catch (InterruptedException | java.util.concurrent.ExecutionException ex) {
                    showError("Error inesperado: " + ex.getMessage());
                    Thread.currentThread().interrupt();
                } finally {
                    loginButton.setEnabled(true);
                    loginButton.setText("\uD83D\uDD12  Iniciar sesión");
                }
            }
        };
        worker.execute();
    }

    private void handleAuthResult(AuthResult result) {
        if (result == AuthResult.SUCCESS) {
            // Abrir dashboard y cerrar login — invokeLater evita el warning "New instance ignored"
            dispose();
            SwingUtilities.invokeLater(com.saia.presentation.home.HomeFrame::new);
        } else {
            showError(authService.getMessageForResult(result));
            // Sacudir ventana visualmente en errores de credenciales
            if (result == AuthResult.INVALID_CREDENTIALS) {
                shakeWindow();
            }
        }
    }

    private void showError(String message) {
        errorLabel.setText("\u26A0  " + message);
    }

    /** Efecto de sacudida horizontal para indicar error de credenciales. */
    private void shakeWindow() {
        final Point origin = getLocation();
        final int distance = 8;
        // xDelta define el desplazamiento horizontal en cada tick del timer
        final int[] xDelta = {distance, -distance, distance, -distance, distance, -distance, 0};

        Timer timer = new Timer(50, null);
        int[] step = {0};
        timer.addActionListener(e -> {
            if (step[0] < xDelta.length) {
                setLocation(origin.x + xDelta[step[0]], origin.y);
                step[0]++;
            } else {
                setLocation(origin);
                ((Timer) e.getSource()).stop();
            }
        });
        timer.start();
    }
}
