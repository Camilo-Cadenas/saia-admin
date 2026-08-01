-- ══════════════════════════════════════════════════════════════════════════════
-- SCRIPT SIMPLE: Tabla de relación Fichas-Aprendices
-- ══════════════════════════════════════════════════════════════════════════════
-- Base de datos: sena
-- Objetivo: Crear solo la tabla ficha_aprendiz
-- Límite: Máximo 30 aprendices por ficha (validado por trigger)
-- ══════════════════════════════════════════════════════════════════════════════

USE sena;

-- ══════════════════════════════════════════════════════════════════════════════
-- Crear tabla de relación
-- ══════════════════════════════════════════════════════════════════════════════

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
    
    -- Índices
    INDEX idx_ficha (id_ficha),
    INDEX idx_cuenta (id_cuenta),
    INDEX idx_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ══════════════════════════════════════════════════════════════════════════════
-- Trigger para validar límite de 30 aprendices
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
-- Listo! Ahora puedes insertar relaciones
-- ══════════════════════════════════════════════════════════════════════════════

-- Ejemplo de uso:
-- INSERT INTO ficha_aprendiz (id_ficha, id_cuenta, estado) VALUES (1, 10, 'ACTIVO');

-- Ver aprendices de una ficha:
-- SELECT * FROM ficha_aprendiz WHERE id_ficha = 1 AND estado = 'ACTIVO';

-- Ver cuántos aprendices tiene cada ficha:
-- SELECT id_ficha, COUNT(*) as total 
-- FROM ficha_aprendiz 
-- WHERE estado = 'ACTIVO' 
-- GROUP BY id_ficha;
