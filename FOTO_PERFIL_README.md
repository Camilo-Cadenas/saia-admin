# 📸 Funcionalidad: Foto de Perfil - Personal de Seguridad

## Descripción

Se ha implementado la funcionalidad para subir, visualizar y gestionar **fotos de perfil** del personal de seguridad en el módulo de gestión de usuarios.

---

## ✨ Características Implementadas

### 1. **Subida de Fotos**
- Selector de archivos con filtro para imágenes
- Formatos soportados: **JPG, JPEG, PNG, WEBP**
- Tamaño máximo: **2 MB**
- Validación automática de formato y tamaño

### 2. **Visualización**
- **Preview circular** de la foto antes de guardar
- Placeholder con ícono cuando no hay foto
- Visualización en tiempo real al seleccionar imagen

### 3. **Gestión**
- Botón "Seleccionar foto" para cargar imagen
- Botón "Eliminar" para quitar la foto seleccionada
- Carga automática de foto existente en modo edición
- Actualización de foto con preservación de la anterior

### 4. **Almacenamiento**
- Las fotos se guardan en: `uploads/perfiles/`
- Nombre de archivo único: `{numDoc}_{timestamp}.{extensión}`
- Ruta almacenada en BD: `foto_perfil` (VARCHAR 500)

---

## 🗂️ Estructura de Almacenamiento

```
proyecto/
├── uploads/
│   └── perfiles/
│       ├── 1234567890_1234567890123.jpg
│       ├── 9876543210_1234567890456.png
│       └── ...
```

### Formato de Nombre de Archivo
```
{numDoc}_{timestamp}.{extensión}

Ejemplo: 1151940954_1703859123456.jpg
```

---

## 📋 Cambios en Base de Datos

### Nueva Columna en Tabla `persona`

```sql
ALTER TABLE persona 
ADD COLUMN foto_perfil VARCHAR(500) NULL 
COMMENT 'Ruta relativa a la foto de perfil del usuario';
```

### Ejecutar Script SQL
```bash
mysql -u usuario -p saia_db < database/add_foto_perfil.sql
```

O ejecutar manualmente desde MySQL Workbench:
1. Abrir el archivo `database/add_foto_perfil.sql`
2. Ejecutar el script

---

## 🎨 Interfaz de Usuario

### Componente de Foto de Perfil

```
┌─────────────────────────────────────┐
│  Foto de Perfil:                    │
│                                      │
│    ┌───────┐   [Seleccionar foto]  │
│    │ 👤    │   [Eliminar]          │
│    │       │                        │
│    └───────┘   JPG, PNG o WEBP     │
│   (Preview)    (máx. 2 MB)         │
└─────────────────────────────────────┘
```

### Estados del Componente

#### 1. Sin Foto (Inicial)
- Círculo gris con ícono de usuario (👤)
- Botón "Eliminar" deshabilitado

#### 2. Foto Seleccionada
- Preview circular de la imagen
- Botón "Eliminar" habilitado

#### 3. Modo Edición con Foto Existente
- Carga automática de la foto desde BD
- Preview circular de la foto actual
- Botón "Eliminar" habilitado

---

## 🔐 Validaciones de Seguridad

### Validación de Formato
```java
// Solo se permiten extensiones seguras
String[] extensionesPermitidas = {"jpg", "jpeg", "png", "webp"};
```

### Validación de Tamaño
```java
// Tamaño máximo: 2 MB
if (file.length() > 2 * 1024 * 1024) {
    // Rechazar archivo
}
```

### Validación de Contenido
- Verificación de firma de archivo (magic bytes)
- Lectura con `ImageIO` para asegurar que es imagen válida
- Protección contra archivos maliciosos

---

## 📦 Archivos Modificados/Creados

### Modelo
- ✅ `Persona.java` - Agregado campo `fotoPerfil`

### DAO
- ✅ `PersonaDAO.java` - Actualizado INSERT/UPDATE/SELECT para incluir foto_perfil

### Presentación
- ✅ `BaseFormPanel.java` - Componente de foto de perfil
  - Método `buildFotoPerfilRow()` - Construye la UI
  - Método `seleccionarFoto()` - Abre selector de archivos
  - Método `eliminarFoto()` - Limpia la foto seleccionada
  - Método `cargarFotoExistente()` - Carga foto en modo edición

- ✅ `RegistroPanel.java` - Guardar foto en registro
  - Integración con `ConfiguracionService.guardarFotoPerfil()`
  - Limpieza de foto al resetear formulario

- ✅ `EditarPanel.java` - Cargar y actualizar foto
  - Carga automática de foto existente
  - Actualización con preservación de foto anterior

### Servicios
- ✅ `ConfiguracionService.java` - Ya existía el método `guardarFotoPerfil()`

### Base de Datos
- ✅ `add_foto_perfil.sql` - Script para agregar columna

### Documentación
- ✅ `FOTO_PERFIL_README.md` - Este archivo

---

## 🚀 Uso de la Funcionalidad

### Registrar Personal con Foto

1. Abrir "Nuevo Personal de Seguridad"
2. Completar datos personales
3. Hacer clic en "Seleccionar foto"
4. Elegir imagen (JPG, PNG o WEBP, máx 2 MB)
5. Verificar preview circular
6. Hacer clic en "GUARDAR"

### Editar Foto de Personal Existente

1. Seleccionar guardia de la lista
2. Hacer clic en "Editar"
3. La foto actual se carga automáticamente
4. Para cambiar:
   - Hacer clic en "Seleccionar foto"
   - Elegir nueva imagen
5. Para eliminar:
   - Hacer clic en "Eliminar"
6. Hacer clic en "GUARDAR CAMBIOS"

---

## 🧪 Casos de Prueba

### Prueba 1: Subir Foto en Registro
```
✓ Seleccionar imagen válida (JPG, 500 KB)
✓ Verificar preview circular
✓ Guardar persona
✓ Verificar que archivo se guardó en uploads/perfiles/
✓ Verificar que ruta se guardó en BD
```

### Prueba 2: Validación de Tamaño
```
✓ Intentar subir imagen > 2 MB
✓ Debe mostrar mensaje: "La imagen no puede superar los 2 MB"
✓ Preview debe permanecer vacío
```

### Prueba 3: Validación de Formato
```
✓ Intentar seleccionar archivo .txt
✓ Selector no debe permitir selección
✓ Solo mostrar archivos .jpg, .png, .webp
```

### Prueba 4: Eliminar Foto
```
✓ Seleccionar foto
✓ Hacer clic en "Eliminar"
✓ Preview debe volver a placeholder
✓ Botón "Eliminar" debe deshabilitarse
```

### Prueba 5: Editar Foto Existente
```
✓ Abrir edición de guardia con foto
✓ Foto debe cargarse automáticamente en preview
✓ Cambiar foto
✓ Guardar cambios
✓ Verificar que archivo anterior se eliminó
✓ Verificar que nuevo archivo existe
```

### Prueba 6: Mantener Foto en Edición
```
✓ Abrir edición de guardia con foto
✓ No cambiar la foto
✓ Modificar otros datos
✓ Guardar cambios
✓ Verificar que foto se mantiene igual
```

---

## 🐛 Troubleshooting

### Problema: La foto no se guarda

**Solución:**
1. Verificar que la carpeta `uploads/perfiles/` existe
2. Verificar permisos de escritura en la carpeta
3. Verificar que la columna `foto_perfil` existe en BD

### Problema: El preview no se muestra

**Solución:**
1. Verificar que el archivo es una imagen válida
2. Verificar que el tamaño no excede 2 MB
3. Verificar logs para excepciones de `ImageIO`

### Problema: Error al cargar foto existente

**Solución:**
1. Verificar que la ruta en BD es correcta
2. Verificar que el archivo existe en disco
3. Verificar permisos de lectura del archivo

---

## 📝 Notas Técnicas

### Formato de Guardado
- Las rutas se guardan como **rutas relativas**: `uploads/perfiles/archivo.jpg`
- NO se guardan rutas absolutas para portabilidad

### Escalado de Imágenes
- El preview se escala a 80x80 píxeles
- La imagen original se guarda sin modificar
- El escalado es solo para visualización

### Limpieza de Archivos
- Cuando se actualiza una foto, el archivo anterior se elimina automáticamente
- Esto previene acumulación de archivos huérfanos

### Seguridad
- Solo se permiten formatos de imagen
- Validación de tamaño para prevenir ataques DoS
- Nombres únicos previenen colisiones y sobrescritura

---

## 🔮 Mejoras Futuras (Opcional)

- [ ] Crop de imagen antes de guardar
- [ ] Redimensionamiento automático a tamaño óptimo
- [ ] Soporte para drag & drop
- [ ] Galería de avatares predefinidos
- [ ] Compresión automática de imágenes grandes
- [ ] Soporte para webcam (tomar foto)

---

## 📞 Soporte

Para dudas o problemas con la funcionalidad de fotos de perfil, contactar al equipo de desarrollo.

**Última actualización**: 2024  
**Versión**: 1.0  
**Responsable**: Equipo de Desarrollo SAIA
