# 🔒 Validaciones de Seguridad - Personal de Seguridad

## Resumen Ejecutivo

Se han implementado validaciones de seguridad robustas en el módulo de gestión de Personal de Seguridad para prevenir vulnerabilidades de desbordamiento de buffer, inyección de datos maliciosos y violaciones de restricciones de base de datos.

---

## ✅ Validaciones Implementadas

### 1. **Protección contra Desbordamiento de Datos (Buffer Overflow)**

#### Restricciones de Base de Datos
Todos los campos están limitados según las restricciones definidas en la base de datos:

| Campo | Tipo en BD | Límite | Implementación |
|-------|-----------|--------|----------------|
| `num_doc` | INT | 10 dígitos (máx: 2,147,483,647) | `InputValidator.MAX_NUM_DOC` |
| `nombres` | VARCHAR(50) | 50 caracteres | `InputValidator.MAX_NOMBRES` |
| `apellidos` | VARCHAR(50) | 50 caracteres c/u | `InputValidator.MAX_APELLIDOS` |
| `email` | VARCHAR(255) | 255 caracteres | `InputValidator.MAX_EMAIL` |
| `telefono` | VARCHAR(20) | 20 dígitos | `InputValidator.MAX_TELEFONO` |
| `empresa_seg` | VARCHAR(100) | 100 caracteres | `InputValidator.MAX_EMPRESA` |

#### Implementación
```java
// Los filtros previenen la entrada de más caracteres de los permitidos
applyAlphanumericFilter(txtNombres, InputValidator.MAX_NOMBRES);
applyAlphanumericFilter(txtApellidos, InputValidator.MAX_APELLIDOS);
applyEmailFilter(txtEmail); // Límite de 255 caracteres
```

---

### 2. **Validación de Campos Numéricos**

#### Campos con Validación Numérica Estricta
- **Número de Documento**: Solo permite dígitos del 0-9
- **Teléfono**: Solo permite dígitos del 0-9

#### Filtros Aplicados
```java
// Filtro numérico con validación de rango INT
applyNumericFilter(txtNumDoc, InputValidator.MAX_NUM_DOC);
applyNumericFilter(txtTelefono, InputValidator.MAX_TELEFONO);
```

#### Prevención de Ataques
✅ **Previene**: Inyección de caracteres no numéricos  
✅ **Previene**: Desbordamiento de enteros (valores > MAX_INT)  
✅ **Previene**: Valores negativos en campos de ID  

---

### 3. **Validación de Email (RFC 5322)**

#### Características
- Limita caracteres permitidos: `a-z`, `A-Z`, `0-9`, `@`, `.`, `_`, `%`, `+`, `-`
- Valida formato completo: `usuario@dominio.com`
- Longitud máxima: 255 caracteres
- Email se guarda en minúsculas (normalización)

#### Implementación
```java
// Filtro en tiempo real
applyEmailFilter(txtEmail);

// Validación completa antes de guardar
if (!InputValidator.isValidEmail(email)) {
    showError("El correo electrónico no tiene un formato válido.");
    return;
}
```

---

### 4. **Sanitización de Datos**

#### Protección contra Inyección
Todos los datos de texto son sanitizados antes de guardarse en la base de datos:

```java
persona.setNombres(InputValidator.sanitize(nombres));
persona.setPApe(InputValidator.sanitize(apes[0]));
persona.setEmpresaSeg(InputValidator.sanitize(empresa));
```

#### Funciones de Sanitización
- **Elimina caracteres de control** no imprimibles
- **Elimina múltiples espacios** consecutivos
- **Trim automático** de espacios iniciales y finales
- **Preserva caracteres** válidos (letras con acentos, puntos, comas, guiones)

---

### 5. **Validación en Tiempo Real (Filtros de Documento)**

Los `DocumentFilter` personalizados previenen la entrada de datos inválidos **antes** de que se escriban en el campo:

#### `numericFilter(maxLength)`
- Solo permite dígitos 0-9
- Valida que el número no exceda `MAX_INT` (2,147,483,647)
- Limita longitud al máximo especificado

#### `alphanumericFilter(maxLength)`
- Permite: letras (con acentos), números, espacios, puntos, comas, guiones, apóstrofes
- Rechaza: caracteres especiales peligrosos (`<`, `>`, `&`, `;`, `|`, etc.)
- Limita longitud según restricciones de BD

#### `emailFilter(maxLength)`
- Solo permite caracteres válidos en emails según RFC 5322
- Longitud máxima: 255 caracteres
- Previene inyección de caracteres especiales

---

## 🛡️ Casos de Uso de Seguridad

### Caso 1: Intento de Desbordamiento en Nombres
```
❌ Usuario intenta ingresar: "Carlos Andrés Martínez López González Rodríguez Fernández..." (>50 chars)
✅ Sistema bloquea después de 50 caracteres
✅ No se permite la entrada adicional
```

### Caso 2: Inyección SQL en Número de Documento
```
❌ Usuario intenta ingresar: "123'; DROP TABLE persona;--"
✅ Solo se permite: "123"
✅ Caracteres especiales son rechazados en tiempo real
```

### Caso 3: Email con Formato Inválido
```
❌ Usuario ingresa: "usuario@dominio" (sin TLD)
✅ Sistema valida formato completo antes de guardar
✅ Muestra mensaje: "El correo electrónico no tiene un formato válido"
```

### Caso 4: Número Documento Excede INT
```
❌ Usuario intenta ingresar: "99999999999" (11 dígitos, excede INT)
✅ Sistema rechaza entrada después del 10º dígito
✅ Valida que el valor no exceda 2,147,483,647
```

### Caso 5: Teléfono con Letras
```
❌ Usuario intenta ingresar: "300-ABC-1234"
✅ Solo se permite: "3001234"
✅ Guiones y letras son rechazados automáticamente
```

---

## 📋 Checklist de Validaciones por Campo

### ✅ Número de Documento
- [x] Solo números (filtro en tiempo real)
- [x] Máximo 10 dígitos
- [x] Valor no excede MAX_INT (2,147,483,647)
- [x] Campo obligatorio
- [x] Validación pre-guardado

### ✅ Nombres
- [x] Caracteres alfanuméricos + acentos
- [x] Máximo 50 caracteres (filtro en tiempo real)
- [x] Campo obligatorio
- [x] Sanitización pre-guardado

### ✅ Apellidos
- [x] Caracteres alfanuméricos + acentos
- [x] Máximo 50 caracteres por apellido
- [x] Campo obligatorio
- [x] Sanitización pre-guardado

### ✅ Correo Electrónico
- [x] Formato RFC 5322 (usuario@dominio.ext)
- [x] Máximo 255 caracteres
- [x] Caracteres válidos para email
- [x] Normalización a minúsculas
- [x] Campo obligatorio
- [x] Validación completa pre-guardado

### ✅ Teléfono
- [x] Solo números (filtro en tiempo real)
- [x] Mínimo 7 dígitos, máximo 20
- [x] Campo opcional
- [x] Validación formato si se proporciona

### ✅ Empresa de Seguridad
- [x] Caracteres alfanuméricos + acentos
- [x] Máximo 100 caracteres (filtro en tiempo real)
- [x] Campo opcional
- [x] Sanitización pre-guardado si se proporciona

---

## 🔐 Capa de Defensa en Profundidad

### Nivel 1: Interfaz de Usuario (Prevención)
- **DocumentFilter** en campos de texto
- Bloquea entrada inválida antes de escribirse
- Feedback visual inmediato

### Nivel 2: Validación Pre-Guardado (Detección)
- Validación exhaustiva antes de enviar a BD
- Mensajes de error descriptivos
- Verificación de rangos y formatos

### Nivel 3: Sanitización (Limpieza)
- Eliminación de caracteres de control
- Normalización de espacios
- Trim automático

### Nivel 4: Base de Datos (Última Línea)
- Restricciones de tipo (INT, VARCHAR)
- Límites de longitud
- Constraints y foreign keys

---

## 📦 Archivos Modificados

1. **`InputValidator.java`** (NUEVO)
   - Clase de utilidad con todos los filtros y validadores
   - Constantes de límites de BD
   - Métodos de sanitización

2. **`BaseFormPanel.java`** (MODIFICADO)
   - Aplicación de filtros a todos los campos
   - Métodos helper para validación
   - Integración con `InputValidator`

3. **`RegistroPanel.java`** (MODIFICADO)
   - Validaciones completas en método `guardar()`
   - Mensajes de error descriptivos
   - Sanitización de datos

4. **`EditarPanel.java`** (MODIFICADO)
   - Validaciones completas en método `guardar()`
   - Consistencia con `RegistroPanel`
   - Sanitización de datos

---

## 🚀 Pruebas Recomendadas

### Pruebas de Validación Numérica
1. Ingresar letras en campo numérico → Debe rechazarse
2. Ingresar número > 2,147,483,647 → Debe rechazarse
3. Ingresar 11+ dígitos en num_doc → Debe bloquearse en el 10º
4. Pegar texto con letras en teléfono → Solo dígitos deben insertarse

### Pruebas de Desbordamiento
1. Ingresar 51+ caracteres en nombres → Debe bloquearse en el 50º
2. Ingresar 256+ caracteres en email → Debe bloquearse en el 255º
3. Copiar/pegar texto largo en cualquier campo → Debe truncarse

### Pruebas de Email
1. Email sin @ → Error de validación
2. Email sin dominio → Error de validación
3. Email con espacios → Debe rechazarse
4. Email válido → Debe guardarse en minúsculas

### Pruebas de Caracteres Especiales
1. Ingresar `<script>alert('xss')</script>` → Debe rechazarse
2. Ingresar `'; DROP TABLE;--` → Solo números/letras deben aceptarse
3. Ingresar caracteres de control (Ctrl+caracteres) → Debe rechazarse

---

## 📞 Soporte

Para dudas o mejoras sobre las validaciones de seguridad, contactar al equipo de desarrollo.

**Última actualización**: 2024  
**Versión**: 2.0  
**Responsable**: Ingeniero de Seguridad Informática
