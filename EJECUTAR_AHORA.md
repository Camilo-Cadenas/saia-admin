# 🚀 EJECUTAR AHORA - Sistema Fichas-Aprendices

## ✅ Código Java: COMPLETADO

Todo el código Java está listo y sin errores de compilación.

---

## 📋 LO QUE DEBES HACER AHORA

### 1️⃣ Ejecutar Script SQL (5 minutos)

Abre **MySQL Workbench** o **línea de comandos** y ejecuta:

```cmd
cd c:\Users\tecsi\OneDrive\Documentos\java\saia-admin
mysql -u root -p sena < SQL_RELACION_FICHA_APRENDIZ.sql
```

O desde MySQL Workbench:
1. Abrir archivo: `SQL_RELACION_FICHA_APRENDIZ.sql`
2. Seleccionar base de datos `sena`
3. Ejecutar todo (Ctrl+Shift+Enter)

✅ **Resultado esperado:** "Script ejecutado exitosamente"

---

### 2️⃣ Compilar Proyecto (2 minutos)

```cmd
cd c:\Users\tecsi\OneDrive\Documentos\java\saia-admin
mvn clean compile
```

O desde tu IDE: **Clean and Build**

✅ **Resultado esperado:** BUILD SUCCESS

---

### 3️⃣ Ejecutar Aplicación (1 minuto)

Inicia la aplicación SAIA y ve al panel **"Aprendices"**.

✅ **Resultado esperado:** Nueva columna **"Ficha Asignada"** visible (mostrará "Sin asignar" inicialmente)

---

### 4️⃣ Asignar Fichas desde SQL (5 minutos)

Ejecuta en MySQL Workbench:

```sql
USE sena;

-- Ver fichas disponibles
SELECT id_ficha, nom_ficha FROM ficha LIMIT 10;

-- Ver aprendices disponibles
SELECT c.id_cuenta, p.num_doc, p.nombres 
FROM cuenta c
INNER JOIN persona p ON c.num_doc = p.num_doc
WHERE c.id_rol = 1 AND c.estado = TRUE
LIMIT 10;

-- Asignar aprendices (reemplaza con IDs reales)
CALL sp_asignar_aprendiz_ficha(1, 10);  -- id_ficha=1, id_cuenta=10
CALL sp_asignar_aprendiz_ficha(1, 11);
CALL sp_asignar_aprendiz_ficha(1, 12);
```

✅ **Resultado esperado:** "Aprendiz asignado exitosamente"

---

### 5️⃣ Verificar en la Aplicación (30 segundos)

Haz clic en **"Actualizar"** en el panel Aprendices.

✅ **Resultado esperado:** Columna "Ficha Asignada" muestra el nombre de la ficha

---

## 📁 Archivos Importantes

### Para Ejecutar:
1. **SQL_RELACION_FICHA_APRENDIZ.sql** ← Ejecuta este primero
2. **EJEMPLOS_USO_FICHAS.sql** ← Ejemplos prácticos

### Para Consultar:
1. **INSTRUCCIONES_RAPIDAS_FICHAS.md** ← Guía rápida
2. **RESUMEN_SISTEMA_FICHAS.md** ← Resumen completo
3. **GUIA_FICHAS_APRENDICES.md** ← Guía detallada

---

## 🎯 Comandos SQL Esenciales

```sql
-- Ver ocupación de fichas
SELECT 
    f.nom_ficha,
    COUNT(fa.id_cuenta) AS ocupados,
    30 - COUNT(fa.id_cuenta) AS cupos
FROM ficha f
LEFT JOIN ficha_aprendiz fa ON f.id_ficha = fa.id_ficha AND fa.estado = 'ACTIVO'
GROUP BY f.id_ficha, f.nom_ficha;

-- Asignar aprendiz a ficha
CALL sp_asignar_aprendiz_ficha(<id_ficha>, <id_cuenta>);

-- Ver aprendices de una ficha
SELECT * FROM vista_ficha_aprendices WHERE id_ficha = <id>;

-- Retirar aprendiz
CALL sp_retirar_aprendiz_ficha(<id_cuenta>);
```

---

## ✅ Checklist

- [ ] Script SQL ejecutado sin errores
- [ ] Proyecto compilado sin errores
- [ ] Aplicación muestra columna "Ficha Asignada"
- [ ] Asignada al menos una ficha de prueba
- [ ] Columna se actualiza correctamente

---

## 🐛 Si Algo Sale Mal

**Problema:** Error al ejecutar script SQL  
**Solución:** Verifica que estés en la base de datos `sena`: `USE sena;`

**Problema:** Error de compilación  
**Solución:** Ejecuta `mvn clean` primero, luego `mvn compile`

**Problema:** Columna no aparece  
**Solución:** Verifica que AprendicesPanel.java se haya compilado correctamente

**Problema:** Muestra "Sin asignar" para todos  
**Solución:** Normal, debes asignar fichas desde SQL usando `sp_asignar_aprendiz_ficha`

---

## 📞 Ayuda

Consulta estos documentos según lo que necesites:

- **Guía rápida:** `INSTRUCCIONES_RAPIDAS_FICHAS.md`
- **Ejemplos SQL:** `EJEMPLOS_USO_FICHAS.sql`
- **Guía completa:** `GUIA_FICHAS_APRENDICES.md`
- **Verificación:** `VERIFICACION_FICHAS_APRENDICES.md`

---

**Tiempo estimado total: 15 minutos**

¡Listo! 🎉
