package com.saia.presentation.home;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

import com.saia.data.EstadisticasDAO;
import com.saia.data.EstadisticasDAO.FranjaHoraria;
import com.saia.data.EstadisticasDAO.InfoPeriodo;
import com.saia.data.EstadisticasDAO.PuntoDia;
import com.saia.util.SessionManager;

/**
 * Panel Dashboard — Estadísticas de Ingresos/Salidas.
 * Layout: Filtros → 4 KPI → Gráfico barras+línea → 3 paneles inferiores → Footer
 */
public class EstadisticasPanel extends JPanel {

    // Paleta
    private static final Color BG       = new Color(0xF0F2F5);
    private static final Color CARD     = Color.WHITE;
    private static final Color BORDER   = new Color(0xE2E8F0);
    private static final Color TXT_D    = new Color(0x1A202C);
    private static final Color TXT_G    = new Color(0x718096);
    private static final Color C_BLUE   = new Color(0x2563EB);
    private static final Color C_GREEN  = new Color(0x059669);
    private static final Color C_PURPLE = new Color(0x7C3AED);
    private static final Color C_ORANGE = new Color(0xEA580C);

    private static final DateTimeFormatter FMT_D  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_TS = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String[] MESES = {
        "Enero","Febrero","Marzo","Abril","Mayo","Junio",
        "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
    };

    private final EstadisticasDAO dao = new EstadisticasDAO();

    // Filtros
    private JComboBox<String> cmbTipo, cmbMes, cmbAnio;
    private JSpinner spDia;

    // KPI labels
    private JLabel kpiTotalVal, kpiTotalSub;
    private JLabel kpiPromVal,  kpiPromSub;
    private JLabel kpiHoraVal;
    private JLabel kpiValidVal;

    // Gráfico principal
    private GraficoBarrasLinea grafico;

    // Panel dona + leyenda
    private GraficoDona donaChart;
    private JPanel      leyendaBox;

    // Info período
    private JLabel lPeriodo, lDias, lMax, lMin;

    // Resumen rápido
    private JLabel rTotal, rProm, rHora, rVal;

    // Footer
    private JLabel lblFooter;

    private LocalDate desde, hasta;

    public EstadisticasPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(16, 20, 10, 20));
        buildUI();
    }

    @Override public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::loadCurrentMonth);
    }

    // ── Build UI ──────────────────────────────────────────────────────────────
    private void buildUI() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setOpaque(false);
        root.add(buildFiltros());
        root.add(vgap(10));
        root.add(buildKpiRow());
        root.add(vgap(10));
        root.add(buildGraficoCard());
        root.add(vgap(10));
        root.add(buildBottomRow());
        root.add(vgap(6));

        JScrollPane scroll = new JScrollPane(root);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    // ── Filtros ───────────────────────────────────────────────────────────────
    private JPanel buildFiltros() {
        JPanel p = card();
        p.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        p.setBorder(new EmptyBorder(0, 14, 0, 14));

        p.add(boldLabel("Buscar por:"));
        cmbTipo = combo(new String[]{"Día","Mes","Año"}, 82);
        p.add(cmbTipo);

        spDia = new JSpinner(new SpinnerDateModel());
        spDia.setEditor(new JSpinner.DateEditor(spDia, "dd/MM/yyyy"));
        spDia.setPreferredSize(new Dimension(130, 30));
        ((SpinnerDateModel) spDia.getModel()).setValue(toDate(LocalDate.now()));
        p.add(spDia);

        cmbMes = combo(MESES, 120);
        cmbMes.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        cmbMes.setVisible(false);
        p.add(cmbMes);

        int ay = LocalDate.now().getYear();
        String[] ay6 = new String[6];
        for (int i = 0; i < 6; i++) ay6[i] = String.valueOf(ay - i);
        cmbAnio = combo(ay6, 82);
        cmbAnio.setVisible(false);
        p.add(cmbAnio);

        cmbTipo.addActionListener(e -> syncFiltroVisibility(p));

        p.add(actionBtn("  Cargar  ", C_BLUE));
        p.add(actionBtn("  Ver todo  ", new Color(0x64748B)));

        // wire buttons
        Component[] comps = p.getComponents();
        for (Component c : comps) {
            if (c instanceof JButton btn) {
                if (btn.getText().contains("Cargar"))   btn.addActionListener(e -> buscar());
                if (btn.getText().contains("Ver todo")) btn.addActionListener(e -> loadAll());
            }
        }
        return p;
    }

    private void syncFiltroVisibility(JPanel p) {
        String t = (String) cmbTipo.getSelectedItem();
        spDia.setVisible("Día".equals(t));
        cmbMes.setVisible("Mes".equals(t));
        cmbAnio.setVisible("Mes".equals(t) || "Año".equals(t));
        p.revalidate(); p.repaint();
    }


    // ── Fila KPI (4 tarjetas) ─────────────────────────────────────────────────
    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        // KPI 1 – Total ingresos
        JPanel k1 = kpiCard(C_BLUE, new Color(0xEFF6FF), "👥", "Total de ingresos");
        kpiTotalVal = kpiValue(k1, "—");
        kpiTotalSub = kpiSubLabel(k1, "vs. período anterior");

        // KPI 2 – Promedio diario
        JPanel k2 = kpiCard(C_GREEN, new Color(0xECFDF5), "📈", "Promedio diario");
        kpiPromVal = kpiValue(k2, "—");
        kpiPromSub = kpiSubLabel(k2, "ingresos por día");

        // KPI 3 – Hora pico
        JPanel k3 = kpiCard(C_PURPLE, new Color(0xF5F3FF), "🕐", "Hora pico promedio");
        kpiHoraVal = kpiValue(k3, "—");
        kpiSubLabel(k3, "mayor flujo de entrada");

        // KPI 4 – Registros válidos
        JPanel k4 = kpiCard(C_ORANGE, new Color(0xFFF7ED), "🛡", "Registros válidos");
        kpiValidVal = kpiValue(k4, "—");
        kpiSubLabel(k4, "consistencia de datos");

        row.add(k1); row.add(k2); row.add(k3); row.add(k4);
        return row;
    }

    private JPanel kpiCard(Color accent, Color bgIco, String ico, String titulo) {
        JPanel p = card();
        p.setLayout(new BorderLayout(12, 0));
        p.setBorder(new EmptyBorder(14, 16, 14, 16));

        // Borde izquierdo de acento
        p.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(14, 16, 14, 16)));

        // Icono circular
        JPanel circle = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgIco);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(ico)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(accent);
                g2.drawString(ico, x, y);
                g2.dispose();
            }
        };
        circle.setOpaque(false);
        circle.setPreferredSize(new Dimension(52, 52));

        JPanel txt = new JPanel();
        txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
        txt.setOpaque(false);

        JLabel lTit = new JLabel(titulo);
        lTit.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lTit.setForeground(TXT_G);
        txt.add(lTit);

        p.add(circle, BorderLayout.WEST);
        p.add(txt, BorderLayout.CENTER);
        p.putClientProperty("txt", txt);
        p.putClientProperty("accent", accent);
        return p;
    }

    private JLabel kpiValue(JPanel card, String v) {
        JPanel txt = (JPanel) card.getClientProperty("txt");
        JLabel l = new JLabel(v);
        l.setFont(new Font("Segoe UI", Font.BOLD, 28));
        l.setForeground(TXT_D);
        txt.add(l);
        return l;
    }

    private JLabel kpiSubLabel(JPanel card, String s) {
        JPanel txt = (JPanel) card.getClientProperty("txt");
        Color accent = (Color) card.getClientProperty("accent");
        JLabel l = new JLabel(s);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        l.setForeground(accent != null ? accent : TXT_G);
        txt.add(l);
        return l;
    }


    // ── Gráfico principal ─────────────────────────────────────────────────────
    private JPanel buildGraficoCard() {
        JPanel c = card();
        c.setLayout(new BorderLayout(0, 8));
        c.setBorder(new EmptyBorder(16, 18, 16, 18));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        // Encabezado
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel t = new JLabel("📊  Ingresos totales por día");
        t.setFont(new Font("Segoe UI", Font.BOLD, 15));
        t.setForeground(TXT_D);

        JPanel leyendaTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        leyendaTop.setOpaque(false);
        leyendaTop.add(legendDot(C_GREEN, "Ingresos diarios (barras)"));
        leyendaTop.add(legendDot(C_BLUE,  "Hora promedio de ingreso (línea)"));

        head.add(t, BorderLayout.WEST);
        head.add(leyendaTop, BorderLayout.EAST);

        grafico = new GraficoBarrasLinea();
        c.add(head,   BorderLayout.NORTH);
        c.add(grafico, BorderLayout.CENTER);
        return c;
    }

    private JPanel legendDot(Color color, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillRoundRect(0, 4, 12, 8, 4, 4);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(14, 16));
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        l.setForeground(TXT_G);
        p.add(dot); p.add(l);
        return p;
    }

    // ── Fila inferior (dona + info + resumen) ─────────────────────────────────
    private JPanel buildBottomRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        row.add(buildDonaCard());
        row.add(buildInfoCard());
        row.add(buildResumenCard());
        return row;
    }

    // Dona
    private JPanel buildDonaCard() {
        JPanel c = card();
        c.setLayout(new BorderLayout(0, 8));
        c.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel t = new JLabel("🍩  Distribución por franja horaria");
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setForeground(TXT_D);

        donaChart = new GraficoDona();
        donaChart.setPreferredSize(new Dimension(110, 110));

        leyendaBox = new JPanel();
        leyendaBox.setLayout(new BoxLayout(leyendaBox, BoxLayout.Y_AXIS));
        leyendaBox.setOpaque(false);

        JPanel body = new JPanel(new BorderLayout(10, 0));
        body.setOpaque(false);
        body.add(donaChart,  BorderLayout.WEST);
        body.add(leyendaBox, BorderLayout.CENTER);

        c.add(t,    BorderLayout.NORTH);
        c.add(body, BorderLayout.CENTER);
        return c;
    }

    // Información del período
    private JPanel buildInfoCard() {
        JPanel c = card();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel t = new JLabel("📋  Información del período");
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setForeground(TXT_D);
        t.setAlignmentX(LEFT_ALIGNMENT);
        c.add(t);
        c.add(vgap(10));

        lPeriodo = infoRow(c, "📅", "Período seleccionado", C_BLUE);   c.add(vgap(6));
        lDias    = infoRow(c, "👥", "Días del período",     TXT_G);    c.add(vgap(6));
        lMax     = infoRow(c, "📈", "Día con más ingresos", C_GREEN);  c.add(vgap(6));
        lMin     = infoRow(c, "📉", "Día con menos ingresos", C_ORANGE);
        return c;
    }

    // Resumen rápido
    private JPanel buildResumenCard() {
        JPanel c = card();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel t = new JLabel("⚡  Resumen rápido");
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setForeground(TXT_D);
        t.setAlignmentX(LEFT_ALIGNMENT);
        c.add(t);
        c.add(vgap(10));

        rTotal = resRow(c, "👥", "Total de ingresos en el período", C_BLUE);   c.add(vgap(6));
        rProm  = resRow(c, "📈", "Promedio diario",                 C_GREEN);  c.add(vgap(6));
        rHora  = resRow(c, "🕐", "Hora pico promedio",              C_PURPLE); c.add(vgap(6));
        rVal   = resRow(c, "🛡", "Registros válidos",               C_ORANGE);
        return c;
    }


    // ── Footer ────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER),
            new EmptyBorder(6, 20, 6, 20)));

        String admin = "";
        try { admin = SessionManager.getInstance().getAdminNombre(); } catch (Exception ignored) {}
        JLabel left = new JLabel("SAIA – Sistema de Apoyo a la Información del Aprendiz  |  " + admin);
        left.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        left.setForeground(TXT_G);

        lblFooter = new JLabel("Última actualización: —");
        lblFooter.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblFooter.setForeground(TXT_G);

        p.add(left,     BorderLayout.WEST);
        p.add(lblFooter, BorderLayout.EAST);
        return p;
    }

    // ── Lógica ────────────────────────────────────────────────────────────────
    private void loadCurrentMonth() {
        desde = LocalDate.now().withDayOfMonth(1);
        hasta = LocalDate.now();
        cmbTipo.setSelectedItem("Mes");
        cmbMes.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        cmbAnio.setSelectedItem(String.valueOf(LocalDate.now().getYear()));
        cmbMes.setVisible(true);
        cmbAnio.setVisible(true);
        spDia.setVisible(false);
        load();
    }

    private void loadAll() {
        desde = LocalDate.now().minusYears(2);
        hasta = LocalDate.now();
        load();
    }

    private void buscar() {
        String tipo = (String) cmbTipo.getSelectedItem();
        switch (tipo == null ? "" : tipo) {
            case "Día" -> {
                java.util.Date d = (java.util.Date) spDia.getValue();
                desde = hasta = d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            }
            case "Mes" -> {
                int m = cmbMes.getSelectedIndex() + 1;
                int a = Integer.parseInt((String) cmbAnio.getSelectedItem());
                desde = LocalDate.of(a, m, 1);
                hasta = desde.withDayOfMonth(desde.lengthOfMonth());
            }
            case "Año" -> {
                int a = Integer.parseInt((String) cmbAnio.getSelectedItem());
                desde = LocalDate.of(a, 1, 1);
                hasta = LocalDate.of(a, 12, 31);
            }
            default -> { loadCurrentMonth(); return; }
        }
        load();
    }

    private void load() {
        final LocalDate d = desde, h = hasta;
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() {
                return new Object[]{
                    dao.totalIngresos(d, h),
                    dao.promedioDiario(d, h),
                    dao.horaPico(d, h),
                    dao.porcentajeValidos(d, h),
                    dao.variacionVsAnterior(d, h),
                    dao.ingresosPorDia(d, h),
                    dao.distribucionFranja(d, h),
                    dao.infoPeriodo(d, h)
                };
            }
            @Override protected void done() {
                try {
                    Object[] r = get();
                    int    total = (int)    r[0];
                    int    prom  = (int)    r[1];
                    String hora  = (String) r[2];
                    String valid = (String) r[3];
                    String var   = (String) r[4];
                    @SuppressWarnings("unchecked") List<PuntoDia>      dias = (List<PuntoDia>)      r[5];
                    @SuppressWarnings("unchecked") List<FranjaHoraria> frs  = (List<FranjaHoraria>) r[6];
                    InfoPeriodo info = (InfoPeriodo) r[7];

                    // KPI
                    kpiTotalVal.setText(String.format("%,d", total).replace(',', '.'));
                    kpiTotalSub.setText(var + " vs. período anterior");
                    kpiPromVal.setText(String.valueOf(prom));
                    kpiPromSub.setText("ingresos por día");
                    kpiHoraVal.setText(hora);
                    kpiValidVal.setText(valid);

                    // Gráfico
                    grafico.setData(dias);

                    // Dona
                    int totFr = frs.stream().mapToInt(f -> f.total).sum();
                    donaChart.setData(frs, totFr);
                    refreshLeyenda(frs);

                    // Info
                    lPeriodo.setText(d.format(FMT_D) + "  →  " + h.format(FMT_D));
                    lDias.setText(info.diasTotales + " días");
                    lMax.setText(info.diaMasIngresos + "  (" + info.maxIngresos + " ingresos)");
                    lMin.setText(info.diaMenosIngresos + "  (" + info.minIngresos + " ingresos)");

                    // Resumen
                    rTotal.setText(String.format("%,d", total).replace(',', '.'));
                    rProm.setText(String.valueOf(prom));
                    rHora.setText(hora);
                    rVal.setText(valid);

                    lblFooter.setText("Última actualización: " +
                            LocalDateTime.now().format(FMT_TS));
                } catch (InterruptedException | ExecutionException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void refreshLeyenda(List<FranjaHoraria> frs) {
        leyendaBox.removeAll();
        for (FranjaHoraria f : frs) {
            Color c = new Color(f.color.r, f.color.g, f.color.b);
            JPanel row = new JPanel(new BorderLayout(5, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

            JPanel dot = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(c);
                    g2.fillOval(1, 4, 10, 10);
                    g2.dispose();
                }
            };
            dot.setOpaque(false);
            dot.setPreferredSize(new Dimension(13, 18));

            JPanel labelBlock = new JPanel(new BorderLayout(2, 0));
            labelBlock.setOpaque(false);
            JLabel lbl = new JLabel(f.label);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lbl.setForeground(TXT_D);
            JLabel val = new JLabel(f.porcentaje + "% · " + f.total);
            val.setFont(new Font("Segoe UI", Font.BOLD, 10));
            val.setForeground(TXT_G);
            labelBlock.add(lbl, BorderLayout.CENTER);
            labelBlock.add(val, BorderLayout.EAST);

            row.add(dot, BorderLayout.WEST);
            row.add(labelBlock, BorderLayout.CENTER);
            leyendaBox.add(row);
            leyendaBox.add(vgap(2));
        }
        leyendaBox.revalidate();
        leyendaBox.repaint();
    }


    // ── Helpers UI ────────────────────────────────────────────────────────────
    private JPanel card() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Sombra suave
                g2.setColor(new Color(0, 0, 0, 12));
                g2.fill(new RoundRectangle2D.Float(2, 3, getWidth()-3, getHeight()-3, 12, 12));
                g2.setColor(CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-2, getHeight()-2, 12, 12));
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-3, getHeight()-3, 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
    }

    private JLabel infoRow(JPanel p, String ico, String label, Color valColor) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel k = new JLabel(ico + "  " + label);
        k.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        k.setForeground(TXT_G);
        JLabel v = new JLabel("—");
        v.setFont(new Font("Segoe UI", Font.BOLD, 11));
        v.setForeground(valColor);
        row.add(k, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        p.add(row);
        return v;
    }

    private JLabel resRow(JPanel p, String ico, String label, Color color) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBorder(new EmptyBorder(2, 0, 2, 0));
        JLabel k = new JLabel(ico + "  " + label);
        k.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        k.setForeground(TXT_G);
        JLabel v = new JLabel("—");
        v.setFont(new Font("Segoe UI", Font.BOLD, 16));
        v.setForeground(color);
        row.add(k, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        p.add(row);
        return v;
    }

    private static JComboBox<String> combo(String[] items, int w) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        c.setBackground(Color.WHITE);
        c.setPreferredSize(new Dimension(w, 30));
        return c;
    }

    private static JLabel boldLabel(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(0x374151));
        return l;
    }

    private static JButton actionBtn(String txt, Color bg) {
        JButton b = new JButton(txt) {
            boolean hov;
            { addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { hov = true; repaint(); }
                @Override public void mouseExited (java.awt.event.MouseEvent e) { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? bg.darker() : bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(90, 30));
        return b;
    }

    private static Component vgap(int h) { return Box.createVerticalStrut(h); }

    private static java.util.Date toDate(LocalDate d) {
        return java.util.Date.from(d.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
    }


    // ═══════════════════════════════════════════════════════════════════════════
    // GRÁFICO BARRAS + LÍNEA (eje Y izq: cantidad, eje Y der: hora del día)
    // ═══════════════════════════════════════════════════════════════════════════
    static class GraficoBarrasLinea extends JPanel {
        private List<PuntoDia> datos;

        GraficoBarrasLinea() {
            setBackground(Color.WHITE);
            setOpaque(true);
        }

        void setData(List<PuntoDia> d) { this.datos = d; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int W = getWidth(), H = getHeight();
            int pL = 52, pR = 52, pT = 36, pB = 46;
            int gW = W - pL - pR, gH = H - pT - pB;

            // Fondo gráfico
            g2.setColor(new Color(0xF8FAFC));
            g2.fillRect(pL, pT, gW, gH);

            if (datos == null || datos.isEmpty()) {
                g2.setColor(new Color(0xA0AEC0));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                String msg = "Sin datos para el período seleccionado";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (W - fm.stringWidth(msg)) / 2, H / 2);
                g2.dispose(); return;
            }

            int n = datos.size();
            int maxBar = datos.stream().mapToInt(p -> p.total).max().orElse(1);
            maxBar = (int)(maxBar * 1.20) + 1;

            // Líneas de cuadrícula y etiquetas eje Y izq (cantidad)
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            int gridLines = 5;
            for (int i = 0; i <= gridLines; i++) {
                int y = pT + gH - i * gH / gridLines;
                g2.setColor(new Color(0xE2E8F0));
                g2.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                        0, new float[]{3, 3}, 0));
                g2.drawLine(pL, y, pL + gW, y);
                g2.setColor(new Color(0x718096));
                String lbl = String.valueOf(i * maxBar / gridLines);
                g2.drawString(lbl, pL - g2.getFontMetrics().stringWidth(lbl) - 5, y + 4);
            }

            // Eje Y derecho (hora 0-24)
            for (int h = 0; h <= 24; h += 6) {
                int y = pT + gH - (int)((double) h / 24 * gH);
                g2.setColor(new Color(0x93C5FD));
                g2.setStroke(new BasicStroke(0.5f));
                g2.drawLine(pL + gW, y, pL + gW + 4, y);
                g2.setColor(new Color(0x2563EB));
                String lbl = h + ":00";
                g2.drawString(lbl, pL + gW + 6, y + 4);
            }

            // Barras + línea hora
            int bw = Math.max(4, gW / (n + 1) - 3);
            int[] lx = new int[n], ly = new int[n];
            for (int i = 0; i < n; i++) {
                int xBar = pL + i * gW / n + (gW / n - bw) / 2;
                int bh   = (int)((double) datos.get(i).total / maxBar * gH);
                int yBar = pT + gH - bh;

                // Barra con gradiente
                GradientPaint gp = new GradientPaint(
                        xBar, yBar, new Color(0x34D399),
                        xBar, pT + gH, new Color(0x059669));
                g2.setPaint(gp);
                g2.setStroke(new BasicStroke(1));
                g2.fill(new RoundRectangle2D.Float(xBar, yBar, bw, bh, 3, 3));
                g2.setColor(new Color(0x065F46));
                g2.draw(new RoundRectangle2D.Float(xBar, yBar, bw, bh, 3, 3));

                // Valor sobre la barra
                if (bh > 14) {
                    g2.setColor(new Color(0x1F2937));
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                    String sv = String.valueOf(datos.get(i).total);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(sv, xBar + (bw - fm.stringWidth(sv)) / 2, yBar - 3);
                }

                // Hora promedio sobre barra (pequeña)
                if (bh > 26 && datos.get(i).horaPromedio != null && !"—".equals(datos.get(i).horaPromedio)) {
                    g2.setColor(new Color(0xFFFFFF, true));
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 7));
                    String sh = datos.get(i).horaPromedio;
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(sh, xBar + (bw - fm.stringWidth(sh)) / 2, yBar + 11);
                }

                // Etiqueta eje X
                int step = Math.max(1, n / 12);
                if (i % step == 0 || i == n - 1) {
                    g2.setColor(new Color(0x4A5568));
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                    String ex = datos.get(i).fecha;
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(ex, xBar + bw / 2 - fm.stringWidth(ex) / 2, pT + gH + 14);
                }

                // Puntos para la línea (hora decimal mapeada a eje Y der)
                double hd = datos.get(i).horaDecimal;
                lx[i] = xBar + bw / 2;
                ly[i] = pT + gH - (int)(hd / 24.0 * gH);
            }

            // Línea azul de hora promedio
            if (n > 1) {
                g2.setColor(new Color(0x2563EB));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 1; i < n; i++) g2.drawLine(lx[i-1], ly[i-1], lx[i], ly[i]);
                for (int i = 0; i < n; i++) {
                    g2.setColor(Color.WHITE);
                    g2.fillOval(lx[i]-4, ly[i]-4, 8, 8);
                    g2.setColor(new Color(0x2563EB));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawOval(lx[i]-4, ly[i]-4, 8, 8);
                }
            }

            // Ejes
            g2.setColor(new Color(0xCBD5E1));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(pL, pT, pL, pT + gH);
            g2.drawLine(pL, pT + gH, pL + gW, pT + gH);

            // Etiquetas de ejes
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(new Color(0x718096));
            g2.drawString("Ingresos", 2, pT - 8);
            g2.drawString("Hora", pL + gW + 4, pT - 8);

            g2.dispose();
        }
    }


    // ═══════════════════════════════════════════════════════════════════════════
    // GRÁFICO DE DONA (franjas horarias)
    // ═══════════════════════════════════════════════════════════════════════════
    static class GraficoDona extends JPanel {
        private List<FranjaHoraria> franjas;
        private int total;

        GraficoDona() { setOpaque(false); }

        void setData(List<FranjaHoraria> f, int t) { franjas = f; total = t; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (franjas == null || franjas.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int sz = Math.min(getWidth(), getHeight()) - 8;
            int x  = (getWidth() - sz) / 2;
            int y  = (getHeight() - sz) / 2;
            float start = 90f;

            for (FranjaHoraria f : franjas) {
                if (total == 0) continue;
                float sweep = (float) f.total / total * 360f;
                Color c = new Color(f.color.r, f.color.g, f.color.b);
                g2.setColor(c);
                g2.fill(new Arc2D.Float(x, y, sz, sz, start, -sweep, Arc2D.PIE));
                // Borde blanco entre segmentos
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new Arc2D.Float(x, y, sz, sz, start, -sweep, Arc2D.PIE));
                start -= sweep;
            }

            // Agujero central (dona)
            int hole = (int)(sz * 0.46);
            g2.setColor(Color.WHITE);
            g2.fillOval(x + (sz - hole) / 2, y + (sz - hole) / 2, hole, hole);

            // Total en el centro
            g2.setColor(new Color(0x1A202C));
            g2.setFont(new Font("Segoe UI", Font.BOLD, (int)(hole * 0.30)));
            FontMetrics fm = g2.getFontMetrics();
            String tt = String.valueOf(total);
            g2.drawString(tt, x + sz/2 - fm.stringWidth(tt)/2,
                          y + sz/2 + fm.getAscent()/2 - 2);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
            FontMetrics fm2 = g2.getFontMetrics();
            g2.setColor(new Color(0x718096));
            String sub = "total";
            g2.drawString(sub, x + sz/2 - fm2.stringWidth(sub)/2,
                          y + sz/2 + fm.getAscent()/2 + 10);

            g2.dispose();
        }
    }
}
