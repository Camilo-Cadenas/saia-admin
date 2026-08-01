# Diagnóstico: Error al Crear Nuevo Guarda

## Cambios Realizados

### 1. Logging Detallado Agregado
Se agregó logging extensivo al método `registrar()` en `PersonalSeguridadService.java` para identificar exactamente en qué paso falla el proceso.

### 2. Documentación Mejorada
Se agregó documentación sobre el ID del rol que debe existir en la base de datos.

## Posibles Causas del Error

### Causa 1: Tabla `rol` no tiene el registro con id_rol = 3

**Verificación:**
```sql
SELECT * FROM rol;
```

**Solución si falta:**
```sql
-- Verificar qué roles existen
SELECT id_rol, nom_rol FROM rol ORDER BY id_rol;

-- Si no existe id_rol = 3 para Personal de Seguridad/Guarda:
INSERT INTO rol (id_rol, nom_rol) VALUES (3, 'Guarda');
-- O si ya existe con otro nombre, verificar que sea id_rol = 3
```

**Roles esperados:**
- id_rol = 1 → 'Aprendiz'
- id_rol = 2 → 'Administrador'
- id_rol = 3 → 'Guarda' (o 'Personal de Seguridad')

### Causa 2: Restricción de Clave Foránea (Foreign Key)

**Error típico:** `Cannot add or update a child row: a foreign key constraint fails`

**Verificación:**
```sql
-- Verificar constraints de la tabla cuenta
SHOW CREATE TABLE cuenta;

-- Verificar que existe la clave foránea a rol
SELECT 
    CONSTRAINT_NAME, 
    COLUMN_NAME, 
    REFERENCED_TABLE_NAME, 
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_NAME = 'cuenta' 
  AND TABLE_SCHEMA = 'sena'
  AND REFERENCED_TABLE_NAME IS NOT NULL;
```

**Solución:** Asegurarse que existe el rol con id = 3 antes de insertar en cuenta.

### Causa 3: Columna `password` vs `password_hash`

La tabla `cuenta` debe tener una columna llamada `password` (no `password_hash`).

**Verificación:**
```sql
DESCRIBE cuenta;
-- o
SHOW COLUMNS FROM cuenta;
```

**Columnas esperadas en `cuenta`:**
- id_cuenta (INT, PK, AUTO_INCREMENT)
- id_rol (INT, FK → rol.id_rol)
- num_doc (INT, FK → persona.num_doc)
- mail (VARCHAR(255))
- password (VARCHAR(255))  ← Debe llamarse 'password', no 'password_hash'
- estado (BOOLEAN/TINYINT)
- fecha_creacion (TIMESTAMP)

**Solución si la columna se llama `password_hash`:**
```sql
ALTER TABLE cuenta CHANGE COLUMN password_hash password VARCHAR(255);
```

### Causa 4: Restricción UNIQUE en `cuenta.mail`

**Error típico:** `Duplicate entry '...' for key 'mail'`

Esto ya está manejado en el código (verifica email duplicado), pero si el error persiste:

**Verificación:**
```sql
SELECT num_doc, mail FROM cuenta WHERE mail = 'email_a_verificar@correo.com';
```

### Causa 5: Error en la tabla `auditoria`

Si falla al insertar en auditoría después de crear el guardia exitosamente.

**Verificación:**
```sql
DESCRIBE auditoria;
```

**Columnas esperadas:**
- id_auditoria (INT, PK, AUTO_INCREMENT)
- accion (VARCHAR)
- entidad (VARCHAR)
- id_entidad (INT)
- descripcion (TEXT)
- num_doc_admin (INT, nullable)
- fecha_hora (TIMESTAMP)

## Cómo Diagnosticar

### Paso 1: Ejecutar la aplicación con logging activado

1. Abre la aplicación SAIA-Admin
2. Ve al módulo "Personal de Seguridad"
3. Haz clic en "Registrar nuevo guarda"
4. Llena el formulario con datos de prueba
5. Haz clic en "GUARDAR"
6. **IMPORTANTE:** Observa la consola/terminal donde se ejecuta la aplicación

### Paso 2: Analizar los logs

Busca en la consola mensajes como:

```
[PersonalSeguridadService] ═══ INICIO REGISTRO ═══
[PersonalSeguridadService] Datos recibidos:
  - Num Doc: 123456789
  - Tip Doc: CC
  - Nombres: Juan
  ...
[PersonalSeguridadService] Verificando documento duplicado...
[PersonalSeguridadService] Verificando email duplicado...
[PersonalSeguridadService] Insertando en tabla persona...
[PersonalSeguridadService] ✓ Persona insertada
[PersonalSeguridadService] Insertando en tabla personal_seguridad...
[PersonalSeguridadService] ✓ Personal seguridad insertado
[PersonalSeguridadService] Insertando en tabla cuenta...
  - id_rol: 3
  - num_doc: 123456789
  - mail: juan@correo.com
  - estado: true
```

**El último mensaje antes del error te dirá exactamente dónde falla.**

### Paso 3: Identificar el error

Los posibles mensajes de error son:

1. **"ERROR: Faltan campos obligatorios"**
   - Problema: Validación de campos fallando
   - Solución: Verificar que todos los campos obligatorios estén llenos

2. **"ERROR: Documento XXXXXX ya existe"**
   - Problema: Ya hay una persona con ese número de documento
   - Solución: Usar otro número de documento o eliminar el registro duplicado

3. **"ERROR: Email XXXX ya existe"**
   - Problema: Ya hay una cuenta con ese email
   - Solución: Usar otro email o eliminar/actualizar el registro duplicado

4. **Error después de "Insertando en tabla persona..." pero antes de "✓ Persona insertada"**
   - Problema: Error en el INSERT de la tabla `persona`
   - Posibles causas:
     - Campo obligatorio NULL (nombres, p_ape, tip_doc, num_doc)
     - Longitud de campo excedida
     - Tipo de dato incorrecto

5. **Error después de "Insertando en tabla personal_seguridad..."**
   - Problema: Error en el INSERT de la tabla `personal_seguridad`
   - Posibles causas:
     - El num_doc no existe en persona (problema de integridad referencial)
     - Campos exceden longitud permitida

6. **Error después de "Insertando en tabla cuenta..."**
   - Problema: Error en el INSERT de la tabla `cuenta`
   - **Causa más probable:** No existe rol con id_rol = 3
   - Solución: Ejecutar el INSERT del rol (ver arriba)

7. **Error después de "Registrando auditoría..."**
   - Problema: Error en el INSERT de la tabla `auditoria`
   - Nota: El guardia YA está creado, solo falla el log de auditoría

## Script de Verificación de Base de Datos

Ejecuta este script completo en MySQL/MariaDB para verificar la estructura:

```sql
-- ═══════════════════════════════════════════════════════════════════
-- SCRIPT DE VERIFICACIÓN - Base de Datos SENA
-- ═══════════════════════════════════════════════════════════════════

USE sena;

-- 1. Verificar tabla ROL
SELECT '=== VERIFICACIÓN TABLA ROL ===' AS '';
SELECT * FROM rol ORDER BY id_rol;

-- 2. Verificar estructura de CUENTA
SELECT '=== ESTRUCTURA TABLA CUENTA ===' AS '';
DESCRIBE cuenta;

-- 3. Verificar estructura de PERSONA
SELECT '=== ESTRUCTURA TABLA PERSONA ===' AS '';
DESCRIBE persona;

-- 4. Verificar estructura de PERSONAL_SEGURIDAD
SELECT '=== ESTRUCTURA TABLA PERSONAL_SEGURIDAD ===' AS '';
DESCRIBE personal_seguridad;

-- 5. Verificar estructura de AUDITORIA
SELECT '=== ESTRUCTURA TABLA AUDITORIA ===' AS '';
DESCRIBE auditoria;

-- 6. Verificar claves foráneas de CUENTA
SELECT '=== CLAVES FORÁNEAS DE CUENTA ===' AS '';
SELECT 
    CONSTRAINT_NAME, 
    COLUMN_NAME, 
    REFERENCED_TABLE_NAME, 
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_NAME = 'cuenta' 
  AND TABLE_SCHEMA = 'sena'
  AND REFERENCED_TABLE_NAME IS NOT NULL;

-- 7. Verificar que no haya registros huérfanos
SELECT '=== VERIFICAR INTEGRIDAD REFERENCIAL ===' AS '';

SELECT 'Cuentas sin rol válido:' AS verificacion, COUNT(*) AS cantidad
FROM cuenta c 
LEFT JOIN rol r ON c.id_rol = r.id_rol 
WHERE r.id_rol IS NULL;

SELECT 'Cuentas sin persona válida:' AS verificacion, COUNT(*) AS cantidad
FROM cuenta c 
LEFT JOIN persona p ON c.num_doc = p.num_doc 
WHERE p.num_doc IS NULL;

SELECT 'Personal seguridad sin persona:' AS verificacion, COUNT(*) AS cantidad
FROM personal_seguridad ps 
LEFT JOIN persona p ON ps.num_doc = p.num_doc 
WHERE p.num_doc IS NULL;
```

## Solución Rápida (Si el rol 3 no existe)

```sql
USE sena;

-- Insertar rol para Personal de Seguridad/Guarda si no existe
INSERT IGNORE INTO rol (id_rol, nom_rol) 
VALUES (3, 'Guarda');

-- Verificar
SELECT * FROM rol;
```

## Después de Aplicar la Solución

1. Reinicia la aplicación SAIA-Admin
2. Intenta registrar un nuevo guarda
3. Observa los logs en la consola
4. El mensaje debería ser: `[PersonalSeguridadService] ═══ REGISTRO EXITOSO ═══`

## Contacto

Si después de seguir estos pasos el error persiste:
1. Copia TODOS los logs de la consola desde `═══ INICIO REGISTRO ═══` hasta el final
2. Copia el resultado del script de verificación de base de datos
3. Proporciona esa información para diagnóstico adicional
