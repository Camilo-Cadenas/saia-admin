package com.saia.presentation.home;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

import com.saia.data.EstadisticasDAO;
import com.saia.data.EstadisticasDAO.ColorRGB;
import com.saia.data.EstadisticasDAO.FranjaHoraria;
import com.saia.data.EstadisticasDAO.InfoPeriodo;
import com.saia.data.EstadisticasDAO.PuntoDia;
import com.saia.util.SessionManager;

/**
 * Panel de Estadísticas de Ingresos/Salidas — diseño dashboard moderno.
 */
public class EstadisticasPanel extends JPanel {

    private static final java.awt.Color BG_PAGE   = new java.awt.Color(0xF5F6FA);
    private static final java.awt.Color CARD_BG   = java.awt.Color.WHITE;
    private static final java.awt.Color BORDER_C  = new java.awt.Color(0xE8ECF1);
    private static final java.awt.Color TEXT_DARK = new java.awt.Color(0x1A1A2E);
    private static final java.awt.Color TEXT_GRAY = new java.awt.Color(0x5A6474);
    private static final java.awt.Color GREEN     = new java.awt.Color(0x2E7D32);
    private static final java.awt.Color BLUE      = new java.awt.Color(0x1565C0);
    private static final java.awt.Color ORANGE    = new java.awt.Color(0xE65100);
    private static final java.awt.Color PURPLE    = new java.awt.Color(0x6A1B9A);

    private static final DateTimeFormatter FMT_D  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_TS = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final EstadisticasDAO dao = new EstadisticasDAO();

    private JComboBox<String> cmbTipo;
    private JSpinner          spDia;
    private JComboBox<String> cmbMes, cmbAnio;

    private JLabel lblKpiTotal, lblKpiProm, lblKpiHora, lblKpiValidos;
    private JLabel lblKpiTotalSub, lblKpiPromSub;

    private GraficoBarrasLinea grafico;
    private GraficoDona        dona;
    private JPanel             leyendaPanel;

    private JLabel lblInfoPeriodo, lblInfoDias, lblInfoMax, lblInfoMin;
    private JLabel lblResTotalMes, lblResProm, lblResHora, lblResValidos;
    private JLabel lblFooterUpdate;

    private LocalDate desdeActual, hastaActual;

    public EstadisticasPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_PAGE);
        setBorder(new EmptyBorder(14, 18, 10, 18));
        buildUI();
    }

    @Override public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::cargarMesActual);
    }

    private void buildUI() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(BG_PAGE);
        root.add(buildFiltros());
        root.add(Box.createVerticalStrut(10));
        root.add(buildKpiRow());
        root.add(Box.createVerticalStrut(10));
        root.add(buildGrafico());
        root.add(Box.createVerticalStrut(10));
        root.add(buildBottom());

        JScrollPane scroll = new JScrollPane(root);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_PAGE);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    // ── Filtros ───────────────────────────────────────────────────────────────
    private JPanel buildFiltros() {
        JPanel p = card(8); p.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        cmbTipo = new JComboBox<>(new String[]{"Día","Mes","Año"});
        cmbTipo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbTipo.setPreferredSize(new Dimension(85, 30));
        p.add(new JLabel("Ver:") {{ setFont(new Font("Segoe UI", Font.BOLD, 12)); }});
        p.add(cmbTipo);

        spDia = new JSpinner(new SpinnerDateModel());
        spDia.setEditor(new JSpinner.DateEditor(spDia, "dd/MM/yyyy"));
        spDia.setPreferredSize(new Dimension(130, 30));
        ((SpinnerDateModel)spDia.getModel()).setValue(toJavaDate(LocalDate.now()));
        p.add(spDia);

        String[] ms={"Enero","Febrero","Marzo","Abril","Mayo","Junio","Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};
        cmbMes = new JComboBox<>(ms); cmbMes.setSelectedIndex(LocalDate.now().getMonthValue()-1);
        cmbMes.setPreferredSize(new Dimension(115,30)); cmbMes.setVisible(false); p.add(cmbMes);

        int ay=LocalDate.now().getYear(); String[] ay6=new String[6];
        for (int i=0;i<6;i++) ay6[i]=String.valueOf(ay-i);
        cmbAnio = new JComboBox<>(ay6); cmbAnio.setPreferredSize(new Dimension(80,30));
        cmbAnio.setVisible(false); p.add(cmbAnio);

        cmbTipo.addActionListener(e -> {
            String t=(String)cmbTipo.getSelectedItem();
            spDia.setVisible("Día".equals(t)); cmbMes.setVisible("Mes".equals(t));
            cmbAnio.setVisible("Mes".equals(t)||"Año".equals(t));
            p.revalidate(); p.repaint();
        });
        JButton ok  = miniBtn("Cargar",   BLUE,     java.awt.Color.WHITE);
        JButton tod = miniBtn("Ver todo", TEXT_GRAY, java.awt.Color.WHITE);
        ok.addActionListener(e -> buscar());
        tod.addActionListener(e -> { desdeActual=LocalDate.now().minusYears(5); hastaActual=LocalDate.now(); cargar(); });
        p.add(ok); p.add(tod);
        return p;
    }

    // ── KPI ───────────────────────────────────────────────────────────────────
    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new GridLayout(1,4,10,0));
        row.setOpaque(false); row.setMaximumSize(new Dimension(Integer.MAX_VALUE,100));

        JPanel k1=kpiCard(new java.awt.Color(0x1565C0),new java.awt.Color(0xE3F2FD),"👥");
        lblKpiTotal=kpiNum(k1,"—","Total de ingresos"); lblKpiTotalSub=kpiSub(k1,"Ingresos en el período");

        JPanel k2=kpiCard(new java.awt.Color(0x2E7D32),new java.awt.Color(0xE8F5E9),"📈");
        lblKpiProm=kpiNum(k2,"—","Promedio diario"); lblKpiPromSub=kpiSub(k2,"Por día activo");

        JPanel k3=kpiCard(new java.awt.Color(0x6A1B9A),new java.awt.Color(0xF3E5F5),"🕐");
        lblKpiHora=kpiNum(k3,"—","Hora pico promedio"); kpiSub(k3,"Horario de mayor flujo");

        JPanel k4=kpiCard(new java.awt.Color(0xE65100),new java.awt.Color(0xFFF3E0),"🛡");
        lblKpiValidos=kpiNum(k4,"100%","Registros válidos"); kpiSub(k4,"Sin inconsistencias");

        row.add(k1); row.add(k2); row.add(k3); row.add(k4);
        return row;
    }

    private JPanel kpiCard(java.awt.Color accent, java.awt.Color bg, String ico) {
        JPanel p=new JPanel(new BorderLayout(10,0));
        p.setBackground(CARD_BG);
        p.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_C,1,true),new EmptyBorder(12,14,12,14)));
        JPanel icoP=new JPanel(){ @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg); g2.fillOval(0,0,46,46);
            g2.setFont(new Font("Segoe UI Emoji",Font.PLAIN,20));
            FontMetrics fm=g2.getFontMetrics();
            g2.setColor(accent); g2.drawString(ico,(46-fm.stringWidth(ico))/2,30);
            g2.dispose();
        }};
        icoP.setOpaque(false); icoP.setPreferredSize(new Dimension(46,46));
        JPanel txt=new JPanel(); txt.setLayout(new BoxLayout(txt,BoxLayout.Y_AXIS)); txt.setOpaque(false);
        p.add(icoP,BorderLayout.WEST); p.add(txt,BorderLayout.CENTER);
        p.putClientProperty("txt",txt);
        return p;
    }

    private JLabel kpiNum(JPanel c,String v,String l){
        JPanel txt=(JPanel)c.getClientProperty("txt");
        JLabel n=new JLabel(v); n.setFont(new Font("Segoe UI",Font.BOLD,24)); n.setForeground(TEXT_DARK);
        JLabel lb=new JLabel(l); lb.setFont(new Font("Segoe UI",Font.PLAIN,11)); lb.setForeground(TEXT_GRAY);
        txt.add(n); txt.add(lb); return n;
    }
    private JLabel kpiSub(JPanel c,String s){
        JPanel txt=(JPanel)c.getClientProperty("txt");
        JLabel l=new JLabel(s); l.setFont(new Font("Segoe UI",Font.PLAIN,10)); l.setForeground(GREEN);
        txt.add(l); return l;
    }

    // ── Gráfico ───────────────────────────────────────────────────────────────
    private JPanel buildGrafico() {
        JPanel card=card(8); card.setLayout(new BorderLayout(0,6));
        card.setBorder(new EmptyBorder(14,16,14,16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE,300));
        JLabel t=new JLabel("Ingresos totales por día");
        t.setFont(new Font("Segoe UI",Font.BOLD,14)); t.setForeground(TEXT_DARK);
        grafico=new GraficoBarrasLinea();
        card.add(t,BorderLayout.NORTH); card.add(grafico,BorderLayout.CENTER);
        return card;
    }

    // ── Fila inferior ─────────────────────────────────────────────────────────
    private JPanel buildBottom() {
        JPanel row=new JPanel(new GridLayout(1,3,10,0));
        row.setOpaque(false); row.setMaximumSize(new Dimension(Integer.MAX_VALUE,220));
        row.add(buildDonaCard()); row.add(buildInfoCard()); row.add(buildResCard());
        return row;
    }

    private JPanel buildDonaCard() {
        JPanel card=card(8); card.setLayout(new BorderLayout(10,0));
        card.setBorder(new EmptyBorder(14,14,14,14));
        JLabel t=new JLabel("Distribución por franja horaria");
        t.setFont(new Font("Segoe UI",Font.BOLD,12)); t.setForeground(TEXT_DARK);
        dona=new GraficoDona(); dona.setPreferredSize(new Dimension(120,120));
        leyendaPanel=new JPanel(); leyendaPanel.setLayout(new BoxLayout(leyendaPanel,BoxLayout.Y_AXIS)); leyendaPanel.setOpaque(false);
        JPanel body=new JPanel(new BorderLayout(10,0)); body.setOpaque(false);
        body.add(dona,BorderLayout.WEST); body.add(leyendaPanel,BorderLayout.CENTER);
        card.add(t,BorderLayout.NORTH); card.add(body,BorderLayout.CENTER);
        return card;
    }

    private JPanel buildInfoCard() {
        JPanel card=card(8); card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14,14,14,14));
        JLabel t=new JLabel("Información del mes");
        t.setFont(new Font("Segoe UI",Font.BOLD,12)); t.setForeground(TEXT_DARK); t.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(t); card.add(Box.createVerticalStrut(10));
        lblInfoPeriodo=infoFila(card,"📅","Período seleccionado","—"); card.add(Box.createVerticalStrut(6));
        lblInfoDias   =infoFila(card,"👥","Días del período","—");     card.add(Box.createVerticalStrut(6));
        lblInfoMax    =infoFila(card,"📈","Día con más ingresos","—");  card.add(Box.createVerticalStrut(6));
        lblInfoMin    =infoFila(card,"📉","Día con menos ingresos","—");
        return card;
    }

    private JPanel buildResCard() {
        JPanel card=card(8); card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14,14,14,14));
        JLabel t=new JLabel("Resumen rápido");
        t.setFont(new Font("Segoe UI",Font.BOLD,12)); t.setForeground(TEXT_DARK); t.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(t); card.add(Box.createVerticalStrut(10));
        lblResTotalMes=resRow(card,"👥","Total de ingresos en el mes",BLUE);  card.add(Box.createVerticalStrut(6));
        lblResProm    =resRow(card,"📈","Promedio diario",GREEN);              card.add(Box.createVerticalStrut(6));
        lblResHora    =resRow(card,"🕐","Hora pico promedio",PURPLE);          card.add(Box.createVerticalStrut(6));
        lblResValidos =resRow(card,"🛡","Registros válidos",ORANGE);
        return card;
    }

    private JPanel buildFooter() {
        JPanel p=new JPanel(new BorderLayout());
        p.setBackground(java.awt.Color.WHITE); p.setBorder(new EmptyBorder(6,18,6,18));
        JLabel u=new JLabel("SAIA — Módulo de Estadísticas | "+SessionManager.getInstance().getAdminNombre());
        u.setFont(new Font("Segoe UI",Font.PLAIN,11)); u.setForeground(TEXT_GRAY);
        lblFooterUpdate=new JLabel("Última actualización: —");
        lblFooterUpdate.setFont(new Font("Segoe UI",Font.PLAIN,11)); lblFooterUpdate.setForeground(TEXT_GRAY);
        p.add(u,BorderLayout.WEST); p.add(lblFooterUpdate,BorderLayout.EAST);
        return p;
    }

    // ── Lógica ────────────────────────────────────────────────────────────────
    private void cargarMesActual() {
        desdeActual=LocalDate.now().withDayOfMonth(1); hastaActual=LocalDate.now();
        cmbTipo.setSelectedItem("Mes"); cmbMes.setSelectedIndex(LocalDate.now().getMonthValue()-1);
        cmbAnio.setSelectedItem(String.valueOf(LocalDate.now().getYear()));
        cmbMes.setVisible(true); cmbAnio.setVisible(true); spDia.setVisible(false);
        cargar();
    }

    private void buscar() {
        String tipo=(String)cmbTipo.getSelectedItem();
        switch (tipo) {
            case "Día" -> { java.util.Date d=(java.util.Date)spDia.getValue(); desdeActual=hastaActual=d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate(); }
            case "Mes" -> { int m=cmbMes.getSelectedIndex()+1; int a=Integer.parseInt((String)cmbAnio.getSelectedItem()); desdeActual=LocalDate.of(a,m,1); hastaActual=desdeActual.withDayOfMonth(desdeActual.lengthOfMonth()); }
            case "Año" -> { int a=Integer.parseInt((String)cmbAnio.getSelectedItem()); desdeActual=LocalDate.of(a,1,1); hastaActual=LocalDate.of(a,12,31); }
        }
        cargar();
    }

    private void cargar() {
        final LocalDate d=desdeActual, h=hastaActual;
        new SwingWorker<Object[],Void>() {
            @Override protected Object[] doInBackground() {
                return new Object[]{dao.totalIngresos(d,h),dao.promedioDiario(d,h),dao.horaPico(d,h),
                        dao.ingresosPorDia(d,h),dao.distribucionFranja(d,h),dao.infoPeriodo(d,h)};
            }
            @Override protected void done() {
                try {
                    Object[] r=get();
                    int total=(int)r[0], prom=(int)r[1]; String hora=(String)r[2];
                    @SuppressWarnings("unchecked") List<PuntoDia> dias=(List<PuntoDia>)r[3];
                    @SuppressWarnings("unchecked") List<FranjaHoraria> fr=(List<FranjaHoraria>)r[4];
                    InfoPeriodo info=(InfoPeriodo)r[5];

                    lblKpiTotal.setText(String.format("%,d",total).replace(',','.'));
                    lblKpiProm.setText(String.valueOf(prom));
                    lblKpiHora.setText(hora);
                    lblKpiValidos.setText("100%");
                    lblKpiTotalSub.setText("Ingresos en el período");
                    lblKpiPromSub.setText("Por día activo");

                    grafico.setData(dias);

                    int totFr=fr.stream().mapToInt(f->f.total).sum();
                    dona.setData(fr,totFr);
                    actualizarLeyenda(fr,totFr);

                    lblInfoPeriodo.setText(d.format(FMT_D)+" - "+h.format(FMT_D));
                    lblInfoDias.setText(info.diasTotales+" días");
                    lblInfoMax.setText(info.diaMasIngresos+" ("+info.maxIngresos+" ingresos)");
                    lblInfoMin.setText(info.diaMenosIngresos+" ("+info.minIngresos+" ingresos)");

                    lblResTotalMes.setText(String.format("%,d",total).replace(',','.'));
                    lblResProm.setText(String.valueOf(prom));
                    lblResHora.setText(hora);
                    lblResValidos.setText("100%");

                    lblFooterUpdate.setText("Última actualización: "+LocalDateTime.now().format(FMT_TS));
                } catch (InterruptedException|ExecutionException ex){ Thread.currentThread().interrupt(); }
            }
        }.execute();
    }

    private void actualizarLeyenda(List<FranjaHoraria> fr, int total) {
        leyendaPanel.removeAll();
        for (FranjaHoraria f:fr) {
            int pct=total>0?(int)(f.total*100.0/total):0;
            JPanel row=new JPanel(new BorderLayout(4,0)); row.setOpaque(false); row.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
            final ColorRGB cr=f.color;
            JPanel dot=new JPanel(){ @Override protected void paintComponent(Graphics g){ Graphics2D g2=(Graphics2D)g.create(); g2.setColor(new java.awt.Color(cr.r,cr.g,cr.b)); g2.fillRoundRect(0,3,12,12,3,3); g2.dispose(); } };
            dot.setOpaque(false); dot.setPreferredSize(new Dimension(14,18));
            JLabel lbl=new JLabel(f.label); lbl.setFont(new Font("Segoe UI",Font.PLAIN,10)); lbl.setForeground(TEXT_DARK);
            JLabel val=new JLabel(pct+"% ("+f.total+")"); val.setFont(new Font("Segoe UI",Font.BOLD,10)); val.setForeground(TEXT_GRAY);
            row.add(dot,BorderLayout.WEST); row.add(lbl,BorderLayout.CENTER); row.add(val,BorderLayout.EAST);
            leyendaPanel.add(row); leyendaPanel.add(Box.createVerticalStrut(3));
        }
        leyendaPanel.revalidate(); leyendaPanel.repaint();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JPanel card(int r) {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new java.awt.Color(0,0,0,10)); g2.fill(new RoundRectangle2D.Float(2,3,getWidth()-3,getHeight()-3,r,r));
                g2.setColor(CARD_BG); g2.fill(new RoundRectangle2D.Float(0,0,getWidth()-2,getHeight()-2,r,r));
                g2.setColor(BORDER_C); g2.setStroke(new BasicStroke(1f)); g2.draw(new RoundRectangle2D.Float(0.5f,0.5f,getWidth()-3,getHeight()-3,r,r));
                g2.dispose(); super.paintComponent(g);
            }
            @Override public boolean isOpaque(){ return false; }
        };
    }

    private JLabel infoFila(JPanel p,String ico,String lbl,String val) {
        JPanel row=new JPanel(new BorderLayout(6,0)); row.setOpaque(false); row.setMaximumSize(new Dimension(Integer.MAX_VALUE,28)); row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel k=new JLabel(ico+" "+lbl); k.setFont(new Font("Segoe UI",Font.PLAIN,11)); k.setForeground(TEXT_GRAY);
        JLabel v=new JLabel(val); v.setFont(new Font("Segoe UI",Font.BOLD,11)); v.setForeground(TEXT_DARK);
        row.add(k,BorderLayout.WEST); row.add(v,BorderLayout.EAST); p.add(row); return v;
    }

    private JLabel resRow(JPanel p,String ico,String lbl,java.awt.Color color) {
        JPanel row=new JPanel(new BorderLayout(6,0)); row.setOpaque(false); row.setMaximumSize(new Dimension(Integer.MAX_VALUE,30)); row.setAlignmentX(Component.LEFT_ALIGNMENT); row.setBorder(new EmptyBorder(2,0,2,0));
        JLabel k=new JLabel(ico+" "+lbl); k.setFont(new Font("Segoe UI",Font.PLAIN,11)); k.setForeground(TEXT_GRAY);
        JLabel v=new JLabel("—"); v.setFont(new Font("Segoe UI",Font.BOLD,14)); v.setForeground(color);
        row.add(k,BorderLayout.WEST); row.add(v,BorderLayout.EAST); p.add(row); return v;
    }

    private static JButton miniBtn(String txt,java.awt.Color bg,java.awt.Color fg) {
        JButton b=new JButton(txt){ boolean h=false; {addMouseListener(new MouseAdapter(){ @Override public void mouseEntered(MouseEvent e){h=true;repaint();} @Override public void mouseExited(MouseEvent e){h=false;repaint();} }); } @Override protected void paintComponent(Graphics g){ Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(h?bg.darker():bg); g2.fill(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,8,8)); g2.dispose(); super.paintComponent(g); } };
        b.setFont(new Font("Segoe UI",Font.BOLD,12)); b.setForeground(fg); b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); b.setPreferredSize(new Dimension(80,30)); return b;
    }

    private static java.util.Date toJavaDate(LocalDate d) { return java.util.Date.from(d.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()); }

    // ── Gráfico de barras + línea ─────────────────────────────────────────────
    static class GraficoBarrasLinea extends JPanel {
        private List<PuntoDia> datos;
        GraficoBarrasLinea(){ setBackground(java.awt.Color.WHITE); setOpaque(true); }
        void setData(List<PuntoDia> d){ this.datos=d; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int W=getWidth(),H=getHeight(),pL=50,pR=60,pT=30,pB=50,gW=W-pL-pR,gH=H-pT-pB;
            g2.setColor(new java.awt.Color(0xF0F4F8)); g2.fillRect(pL,pT,gW,gH);
            if (datos==null||datos.isEmpty()){ g2.setColor(new java.awt.Color(0x999999)); g2.setFont(new Font("Segoe UI",Font.PLAIN,12)); String m="Sin datos"; FontMetrics fm=g2.getFontMetrics(); g2.drawString(m,(W-fm.stringWidth(m))/2,H/2); g2.dispose(); return; }
            int n=datos.size(), maxBar=datos.stream().mapToInt(p->p.total).max().orElse(1); maxBar=(int)(maxBar*1.15)+1;
            g2.setFont(new Font("Segoe UI",Font.PLAIN,9));
            for (int i=0;i<=5;i++) { int y=pT+gH-i*gH/5; g2.setColor(new java.awt.Color(0xDDE3EE)); g2.setStroke(new BasicStroke(0.7f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,0,new float[]{3,3},0)); g2.drawLine(pL,y,pL+gW,y); g2.setColor(new java.awt.Color(0x888888)); String lbl=String.valueOf(i*maxBar/5); g2.drawString(lbl,pL-g2.getFontMetrics().stringWidth(lbl)-4,y+4); }
            int bw=Math.max(4,gW/n-4); int[] lx=new int[n],ly=new int[n];
            for (int i=0;i<n;i++) {
                int x=pL+i*gW/n+(gW/n-bw)/2, bh=(int)((double)datos.get(i).total/maxBar*gH);
                GradientPaint gp=new GradientPaint(x,pT+gH-bh,new java.awt.Color(0x43A047),x,pT+gH,new java.awt.Color(0x2E7D32));
                g2.setPaint(gp); g2.setStroke(new BasicStroke(1)); g2.fillRoundRect(x,pT+gH-bh,bw,bh,3,3);
                g2.setColor(new java.awt.Color(0x1B5E20)); g2.drawRoundRect(x,pT+gH-bh,bw,bh,3,3);
                if (bh>16){ g2.setColor(new java.awt.Color(0x333333)); g2.setFont(new Font("Segoe UI",Font.BOLD,9)); String sv=String.valueOf(datos.get(i).total); FontMetrics fm=g2.getFontMetrics(); g2.drawString(sv,x+(bw-fm.stringWidth(sv))/2,pT+gH-bh-2); }
                int step=Math.max(1,n/15);
                if (i%step==0||i==n-1){ g2.setColor(new java.awt.Color(0x555555)); g2.setFont(new Font("Segoe UI",Font.PLAIN,9)); String ex=datos.get(i).fecha; FontMetrics fm=g2.getFontMetrics(); g2.drawString(ex,x+bw/2-fm.stringWidth(ex)/2,pT+gH+14); }
                lx[i]=x+bw/2; ly[i]=pT+gH-bh;
            }
            if (n>1){ g2.setColor(new java.awt.Color(0x1565C0)); g2.setStroke(new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND)); for (int i=1;i<n;i++) g2.drawLine(lx[i-1],ly[i-1],lx[i],ly[i]); for (int i=0;i<n;i++){ g2.setColor(java.awt.Color.WHITE); g2.fillOval(lx[i]-4,ly[i]-4,8,8); g2.setColor(new java.awt.Color(0x1565C0)); g2.setStroke(new BasicStroke(1.5f)); g2.drawOval(lx[i]-4,ly[i]-4,8,8); } }
            g2.setColor(new java.awt.Color(0xBBBBBB)); g2.setStroke(new BasicStroke(1.5f)); g2.drawLine(pL,pT,pL,pT+gH); g2.drawLine(pL,pT+gH,pL+gW,pT+gH);
            // Leyenda
            g2.setFont(new Font("Segoe UI",Font.PLAIN,10));
            g2.setColor(new java.awt.Color(0x2E7D32)); g2.fillRect(pL+gW-200,pT-18,12,10); g2.setColor(new java.awt.Color(0x333333)); g2.drawString("Total ingresos",pL+gW-183,pT-9);
            g2.setColor(new java.awt.Color(0x1565C0)); g2.setStroke(new BasicStroke(2)); g2.drawLine(pL+gW-80,pT-13,pL+gW-66,pT-13); g2.fillOval(pL+gW-77,pT-16,6,6); g2.setColor(new java.awt.Color(0x333333)); g2.drawString("Promedio",pL+gW-60,pT-9);
            g2.dispose();
        }
    }

    // ── Gráfico de dona ───────────────────────────────────────────────────────
    static class GraficoDona extends JPanel {
        private List<FranjaHoraria> franjas; private int total;
        GraficoDona(){ setOpaque(false); }
        void setData(List<FranjaHoraria> f,int t){ franjas=f; total=t; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (franjas==null||franjas.isEmpty()) return;
            Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int sz=Math.min(getWidth(),getHeight())-10, x=(getWidth()-sz)/2, y=(getHeight()-sz)/2; float start=90;
            for (FranjaHoraria f:franjas){ float sw=total>0?(float)f.total/total*360:0; g2.setColor(new java.awt.Color(f.color.r,f.color.g,f.color.b)); g2.fill(new Arc2D.Float(x,y,sz,sz,start,-sw,Arc2D.PIE)); start-=sw; }
            int hole=sz/3; g2.setColor(java.awt.Color.WHITE); g2.fillOval(x+(sz-hole)/2,y+(sz-hole)/2,hole,hole);
            g2.dispose();
        }
    }
}
