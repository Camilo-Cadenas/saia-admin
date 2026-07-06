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
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
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

import com.saia.business.PersonalSeguridadService;
import com.saia.model.Persona;
import com.saia.model.PersonalSeguridad;
import static com.saia.presentation.home.PersonalSeguridadPanel.BG_PAGE;
import static com.saia.presentation.home.PersonalSeguridadPanel.BORDER_C;
import static com.saia.presentation.home.PersonalSeguridadPanel.CARD_BG;
import static com.saia.presentation.home.PersonalSeguridadPanel.GREEN;
import static com.saia.presentation.home.PersonalSeguridadPanel.NAVY;
import static com.saia.presentation.home.PersonalSeguridadPanel.TEXT_DARK;

/**
 * Sub-panel LISTA: tabla de guardias con columnas separadas para Editar y Bloquear.
 *
 * Columnas: # | N°Doc | TipDoc | Nombres | Apellidos | Email | Tel | Turno | Empresa | Estado | Editar | Bloqueo
 */
class ListaPanel extends JPanel {

    private static final String[] COLS = {
        "#", "N° Documento", "Tipo Doc", "Nombres", "Apellidos",
        "Email", "Teléfono", "Turno", "Empresa", "Estado", "Editar", "Bloqueo"
    };

    private final PersonalSeguridadPanel    parent;
    private final DefaultTableModel         tableModel;
    private final JTable                    table;
    private       List<PersonalSeguridad>   todos = new ArrayList<>();
    private       JTextField                txtBuscar;

    ListaPanel(PersonalSeguridadPanel parent) {
        this.parent     = parent;
        this.tableModel = buildModel();
        this.table      = buildTable();
        setLayout(new BorderLayout());
        setBackground(BG_PAGE);
        setBorder(new EmptyBorder(20, 24, 20, 24));
        buildUI();
    }

    private void buildUI() {
        add(buildToolbar(),   BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 0, 14, 0));

        JLabel titulo = new JLabel("\uD83D\uDC6E  Personal de Seguridad");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titulo.setForeground(TEXT_DARK);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        txtBuscar = new JTextField();
        txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtBuscar.setBorder(new LineBorder(BORDER_C, 1, true));
        txtBuscar.setPreferredSize(new Dimension(190, 34));
        txtBuscar.setForeground(new Color(0xAAAAAA));
        txtBuscar.setText("Buscar por N° doc...");

        ((AbstractDocument) txtBuscar.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void insertString(FilterBypass fb, int off, String s, AttributeSet a)
                    throws BadLocationException {
                if (s != null && s.matches("\\d+")) super.insertString(fb, off, s, a);
            }
            @Override public void replace(FilterBypass fb, int off, int len, String s, AttributeSet a)
                    throws BadLocationException {
                if (s != null && s.matches("\\d*")) super.replace(fb, off, len, s, a);
            }
        });

        txtBuscar.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if ("Buscar por N° doc...".equals(txtBuscar.getText())) {
                    txtBuscar.setText(""); txtBuscar.setForeground(new Color(0x333333));
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (txtBuscar.getText().isEmpty()) {
                    txtBuscar.setText("Buscar por N° doc...");
                    txtBuscar.setForeground(new Color(0xAAAAAA));
                    poblarTabla(todos);
                }
            }
        });
        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate (javax.swing.event.DocumentEvent e) { doFilter(); }
            @Override public void removeUpdate (javax.swing.event.DocumentEvent e) { doFilter(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { doFilter(); }
            void doFilter() {
                String t = txtBuscar.getText();
                filtrar("Buscar por N° doc...".equals(t) ? "" : t);
            }
        });

        JButton btnNuevo   = makeBtn("\u002B  Nuevo",    GREEN, Color.WHITE, 120);
        JButton btnRefresh = makeBtn("\u21BB  Actualizar", NAVY, Color.WHITE, 120);
        btnNuevo.addActionListener(e -> parent.mostrar("REGISTRO"));
        btnRefresh.addActionListener(e -> cargarDatos());

        right.add(txtBuscar); right.add(btnNuevo); right.add(btnRefresh);
        bar.add(titulo, BorderLayout.WEST);
        bar.add(right,  BorderLayout.EAST);
        return bar;
    }

    // ── Tabla ─────────────────────────────────────────────────────────────────

    private JPanel buildTableCard() {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 14));
                g2.fill(new RoundRectangle2D.Float(2, 3, getWidth()-3, getHeight()-3, 10, 10));
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-2, getHeight()-2, 10, 10));
                g2.dispose(); super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };

        JTableHeader header = table.getTableHeader();
        header.setBackground(NAVY); header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setPreferredSize(new Dimension(0, 36));
        header.setReorderingAllowed(false);

        table.setBackground(CARD_BG);    table.setForeground(TEXT_DARK);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(42);
        table.setShowHorizontalLines(true); table.setGridColor(new Color(0xEEEEEE));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(0xE8F0FB));
        table.setFocusable(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(CARD_BG);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        card.add(header, BorderLayout.NORTH);
        card.add(scroll,  BorderLayout.CENTER);
        return card;
    }

    private DefaultTableModel buildModel() {
        return new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 10 || c == 11; }
            @Override public Class<?> getColumnClass(int c) {
                return c == 0 ? Integer.class : Object.class;
            }
        };
    }

    private JTable buildTable() {
        JTable t = new JTable(tableModel);

        // Col 9 — Estado (badge)
        t.getColumnModel().getColumn(9).setCellRenderer(new EstadoRenderer());

        // Col 10 — Editar
        t.getColumnModel().getColumn(10).setCellRenderer(new EditarRenderer());
        t.getColumnModel().getColumn(10).setCellEditor(new EditarEditor(parent));

        // Col 11 — Bloqueo / Habilitar
        t.getColumnModel().getColumn(11).setCellRenderer(new BloqueoRenderer());
        t.getColumnModel().getColumn(11).setCellEditor(new BloqueoEditor(parent, this));

        // Anchos
        int[] w = {35, 105, 110, 130, 120, 170, 95, 80, 130, 75, 70, 85};
        for (int i = 0; i < w.length; i++)
            t.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        t.getColumnModel().getColumn(0).setMaxWidth(35);
        return t;
    }

    // ── Carga y filtrado ──────────────────────────────────────────────────────

    void cargarDatos() {
        SwingWorker<List<PersonalSeguridad>, Void> w = new SwingWorker<>() {
            @Override protected List<PersonalSeguridad> doInBackground() {
                return parent.getService().listarTodos();
            }
            @Override protected void done() {
                try {
                    todos = get(); poblarTabla(todos);
                } catch (InterruptedException | ExecutionException ex) {
                    JOptionPane.showMessageDialog(ListaPanel.this,
                        "Error cargando datos: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    Thread.currentThread().interrupt();
                }
            }
        };
        w.execute();
    }

    private void filtrar(String q) {
        if (q.isEmpty()) { poblarTabla(todos); return; }
        List<PersonalSeguridad> r = new ArrayList<>();
        for (PersonalSeguridad g : todos)
            if (String.valueOf(g.getNumDoc()).startsWith(q)) r.add(g);
        poblarTabla(r);
    }

    private void poblarTabla(List<PersonalSeguridad> lista) {
        tableModel.setRowCount(0);
        if (lista.isEmpty()) {
            tableModel.addRow(new Object[]{"", "Sin registros", "", "", "", "", "", "", "", "", "", ""});
            return;
        }
        int i = 1;
        for (PersonalSeguridad g : lista) {
            Persona p = g.getPersona();
            tableModel.addRow(new Object[]{
                i++,
                g.getNumDoc(),
                p != null ? nvl(p.getTipDoc())   : "",
                p != null ? nvl(p.getNombres())  : "",
                p != null ? apellidos(p)         : "",
                p != null ? nvl(p.getEmail())    : "",
                p != null ? nvl(p.getTel(), "—") : "—",
                nvl(g.getTurno(),      "—"),
                nvl(g.getEmpresaSeg(), "—"),
                g.isCuentaActiva() ? "Activa" : "Inactiva",
                g,  // col 10 Editar
                g   // col 11 Bloqueo
            });
        }
    }

    // ── Helpers estáticos ─────────────────────────────────────────────────────

    private static String nvl(String v)           { return v != null ? v : ""; }
    private static String nvl(String v, String d) { return (v != null && !v.isBlank()) ? v : d; }
    private static String apellidos(Persona p) {
        String a = nvl(p.getPApe());
        if (p.getSApe() != null && !p.getSApe().isBlank()) a += " " + p.getSApe();
        return a.trim();
    }

    private static JButton makeBtn(String text, Color bg, Color fg, int w) {
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
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 8, 8));
                g2.dispose(); super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(fg); btn.setOpaque(false);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(w, 34));
        return btn;
    }

    // ── Renderer Estado ───────────────────────────────────────────────────────
    private static class EstadoRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setFont(new Font("Segoe UI", Font.BOLD, 11));
            l.setOpaque(true);
            if ("Activa".equals(v)) {
                l.setForeground(new Color(0x1B5E20)); l.setBackground(new Color(0xE8F5E9));
            } else {
                l.setForeground(new Color(0xB71C1C)); l.setBackground(new Color(0xFFEBEE));
            }
            return l;
        }
    }

    // ── Columna EDITAR ────────────────────────────────────────────────────────
    private static class EditarRenderer implements TableCellRenderer {
        private final JButton btn = makeStaticBtn("\u270E  Editar", NAVY);
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int r, int c) { return btn; }
    }

    private static class EditarEditor extends DefaultCellEditor {
        private final PersonalSeguridadPanel panelRef;
        private PersonalSeguridad g;
        private final JButton btn;

        EditarEditor(PersonalSeguridadPanel p) {
            super(new JCheckBox());
            panelRef = p;
            btn = makeStaticBtn("\u270E  Editar", NAVY);
            btn.addActionListener(e -> { fireEditingStopped(); if (g != null) panelRef.abrirEditar(g); });
        }

        @Override public Component getTableCellEditorComponent(JTable t, Object v,
                boolean sel, int r, int c) {
            g = (v instanceof PersonalSeguridad) ? (PersonalSeguridad) v : null; return btn;
        }
        @Override public Object getCellEditorValue() { return g; }
    }

    // ── Columna BLOQUEO ───────────────────────────────────────────────────────
    private static class BloqueoRenderer implements TableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int r, int c) {
            boolean activa = !(v instanceof PersonalSeguridad) || ((PersonalSeguridad) v).isCuentaActiva();
            return makeStaticBtn(
                activa ? "\uD83D\uDD12 Bloquear" : "\u2705 Habilitar",
                activa ? new Color(0xC62828) : new Color(0x2E7D32));
        }
    }

    private class BloqueoEditor extends DefaultCellEditor {
        private final PersonalSeguridadPanel panelRef;
        private final ListaPanel             listaRef;
        private PersonalSeguridad            g;
        private final JButton                btn;

        BloqueoEditor(PersonalSeguridadPanel p, ListaPanel lista) {
            super(new JCheckBox());
            panelRef = p; listaRef = lista;
            btn = new JButton();
            btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btn.setForeground(Color.WHITE); btn.setOpaque(true);
            btn.setBorderPainted(false); btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                fireEditingStopped();
                if (g == null) return;
                boolean bloquear = g.isCuentaActiva();
                String msg = (bloquear ? "¿Bloquear a " : "¿Habilitar a ") + g.getNombreCompleto() + "?";
                int r2 = JOptionPane.showConfirmDialog(panelRef, msg,
                        bloquear ? "Bloquear" : "Habilitar",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (r2 != JOptionPane.YES_OPTION) return;
                final PersonalSeguridad gFinal = g;
                SwingWorker<PersonalSeguridadService.BloqueoResult, Void> w = new SwingWorker<>() {
                    @Override protected PersonalSeguridadService.BloqueoResult doInBackground() {
                        return bloquear
                                ? panelRef.getService().bloquear(gFinal.getNumDoc())
                                : panelRef.getService().desbloquear(gFinal.getNumDoc());
                    }
                    @Override protected void done() {
                        try {
                            if (get() == PersonalSeguridadService.BloqueoResult.OK) {
                                JOptionPane.showMessageDialog(panelRef,
                                        bloquear ? "Cuenta bloqueada." : "Cuenta habilitada.",
                                        "OK", JOptionPane.INFORMATION_MESSAGE);
                                listaRef.cargarDatos();
                            } else {
                                JOptionPane.showMessageDialog(panelRef,
                                        "Error al cambiar estado.", "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (InterruptedException | java.util.concurrent.ExecutionException ex) {
                            JOptionPane.showMessageDialog(panelRef, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            Thread.currentThread().interrupt();
                        }
                    }
                };
                w.execute();
            });
        }

        @Override public Component getTableCellEditorComponent(JTable t, Object v,
                boolean sel, int r, int c) {
            g = (v instanceof PersonalSeguridad) ? (PersonalSeguridad) v : null;
            boolean activa = g == null || g.isCuentaActiva();
            btn.setText(activa ? "\uD83D\uDD12 Bloquear" : "\u2705 Habilitar");
            btn.setBackground(activa ? new Color(0xC62828) : new Color(0x2E7D32));
            return btn;
        }
        @Override public Object getCellEditorValue() { return g; }
    }

    // ── Helper de botones de tabla ────────────────────────────────────────────
    private static JButton makeStaticBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 11));
        b.setForeground(Color.WHITE); b.setBackground(bg);
        b.setOpaque(true); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
