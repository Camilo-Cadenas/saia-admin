package com.saia.presentation.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JPanel;

/**
 * Componente gráfico de dona (donut chart) dibujado con Java2D.
 * Recibe un mapa de {etiqueta → valor} y renderiza los segmentos.
 */
public class DonutChart extends JPanel {

    private static final Color[] PALETTE = {
        new Color(0x2E7D32),   // verde
        new Color(0xFF8C00),   // naranja
        new Color(0xC62828),   // rojo
        new Color(0x1565C0),   // azul
        new Color(0x6A1B9A)    // morado
    };

    private Map<String, Integer> data = new LinkedHashMap<>();
    private int total = 0;
    private String centerLabel = "";

    public DonutChart() {
        setOpaque(false);
    }

    public void setData(Map<String, Integer> data, String centerLabel) {
        this.data        = data;
        this.centerLabel = centerLabel;
        this.total       = data.values().stream().mapToInt(Integer::intValue).sum();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (data == null || data.isEmpty()) {
            drawEmpty(g);
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int size   = Math.min(getWidth(), getHeight()) - 10;
        int x      = (getWidth()  - size) / 2;
        int y      = (getHeight() - size) / 2;
        int thick  = size / 5;   // grosor del anillo
        float startAngle = 90f;  // empieza arriba

        int i = 0;
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            float sweep = total > 0 ? (entry.getValue() * 360f / total) : 0;
            Color c = PALETTE[i % PALETTE.length];

            g2.setColor(c);
            Arc2D arc = new Arc2D.Float(x, y, size, size, startAngle, -sweep, Arc2D.PIE);
            g2.fill(arc);

            startAngle -= sweep;
            i++;
        }

        // Hoyo central blanco
        int innerSize = size - thick * 2;
        int ix = x + thick;
        int iy = y + thick;
        g2.setColor(getParent() != null ? getParent().getBackground() : Color.WHITE);
        g2.fill(new Ellipse2D.Float(ix, iy, innerSize, innerSize));

        // Texto central
        int cx = getWidth()  / 2;
        int cy = getHeight() / 2;

        g2.setColor(new Color(0x1A1A1A));
        g2.setFont(new Font("Segoe UI", Font.BOLD, size / 6));
        FontMetrics fm = g2.getFontMetrics();
        String totalStr = String.valueOf(total);
        g2.drawString(totalStr,
                cx - fm.stringWidth(totalStr) / 2,
                cy + fm.getAscent() / 4);

        if (!centerLabel.isEmpty()) {
            g2.setFont(new Font("Segoe UI", Font.PLAIN, size / 10));
            fm = g2.getFontMetrics();
            g2.setColor(new Color(0x888888));
            g2.drawString(centerLabel,
                    cx - fm.stringWidth(centerLabel) / 2,
                    cy + fm.getAscent() / 4 + size / 8);
        }

        g2.dispose();
    }

    private void drawEmpty(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int size  = Math.min(getWidth(), getHeight()) - 10;
        int x     = (getWidth() - size) / 2;
        int y     = (getHeight() - size) / 2;
        int thick = size / 5;
        g2.setColor(new Color(0xEEEEEE));
        g2.setStroke(new BasicStroke(thick));
        g2.draw(new Ellipse2D.Float(x + thick / 2f, y + thick / 2f,
                size - thick, size - thick));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(0x999999));
        String msg = "Sin datos";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg,
                getWidth() / 2 - fm.stringWidth(msg) / 2,
                getHeight() / 2 + fm.getAscent() / 3);
        g2.dispose();
    }

    /**
     * Retorna el color asignado a la posición {@code index} en la paleta.
     */
    public static Color colorAt(int index) {
        return PALETTE[index % PALETTE.length];
    }
}
