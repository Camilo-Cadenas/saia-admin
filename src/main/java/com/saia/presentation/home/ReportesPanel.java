package com.saia.presentation.home;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import com.saia.data.ReporteConsultaDAO;
import com.saia.data.ReporteConsultaDAO.EstadisticaPunto;
import com.saia.data.ReporteConsultaDAO.FilaReporte;
import com.saia.data.ReporteConsultaDAO.ResumenPeriodo;

/**
 * Panel "Gestión de Reportes" — dos pestañas:
 *   1. Reportes realizados por guardas (reporte_rechazo)
 *   2. Estadísticas de Ingresos/Salidas (historial_ingreso)
 */
public class ReportesPanel extends JPanel {

    // ── Colores ───────────────────────────────────────────────────────────────
    private static final Color NAVY      = new Color(0x1A3A5C);
    private static final Color GREEN     = new Color(0x2E7D32);
    private static final Color BG_PAGE   = new Color(0xF5F6FA);
    private static final Color CARD_BG   = Color.WHITE;
    private static final Color BORDER_C  = new Color(0xDDE1E7);
    private static final Color TEXT_DARK = new Color(0x1A1A2E);
    private static final Color TEXT_GRAY = new Color(0x5A6474);

    private static final DateTimeFormatter FMT_TS   = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReporteConsultaDAO dao = new ReporteConsultaDAO();

    // ── Pestaña 1 — Reportes ──────────────────────────────────────────────────
    private JComboBox<String> cmbTipoBusqueda; // Todos / Día / Mes / Año
    private JSpinner          spDia;           // date spinner para Día
    private JComboBox<String> cmbMes;          // 1-12 para Mes
    private JComboBox<String> cmbAnio;         // año para Mes y Año
    private DefaultTableModel reporteModel;
    private JTable            reporteTable;
    private JLabel            lblConteo;
    private JLabel            lblStatTotal, lblStatHoy, lblStatGuardas, lblStatAprendices;
    private List<FilaReporte> listaReportes = new ArrayList<>();

    // ── Pestaña 2 — Estadísticas ──────────────────────────────────────────────
    private JComboBox<String> cmbTipoEstad;  // Día / Mes / Año / Todos
    private JSpinner          spDiaEstad;    // date picker para Día
    private JComboBox<String> cmbMesEstad;   // mes 1-12
    private JComboBox<String> cmbAnioEstad;  // año para Mes y Año
    private JLabel            lblTotalI, lblTotalS, lblNetos;
    private GraficoLineas     grafico;
    private JLabel            lblResumenTitulo;
    private JLabel            lblResI, lblResS, lblResPDI, lblResPDS, lblResDif;

    public ReportesPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_PAGE);
        setBorder(new EmptyBorder(16, 20, 16, 20));
        buildUI();
    }

    private void buildUI() {
        // Solo pestaña de Reportes — Estadísticas movidas a panel de Auditoría
        JPanel tabReportes = buildTabReportes();
        tabReportes.setBorder(new EmptyBorder(14, 0, 0, 0));
        add(tabReportes, BorderLayout.CENTER);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PESTAÑA 1 — REPORTES POR GUARDA
    // ═══════════════════════════════════════════════════════════════════════════

    private JPanel buildTabReportes() {
        JPanel tab = new JPanel(new BorderLayout(0, 14));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(14, 0, 0, 0));

        // Fila superior: filtros en línea horizontal
        tab.add(buildFiltrosHorizontal(), BorderLayout.NORTH);

        // Fila media: 4 tarjetas de estadísticas
        tab.add(buildStatCards(), BorderLayout.CENTER);

        // Tabla de reportes
        tab.add(buildListaReportes(), BorderLayout.SOUTH);
        return tab;
    }

    // ── Filtros horizontales ──────────────────────────────────────────────────
    private JPanel buildFiltrosHorizontal() {
        JPanel p = card();
        p.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));
        p.setBorder(new EmptyBorder(8, 14, 8, 14));

        // Tipo de búsqueda
        p.add(label("Buscar por:"));
        cmbTipoBusqueda = new JComboBox<>(new String[]{"Todos", "Día", "Mes", "Año"});
        styleCombo(cmbTipoBusqueda, 110);
        p.add(cmbTipoBusqueda);

        // ── Picker Día ────────────────────────────────────────────────────────
        spDia = buildDateSpinner(LocalDate.now());
        spDia.setPreferredSize(new Dimension(140, 32));
        spDia.setVisible(false);
        p.add(spDia);

        // ── Combo Mes ─────────────────────────────────────────────────────────
        String[] meses = {"Enero","Febrero","Marzo","Abril","Mayo","Junio",
                          "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};
        cmbMes = new JComboBox<>(meses);
        cmbMes.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        styleCombo(cmbMes, 120);
        cmbMes.setVisible(false);
        p.add(cmbMes);

        // ── Combo Año (compartido entre Mes y Año) ────────────────────────────
        int anioActual = LocalDate.now().getYear();
        String[] anios = new String[5];
        for (int i = 0; i < 5; i++) anios[i] = String.valueOf(anioActual - i);
        cmbAnio = new JComboBox<>(anios);
        styleCombo(cmbAnio, 90);
        cmbAnio.setVisible(false);
        p.add(cmbAnio);

        // Listener: mostrar/ocultar controles según tipo
        cmbTipoBusqueda.addActionListener(e -> actualizarControlesFiltro(p));

        // Botones
        JButton btnBuscar  = makeBtn("🔍  Buscar",  GREEN,                    Color.WHITE, 110, 32);
        JButton btnLimpiar = makeBtn("↺  Todos",    new Color(0x6C757D), Color.WHITE, 100, 32);
        btnBuscar.addActionListener(e -> buscarReportes());
        btnLimpiar.addActionListener(e -> {
            cmbTipoBusqueda.setSelectedIndex(0);
            cargarTodosReportes();
        });
        p.add(btnBuscar);
        p.add(btnLimpiar);
        return p;
    }

    /** Muestra/oculta los controles de fecha según el tipo de búsqueda. */
    private void actualizarControlesFiltro(JPanel p) {
        String tipo = (String) cmbTipoBusqueda.getSelectedItem();
        spDia.setVisible("Día".equals(tipo));
        cmbMes.setVisible("Mes".equals(tipo));
        cmbAnio.setVisible("Mes".equals(tipo) || "Año".equals(tipo));
        p.revalidate(); p.repaint();
    }

    // ── Tarjetas de estadísticas ──────────────────────────────────────────────
    private JPanel buildStatCards() {
        JPanel row = new JPanel(new GridLayout(1, 4, 12, 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(0, 90));

        lblStatTotal      = statCard(row, "📋", "Total reportes",       new Color(0x1565C0));
        lblStatHoy        = statCard(row, "📅", "Reportes hoy",          new Color(0x6A1B9A));
        lblStatGuardas    = statCard(row, "👮", "Guardas activos",       new Color(0x2E7D32));
        lblStatAprendices = statCard(row, "🎓", "Aprendices reportados", new Color(0xE65100));
        return row;
    }

    private JLabel statCard(JPanel row, String emoji, String titulo, Color color) {
        JPanel card = card();
        card.setLayout(new BorderLayout(10, 0));
        card.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel ico = new JLabel(emoji);
        ico.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));

        JPanel txt = new JPanel();
        txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
        txt.setOpaque(false);

        JLabel val = new JLabel("—");
        val.setFont(new Font("Segoe UI", Font.BOLD, 26));
        val.setForeground(color);

        JLabel lbl = new JLabel(titulo);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(TEXT_GRAY);

        txt.add(val); txt.add(lbl);
        card.add(ico, BorderLayout.WEST);
        card.add(txt, BorderLayout.CENTER);
        row.add(card);
        return val;
    }

    // ── Tabla de reportes ─────────────────────────────────────────────────────
    private JPanel buildListaReportes() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);

        // Encabezado
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titulo = new JLabel("Listado de reportes");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titulo.setForeground(TEXT_DARK);

        lblConteo = new JLabel("");
        lblConteo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblConteo.setForeground(TEXT_GRAY);

        JPanel hRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        hRight.setOpaque(false);
        hRight.add(makeBtn("⬇ Exportar", GREEN, Color.WHITE, 110, 30));
        header.add(titulo,  BorderLayout.WEST);
        header.add(hRight,  BorderLayout.EAST);

        // Columnas: Fecha y hora | Guarda | ID Guarda | Aprendiz | ID Aprendiz | Tipo reporte | Descripción | Acciones
        String[] cols = {"Fecha y hora", "Guarda", "Identificación\nguarda",
                         "Aprendiz", "Identificación\naprendiz",
                         "Tipo de reporte", "Descripción", "Acciones"};
        reporteModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 7; }
            @Override public Class<?> getColumnClass(int c) { return Object.class; }
        };

        reporteTable = new JTable(reporteModel);
        reporteTable.getTableHeader().setBackground(new Color(0xF8F9FA));
        reporteTable.getTableHeader().setForeground(TEXT_DARK);
        reporteTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        reporteTable.getTableHeader().setPreferredSize(new Dimension(0, 36));
        reporteTable.getTableHeader().setReorderingAllowed(false);
        reporteTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        reporteTable.setRowHeight(48);
        reporteTable.setBackground(CARD_BG);
        reporteTable.setGridColor(new Color(0xF0F0F0));
        reporteTable.setShowVerticalLines(false);
        reporteTable.setSelectionBackground(new Color(0xE8F0FB));
        reporteTable.setFocusable(false);

        // Anchos de columnas
        int[] w = {130, 160, 110, 160, 110, 130, 200, 70};
        for (int i = 0; i < w.length; i++) reporteTable.getColumnModel().getColumn(i).setPreferredWidth(w[i]);

        // Renderer y editor de la columna Acciones (ojo)
        reporteTable.getColumnModel().getColumn(7).setCellRenderer(new OjoRenderer());
        reporteTable.getColumnModel().getColumn(7).setCellEditor(new OjoEditor(this));

        // Renderer de tipo de reporte (badge de color)
        reporteTable.getColumnModel().getColumn(5).setCellRenderer(new TipoReporteRenderer());

        JScrollPane scroll = new JScrollPane(reporteTable);
        scroll.setBorder(new LineBorder(BORDER_C, 1, true));
        scroll.getViewport().setBackground(CARD_BG);
        scroll.setPreferredSize(new Dimension(0, 320));

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(lblConteo, BorderLayout.WEST);

        p.add(header, BorderLayout.NORTH);
        p.add(scroll,  BorderLayout.CENTER);
        p.add(footer,  BorderLayout.SOUTH);
        return p;
    }

    // ── Renderer tipo de reporte (badge coloreado) ────────────────────────────
    private static class TipoReporteRenderer extends DefaultTableCellRenderer {
        private static final Color[][] BADGES = {
            {new Color(0xFFF3CD), new Color(0x856404)}, // Ingreso tarde
            {new Color(0xFFE0E0), new Color(0xC62828)}, // Salida no autorizada
            {new Color(0xE0F7FA), new Color(0x006064)}, // Uso no autorizado
            {new Color(0xF3E5F5), new Color(0x6A1B9A)}, // Falta de respeto
            {new Color(0xE8F5E9), new Color(0x1B5E20)}, // Uniforme incompleto
            {new Color(0xFCE4EC), new Color(0x880E4F)}, // Comportamiento
        };
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int r, int c) {
            String txt = v != null ? v.toString() : "";
            JLabel lbl = new JLabel(txt, SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lbl.setOpaque(true);
            lbl.setBorder(new EmptyBorder(3, 8, 3, 8));
            int idx = Math.abs(txt.hashCode() % BADGES.length);
            lbl.setBackground(BADGES[idx][0]);
            lbl.setForeground(BADGES[idx][1]);
            return lbl;
        }
    }

    // ── Renderer / Editor del ojo ──────────────────────────────────────────────
    private static class OjoRenderer implements TableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int r, int c) {
            JLabel lbl = new JLabel("\uD83D\uDC41", SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            lbl.setForeground(new Color(0x1565C0));
            lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lbl.setOpaque(true);
            lbl.setBackground(sel ? new Color(0xE8F0FB) : CARD_BG);
            return lbl;
        }
    }

    private class OjoEditor extends DefaultCellEditor {
        private FilaReporte filaActual;
        private final JLabel lbl;

        OjoEditor(ReportesPanel p) {
            super(new JCheckBox());
            lbl = new JLabel("\uD83D\uDC41", SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            lbl.setForeground(new Color(0x1565C0));
            lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lbl.setOpaque(true);
            lbl.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    fireEditingStopped();
                    if (filaActual != null) mostrarDetalleReporte(filaActual);
                }
            });
        }

        @Override public Component getTableCellEditorComponent(JTable t, Object v,
                boolean sel, int r, int c) {
            filaActual = (r >= 0 && r < listaReportes.size()) ? listaReportes.get(r) : null;
            lbl.setBackground(new Color(0xE8F0FB));
            return lbl;
        }
        @Override public Object getCellEditorValue() { return filaActual; }
    }

    // ── Diálogo de detalle del reporte ────────────────────────────────────────
    private void mostrarDetalleReporte(FilaReporte f) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Detalle del Reporte #" + f.idReporte, true);
        dlg.setLayout(new BorderLayout());
        dlg.setSize(820, 620);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.getContentPane().setBackground(new Color(0xF8F9FA));

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(new Color(0xF8F9FA));
        root.setBorder(new EmptyBorder(20, 24, 16, 24));

        // ── Título ────────────────────────────────────────────────────────────
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel titleLbl = new JLabel("⚠  Detalle del Reporte #" + f.idReporte);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLbl.setForeground(new Color(0x1A3A5C));
        titleRow.add(titleLbl, BorderLayout.WEST);
        root.add(titleRow);
        root.add(Box.createVerticalStrut(10));

        // Separador
        JSeparator sep0 = new JSeparator();
        sep0.setForeground(new Color(0xDDE1E7));
        sep0.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        root.add(sep0);
        root.add(Box.createVerticalStrut(10));

        // ── Fecha y hora ──────────────────────────────────────────────────────
        JPanel fechaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fechaRow.setOpaque(false);
        String fechaStr = f.fechaReporte != null ? f.fechaReporte.format(FMT_TS) : "—";
        JLabel fechaIco = new JLabel("📅");
        fechaIco.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        JLabel fechaLbl = new JLabel("Fecha y hora del reporte:");
        fechaLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        fechaLbl.setForeground(TEXT_DARK);
        JLabel fechaVal = new JLabel(fechaStr);
        fechaVal.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        fechaVal.setForeground(new Color(0x1565C0));
        fechaRow.add(fechaIco); fechaRow.add(fechaLbl); fechaRow.add(fechaVal);
        root.add(fechaRow);
        root.add(Box.createVerticalStrut(12));

        // ── Tarjetas Guardia + Aprendiz ───────────────────────────────────────
        JPanel cards = new JPanel(new GridLayout(1, 2, 14, 0));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        // Tarjeta Guardia
        JPanel cardG = detalleCard();
        cardG.setLayout(new BorderLayout(12, 0));
        cardG.setBorder(new EmptyBorder(14, 14, 14, 14));
        JLabel tG = new JLabel("🛡  Personal de Seguridad (Guardia)");
        tG.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tG.setForeground(new Color(0x1A3A5C));
        tG.setBorder(new EmptyBorder(0,0,10,0));

        // Avatar placeholder guardia
        JPanel avatarG = buildAvatar(new Color(0x3F51B5));
        JPanel infoG = new JPanel();
        infoG.setLayout(new BoxLayout(infoG, BoxLayout.Y_AXIS));
        infoG.setOpaque(false);
        addInfoFila(infoG, "Identificación:", f.tipDocGuarda + " " + f.numDocGuarda);
        addInfoFila(infoG, "Nombre:",         f.nombreGuarda);
        addInfoFila(infoG, "Correo:",         f.nombreGuarda.toLowerCase().replace(" ",".")+  "@sena.edu.co");
        addInfoFila(infoG, "Cargo:",          "Personal de Seguridad");

        JPanel bodyG = new JPanel(new BorderLayout(12, 0));
        bodyG.setOpaque(false);
        bodyG.add(avatarG, BorderLayout.WEST);
        bodyG.add(infoG,   BorderLayout.CENTER);
        cardG.add(tG,    BorderLayout.NORTH);
        cardG.add(bodyG, BorderLayout.CENTER);

        // Tarjeta Aprendiz
        JPanel cardA = detalleCard();
        cardA.setLayout(new BorderLayout(12, 0));
        cardA.setBorder(new EmptyBorder(14, 14, 14, 14));
        JLabel tA = new JLabel("🎓  Aprendiz");
        tA.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tA.setForeground(new Color(0x2E7D32));
        tA.setBorder(new EmptyBorder(0,0,10,0));

        JPanel avatarA = buildAvatar(new Color(0x388E3C));
        JPanel infoA = new JPanel();
        infoA.setLayout(new BoxLayout(infoA, BoxLayout.Y_AXIS));
        infoA.setOpaque(false);
        addInfoFila(infoA, "Identificación:", f.tipDocAprendiz + " " + f.numDocAprendiz);
        addInfoFila(infoA, "Nombre:",         f.nombreAprendiz);
        addInfoFila(infoA, "Correo:",         f.nombreAprendiz.toLowerCase().replace(" ",".")+ "@soy.sena.edu.co");
        // Badge estado
        JPanel estadoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        estadoRow.setOpaque(false);
        JLabel estadoK = new JLabel("Estado: ");
        estadoK.setFont(new Font("Segoe UI", Font.BOLD, 11));
        estadoK.setForeground(TEXT_GRAY);
        JLabel estadoV = new JLabel("  Activo  ");
        estadoV.setFont(new Font("Segoe UI", Font.BOLD, 11));
        estadoV.setForeground(new Color(0x1B5E20));
        estadoV.setOpaque(true);
        estadoV.setBackground(new Color(0xE8F5E9));
        estadoV.setBorder(new LineBorder(new Color(0x2E7D32), 1, true));
        estadoRow.add(estadoK); estadoRow.add(estadoV);
        infoA.add(estadoRow);

        JPanel bodyA = new JPanel(new BorderLayout(12, 0));
        bodyA.setOpaque(false);
        bodyA.add(avatarA, BorderLayout.WEST);
        bodyA.add(infoA,   BorderLayout.CENTER);
        cardA.add(tA,    BorderLayout.NORTH);
        cardA.add(bodyA, BorderLayout.CENTER);

        cards.add(cardG); cards.add(cardA);
        root.add(cards);
        root.add(Box.createVerticalStrut(12));

        // ── Información del Reporte ───────────────────────────────────────────
        JPanel infoRep = detalleCard();
        infoRep.setLayout(new BoxLayout(infoRep, BoxLayout.Y_AXIS));
        infoRep.setBorder(new EmptyBorder(14, 16, 14, 16));
        JLabel tRep = new JLabel("📋  Información del Reporte");
        tRep.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tRep.setForeground(TEXT_DARK);
        infoRep.add(tRep);
        infoRep.add(Box.createVerticalStrut(10));

        // Tipo con badge
        JPanel tipoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        tipoRow.setOpaque(false);
        JLabel tipoK = new JLabel("🏷  Tipo de reporte:");
        tipoK.setFont(new Font("Segoe UI", Font.BOLD, 11)); tipoK.setForeground(TEXT_GRAY);
        JLabel tipoV = new JLabel("  " + f.motivo + "  ");
        tipoV.setFont(new Font("Segoe UI", Font.BOLD, 11));
        tipoV.setForeground(new Color(0x856404)); tipoV.setOpaque(true);
        tipoV.setBackground(new Color(0xFFF3CD));
        tipoV.setBorder(new LineBorder(new Color(0xFFC107), 1, true));
        tipoRow.add(tipoK); tipoRow.add(tipoV);
        infoRep.add(tipoRow);

        // Descripción
        addInfoFilaH(infoRep, "💬  Descripción:",        f.descripcion.isEmpty() ? "—" : f.descripcion);
        infoRep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        root.add(infoRep);
        root.add(Box.createVerticalStrut(12));

        // ── Información adicional ─────────────────────────────────────────────
        JPanel infoAd = detalleCard();
        infoAd.setLayout(new GridLayout(2, 3, 14, 8));
        infoAd.setBorder(new EmptyBorder(14,16,14,16));

        addMiniCard(infoAd, "#",  "Número de reporte:", "#" + f.idReporte,          new Color(0x3F51B5));
        addMiniCard(infoAd, "👤", "Reportado por:",    f.nombreGuarda + "\n(" + f.tipDocGuarda + " " + f.numDocGuarda + ")", new Color(0x9C27B0));
        addMiniCard(infoAd, "💻", "Medio de reporte:", "SAIA - Módulo de Seguridad", new Color(0x2196F3));
        addMiniCard(infoAd, "📋", "Estado del reporte:", "Abierto",                 new Color(0x4CAF50));
        addMiniCard(infoAd, "📅", "Fecha de creación:", fechaStr,                   new Color(0x009688));
        addMiniCard(infoAd, "🕐", "Última actualización:", fechaStr,                new Color(0xFF9800));
        infoAd.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        root.add(infoAd);

        // ── Botón cerrar ──────────────────────────────────────────────────────
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnRow.setBackground(new Color(0xF8F9FA));
        JButton btnCerrar = makeBtn("  Cerrar  ", NAVY, Color.WHITE, 120, 36);
        btnCerrar.addActionListener(e -> dlg.dispose());
        btnRow.add(btnCerrar);

        JScrollPane scroll = new JScrollPane(root);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(0xF8F9FA));
        dlg.add(scroll,  BorderLayout.CENTER);
        dlg.add(btnRow,  BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    /** Crea una tarjeta blanca con borde sutil. */
    private JPanel detalleCard() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(new LineBorder(BORDER_C, 1, true));
        return p;
    }

    /** Avatar placeholder circular con inicial. */
    private JPanel buildAvatar(Color color) {
        JPanel av = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
                g2.setColor(color);
                g2.drawString("\uD83D\uDC64", 20, 44);
                g2.dispose();
            }
        };
        av.setOpaque(false);
        av.setPreferredSize(new Dimension(70, 70));
        return av;
    }

    /** Fila de información label: valor en BoxLayout vertical. */
    private void addInfoFila(JPanel p, String key, String val) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        row.setOpaque(false);
        JLabel k = new JLabel(key); k.setFont(new Font("Segoe UI", Font.BOLD, 11)); k.setForeground(TEXT_GRAY);
        JLabel v = new JLabel(val); v.setFont(new Font("Segoe UI", Font.PLAIN, 11)); v.setForeground(TEXT_DARK);
        row.add(k); row.add(v); p.add(row);
    }

    /** Fila horizontal label: valor para sección de reporte. */
    private void addInfoFilaH(JPanel p, String key, String val) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row.setOpaque(false);
        JLabel k = new JLabel(key); k.setFont(new Font("Segoe UI", Font.BOLD, 11)); k.setForeground(TEXT_GRAY);
        JLabel v = new JLabel(val); v.setFont(new Font("Segoe UI", Font.PLAIN, 11)); v.setForeground(TEXT_DARK);
        row.add(k); row.add(v); p.add(row);
    }

    /** Mini tarjeta para información adicional. */
    private void addMiniCard(JPanel grid, String ico, String titulo, String valor, Color color) {
        JPanel c = new JPanel();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 12));
        c.setBorder(new EmptyBorder(8, 10, 8, 10));
        JLabel t = new JLabel(ico + " " + titulo);
        t.setFont(new Font("Segoe UI", Font.BOLD, 10));
        t.setForeground(TEXT_GRAY);
        JLabel v = new JLabel("<html>" + valor.replace("\n","<br>") + "</html>");
        v.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        v.setForeground(TEXT_DARK);
        c.add(t); c.add(Box.createVerticalStrut(4)); c.add(v);
        grid.add(c);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PESTAÑA 2 — ESTADÍSTICAS
    // ═══════════════════════════════════════════════════════════════════════════

    private JPanel buildTabEstadisticas() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(14, 0, 0, 0));

        // ── Filtros en línea ──────────────────────────────────────────────────
        JPanel filtros = card();
        filtros.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));
        filtros.setBorder(new EmptyBorder(8, 14, 8, 14));

        filtros.add(label("Ver por:"));
        cmbTipoEstad = new JComboBox<>(new String[]{"Día", "Mes", "Año"});
        styleCombo(cmbTipoEstad, 90);
        filtros.add(cmbTipoEstad);

        // Date picker para Día
        spDiaEstad = buildDateSpinner(LocalDate.now());
        spDiaEstad.setPreferredSize(new Dimension(140, 32));
        filtros.add(spDiaEstad);

        // Combo Mes
        String[] meses = {"Enero","Febrero","Marzo","Abril","Mayo","Junio",
                          "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};
        cmbMesEstad = new JComboBox<>(meses);
        cmbMesEstad.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        styleCombo(cmbMesEstad, 120);
        cmbMesEstad.setVisible(false);
        filtros.add(cmbMesEstad);

        // Combo Año
        int anioAct = LocalDate.now().getYear();
        String[] anios = new String[6];
        for (int i = 0; i < 6; i++) anios[i] = String.valueOf(anioAct - i);
        cmbAnioEstad = new JComboBox<>(anios);
        styleCombo(cmbAnioEstad, 90);
        cmbAnioEstad.setVisible(false);
        filtros.add(cmbAnioEstad);

        // Listener tipo
        cmbTipoEstad.addActionListener(e -> actualizarControlesEstad(filtros));

        // Botones
        JButton btnCargar = makeBtn("📊 Cargar",    NAVY,                Color.WHITE, 105, 32);
        JButton btnTodos  = makeBtn("🔄 Mostrar todos", new Color(0x6C757D), Color.WHITE, 135, 32);
        btnCargar.addActionListener(e -> cargarEstadisticas());
        btnTodos.addActionListener(e -> cargarEstadisticasTodas());
        filtros.add(btnCargar);
        filtros.add(btnTodos);
        tab.add(filtros, BorderLayout.NORTH);

        // ── Cuerpo: Totales + Gráfico + Resumen ───────────────────────────────
        JPanel body = new JPanel(new BorderLayout(14, 0));
        body.setOpaque(false);

        // Izquierda: totales
        JPanel left = card();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(new EmptyBorder(16,16,16,16));
        left.setPreferredSize(new Dimension(185, 0));
        addSection(left, "Totales");
        left.add(Box.createVerticalStrut(12));
        lblTotalI = stat(left, "Total Ingresos", new Color(0x1565C0));
        left.add(Box.createVerticalStrut(8));
        lblTotalS = stat(left, "Total Salidas",  new Color(0x2E7D32));
        left.add(Box.createVerticalStrut(8));
        lblNetos  = stat(left, "Netos",          new Color(0xE65100));
        left.add(Box.createVerticalGlue());

        // Centro: gráfico
        grafico = new GraficoLineas();
        JPanel graficoCard = card();
        graficoCard.setLayout(new BorderLayout());
        graficoCard.setBorder(new EmptyBorder(14, 14, 14, 14));
        JLabel tGraf = new JLabel("Ingresos y Salidas por Período");
        tGraf.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tGraf.setForeground(TEXT_DARK);
        tGraf.setBorder(new EmptyBorder(0,0,8,0));
        graficoCard.add(tGraf,  BorderLayout.NORTH);
        graficoCard.add(grafico, BorderLayout.CENTER);

        // Derecha: resumen
        JPanel right = card();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(new EmptyBorder(16,16,16,16));
        right.setPreferredSize(new Dimension(225, 0));
        JLabel rTit = new JLabel("Resumen por período");
        rTit.setFont(new Font("Segoe UI", Font.BOLD, 13));
        rTit.setForeground(TEXT_DARK);
        right.add(rTit);
        right.add(Box.createVerticalStrut(4));
        lblResumenTitulo = new JLabel("—");
        lblResumenTitulo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblResumenTitulo.setForeground(TEXT_GRAY);
        right.add(lblResumenTitulo);
        right.add(Box.createVerticalStrut(14));
        lblResI   = resumenFila(right, "📥 Total Ingresos",      new Color(0x1565C0));
        lblResS   = resumenFila(right, "📤 Total Salidas",        new Color(0x2E7D32));
        lblResPDI = resumenFila(right, "📅 Prom. Diario Ingr.",  new Color(0x0D47A1));
        lblResPDS = resumenFila(right, "📅 Prom. Diario Sal.",   new Color(0x1B5E20));
        lblResDif = resumenFila(right, "↗ Diferencia Neta",     new Color(0xE65100));
        right.add(Box.createVerticalGlue());

        body.add(left,       BorderLayout.WEST);
        body.add(graficoCard, BorderLayout.CENTER);
        body.add(right,      BorderLayout.EAST);
        tab.add(body, BorderLayout.CENTER);
        return tab;
    }

    /** Muestra/oculta controles de estadísticas según tipo seleccionado. */
    private void actualizarControlesEstad(JPanel p) {
        String tipo = (String) cmbTipoEstad.getSelectedItem();
        spDiaEstad.setVisible("Día".equals(tipo));
        cmbMesEstad.setVisible("Mes".equals(tipo));
        cmbAnioEstad.setVisible("Mes".equals(tipo) || "Año".equals(tipo));
        p.revalidate(); p.repaint();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LÓGICA DE CARGA
    // ═══════════════════════════════════════════════════════════════════════════

    /** Carga inicial: todos los reportes del más reciente al más antiguo. */
    private void cargarTodosReportes() {
        // Rango amplio para traer todo (último año completo)
        LocalDate desde = LocalDate.now().minusYears(5);
        LocalDate hasta = LocalDate.now();
        ejecutarCarga(desde, hasta, "");
    }

    @Override public void addNotify() {
        super.addNotify();
        // Carga inicial de reportes
        SwingUtilities.invokeLater(this::cargarTodosReportes);
        // Estadísticas del día actual por defecto
        SwingUtilities.invokeLater(() ->
            ejecutarEstadisticas(LocalDate.now(), LocalDate.now(), "Diaria",
                    LocalDate.now().format(FMT_DATE)));
    }

    private void buscarReportes() {
        String tipo = (String) cmbTipoBusqueda.getSelectedItem();
        LocalDate desde, hasta;
        String mensajeVacio;

        switch (tipo) {
            case "Día" -> {
                LocalDate dia = getSpinnerDate(spDia);
                desde = dia; hasta = dia;
                mensajeVacio = "No existen reportes el " + dia.format(FMT_DATE);
            }
            case "Mes" -> {
                int mes  = cmbMes.getSelectedIndex() + 1;
                int anio = Integer.parseInt((String) cmbAnio.getSelectedItem());
                desde = LocalDate.of(anio, mes, 1);
                hasta = desde.withDayOfMonth(desde.lengthOfMonth());
                mensajeVacio = "No existen reportes en " + cmbMes.getSelectedItem() + " " + anio;
            }
            case "Año" -> {
                int anio = Integer.parseInt((String) cmbAnio.getSelectedItem());
                desde = LocalDate.of(anio, 1, 1);
                hasta = LocalDate.of(anio, 12, 31);
                mensajeVacio = "No existen reportes en el año " + anio;
            }
            default -> { cargarTodosReportes(); return; }
        }
        ejecutarCarga(desde, hasta, mensajeVacio);
    }

    private void ejecutarCarga(LocalDate desde, LocalDate hasta, String msgVacio) {
        lblConteo.setText("Cargando...");
        reporteModel.setRowCount(0);
        final LocalDate d = desde, h = hasta;

        SwingWorker<Object[], Void> w = new SwingWorker<>() {
            @Override protected Object[] doInBackground() {
                List<FilaReporte> lista = dao.findReportes(d, h, -1, -1);
                int total      = dao.countTotalReportes(d, h);
                int hoy2       = dao.countReportesHoy();
                int guardas    = dao.countGuardasActivos();
                int aprendices = dao.countAprendicesReportados(d, h);
                return new Object[]{lista, total, hoy2, guardas, aprendices};
            }
            @Override protected void done() {
                try {
                    Object[] r = get();
                    @SuppressWarnings("unchecked")
                    List<FilaReporte> lista = (List<FilaReporte>) r[0];
                    listaReportes = lista;

                    // Tarjetas
                    lblStatTotal.setText(String.valueOf(r[1]));
                    lblStatHoy.setText(String.valueOf(r[2]));
                    lblStatGuardas.setText(String.valueOf(r[3]));
                    lblStatAprendices.setText(String.valueOf(r[4]));

                    // Tabla
                    reporteModel.setRowCount(0);
                    if (lista.isEmpty()) {
                        String msg = msgVacio.isEmpty()
                                ? "No hay reportes registrados."
                                : msgVacio;
                        reporteModel.addRow(new Object[]{msg,"","","","","","",null});
                        lblConteo.setText(msg);
                    } else {
                        for (FilaReporte f : lista) {
                            String fecha = f.fechaReporte != null ? f.fechaReporte.format(FMT_TS) : "";
                            String docG  = f.tipDocGuarda   + " " + f.numDocGuarda;
                            String docA  = f.tipDocAprendiz + " " + f.numDocAprendiz;
                            reporteModel.addRow(new Object[]{
                                fecha, f.nombreGuarda, docG,
                                f.nombreAprendiz, docA,
                                f.motivo, f.descripcion, f
                            });
                        }
                        lblConteo.setText("Mostrando 1 a " + lista.size() + " de " + lista.size() + " reportes");
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    lblConteo.setText("Error al cargar");
                    Thread.currentThread().interrupt();
                }
            }
        };
        w.execute();
    }

    private void cargarEstadisticas() {
        String tipo = (String) cmbTipoEstad.getSelectedItem();
        LocalDate desde, hasta;
        String label;

        switch (tipo) {
            case "Día" -> {
                LocalDate dia = getSpinnerDate(spDiaEstad);
                desde = dia; hasta = dia;
                label = dia.format(FMT_DATE);
            }
            case "Mes" -> {
                int mes  = cmbMesEstad.getSelectedIndex() + 1;
                int anio = Integer.parseInt((String) cmbAnioEstad.getSelectedItem());
                desde = LocalDate.of(anio, mes, 1);
                hasta = desde.withDayOfMonth(desde.lengthOfMonth());
                label = cmbMesEstad.getSelectedItem() + " " + anio;
            }
            case "Año" -> {
                int anio = Integer.parseInt((String) cmbAnioEstad.getSelectedItem());
                desde = LocalDate.of(anio, 1, 1);
                hasta = LocalDate.of(anio, 12, 31);
                label = String.valueOf(anio);
            }
            default -> { desde = LocalDate.now().minusYears(5); hasta = LocalDate.now(); label = "Todo el período"; }
        }
        ejecutarEstadisticas(desde, hasta, tipo, label);
    }

    private void cargarEstadisticasTodas() {
        // Últimos 5 años agrupado por mes
        LocalDate desde = LocalDate.now().minusYears(5);
        LocalDate hasta = LocalDate.now();
        ejecutarEstadisticas(desde, hasta, "Mensual", "Todo el período");
    }

    private void ejecutarEstadisticas(LocalDate desde, LocalDate hasta, String tipo, String labelPeriodo) {
        // Mapear tipo de vista al método de agrupación
        String tipoAgrup = switch (tipo) {
            case "Mes", "Mensual" -> "Mensual";
            case "Año", "Anual"   -> "Anual";
            default               -> "Diaria";
        };
        final String labelFinal = labelPeriodo;
        final String agrup      = tipoAgrup;
        final LocalDate d = desde, h = hasta;

        SwingWorker<Object[], Void> w = new SwingWorker<>() {
            @Override protected Object[] doInBackground() {
                return new Object[]{
                    dao.getEstadisticasPorPeriodo(d, h, agrup),
                    dao.getResumen(d, h)
                };
            }
            @Override protected void done() {
                try {
                    Object[] r = get();
                    @SuppressWarnings("unchecked")
                    List<EstadisticaPunto> pts = (List<EstadisticaPunto>) r[0];
                    ResumenPeriodo res = (ResumenPeriodo) r[1];
                    int netos = res.totalIngresos - res.totalSalidas;

                    // Panel izquierdo
                    lblTotalI.setText(String.valueOf(res.totalIngresos));
                    lblTotalS.setText(String.valueOf(res.totalSalidas));
                    lblNetos.setText((netos >= 0 ? "+" : "") + netos);

                    // Panel derecho
                    lblResumenTitulo.setText(labelFinal);
                    lblResI.setText(String.valueOf(res.totalIngresos));
                    lblResS.setText(String.valueOf(res.totalSalidas));
                    lblResPDI.setText(String.valueOf(res.promedioDiarioIngresos));
                    lblResPDS.setText(String.valueOf(res.promedioDiarioSalidas));
                    lblResDif.setText((netos >= 0 ? "+" : "") + netos);

                    // Gráfico
                    grafico.setData(pts, agrup);
                } catch (InterruptedException | ExecutionException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        w.execute();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS UI
    // ═══════════════════════════════════════════════════════════════════════════

    private JPanel card() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0,0,0,12));
                g2.fill(new RoundRectangle2D.Float(2,3,getWidth()-3,getHeight()-3,10,10));
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth()-2,getHeight()-2,10,10));
                g2.setColor(BORDER_C); g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f,0.5f,getWidth()-3,getHeight()-3,10,10));
                g2.dispose(); super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
    }

    private JLabel label(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(TEXT_DARK);
        return l;
    }

    private void addSection(JPanel p, String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(TEXT_DARK);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l); p.add(Box.createVerticalStrut(4));
    }

    private void styleCombo(JComboBox<String> c, int w) {
        c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        c.setBackground(Color.WHITE);
        c.setPreferredSize(new Dimension(w, 32));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private JSpinner buildDateSpinner(LocalDate initial) {
        SpinnerDateModel m = new SpinnerDateModel();
        m.setValue(toDate(initial));
        JSpinner s = new JSpinner(m);
        s.setEditor(new JSpinner.DateEditor(s, "dd/MM/yyyy"));
        s.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        return s;
    }

    private java.util.Date toDate(LocalDate d) {
        return java.util.Date.from(d.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
    }

    private LocalDate getSpinnerDate(JSpinner s) {
        java.util.Date d = (java.util.Date) s.getValue();
        return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private JLabel stat(JPanel p, String lbl, Color color) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel k = new JLabel(lbl); k.setFont(new Font("Segoe UI", Font.PLAIN, 12)); k.setForeground(TEXT_DARK);
        JLabel v = new JLabel("—"); v.setFont(new Font("Segoe UI", Font.BOLD, 18)); v.setForeground(color);
        v.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(k, BorderLayout.WEST); row.add(v, BorderLayout.EAST); p.add(row);
        return v;
    }

    private JLabel resumenFila(JPanel p, String lbl, Color color) {
        JPanel row = new JPanel(new BorderLayout()); row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row.setBorder(new EmptyBorder(3,0,3,0));
        JLabel k = new JLabel(lbl); k.setFont(new Font("Segoe UI", Font.PLAIN, 11)); k.setForeground(TEXT_GRAY);
        JLabel v = new JLabel("—"); v.setFont(new Font("Segoe UI", Font.BOLD, 14)); v.setForeground(color);
        v.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(k, BorderLayout.WEST); row.add(v, BorderLayout.EAST);
        p.add(row); p.add(Box.createVerticalStrut(2));
        return v;
    }

    private static JButton makeBtn(String text, Color bg, Color fg, int w, int h) {
        JButton btn = new JButton(text) {
            boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                @Override public void mouseExited (MouseEvent e) { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? bg.darker() : bg);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,8,8));
                g2.dispose(); super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12)); btn.setForeground(fg);
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(w, h)); btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        return btn;
    }
}
