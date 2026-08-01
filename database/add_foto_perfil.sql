-- Script SQL para agregar el campo foto_perfil a la tabla persona
-- Fecha: 2024
-- Propósito: Permitir almacenar la ruta de la foto de perfil del personal de seguridad

-- Agregar columna foto_perfil a la tabla persona
ALTER TABLE persona 
ADD COLUMN foto_perfil VARCHAR(500) NULL 
COMMENT 'Ruta relativa a la foto de perfil del usuario';

-- Crear índice para búsquedas rápidas (opcional, pero recomendado)
CREATE INDEX idx_persona_foto ON persona(foto_perfil);

-- Verificar la estructura de la tabla
DESCRIBE persona;

-- Mensaje de confirmación
SELECT 'Columna foto_perfil agregada exitosamente a la tabla persona' AS resultado;
