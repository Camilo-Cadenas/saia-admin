package com.saia.presentation.home;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import com.saia.business.ReporteExportService;
import com.saia.data.GeneradorReportesDAO;
import com.saia.model.FiltrosReporte;
import com.saia.model.FormatoDescarga;
import com.saia.model.TipoReporte;

/**
 * Panel "Descarga de Reportes" — diseño completo con:
 *  1. Banner informativo (ocultable)
 *  2. Selector de tipo de reporte (tarjetas)
 *  3. Filtros (fecha, programa, estado, sede, jornada, guarda)
 *  4. Formato de descarga (Excel / PDF / CSV)
 *  5. Pie de acciones (Vista previa + Descargar)
 */
public class DescargaReportesPanel extends JPanel {

    // ── Paleta ────────────────────────────────────────────────────────────────
    static final Color BG       = new Color(0xF0F2F5);
    static final Color CARD     = Color.WHITE;
    static final Color BORDER   = new Color(0xE2E8F0);
    static final Color TXT_D    = new Color(0x1A202C);
    static final Color TXT_G    = new Color(0x718096);
    static final Color C_GREEN  = new Color(0x059669);
    static final Color C_BLUE   = new Color(0x2563EB);
    static final Color NAVY     = new Color(0x1A3A5C);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Estado ────────────────────────────────────────────────────────────────
    private TipoReporte      tipoSeleccionado   = null;
    private FormatoDescarga  formatoSeleccionado = FormatoDescarga.EXCEL;
    private final Map<TipoReporte,     TarjetaTipo>   tarjetasTipo    = new EnumMap<>(TipoReporte.class);
    private final Map<FormatoDescarga, TarjetaFormato> tarjetasFormato = new EnumMap<>(FormatoDescarga.class);

    // ── Filtros UI ────────────────────────────────────────────────────────────
    private JSpinner   spFechaIni, spFechaFin;
    private JComboBox<String> cmbPrograma, cmbEstado, cmbSede, cmbJornada, cmbGuarda;

    // ── Acciones ──────────────────────────────────────────────────────────────
    private JButton btnDescargar, btnPreview;

    // ── DAOs / servicios ──────────────────────────────────────────────────────
    private final GeneradorReportesDAO dao     = new GeneradorReportesDAO();
    private final ReporteExportService exporter = new ReporteExportService();

    // ── Banner ────────────────────────────────────────────────────────────────
    private JPanel bannerPanel;

    public DescargaReportesPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(16, 20, 16, 20));
        buildUI();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BUILD UI
    // ═════════════════════════════════════════════════════════════════════════
    private void buildUI() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setOpaque(false);

        bannerPanel = buildBanner();
        root.add(bannerPanel);
        root.add(vgap(12));
        root.add(buildSeccionTipo());
        root.add(vgap(12));
        root.add(buildSeccionFiltros());
        root.add(vgap(12));
        root.add(buildSeccionFormato());
        root.add(vgap(14));
        root.add(buildPieAcciones());

        JScrollPane scroll = new JScrollPane(root);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
    }

    // ── Banner informativo ────────────────────────────────────────────────────
    private JPanel buildBanner() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(new Color(0xEFF6FF));
        p.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(0xBFDBFE), 1, true),
            new EmptyBorder(12, 16, 12, 16)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel ico = new JLabel("ℹ");
        ico.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        ico.setForeground(new Color(0x2563EB));

        JPanel txt = new JPanel();
        txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
        txt.setOpaque(false);
        JLabel titulo = new JLabel("¿Cómo funciona?");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titulo.setForeground(new Color(0x1E40AF));
        JLabel desc = new JLabel(
            "Selecciona el tipo de reporte, configura los filtros, elige el formato y descarga.");
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        desc.setForeground(new Color(0x1E40AF));
        txt.add(titulo); txt.add(desc);

        left.add(ico); left.add(txt);

        JButton btnX = new JButton("✕");
        btnX.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnX.setForeground(new Color(0x6B7280));
        btnX.setOpaque(false); btnX.setContentAreaFilled(false);
        btnX.setBorderPainted(false); btnX.setFocusPainted(false);
        btnX.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnX.addActionListener(e -> {
            p.setVisible(false);
            p.getParent().revalidate();
        });

        p.add(left,  BorderLayout.CENTER);
        p.add(btnX,  BorderLayout.EAST);
        return p;
    }

    // ── Sección 1 — Tipo de reporte ───────────────────────────────────────────
    private JPanel buildSeccionTipo() {
        JPanel sec = seccion("1. Tipo de reporte");

        JPanel cards = new JPanel(new GridLayout(1, TipoReporte.values().length, 10, 0));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        for (TipoReporte t : TipoReporte.values()) {
            TarjetaTipo tc = new TarjetaTipo(t);
            tc.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    onTipoReporteSelected(t);
                }
            });
            tarjetasTipo.put(t, tc);
            cards.add(tc);
        }

        sec.add(cards);
        return wrap(sec);
    }

    // ── Sección 2 — Filtros ───────────────────────────────────────────────────
    private JPanel buildSeccionFiltros() {
        JPanel sec = seccion("2. Filtros del reporte");

        JPanel grid = new JPanel(new GridLayout(2, 4, 12, 10));
        grid.setOpaque(false);

        // Fila 1
        grid.add(labeledFilter("Fecha inicio", buildDateSpinner(LocalDate.now().withDayOfMonth(1))));
        grid.add(labeledFilter("Fecha fin",    buildDateSpinner(LocalDate.now())));
        grid.add(labeledFilter("Programa",
            cmbPrograma = comboBox(List.of("Todos"))));
        grid.add(labeledFilter("Estado del aprendiz",
            cmbEstado = comboBox(List.of("Todos", "Activo", "Inactivo"))));

        // Fila 2
        grid.add(labeledFilter("Sede / Centro",
            cmbSede = comboBox(List.of("Todos"))));
        grid.add(labeledFilter("Jornada",
            cmbJornada = comboBox(List.of("Todos"))));
        grid.add(labeledFilter("Guarda / Personal seguridad",
            cmbGuarda = comboBox(List.of("Todos"))));
        grid.add(buildLimpiarBtn());

        sec.add(grid);

        // Cargar combos desde BD en segundo plano
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            java.util.List<String> programas, centros, jornadas, guardas;
            @Override protected Void doInBackground() {
                programas = dao.getProgramas();
                centros   = dao.getCentros();
                jornadas  = dao.getJornadas();
                guardas   = dao.getGuardas();
                return null;
            }
            @Override protected void done() {
                updateCombo(cmbPrograma, programas);
                updateCombo(cmbSede,     centros);
                updateCombo(cmbJornada,  jornadas);
                updateCombo(cmbGuarda,   guardas);
            }
        };
        w.execute();

        return wrap(sec);
    }

    // ── Sección 3 — Formato de descarga ──────────────────────────────────────
    private JPanel buildSeccionFormato() {
        JPanel sec = seccion("3. Formato de descarga");

        JPanel cards = new JPanel(new GridLayout(1, FormatoDescarga.values().length, 10, 0));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        for (FormatoDescarga f : FormatoDescarga.values()) {
            TarjetaFormato tf = new TarjetaFormato(f);
            tf.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    onFormatoSelected(f);
                }
            });
            tarjetasFormato.put(f, tf);
            cards.add(tf);
        }
        // Excel seleccionado por defecto
        tarjetasFormato.get(FormatoDescarga.EXCEL).setSelected(true);

        sec.add(cards);
        return wrap(sec);
    }

    // ── Pie de acciones ───────────────────────────────────────────────────────
    private JPanel buildPieAcciones() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        btnPreview = outlineBtn("👁  Vista previa", C_BLUE);
        btnPreview.addActionListener(e -> onVistaPrevia());

        btnDescargar = solidBtn("⬇  Descargar reporte", C_GREEN);
        btnDescargar.setEnabled(false);
        btnDescargar.addActionListener(e -> onDescargarReporte());

        p.add(btnPreview);
        p.add(btnDescargar);
        return p;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CONTROLLER METHODS
    // ═════════════════════════════════════════════════════════════════════════

    /** Invocado al hacer clic en una tarjeta de tipo. */
    private void onTipoReporteSelected(TipoReporte tipo) {
        tipoSeleccionado = tipo;
        tarjetasTipo.values().forEach(t -> t.setSelected(false));
        tarjetasTipo.get(tipo).setSelected(true);
        validarEstadoDescargar();
    }

    /** Invocado al hacer clic en una tarjeta de formato. */
    private void onFormatoSelected(FormatoDescarga fmt) {
        formatoSeleccionado = fmt;
        tarjetasFormato.values().forEach(t -> t.setSelected(false));
        tarjetasFormato.get(fmt).setSelected(true);
    }

    /** Resetea todos los filtros a sus valores por defecto. */
    private void onLimpiarFiltros() {
        setSpinnerDate(spFechaIni, LocalDate.now().withDayOfMonth(1));
        setSpinnerDate(spFechaFin, LocalDate.now());
        cmbPrograma.setSelectedIndex(0);
        cmbEstado.setSelectedIndex(0);
        cmbSede.setSelectedIndex(0);
        cmbJornada.setSelectedIndex(0);
        cmbGuarda.setSelectedIndex(0);
    }

    /** Muestra un diálogo modal con los datos filtrados (vista previa). */
    private void onVistaPrevia() {
        FiltrosReporte filtros = leerFiltros();
        TipoReporte tipo = tipoSeleccionado != null
                ? tipoSeleccionado : TipoReporte.HISTORIAL_INGRESOS;

        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner, "Vista previa — " + tipo.titulo, true);
        dlg.setSize(900, 560);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(BG);

        JLabel lCargando = new JLabel("Cargando datos…", SwingConstants.CENTER);
        lCargando.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dlg.add(lCargando, BorderLayout.CENTER);
        dlg.setVisible(false); // se mostrará tras cargar

        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() {
                List<String> cols = GeneradorReportesDAO.getColumnas(tipo);
                List<Map<String, Object>> rows = dao.getData(tipo, filtros);
                return new Object[]{cols, rows};
            }
            @Override protected void done() {
                try {
                    Object[] r = get();
                    @SuppressWarnings("unchecked") List<String> cols = (List<String>) r[0];
                    @SuppressWarnings("unchecked") List<Map<String,Object>> rows =
                            (List<Map<String,Object>>) r[1];
                    dlg.getContentPane().removeAll();
                    dlg.add(buildPreviewContent(tipo, cols, rows), BorderLayout.CENTER);
                    dlg.revalidate();
                    dlg.setVisible(true);
                } catch (InterruptedException | ExecutionException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
        dlg.setVisible(true);
    }

    /** Ejecuta la generación y descarga del archivo. */
    private void onDescargarReporte() {
        if (tipoSeleccionado == null) {
            JOptionPane.showMessageDialog(this,
                "Selecciona un tipo de reporte primero.",
                "Reporte", JOptionPane.WARNING_MESSAGE);
            return;
        }
        FiltrosReporte filtros = leerFiltros();
        if (!filtros.isFechasValidas()) {
            JOptionPane.showMessageDialog(this,
                "La fecha de inicio no puede ser posterior a la fecha fin.",
                "Fechas inválidas", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Selecciona la carpeta de destino");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File dir = fc.getSelectedFile();

        btnDescargar.setEnabled(false);
        btnDescargar.setText("⏳  Generando…");

        final TipoReporte tipo = tipoSeleccionado;
        final FormatoDescarga fmt = formatoSeleccionado;

        new SwingWorker<File, Void>() {
            @Override protected File doInBackground() throws Exception {
                List<String>           cols = GeneradorReportesDAO.getColumnas(tipo);
                List<Map<String, Object>> rows = dao.getData(tipo, filtros);
                return exporter.exportar(dir, tipo, filtros, fmt, cols, rows);
            }
            @Override protected void done() {
                btnDescargar.setEnabled(true);
                btnDescargar.setText("⬇  Descargar reporte");
                try {
                    File archivo = get();
                    int resp = JOptionPane.showConfirmDialog(DescargaReportesPanel.this,
                        "<html>Reporte generado correctamente.<br>" +
                        "<b>" + archivo.getName() + "</b><br><br>" +
                        "¿Abrir la carpeta de destino?</html>",
                        "Descarga completada", JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);
                    if (resp == JOptionPane.YES_OPTION && Desktop.isDesktopSupported())
                        Desktop.getDesktop().open(archivo.getParentFile());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DescargaReportesPanel.this,
                        "Error al generar el reporte:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPERS — LÓGICA
    // ═════════════════════════════════════════════════════════════════════════

    private FiltrosReporte leerFiltros() {
        FiltrosReporte f = new FiltrosReporte();
        f.setFechaInicio(getSpinnerDate(spFechaIni));
        f.setFechaFin   (getSpinnerDate(spFechaFin));
        // Para programa y sede, el combo muestra "SIGLA - Nombre"; extraemos la sigla
        f.setPrograma       (extraerSigla(comboVal(cmbPrograma)));
        f.setEstadoAprendiz (comboVal(cmbEstado));
        f.setSedeCentro     (extraerSigla(comboVal(cmbSede)));
        f.setJornada        (comboVal(cmbJornada));
        f.setGuarda         (comboVal(cmbGuarda));
        return f;
    }

    /** Extrae la sigla del formato "SIGLA - Descripción". Si no hay guion, devuelve el valor completo. */
    private static String extraerSigla(String v) {
        if (v == null) return null;
        int idx = v.indexOf(" - ");
        return idx > 0 ? v.substring(0, idx).trim() : v;
    }

    private String comboVal(JComboBox<String> c) {
        String v = (String) c.getSelectedItem();
        return (v == null || "Todos".equals(v)) ? null : v;
    }

    private void validarEstadoDescargar() {
        btnDescargar.setEnabled(tipoSeleccionado != null);
    }

    private void updateCombo(JComboBox<String> cmb, List<String> items) {
        cmb.removeAllItems();
        items.forEach(cmb::addItem);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PREVIEW CONTENT
    // ═════════════════════════════════════════════════════════════════════════

    private JPanel buildPreviewContent(TipoReporte tipo,
                                        List<String> cols,
                                        List<Map<String, Object>> rows) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Encabezado
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel t = new JLabel(tipo.icono + "  " + tipo.titulo +
                "  —  " + rows.size() + " registros");
        t.setFont(new Font("Segoe UI", Font.BOLD, 15));
        t.setForeground(TXT_D);

        JLabel sub = new JLabel("Vista previa (máx. 100 filas)  |  " +
                LocalDate.now().format(FMT));
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(TXT_G);
        head.add(t,   BorderLayout.WEST);
        head.add(sub, BorderLayout.EAST);
        p.add(head, BorderLayout.NORTH);

        // Tabla
        String[] colArr = cols.toArray(new String[0]);
        DefaultTableModel model = new DefaultTableModel(colArr, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        int limit = Math.min(rows.size(), 100);
        for (int i = 0; i < limit; i++) {
            Map<String, Object> row = rows.get(i);
            Object[] arr = row.values().toArray();
            model.addRow(arr);
        }

        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(28);
        table.setBackground(CARD);
        table.setGridColor(new Color(0xF0F0F0));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(0xEFF6FF));
        table.setFocusable(false);

        JTableHeader header = table.getTableHeader();
        header.setBackground(NAVY);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setReorderingAllowed(false);

        // Alternar colores de fila
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            final Color ODD = new Color(0xF8FAFC);
            @Override public Component getTableCellRendererComponent(
                    JTable tbl, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(tbl, v, sel, foc, r, c);
                if (!sel) comp.setBackground(r % 2 == 0 ? Color.WHITE : ODD);
                ((JLabel) comp).setBorder(new EmptyBorder(0, 8, 0, 8));
                return comp;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BORDER, 1, true));
        scroll.getViewport().setBackground(CARD);
        p.add(scroll, BorderLayout.CENTER);

        // Footer del diálogo
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        foot.setOpaque(false);
        JButton close = solidBtn("  Cerrar  ", new Color(0x64748B));
        close.addActionListener(e ->
            SwingUtilities.getWindowAncestor((Component) e.getSource()).dispose());
        foot.add(close);
        p.add(foot, BorderLayout.SOUTH);

        return p;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPERS — UI BUILDERS
    // ═════════════════════════════════════════════════════════════════════════

    private JPanel seccion(String titulo) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        JLabel lbl = new JLabel(titulo);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(TXT_D);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        lbl.setBorder(new EmptyBorder(0, 0, 8, 0));
        p.add(lbl);
        return p;
    }

    private JPanel wrap(JPanel content) {
        JPanel card = card();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel card() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 10));
                g2.fill(new RoundRectangle2D.Float(2, 3, getWidth()-3, getHeight()-3, 12, 12));
                g2.setColor(CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-2, getHeight()-2, 12, 12));
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-3, getHeight()-3, 12, 12));
                g2.dispose(); super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
    }

    private JPanel labeledFilter(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(TXT_G);
        p.add(lbl,  BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private JSpinner buildDateSpinner(LocalDate initial) {
        javax.swing.SpinnerDateModel m = new javax.swing.SpinnerDateModel();
        m.setValue(toDate(initial));
        JSpinner s = new JSpinner(m);
        s.setEditor(new JSpinner.DateEditor(s, "dd/MM/yyyy"));
        s.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        s.setPreferredSize(new Dimension(0, 32));
        if (spFechaIni == null) spFechaIni = s; else spFechaFin = s;
        return s;
    }

    private JComboBox<String> comboBox(List<String> items) {
        JComboBox<String> c = new JComboBox<>(items.toArray(new String[0]));
        c.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        c.setBackground(Color.WHITE);
        c.setPreferredSize(new Dimension(0, 32));
        return c;
    }

    private JPanel buildLimpiarBtn() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel lbl = new JLabel(" ");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        p.add(lbl, BorderLayout.NORTH);
        JButton btn = outlineBtn("↺  Limpiar filtros", new Color(0x64748B));
        btn.addActionListener(e -> onLimpiarFiltros());
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        btn.setPreferredSize(new Dimension(0, 32));
        p.add(btn, BorderLayout.CENTER);
        return p;
    }

    private static JButton solidBtn(String text, Color bg) {
        JButton b = new JButton(text) {
            boolean hov;
            { addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hov = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? bg.darker() : bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 8, 8));
                g2.dispose(); super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(Color.WHITE); b.setOpaque(false);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(170, 38));
        return b;
    }

    private static JButton outlineBtn(String text, Color accent) {
        JButton b = new JButton(text) {
            boolean hov;
            { addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hov = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = hov ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 18)
                               : Color.WHITE;
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 8, 8));
                g2.setColor(accent); g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-2, getHeight()-2, 8, 8));
                g2.dispose(); super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(accent); b.setOpaque(false);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(155, 38));
        return b;
    }

    private static Component vgap(int h) { return Box.createVerticalStrut(h); }

    private static java.util.Date toDate(LocalDate d) {
        return java.util.Date.from(d.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate getSpinnerDate(JSpinner s) {
        java.util.Date d = (java.util.Date) s.getValue();
        return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private static void setSpinnerDate(JSpinner s, LocalDate d) {
        s.setValue(toDate(d));
    }


    // ═════════════════════════════════════════════════════════════════════════
    // TARJETA TIPO DE REPORTE
    // ═════════════════════════════════════════════════════════════════════════

    static class TarjetaTipo extends JPanel {
        private boolean selected = false;
        private boolean hovered  = false;
        private final TipoReporte tipo;

        private static final Color SEL_BORDER = new Color(0x059669);
        private static final Color SEL_BG     = new Color(0xF0FDF4);

        TarjetaTipo(TipoReporte tipo) {
            this.tipo = tipo;
            setLayout(new BorderLayout(0, 6));
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(14, 10, 12, 10));

            // Icono circular
            Color accent = new Color(tipo.colorHex);
            JPanel circle = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color bg = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 22);
                    g2.setColor(bg); g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
                    FontMetrics fm = g2.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(tipo.icono)) / 2;
                    int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                    g2.setColor(accent);
                    g2.drawString(tipo.icono, x, y);
                    g2.dispose();
                }
            };
            circle.setOpaque(false);
            circle.setPreferredSize(new Dimension(48, 48));

            JPanel circleCtr = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            circleCtr.setOpaque(false);
            circleCtr.add(circle);

            JPanel txt = new JPanel();
            txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
            txt.setOpaque(false);

            JLabel lTitulo = new JLabel(tipo.titulo, SwingConstants.CENTER);
            lTitulo.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lTitulo.setForeground(TXT_D);
            lTitulo.setAlignmentX(CENTER_ALIGNMENT);

            JLabel lDesc = new JLabel("<html><center>" + tipo.descripcion + "</center></html>",
                    SwingConstants.CENTER);
            lDesc.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lDesc.setForeground(TXT_G);
            lDesc.setAlignmentX(CENTER_ALIGNMENT);

            txt.add(lTitulo);
            txt.add(Box.createVerticalStrut(3));
            txt.add(lDesc);

            add(circleCtr, BorderLayout.NORTH);
            add(txt,       BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }

        void setSelected(boolean v) { selected = v; repaint(); }
        boolean isSelected() { return selected; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Sombra
            g2.setColor(new Color(0, 0, 0, selected ? 18 : 8));
            g2.fill(new RoundRectangle2D.Float(2, 3, getWidth()-3, getHeight()-3, 12, 12));

            // Fondo
            Color bg = selected ? SEL_BG : (hovered ? new Color(0xF8FAFC) : CARD);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-2, getHeight()-2, 12, 12));

            // Borde
            Color border = selected ? SEL_BORDER : (hovered ? new Color(0xCBD5E1) : BORDER);
            g2.setColor(border);
            g2.setStroke(new BasicStroke(selected ? 2f : 1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-3, getHeight()-3, 12, 12));

            g2.dispose();
            super.paintComponent(g);
        }

        @Override protected void paintChildren(Graphics g) {
            super.paintChildren(g);
            // Check verde en esquina superior derecha cuando está seleccionado
            if (selected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() - 14, cy = 10, r = 9;
                g2.setColor(SEL_BORDER);
                g2.fillOval(cx - r, cy - r, r*2, r*2);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                g2.drawString("✓", cx - 4, cy + 4);
                g2.dispose();
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TARJETA FORMATO DE DESCARGA
    // ═════════════════════════════════════════════════════════════════════════

    static class TarjetaFormato extends JPanel {
        private boolean selected = false;
        private boolean hovered  = false;
        private final FormatoDescarga formato;

        private static final Color SEL_BORDER = new Color(0x059669);
        private static final Color SEL_BG     = new Color(0xF0FDF4);

        // Colores por formato
        private static final Map<FormatoDescarga, Color> COLORES = Map.of(
            FormatoDescarga.EXCEL, new Color(0x059669),
            FormatoDescarga.PDF,   new Color(0xDC2626),
            FormatoDescarga.CSV,   new Color(0xEA580C)
        );

        TarjetaFormato(FormatoDescarga formato) {
            this.formato = formato;
            setLayout(new BorderLayout(8, 0));
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(12, 14, 12, 14));

            Color accent = COLORES.getOrDefault(formato, new Color(0x2563EB));

            // Radio button dibujado
            JPanel radio = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(selected ? SEL_BORDER : new Color(0xCBD5E1));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawOval(1, 1, 14, 14);
                    if (selected) { g2.setColor(SEL_BORDER); g2.fillOval(4, 4, 8, 8); }
                    g2.dispose();
                }
            };
            radio.setOpaque(false);
            radio.setPreferredSize(new Dimension(18, 18));

            JPanel radioWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
            radioWrap.setOpaque(false);
            radioWrap.add(radio);

            // Contenido
            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setOpaque(false);

            JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            titleRow.setOpaque(false);
            JLabel ico = new JLabel(formato.icono);
            ico.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
            ico.setForeground(accent);
            JLabel title = new JLabel(formato.titulo + "  " + formato.extension);
            title.setFont(new Font("Segoe UI", Font.BOLD, 12));
            title.setForeground(TXT_D);
            titleRow.add(ico); titleRow.add(title);

            JLabel desc = new JLabel(formato.descripcion);
            desc.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            desc.setForeground(TXT_G);

            body.add(titleRow);
            body.add(Box.createVerticalStrut(2));
            body.add(desc);

            add(radioWrap, BorderLayout.WEST);
            add(body,      BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }

        void setSelected(boolean v) { selected = v; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, selected ? 16 : 7));
            g2.fill(new RoundRectangle2D.Float(2, 3, getWidth()-3, getHeight()-3, 10, 10));
            Color bg = selected ? SEL_BG : (hovered ? new Color(0xF8FAFC) : CARD);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-2, getHeight()-2, 10, 10));
            Color border = selected ? SEL_BORDER : (hovered ? new Color(0xCBD5E1) : BORDER);
            g2.setColor(border);
            g2.setStroke(new BasicStroke(selected ? 2f : 1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-3, getHeight()-3, 10, 10));
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
