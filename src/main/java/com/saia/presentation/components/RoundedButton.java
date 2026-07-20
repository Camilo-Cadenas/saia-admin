package com.saia.presentation.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;

/**
 * Botón personalizado con bordes redondeados y efecto hover.
 */
public class RoundedButton extends JButton {

    private Color backgroundColor;
    private Color hoverColor;
    private Color pressedColor;
    @SuppressWarnings("unused") // se aplica vía setForeground() en el constructor
    private Color foregroundColor;
    private int radius;
    private boolean isHovered = false;
    private boolean isPressed = false;

    public RoundedButton(String text) {
        this(text,
            com.saia.presentation.UITheme.PRIMARY,
            com.saia.presentation.UITheme.PRIMARY_DARK,
            com.saia.presentation.UITheme.SECONDARY_DARK,
            Color.WHITE, 30);
    }

    public RoundedButton(String text, Color backgroundColor, Color hoverColor,
                         Color pressedColor, Color foregroundColor, int radius) {
        super(text);
        this.backgroundColor = backgroundColor;
        this.hoverColor = hoverColor;
        this.pressedColor = pressedColor;
        this.foregroundColor = foregroundColor;
        this.radius = radius;

        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setForeground(foregroundColor);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(com.saia.presentation.UITheme.FONT_LABEL);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                isPressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
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

        // Sombra suave
        if (!isPressed) {
            g2.setColor(new Color(0, 0, 0, 30));
            g2.fill(new RoundRectangle2D.Float(2, 3, w - 2, h - 2, radius, radius));
        }

        // Fondo del botón
        Color bgColor;
        if (isPressed) {
            bgColor = pressedColor;
        } else if (isHovered) {
            bgColor = hoverColor;
        } else {
            bgColor = backgroundColor;
        }
        g2.setColor(bgColor);
        g2.fill(new RoundRectangle2D.Float(0, 0, w - 2, h - 2, radius, radius));

        g2.dispose();

        // Texto e ícono del botón
        super.paintComponent(g);
    }

    @Override
    public boolean contains(int x, int y) {
        Shape shape = new RoundRectangle2D.Float(0, 0, getWidth() - 2, getHeight() - 2, radius, radius);
        return shape.contains(x, y);
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        repaint();
    }

    public void setHoverColor(Color color) {
        this.hoverColor = color;
        repaint();
    }

    public void setButtonRadius(int radius) {
        this.radius = radius;
        repaint();
    }
}
