package com.saia.presentation.home;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import com.saia.business.AprendizService;
import com.saia.business.AprendizService.EstadoResult;
import com.saia.model.Aprendiz;
import com.saia.model.Persona;

/**
 * Panel de gestión de Aprendices.
 * Muestra la lista completa con estado de cuenta y botones de Bloquear/Habilitar.
 */
public class AprendicesPanel extends JPanel {

    // ── Colores ───────────────────────────────────────────────────────────────
    private static final Color NAVY        = new Color(0x1A3A5C);
    private static final Color BG_PAGE     = new Color(0xF5F6FA);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color BORDER_C    = new Color(0xE8E8E8);
    private static final Color TEXT_DARK   = new Color(0x1A1A2E);

    private static final String[] COLS = {
        "#", "Tipo Doc", "N° Documento", "Nombres", "Apellidos",
        "Correo", "Centro", "Nombre Ficha", "N° Ficha", "Estado", "Acción"
    };

    private final AprendizService        service    = new AprendizService();
    private final DefaultTableModel      tableModel = buildTableModel();
    private final JTable                 table      = buildTable();
    private       List<Aprendiz>         todos      = new ArrayList<>();

    private JTextField txtBuscarDoc;
    private JTextField txtBuscarFicha;
    private JTextField txtBuscarPrograma;

    public AprendicesPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG_PAGE);
        setBorder(new EmptyBorder(20, 24, 20, 24));
        buildUI();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::cargarDatos);
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    private void buildUI() {
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 0, 14, 0));

        JLabel titulo = new JLabel("\uD83C\uDF93  Aprendices");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titulo.setForeground(TEXT_DARK);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        // ── Buscador por N° documento ─────────────────────────────────────────
        txtBuscarDoc = buildSearchField("N° documento...", 160);

        // ── Buscador por ficha ────────────────────────────────────────────────
        txtBuscarFicha = buildSearchField("N° ficha...", 110);

        // ── Buscador por nombre de ficha (texto libre, ej: ADSO-20) ──────────
        txtBuscarPrograma = buildSearchFieldText("Ej: ADSO-20...", 140);

        // DocumentListener compartido que activa el filtrado
        javax.swing.event.DocumentListener dl = new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate (javax.swing.event.DocumentEvent e) { filtrar(); }
            @Override public void removeUpdate (javax.swing.event.DocumentEvent e) { filtrar(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
        };
        txtBuscarDoc.getDocument().addDocumentListener(dl);
        txtBuscarFicha.getDocument().addDocumentListener(dl);
        txtBuscarPrograma.getDocument().addDocumentListener(dl);

        JButton btnRefresh = buildBtn("\u21BB  Actualizar", NAVY, Color.WHITE, 120);
        btnRefresh.addActionListener(e -> cargarDatos());

        right.add(new JLabel("Doc:") {{ setFont(new Font("Segoe UI", Font.BOLD, 12)); setForeground(TEXT_DARK); }});
        right.add(txtBuscarDoc);
        right.add(new JLabel("Ficha:") {{ setFont(new Font("Segoe UI", Font.BOLD, 12)); setForeground(TEXT_DARK); }});
        right.add(txtBuscarFicha);
        right.add(new JLabel("Nombre ficha:") {{ setFont(new Font("Segoe UI", Font.BOLD, 12)); setForeground(TEXT_DARK); }});
        right.add(txtBuscarPrograma);
        right.add(btnRefresh);

        bar.add(titulo, BorderLayout.WEST);
        bar.add(right,  BorderLayout.EAST);
        return bar;
    }

    /** Crea un campo de búsqueda con placeholder y filtro numérico. */
    private JTextField buildSearchField(String placeholder, int width) {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setBorder(new LineBorder(BORDER_C, 1, true));
        f.setPreferredSize(new Dimension(width, 34));
        f.setForeground(new Color(0xAAAAAA));
        f.setText(placeholder);

        // Filtro: solo números
        ((AbstractDocument) f.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void insertString(FilterBypass fb, int off, String s, AttributeSet a)
                    throws BadLocationException {
                if (s != null && s.matches("\\d+")) super.insertString(fb, off, s, a);
            }
            @Override public void replace(FilterBypass fb, int off, int len, String s, AttributeSet a)
                    throws BadLocationException {
                if (s != null && s.matches("\\d*")) super.replace(fb, off, len, s, a);
            }
        });

        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (f.getText().equals(placeholder)) {
                    f.setText(""); f.setForeground(Color.BLACK);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) {
                    f.setText(placeholder);
                    f.setForeground(new Color(0xAAAAAA));
                }
            }
        });
        return f;
    }

    /** Crea un campo de búsqueda con placeholder, sin restricción de tipo. */
    private JTextField buildSearchFieldText(String placeholder, int width) {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setBorder(new LineBorder(BORDER_C, 1, true));
        f.setPreferredSize(new Dimension(width, 34));
        f.setForeground(new Color(0xAAAAAA));
        f.setText(placeholder);
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (f.getText().equals(placeholder)) {
                    f.setText(""); f.setForeground(Color.BLACK);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) {
                    f.setText(placeholder);
                    f.setForeground(new Color(0xAAAAAA));
                }
            }
        });
        return f;
    }

    private JPanel buildTableCard() {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0,0,0,14));
                g2.fill(new RoundRectangle2D.Float(2,3,getWidth()-3,getHeight()-3,10,10));
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth()-2,getHeight()-2,10,10));
                g2.dispose(); super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };

        JTableHeader header = table.getTableHeader();
        header.setBackground(NAVY);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setPreferredSize(new Dimension(0, 36));
        header.setReorderingAllowed(false);

        table.setBackground(CARD_BG);
        table.setForeground(TEXT_DARK);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(42);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(0xEEEEEE));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(0xE8F0FB));
        table.setSelectionForeground(TEXT_DARK);
        table.setFocusable(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(CARD_BG);

        card.add(header, BorderLayout.NORTH);
        card.add(scroll,  BorderLayout.CENTER);
        return card;
    }

    // ── Tabla ─────────────────────────────────────────────────────────────────

    private DefaultTableModel buildTableModel() {
        return new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 10; }
            @Override public Class<?> getColumnClass(int c) {
                return c == 0 ? Integer.class : Object.class;
            }
        };
    }

    private JTable buildTable() {
        JTable t = new JTable(tableModel);
        // Columna Estado (índice 9) con badge
        t.getColumnModel().getColumn(9).setCellRenderer(new EstadoRenderer());
        // Columna Acción (índice 10)
        t.getColumnModel().getColumn(10).setCellRenderer(new AccionRenderer());
        t.getColumnModel().getColumn(10).setCellEditor(new AccionEditor(this));
        // Anchos
        int[] w = {35, 100, 110, 140, 140, 180, 80, 90, 70, 80, 110};
        for (int i = 0; i < w.length; i++) t.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        t.getColumnModel().getColumn(0).setMaxWidth(35);
        return t;
    }

    // ── Carga y filtrado ──────────────────────────────────────────────────────

    void cargarDatos() {
        tableModel.setRowCount(0);
        tableModel.addRow(new Object[]{"", "Cargando...", "", "", "", "", "", "", ""});

        SwingWorker<List<Aprendiz>, Void> w = new SwingWorker<>() {
            @Override protected List<Aprendiz> doInBackground() {
                return service.listarTodos();
            }
            @Override protected void done() {
                try {
                    todos = get();
                    poblarTabla(todos);
                } catch (InterruptedException | ExecutionException ex) {
                    tableModel.setRowCount(0);
                    tableModel.addRow(new Object[]{"", "Error al cargar datos", "", "", "", "", "", "", ""});
                    Thread.currentThread().interrupt();
                }
            }
        };
        w.execute();
    }

    private void filtrar() {
        String tDoc      = getSearchValue(txtBuscarDoc,      "N° documento...");
        String tFicha    = getSearchValue(txtBuscarFicha,    "N° ficha...");
        String tPrograma = getSearchValue(txtBuscarPrograma, "Ej: ADSO-20...").toLowerCase();

        if (tDoc.isEmpty() && tFicha.isEmpty() && tPrograma.isEmpty()) {
            poblarTabla(todos); return;
        }

        List<Aprendiz> filtrados = new ArrayList<>();
        for (Aprendiz a : todos) {
            boolean matchDoc      = tDoc.isEmpty()
                    || String.valueOf(a.getNumDoc()).startsWith(tDoc);
            boolean matchFicha    = tFicha.isEmpty()
                    || String.valueOf(a.getIdFicha()).startsWith(tFicha);
            boolean matchPrograma = tPrograma.isEmpty()
                    || (a.getSiglasProg() != null
                        && a.getSiglasProg().toLowerCase().contains(tPrograma));
            if (matchDoc && matchFicha && matchPrograma) filtrados.add(a);
        }
        poblarTabla(filtrados);
    }

    /** Retorna el texto del campo si no es el placeholder ni está vacío. */
    private String getSearchValue(JTextField f, String placeholder) {
        String t = f.getText().trim();
        return t.equals(placeholder) ? "" : t;
    }

    private void poblarTabla(List<Aprendiz> lista) {
        tableModel.setRowCount(0);
        if (lista.isEmpty()) {
            tableModel.addRow(new Object[]{"", "", "Sin aprendices registrados", "", "", "", "", "", "", "", ""});
            return;
        }
        int i = 1;
        for (Aprendiz a : lista) {
            Persona p = a.getPersona();
            tableModel.addRow(new Object[]{
                i++,
                p != null ? nvl(p.getTipDoc()) : "",
                a.getNumDoc(),
                p != null ? nvl(p.getNombres()) : "",
                p != null ? apellidos(p) : "",
                p != null ? nvl(p.getEmail()) : "",
                nvl(a.getSiglasCentro(), "—"),
                nvl(a.getSiglasProg(),   "—"),
                a.getIdFicha() > 0 ? a.getIdFicha() : "—",
                a.isCuentaActiva() ? "Activa" : "Inactiva",
                a  // objeto completo para el editor
            });
        }
    }

    // ── Acción bloquear/habilitar ─────────────────────────────────────────────

    void ejecutarAccion(Aprendiz a) {
        boolean bloquear = a.isCuentaActiva();
        String msg = bloquear
                ? "¿Desea BLOQUEAR la cuenta de " + a.getNombreCompleto() + "?"
                : "¿Desea HABILITAR la cuenta de " + a.getNombreCompleto() + "?";
        int r = JOptionPane.showConfirmDialog(this, msg,
                bloquear ? "Bloquear aprendiz" : "Habilitar aprendiz",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;

        SwingWorker<EstadoResult, Void> w = new SwingWorker<>() {
            @Override protected EstadoResult doInBackground() {
                return bloquear
                        ? service.bloquear(a.getNumDoc())
                        : service.desbloquear(a.getNumDoc());
            }
            @Override protected void done() {
                try {
                    EstadoResult res = get();
                    if (res == EstadoResult.OK) {
                        String ok = bloquear
                                ? "Cuenta bloqueada correctamente."
                                : "Cuenta habilitada correctamente.";
                        JOptionPane.showMessageDialog(AprendicesPanel.this,
                                ok, "Operación exitosa", JOptionPane.INFORMATION_MESSAGE);
                        cargarDatos();
                    } else {
                        JOptionPane.showMessageDialog(AprendicesPanel.this,
                                "Error al cambiar estado. Intente nuevamente.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    JOptionPane.showMessageDialog(AprendicesPanel.this,
                            "Error inesperado: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    Thread.currentThread().interrupt();
                }
            }
        };
        w.execute();
    }

    // ── Renderers y Editor ────────────────────────────────────────────────────

    /** Renderer de la columna Estado con badge de color. */
    private static class EstadoRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int r, int c) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            if ("Activa".equals(v)) {
                lbl.setForeground(new Color(0x1B5E20));
                lbl.setBackground(new Color(0xE8F5E9));
            } else {
                lbl.setForeground(new Color(0xB71C1C));
                lbl.setBackground(new Color(0xFFEBEE));
            }
            lbl.setOpaque(true);
            return lbl;
        }
    }

    /** Renderer del botón de acción. */
    private static class AccionRenderer implements TableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int r, int c) {
            boolean activo = v instanceof Aprendiz && ((Aprendiz) v).isCuentaActiva();
            return makeBtn(activo);
        }
        private static JButton makeBtn(boolean activo) {
            JButton btn = new JButton(activo ? "\uD83D\uDD12 Bloquear" : "\u2705 Habilitar");
            btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btn.setForeground(Color.WHITE);
            btn.setBackground(activo ? new Color(0xC62828) : new Color(0x2E7D32));
            btn.setOpaque(true);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            return btn;
        }
    }

    /** Editor del botón de acción. */
    private static class AccionEditor extends DefaultCellEditor {
        private final AprendicesPanel panelRef;
        private Aprendiz current;
        private final JButton btn;

        AccionEditor(AprendicesPanel panel) {
            super(new JCheckBox());
            this.panelRef = panel;
            btn = new JButton();
            btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btn.setForeground(Color.WHITE);
            btn.setOpaque(true);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                fireEditingStopped();
                if (current != null) panelRef.ejecutarAccion(current);
            });
        }

        @Override public Component getTableCellEditorComponent(JTable t, Object v,
                boolean sel, int r, int c) {
            current = (v instanceof Aprendiz) ? (Aprendiz) v : null;
            boolean activo = current != null && current.isCuentaActiva();
            btn.setText(activo ? "\uD83D\uDD12 Bloquear" : "\u2705 Habilitar");
            btn.setBackground(activo ? new Color(0xC62828) : new Color(0x2E7D32));
            return btn;
        }

        @Override public Object getCellEditorValue() { return current; }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String nvl(String v) { return v != null ? v : ""; }
    private static String nvl(String v, String def) { return (v != null && !v.isBlank()) ? v : def; }
    private static String apellidos(Persona p) {
        String a = nvl(p.getPApe());
        if (p.getSApe() != null && !p.getSApe().isBlank()) a += " " + p.getSApe();
        return a.trim();
    }

    private static JButton buildBtn(String text, Color bg, Color fg, int w) {
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
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(w, 34));
        return btn;
    }
}
