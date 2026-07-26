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
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import com.saia.business.AuditoriaService;
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

    private static final String[] COLS = {
        "Fecha", "Hora", "Usuario", "Acción", "Entidad", "Descripción", "Módulo", "Detalle"
    };

    // ── Servicio ──────────────────────────────────────────────────────────────
    private final AuditoriaService service = new AuditoriaService();

    // ── Estado ────────────────────────────────────────────────────────────────
    private final List<RegistroAuditoria> todosRegistros = new ArrayList<>();

    // ── Filtros ───────────────────────────────────────────────────────────────
    private JSpinner  spDesde, spHasta;
    private JComboBox<String> cmbAccion, cmbEntidad;

    // ── Tabla ─────────────────────────────────────────────────────────────────
    private DefaultTableModel tableModel;
    private JTable            table;
    private JLabel            lblConteo;

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

        JLabel titulo = new JLabel("  Historial de Auditoría");
        titulo.setIcon(com.saia.presentation.IconUtil.icon(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.HISTORY, 20, TXT_D));
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
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 12));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        card.setBorder(new EmptyBorder(2, 6, 2, 6));

        // Fecha desde
        card.add(boldLabel("Desde:"));
        spDesde = dateSpinner(LocalDate.now().withDayOfMonth(1));
        card.add(spDesde);

        // Espacio
        card.add(Box.createHorizontalStrut(8));

        // Fecha hasta
        card.add(boldLabel("Hasta:"));
        spHasta = dateSpinner(LocalDate.now());
        card.add(spHasta);

        // Espacio
        card.add(Box.createHorizontalStrut(8));

        // Acción
        card.add(boldLabel("Acción:"));
        cmbAccion = combo(new String[]{"Todas"}, 160);
        card.add(cmbAccion);

        // Espacio
        card.add(Box.createHorizontalStrut(8));

        // Entidad / Módulo
        card.add(boldLabel("Módulo:"));
        cmbEntidad = combo(new String[]{"Todas"}, 160);
        card.add(cmbEntidad);

        // Espacio flexible para empujar los botones a la derecha
        card.add(Box.createHorizontalStrut(20));

        // Botón limpiar
        JButton btnLimpiar = UITheme.outlineButton("  Limpiar filtros", new Color(0x64748B), 150, 32);
        btnLimpiar.setIcon(com.saia.presentation.IconUtil.icon(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.REDO, 14, new Color(0x64748B)));
        btnLimpiar.addActionListener(e -> limpiarFiltros());
        card.add(btnLimpiar);

        // Espacio entre botones
        card.add(Box.createHorizontalStrut(6));

        // Botón buscar
        JButton btnBuscar = UITheme.solidButton("  Buscar", BLUE, 110, 32);
        btnBuscar.setIcon(com.saia.presentation.IconUtil.icon(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.SEARCH, 14, Color.WHITE));
        btnBuscar.addActionListener(e -> buscar());
        card.add(btnBuscar);

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
        UITheme.styleTableHeader(table);
        table.getTableHeader().setResizingAllowed(true);

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

        // Renderer por defecto con padding y centrado
        DefaultTableCellRenderer defRender = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
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

        card.add(table.getTableHeader(), BorderLayout.NORTH);
        card.add(scroll,  BorderLayout.CENTER);

        // Pie con contador de registros
        JPanel pie = new JPanel(new BorderLayout());
        pie.setBackground(new Color(0xFAFAFA));
        pie.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER),
            new EmptyBorder(8, 14, 8, 14)));

        lblConteo = new JLabel("Cargando…");
        lblConteo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblConteo.setForeground(TXT_G);
        pie.add(lblConteo, BorderLayout.WEST);

        card.add(pie, BorderLayout.SOUTH);

        outer.add(card, BorderLayout.CENTER);
        return outer;
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

    /** Resetea todos los filtros a valores por defecto. */
    private void limpiarFiltros() {
        setSpinnerDate(spDesde, LocalDate.now().withDayOfMonth(1));
        setSpinnerDate(spHasta, LocalDate.now());
        cmbAccion.setSelectedIndex(0);
        cmbEntidad.setSelectedIndex(0);
        buscar();
    }

    /** Ejecuta la búsqueda asíncrona de todos los registros. */
    void buscar() {
        LocalDate desde  = getSpinnerDate(spDesde);
        LocalDate hasta  = getSpinnerDate(spHasta);
        String accion    = comboVal(cmbAccion);
        String entidad   = comboVal(cmbEntidad);

        tableModel.setRowCount(0);
        tableModel.addRow(new Object[]{"Cargando…","","","","","","",null});
        lblConteo.setText("Cargando…");

        new SwingWorker<List<RegistroAuditoria>, Void>() {
            @Override protected List<RegistroAuditoria> doInBackground() {
                return service.buscarTodos(desde, hasta, accion, entidad, null);
            }
            @Override protected void done() {
                try {
                    List<RegistroAuditoria> registros = get();
                    todosRegistros.clear();
                    todosRegistros.addAll(registros);
                    poblarTabla();
                } catch (InterruptedException | ExecutionException ex) {
                    tableModel.setRowCount(0);
                    tableModel.addRow(new Object[]{"Error al cargar","","","","","","",null});
                    lblConteo.setText("Error al cargar registros.");
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void poblarTabla() {
        tableModel.setRowCount(0);
        if (todosRegistros.isEmpty()) {
            tableModel.addRow(new Object[]{"Sin resultados","","","","","","",null});
            lblConteo.setText("No se encontraron registros.");
            return;
        }
        for (RegistroAuditoria r : todosRegistros) {
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
        lblConteo.setText("Mostrando " + todosRegistros.size() + " registros");
    }

    /** Muestra el diálogo de detalle. */
    void verDetalle(RegistroAuditoria r) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner, "Detalle del Registro de Auditoría", true);
        dlg.setSize(750, 650);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(true);
        dlg.add(buildDetalleContent(r));
        dlg.setVisible(true);
    }


    // ═════════════════════════════════════════════════════════════════════════
    // DIÁLOGO DETALLE
    // ═════════════════════════════════════════════════════════════════════════

    private JPanel buildDetalleContent(RegistroAuditoria r) {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(24, 28, 20, 28));

        // === ENCABEZADO ===
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);
        
        // Fila 1: Título con badge
        JPanel titleRow = new JPanel(new BorderLayout(12, 0));
        titleRow.setOpaque(false);
        
        JLabel titulo = new JLabel("Registro de Auditoría #" + r.getIdActividad());
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titulo.setForeground(NAVY);
        titulo.setIcon(com.saia.presentation.IconUtil.icon(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.FILE_ALT, 18, NAVY));
        
        JLabel badge = makeBadge(r.getAccion());
        
        titleRow.add(titulo, BorderLayout.CENTER);
        titleRow.add(badge, BorderLayout.EAST);
        
        // Fila 2: Fecha y hora con icono
        JLabel fechaHora = new JLabel(r.getFechaHoraStr());
        fechaHora.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fechaHora.setForeground(TXT_G);
        fechaHora.setIcon(com.saia.presentation.IconUtil.icon(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.CLOCK, 12, TXT_G));
        
        header.add(titleRow, BorderLayout.NORTH);
        header.add(fechaHora, BorderLayout.CENTER);
        
        p.add(header, BorderLayout.NORTH);

        // === CUERPO ===
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        // Sección: Información del Administrador
        body.add(buildSeccion("Administrador que realizó la acción", 
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.USER_SHIELD));
        body.add(vgap(8));
        
        JPanel adminCard = infoCard();
        adminCard.setLayout(new GridLayout(3, 2, 16, 12));
        addInfoField(adminCard, "Nombre completo", r.getUsuario());
        addInfoField(adminCard, "N° de Documento", String.valueOf(r.getNumDocUsuario()));
        addInfoField(adminCard, "Rol en el sistema", "Administrador SAIA");
        addInfoField(adminCard, "Fecha de la acción", r.getFechaStr());
        addInfoField(adminCard, "Hora de la acción", r.getHoraStr());
        addInfoField(adminCard, "Módulo utilizado", r.getModulo());
        body.add(adminCard);
        body.add(vgap(18));

        // Sección: Información de la Persona Afectada
        if (r.getNumDocAfectado() > 0) {
            body.add(buildSeccion("Persona afectada por la acción", 
                org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.USER));
            body.add(vgap(8));
            
            JPanel afectadoCard = infoCard();
            afectadoCard.setLayout(new GridLayout(2, 2, 16, 12));
            
            // Obtener nombre desde la base de datos
            String nombreAfectado = service.getNombrePersona(r.getNumDocAfectado());
            
            addInfoField(afectadoCard, "N° de Documento", String.valueOf(r.getNumDocAfectado()));
            addInfoField(afectadoCard, "Nombre", nombreAfectado);
            addInfoField(afectadoCard, "Tipo de usuario", r.getEntidad());
            addInfoField(afectadoCard, "Estado actual", 
                r.getAccion() == Accion.BLOQUEAR ? "Cuenta Bloqueada" : 
                r.getAccion() == Accion.HABILITAR ? "Cuenta Activa" : "—");
            
            body.add(afectadoCard);
            body.add(vgap(18));
        }

        // Sección: Detalles de la Acción
        body.add(buildSeccion("Detalles de la operación", 
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.TASKS));
        body.add(vgap(8));
        
        JPanel accionCard = infoCard();
        accionCard.setLayout(new GridLayout(2, 2, 16, 12));
        addInfoField(accionCard, "Tipo de acción", r.getAccion().label);
        addInfoField(accionCard, "Entidad modificada", r.getEntidad());
        addInfoField(accionCard, "Módulo del sistema", r.getModulo());
        addInfoField(accionCard, "ID del registro", "#" + r.getIdActividad());
        body.add(accionCard);
        body.add(vgap(18));

        // Sección: Descripción Detallada
        body.add(buildSeccion("Descripción completa de la acción", 
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.ALIGN_LEFT));
        body.add(vgap(8));
        
        JPanel descCard = infoCard();
        descCard.setLayout(new BorderLayout());
        
        JTextArea descArea = new JTextArea(r.getDescripcion());
        descArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descArea.setForeground(TXT_D);
        descArea.setBackground(CARD);
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        descArea.setRows(3);
        
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setBorder(null);
        descScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        descScroll.setPreferredSize(new Dimension(0, 90));
        
        descCard.add(descScroll, BorderLayout.CENTER);
        body.add(descCard);

        JScrollPane bodyScroll = new JScrollPane(body);
        bodyScroll.setBorder(null);
        bodyScroll.getViewport().setBackground(BG);
        bodyScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        p.add(bodyScroll, BorderLayout.CENTER);

        // === PIE ===
        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        foot.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        // Info adicional a la izquierda
        JLabel infoRegistro = new JLabel("Registro guardado en el sistema de auditoría SAIA");
        infoRegistro.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        infoRegistro.setForeground(TXT_G);
        infoRegistro.setIcon(com.saia.presentation.IconUtil.icon(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.INFO_CIRCLE, 10, TXT_G));
        
        // Botón cerrar a la derecha
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnPanel.setOpaque(false);
        
        JButton close = UITheme.solidButton("  Cerrar  ", NAVY, 120, 36);
        close.setIcon(com.saia.presentation.IconUtil.icon(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.TIMES, 14, Color.WHITE));
        close.addActionListener(e ->
            SwingUtilities.getWindowAncestor((Component) e.getSource()).dispose());
        btnPanel.add(close);
        
        foot.add(infoRegistro, BorderLayout.WEST);
        foot.add(btnPanel, BorderLayout.EAST);
        
        p.add(foot, BorderLayout.SOUTH);
        
        return p;
    }

    private JLabel buildSeccion(String titulo, org.kordamp.ikonli.fontawesome5.FontAwesomeSolid icono) {
        JLabel lbl = new JLabel("  " + titulo);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(NAVY);
        lbl.setIcon(com.saia.presentation.IconUtil.icon(icono, 14, NAVY));
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel infoCard() {
        JPanel card = new JPanel();
        card.setBackground(CARD);
        card.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(16, 18, 16, 18)));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return card;
    }

    private void addInfoField(JPanel panel, String label, String valor) {
        JPanel fieldPanel = new JPanel(new BorderLayout(0, 4));
        fieldPanel.setOpaque(false);
        
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(TXT_G);
        
        JLabel val = new JLabel(valor);
        val.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        val.setForeground(TXT_D);
        
        fieldPanel.add(lbl, BorderLayout.NORTH);
        fieldPanel.add(val, BorderLayout.CENTER);
        
        panel.add(fieldPanel);
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
            l.setHorizontalAlignment(SwingConstants.CENTER);
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
            l.setHorizontalAlignment(SwingConstants.CENTER);
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
            l.setHorizontalAlignment(SwingConstants.CENTER);
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
        JButton b = new JButton("  Ver detalle") {
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
        b.setIcon(com.saia.presentation.IconUtil.icon(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.EYE, 12, new Color(0x2563EB)));
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
