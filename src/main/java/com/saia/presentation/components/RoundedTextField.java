package com.saia.presentation.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Campo de texto personalizado con bordes redondeados y soporte para texto placeholder.
 */
public class RoundedTextField extends JTextField {

    private String placeholder;
    private Color borderColor = new Color(0xDDDDDD);
    private Color focusBorderColor = new Color(0x2E7D32);
    private Color placeholderColor = new Color(0xAAAAAA);
    private int radius;
    private boolean isFocused = false;

    public RoundedTextField(int columns) {
        this("", columns, 12);
    }

    public RoundedTextField(String placeholder, int columns, int radius) {
        super(columns);
        this.placeholder = placeholder;
        this.radius = radius;

        setOpaque(false);
        setBorder(new EmptyBorder(10, 14, 10, 14));
        setFont(new Font("Segoe UI", Font.PLAIN, 14));
        setForeground(new Color(0x333333));
        setBackground(Color.WHITE);

        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                isFocused = true;
                repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                isFocused = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        // Fondo blanco redondeado
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, radius, radius));

        // Borde
        g2.setColor(isFocused ? focusBorderColor : borderColor);
        g2.setStroke(new BasicStroke(isFocused ? 2f : 1.5f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, radius, radius));

        g2.dispose();

        super.paintComponent(g);

        // Placeholder
        if (getText().isEmpty() && placeholder != null && !placeholder.isEmpty() && !isFocusOwner()) {
            Graphics2D g2ph = (Graphics2D) g.create();
            g2ph.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2ph.setColor(placeholderColor);
            g2ph.setFont(getFont());
            Insets insets = getInsets();
            FontMetrics fm = g2ph.getFontMetrics();
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2ph.drawString(placeholder, insets.left, y);
            g2ph.dispose();
        }
    }

    @Override
    protected void paintBorder(Graphics g) {
        // El borde se dibuja en paintComponent
    }

    @Override
    public boolean isOpaque() {
        return false;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    public void setBorderColor(Color color) {
        this.borderColor = color;
        repaint();
    }

    public void setFocusBorderColor(Color color) {
        this.focusBorderColor = color;
        repaint();
    }

    public void setFieldRadius(int radius) {
        this.radius = radius;
        repaint();
    }
}
