package com.saia.presentation;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JLabel;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * Utilidad centralizada para crear iconos Ikonli (FontAwesome 5 Solid + Material Design)
 * en todos los paneles de la capa de vista SAIA.
 *
 * <pre>
 * Uso básico:
 *   // Icono en un botón
 *   JButton btn = new JButton("Guardar");
 *   btn.setIcon(IconUtil.icon(FontAwesomeSolid.SAVE, 14, Color.WHITE));
 *
 *   // Icono como JLabel independiente
 *   JLabel ico = IconUtil.label(FontAwesomeSolid.USER, 20, UITheme.PRIMARY);
 *
 *   // Iconos predefinidos del sistema SAIA
 *   btn.setIcon(IconUtil.SAVE);
 *   btn.setIcon(IconUtil.EDIT);
 * </pre>
 */
public final class IconUtil {

    private IconUtil() {}

    // ── Colores de iconos ─────────────────────────────────────────────────────
    public static final Color WHITE  = Color.WHITE;
    public static final Color DARK   = UITheme.TEXT_PRIMARY;
    public static final Color GREEN  = UITheme.PRIMARY;
    public static final Color ORANGE = UITheme.ACCENT;
    public static final Color GRAY   = UITheme.TEXT_SECONDARY;
    public static final Color RED    = UITheme.ERROR;

    // ── Fábrica base ─────────────────────────────────────────────────────────

    /**
     * Crea un FontIcon de FontAwesome 5 Solid con color y tamaño especificados.
     */
    public static FontIcon icon(FontAwesomeSolid glyph, int size, Color color) {
        FontIcon fi = FontIcon.of(glyph, size, color);
        return fi;
    }

    /**
     * Crea un FontIcon de Material Design con color y tamaño especificados.
     */
    public static FontIcon icon(MaterialDesign glyph, int size, Color color) {
        return FontIcon.of(glyph, size, color);
    }

    /**
     * Crea un JLabel con un icono centrado, listo para añadir a paneles.
     */
    public static JLabel label(FontAwesomeSolid glyph, int size, Color color) {
        JLabel l = new JLabel(icon(glyph, size, color));
        l.setPreferredSize(new Dimension(size + 4, size + 4));
        return l;
    }

    public static JLabel label(MaterialDesign glyph, int size, Color color) {
        JLabel l = new JLabel(icon(glyph, size, color));
        l.setPreferredSize(new Dimension(size + 4, size + 4));
        return l;
    }

    // ── Iconos predefinidos para la app SAIA ─────────────────────────────────
    // Tamaño estándar en botones de barra: 14px
    // Tamaño en KPI cards / sección headers: 20px

    // — Acciones principales —
    public static FontIcon save()     { return icon(FontAwesomeSolid.SAVE,           14, WHITE); }
    public static FontIcon edit()     { return icon(FontAwesomeSolid.EDIT,           14, WHITE); }
    public static FontIcon add()      { return icon(FontAwesomeSolid.PLUS,           14, WHITE); }
    public static FontIcon delete()   { return icon(FontAwesomeSolid.TRASH,          14, WHITE); }
    public static FontIcon refresh()  { return icon(FontAwesomeSolid.SYNC_ALT,       14, WHITE); }
    public static FontIcon search()   { return icon(FontAwesomeSolid.SEARCH,         14, GRAY);  }
    public static FontIcon filter()   { return icon(FontAwesomeSolid.FILTER,         14, GRAY);  }
    public static FontIcon clear()    { return icon(FontAwesomeSolid.TIMES_CIRCLE,   14, GRAY);  }
    public static FontIcon download() { return icon(FontAwesomeSolid.DOWNLOAD,       14, WHITE); }
    public static FontIcon upload()   { return icon(FontAwesomeSolid.UPLOAD,         14, WHITE); }
    public static FontIcon eye()      { return icon(FontAwesomeSolid.EYE,            14, WHITE); }
    public static FontIcon back()     { return icon(FontAwesomeSolid.ARROW_LEFT,     14, WHITE); }
    public static FontIcon close()    { return icon(FontAwesomeSolid.TIMES,          14, GRAY);  }
    public static FontIcon lock()     { return icon(FontAwesomeSolid.LOCK,           14, WHITE); }
    public static FontIcon unlock()   { return icon(FontAwesomeSolid.LOCK_OPEN,      14, WHITE); }
    public static FontIcon check()    { return icon(FontAwesomeSolid.CHECK,          14, WHITE); }
    public static FontIcon calendar() { return icon(FontAwesomeSolid.CALENDAR_ALT,   14, WHITE); }
    public static FontIcon logout()   { return icon(FontAwesomeSolid.SIGN_OUT_ALT,   14, RED);   }

    // — Sidebar / Navegación (16px sobre fondo blanco) —
    public static FontIcon navHome()       { return icon(FontAwesomeSolid.HOME,            16, GRAY); }
    public static FontIcon navGuards()     { return icon(FontAwesomeSolid.USER_SHIELD,      16, GRAY); }
    public static FontIcon navStudents()   { return icon(FontAwesomeSolid.USER_GRADUATE,    16, GRAY); }
    public static FontIcon navBlocked()    { return icon(FontAwesomeSolid.USER_SLASH,       16, GRAY); }
    public static FontIcon navReports()    { return icon(FontAwesomeSolid.FILE_ALT,         16, GRAY); }
    public static FontIcon navDownload()   { return icon(FontAwesomeSolid.FILE_DOWNLOAD,    16, GRAY); }
    public static FontIcon navAudit()      { return icon(FontAwesomeSolid.HISTORY,          16, GRAY); }
    public static FontIcon navStats()      { return icon(FontAwesomeSolid.CHART_BAR,        16, GRAY); }
    public static FontIcon navConfig()     { return icon(FontAwesomeSolid.COG,              16, GRAY); }

    // Variante activa (verde SENA)
    public static FontIcon navHomeActive()     { return icon(FontAwesomeSolid.HOME,           16, GREEN); }
    public static FontIcon navGuardsActive()   { return icon(FontAwesomeSolid.USER_SHIELD,    16, GREEN); }
    public static FontIcon navStudentsActive() { return icon(FontAwesomeSolid.USER_GRADUATE,  16, GREEN); }
    public static FontIcon navBlockedActive()  { return icon(FontAwesomeSolid.USER_SLASH,     16, GREEN); }
    public static FontIcon navReportsActive()  { return icon(FontAwesomeSolid.FILE_ALT,       16, GREEN); }
    public static FontIcon navDownloadActive() { return icon(FontAwesomeSolid.FILE_DOWNLOAD,  16, GREEN); }
    public static FontIcon navAuditActive()    { return icon(FontAwesomeSolid.HISTORY,        16, GREEN); }
    public static FontIcon navStatsActive()    { return icon(FontAwesomeSolid.CHART_BAR,      16, GREEN); }
    public static FontIcon navConfigActive()   { return icon(FontAwesomeSolid.COG,            16, GREEN); }

    // — KPI / Dashboard (24px en tarjetas) —
    public static FontIcon kpiUsers()    { return icon(FontAwesomeSolid.USERS,         24, new Color(0x1565C0)); }
    public static FontIcon kpiReports()  { return icon(FontAwesomeSolid.FILE_ALT,      24, new Color(0xE65100)); }
    public static FontIcon kpiBlocked()  { return icon(FontAwesomeSolid.USER_LOCK,     24, new Color(0x6A1B9A)); }
    public static FontIcon kpiGuards()   { return icon(FontAwesomeSolid.SHIELD_ALT,    24, UITheme.PRIMARY);     }

    // — Tabla (12px en celdas de botón) —
    public static FontIcon tblEdit()     { return icon(FontAwesomeSolid.PEN,           12, WHITE); }
    public static FontIcon tblLock()     { return icon(FontAwesomeSolid.LOCK,          12, WHITE); }
    public static FontIcon tblUnlock()   { return icon(FontAwesomeSolid.LOCK_OPEN,     12, WHITE); }
    public static FontIcon tblEye()      { return icon(FontAwesomeSolid.EYE,           12, new Color(0x2563EB)); }
}
