package com.saia.presentation.home;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.JPanel;

import com.saia.data.ReporteConsultaDAO.EstadisticaPunto;

/**
 * Gráfico de barras agrupadas: Ingresos (azul) | Salidas (verde) | Diferencia (naranja)
 * por cada período del eje X.
 * Dibujado con Java2D — sin dependencias externas.
 */
class GraficoLineas extends JPanel {

    // Colores de las 3 series
    private static final Color COLOR_INGR = new Color(0x1565C0);  // azul
    private static final Color COLOR_SAL  = new Color(0x2E7D32);  // verde
    private static final Color COLOR_DIF  = new Color(0xE65100);  // naranja

    // Colores de fondo y grilla
    private static final Color BG_CHART  = new Color(0xEFF3FA);
    private static final Color COLOR_GRID = new Color(0xDDE3EE);
    private static final Color COLOR_AXIS = new Color(0x9BAABF);
    private static final Color COLOR_TEXT = new Color(0x555F70);

    private List<EstadisticaPunto> datos;

    GraficoLineas() {
        setOpaque(true);
        setBackground(Color.WHITE);
    }

    void setData(List<EstadisticaPunto> datos, String tipo) {
        this.datos = datos;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int W = getWidth(), H = getHeight();
        int padL = 60, padR = 20, padT = 24, padB = 58;
        int gW = W - padL - padR;
        int gH = H - padT - padB;

        // Fondo del área del gráfico
        g2.setColor(BG_CHART);
        g2.fillRoundRect(padL, padT, gW, gH, 6, 6);

        if (datos == null || datos.isEmpty()) {
            g2.setColor(COLOR_TEXT);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            String msg = "Sin datos para el período seleccionado";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (W - fm.stringWidth(msg)) / 2, H / 2);
            g2.dispose(); return;
        }

        int n = datos.size();

        // ── Calcular máximo ────────────────────────────────────────────────────
        int maxVal = 1;
        for (EstadisticaPunto p : datos) {
            maxVal = Math.max(maxVal, Math.max(p.ingresos, p.salidas));
        }
        maxVal = (int)(maxVal * 1.18) + 1;

        // ── Grilla horizontal ──────────────────────────────────────────────────
        int gridLines = 5;
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        for (int i = 0; i <= gridLines; i++) {
            int y = padT + gH - i * gH / gridLines;
            g2.setColor(COLOR_GRID);
            g2.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    0, new float[]{4, 3}, 0));
            g2.drawLine(padL, y, padL + gW, y);
            // Etiqueta eje Y
            g2.setColor(COLOR_TEXT);
            int val = i * maxVal / gridLines;
            String lbl = val >= 10000 ? (val/1000) + "k" : String.valueOf(val);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lbl, padL - fm.stringWidth(lbl) - 6, y + 4);
        }

        // ── Calcular dimensiones de barras ─────────────────────────────────────
        // Cada "grupo" tiene 3 barras: Ingreso, Salida, Diferencia
        int groupCount  = n;
        int groupW      = gW / (groupCount == 0 ? 1 : groupCount);
        int barW        = Math.max(4, (groupW - 12) / 3);  // 3 barras + espacios
        int barGap      = Math.max(1, (groupW - 3 * barW) / 4);
        int groupOffset = barGap; // margen izquierdo dentro del grupo

        // ── Dibujar barras ─────────────────────────────────────────────────────
        for (int i = 0; i < n; i++) {
            EstadisticaPunto pt = datos.get(i);
            int gx = padL + i * groupW;   // inicio del grupo en X
            int cx = gx + groupW / 2;     // centro del grupo (para etiqueta X)

            int hI = (int)((double) pt.ingresos / maxVal * gH);
            int hS = (int)((double) pt.salidas  / maxVal * gH);
            int dif = pt.ingresos - pt.salidas;
            int hD = (int)((double) Math.abs(dif) / maxVal * gH);

            int x1 = gx + groupOffset;
            int x2 = x1 + barW + barGap;
            int x3 = x2 + barW + barGap;
            int base = padT + gH;

            // Sombra suave
            g2.setColor(new Color(0, 0, 0, 18));
            g2.fillRoundRect(x1+2, base - hI + 2, barW, hI, 4, 4);
            g2.fillRoundRect(x2+2, base - hS + 2, barW, hS, 4, 4);
            g2.fillRoundRect(x3+2, base - hD + 2, barW, hD, 4, 4);

            // Barra Ingresos
            paintBar(g2, x1, base, barW, hI, COLOR_INGR);

            // Barra Salidas
            paintBar(g2, x2, base, barW, hS, COLOR_SAL);

            // Barra Diferencia (naranja si positiva, rojo si negativa)
            Color cDif = dif >= 0 ? COLOR_DIF : new Color(0xC62828);
            paintBar(g2, x3, base, barW, Math.max(hD, 2), cDif);

            // Valor encima de la barra más alta
            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            g2.setColor(COLOR_TEXT);
            int maxH = Math.max(hI, Math.max(hS, hD));
            if (maxH > 14) {
                String v = String.valueOf(Math.max(pt.ingresos, Math.max(pt.salidas, Math.abs(dif))));
                FontMetrics fm = g2.getFontMetrics();
                int vx = cx - fm.stringWidth(v)/2;
                g2.drawString(v, vx, base - maxH - 4);
            }

            // Etiqueta eje X
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(COLOR_TEXT);
            String etq = pt.etiqueta != null ? pt.etiqueta : "";
            FontMetrics fm = g2.getFontMetrics();
            int ex = cx - fm.stringWidth(etq)/2;
            g2.drawString(etq, ex, padT + gH + 16);
        }

        // ── Ejes ───────────────────────────────────────────────────────────────
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(COLOR_AXIS);
        g2.drawLine(padL, padT, padL, padT + gH);
        g2.drawLine(padL, padT + gH, padL + gW, padT + gH);

        // ── Leyenda ────────────────────────────────────────────────────────────
        drawLegend(g2, padL + gW - 290, padT - 18);

        g2.dispose();
    }

    private void paintBar(Graphics2D g2, int x, int base, int w, int h, Color color) {
        if (h <= 0) return;
        // Gradiente vertical
        GradientPaint gp = new GradientPaint(x, base - h, color.brighter(), x, base, color.darker());
        g2.setPaint(gp);
        g2.fillRoundRect(x, base - h, w, h, 4, 4);
        // Borde sutil
        g2.setColor(color.darker());
        g2.setStroke(new BasicStroke(0.8f));
        g2.drawRoundRect(x, base - h, w, h, 4, 4);
    }

    private void drawLegend(Graphics2D g2, int x, int y) {
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        int bw = 12, bh = 10, gap = 6;

        // Ingresos
        g2.setColor(COLOR_INGR); g2.fillRoundRect(x, y, bw, bh, 3, 3);
        g2.setColor(COLOR_TEXT); g2.drawString("Ingresos", x + bw + gap, y + 9);

        // Salidas
        int x2 = x + 80;
        g2.setColor(COLOR_SAL); g2.fillRoundRect(x2, y, bw, bh, 3, 3);
        g2.setColor(COLOR_TEXT); g2.drawString("Salidas", x2 + bw + gap, y + 9);

        // Diferencia
        int x3 = x + 155;
        g2.setColor(COLOR_DIF); g2.fillRoundRect(x3, y, bw, bh, 3, 3);
        g2.setColor(COLOR_TEXT); g2.drawString("Diferencia", x3 + bw + gap, y + 9);
    }
}
