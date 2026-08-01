# ✅ Verificación e Instalación del Sistema Fichas-Aprendices

## 📋 Estado Actual

✅ **Código Java completado:**
- `FichaAprendiz.java` - Modelo de datos
- `FichaAprendizDAO.java` - Acceso a datos (SQL_INSERT no utilizado eliminado)
- `FichaAprendizService.java` - Lógica de negocio
- `AprendicesPanel.java` - **ACTUALIZADO** con columna "Ficha Asignada"

🔲 **Pendiente: Ejecutar script SQL en base de datos**

---

## 🚀 PASO 1: Ejecutar Script SQL

### Opción A: Desde MySQL Workbench (Recomendado)

1. Abre MySQL Workbench
2. Conéctate a tu servidor MySQL
3. Selecciona la base de datos `sena`
4. Abre el archivo: `SQL_RELACION_FICHA_APRENDIZ.sql`
5. Ejecuta todo el script (Ctrl+Shift+Enter)

### Opción B: Desde línea de comandos

```cmd
cd c:\Users\tecsi\OneDrive\Documentos\java\saia-admin
mysql -u root -p sena < SQL_RELACION_FICHA_APRENDIZ.sql
```

---

## ✅ PASO 2: Verificar Instalación

Ejecuta estas consultas en MySQL Workbench para verificar:

### 2.1. Verificar que la tabla se creó correctamente

```sql
USE sena;
DESCRIBE ficha_aprendiz;
```

**Resultado esperado:**
```
+------------------+--------------------------------------+------+-----+-------------------+
| Field            | Type                                 | Null | Key | Default           |
+------------------+--------------------------------------+------+-----+-------------------+
| id_ficha_aprendiz| int                                  | NO   | PRI | NULL              |
| id_ficha         | int                                  | NO   | MUL | NULL              |
| id_cuenta        | int                                  | NO   | MUL | NULL              |
| fecha_asignacion | timestamp                            | YES  |     | CURRENT_TIMESTAMP |
| estado           | enum('ACTIVO','INACTIVO','RETIRADO') | YES  |     | ACTIVO            |
+------------------+--------------------------------------+------+-----+-------------------+
```

### 2.2. Verificar que la vista existe

```sql
DESCRIBE vista_ficha_aprendices;
```

### 2.3. Verificar que los procedimientos existen

```sql
SHOW PROCEDURE STATUS WHERE Db = 'sena' 
AND Name IN ('sp_asignar_aprendiz_ficha', 'sp_retirar_aprendiz_ficha', 'sp_transferir_aprendiz_ficha');
```

**Resultado esperado:** 3 procedimientos

### 2.4. Verificar que el trigger existe

```sql
SHOW TRIGGERS FROM sena WHERE `Trigger` = 'before_insert_ficha_aprendiz';
```

---

## 🧪 PASO 3: Pruebas de Funcionalidad

### Prueba 1: Ver fichas con cupos disponibles

```sql
SELECT 
    f.id_ficha,
    f.nom_ficha,
    COUNT(fa.id_cuenta) AS ocupados,
    30 - COUNT(fa.id_cuenta) AS cupos
FROM ficha f
LEFT JOIN ficha_aprendiz fa ON f.id_ficha = fa.id_ficha AND fa.estado = 'ACTIVO'
GROUP BY f.id_ficha, f.nom_ficha
ORDER BY cupos DESC;
```

### Prueba 2: Ver aprendices sin ficha

```sql
SELECT 
    c.id_cuenta,
    p.num_doc,
    p.nombres,
    CONCAT(p.p_ape, IFNULL(CONCAT(' ', p.s_ape), '')) AS apellidos
FROM cuenta c
INNER JOIN persona p ON c.num_doc = p.num_doc
WHERE c.id_rol = 1 
  AND c.estado = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM ficha_aprendiz fa 
      WHERE fa.id_cuenta = c.id_cuenta AND fa.estado = 'ACTIVO'
  )
LIMIT 10;
```

### Prueba 3: Asignar un aprendiz a una ficha (EJEMPLO)

```sql
-- Reemplaza 1 con id_ficha real y 5 con id_cuenta real
CALL sp_asignar_aprendiz_ficha(1, 5);
```

Si la asignación es exitosa, verás:
```
+----------------------------------+
| mensaje                          |
+----------------------------------+
| Aprendiz asignado exitosamente   |
+----------------------------------+
```

### Prueba 4: Validar límite de 30 aprendices

Si intentas asignar el aprendiz 31 a una ficha llena:

```sql
-- Esto debe FALLAR con error
CALL sp_asignar_aprendiz_ficha(1, 999);
```

**Error esperado:** `La ficha ha alcanzado el límite de 30 aprendices`

---

## 🔧 PASO 4: Compilar y Probar en Java

### 4.1. Compilar el proyecto

```cmd
cd c:\Users\tecsi\OneDrive\Documentos\java\saia-admin
mvn clean compile
```

O si usas IDE:
- En NetBeans: Clean and Build
- En Eclipse: Project > Clean
- En IntelliJ IDEA: Build > Rebuild Project

### 4.2. Ejecutar la aplicación

Inicia la aplicación y verifica:

✅ **Panel Aprendices:** Debe mostrar nueva columna "Ficha Asignada"
- Aprendices sin ficha: "Sin asignar"
- Aprendices con ficha: Nombre de la ficha

---

## 📊 PASO 5: Verificar Integración Visual

### Antes de asignar fichas:

| # | Nombres | Apellidos | N° Ficha | **Ficha Asignada** | Estado |
|---|---------|-----------|----------|-------------------|--------|
| 1 | Juan    | Pérez     | 2401     | **Sin asignar**   | Activa |
| 2 | María   | García    | 2401     | **Sin asignar**   | Activa |

### Después de asignar fichas (desde SQL):

```sql
-- Ejemplo: Asignar varios aprendices a ficha 1
CALL sp_asignar_aprendiz_ficha(1, 10);  -- id_cuenta = 10
CALL sp_asignar_aprendiz_ficha(1, 11);  -- id_cuenta = 11
CALL sp_asignar_aprendiz_ficha(1, 12);  -- id_cuenta = 12
```

| # | Nombres | Apellidos | N° Ficha | **Ficha Asignada** | Estado |
|---|---------|-----------|----------|-------------------|--------|
| 1 | Juan    | Pérez     | 2401     | **ADSO-2401**     | Activa |
| 2 | María   | García    | 2401     | **ADSO-2401**     | Activa |

---

## 🎯 Gestión de Fichas

Las fichas se gestionan **directamente desde SQL** usando los procedimientos almacenados:

### Asignar aprendiz a ficha:
```sql
CALL sp_asignar_aprendiz_ficha(<id_ficha>, <id_cuenta>);
```

### Retirar aprendiz de ficha:
```sql
CALL sp_retirar_aprendiz_ficha(<id_cuenta>);
```

### Transferir aprendiz a otra ficha:
```sql
CALL sp_transferir_aprendiz_ficha(<id_cuenta>, <id_ficha_nueva>);
```

La columna **"Ficha Asignada"** en el panel de Aprendices se actualiza automáticamente cada 5 segundos.

---

## 🐛 Solución de Problemas

### Error: "Table 'sena.ficha_aprendiz' doesn't exist"

**Causa:** No se ejecutó el script SQL.

**Solución:** Ejecuta `SQL_RELACION_FICHA_APRENDIZ.sql`

### Error: "Cannot add or update a child row: a foreign key constraint fails"

**Causa:** El `id_ficha` o `id_cuenta` no existe.

**Solución:** Verifica que existan registros:

```sql
-- Ver fichas disponibles
SELECT id_ficha, nom_ficha FROM ficha LIMIT 10;

-- Ver cuentas de aprendices disponibles
SELECT id_cuenta, num_doc FROM cuenta WHERE id_rol = 1 LIMIT 10;
```

### Error: "Unknown column 'estado_en_ficha' in vista_ficha_aprendices"

**Causa:** La vista no se creó correctamente.

**Solución:** Recrea la vista manualmente:

```sql
DROP VIEW IF EXISTS vista_ficha_aprendices;

CREATE VIEW vista_ficha_aprendices AS
SELECT 
    fa.id_ficha_aprendiz,
    fa.id_ficha,
    f.nom_ficha,
    fa.id_cuenta,
    p.num_doc,
    p.nombres,
    CONCAT(p.p_ape, IFNULL(CONCAT(' ', p.s_ape), '')) AS apellidos,
    fa.estado AS estado_en_ficha,
    fa.fecha_asignacion,
    (SELECT COUNT(*) FROM ficha_aprendiz fa2 
     WHERE fa2.id_ficha = fa.id_ficha AND fa2.estado = 'ACTIVO') AS total_aprendices_activos
FROM ficha_aprendiz fa
INNER JOIN ficha f ON fa.id_ficha = f.id_ficha
INNER JOIN cuenta c ON fa.id_cuenta = c.id_cuenta
INNER JOIN persona p ON c.num_doc = p.num_doc
WHERE c.id_rol = 1;
```

### Columna "Ficha Asignada" muestra "Sin asignar" para todos

**Causa:** Los aprendices aún no están asignados a fichas.

**Solución:** Asigna aprendices usando:

```sql
CALL sp_asignar_aprendiz_ficha(<id_ficha>, <id_cuenta>);
```

---

## 📝 Resumen de Cambios

### Archivos Nuevos
- ✅ `SQL_RELACION_FICHA_APRENDIZ.sql` - Script de base de datos
- ✅ `GUIA_FICHAS_APRENDICES.md` - Documentación completa
- ✅ `FichaAprendiz.java` - Modelo de datos
- ✅ `FichaAprendizDAO.java` - Acceso a datos
- ✅ `FichaAprendizService.java` - Lógica de negocio
- ✅ `VERIFICACION_FICHAS_APRENDICES.md` - Este documento

### Archivos Modificados
- ✅ `AprendicesPanel.java` - Agregada columna "Ficha Asignada"

### Base de Datos
- 🔲 Tabla `ficha_aprendiz` (pendiente de crear)
- 🔲 Vista `vista_ficha_aprendices` (pendiente de crear)
- 🔲 Trigger `before_insert_ficha_aprendiz` (pendiente de crear)
- 🔲 Procedimientos almacenados (pendiente de crear)

---

## ✅ Checklist de Verificación

Marca cada item cuando lo completes:

- [ ] Script SQL ejecutado sin errores
- [ ] Tabla `ficha_aprendiz` existe
- [ ] Vista `vista_ficha_aprendices` existe
- [ ] Trigger `before_insert_ficha_aprendiz` existe
- [ ] 3 procedimientos almacenados creados
- [ ] Proyecto Java compilado sin errores
- [ ] Aplicación inicia correctamente
- [ ] Panel Aprendices muestra columna "Ficha Asignada"
- [ ] Al asignar ficha desde SQL, se refleja en la aplicación
- [ ] Límite de 30 aprendices funciona correctamente

---

**¿Necesitas ayuda?** Consulta `GUIA_FICHAS_APRENDICES.md` para ejemplos de uso desde Java.

**Fecha:** Agosto 2026  
**Versión:** 1.0.0
