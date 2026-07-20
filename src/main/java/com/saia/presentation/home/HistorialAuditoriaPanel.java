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

import javax.swing.BorderFactory;
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
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import com.saia.business.AuditoriaService;
import com.saia.business.AuditoriaService.PaginaAuditoria;
import com.saia.model.RegistroAuditoria;
import com.saia.model.RegistroAuditoria.Accion;
import com.saia.presentation.UITheme;

/**
 * Panel "Historial de Auditoría".
 * Diseño: Encabezado → Filtros → Tabla paginada con badges de acción → Paginación.
 */
public class HistorialAuditoriaPanel extends JPanel {

    // ── Paleta — desde UITheme (identidad SENA) ───────────────────────────────
    private static final Color BG     = UITheme.BG_SECONDARY;
    private static final Color CARD   = UITheme.BG_WHITE;
    private static final Color BORDER = UITheme.BORDER;
    private static final Color TXT_D  = UITheme.TEXT_PRIMARY;
    private static final Color TXT_G  = UITheme.TEXT_SECONDARY;
    private static final Color NAVY   = UITheme.PRIMARY;
    private static final Color BLUE   = UITheme.PRIMARY;
    private static final Color GREEN  = UITheme.SECONDARY;

    private static final int PAGE_SIZE = AuditoriaService.PAGE_SIZE;

    private static final String[] COLS = {
        "Fecha", "Hora", "Usuario", "Acción", "Entidad", "Descripción", "Módulo", "Detalle"
    };

    private static final DateTimeFormatter FMT_FULL =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // ── Servicio ──────────────────────────────────────────────────────────────
    private final AuditoriaService service = new AuditoriaService();

    // ── Estado ────────────────────────────────────────────────────────────────
    private int paginaActual = 0;
    private int totalRegistros = 0;
    private int totalPaginas = 1;
    private final List<RegistroAuditoria> paginaData = new ArrayList<>();

    // ── Filtros ───────────────────────────────────────────────────────────────
    private JSpinner  spDesde, spHasta;
    private JComboBox<String> cmbAccion, cmbEntidad;
    private JTextField txtBuscar;

    // ── Tabla ─────────────────────────────────────────────────────────────────
    private DefaultTableModel tableModel;
    private JTable            table;

    // ── Paginación ────────────────────────────────────────────────────────────
    private JLabel  lblConteo;
    private JPanel  paginacionBox;

    // ── Constructor ───────────────────────────────────────────────────────────
    public HistorialAuditoriaPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG);
        setBorder(new EmptyBorder(16, 22, 16, 22));
        buildUI();
    }

    @Override public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(() -> {
            cargarCombosAsync();
            buscar();
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BUILD UI
    // ═════════════════════════════════════════════════════════════════════════
    private void buildUI() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setOpaque(false);

        root.add(buildEncabezado());
        root.add(vgap(12));
        root.add(buildFiltros());
        root.add(vgap(12));
        root.add(buildTablaCard());

        JScrollPane scroll = new JScrollPane(root);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
    }

    // ── Encabezado ────────────────────────────────────────────────────────────
    private JPanel buildEncabezado() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        JLabel titulo = new JLabel("🗂  Historial de Auditoría");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titulo.setForeground(TXT_D);

        JLabel sub = new JLabel(
            "Consulta todas las modificaciones y creaciones realizadas en el sistema.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(TXT_G);

        p.add(titulo, BorderLayout.NORTH);
        p.add(sub,    BorderLayout.CENTER);
        return p;
    }

    // ── Filtros ───────────────────────────────────────────────────────────────
    private JPanel buildFiltros() {
        JPanel card = card();
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        // Fecha desde
        card.add(boldLabel("Desde:"));
        spDesde = dateSpinner(LocalDate.now().withDayOfMonth(1));
        card.add(spDesde);

        // Fecha hasta
        card.add(boldLabel("Hasta:"));
        spHasta = dateSpinner(LocalDate.now());
        card.add(spHasta);

        // Acción
        card.add(boldLabel("Acción:"));
        cmbAccion = combo(new String[]{"Todas"}, 130);
        card.add(cmbAccion);

        // Entidad / Módulo
        card.add(boldLabel("Módulo:"));
        cmbEntidad = combo(new String[]{"Todas"}, 130);
        card.add(cmbEntidad);

        // Búsqueda libre
        txtBuscar = searchField();
        card.add(txtBuscar);

        // Botón limpiar
        JButton btnLimpiar = outlineBtn("⧳  Limpiar filtros", new Color(0x64748B));
        btnLimpiar.addActionListener(e -> limpiarFiltros());
        card.add(btnLimpiar);

        // Botón buscar
        JButton btnBuscar = solidBtn("🔍  Buscar", BLUE);
        btnBuscar.addActionListener(e -> { paginaActual = 0; buscar(); });
        card.add(btnBuscar);

        // Buscar al escribir (con debounce liviano)
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { scheduleSearch(); }
            @Override public void removeUpdate(DocumentEvent e) { scheduleSearch(); }
            @Override public void changedUpdate(DocumentEvent e) {}
        });

        return card;
    }

    // ── Tabla ─────────────────────────────────────────────────────────────────
    private JPanel buildTablaCard() {
        JPanel outer = new JPanel(new BorderLayout(0, 10));
        outer.setOpaque(false);

        // Tarjeta
        JPanel card = card();
        card.setLayout(new BorderLayout(0, 0));

        // Modelo
        tableModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 7; }
            @Override public Class<?> getColumnClass(int c) { return Object.class; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(46);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setBackground(CARD);
        table.setGridColor(new Color(0xF1F5F9));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionBackground(new Color(0xEFF6FF));
        table.setSelectionForeground(TXT_D);
        table.setFocusable(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        // Header
        JTableHeader header = table.getTableHeader();
        header.setBackground(NAVY);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);

        // Anchos de columna
        int[] widths = {82, 72, 140, 110, 100, 270, 120, 90};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Renderers
        table.getColumnModel().getColumn(0).setCellRenderer(new FechaRenderer());
        table.getColumnModel().getColumn(1).setCellRenderer(new HoraRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new UsuarioRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new AccionBadgeRenderer());
        table.getColumnModel().getColumn(7).setCellRenderer(new DetalleRenderer());
        table.getColumnModel().getColumn(7).setCellEditor(new DetalleEditor(this));

        // Renderer por defecto con padding
        DefaultTableCellRenderer defRender = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                if (!sel) comp.setBackground(r % 2 == 0 ? CARD : new Color(0xF8FAFC));
                return comp;
            }
        };
        for (int c : new int[]{4, 5, 6}) {
            table.getColumnModel().getColumn(c).setCellRenderer(defRender);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(CARD);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        card.add(header, BorderLayout.NORTH);
        card.add(scroll,  BorderLayout.CENTER);
        card.add(buildPie(), BorderLayout.SOUTH);

        outer.add(card, BorderLayout.CENTER);
        return outer;
    }

    // ── Pie de paginación ─────────────────────────────────────────────────────
    private JPanel buildPie() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(new Color(0xFAFAFA));
        p.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER),
            new EmptyBorder(8, 14, 8, 14)));

        lblConteo = new JLabel("Cargando…");
        lblConteo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblConteo.setForeground(TXT_G);

        paginacionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        paginacionBox.setOpaque(false);

        p.add(lblConteo,    BorderLayout.WEST);
        p.add(paginacionBox, BorderLayout.EAST);
        return p;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CONTROLLER — LÓGICA
    // ═════════════════════════════════════════════════════════════════════════

    /** Carga acciones y entidades desde BD en segundo plano. */
    private void cargarCombosAsync() {
        new SwingWorker<Void, Void>() {
            List<String> acciones, entidades;
            @Override protected Void doInBackground() {
                acciones  = service.getAcciones();
                entidades = service.getEntidades();
                return null;
            }
            @Override protected void done() {
                updateCombo(cmbAccion,  acciones);
                updateCombo(cmbEntidad, entidades);
            }
        }.execute();
    }

    /** Aplica los filtros actuales y recarga desde página 0. */
    private void aplicarFiltros() {
        paginaActual = 0;
        buscar();
    }

    /** Resetea todos los filtros a valores por defecto. */
    private void limpiarFiltros() {
        setSpinnerDate(spDesde, LocalDate.now().withDayOfMonth(1));
        setSpinnerDate(spHasta, LocalDate.now());
        cmbAccion.setSelectedIndex(0);
        cmbEntidad.setSelectedIndex(0);
        txtBuscar.setText("");
        paginaActual = 0;
        buscar();
    }

    /** Ejecuta la búsqueda asíncrona. */
    void buscar() {
        LocalDate desde  = getSpinnerDate(spDesde);
        LocalDate hasta  = getSpinnerDate(spHasta);
        String accion    = comboVal(cmbAccion);
        String entidad   = comboVal(cmbEntidad);
        String texto     = txtBuscar.getText().trim();
        final int pag    = paginaActual;

        tableModel.setRowCount(0);
        tableModel.addRow(new Object[]{"Cargando…","","","","","","",null});
        lblConteo.setText("Cargando…");

        new SwingWorker<PaginaAuditoria, Void>() {
            @Override protected PaginaAuditoria doInBackground() {
                return service.buscar(desde, hasta, accion, entidad,
                        texto.isEmpty() ? null : texto, pag);
            }
            @Override protected void done() {
                try {
                    PaginaAuditoria res = get();
                    paginaData.clear();
                    paginaData.addAll(res.registros());
                    totalRegistros = res.totalRegistros();
                    totalPaginas   = res.totalPaginas();
                    paginaActual   = res.paginaActual();
                    poblarTabla();
                    actualizarPaginacion();
                } catch (InterruptedException | ExecutionException ex) {
                    tableModel.setRowCount(0);
                    tableModel.addRow(new Object[]{"Error al cargar","","","","","","",null});
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void poblarTabla() {
        tableModel.setRowCount(0);
        if (paginaData.isEmpty()) {
            tableModel.addRow(new Object[]{"Sin resultados","","","","","","",null});
            lblConteo.setText("No se encontraron registros.");
            return;
        }
        for (RegistroAuditoria r : paginaData) {
            tableModel.addRow(new Object[]{
                r.getFechaStr(),
                r.getHoraStr(),
                r.getUsuario(),
                r,                      // Acción — renderer usa el objeto completo
                r.getEntidad(),
                r.getDescripcion(),
                r.getModulo(),
                r                       // Detalle — editor usa el objeto completo
            });
        }
        int desde = paginaActual * PAGE_SIZE + 1;
        int hasta = Math.min(desde + paginaData.size() - 1, totalRegistros);
        lblConteo.setText("Mostrando " + desde + " a " + hasta +
                " de " + totalRegistros + " registros");
    }

    private void actualizarPaginacion() {
        paginacionBox.removeAll();

        // Flecha anterior
        JButton prev = pageArrow("‹");
        prev.setEnabled(paginaActual > 0);
        prev.addActionListener(e -> cambiarPagina(paginaActual - 1));
        paginacionBox.add(prev);

        // Números de página con elipsis
        List<Integer> pagNums = calcularPaginas(paginaActual, totalPaginas);
        int prev_ = -1;
        for (int pg : pagNums) {
            if (prev_ != -1 && pg - prev_ > 1) {
                JLabel dots = new JLabel("…");
                dots.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                dots.setForeground(TXT_G);
                dots.setBorder(new EmptyBorder(0, 4, 0, 4));
                paginacionBox.add(dots);
            }
            paginacionBox.add(pageBtn(pg, pg == paginaActual));
            prev_ = pg;
        }

        // Flecha siguiente
        JButton next = pageArrow("›");
        next.setEnabled(paginaActual < totalPaginas - 1);
        next.addActionListener(e -> cambiarPagina(paginaActual + 1));
        paginacionBox.add(next);

        paginacionBox.revalidate();
        paginacionBox.repaint();
    }

    /** Cambia a la página indicada y recarga. */
    void cambiarPagina(int pagina) {
        paginaActual = pagina;
        buscar();
    }

    /** Muestra el diálogo de detalle. */
    void verDetalle(RegistroAuditoria r) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner, "Detalle del registro #" + r.getIdActividad(), true);
        dlg.setSize(520, 380);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.add(buildDetalleContent(r));
        dlg.setVisible(true);
    }


    // ═════════════════════════════════════════════════════════════════════════
    // DIÁLOGO DETALLE
    // ═════════════════════════════════════════════════════════════════════════

    private JPanel buildDetalleContent(RegistroAuditoria r) {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(20, 24, 16, 24));

        // Título con badge de acción
        JPanel headRow = new JPanel(new BorderLayout(12, 0));
        headRow.setOpaque(false);
        JLabel titulo = new JLabel("Registro de Auditoría  #" + r.getIdActividad());
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titulo.setForeground(NAVY);
        JLabel badge = makeBadge(r.getAccion());
        headRow.add(titulo, BorderLayout.CENTER);
        headRow.add(badge,  BorderLayout.EAST);
        p.add(headRow, BorderLayout.NORTH);

        // Campos
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8));
        fields.setBackground(CARD);
        fields.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(14, 16, 14, 16)));

        addField(fields, "Fecha y hora",    r.getFechaHoraStr());
        addField(fields, "Usuario",         r.getUsuario() + "  (N° doc: " + r.getNumDocUsuario() + ")");
        addField(fields, "Acción",          r.getAccion().label);
        addField(fields, "Entidad",         r.getEntidad());
        addField(fields, "Módulo",          r.getModulo());
        addField(fields, "Doc. afectado",   r.getNumDocAfectado() > 0 ? String.valueOf(r.getNumDocAfectado()) : "—");

        // Descripción (ancho completo)
        JLabel dKey = new JLabel("Descripción:");
        dKey.setFont(new Font("Segoe UI", Font.BOLD, 11));
        dKey.setForeground(TXT_G);

        JTextArea dVal = new JTextArea(r.getDescripcion());
        dVal.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dVal.setForeground(TXT_D);
        dVal.setEditable(false);
        dVal.setOpaque(false);
        dVal.setLineWrap(true);
        dVal.setWrapStyleWord(true);
        dVal.setBorder(null);

        JPanel descBlock = new JPanel(new BorderLayout());
        descBlock.setBackground(CARD);
        descBlock.add(dKey, BorderLayout.NORTH);
        descBlock.add(dVal, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(fields,    BorderLayout.NORTH);
        body.add(descBlock, BorderLayout.CENTER);
        p.add(body, BorderLayout.CENTER);

        // Pie
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        foot.setOpaque(false);
        JButton close = solidBtn("  Cerrar  ", NAVY);
        close.addActionListener(e ->
            SwingUtilities.getWindowAncestor((Component) e.getSource()).dispose());
        foot.add(close);
        p.add(foot, BorderLayout.SOUTH);
        return p;
    }

    private void addField(JPanel p, String key, String val) {
        JLabel k = new JLabel(key + ":");
        k.setFont(new Font("Segoe UI", Font.BOLD, 11));
        k.setForeground(TXT_G);
        JLabel v = new JLabel(val);
        v.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        v.setForeground(TXT_D);
        p.add(k); p.add(v);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // RENDERERS
    // ═════════════════════════════════════════════════════════════════════════

    /** Badge colorido para la columna Acción. */
    private static class AccionBadgeRenderer implements TableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            RegistroAuditoria reg = (v instanceof RegistroAuditoria) ? (RegistroAuditoria) v : null;
            Accion accion = (reg != null) ? reg.getAccion() : Accion.OTRO;
            JLabel lbl = makeBadge(accion);
            lbl.setOpaque(true);
            lbl.setBackground(sel ? new Color(0xEFF6FF) : (r % 2 == 0 ? Color.WHITE : new Color(0xF8FAFC)));
            return lbl;
        }
    }

    static JLabel makeBadge(Accion accion) {
        JLabel lbl = new JLabel("  " + accion.label + "  ", SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accion.colorBg));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(new Color(accion.colorFg));
        lbl.setBorder(new EmptyBorder(3, 8, 3, 8));
        return lbl;
    }

    private static class FechaRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            l.setFont(new Font("Segoe UI", Font.BOLD, 11));
            l.setForeground(TXT_D);
            l.setBorder(new EmptyBorder(0, 10, 0, 6));
            if (!sel) l.setBackground(r % 2 == 0 ? Color.WHITE : new Color(0xF8FAFC));
            return l;
        }
    }

    private static class HoraRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            l.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            l.setForeground(TXT_G);
            l.setBorder(new EmptyBorder(0, 6, 0, 10));
            if (!sel) l.setBackground(r % 2 == 0 ? Color.WHITE : new Color(0xF8FAFC));
            return l;
        }
    }

    private static class UsuarioRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            l.setFont(new Font("Segoe UI", Font.BOLD, 12));
            l.setForeground(NAVY);
            l.setBorder(new EmptyBorder(0, 10, 0, 10));
            if (!sel) l.setBackground(r % 2 == 0 ? Color.WHITE : new Color(0xF8FAFC));
            return l;
        }
    }

    private static class DetalleRenderer implements TableCellRenderer {
        private final JButton btn = makeDetalleBtn();
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            btn.setBackground(sel ? new Color(0xEFF6FF) : (r % 2 == 0 ? Color.WHITE : new Color(0xF8FAFC)));
            return btn;
        }
    }

    private static class DetalleEditor extends DefaultCellEditor {
        private final HistorialAuditoriaPanel panelRef;
        private RegistroAuditoria current;
        private final JButton btn = makeDetalleBtn();

        DetalleEditor(HistorialAuditoriaPanel ref) {
            super(new JCheckBox());
            this.panelRef = ref;
            btn.addActionListener(e -> {
                fireEditingStopped();
                if (current != null) panelRef.verDetalle(current);
            });
        }
        @Override public Component getTableCellEditorComponent(
                JTable t, Object v, boolean sel, int r, int c) {
            current = (v instanceof RegistroAuditoria) ? (RegistroAuditoria) v : null;
            return btn;
        }
        @Override public Object getCellEditorValue() { return current; }
    }

    static JButton makeDetalleBtn() {
        JButton b = new JButton("👁  Ver detalle") {
            boolean hov;
            { addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hov = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg2 = hov ? new Color(0xDBEAFE) : new Color(0xEFF6FF);
                g2.setColor(bg2);
                g2.fill(new RoundRectangle2D.Float(2, 4, getWidth()-4, getHeight()-8, 6, 6));
                g2.setColor(new Color(0x2563EB));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(2, 4, getWidth()-4, getHeight()-8, 6, 6));
                g2.dispose(); super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 10));
        b.setForeground(new Color(0x2563EB));
        b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }


    // ═════════════════════════════════════════════════════════════════════════
    // HELPERS UI
    // ═════════════════════════════════════════════════════════════════════════

    private JPanel card() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0,0,0,10));
                g2.fill(new RoundRectangle2D.Float(2, 3, getWidth()-3, getHeight()-3, 12, 12));
                g2.setColor(CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-2, getHeight()-2, 12, 12));
                g2.setColor(BORDER); g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-3, getHeight()-3, 12, 12));
                g2.dispose(); super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
    }

    private JTextField searchField() {
        JTextField f = new JTextField(20);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        f.setPreferredSize(new Dimension(240, 30));
        f.putClientProperty("JTextField.placeholderText",
            "Buscar por usuario, acción o módulo…");
        f.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(0, 8, 0, 8)));
        return f;
    }

    private JSpinner dateSpinner(LocalDate initial) {
        javax.swing.SpinnerDateModel m = new javax.swing.SpinnerDateModel();
        m.setValue(toDate(initial));
        JSpinner s = new JSpinner(m);
        s.setEditor(new JSpinner.DateEditor(s, "dd/MM/yyyy"));
        s.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        s.setPreferredSize(new Dimension(118, 30));
        return s;
    }

    private JComboBox<String> combo(String[] items, int w) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        c.setBackground(Color.WHITE);
        c.setPreferredSize(new Dimension(w, 30));
        return c;
    }

    private JLabel boldLabel(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(TXT_G);
        return l;
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
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,8,8));
                g2.dispose(); super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 12)); b.setForeground(Color.WHITE);
        b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(100, 30));
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
                g2.setColor(hov ? new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),20) : Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,8,8));
                g2.setColor(accent); g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0.5f,0.5f,getWidth()-2,getHeight()-2,8,8));
                g2.dispose(); super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 11)); b.setForeground(accent);
        b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(130, 30));
        return b;
    }

    private JButton pageBtn(int pg, boolean active) {
        JButton b = new JButton(String.valueOf(pg + 1)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(active ? BLUE : Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,6,6));
                if (!active) { g2.setColor(BORDER); g2.setStroke(new BasicStroke(1f));
                    g2.draw(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,6,6)); }
                g2.dispose(); super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 11));
        b.setForeground(active ? Color.WHITE : TXT_D);
        b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(30, 26));
        if (!active) b.addActionListener(e -> cambiarPagina(pg));
        return b;
    }

    private JButton pageArrow(String sym) {
        JButton b = new JButton(sym) {
            boolean hov;
            { addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hov = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                if (hov && isEnabled()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0xEFF6FF));
                    g2.fill(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,6,6));
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 16));
        b.setForeground(TXT_G); b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(28, 26));
        return b;
    }

    /** Calcula qué números de página mostrar (con ventana deslizante). */
    private List<Integer> calcularPaginas(int current, int total) {
        List<Integer> list = new ArrayList<>();
        if (total <= 7) {
            for (int i = 0; i < total; i++) list.add(i);
        } else {
            list.add(0);
            int lo = Math.max(1, current - 1);
            int hi = Math.min(total - 2, current + 1);
            for (int i = lo; i <= hi; i++) list.add(i);
            list.add(total - 1);
        }
        return list.stream().distinct().sorted().toList();
    }

    // ── Debounce ──────────────────────────────────────────────────────────────
    private Timer debounceTimer;
    private void scheduleSearch() {
        if (debounceTimer != null && debounceTimer.isRunning()) debounceTimer.stop();
        debounceTimer = new Timer(350, e -> { paginaActual = 0; buscar(); });
        debounceTimer.setRepeats(false);
        debounceTimer.start();
    }

    // ── Helpers generales ────────────────────────────────────────────────────
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

    private static String comboVal(JComboBox<String> c) {
        String v = (String) c.getSelectedItem();
        return (v == null || "Todas".equals(v) || "Todos".equals(v)) ? null : v;
    }

    private static void updateCombo(JComboBox<String> c, List<String> items) {
        c.removeAllItems();
        items.forEach(c::addItem);
    }
}
