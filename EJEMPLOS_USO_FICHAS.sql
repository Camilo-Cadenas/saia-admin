-- ══════════════════════════════════════════════════════════════════════════════
-- EJEMPLOS DE USO: Sistema Fichas-Aprendices
-- ══════════════════════════════════════════════════════════════════════════════
-- Base de datos: sena
-- Requisito: Haber ejecutado SQL_RELACION_FICHA_APRENDIZ.sql primero
-- ══════════════════════════════════════════════════════════════════════════════

USE sena;

-- ══════════════════════════════════════════════════════════════════════════════
-- CONSULTAS BÁSICAS DE EXPLORACIÓN
-- ══════════════════════════════════════════════════════════════════════════════

-- 1. Ver todas las fichas disponibles
SELECT id_ficha, nom_ficha, jornada, fecha_inicio, fecha_fin 
FROM ficha 
ORDER BY nom_ficha;

-- 2. Ver todos los aprendices (cuentas con rol=1)
SELECT 
    c.id_cuenta,
    p.num_doc,
    p.nombres,
    CONCAT(p.p_ape, IFNULL(CONCAT(' ', p.s_ape), '')) AS apellidos,
    c.estado AS cuenta_activa
FROM cuenta c
INNER JOIN persona p ON c.num_doc = p.num_doc
WHERE c.id_rol = 1
ORDER BY p.p_ape, p.nombres;

-- 3. Ver cuántos aprendices tiene cada ficha
SELECT 
    f.id_ficha,
    f.nom_ficha,
    COUNT(fa.id_cuenta) AS aprendices_activos,
    30 - COUNT(fa.id_cuenta) AS cupos_disponibles,
    ROUND((COUNT(fa.id_cuenta) * 100.0 / 30), 1) AS porcentaje_ocupado
FROM ficha f
LEFT JOIN ficha_aprendiz fa ON f.id_ficha = fa.id_ficha AND fa.estado = 'ACTIVO'
GROUP BY f.id_ficha, f.nom_ficha
ORDER BY aprendices_activos DESC;

-- 4. Ver aprendices sin ficha asignada
SELECT 
    c.id_cuenta,
    p.num_doc,
    p.nombres,
    CONCAT(p.p_ape, IFNULL(CONCAT(' ', p.s_ape), '')) AS apellidos,
    p.email
FROM cuenta c
INNER JOIN persona p ON c.num_doc = p.num_doc
WHERE c.id_rol = 1 
  AND c.estado = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM ficha_aprendiz fa 
      WHERE fa.id_cuenta = c.id_cuenta AND fa.estado = 'ACTIVO'
  )
ORDER BY p.p_ape, p.nombres;

-- ══════════════════════════════════════════════════════════════════════════════
-- ASIGNAR APRENDICES A FICHAS
-- ══════════════════════════════════════════════════════════════════════════════

-- Ejemplo 1: Asignar un aprendiz a una ficha
-- Reemplaza los valores con IDs reales de tu base de datos
CALL sp_asignar_aprendiz_ficha(1, 10);  
-- Parámetros: id_ficha=1, id_cuenta=10

-- Ejemplo 2: Asignar múltiples aprendices a la misma ficha
CALL sp_asignar_aprendiz_ficha(1, 10);
CALL sp_asignar_aprendiz_ficha(1, 11);
CALL sp_asignar_aprendiz_ficha(1, 12);
CALL sp_asignar_aprendiz_ficha(1, 13);
CALL sp_asignar_aprendiz_ficha(1, 14);

-- Ejemplo 3: Verificar la asignación
SELECT * FROM vista_ficha_aprendices 
WHERE id_ficha = 1 
  AND estado_en_ficha = 'ACTIVO'
ORDER BY apellidos, nombres;

-- ══════════════════════════════════════════════════════════════════════════════
-- GESTIONAR APRENDICES ASIGNADOS
-- ══════════════════════════════════════════════════════════════════════════════

-- Ejemplo 4: Ver la ficha de un aprendiz específico
SELECT * FROM vista_ficha_aprendices 
WHERE id_cuenta = 10 
  AND estado_en_ficha = 'ACTIVO';

-- Ejemplo 5: Retirar un aprendiz de su ficha
CALL sp_retirar_aprendiz_ficha(10);  -- id_cuenta=10

-- Ejemplo 6: Transferir un aprendiz a otra ficha
CALL sp_transferir_aprendiz_ficha(10, 2);  
-- Parámetros: id_cuenta=10, id_ficha_nueva=2

-- ══════════════════════════════════════════════════════════════════════════════
-- CONSULTAS AVANZADAS
-- ══════════════════════════════════════════════════════════════════════════════

-- Ejemplo 7: Ver fichas con cupos disponibles (más de 5 cupos)
SELECT 
    f.id_ficha,
    f.nom_ficha,
    f.jornada,
    COUNT(fa.id_cuenta) AS ocupados,
    30 - COUNT(fa.id_cuenta) AS cupos
FROM ficha f
LEFT JOIN ficha_aprendiz fa ON f.id_ficha = fa.id_ficha AND fa.estado = 'ACTIVO'
GROUP BY f.id_ficha, f.nom_ficha, f.jornada
HAVING cupos >= 5
ORDER BY cupos DESC;

-- Ejemplo 8: Ver fichas que están llegando al límite (más de 25 aprendices)
SELECT 
    f.id_ficha,
    f.nom_ficha,
    COUNT(fa.id_cuenta) AS ocupados,
    30 - COUNT(fa.id_cuenta) AS cupos_restantes
FROM ficha f
INNER JOIN ficha_aprendiz fa ON f.id_ficha = fa.id_ficha AND fa.estado = 'ACTIVO'
GROUP BY f.id_ficha, f.nom_ficha
HAVING ocupados >= 25
ORDER BY ocupados DESC;

-- Ejemplo 9: Ver historial completo de un aprendiz (todas sus asignaciones)
SELECT 
    fa.id_ficha_aprendiz,
    f.nom_ficha,
    fa.estado,
    fa.fecha_asignacion
FROM ficha_aprendiz fa
INNER JOIN ficha f ON fa.id_ficha = f.id_ficha
WHERE fa.id_cuenta = 10  -- Reemplaza con id_cuenta real
ORDER BY fa.fecha_asignacion DESC;

-- Ejemplo 10: Ver estadísticas generales del sistema
SELECT 
    (SELECT COUNT(*) FROM ficha) AS total_fichas,
    (SELECT COUNT(*) FROM cuenta WHERE id_rol = 1 AND estado = TRUE) AS total_aprendices_activos,
    (SELECT COUNT(*) FROM ficha_aprendiz WHERE estado = 'ACTIVO') AS aprendices_asignados,
    (SELECT COUNT(*) FROM cuenta c 
     WHERE c.id_rol = 1 AND c.estado = TRUE
       AND NOT EXISTS (
           SELECT 1 FROM ficha_aprendiz fa 
           WHERE fa.id_cuenta = c.id_cuenta AND fa.estado = 'ACTIVO'
       )
    ) AS aprendices_sin_ficha;

-- ══════════════════════════════════════════════════════════════════════════════
-- VALIDACIONES Y PRUEBAS
-- ══════════════════════════════════════════════════════════════════════════════

-- Prueba 1: Intentar asignar más de 30 aprendices (debe fallar)
-- Primero verifica cuántos tiene una ficha:
SELECT COUNT(*) FROM ficha_aprendiz WHERE id_ficha = 1 AND estado = 'ACTIVO';
-- Si ya tiene 30, este comando fallará con error:
-- CALL sp_asignar_aprendiz_ficha(1, 999);

-- Prueba 2: Intentar asignar un aprendiz que ya está en otra ficha (debe fallar)
-- CALL sp_asignar_aprendiz_ficha(1, 10);  -- Primera vez: OK
-- CALL sp_asignar_aprendiz_ficha(2, 10);  -- Segunda vez: FALLA (ya está asignado)

-- Prueba 3: Verificar que solo se pueden asignar aprendices (rol=1)
-- Esto fallará si id_cuenta no es de un aprendiz:
-- CALL sp_asignar_aprendiz_ficha(1, <id_cuenta_de_admin>);

-- ══════════════════════════════════════════════════════════════════════════════
-- REPORTES ÚTILES
-- ══════════════════════════════════════════════════════════════════════════════

-- Reporte 1: Fichas ordenadas por ocupación (más llenas primero)
SELECT 
    f.id_ficha,
    f.nom_ficha,
    f.jornada,
    COUNT(fa.id_cuenta) AS aprendices,
    30 - COUNT(fa.id_cuenta) AS cupos,
    CASE 
        WHEN COUNT(fa.id_cuenta) >= 25 THEN '⚠️ Casi llena'
        WHEN COUNT(fa.id_cuenta) >= 15 THEN '🟡 Media ocupación'
        ELSE '🟢 Disponible'
    END AS estado_ocupacion
FROM ficha f
LEFT JOIN ficha_aprendiz fa ON f.id_ficha = fa.id_ficha AND fa.estado = 'ACTIVO'
GROUP BY f.id_ficha, f.nom_ficha, f.jornada
ORDER BY aprendices DESC;

-- Reporte 2: Aprendices por ficha con información completa
SELECT 
    f.nom_ficha,
    f.jornada,
    p.num_doc,
    p.nombres,
    CONCAT(p.p_ape, IFNULL(CONCAT(' ', p.s_ape), '')) AS apellidos,
    p.email,
    fa.fecha_asignacion,
    DATEDIFF(CURDATE(), fa.fecha_asignacion) AS dias_en_ficha
FROM ficha_aprendiz fa
INNER JOIN ficha f ON fa.id_ficha = f.id_ficha
INNER JOIN cuenta c ON fa.id_cuenta = c.id_cuenta
INNER JOIN persona p ON c.num_doc = p.num_doc
WHERE fa.estado = 'ACTIVO'
ORDER BY f.nom_ficha, p.p_ape, p.nombres;

-- Reporte 3: Resumen por estado de asignación
SELECT 
    estado,
    COUNT(*) AS total,
    ROUND((COUNT(*) * 100.0 / (SELECT COUNT(*) FROM ficha_aprendiz)), 2) AS porcentaje
FROM ficha_aprendiz
GROUP BY estado;

-- ══════════════════════════════════════════════════════════════════════════════
-- MANTENIMIENTO
-- ══════════════════════════════════════════════════════════════════════════════

-- Limpiar asignaciones de aprendices con cuentas inactivas
UPDATE ficha_aprendiz fa
INNER JOIN cuenta c ON fa.id_cuenta = c.id_cuenta
SET fa.estado = 'INACTIVO'
WHERE c.estado = FALSE AND fa.estado = 'ACTIVO';

-- Ver cuántas asignaciones se limpiarían (sin ejecutar la limpieza)
SELECT COUNT(*) AS asignaciones_a_limpiar
FROM ficha_aprendiz fa
INNER JOIN cuenta c ON fa.id_cuenta = c.id_cuenta
WHERE c.estado = FALSE AND fa.estado = 'ACTIVO';

-- ══════════════════════════════════════════════════════════════════════════════
-- NOTAS IMPORTANTES
-- ══════════════════════════════════════════════════════════════════════════════

/*
1. Siempre reemplaza los IDs de ejemplo (1, 10, 999, etc.) con IDs reales de tu BD

2. Para encontrar IDs reales:
   - Fichas: SELECT id_ficha, nom_ficha FROM ficha;
   - Aprendices: SELECT id_cuenta, num_doc FROM cuenta WHERE id_rol = 1;

3. Los procedimientos almacenados validan automáticamente:
   - Límite de 30 aprendices por ficha
   - Solo aprendices (rol=1)
   - Un aprendiz solo en una ficha activa

4. Estados posibles:
   - ACTIVO: Aprendiz está actualmente en la ficha
   - INACTIVO: Asignación pausada temporalmente
   - RETIRADO: Aprendiz fue retirado permanentemente

5. La vista `vista_ficha_aprendices` incluye información completa (JOIN automático)
*/

-- ══════════════════════════════════════════════════════════════════════════════
-- FIN DE EJEMPLOS
-- ══════════════════════════════════════════════════════════════════════════════
