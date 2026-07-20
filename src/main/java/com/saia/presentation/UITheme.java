package com.saia.presentation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.JTableHeader;

/**
 * Clase de constantes y utilidades de estilo para toda la capa de vista SAIA.
 *
 * <pre>
 * Paleta SENA:
 *   Verde principal  #238276  → encabezados, botones primarios, topbar
 *   Naranja          #FC7323  → acento: botones de acción clave, alertas
 *   Verde comp.      #596548  → hover, bordes activos, estados secundarios
 *   Blanco           #FFFFFF  → fondo de paneles y tarjetas
 *   Gris claro       #F2F2F2  → fondos secundarios, separadores, filas tabla
 *   Gris oscuro      #2B2B2B  → texto principal
 * </pre>
 *
 * <b>USO EN PANELES NUEVOS:</b>
 * <pre>
 *   setBackground(UITheme.BG_SECONDARY);
 *   JButton btn = UITheme.primaryButton("Guardar");
 *   JButton btnX = UITheme.secondaryButton("Cancelar");
 *   JButton btnN = UITheme.accentButton("Registrar");
 *   JTable tabla = new JTable(model);
 *   UITheme.styleTable(tabla);
 *   JTextField campo = new JTextField();
 *   UITheme.styleTextField(campo);
 *   JComboBox&lt;String&gt; cmb = new JComboBox&lt;&gt;();
 *   UITheme.styleComboBox(cmb);
 * </pre>
 */
public final class UITheme {

    private UITheme() {}

    // ── Paleta SENA ───────────────────────────────────────────────────────────

    /** Verde principal — #238276 */
    public static final Color PRIMARY        = new Color(0x238276);
    /** Verde hover / borde activo — #1D6B62 */
    public static final Color PRIMARY_DARK   = new Color(0x1D6B62);
    /** Verde suave (hover fondo) — #E6F3F2 */
    public static final Color PRIMARY_PALE   = new Color(0xE6F3F2);
    /** Verde complementario — #596548 */
    public static final Color SECONDARY      = new Color(0x596548);
    /** Verde complementario hover — #445038 */
    public static final Color SECONDARY_DARK = new Color(0x445038);
    /** Naranja acento — #FC7323 */
    public static final Color ACCENT         = new Color(0xFC7323);
    /** Naranja hover — #E05E0F */
    public static final Color ACCENT_DARK    = new Color(0xE05E0F);

    /** Blanco — fondo tarjetas/panels */
    public static final Color BG_WHITE       = Color.WHITE;
    /** Gris claro — fondo de página */
    public static final Color BG_SECONDARY   = new Color(0xF2F2F2);
    /** Gris muy claro — filas alternas tabla */
    public static final Color BG_STRIPE      = new Color(0xF7F8FA);
    /** Borde sutil */
    public static final Color BORDER         = new Color(0xDDE1E7);
    /** Texto principal */
    public static final Color TEXT_PRIMARY   = new Color(0x2B2B2B);
    /** Texto secundario / placeholders */
    public static final Color TEXT_SECONDARY = new Color(0x6B7280);
    /** Texto claro / captions */
    public static final Color TEXT_LIGHT     = new Color(0x9CA3AF);
    /** Rojo error */
    public static final Color ERROR          = new Color(0xC62828);

    // ── Topbar ────────────────────────────────────────────────────────────────
    /** Inicio del gradiente del topbar */
    public static final Color TOPBAR_START = new Color(0x1A6B5F);
    /** Fin del gradiente del topbar */
    public static final Color TOPBAR_END   = new Color(0x238276);

    // ── Tipografía ────────────────────────────────────────────────────────────

    /** Título principal de página — 20px negrita */
    public static final Font FONT_PAGE_TITLE  = new Font("Segoe UI", Font.BOLD,  20);
    /** Título de sección / tarjeta — 15px negrita */
    public static final Font FONT_SECTION     = new Font("Segoe UI", Font.BOLD,  15);
    /** Subtítulo / label de campo — 13px negrita */
    public static final Font FONT_LABEL       = new Font("Segoe UI", Font.BOLD,  13);
    /** Texto de cuerpo / celdas — 13px normal */
    public static final Font FONT_BODY        = new Font("Segoe UI", Font.PLAIN, 13);
    /** Texto de detalle / caption — 11px normal */
    public static final Font FONT_CAPTION     = new Font("Segoe UI", Font.PLAIN, 11);
    /** Número KPI grande — 28px negrita */
    public static final Font FONT_KPI         = new Font("Segoe UI", Font.BOLD,  28);
    /** Encabezado de tabla — 12px negrita */
    public static final Font FONT_TABLE_HEADER = new Font("Segoe UI", Font.BOLD, 12);
    /** Celda de tabla — 12px normal */
    public static final Font FONT_TABLE_CELL  = new Font("Segoe UI", Font.PLAIN, 12);
    /** Sidebar — 13px */
    public static final Font FONT_SIDEBAR     = new Font("Segoe UI", Font.PLAIN, 13);
    /** Sidebar etiqueta sección — 10px negrita */
    public static final Font FONT_SIDEBAR_SEC = new Font("Segoe UI", Font.BOLD,  10);

    // ── Espaciado (múltiplos de 8px) ─────────────────────────────────────────

    public static final int SPACING_XS  = 4;
    public static final int SPACING_SM  = 8;
    public static final int SPACING_MD  = 16;
    public static final int SPACING_LG  = 24;
    public static final int SPACING_XL  = 32;

    // ── Bordes reutilizables ──────────────────────────────────────────────────

    public static Border cardBorder() {
        return new LineBorder(BORDER, 1, true);
    }

    public static Border fieldBorder() {
        return new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(SPACING_XS, SPACING_SM, SPACING_XS, SPACING_SM));
    }

    public static Border fieldBorderFocus() {
        return new CompoundBorder(
            new LineBorder(PRIMARY, 2, true),
            new EmptyBorder(SPACING_XS - 1, SPACING_SM - 1, SPACING_XS - 1, SPACING_SM - 1));
    }

    public static Border panelPadding() {
        return new EmptyBorder(SPACING_LG, SPACING_LG, SPACING_LG, SPACING_LG);
    }

    public static Border innerPadding() {
        return new EmptyBorder(SPACING_MD, SPACING_MD, SPACING_MD, SPACING_MD);
    }

    // ── Fábrica de botones ────────────────────────────────────────────────────

    /**
     * Botón primario: fondo verde principal, texto blanco.
     * Úsalo para la acción principal de cada formulario ("Guardar", "Registrar").
     */
    public static JButton primaryButton(String text) {
        return primaryButton(text, 140, 38);
    }

    public static JButton primaryButton(String text, int w, int h) {
        return makeBtn(text, PRIMARY, PRIMARY_DARK, Color.WHITE, w, h);
    }

    /**
     * Botón secundario: borde verde, fondo blanco/transparente.
     * Úsalo para acciones secundarias ("Cancelar", "Volver", "Limpiar").
     */
    public static JButton secondaryButton(String text) {
        return secondaryButton(text, 120, 38);
    }

    public static JButton secondaryButton(String text, int w, int h) {
        JButton btn = new JButton(text) {
            boolean hov;
            { addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                @Override public void mouseExited (MouseEvent e) { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? PRIMARY_PALE : BG_WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 8, 8));
                g2.setColor(PRIMARY);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-2, getHeight()-2, 8, 8));
                g2.dispose(); super.paintComponent(g);
            }
        };
        applyBtnBase(btn, PRIMARY, w, h);
        btn.setForeground(PRIMARY);
        return btn;
    }

    /**
     * Botón acento: fondo naranja, texto blanco.
     * Úsalo con moderación — solo en la acción de mayor relevancia de la pantalla.
     */
    public static JButton accentButton(String text) {
        return accentButton(text, 140, 38);
    }

    public static JButton accentButton(String text, int w, int h) {
        return makeBtn(text, ACCENT, ACCENT_DARK, Color.WHITE, w, h);
    }

    /**
     * Botón de peligro/error: borde rojo, texto rojo.
     * Úsalo solo para acciones destructivas.
     */
    public static JButton dangerButton(String text) {
        return dangerButton(text, 120, 38);
    }

    public static JButton dangerButton(String text, int w, int h) {
        JButton btn = new JButton(text) {
            boolean hov;
            { addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                @Override public void mouseExited (MouseEvent e) { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? new Color(0xFEE2E2) : BG_WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 8, 8));
                g2.setColor(new java.awt.Color(0xC6, 0x28, 0x28));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-2, getHeight()-2, 8, 8));
                g2.dispose(); super.paintComponent(g);
            }
        };
        applyBtnBase(btn, ERROR, w, h);
        btn.setForeground(ERROR);
        return btn;
    }

    // ── Estilos globales para componentes ─────────────────────────────────────

    /**
     * Aplica el estilo SAIA a una JTable y su JScrollPane contenedor.
     * Llama a este método una vez tras crear la tabla.
     */
    public static void styleTable(JTable table) {
        table.setFont(FONT_TABLE_CELL);
        table.setForeground(TEXT_PRIMARY);
        table.setBackground(BG_WHITE);
        table.setGridColor(new Color(0xEEEEEE));
        table.setRowHeight(40);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionBackground(PRIMARY_PALE);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setFocusable(false);
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));

        JTableHeader header = table.getTableHeader();
        header.setBackground(PRIMARY);
        header.setForeground(Color.WHITE);
        header.setFont(FONT_TABLE_HEADER);
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createEmptyBorder());
    }

    /**
     * Aplica estilo SAIA a un JScrollPane que envuelve una tabla.
     */
    public static void styleScrollPane(JScrollPane scroll) {
        scroll.setBorder(new LineBorder(BORDER, 1, true));
        scroll.getViewport().setBackground(BG_WHITE);
    }

    /**
     * Aplica estilo SAIA a un JTextField.
     */
    public static void styleTextField(JTextField field) {
        field.setFont(FONT_BODY);
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(BG_WHITE);
        field.setBorder(fieldBorder());
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 34));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                field.setBorder(fieldBorderFocus());
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                field.setBorder(fieldBorder());
            }
        });
    }

    /**
     * Aplica estilo SAIA a un JComboBox.
     */
    public static <T> void styleComboBox(JComboBox<T> combo) {
        combo.setFont(FONT_BODY);
        combo.setBackground(BG_WHITE);
        combo.setForeground(TEXT_PRIMARY);
        combo.setBorder(new LineBorder(BORDER, 1, true));
        combo.setPreferredSize(new Dimension(combo.getPreferredSize().width, 34));
        combo.setFocusable(false);
    }

    /**
     * Crea un JLabel de título de página con el estilo correcto.
     */
    public static JLabel pageTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_PAGE_TITLE);
        l.setForeground(TEXT_PRIMARY);
        return l;
    }

    /**
     * Crea un JLabel de título de sección/tarjeta.
     */
    public static JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_SECTION);
        l.setForeground(TEXT_PRIMARY);
        return l;
    }

    /**
     * Crea un JLabel de caption/subtítulo.
     */
    public static JLabel caption(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_CAPTION);
        l.setForeground(TEXT_SECONDARY);
        return l;
    }

    // ── Aplicar UIManager global (llamar en Main.java) ────────────────────────

    /**
     * Aplica el tema SAIA globalmente vía UIManager.
     * Debe llamarse ANTES de crear cualquier ventana, en Main.java.
     *
     * <pre>UITheme.applyGlobal();</pre>
     */
    public static void applyGlobal() {
        // Fondo general
        UIManager.put("Panel.background",        BG_WHITE);
        UIManager.put("OptionPane.background",   BG_WHITE);
        UIManager.put("ToolTip.background",      new Color(0x1A1A1A));
        UIManager.put("ToolTip.foreground",      Color.WHITE);
        UIManager.put("ToolTip.font",            FONT_CAPTION);

        // JButton por defecto (botones en JOptionPane, JDialog, etc.)
        UIManager.put("Button.background",       PRIMARY);
        UIManager.put("Button.foreground",       Color.WHITE);
        UIManager.put("Button.font",             FONT_BODY);
        UIManager.put("Button.focus",            new Color(0, 0, 0, 0));
        UIManager.put("Button.select",           PRIMARY_DARK);

        // JTextField
        UIManager.put("TextField.background",    BG_WHITE);
        UIManager.put("TextField.foreground",    TEXT_PRIMARY);
        UIManager.put("TextField.font",          FONT_BODY);
        UIManager.put("TextField.caretForeground", PRIMARY);
        UIManager.put("TextField.selectionBackground", PRIMARY);
        UIManager.put("TextField.selectionForeground", Color.WHITE);
        UIManager.put("TextField.border",        fieldBorder());

        // JPasswordField
        UIManager.put("PasswordField.background", BG_WHITE);
        UIManager.put("PasswordField.foreground", TEXT_PRIMARY);
        UIManager.put("PasswordField.font",       FONT_BODY);
        UIManager.put("PasswordField.caretForeground", PRIMARY);
        UIManager.put("PasswordField.selectionBackground", PRIMARY);
        UIManager.put("PasswordField.selectionForeground", Color.WHITE);
        UIManager.put("PasswordField.border",     fieldBorder());

        // JComboBox
        UIManager.put("ComboBox.background",     BG_WHITE);
        UIManager.put("ComboBox.foreground",     TEXT_PRIMARY);
        UIManager.put("ComboBox.font",           FONT_BODY);
        UIManager.put("ComboBox.selectionBackground", PRIMARY);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("ComboBox.border",         new LineBorder(BORDER, 1));

        // JTable
        UIManager.put("Table.background",        BG_WHITE);
        UIManager.put("Table.foreground",        TEXT_PRIMARY);
        UIManager.put("Table.font",              FONT_TABLE_CELL);
        UIManager.put("Table.gridColor",         new Color(0xEEEEEE));
        UIManager.put("Table.selectionBackground", PRIMARY_PALE);
        UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
        UIManager.put("TableHeader.background",  PRIMARY);
        UIManager.put("TableHeader.foreground",  Color.WHITE);
        UIManager.put("TableHeader.font",        FONT_TABLE_HEADER);
        UIManager.put("TableHeader.cellBorder",  BorderFactory.createEmptyBorder(0, 8, 0, 8));

        // JScrollPane
        UIManager.put("ScrollPane.background",   BG_WHITE);
        UIManager.put("ScrollPane.border",       new LineBorder(BORDER, 1));
        UIManager.put("ScrollBar.thumb",         new Color(0xC1CBD9));
        UIManager.put("ScrollBar.track",         BG_SECONDARY);
        UIManager.put("ScrollBar.thumbHighlight", PRIMARY_PALE);

        // JLabel
        UIManager.put("Label.foreground",        TEXT_PRIMARY);
        UIManager.put("Label.font",              FONT_BODY);

        // JSpinner
        UIManager.put("Spinner.background",      BG_WHITE);
        UIManager.put("Spinner.foreground",      TEXT_PRIMARY);
        UIManager.put("Spinner.font",            FONT_BODY);

        // JCheckBox
        UIManager.put("CheckBox.font",           FONT_BODY);
        UIManager.put("CheckBox.foreground",     TEXT_PRIMARY);

        // Dialogs / OptionPane
        UIManager.put("OptionPane.messageFont",  FONT_BODY);
        UIManager.put("OptionPane.buttonFont",   FONT_LABEL);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("OptionPane.background",   BG_WHITE);

        // Spinner editor
        UIManager.put("FormattedTextField.background", BG_WHITE);
        UIManager.put("FormattedTextField.foreground", TEXT_PRIMARY);
        UIManager.put("FormattedTextField.font",       FONT_BODY);
    }

    // ── Helpers internos ──────────────────────────────────────────────────────

    private static JButton makeBtn(String text, Color bg, Color bgHov, Color fg, int w, int h) {
        JButton btn = new JButton(text) {
            boolean hov;
            { addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                @Override public void mouseExited (MouseEvent e) { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? bgHov : bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 8, 8));
                g2.dispose(); super.paintComponent(g);
            }
        };
        applyBtnBase(btn, fg, w, h);
        btn.setForeground(fg);
        return btn;
    }

    private static void applyBtnBase(JButton btn, Color fg, int w, int h) {
        btn.setFont(FONT_LABEL);
        btn.setForeground(fg);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(w, h));
        btn.setHorizontalAlignment(SwingConstants.CENTER);
    }

    // ── Card helper reutilizable ──────────────────────────────────────────────

    /**
     * Crea un JPanel con fondo blanco, sombra suave y esquinas redondeadas.
     * Listo para usar como tarjeta en cualquier panel.
     *
     * <pre>JPanel card = UITheme.card(12);</pre>
     */
    public static javax.swing.JPanel card(int radius) {
        return new javax.swing.JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 10));
                g2.fill(new RoundRectangle2D.Float(2, 3, getWidth()-3, getHeight()-3, radius, radius));
                g2.setColor(BG_WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-2, getHeight()-2, radius, radius));
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-3, getHeight()-3, radius, radius));
                g2.dispose(); super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
    }
}
