-- ══════════════════════════════════════════════════════════════════════════════
-- SCRIPT: Relación Fichas de Formación con Cuentas de Aprendices
-- ══════════════════════════════════════════════════════════════════════════════
-- Base de datos: sena
-- Objetivo: Establecer relación entre fichas y aprendices (rol=1)
-- Límite: Máximo 30 aprendices por ficha
-- ══════════════════════════════════════════════════════════════════════════════

USE sena;

-- ══════════════════════════════════════════════════════════════════════════════
-- PASO 1: Verificar estructura actual de las tablas
-- ══════════════════════════════════════════════════════════════════════════════

-- Ver estructura de tabla ficha
DESCRIBE ficha;

-- Ver estructura de tabla cuenta
DESCRIBE cuenta;

-- Ver estructura de tabla persona
DESCRIBE persona;

-- ══════════════════════════════════════════════════════════════════════════════
-- PASO 2: Crear tabla de relación (si no existe)
-- ══════════════════════════════════════════════════════════════════════════════

-- Opción A: Si la tabla ficha NO tiene columna num_doc
-- Creamos una tabla intermedia para la relación muchos-a-muchos
-- (Una ficha tiene muchos aprendices, un aprendiz puede estar en UNA ficha)

CREATE TABLE IF NOT EXISTS ficha_aprendiz (
    id_ficha_aprendiz INT AUTO_INCREMENT PRIMARY KEY,
    id_ficha INT NOT NULL,
    id_cuenta INT NOT NULL,
    fecha_asignacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado ENUM('ACTIVO', 'INACTIVO', 'RETIRADO') DEFAULT 'ACTIVO',
    
    -- Claves foráneas
    FOREIGN KEY (id_ficha) REFERENCES ficha(id_ficha) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE,
    FOREIGN KEY (id_cuenta) REFERENCES cuenta(id_cuenta) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE,
    
    -- Un aprendiz solo puede estar en UNA ficha a la vez (activo)
    UNIQUE KEY unique_aprendiz_activo (id_cuenta, estado),
    
    -- Índices para mejorar rendimiento
    INDEX idx_ficha (id_ficha),
    INDEX idx_cuenta (id_cuenta),
    INDEX idx_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ══════════════════════════════════════════════════════════════════════════════
-- PASO 3: Crear trigger para validar límite de 30 aprendices por ficha
-- ══════════════════════════════════════════════════════════════════════════════

DELIMITER $$

DROP TRIGGER IF EXISTS before_insert_ficha_aprendiz$$

CREATE TRIGGER before_insert_ficha_aprendiz
BEFORE INSERT ON ficha_aprendiz
FOR EACH ROW
BEGIN
    DECLARE contador INT;
    DECLARE id_rol_aprendiz INT;
    
    -- Verificar que la cuenta sea de un aprendiz (rol = 1)
    SELECT id_rol INTO id_rol_aprendiz 
    FROM cuenta 
    WHERE id_cuenta = NEW.id_cuenta;
    
    IF id_rol_aprendiz IS NULL THEN
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'La cuenta no existe';
    END IF;
    
    IF id_rol_aprendiz != 1 THEN
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'Solo se pueden asignar cuentas de aprendices (rol=1) a fichas';
    END IF;
    
    -- Contar aprendices activos en la ficha
    SELECT COUNT(*) INTO contador 
    FROM ficha_aprendiz 
    WHERE id_ficha = NEW.id_ficha 
      AND estado = 'ACTIVO';
    
    -- Validar límite de 30 aprendices
    IF contador >= 30 THEN
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'La ficha ha alcanzado el límite máximo de 30 aprendices activos';
    END IF;
END$$

DELIMITER ;

-- ══════════════════════════════════════════════════════════════════════════════
-- PASO 4: Crear vista para consultas simplificadas
-- ══════════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE VIEW vista_ficha_aprendices AS
SELECT 
    fa.id_ficha_aprendiz,
    fa.id_ficha,
    f.nom_ficha,
    f.jornada,
    f.fecha_inicio,
    f.fecha_fin,
    fa.id_cuenta,
    c.num_doc,
    p.tip_doc,
    p.nombres,
    CONCAT(p.p_ape, IFNULL(CONCAT(' ', p.s_ape), '')) AS apellidos,
    p.email,
    c.estado AS cuenta_activa,
    fa.estado AS estado_en_ficha,
    fa.fecha_asignacion,
    pf.nom_prog AS programa,
    pf.siglas_prog,
    cf.nom_centro AS centro,
    cf.siglas_centro,
    -- Contador de aprendices en la ficha
    (SELECT COUNT(*) 
     FROM ficha_aprendiz fa2 
     WHERE fa2.id_ficha = fa.id_ficha 
       AND fa2.estado = 'ACTIVO') AS total_aprendices_activos
FROM ficha_aprendiz fa
INNER JOIN ficha f ON fa.id_ficha = f.id_ficha
INNER JOIN cuenta c ON fa.id_cuenta = c.id_cuenta
INNER JOIN persona p ON c.num_doc = p.num_doc
LEFT JOIN programa_formacion pf ON f.id_programa = pf.id_programa
LEFT JOIN centro_formacion cf ON pf.id_centro = cf.id_centro
WHERE c.id_rol = 1; -- Solo aprendices

-- ══════════════════════════════════════════════════════════════════════════════
-- PASO 5: Procedimientos almacenados para operaciones comunes
-- ══════════════════════════════════════════════════════════════════════════════

-- Procedimiento para asignar aprendiz a ficha
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_asignar_aprendiz_ficha$$

CREATE PROCEDURE sp_asignar_aprendiz_ficha(
    IN p_id_ficha INT,
    IN p_id_cuenta INT
)
BEGIN
    DECLARE v_contador INT;
    DECLARE v_existe INT;
    
    -- Verificar si el aprendiz ya está en una ficha activa
    SELECT COUNT(*) INTO v_existe 
    FROM ficha_aprendiz 
    WHERE id_cuenta = p_id_cuenta 
      AND estado = 'ACTIVO';
    
    IF v_existe > 0 THEN
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'El aprendiz ya está asignado a una ficha activa';
    END IF;
    
    -- Contar aprendices en la ficha
    SELECT COUNT(*) INTO v_contador 
    FROM ficha_aprendiz 
    WHERE id_ficha = p_id_ficha 
      AND estado = 'ACTIVO';
    
    IF v_contador >= 30 THEN
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'La ficha ha alcanzado el límite de 30 aprendices';
    END IF;
    
    -- Insertar relación
    INSERT INTO ficha_aprendiz (id_ficha, id_cuenta, estado) 
    VALUES (p_id_ficha, p_id_cuenta, 'ACTIVO');
    
    SELECT 'Aprendiz asignado exitosamente' AS mensaje;
END$$

-- Procedimiento para retirar aprendiz de ficha
DROP PROCEDURE IF EXISTS sp_retirar_aprendiz_ficha$$

CREATE PROCEDURE sp_retirar_aprendiz_ficha(
    IN p_id_cuenta INT
)
BEGIN
    UPDATE ficha_aprendiz 
    SET estado = 'RETIRADO' 
    WHERE id_cuenta = p_id_cuenta 
      AND estado = 'ACTIVO';
    
    IF ROW_COUNT() = 0 THEN
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'El aprendiz no está asignado a ninguna ficha activa';
    END IF;
    
    SELECT 'Aprendiz retirado exitosamente' AS mensaje;
END$$

-- Procedimiento para transferir aprendiz a otra ficha
DROP PROCEDURE IF EXISTS sp_transferir_aprendiz_ficha$$

CREATE PROCEDURE sp_transferir_aprendiz_ficha(
    IN p_id_cuenta INT,
    IN p_id_ficha_nueva INT
)
BEGIN
    DECLARE v_contador INT;
    
    -- Contar aprendices en la nueva ficha
    SELECT COUNT(*) INTO v_contador 
    FROM ficha_aprendiz 
    WHERE id_ficha = p_id_ficha_nueva 
      AND estado = 'ACTIVO';
    
    IF v_contador >= 30 THEN
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'La ficha destino ha alcanzado el límite de 30 aprendices';
    END IF;
    
    -- Inactivar asignación actual
    UPDATE ficha_aprendiz 
    SET estado = 'INACTIVO' 
    WHERE id_cuenta = p_id_cuenta 
      AND estado = 'ACTIVO';
    
    -- Crear nueva asignación
    INSERT INTO ficha_aprendiz (id_ficha, id_cuenta, estado) 
    VALUES (p_id_ficha_nueva, p_id_cuenta, 'ACTIVO');
    
    SELECT 'Aprendiz transferido exitosamente' AS mensaje;
END$$

DELIMITER ;

-- ══════════════════════════════════════════════════════════════════════════════
-- PASO 6: Consultas útiles para verificación y mantenimiento
-- ══════════════════════════════════════════════════════════════════════════════

-- Ver todas las fichas con su conteo de aprendices
SELECT 
    f.id_ficha,
    f.nom_ficha,
    f.jornada,
    pf.nom_prog AS programa,
    COUNT(fa.id_cuenta) AS total_aprendices,
    30 - COUNT(fa.id_cuenta) AS cupos_disponibles
FROM ficha f
LEFT JOIN ficha_aprendiz fa ON f.id_ficha = fa.id_ficha AND fa.estado = 'ACTIVO'
LEFT JOIN programa_formacion pf ON f.id_programa = pf.id_programa
GROUP BY f.id_ficha, f.nom_ficha, f.jornada, pf.nom_prog
ORDER BY total_aprendices DESC;

-- Ver aprendices sin ficha asignada
SELECT 
    c.id_cuenta,
    p.num_doc,
    p.nombres,
    CONCAT(p.p_ape, IFNULL(CONCAT(' ', p.s_ape), '')) AS apellidos,
    p.email
FROM cuenta c
INNER JOIN persona p ON c.num_doc = p.num_doc
WHERE c.id_rol = 1  -- Aprendices
  AND c.estado = TRUE  -- Activos
  AND NOT EXISTS (
      SELECT 1 
      FROM ficha_aprendiz fa 
      WHERE fa.id_cuenta = c.id_cuenta 
        AND fa.estado = 'ACTIVO'
  )
ORDER BY p.p_ape, p.nombres;

-- Ver fichas que están llegando al límite (más de 25 aprendices)
SELECT 
    f.id_ficha,
    f.nom_ficha,
    pf.nom_prog,
    COUNT(fa.id_cuenta) AS total_aprendices,
    30 - COUNT(fa.id_cuenta) AS cupos_disponibles
FROM ficha f
INNER JOIN ficha_aprendiz fa ON f.id_ficha = fa.id_ficha AND fa.estado = 'ACTIVO'
LEFT JOIN programa_formacion pf ON f.id_programa = pf.id_programa
GROUP BY f.id_ficha, f.nom_ficha, pf.nom_prog
HAVING total_aprendices >= 25
ORDER BY total_aprendices DESC;

-- ══════════════════════════════════════════════════════════════════════════════
-- PASO 7: Migración de datos existentes (SI APLICA)
-- ══════════════════════════════════════════════════════════════════════════════

-- Si actualmente tienes una columna num_doc en ficha que relaciona directamente:
-- Migrar datos existentes a la nueva tabla de relación

-- NOTA: Ejecutar solo SI tienes datos previos que migrar
-- Descomentar las siguientes líneas si es necesario:

/*
INSERT INTO ficha_aprendiz (id_ficha, id_cuenta, estado)
SELECT 
    f.id_ficha,
    c.id_cuenta,
    CASE 
        WHEN c.estado = TRUE THEN 'ACTIVO'
        ELSE 'INACTIVO'
    END
FROM ficha f
INNER JOIN persona p ON f.num_doc = p.num_doc
INNER JOIN cuenta c ON p.num_doc = c.num_doc
WHERE c.id_rol = 1  -- Solo aprendices
  AND f.num_doc IS NOT NULL;

-- Después de migrar, puedes eliminar la columna num_doc de ficha si ya no la necesitas:
-- ALTER TABLE ficha DROP COLUMN num_doc;
*/

-- ══════════════════════════════════════════════════════════════════════════════
-- PASO 8: Ejemplos de uso
-- ══════════════════════════════════════════════════════════════════════════════

-- Ejemplo 1: Asignar aprendiz a ficha
-- CALL sp_asignar_aprendiz_ficha(1, 5);  -- id_ficha=1, id_cuenta=5

-- Ejemplo 2: Ver aprendices de una ficha específica
-- SELECT * FROM vista_ficha_aprendices WHERE id_ficha = 1;

-- Ejemplo 3: Retirar aprendiz de ficha
-- CALL sp_retirar_aprendiz_ficha(5);  -- id_cuenta=5

-- Ejemplo 4: Transferir aprendiz a otra ficha
-- CALL sp_transferir_aprendiz_ficha(5, 2);  -- id_cuenta=5 a id_ficha=2

-- ══════════════════════════════════════════════════════════════════════════════
-- FIN DEL SCRIPT
-- ══════════════════════════════════════════════════════════════════════════════
