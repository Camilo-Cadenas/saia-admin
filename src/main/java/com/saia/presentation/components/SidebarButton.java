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
import javax.swing.JButton;
import javax.swing.SwingConstants;

/**
 * Botón de la barra lateral del menú de navegación.
 * Soporta estado activo (seleccionado) y hover.
 */
public class SidebarButton extends JButton {

    private static final Color COLOR_ACTIVE_BG   = new Color(0xE8F5E9);
    private static final Color COLOR_ACTIVE_TEXT  = new Color(0x2E7D32);
    private static final Color COLOR_NORMAL_TEXT  = new Color(0x444444);
    private static final Color COLOR_HOVER_BG    = new Color(0xF1F8F2);
    private static final Color COLOR_ACTIVE_LINE  = new Color(0x2E7D32);

    private boolean active  = false;
    private boolean hovered = false;

    public SidebarButton(String text) {
        super(text);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setHorizontalAlignment(SwingConstants.LEFT);
        setFont(new Font("Segoe UI", Font.PLAIN, 13));
        setForeground(COLOR_NORMAL_TEXT);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setIconTextGap(10);
        setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        setPreferredSize(new Dimension(200, 40));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
        });
    }

    public void setActive(boolean active) {
        this.active = active;
        setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 13));
        setForeground(active ? COLOR_ACTIVE_TEXT : COLOR_NORMAL_TEXT);
        repaint();
    }

    public boolean isActive() { return active; }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        if (active) {
            // Fondo verde muy claro redondeado
            g2.setColor(COLOR_ACTIVE_BG);
            g2.fill(new RoundRectangle2D.Float(4, 2, w - 8, h - 4, 8, 8));
            // Línea verde izquierda
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
