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
import javax.swing.JComboBox;
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

import com.saia.business.BloqueoService;
import com.saia.business.BloqueoService.HabilitarResult;
import com.saia.data.UsuarioBloqueadoDAO.UsuarioBloqueado;

/**
 * Panel "Usuarios Bloqueados".
 * Muestra todos los usuarios bloqueados (aprendices + personal de seguridad).
 * Permite buscar por N° documento o filtrar por rol.
 * Botón "Habilitar" para restaurar el acceso.
 */
public class BloqueoPanel extends JPanel {

    // ── Colores ───────────────────────────────────────────────────────────────
    private static final Color NAVY     = new Color(0x1A3A5C);
    private static final Color GREEN    = new Color(0x2E7D32);
    private static final Color BG_PAGE  = new Color(0xF5F6FA);
    private static final Color CARD_BG  = Color.WHITE;
    private static final Color BORDER_C = new Color(0xE8E8E8);
    private static final Color TEXT_DARK = new Color(0x1A1A2E);

    private static final String[] COLS = {
        "#", "N° Documento", "Tipo Doc", "Nombres", "Apellidos",
        "Correo", "Teléfono", "Rol", "Habilitar"
    };

    private static final String FILTRO_TODOS     = "Todos";
    private static final String FILTRO_PERSONAL  = "Personal de Seguridad";
    private static final String FILTRO_APRENDIZ  = "Aprendiz";

    private final BloqueoService    service    = new BloqueoService();
    private final DefaultTableModel tableModel = buildModel();
    private final JTable            table      = buildTable();
    private       List<UsuarioBloqueado> todos = new ArrayList<>();

    private JTextField        txtBuscar;
    private JComboBox<String> cmbRol;

    public BloqueoPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_PAGE);
        setBorder(new EmptyBorder(20, 24, 20, 24));
        buildUI();
    }

    @Override public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::cargarDatos);
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    private void buildUI() {
        add(buildToolbar(),   BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 0, 14, 0));

        JLabel titulo = new JLabel("\uD83D\uDD12  Usuarios Bloqueados");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titulo.setForeground(TEXT_DARK);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        // Buscador por N° documento
        txtBuscar = new JTextField();
        txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtBuscar.setBorder(new LineBorder(BORDER_C, 1, true));
        txtBuscar.setPreferredSize(new Dimension(190, 34));
        txtBuscar.setForeground(new Color(0xAAAAAA));
        txtBuscar.setText("N° documento...");
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
                if ("N° documento...".equals(txtBuscar.getText())) {
                    txtBuscar.setText(""); txtBuscar.setForeground(new Color(0x333333));
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (txtBuscar.getText().isEmpty()) {
                    txtBuscar.setText("N° documento...");
                    txtBuscar.setForeground(new Color(0xAAAAAA));
                }
                filtrar();
            }
        });
        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate (javax.swing.event.DocumentEvent e) { filtrar(); }
            @Override public void removeUpdate (javax.swing.event.DocumentEvent e) { filtrar(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
        });

        // Filtro por rol
        cmbRol = new JComboBox<>(new String[]{FILTRO_TODOS, FILTRO_PERSONAL, FILTRO_APRENDIZ});
        cmbRol.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbRol.setBackground(Color.WHITE);
        cmbRol.setPreferredSize(new Dimension(190, 34));
        cmbRol.addActionListener(e -> filtrar());

        JButton btnRefresh = makeBtn("\u21BB  Actualizar", NAVY, Color.WHITE, 120);
        btnRefresh.addActionListener(e -> cargarDatos());

        right.add(new JLabel("Doc:") {{ setFont(new Font("Segoe UI", Font.BOLD, 12)); setForeground(TEXT_DARK); }});
        right.add(txtBuscar);
        right.add(new JLabel("Rol:") {{ setFont(new Font("Segoe UI", Font.BOLD, 12)); setForeground(TEXT_DARK); }});
        right.add(cmbRol);
        right.add(btnRefresh);

        bar.add(titulo, BorderLayout.WEST);
        bar.add(right,  BorderLayout.EAST);
        return bar;
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
        header.setBackground(NAVY); header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setPreferredSize(new Dimension(0, 36));
        header.setReorderingAllowed(false);

        table.setBackground(CARD_BG); table.setForeground(TEXT_DARK);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(42);
        table.setShowHorizontalLines(true); table.setGridColor(new Color(0xEEEEEE));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(0xE8F0FB));
        table.setFocusable(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null); scroll.getViewport().setBackground(CARD_BG);

        card.add(header, BorderLayout.NORTH);
        card.add(scroll,  BorderLayout.CENTER);
        return card;
    }

    private DefaultTableModel buildModel() {
        return new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 8; }
            @Override public Class<?> getColumnClass(int c) {
                return c == 0 ? Integer.class : Object.class;
            }
        };
    }

    private JTable buildTable() {
        JTable t = new JTable(tableModel);
        // Columna Rol con badge de color
        t.getColumnModel().getColumn(7).setCellRenderer(new RolRenderer());
        // Columna Habilitar
        t.getColumnModel().getColumn(8).setCellRenderer(new HabilitarRenderer());
        t.getColumnModel().getColumn(8).setCellEditor(new HabilitarEditor(this));
        // Anchos
        int[] w = {35, 110, 115, 150, 145, 185, 100, 145, 100};
        for (int i = 0; i < w.length; i++)
            t.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        t.getColumnModel().getColumn(0).setMaxWidth(35);
        return t;
    }

    // ── Carga y filtrado ──────────────────────────────────────────────────────

    void cargarDatos() {
        tableModel.setRowCount(0);
        tableModel.addRow(new Object[]{"", "Cargando...", "", "", "", "", "", "", ""});

        SwingWorker<List<UsuarioBloqueado>, Void> w = new SwingWorker<>() {
            @Override protected List<UsuarioBloqueado> doInBackground() {
                return service.listarBloqueados();
            }
            @Override protected void done() {
                try { todos = get(); filtrar(); }
                catch (InterruptedException | ExecutionException ex) {
                    tableModel.setRowCount(0);
                    tableModel.addRow(new Object[]{"", "Error al cargar", "", "", "", "", "", "", ""});
                    Thread.currentThread().interrupt();
                }
            }
        };
        w.execute();
    }

    private void filtrar() {
        String doc = getDocQuery();
        String rol = (String) cmbRol.getSelectedItem();

        List<UsuarioBloqueado> filtrados = new ArrayList<>();
        for (UsuarioBloqueado u : todos) {
            boolean matchDoc = doc.isEmpty() || String.valueOf(u.numDoc).startsWith(doc);
            boolean matchRol = FILTRO_TODOS.equals(rol) || rol.equals(u.rol);
            if (matchDoc && matchRol) filtrados.add(u);
        }
        poblarTabla(filtrados);
    }

    private String getDocQuery() {
        String t = txtBuscar.getText().trim();
        return "N° documento...".equals(t) ? "" : t;
    }

    private void poblarTabla(List<UsuarioBloqueado> lista) {
        tableModel.setRowCount(0);
        if (lista.isEmpty()) {
            tableModel.addRow(new Object[]{"", "No hay usuarios bloqueados", "", "", "", "", "", "", ""});
            return;
        }
        int i = 1;
        for (UsuarioBloqueado u : lista) {
            String apels = u.pApe + (u.sApe != null && !u.sApe.isBlank() ? " " + u.sApe : "");
            tableModel.addRow(new Object[]{
                i++,
                u.numDoc,
                nvl(u.tipDoc),
                nvl(u.nombres),
                apels.trim(),
                nvl(u.email),
                nvl(u.tel, "—"),
                u.rol,
                u   // objeto completo para el editor
            });
        }
    }

    // ── Acción habilitar ──────────────────────────────────────────────────────

    void habilitar(UsuarioBloqueado u) {
        int r = JOptionPane.showConfirmDialog(this,
                "<html>¿Habilitar la cuenta de <b>" + u.getNombreCompleto() + "</b>?</html>",
                "Habilitar usuario", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;

        SwingWorker<HabilitarResult, Void> w = new SwingWorker<>() {
            @Override protected HabilitarResult doInBackground() {
                return service.habilitar(u.numDoc, u.rol);
            }
            @Override protected void done() {
                try {
                    HabilitarResult res = get();
                    if (res == HabilitarResult.OK) {
                        JOptionPane.showMessageDialog(BloqueoPanel.this,
                                "<html>La cuenta de <b>" + u.getNombreCompleto()
                                + "</b> fue habilitada correctamente.</html>",
                                "Habilitado", JOptionPane.INFORMATION_MESSAGE);
                        cargarDatos();
                    } else {
                        JOptionPane.showMessageDialog(BloqueoPanel.this,
                                "Error al habilitar. Intente nuevamente.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    JOptionPane.showMessageDialog(BloqueoPanel.this,
                            ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    Thread.currentThread().interrupt();
                }
            }
        };
        w.execute();
    }

    // ── Renderers y editores ──────────────────────────────────────────────────

    private static class RolRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setFont(new Font("Segoe UI", Font.BOLD, 11));
            l.setOpaque(true);
            if (FILTRO_PERSONAL.equals(v)) {
                l.setForeground(new Color(0x0D47A1)); l.setBackground(new Color(0xE3F2FD));
            } else if (FILTRO_APRENDIZ.equals(v)) {
                l.setForeground(new Color(0x4A148C)); l.setBackground(new Color(0xF3E5F5));
            } else {
                l.setForeground(TEXT_DARK); l.setBackground(Color.WHITE);
            }
            return l;
        }
    }

    private static class HabilitarRenderer implements TableCellRenderer {
        private final JButton btn = makeStaticBtn("\u2705  Habilitar", GREEN);
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int r, int c) { return btn; }
    }

    private static class HabilitarEditor extends DefaultCellEditor {
        private final BloqueoPanel panelRef;
        private UsuarioBloqueado   current;
        private final JButton      btn;

        HabilitarEditor(BloqueoPanel p) {
            super(new JCheckBox());
            panelRef = p;
            btn = makeStaticBtn("\u2705  Habilitar", GREEN);
            btn.addActionListener(e -> {
                fireEditingStopped();
                if (current != null) panelRef.habilitar(current);
            });
        }

        @Override public Component getTableCellEditorComponent(JTable t, Object v,
                boolean sel, int r, int c) {
            current = (v instanceof UsuarioBloqueado) ? (UsuarioBloqueado) v : null; return btn;
        }
        @Override public Object getCellEditorValue() { return current; }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String nvl(String v)           { return v != null ? v : ""; }
    private static String nvl(String v, String d) { return (v != null && !v.isBlank()) ? v : d; }

    private static JButton makeStaticBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 11));
        b.setForeground(Color.WHITE); b.setBackground(bg);
        b.setOpaque(true); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
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
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,8,8));
                g2.dispose(); super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12)); btn.setForeground(fg);
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(w, 34));
        return btn;
    }
}
