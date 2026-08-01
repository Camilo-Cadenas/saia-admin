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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;

import com.saia.model.Persona;
import com.saia.model.PersonalSeguridad;
import static com.saia.presentation.home.PersonalSeguridadPanel.BG_PAGE;
import static com.saia.presentation.home.PersonalSeguridadPanel.BORDER_C;
import static com.saia.presentation.home.PersonalSeguridadPanel.BORDER_ERR;
import static com.saia.presentation.home.PersonalSeguridadPanel.CARD_BG;
import static com.saia.presentation.home.PersonalSeguridadPanel.NAVY;
import static com.saia.presentation.home.PersonalSeguridadPanel.NAVY_LIGHT;
import static com.saia.presentation.home.PersonalSeguridadPanel.TEXT_DARK;

/**
 * Clase base con los componentes y helpers compartidos entre
 * {@link RegistroPanel} y {@link EditarPanel}.
 */
abstract class BaseFormPanel extends JPanel {

    protected final PersonalSeguridadPanel parent;

    // Campos personales
    protected JComboBox<String> cmbTipDoc;
    protected JTextField        txtNumDoc;
    protected JTextField        txtNombres;
    protected JTextField        txtApellidos;
    protected JTextField        txtEmail;
    protected JTextField        txtTelefono;
    protected JTextField        txtFechaNac;
    protected LocalDate         fechaSeleccionada;
    protected JComboBox<String> cmbTipSang;
    protected JComboBox<String> cmbGenero;
    
    // Componentes de foto de perfil
    protected JLabel            lblFotoPreview;
    protected JButton           btnSeleccionarFoto;
    protected JButton           btnEliminarFoto;
    protected byte[]            fotoBytes;
    protected String            fotoNombreArchivo;
    protected String            fotoRutaActual;  // Para edición

    // Opciones estáticas
    static final String[] TIPOS_SANGRE = {
        "-- Seleccione --", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
    };
    static final String[] GENEROS = {
        "-- Seleccione --", "Masculino", "Femenino", "No binario",
        "Género fluido", "Prefiero no decirlo"
    };

    // Campos de guardia
    protected JTextField        txtEmpresa;
    protected JComboBox<String> cmbTurno;
    protected JComboBox<String> cmbEstado;

    protected BaseFormPanel(PersonalSeguridadPanel parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        setBackground(BG_PAGE);
        setBorder(new EmptyBorder(20, 24, 20, 24));
    }

    // ── Tarjeta Datos Personales ──────────────────────────────────────────────
    protected JPanel buildPersonalCard(boolean numDocEditable) {
        JPanel card = formCard();
        card.setLayout(new BorderLayout());
        card.add(sectionTitle("  DATOS PERSONALES",
            com.saia.presentation.IconUtil.icon(
                org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.USER, 12,
                java.awt.Color.WHITE)), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(14, 18, 14, 18));
        GridBagConstraints gc = defaultGbc();

        cmbTipDoc = new JComboBox<>(new String[]{
            "Cédula de Ciudadanía","Tarjeta de Identidad","Cédula de Extranjería","Pasaporte","NIT"
        });
        styleCombo(cmbTipDoc);
        addRow(form, gc, "Tipo de Documento: *", cmbTipDoc, 0);

        txtNumDoc = field("0123456789");
        if (numDocEditable) {
            applyNumericFilter(txtNumDoc, InputValidator.MAX_NUM_DOC);
        } else { 
            txtNumDoc.setEditable(false); 
            txtNumDoc.setBackground(new Color(0xF5F5F5)); 
        }
        addRow(form, gc, "N° Documento: *", txtNumDoc, 1);

        txtNombres   = field("Personal Seguridad");
        applyAlphanumericFilter(txtNombres, InputValidator.MAX_NOMBRES);
        
        txtApellidos = field("Personal Seguridad");
        applyAlphanumericFilter(txtApellidos, InputValidator.MAX_APELLIDOS);
        
        txtEmail     = field("ejemplo@correo.com");
        applyEmailFilter(txtEmail);
        
        txtTelefono  = field("3001234567");
        applyNumericFilter(txtTelefono, InputValidator.MAX_TELEFONO);

        addRow(form, gc, "Nombres: *",           txtNombres,   2);
        addRow(form, gc, "Apellidos: *",          txtApellidos, 3);
        addRow(form, gc, "Correo Electrónico: *", txtEmail,     4);
        addRow(form, gc, "Teléfono:",             txtTelefono,  5);
        addRow(form, gc, "Fecha de Nacimiento:",  buildDatePickerRow(), 6);

        // Tipo de sangre
        cmbTipSang = new JComboBox<>(TIPOS_SANGRE);
        styleCombo(cmbTipSang);
        addRow(form, gc, "Tipo de Sangre:",       cmbTipSang,   7);

        // Género
        cmbGenero = new JComboBox<>(GENEROS);
        styleCombo(cmbGenero);
        addRow(form, gc, "Género:",               cmbGenero,    8);

        card.add(form, BorderLayout.CENTER);
        return card;
    }
    
    // ── Componente de Foto de Perfil ──────────────────────────────────────────
    
    /**
     * Construye el panel para seleccionar/previsualizar foto de perfil.
     * 
     * @return Panel con preview de foto y botones de acción
     */
    private JPanel buildFotoPerfilRow() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        
        // Preview de la foto (circular)
        lblFotoPreview = new JLabel() {
            private java.awt.Image fotoImage = null;
            
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, 
                                   java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                                   java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                
                if (fotoImage != null) {
                    // Dibujar foto circular
                    g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, 80, 80));
                    g2.drawImage(fotoImage, 0, 0, 80, 80, null);
                    g2.setClip(null);
                    
                    // Borde
                    g2.setColor(NAVY);
                    g2.setStroke(new java.awt.BasicStroke(2f));
                    g2.draw(new java.awt.geom.Ellipse2D.Float(1, 1, 78, 78));
                } else {
                    // Fondo circular suave
                    g2.setColor(new Color(0xE8EAF6));
                    g2.fill(new java.awt.geom.Ellipse2D.Float(0, 0, 80, 80));
                    
                    // Borde del círculo
                    g2.setColor(new Color(0xC5CAE9));
                    g2.setStroke(new java.awt.BasicStroke(2f));
                    g2.draw(new java.awt.geom.Ellipse2D.Float(1, 1, 78, 78));
                    
                    // Ícono de cámara
                    g2.setColor(new Color(0x9FA8DA));
                    
                    // Cuerpo de la cámara (rectángulo redondeado)
                    int camWidth = 36;
                    int camHeight = 28;
                    int camX = 40 - camWidth / 2;
                    int camY = 32;
                    g2.fill(new java.awt.geom.RoundRectangle2D.Float(
                        camX, camY, camWidth, camHeight, 6, 6));
                    
                    // Visor/flash superior (rectángulo pequeño)
                    int visorWidth = 12;
                    int visorHeight = 6;
                    int visorX = camX + 4;
                    int visorY = camY - 8;
                    g2.fill(new java.awt.geom.RoundRectangle2D.Float(
                        visorX, visorY, visorWidth, visorHeight, 3, 3));
                    
                    // Lente (círculo en el centro)
                    g2.setColor(new Color(0x7986CB)); // Azul más oscuro para el lente
                    int lensSize = 16;
                    int lensX = 40 - lensSize / 2;
                    int lensY = camY + (camHeight - lensSize) / 2;
                    g2.fillOval(lensX, lensY, lensSize, lensSize);
                    
                    // Reflejo en el lente (círculo pequeño blanco)
                    g2.setColor(new Color(255, 255, 255, 160));
                    int reflectSize = 6;
                    g2.fillOval(lensX + 3, lensY + 3, reflectSize, reflectSize);
                }
                
                g2.dispose();
            }
            
            @SuppressWarnings("unused") // Usado mediante reflexión en seleccionarFoto() y cargarFotoExistente()
            public void setFotoImage(java.awt.Image img) {
                this.fotoImage = img;
                repaint();
            }
        };
        lblFotoPreview.setPreferredSize(new Dimension(80, 80));
        lblFotoPreview.setOpaque(false);
        
        // Botones
        JPanel botonesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botonesPanel.setOpaque(false);
        
        // Botón Seleccionar (Verde)
        btnSeleccionarFoto = new JButton("Seleccionar foto") {
            boolean hov = false;
            { 
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hov=true; repaint(); }
                    @Override public void mouseExited (MouseEvent e) { hov=false;repaint(); }
                }); 
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color baseColor = new Color(0x4CAF50); // Verde Material
                g2.setColor(hov ? baseColor.darker() : baseColor);
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,8,8));
                g2.dispose(); 
                super.paintComponent(g);
            }
        };
        btnSeleccionarFoto.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnSeleccionarFoto.setForeground(Color.WHITE);
        btnSeleccionarFoto.setOpaque(false);
        btnSeleccionarFoto.setContentAreaFilled(false);
        btnSeleccionarFoto.setBorderPainted(false);
        btnSeleccionarFoto.setFocusPainted(false);
        btnSeleccionarFoto.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSeleccionarFoto.setPreferredSize(new Dimension(130, 32));
        btnSeleccionarFoto.setIcon(com.saia.presentation.IconUtil.icon(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.UPLOAD, 12, Color.WHITE));
        btnSeleccionarFoto.addActionListener(e -> seleccionarFoto());
        
        // Botón Eliminar (Rojo)
        btnEliminarFoto = new JButton("Eliminar") {
            boolean hov = false;
            { 
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { 
                        if (isEnabled()) {
                            hov = true;
                            repaint(); 
                        }
                    }
                    @Override public void mouseExited (MouseEvent e) { 
                        hov = false;
                        repaint(); 
                    }
                }); 
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                Color baseColor = new Color(0xE53935); // Rojo Material
                
                // Si está deshabilitado, usar color más claro
                if (!isEnabled()) {
                    baseColor = new Color(0xEF9A9A); // Rojo claro para deshabilitado
                } else if (hov) {
                    baseColor = baseColor.darker();
                }
                
                g2.setColor(baseColor);
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,8,8));
                g2.dispose(); 
                super.paintComponent(g);
            }
            
            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                // FORZAR color blanco siempre, incluso cuando está deshabilitado
                setForeground(Color.WHITE);
            }
        };
        btnEliminarFoto.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnEliminarFoto.setForeground(Color.WHITE); // Color inicial blanco
        btnEliminarFoto.setOpaque(false);
        btnEliminarFoto.setContentAreaFilled(false);
        btnEliminarFoto.setBorderPainted(false);
        btnEliminarFoto.setFocusPainted(false);
        btnEliminarFoto.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEliminarFoto.setPreferredSize(new Dimension(90, 32));
        btnEliminarFoto.setIcon(com.saia.presentation.IconUtil.icon(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.TRASH_ALT, 11, Color.WHITE));
        btnEliminarFoto.setEnabled(false);
        
        // Después de deshabilitar, volver a forzar el color blanco
        btnEliminarFoto.setForeground(Color.WHITE);
        btnEliminarFoto.setDisabledIcon(com.saia.presentation.IconUtil.icon(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.TRASH_ALT, 11, Color.WHITE));
        
        btnEliminarFoto.addActionListener(e -> eliminarFoto());
        
        botonesPanel.add(btnSeleccionarFoto);
        botonesPanel.add(btnEliminarFoto);
        
        // Info de restricciones
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.add(botonesPanel);
        
        JLabel lblInfo = new JLabel("<html><span style='font-size:9px; color:#888;'>" +
                                    "JPG, PNG o WEBP (máx. 2 MB)</span></html>");
        lblInfo.setBorder(new EmptyBorder(4, 0, 0, 0));
        infoPanel.add(lblInfo);
        
        panel.add(lblFotoPreview, BorderLayout.WEST);
        panel.add(infoPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Abre un diálogo para seleccionar una imagen de perfil.
     */
    protected void seleccionarFoto() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle("Seleccionar foto de perfil");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Imágenes (JPG, PNG, WEBP)", "jpg", "jpeg", "png", "webp"));
        
        // ── CONFIGURAR VISTA PREVIA DE IMÁGENES ──
        chooser.setAccessory(createImagePreviewPanel(chooser));
        chooser.setFileView(new javax.swing.filechooser.FileView() {
            @Override
            public javax.swing.Icon getIcon(java.io.File f) {
                if (f.isFile()) {
                    String name = f.getName().toLowerCase();
                    if (name.endsWith(".jpg") || name.endsWith(".jpeg") || 
                        name.endsWith(".png") || name.endsWith(".webp")) {
                        // Cargar miniatura del archivo
                        try {
                            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(f);
                            if (img != null) {
                                java.awt.Image scaled = img.getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH);
                                return new javax.swing.ImageIcon(scaled);
                            }
                        } catch (java.io.IOException e) {
                            // Si falla la lectura del archivo, usar ícono por defecto
                        }
                    }
                }
                return super.getIcon(f);
            }
        });
        
        if (chooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            java.io.File file = chooser.getSelectedFile();
            
            // Validar tamaño (máx 2 MB)
            if (file.length() > 2 * 1024 * 1024) {
                javax.swing.JOptionPane.showMessageDialog(this,
                    "La imagen no puede superar los 2 MB.",
                    "Archivo muy grande", javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            try {
                // Leer bytes del archivo
                fotoBytes = java.nio.file.Files.readAllBytes(file.toPath());
                fotoNombreArchivo = file.getName();
                
                // Cargar preview
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(file);
                if (img != null) {
                    java.awt.Image scaled = img.getScaledInstance(80, 80, java.awt.Image.SCALE_SMOOTH);
                    try {
                        lblFotoPreview.getClass()
                            .getMethod("setFotoImage", java.awt.Image.class)
                            .invoke(lblFotoPreview, scaled);
                    } catch (ReflectiveOperationException ex) {
                        System.err.println("Error setting preview: " + ex.getMessage());
                    }
                    btnEliminarFoto.setEnabled(true);
                    // Forzar color blanco después de habilitar
                    btnEliminarFoto.setForeground(Color.WHITE);
                }
            } catch (java.io.IOException ex) {
                javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al cargar la imagen: " + ex.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Crea un panel de vista previa de imágenes para el JFileChooser.
     * Muestra una miniatura grande de la imagen seleccionada.
     * 
     * @param chooser El JFileChooser al que se asociará el panel
     * @return Panel de vista previa
     */
    private JPanel createImagePreviewPanel(javax.swing.JFileChooser chooser) {
        JPanel previewPanel = new JPanel(new BorderLayout(10, 10));
        previewPanel.setPreferredSize(new Dimension(250, 350));
        previewPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        previewPanel.setBackground(Color.WHITE);
        
        // Título
        JLabel lblTitulo = new JLabel("Vista Previa", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitulo.setForeground(TEXT_DARK);
        lblTitulo.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        // Panel para la imagen
        JPanel imageContainer = new JPanel(new BorderLayout());
        imageContainer.setBackground(new Color(0xF5F5F5));
        imageContainer.setBorder(new LineBorder(new Color(0xD0D0D0), 1, true));
        imageContainer.setPreferredSize(new Dimension(230, 230));
        
        JLabel lblPreview = new JLabel("", SwingConstants.CENTER);
        lblPreview.setVerticalAlignment(SwingConstants.CENTER);
        lblPreview.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblPreview.setForeground(new Color(0x888888));
        imageContainer.add(lblPreview, BorderLayout.CENTER);
        
        // Panel de información
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(new EmptyBorder(10, 5, 0, 5));
        
        JLabel lblNombre = new JLabel(" ");
        lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblNombre.setForeground(TEXT_DARK);
        lblNombre.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel lblDimensiones = new JLabel(" ");
        lblDimensiones.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblDimensiones.setForeground(new Color(0x666666));
        lblDimensiones.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel lblTamano = new JLabel(" ");
        lblTamano.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblTamano.setForeground(new Color(0x666666));
        lblTamano.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        infoPanel.add(lblNombre);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(lblDimensiones);
        infoPanel.add(Box.createVerticalStrut(3));
        infoPanel.add(lblTamano);
        
        previewPanel.add(lblTitulo, BorderLayout.NORTH);
        previewPanel.add(imageContainer, BorderLayout.CENTER);
        previewPanel.add(infoPanel, BorderLayout.SOUTH);
        
        // Listener para actualizar la vista previa cuando cambia la selección
        chooser.addPropertyChangeListener(evt -> {
            if (javax.swing.JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(evt.getPropertyName())) {
                java.io.File file = (java.io.File) evt.getNewValue();
                
                if (file == null || !file.isFile()) {
                    lblPreview.setIcon(null);
                    lblPreview.setText("No hay imagen seleccionada");
                    lblNombre.setText(" ");
                    lblDimensiones.setText(" ");
                    lblTamano.setText(" ");
                    return;
                }
                
                String name = file.getName().toLowerCase();
                if (name.endsWith(".jpg") || name.endsWith(".jpeg") || 
                    name.endsWith(".png") || name.endsWith(".webp")) {
                    
                    // Cargar imagen en hilo separado para no bloquear la UI
                    new javax.swing.SwingWorker<java.awt.Image, Void>() {
                        private String nombreArchivo;
                        private String dimensiones;
                        private String tamano;
                        
                        @Override
                        protected java.awt.Image doInBackground() throws Exception {
                            try {
                                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(file);
                                if (img != null) {
                                    nombreArchivo = file.getName();
                                    dimensiones = img.getWidth() + " × " + img.getHeight() + " px";
                                    
                                    long bytes = file.length();
                                    if (bytes < 1024) {
                                        tamano = bytes + " bytes";
                                    } else if (bytes < 1024 * 1024) {
                                        tamano = String.format("%.1f KB", bytes / 1024.0);
                                    } else {
                                        tamano = String.format("%.2f MB", bytes / (1024.0 * 1024.0));
                                    }
                                    
                                    // Escalar manteniendo proporción
                                    int maxWidth = 220;
                                    int maxHeight = 220;
                                    double scale = Math.min(
                                        (double) maxWidth / img.getWidth(),
                                        (double) maxHeight / img.getHeight()
                                    );
                                    int newWidth = (int) (img.getWidth() * scale);
                                    int newHeight = (int) (img.getHeight() * scale);
                                    
                                    return img.getScaledInstance(newWidth, newHeight, java.awt.Image.SCALE_SMOOTH);
                                }
                            } catch (java.io.IOException e) {
                                System.err.println("Error cargando preview: " + e.getMessage());
                            }
                            return null;
                        }
                        
                        @Override
                        protected void done() {
                            try {
                                java.awt.Image previewImg = get();
                                if (previewImg != null) {
                                    lblPreview.setIcon(new javax.swing.ImageIcon(previewImg));
                                    lblPreview.setText("");
                                    lblNombre.setText("📄 " + nombreArchivo);
                                    lblDimensiones.setText("📐 " + dimensiones);
                                    lblTamano.setText("💾 " + tamano);
                                } else {
                                    lblPreview.setIcon(null);
                                    lblPreview.setText("Error al cargar imagen");
                                    lblNombre.setText(" ");
                                    lblDimensiones.setText(" ");
                                    lblTamano.setText(" ");
                                }
                            } catch (java.util.concurrent.ExecutionException | InterruptedException e) {
                                lblPreview.setIcon(null);
                                lblPreview.setText("Error al cargar imagen");
                            }
                        }
                    }.execute();
                    
                } else {
                    lblPreview.setIcon(null);
                    lblPreview.setText("No es una imagen válida");
                    lblNombre.setText(file.getName());
                    lblDimensiones.setText(" ");
                    lblTamano.setText(" ");
                }
            }
        });
        
        return previewPanel;
    }
    
    /**
     * Elimina la foto de perfil seleccionada.
     */
    protected void eliminarFoto() {
        fotoBytes = null;
        fotoNombreArchivo = null;
        
        // Limpiar preview
        try {
            lblFotoPreview.getClass()
                .getMethod("setFotoImage", java.awt.Image.class)
                .invoke(lblFotoPreview, (java.awt.Image) null);
        } catch (ReflectiveOperationException ex) {
            System.err.println("Error clearing preview: " + ex.getMessage());
        }
        
        btnEliminarFoto.setEnabled(false);
        // Forzar color blanco después de deshabilitar
        btnEliminarFoto.setForeground(Color.WHITE);
    }
    
    /**
     * Carga una foto existente desde la base de datos para el modo edición.
     * 
     * @param rutaFoto Ruta de la foto en disco
     */
    protected void cargarFotoExistente(String rutaFoto) {
        if (rutaFoto == null || rutaFoto.isBlank()) {
            eliminarFoto();
            return;
        }
        
        fotoRutaActual = rutaFoto;
        
        // Cargar preview de forma asíncrona
        new javax.swing.SwingWorker<java.awt.Image, Void>() {
            @Override
            protected java.awt.Image doInBackground() {
                try {
                    java.io.File f = new java.io.File(rutaFoto);
                    if (!f.exists()) return null;
                    
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(f);
                    if (img == null) return null;
                    
                    return img.getScaledInstance(80, 80, java.awt.Image.SCALE_SMOOTH);
                } catch (java.io.IOException e) {
                    System.err.println("Error cargando foto existente: " + e.getMessage());
                    return null;
                }
            }
            
            @Override
            protected void done() {
                try {
                    java.awt.Image img = get();
                    if (img != null) {
                        lblFotoPreview.getClass()
                            .getMethod("setFotoImage", java.awt.Image.class)
                            .invoke(lblFotoPreview, img);
                        btnEliminarFoto.setEnabled(true);
                        // Forzar color blanco después de habilitar
                        btnEliminarFoto.setForeground(Color.WHITE);
                    }
                } catch (java.util.concurrent.ExecutionException | InterruptedException ex) {
                    System.err.println("Error obteniendo imagen: " + ex.getMessage());
                } catch (ReflectiveOperationException ex) {
                    System.err.println("Error mostrando foto: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // ── Tarjeta Datos del Guardia ─────────────────────────────────────────────
    protected JPanel buildGuardaCard() {
        JPanel card = formCard();
        card.setLayout(new BorderLayout());
        card.add(sectionTitle("  DATOS DEL GUARDA",
            com.saia.presentation.IconUtil.icon(
                org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.SHIELD_ALT, 12,
                java.awt.Color.WHITE)), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(14, 18, 14, 18));
        GridBagConstraints gc = defaultGbc();

        txtEmpresa = field("Empresa Seguridad");
        applyAlphanumericFilter(txtEmpresa, InputValidator.MAX_EMPRESA);
        
        cmbTurno   = new JComboBox<>(new String[]{
            "-- Seleccione turno --","Mañana","Tarde","Noche","Rotativo"
        });
        cmbEstado  = new JComboBox<>(new String[]{"Activo","Inactivo"});
        styleCombo(cmbTurno);
        styleCombo(cmbEstado);

        addRow(form, gc, "Empresa de Seguridad:", txtEmpresa, 0);
        addRow(form, gc, "Turno:",                cmbTurno,   1);
        addRow(form, gc, "Estado:",               cmbEstado,  2);
        
        // Foto de perfil
        addRow(form, gc, "Foto de Perfil:",       buildFotoPerfilRow(), 3);

        GridBagConstraints fill = (GridBagConstraints) gc.clone();
        fill.gridy = 4; fill.weighty = 1.0; fill.gridwidth = 2;
        form.add(Box.createGlue(), fill);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    // ── Date picker ───────────────────────────────────────────────────────────
    private JPanel buildDatePickerRow() {
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.setOpaque(false);

        txtFechaNac = new JTextField("dd/mm/aaaa");
        txtFechaNac.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtFechaNac.setEditable(false);
        txtFechaNac.setForeground(new Color(0xAAAAAA));
        txtFechaNac.setBackground(Color.WHITE);
        txtFechaNac.setBorder(new LineBorder(BORDER_C, 1, true));
        txtFechaNac.setPreferredSize(new Dimension(0, 34));

        JButton btnCal = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(NAVY);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,6,6));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnCal.setIcon(com.saia.presentation.IconUtil.calendar());
        btnCal.setOpaque(false); btnCal.setContentAreaFilled(false);
        btnCal.setBorderPainted(false); btnCal.setFocusPainted(false);
        btnCal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCal.setPreferredSize(new Dimension(34, 34));
        btnCal.addActionListener(e -> showCalendarPopup(btnCal));

        p.add(txtFechaNac, BorderLayout.CENTER);
        p.add(btnCal,      BorderLayout.EAST);
        return p;
    }

    protected void showCalendarPopup(Component owner) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Seleccionar fecha", true);
        dlg.setLayout(new BorderLayout(15, 15));
        dlg.getRootPane().setBorder(new EmptyBorder(25, 30, 25, 30));
        dlg.setResizable(false);
        dlg.getContentPane().setBackground(Color.WHITE);

        // Panel del selector de fecha con dropdowns
        JPanel selectorPanel = buildDateSelectorPanel();
        
        // Botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton btnAceptar = new JButton("Aceptar") {
            boolean hov = false;
            { 
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hov=true; repaint(); }
                    @Override public void mouseExited (MouseEvent e) { hov=false;repaint(); }
                }); 
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? NAVY.darker() : NAVY);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,8,8));
                g2.dispose(); 
                super.paintComponent(g);
            }
        };
        btnAceptar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAceptar.setForeground(Color.WHITE);
        btnAceptar.setOpaque(false);
        btnAceptar.setContentAreaFilled(false);
        btnAceptar.setBorderPainted(false);
        btnAceptar.setFocusPainted(false);
        btnAceptar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAceptar.setPreferredSize(new Dimension(110, 36));
        
        JButton btnCancelar = new JButton("Cancelar") {
            boolean hov = false;
            { 
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hov=true; repaint(); }
                    @Override public void mouseExited (MouseEvent e) { hov=false;repaint(); }
                }); 
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color btnColor = new Color(0xE0E0E0);
                g2.setColor(hov ? btnColor.darker() : btnColor);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,8,8));
                g2.dispose(); 
                super.paintComponent(g);
            }
        };
        btnCancelar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancelar.setForeground(new Color(0x333333));
        btnCancelar.setOpaque(false);
        btnCancelar.setContentAreaFilled(false);
        btnCancelar.setBorderPainted(false);
        btnCancelar.setFocusPainted(false);
        btnCancelar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCancelar.setPreferredSize(new Dimension(110, 36));
        
        btnAceptar.addActionListener(ev -> {
            if (selectedCalendarDate != null) {
                fechaSeleccionada = selectedCalendarDate;
                txtFechaNac.setText(fechaSeleccionada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                txtFechaNac.setForeground(new Color(0x333333));
            }
            dlg.dispose();
        });
        
        btnCancelar.addActionListener(ev -> dlg.dispose());
        
        buttonPanel.add(btnCancelar);
        buttonPanel.add(btnAceptar);
        
        dlg.add(selectorPanel, BorderLayout.CENTER);
        dlg.add(buttonPanel, BorderLayout.SOUTH);
        
        dlg.pack();
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }
    
    // Variable para almacenar la fecha seleccionada en el calendario
    private LocalDate selectedCalendarDate;
    
    /**
     * Construye el panel del selector de fecha con dropdowns para día, mes y año.
     * Diseño moderno y fácil de usar.
     */
    private JPanel buildDateSelectorPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));
        
        // Título con ícono
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        JLabel iconLabel = new JLabel(com.saia.presentation.IconUtil.icon(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.CALENDAR_ALT, 20, NAVY));
        JLabel titleLabel = new JLabel("Seleccione su fecha de nacimiento");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setForeground(TEXT_DARK);
        
        headerPanel.add(iconLabel);
        headerPanel.add(titleLabel);
        
        // Panel de selectores
        JPanel selectorsPanel = new JPanel(new GridBagLayout());
        selectorsPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Inicializar con fecha actual o fecha seleccionada
        LocalDate initialDate = fechaSeleccionada != null ? fechaSeleccionada : LocalDate.now();
        selectedCalendarDate = initialDate;
        
        // ── Selector de DÍA ──
        JLabel lblDia = new JLabel("Día:");
        lblDia.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDia.setForeground(TEXT_DARK);
        
        Integer[] dias = new Integer[31];
        for (int i = 0; i < 31; i++) dias[i] = i + 1;
        JComboBox<Integer> cmbDia = new JComboBox<>(dias);
        cmbDia.setSelectedItem(initialDate.getDayOfMonth());
        cmbDia.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cmbDia.setPreferredSize(new Dimension(100, 40));
        styleModernCombo(cmbDia);
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        selectorsPanel.add(lblDia, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        selectorsPanel.add(cmbDia, gbc);
        
        // ── Selector de MES ──
        JLabel lblMes = new JLabel("Mes:");
        lblMes.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblMes.setForeground(TEXT_DARK);
        
        String[] meses = {
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        };
        JComboBox<String> cmbMes = new JComboBox<>(meses);
        cmbMes.setSelectedIndex(initialDate.getMonthValue() - 1);
        cmbMes.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cmbMes.setPreferredSize(new Dimension(150, 40));
        styleModernCombo(cmbMes);
        
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.0;
        selectorsPanel.add(lblMes, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.4;
        selectorsPanel.add(cmbMes, gbc);
        
        // ── Selector de AÑO ──
        JLabel lblAnio = new JLabel("Año:");
        lblAnio.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblAnio.setForeground(TEXT_DARK);
        
        int currentYear = LocalDate.now().getYear();
        Integer[] anios = new Integer[120]; // Últimos 120 años
        for (int i = 0; i < 120; i++) anios[i] = currentYear - i;
        JComboBox<Integer> cmbAnio = new JComboBox<>(anios);
        cmbAnio.setSelectedItem(initialDate.getYear());
        cmbAnio.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cmbAnio.setPreferredSize(new Dimension(110, 40));
        styleModernCombo(cmbAnio);
        
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.0;
        selectorsPanel.add(lblAnio, gbc);
        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.3;
        selectorsPanel.add(cmbAnio, gbc);
        
        // Vista previa de la fecha seleccionada
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBackground(new Color(0xF5F7FA));
        previewPanel.setBorder(new LineBorder(new Color(0xD1D9E6), 1, true));
        previewPanel.setPreferredSize(new Dimension(0, 60));
        
        JLabel lblPreviewTitle = new JLabel("Fecha seleccionada:");
        lblPreviewTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblPreviewTitle.setForeground(new Color(0x666666));
        lblPreviewTitle.setBorder(new EmptyBorder(8, 15, 2, 15));
        
        JLabel lblPreviewDate = new JLabel(initialDate.format(
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", 
            java.util.Locale.forLanguageTag("es-ES"))));
        lblPreviewDate.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblPreviewDate.setForeground(NAVY);
        lblPreviewDate.setBorder(new EmptyBorder(2, 15, 8, 15));
        
        JPanel previewContent = new JPanel(new BorderLayout());
        previewContent.setOpaque(false);
        previewContent.add(lblPreviewTitle, BorderLayout.NORTH);
        previewContent.add(lblPreviewDate, BorderLayout.CENTER);
        
        previewPanel.add(previewContent, BorderLayout.CENTER);
        
        // Listener para actualizar la fecha cuando cambian los selectores
        java.awt.event.ActionListener updateDateListener = e -> {
            try {
                int dia = (Integer) cmbDia.getSelectedItem();
                int mes = cmbMes.getSelectedIndex() + 1;
                int anio = (Integer) cmbAnio.getSelectedItem();
                
                // Validar que la fecha sea válida
                if (dia > 28) {
                    // Verificar días válidos para el mes seleccionado
                    int maxDias = LocalDate.of(anio, mes, 1).lengthOfMonth();
                    if (dia > maxDias) {
                        dia = maxDias;
                        cmbDia.setSelectedItem(dia);
                    }
                }
                
                LocalDate newDate = LocalDate.of(anio, mes, dia);
                selectedCalendarDate = newDate;
                
                // Actualizar vista previa
                lblPreviewDate.setText(newDate.format(
                    DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", 
                    java.util.Locale.forLanguageTag("es-ES"))));
                
            } catch (Exception ex) {
                // Si hay error, mantener fecha actual
                System.err.println("Error actualizando fecha: " + ex.getMessage());
            }
        };
        
        cmbDia.addActionListener(updateDateListener);
        cmbMes.addActionListener(updateDateListener);
        cmbAnio.addActionListener(updateDateListener);
        
        // Ensamblar panel
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(selectorsPanel, BorderLayout.CENTER);
        panel.add(previewPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Aplica un estilo moderno a los JComboBox del selector de fecha.
     */
    private void styleModernCombo(JComboBox<?> combo) {
        combo.setBackground(Color.WHITE);
        combo.setForeground(new Color(0x333333));
        combo.setBorder(new LineBorder(new Color(0xD1D9E6), 2, true));
        combo.setFocusable(true);
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Renderizador personalizado para centrar el texto
        combo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, 
                    Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBorder(new EmptyBorder(6, 10, 6, 10));
                
                if (isSelected) {
                    label.setBackground(NAVY);
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(Color.WHITE);
                    label.setForeground(new Color(0x333333));
                }
                
                return label;
            }
        });
    }
    
    // ── Leer campos ───────────────────────────────────────────────────────────
    protected Persona leerPersona(boolean numDocEditable) {
        Persona p = new Persona();
        p.setTipDoc((String) cmbTipDoc.getSelectedItem());
        if (numDocEditable) {
            String nd = txtNumDoc.getText().trim();
            if (!nd.isEmpty()) p.setNumDoc(Integer.parseInt(nd));
        } else {
            String nd = txtNumDoc.getText().trim();
            if (!nd.isEmpty()) p.setNumDoc(Integer.parseInt(nd));
        }
        p.setNombres(val(txtNombres, "Ej: Carlos Andrés"));
        String apes = val(txtApellidos, "Ej: López Martínez");
        String[] parts = apes.split("\\s+", 2);
        p.setPApe(parts.length > 0 ? parts[0] : "");
        p.setSApe(parts.length > 1 ? parts[1] : null);
        p.setEmail(val(txtEmail, "ejemplo@correo.com"));
        String tel = val(txtTelefono, "Ej: 3001234567");
        if (!tel.isEmpty()) p.setTel(tel);
        if (fechaSeleccionada != null) p.setFechaNac(fechaSeleccionada);

        // Tipo de sangre
        String ts = (String) cmbTipSang.getSelectedItem();
        if (ts != null && !ts.equals("-- Seleccione --")) p.setTipSang(ts);

        // Género
        String gen = (String) cmbGenero.getSelectedItem();
        if (gen != null && !gen.equals("-- Seleccione --")) p.setGenero(gen);

        return p;
    }

    protected PersonalSeguridad leerGuardia() {
        PersonalSeguridad g = new PersonalSeguridad();
        String turno = (String) cmbTurno.getSelectedItem();
        g.setTurno("-- Seleccione turno --".equals(turno) ? null : turno);
        g.setEmpresaSeg(val(txtEmpresa, "Ej: Seguridad Total S.A.S"));
        return g;
    }

    /**
     * Obtiene el valor real del campo de texto, excluyendo el placeholder.
     * 
     * @param f Campo de texto
     * @param ph Texto del placeholder
     * @return Valor del campo sin el placeholder, trimmed
     */
    protected String val(JTextField f, String ph) {
        String v = f.getText().trim();
        // Si el texto es igual al placeholder o está vacío, retornar cadena vacía
        return (v.equals(ph) || v.isEmpty()) ? "" : v;
    }

    protected void markError(JTextField f) { f.setBorder(new LineBorder(BORDER_ERR, 2, true)); }
    protected void clearError(JTextField f){ f.setBorder(new LineBorder(BORDER_C,   1, true)); }

    protected boolean validateEmail(String e) {
        return InputValidator.isValidEmail(e);
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────
    protected JPanel formCard() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0,0,0,14));
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

    protected JPanel sectionTitle(String text) {
        return sectionTitle(text, null);
    }

    protected JPanel sectionTitle(String text, javax.swing.Icon icon) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(NAVY);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),10,10));
                g2.fillRect(0, getHeight()/2, getWidth(), getHeight()/2);
                g2.dispose(); super.paintComponent(g);
            }
        };
        p.setOpaque(false); p.setPreferredSize(new Dimension(0, 40));
        if (icon != null) {
            JLabel ico = new JLabel(icon);
            p.add(ico);
        }
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(Color.WHITE);
        p.add(lbl);
        return p;
    }

    protected JTextField field(String ph) {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setText(ph); 
        f.setForeground(new Color(0xAAAAAA));
        f.setBorder(new LineBorder(BORDER_C, 1, true));
        f.setPreferredSize(new Dimension(0, 34));
        
        // Ocultar el caret cuando hay placeholder
        f.getCaret().setVisible(false);
        
        // Variable para rastrear si es placeholder
        final boolean[] isPlaceholder = {true};
        
        f.addFocusListener(new FocusAdapter() {
            @Override 
            public void focusGained(FocusEvent e) {
                // Si es placeholder, limpiar el campo
                if (isPlaceholder[0]) {
                    f.setText(""); 
                    f.setForeground(new Color(0x333333));
                    isPlaceholder[0] = false;
                }
                f.setBorder(new LineBorder(NAVY_LIGHT, 2, true));
                f.getCaret().setVisible(true);
            }
            
            @Override 
            public void focusLost(FocusEvent e) {
                // Si está vacío, restaurar placeholder
                String text = f.getText().trim();
                if (text.isEmpty()) { 
                    f.setText(ph); 
                    f.setForeground(new Color(0xAAAAAA));
                    f.getCaret().setVisible(false);
                    isPlaceholder[0] = true;
                } else {
                    isPlaceholder[0] = false;
                }
                f.setBorder(new LineBorder(BORDER_C, 1, true));
            }
        });
        
        // Listener de teclado para limpiar placeholder al escribir
        f.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                if (isPlaceholder[0]) {
                    f.setText("");
                    f.setForeground(new Color(0x333333));
                    isPlaceholder[0] = false;
                }
            }
        });
        
        return f;
    }

    protected void styleCombo(JComboBox<String> c) {
        c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        c.setBackground(Color.WHITE);
        c.setBorder(new LineBorder(BORDER_C, 1, true));
        c.setPreferredSize(new Dimension(0, 34));
        c.setFocusable(false);
    }

    protected JButton actionBtn(String text, Color bg, Color fg, int w) {
        JButton btn = new JButton(text) {
            boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hov=true; repaint(); }
                @Override public void mouseExited (MouseEvent e) { hov=false;repaint(); }
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
        btn.setForeground(fg); btn.setOpaque(false);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(w, 38));
        return btn;
    }

    protected void addRow(JPanel form, GridBagConstraints gc, String label, Component comp, int row) {
        gc.gridy = row; gc.gridx = 0; gc.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(TEXT_DARK);
        form.add(lbl, gc);
        gc.gridx = 1; gc.weightx = 1.0;
        form.add(comp, gc);
    }

    protected GridBagConstraints defaultGbc() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(5, 6, 5, 6);
        gc.weightx = 0; gc.weighty = 0;
        return gc;
    }

    /**
     * Aplica filtro numérico estricto a un campo de texto.
     * Previene entrada de caracteres no numéricos y limita la longitud.
     * Compatible con placeholders - el filtro se desactiva cuando el campo contiene el placeholder.
     * 
     * @param f Campo de texto
     * @param maxLength Longitud máxima permitida
     */
    protected void applyNumericFilter(JTextField f, int maxLength) {
        AbstractDocument doc = (AbstractDocument) f.getDocument();
        
        // Guardar referencia al placeholder para que el filtro lo reconozca
        final String placeholder = f.getText();
        
        doc.setDocumentFilter(new javax.swing.text.DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws javax.swing.text.BadLocationException {
                if (string == null) return;
                
                // Si el campo tiene el placeholder, permitir cualquier inserción
                String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
                if (currentText.equals(placeholder)) {
                    super.insertString(fb, offset, string, attr);
                    return;
                }
                
                // Aplicar filtro numérico normal
                if (string.matches("\\d+")) {
                    int currentLength = fb.getDocument().getLength();
                    int finalLength = currentLength + string.length();
                    
                    // Para teléfonos, solo verificar longitud (no MAX_INT)
                    if (finalLength <= maxLength) {
                        super.insertString(fb, offset, string, attr);
                    }
                }
            }
            
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws javax.swing.text.BadLocationException {
                if (text == null) {
                    super.replace(fb, offset, length, text, attrs);
                    return;
                }
                
                // Si el campo tiene el placeholder, permitir cualquier reemplazo
                String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
                if (currentText.equals(placeholder)) {
                    super.replace(fb, offset, length, text, attrs);
                    return;
                }
                
                // Aplicar filtro numérico normal
                if (text.matches("\\d*")) {
                    int currentLength = fb.getDocument().getLength();
                    int finalLength = currentLength - length + text.length();
                    
                    // Para teléfonos, solo verificar longitud (no MAX_INT)
                    if (finalLength <= maxLength) {
                        super.replace(fb, offset, length, text, attrs);
                    }
                }
            }
        });
    }

    /**
     * Aplica filtro alfanumérico a un campo de texto.
     * Previene desbordamiento limitando la longitud según restricciones de BD.
     * Compatible con placeholders - el filtro se desactiva cuando el campo contiene el placeholder.
     * 
     * @param f Campo de texto
     * @param maxLength Longitud máxima permitida
     */
    protected void applyAlphanumericFilter(JTextField f, int maxLength) {
        AbstractDocument doc = (AbstractDocument) f.getDocument();
        
        // Guardar referencia al placeholder para que el filtro lo reconozca
        final String placeholder = f.getText();
        
        doc.setDocumentFilter(new javax.swing.text.DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws javax.swing.text.BadLocationException {
                if (string == null) return;
                
                // Si el campo tiene el placeholder, permitir cualquier inserción (será limpiado por el listener)
                String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
                if (currentText.equals(placeholder)) {
                    super.insertString(fb, offset, string, attr);
                    return;
                }
                
                // Aplicar filtro normal
                if (InputValidator.isValidAlphanumericChars(string)) {
                    int currentLength = fb.getDocument().getLength();
                    if (currentLength + string.length() <= maxLength) {
                        super.insertString(fb, offset, string, attr);
                    }
                }
            }
            
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws javax.swing.text.BadLocationException {
                if (text == null) {
                    super.replace(fb, offset, length, text, attrs);
                    return;
                }
                
                // Si el campo tiene el placeholder, permitir cualquier reemplazo
                String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
                if (currentText.equals(placeholder)) {
                    super.replace(fb, offset, length, text, attrs);
                    return;
                }
                
                // Aplicar filtro normal
                if (InputValidator.isValidAlphanumericChars(text)) {
                    int currentLength = fb.getDocument().getLength();
                    int finalLength = currentLength - length + text.length();
                    
                    if (finalLength <= maxLength) {
                        super.replace(fb, offset, length, text, attrs);
                    }
                }
            }
        });
    }

    /**
     * Aplica filtro de email a un campo de texto.
     * Previene caracteres inválidos y limita longitud a 255 caracteres.
     * Compatible con placeholders - el filtro se desactiva cuando el campo contiene el placeholder.
     * 
     * @param f Campo de texto para email
     */
    protected void applyEmailFilter(JTextField f) {
        AbstractDocument doc = (AbstractDocument) f.getDocument();
        
        // Guardar referencia al placeholder
        final String placeholder = f.getText();
        
        doc.setDocumentFilter(new javax.swing.text.DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws javax.swing.text.BadLocationException {
                if (string == null) return;
                
                // Si el campo tiene el placeholder, permitir inserción
                String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
                if (currentText.equals(placeholder)) {
                    super.insertString(fb, offset, string, attr);
                    return;
                }
                
                // Aplicar filtro normal
                if (InputValidator.isValidEmailChars(string)) {
                    int currentLength = fb.getDocument().getLength();
                    if (currentLength + string.length() <= InputValidator.MAX_EMAIL) {
                        super.insertString(fb, offset, string, attr);
                    }
                }
            }
            
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws javax.swing.text.BadLocationException {
                if (text == null) {
                    super.replace(fb, offset, length, text, attrs);
                    return;
                }
                
                // Si el campo tiene el placeholder, permitir reemplazo
                String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
                if (currentText.equals(placeholder)) {
                    super.replace(fb, offset, length, text, attrs);
                    return;
                }
                
                // Aplicar filtro normal
                if (InputValidator.isValidEmailChars(text)) {
                    int currentLength = fb.getDocument().getLength();
                    int finalLength = currentLength - length + text.length();
                    
                    if (finalLength <= InputValidator.MAX_EMAIL) {
                        super.replace(fb, offset, length, text, attrs);
                    }
                }
            }
        });
    }
}
