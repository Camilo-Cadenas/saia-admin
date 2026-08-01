# 🚀 Instrucciones Rápidas: Sistema Fichas-Aprendices

## ¿Qué hace este sistema?

Relaciona cuentas de aprendices (rol=1) con fichas de formación. Cada ficha puede tener **máximo 30 aprendices activos**.

---

## 📦 PASO 1: Instalar en Base de Datos

Ejecuta este script en MySQL Workbench o desde línea de comandos:

```cmd
cd c:\Users\tecsi\OneDrive\Documentos\java\saia-admin
mysql -u root -p sena < SQL_RELACION_FICHA_APRENDIZ.sql
```

O desde MySQL Workbench:
1. Abre el archivo `SQL_RELACION_FICHA_APRENDIZ.sql`
2. Selecciona la base de datos `sena`
3. Ejecuta todo (Ctrl+Shift+Enter)

---

## ✅ PASO 2: Verificar Instalación

```sql
USE sena;

-- Debe mostrar la estructura de la tabla
DESCRIBE ficha_aprendiz;

-- Debe mostrar 3 procedimientos
SHOW PROCEDURE STATUS WHERE Db = 'sena' 
AND Name LIKE '%ficha%';
```

---

## 🎯 PASO 3: Asignar Aprendices a Fichas

### Ver fichas disponibles:

```sql
SELECT id_ficha, nom_ficha FROM ficha;
```

### Ver aprendices sin ficha:

```sql
SELECT c.id_cuenta, p.num_doc, p.nombres, p.p_ape
FROM cuenta c
INNER JOIN persona p ON c.num_doc = p.num_doc
WHERE c.id_rol = 1 AND c.estado = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM ficha_aprendiz fa 
      WHERE fa.id_cuenta = c.id_cuenta AND fa.estado = 'ACTIVO'
  );
```

### Asignar aprendiz a ficha:

```sql
-- Ejemplo: Asignar aprendiz con id_cuenta=10 a ficha con id_ficha=1
CALL sp_asignar_aprendiz_ficha(1, 10);
```

### Ver cuántos aprendices tiene cada ficha:

```sql
SELECT 
    f.id_ficha,
    f.nom_ficha,
    COUNT(fa.id_cuenta) AS aprendices_activos,
    30 - COUNT(fa.id_cuenta) AS cupos_disponibles
FROM ficha f
LEFT JOIN ficha_aprendiz fa ON f.id_ficha = fa.id_ficha AND fa.estado = 'ACTIVO'
GROUP BY f.id_ficha, f.nom_ficha;
```

---

## 🖥️ PASO 4: Compilar y Ver en la Aplicación

```cmd
cd c:\Users\tecsi\OneDrive\Documentos\java\saia-admin
mvn clean compile
```

Luego ejecuta la aplicación y ve al panel **Aprendices**. Verás la columna **"Ficha Asignada"**:

- "Sin asignar" = Aprendiz no tiene ficha
- "ADSO-2401" = Aprendiz asignado a esa ficha

La columna se actualiza automáticamente cada 5 segundos.

---

## 📝 Comandos Útiles

### Asignar varios aprendices a una ficha:

```sql
CALL sp_asignar_aprendiz_ficha(1, 10);
CALL sp_asignar_aprendiz_ficha(1, 11);
CALL sp_asignar_aprendiz_ficha(1, 12);
```

### Retirar aprendiz de ficha:

```sql
CALL sp_retirar_aprendiz_ficha(10);  -- id_cuenta
```

### Transferir aprendiz a otra ficha:

```sql
CALL sp_transferir_aprendiz_ficha(10, 2);  -- id_cuenta, id_ficha_nueva
```

### Ver todos los aprendices de una ficha:

```sql
SELECT * FROM vista_ficha_aprendices WHERE id_ficha = 1;
```

---

## 🛡️ Validaciones Automáticas

El sistema valida automáticamente:

✅ **Límite de 30:** No puedes asignar más de 30 aprendices a una ficha  
✅ **Solo aprendices:** Solo cuentas con rol=1 pueden asignarse  
✅ **Una ficha activa:** Un aprendiz solo puede estar en UNA ficha activa  

---

## 🐛 Errores Comunes

### "La ficha ha alcanzado el límite de 30 aprendices"
La ficha está llena. Usa otra ficha o retira aprendices inactivos.

### "El aprendiz ya está asignado a una ficha activa"
El aprendiz ya está en otra ficha. Usa `sp_transferir_aprendiz_ficha` o primero retíralo.

### "Solo se pueden asignar cuentas de aprendices (rol=1)"
La cuenta no es de un aprendiz. Verifica con:
```sql
SELECT id_rol FROM cuenta WHERE id_cuenta = <id>;
```

---

## ✅ Resumen

1. **Ejecuta** `SQL_RELACION_FICHA_APRENDIZ.sql` en la BD `sena`
2. **Asigna fichas** usando `CALL sp_asignar_aprendiz_ficha(id_ficha, id_cuenta)`
3. **Compila** el proyecto Java
4. **Ve al panel Aprendices** - verás la columna "Ficha Asignada"

¡Listo! No necesitas crear ningún panel UI adicional.

---

**Archivos importantes:**
- `SQL_RELACION_FICHA_APRENDIZ.sql` - Script de base de datos
- `GUIA_FICHAS_APRENDICES.md` - Guía completa con ejemplos Java
- `VERIFICACION_FICHAS_APRENDICES.md` - Verificación paso a paso

**Fecha:** Agosto 2026
