package com.saia.data;

/**
 * Excepción no comprobada que encapsula los errores de la capa de acceso a datos.
 * Evita contaminar las capas superiores con {@link java.sql.SQLException}.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
