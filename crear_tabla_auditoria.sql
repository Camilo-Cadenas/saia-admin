-- ============================================================
--  SAIA - Tabla de auditoría de actividad
-- ============================================================
CREATE TABLE IF NOT EXISTS auditoria_actividad (
    id_actividad   INT          NOT NULL AUTO_INCREMENT,
    tipo_accion    VARCHAR(30)  NOT NULL,          -- CREAR, ACTUALIZAR, BLOQUEAR, etc.
    entidad        VARCHAR(50)  NOT NULL,          -- personal_seguridad, aprendiz, etc.
    num_doc        INT          NULL,              -- num_doc del afectado
    descripcion    VARCHAR(300) NOT NULL,          -- texto legible de la acción
    realizado_por  INT          NULL,              -- num_doc del admin que hizo la acción
    fecha_hora     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_actividad)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Índice para consultas por fecha descendente
CREATE INDEX idx_auditoria_fecha ON auditoria_actividad (fecha_hora DESC);
