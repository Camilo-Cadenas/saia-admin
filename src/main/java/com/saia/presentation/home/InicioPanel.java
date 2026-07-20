package com.saia.presentation.home;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import com.saia.business.DashboardService;
import com.saia.business.DashboardStats;
import com.saia.data.DashboardDAO.ActividadItem;
import com.saia.presentation.UITheme;
import com.saia.presentation.components.DonutChart;
import com.saia.util.SessionManager;

/**
 * Panel central "Inicio" del dashboard.
 * Muestra tarjetas de estadísticas, actividad reciente, dona de reportes
 * e ingresos/salidas del día. Carga datos en SwingWorker.
 */
public class InicioPanel extends JPanel {

    // Colores — desde UITheme (paleta SENA)
    private static final Color GREEN_MID   = UITheme.PRIMARY;
    private static final Color GREEN_PALE  = UITheme.PRIMARY_PALE;
    private static final Color BG_PAGE     = UITheme.BG_SECONDARY;
    private static final Color CARD_BG     = UITheme.BG_WHITE;
    private static final Color BORDER_C    = UITheme.BORDER;
    private static final Color TEXT_DARK   = UITheme.TEXT_PRIMARY;
    private static final Color TEXT_GRAY   = UITheme.TEXT_SECONDARY;
    private static final Color TEXT_LIGHT  = UITheme.TEXT_LIGHT;

    private final DashboardService service = new DashboardService();

    // Etiquetas de tarjetas de stats
    private JLabel lblPersonalNum, lblPersonalActInact;
    private JLabel lblAprendicesNum, lblAprendicesActInact;
    private JLabel lblReportesNum, lblReportesHoyMes;
    private JLabel lblBloqueadosNum, lblBloqueadosDet;

    // Actividad reciente
    private JPanel actividadContainer;

    // Dona
    private DonutChart donut;
    private JPanel    legendPanel;

    // Ingresos/salidas
    private JLabel lblIngresos, lblSalidas;

    // Fecha/hora
    private JLabel lblFechaHora;

    public InicioPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_PAGE);
        setBorder(new EmptyBorder(20, 24, 20, 24));
        buildUI();
        // loadDataAsync() se llama desde HomeFrame después de añadir al CardLayout
    }

    // ── Construcción de la UI ─────────────────────────────────────────────────

    private void buildUI() {
        JScrollPane scroll = new JScrollPane(buildContent());
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_PAGE);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(BG_PAGE);
        root.setBorder(new EmptyBorder(0, 0, 20, 0));

        root.add(buildWelcomeBar());
        root.add(Box.createVerticalStrut(18));
        root.add(buildStatsRow());
        root.add(Box.createVerticalStrut(18));
        root.add(buildMiddleRow());
        root.add(Box.createVerticalStrut(18));
        root.add(buildIngresosRow());
        return root;
    }

    // ── Barra de bienvenida ───────────────────────────────────────────────────

    private JPanel buildWelcomeBar() {
        JPanel p = card(8);
        p.setLayout(new BorderLayout());
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        // Icono persona
        JLabel ico = new JLabel();
        ico.setPreferredSize(new Dimension(48, 48));
        ico.setBorder(new EmptyBorder(0, 14, 0, 14));

        JPanel icoWrapper = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GREEN_PALE);
                g2.fillOval(4, 4, 40, 40);
                g2.setColor(GREEN_MID);
                g2.fillOval(16, 10, 16, 16);
                g2.fillArc(11, 28, 26, 18, 0, 180);
                g2.dispose();
            }
        };
        icoWrapper.setOpaque(false);
        icoWrapper.setPreferredSize(new Dimension(52, 52));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        left.setOpaque(false);
        left.add(icoWrapper);

        JPanel txt = new JPanel();
        txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
        txt.setOpaque(false);
        txt.setBorder(new EmptyBorder(10, 0, 0, 0));
        String nombre = SessionManager.getInstance().getAdminNombre();
        JLabel t1 = new JLabel("¡Bienvenido, " + (nombre.isEmpty() ? "Administrador" : nombre) + "!");
        t1.setFont(UITheme.FONT_SECTION);
        t1.setForeground(TEXT_DARK);
        JLabel t2 = new JLabel("Aquí tienes un resumen general del sistema SAIA.");
        t2.setFont(UITheme.FONT_BODY);
        t2.setForeground(TEXT_GRAY);
        txt.add(t1); txt.add(t2);
        left.add(txt);

        lblFechaHora = new JLabel();
        lblFechaHora.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblFechaHora.setForeground(TEXT_GRAY);
        lblFechaHora.setBorder(new EmptyBorder(0, 0, 0, 16));
        lblFechaHora.setHorizontalAlignment(SwingConstants.RIGHT);
        updateFechaHora();

        // Actualizar hora cada minuto
        Timer t = new Timer(60_000, e -> updateFechaHora());
        t.start();

        p.add(left,       BorderLayout.WEST);
        p.add(lblFechaHora, BorderLayout.EAST);
        return p;
    }

    private void updateFechaHora() {
        LocalDateTime now = LocalDateTime.now();
        String fecha = now.format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy",
                new java.util.Locale("es", "CO")));
        String hora  = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        lblFechaHora.setText("<html><center>" + fecha + "<br><b>" + hora + "</b></center></html>");
    }

    // ── Fila de tarjetas de estadísticas ─────────────────────────────────────

    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        // Tarjeta 1 – Personal de Seguridad
        JPanel c1 = buildStatCard(GREEN_MID, "\uD83D\uDC6E", "Personal de Seguridad", "...");
        lblPersonalNum      = findNumLabel(c1);
        lblPersonalActInact = findSubLabel(c1);

        // Tarjeta 2 – Aprendices
        JPanel c2 = buildStatCard(new Color(0x1565C0), "\uD83C\uDF93", "Aprendices Registrados", "...");
        lblAprendicesNum      = findNumLabel(c2);
        lblAprendicesActInact = findSubLabel(c2);

        // Tarjeta 3 – Reportes
        JPanel c3 = buildStatCard(new Color(0xE65100), "\uD83D\uDCCB", "Reportes Registrados", "...");
        lblReportesNum      = findNumLabel(c3);
        lblReportesHoyMes   = findSubLabel(c3);

        // Tarjeta 4 – Bloqueados
        JPanel c4 = buildStatCard(new Color(0x6A1B9A), "\uD83D\uDD12", "Usuarios Bloqueados", "...");
        lblBloqueadosNum  = findNumLabel(c4);
        lblBloqueadosDet  = findSubLabel(c4);

        row.add(c1); row.add(c2); row.add(c3); row.add(c4);
        return row;
    }

    private JPanel buildStatCard(Color accentColor, String emoji, String title, String num) {
        JPanel card = card(10);
        card.setLayout(new BorderLayout(0, 4));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));

        // Ícono
        JPanel icoBox = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(),
                        accentColor.getBlue(), 25));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        icoBox.setOpaque(false);
        icoBox.setPreferredSize(new Dimension(36, 36));
        icoBox.setLayout(new GridBagLayout());
        JLabel icoLbl = new JLabel(emoji);
        icoLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        icoLbl.setForeground(accentColor);
        icoBox.add(icoLbl);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_CAPTION);
        titleLbl.setForeground(TEXT_GRAY);
        titleLbl.putClientProperty("type", "title");

        JLabel numLbl = new JLabel(num);
        numLbl.setFont(UITheme.FONT_KPI);
        numLbl.setForeground(TEXT_DARK);
        numLbl.putClientProperty("type", "num");

        JLabel subLbl = new JLabel(" ");
        subLbl.setFont(UITheme.FONT_CAPTION);
        subLbl.setForeground(TEXT_LIGHT);
        subLbl.putClientProperty("type", "sub");

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(titleLbl, BorderLayout.CENTER);
        top.add(icoBox,   BorderLayout.EAST);

        JPanel bot = new JPanel();
        bot.setLayout(new BoxLayout(bot, BoxLayout.Y_AXIS));
        bot.setOpaque(false);
        bot.add(numLbl);
        bot.add(subLbl);

        card.add(top, BorderLayout.NORTH);
        card.add(bot, BorderLayout.CENTER);
        return card;
    }

    /** Encuentra el JLabel de número grande dentro de una tarjeta. */
    private JLabel findNumLabel(JPanel card) {
        return findLabelByProp(card, "num");
    }
    private JLabel findSubLabel(JPanel card) {
        return findLabelByProp(card, "sub");
    }
    private JLabel findLabelByProp(Container c, String val) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JLabel lbl) {
                if (val.equals(lbl.getClientProperty("type"))) return lbl;
            }
            if (comp instanceof Container cont) {
                JLabel found = findLabelByProp(cont, val);
                if (found != null) return found;
            }
        }
        return null;
    }

    // ── Fila intermedia: Actividad Reciente + Reportes por Tipo ──────────────

    private JPanel buildMiddleRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        row.add(buildActividadCard());
        row.add(buildDonutCard());
        return row;
    }

    private JPanel buildActividadCard() {
        JPanel card = card(10);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("\u2261  Actividad Reciente");
        title.setFont(UITheme.FONT_SECTION);
        title.setForeground(TEXT_DARK);
        card.add(title, BorderLayout.NORTH);

        actividadContainer = new JPanel();
        actividadContainer.setLayout(new BoxLayout(actividadContainer, BoxLayout.Y_AXIS));
        actividadContainer.setOpaque(false);

        JLabel loading = new JLabel("Cargando...");
        loading.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        loading.setForeground(TEXT_LIGHT);
        actividadContainer.add(loading);

        card.add(actividadContainer, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildDonutCard() {
        JPanel card = card(10);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("\uD83D\uDCC8  Reportes por Tipo");
        title.setFont(UITheme.FONT_SECTION);
        title.setForeground(TEXT_DARK);

        JLabel subtitle = new JLabel("Este mes");
        subtitle.setFont(UITheme.FONT_CAPTION);
        subtitle.setForeground(TEXT_GRAY);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title,    BorderLayout.WEST);
        header.add(subtitle, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(12, 0));
        body.setOpaque(false);

        donut = new DonutChart();
        donut.setPreferredSize(new Dimension(140, 140));

        legendPanel = new JPanel();
        legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.Y_AXIS));
        legendPanel.setOpaque(false);

        JLabel ph = new JLabel("Sin datos registrados");
        ph.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        ph.setForeground(TEXT_LIGHT);
        legendPanel.add(ph);

        body.add(donut,       BorderLayout.WEST);
        body.add(legendPanel, BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    // ── Fila inferior: Ingresos / Salidas ────────────────────────────────────

    private JPanel buildIngresosRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        JPanel c1 = buildIngresCard("\u2192", "Ingresos Activos", GREEN_MID);
        JPanel c2 = buildIngresCard("\u2190", "Salidas Registradas", new Color(0xE65100));
        lblIngresos = findNumLabel(c1);
        lblSalidas  = findNumLabel(c2);
        row.add(c1); row.add(c2);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        JLabel title = new JLabel("\u21C4  Ingresos y Salidas de Hoy");
        title.setFont(UITheme.FONT_SECTION);
        title.setForeground(TEXT_DARK);
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        wrap.add(title, BorderLayout.NORTH);
        wrap.add(row,   BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildIngresCard(String arrow, String label, Color accentColor) {
        JPanel card = card(10);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(new EmptyBorder(14, 18, 14, 18));

        JLabel arrowLbl = new JLabel(arrow);
        arrowLbl.setFont(new Font("Segoe UI", Font.BOLD, 28));
        arrowLbl.setForeground(accentColor);

        JLabel titleLbl = new JLabel(label);
        titleLbl.setFont(UITheme.FONT_CAPTION);
        titleLbl.setForeground(TEXT_GRAY);

        JLabel numLbl = new JLabel("...");
        numLbl.setFont(UITheme.FONT_KPI);
        numLbl.setForeground(TEXT_DARK);
        numLbl.putClientProperty("type", "num");

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        info.add(titleLbl);
        info.add(numLbl);

        card.add(arrowLbl, BorderLayout.WEST);
        card.add(info,     BorderLayout.CENTER);
        return card;
    }

    // ── Carga de datos (SwingWorker) ──────────────────────────────────────────

    public void loadDataAsync() {
        SwingWorker<DashboardStats, Void> worker = new SwingWorker<>() {
            @Override
            protected DashboardStats doInBackground() {
                return service.loadStats();
            }
            @Override
            protected void done() {
                try {
                    DashboardStats s = get();
                    applyStats(s);
                } catch (InterruptedException | java.util.concurrent.ExecutionException ex) {
                    System.err.println("[InicioPanel] Error cargando stats: " + ex.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        };
        worker.execute();
    }

    private void applyStats(DashboardStats s) {
        // Tarjeta 1 – Personal
        if (lblPersonalNum != null) {
            if (s.personalTotal == 0) {
                lblPersonalNum.setText("0");
                if (lblPersonalActInact != null)
                    lblPersonalActInact.setText("Sin datos registrados");
            } else {
                lblPersonalNum.setText(String.valueOf(s.personalTotal));
                if (lblPersonalActInact != null)
                    lblPersonalActInact.setText("Activos: " + s.personalActivo
                            + "   Inactivos: " + s.personalInactivo);
            }
        }
        // Tarjeta 2 – Aprendices
        if (lblAprendicesNum != null) {
            if (s.aprendicesTotal == 0) {
                lblAprendicesNum.setText("0");
                if (lblAprendicesActInact != null)
                    lblAprendicesActInact.setText("Sin datos registrados");
            } else {
                lblAprendicesNum.setText(String.valueOf(s.aprendicesTotal));
                if (lblAprendicesActInact != null)
                    lblAprendicesActInact.setText("Activos: " + s.aprendicesActivos
                            + "   Inactivos: " + s.aprendicesInactivos);
            }
        }
        // Tarjeta 3 – Reportes
        if (lblReportesNum != null) {
            if (s.reportesTotal == 0) {
                lblReportesNum.setText("0");
                if (lblReportesHoyMes != null)
                    lblReportesHoyMes.setText("Sin datos registrados");
            } else {
                lblReportesNum.setText(String.valueOf(s.reportesTotal));
                if (lblReportesHoyMes != null)
                    lblReportesHoyMes.setText("Hoy: " + s.reportesHoy
                            + "   Este mes: " + s.reportesMes);
            }
        }
        // Tarjeta 4 – Bloqueados
        if (lblBloqueadosNum != null) {
            if (s.getBloqueadosTotal() == 0) {
                lblBloqueadosNum.setText("0");
                if (lblBloqueadosDet != null)
                    lblBloqueadosDet.setText("Sin datos registrados");
            } else {
                lblBloqueadosNum.setText(String.valueOf(s.getBloqueadosTotal()));
                if (lblBloqueadosDet != null)
                    lblBloqueadosDet.setText("Personal: " + s.bloqueadosPersonal
                            + "   Aprendices: " + s.bloqueadosAprendices);
            }
        }
        // Actividad reciente
        actividadContainer.removeAll();
        if (s.actividadReciente == null || s.actividadReciente.isEmpty()) {
            JLabel none = new JLabel("No hay actividad reciente registrada.");
            none.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            none.setForeground(TEXT_LIGHT);
            actividadContainer.add(none);
        } else {
            for (ActividadItem item : s.actividadReciente) {
                actividadContainer.add(buildActividadRow(item));
                actividadContainer.add(Box.createVerticalStrut(6));
            }
        }
        actividadContainer.revalidate();
        actividadContainer.repaint();

        // Dona
        if (s.reportesPorTipo == null || s.reportesPorTipo.isEmpty()) {
            donut.setData(new java.util.LinkedHashMap<>(), "");
            legendPanel.removeAll();
            JLabel none = new JLabel("Sin datos de reportes este mes.");
            none.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            none.setForeground(TEXT_LIGHT);
            legendPanel.add(none);
        } else {
            int total = s.reportesPorTipo.values().stream().mapToInt(Integer::intValue).sum();
            donut.setData(s.reportesPorTipo, "Total");
            legendPanel.removeAll();
            int idx = 0;
            for (Map.Entry<String, Integer> e : s.reportesPorTipo.entrySet()) {
                legendPanel.add(buildLegendRow(e.getKey(), e.getValue(), total, idx++));
                legendPanel.add(Box.createVerticalStrut(5));
            }
            JLabel totalLbl = new JLabel("Total de reportes este mes   " + total);
            totalLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            totalLbl.setForeground(TEXT_DARK);
            legendPanel.add(Box.createVerticalStrut(6));
            legendPanel.add(totalLbl);
        }
        legendPanel.revalidate();
        legendPanel.repaint();

        // Ingresos / Salidas
        if (lblIngresos != null) lblIngresos.setText(String.valueOf(s.ingresosHoy));
        if (lblSalidas  != null) lblSalidas.setText(String.valueOf(s.salidasHoy));
    }

    private JPanel buildActividadRow(ActividadItem item) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        // Bullet
        JPanel bullet = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GREEN_MID);
                g2.fillOval(4, 4, 8, 8);
                g2.dispose();
            }
        };
        bullet.setOpaque(false);
        bullet.setPreferredSize(new Dimension(16, 16));

        JLabel desc = new JLabel("<html><b>" + esc(item.getDescripcion()) + "</b><br>"
                + "<font color='#999999'>" + esc(item.getTipo()) + "</font></html>");
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        desc.setForeground(TEXT_DARK);

        String horaStr = "";
        if (item.getMomento() != null)
            horaStr = item.getMomento().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
        JLabel hora = new JLabel(horaStr);
        hora.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hora.setForeground(TEXT_LIGHT);

        row.add(bullet, BorderLayout.WEST);
        row.add(desc,   BorderLayout.CENTER);
        row.add(hora,   BorderLayout.EAST);
        return row;
    }

    private JPanel buildLegendRow(String label, int count, int total, int idx) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);

        JPanel colorBox = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(DonutChart.colorAt(idx));
                g2.fillRoundRect(0, 2, 12, 12, 4, 4);
                g2.dispose();
            }
        };
        colorBox.setOpaque(false);
        colorBox.setPreferredSize(new Dimension(12, 16));

        int pct = total > 0 ? (count * 100 / total) : 0;
        JLabel lbl = new JLabel(label + "   " + count + " (" + pct + "%)");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(TEXT_DARK);

        row.add(colorBox);
        row.add(lbl);
        return row;
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    /** Crea un panel blanco con sombra suave y esquinas redondeadas. */
    private JPanel card(int radius) {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Sombra
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fill(new RoundRectangle2D.Float(2, 3, getWidth() - 3, getHeight() - 3, radius, radius));
                // Fondo blanco
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 2, getHeight() - 2, radius, radius));
                // Borde
                g2.setColor(BORDER_C);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 3, getHeight() - 3, radius, radius));
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
