package com.saia.presentation.components;

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
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingConstants;

import com.saia.presentation.UITheme;

/**
 * Botón de la barra lateral con paleta de identidad SENA y soporte para iconos Ikonli.
 *
 * Uso con icono:
 * <pre>
 *   SidebarButton btn = new SidebarButton("Inicio",
 *       IconUtil.navHome(), IconUtil.navHomeActive());
 * </pre>
 * Sin icono (retro-compatible):
 * <pre>
 *   SidebarButton btn = new SidebarButton("Inicio");
 * </pre>
 */
public class SidebarButton extends JButton {

    private static final Color COLOR_ACTIVE_BG   = UITheme.PRIMARY_PALE;
    private static final Color COLOR_ACTIVE_TEXT  = UITheme.PRIMARY;
    private static final Color COLOR_NORMAL_TEXT  = new Color(0x374151);
    private static final Color COLOR_HOVER_BG    = new Color(0xF0F4F3);
    private static final Color COLOR_ACTIVE_LINE  = UITheme.PRIMARY;

    private boolean active  = false;
    private boolean hovered = false;

    private final Icon iconNormal;
    private final Icon iconActive;

    /** Constructor sin icono — retro-compatible. */
    public SidebarButton(String text) {
        this(text, null, null);
    }

    /** Constructor con icono normal y activo. */
    public SidebarButton(String text, Icon normalIcon, Icon activeIcon) {
        super(text);
        this.iconNormal = normalIcon;
        this.iconActive = activeIcon;

        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setHorizontalAlignment(SwingConstants.LEFT);
        setFont(UITheme.FONT_SIDEBAR);
        setForeground(COLOR_NORMAL_TEXT);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setIconTextGap(10);
        setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        setPreferredSize(new Dimension(200, 40));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        if (normalIcon != null) setIcon(normalIcon);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
        });
    }

    public void setActive(boolean active) {
        this.active = active;
        setFont(active
            ? UITheme.FONT_SIDEBAR.deriveFont(Font.BOLD)
            : UITheme.FONT_SIDEBAR);
        setForeground(active ? COLOR_ACTIVE_TEXT : COLOR_NORMAL_TEXT);
        // Cambia icono según estado
        if (active && iconActive != null) setIcon(iconActive);
        else if (!active && iconNormal != null) setIcon(iconNormal);
        repaint();
    }

    public boolean isActive() { return active; }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        if (active) {
            g2.setColor(COLOR_ACTIVE_BG);
            g2.fill(new RoundRectangle2D.Float(4, 2, w - 8, h - 4, 8, 8));
            g2.setColor(COLOR_ACTIVE_LINE);
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(4, 6, 4, h - 6);
        } else if (hovered) {
            g2.setColor(COLOR_HOVER_BG);
            g2.fill(new RoundRectangle2D.Float(4, 2, w - 8, h - 4, 8, 8));
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
