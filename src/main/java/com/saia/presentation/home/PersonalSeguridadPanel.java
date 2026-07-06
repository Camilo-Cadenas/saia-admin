package com.saia.presentation.home;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.saia.business.PersonalSeguridadService;
import com.saia.model.PersonalSeguridad;

/**
 * Panel principal de "Personal de Seguridad".
 * Contiene tres sub-paneles intercambiables con CardLayout:
 *   LISTA   – tabla con todos los guardias + buscador
 *   REGISTRO – formulario de registro de nuevo guardia
 *   EDITAR   – formulario de edición del guardia seleccionado
 */
public class PersonalSeguridadPanel extends JPanel {

    // ── Colores ───────────────────────────────────────────────────────────────
    static final Color NAVY       = new Color(0x1A3A5C);
    static final Color NAVY_LIGHT = new Color(0x2C5282);
    static final Color GREEN      = new Color(0x2E7D32);
    static final Color BG_PAGE    = new Color(0xF5F6FA);
    static final Color CARD_BG    = Color.WHITE;
    static final Color BORDER_C   = new Color(0xDDE1E7);
    static final Color BORDER_ERR = new Color(0xC62828);
    static final Color TEXT_DARK  = new Color(0x1A1A2E);
    @SuppressWarnings("unused") // usada vía import static en sub-paneles
    static final Color TEXT_GRAY  = new Color(0x5A6474);
    static final Color BTN_CANCEL = new Color(0x6C757D);

    // ── Cards ─────────────────────────────────────────────────────────────────
    private static final String CARD_LISTA    = "LISTA";
    private static final String CARD_REGISTRO = "REGISTRO";
    private static final String CARD_EDITAR   = "EDITAR";

    private final CardLayout   cards   = new CardLayout();
    private final JPanel       content = new JPanel(cards);

    final PersonalSeguridadService service = new PersonalSeguridadService();

    PersonalSeguridadService getService() { return service; }

    // Sub-paneles
    private ListaPanel      listaPanel;
    private RegistroPanel   registroPanel;
    private EditarPanel     editarPanel;

    public PersonalSeguridadPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_PAGE);
        buildUI();
    }

    private void buildUI() {
        listaPanel    = new ListaPanel(this);
        registroPanel = new RegistroPanel(this);
        editarPanel   = new EditarPanel(this);

        content.setOpaque(false);
        content.add(listaPanel,    CARD_LISTA);
        content.add(registroPanel, CARD_REGISTRO);
        content.add(editarPanel,   CARD_EDITAR);

        add(content, BorderLayout.CENTER);
        // Mostrar lista por defecto (sin cargar datos todavía)
        cards.show(content, CARD_LISTA);
    }

    /** Se llama automáticamente cuando el panel se añade a la ventana visible. */
    @Override
    public void addNotify() {
        super.addNotify();
        // Cargar datos la primera vez que el panel se hace visible
        SwingUtilities.invokeLater(listaPanel::cargarDatos);
    }

    void mostrar(String card) {
        cards.show(content, card);
        if (CARD_LISTA.equals(card)) {
            SwingUtilities.invokeLater(() -> listaPanel.cargarDatos());
        }
    }

    void abrirEditar(PersonalSeguridad g) {
        editarPanel.cargar(g);
        mostrar(CARD_EDITAR);
    }
}
