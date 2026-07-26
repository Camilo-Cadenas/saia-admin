# Componentes UI - Sistema SAIA

## Botones Estandarizados

Todos los botones del sistema deben usar los métodos de la clase `UITheme` para mantener consistencia visual y facilitar el mantenimiento.

### Tipos de Botones Disponibles

#### 1. Botón Primario (`primaryButton`)
**Uso:** Acción principal en formularios (Guardar, Registrar, Enviar)
- Fondo: Verde SENA (#238276)
- Texto: Blanco
- Hover: Verde oscuro

```java
// Tamaño por defecto (140x38)
JButton btnGuardar = UITheme.primaryButton("Guardar");

// Tamaño personalizado
JButton btnGuardar = UITheme.primaryButton("Guardar", 150, 40);
```

#### 2. Botón Secundario (`secondaryButton`)
**Uso:** Acciones secundarias (Cancelar, Volver, Limpiar)
- Fondo: Blanco/Transparente
- Borde: Verde SENA
- Texto: Verde SENA
- Hover: Fondo verde pálido

```java
// Tamaño por defecto (120x38)
JButton btnCancelar = UITheme.secondaryButton("Cancelar");

// Tamaño personalizado
JButton btnCancelar = UITheme.secondaryButton("Cancelar", 130, 36);
```

#### 3. Botón Acento (`accentButton`)
**Uso:** Acción destacada única en la pantalla
- Fondo: Naranja SENA (#FC7323)
- Texto: Blanco
- Hover: Naranja oscuro

```java
// Tamaño por defecto (140x38)
JButton btnRegistrar = UITheme.accentButton("Registrar");

// Tamaño personalizado
JButton btnRegistrar = UITheme.accentButton("Registrar", 150, 40);
```

#### 4. Botón Peligro (`dangerButton`)
**Uso:** Acciones destructivas (Eliminar, Bloquear permanentemente)
- Fondo: Blanco
- Borde: Rojo (#C62828)
- Texto: Rojo
- Hover: Fondo rojo pálido

```java
// Tamaño por defecto (120x38)
JButton btnEliminar = UITheme.dangerButton("Eliminar");

// Tamaño personalizado
JButton btnEliminar = UITheme.dangerButton("Eliminar", 130, 36);
```

#### 5. Botón Sólido Personalizado (`solidButton`)
**Uso:** Cuando necesitas un color específico no cubierto por los anteriores
- Fondo: Color personalizado
- Texto: **Siempre blanco** (garantiza legibilidad)
- Hover: Color más oscuro automático

```java
// Tamaño por defecto (140x38)
Color azul = new Color(0x0D47A1);
JButton btnBuscar = UITheme.solidButton("Buscar", azul);

// Tamaño personalizado
JButton btnBuscar = UITheme.solidButton("Buscar", azul, 120, 32);
```

#### 6. Botón Outline Personalizado (`outlineButton`)
**Uso:** Botón con borde de color personalizado
- Fondo: Blanco/Transparente
- Borde: Color personalizado
- Texto: Color personalizado
- Hover: Fondo con transparencia del color del borde

```java
// Tamaño por defecto (140x38)
Color gris = new Color(0x64748B);
JButton btnLimpiar = UITheme.outlineButton("Limpiar", gris);

// Tamaño personalizado
JButton btnLimpiar = UITheme.outlineButton("Limpiar", gris, 150, 32);
```

### Agregar Iconos a los Botones

Todos los botones admiten iconos usando `IconUtil`:

```java
JButton btnBuscar = UITheme.solidButton("  Buscar", new Color(0x238276), 110, 32);
btnBuscar.setIcon(IconUtil.icon(
    FontAwesomeSolid.SEARCH, 14, Color.WHITE));
```

**Nota:** Agrega espacios al inicio del texto (`"  Buscar"`) para separación visual del icono.

### Ejemplos Completos

#### Formulario con Botones Primario y Secundario
```java
JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

JButton btnCancelar = UITheme.secondaryButton("Cancelar");
btnCancelar.addActionListener(e -> cerrarFormulario());

JButton btnGuardar = UITheme.primaryButton("Guardar");
btnGuardar.addActionListener(e -> guardarDatos());

footer.add(btnCancelar);
footer.add(btnGuardar);
```

#### Panel de Búsqueda
```java
JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));

JButton btnLimpiar = UITheme.outlineButton("  Limpiar filtros", new Color(0x64748B), 150, 32);
btnLimpiar.setIcon(IconUtil.icon(FontAwesomeSolid.REDO, 14, new Color(0x64748B)));
btnLimpiar.addActionListener(e -> limpiarFiltros());

JButton btnBuscar = UITheme.solidButton("  Buscar", UITheme.PRIMARY, 110, 32);
btnBuscar.setIcon(IconUtil.icon(FontAwesomeSolid.SEARCH, 14, Color.WHITE));
btnBuscar.addActionListener(e -> buscar());

filtros.add(btnLimpiar);
filtros.add(btnBuscar);
```

#### Diálogo con Botón de Cierre
```java
JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));

JButton btnCerrar = UITheme.solidButton("  Cerrar  ", UITheme.PRIMARY, 120, 36);
btnCerrar.setIcon(IconUtil.icon(FontAwesomeSolid.TIMES, 14, Color.WHITE));
btnCerrar.addActionListener(e -> dialog.dispose());

footer.add(btnCerrar);
```

### Colores Predefinidos en UITheme

Para usar con `solidButton` y `outlineButton`:

```java
UITheme.PRIMARY        // #238276 - Verde SENA principal
UITheme.PRIMARY_DARK   // #1D6B62 - Verde oscuro
UITheme.SECONDARY      // #596548 - Verde complementario
UITheme.ACCENT         // #FC7323 - Naranja SENA
UITheme.ERROR          // #C62828 - Rojo error
UITheme.TEXT_PRIMARY   // #2B2B2B - Texto principal
UITheme.TEXT_SECONDARY // #6B7280 - Texto secundario
```

### Migración de Código Antiguo

#### Antes (código duplicado):
```java
private static JButton solidBtn(String text, Color bg) {
    JButton b = new JButton(text) {
        // ... código duplicado de 30+ líneas
    };
    return b;
}

JButton btn = solidBtn("Buscar", new Color(0x238276));
btn.setForeground(Color.WHITE);
```

#### Después (usando UITheme):
```java
JButton btn = UITheme.solidButton("Buscar", UITheme.PRIMARY);
```

### Ventajas del Enfoque Estandarizado

✅ **Consistencia visual** - Todos los botones tienen el mismo diseño
✅ **Menos código** - No necesitas duplicar lógica de botones
✅ **Fácil mantenimiento** - Un cambio en UITheme afecta todo el sistema
✅ **Texto legible** - Los botones sólidos siempre usan texto blanco
✅ **Accesibilidad** - Colores y contrastes siguiendo estándares SENA

### Notas Importantes

1. **NO crear métodos locales** como `solidBtn()`, `outlineBtn()`, `makeBtn()` en tus paneles
2. **SIEMPRE usar** los métodos de `UITheme`
3. Los botones sólidos (`solidButton`) **siempre tienen texto blanco** - no es necesario llamar a `setForeground(Color.WHITE)`
4. Agregar espacios al inicio del texto para separación visual con iconos: `"  Buscar"` en lugar de `"Buscar"`
5. Los tamaños recomendados:
   - Botones normales: `38px` de altura
   - Botones compactos (filtros): `32px` de altura
   - Botones grandes (diálogos): `40-46px` de altura

---

## Otros Componentes UI

### Tarjetas (`card`)
```java
JPanel card = UITheme.card(12); // Radio de esquinas
card.setLayout(new BorderLayout());
card.setBorder(UITheme.innerPadding());
```

### Campos de Texto
```java
JTextField campo = new JTextField();
UITheme.styleTextField(campo);
```

### ComboBox
```java
JComboBox<String> combo = new JComboBox<>();
UITheme.styleComboBox(combo);
```

### Tablas
```java
JTable tabla = new JTable(model);
UITheme.styleTable(tabla);
UITheme.styleTableHeader(tabla); // Encabezado verde con texto blanco
```

### Labels
```java
JLabel titulo = UITheme.pageTitle("Mi Título");
JLabel seccion = UITheme.sectionTitle("Sección");
JLabel caption = UITheme.caption("Texto descriptivo");
```

---

**Última actualización:** Enero 2025
**Mantenedor:** Equipo de Desarrollo SAIA
