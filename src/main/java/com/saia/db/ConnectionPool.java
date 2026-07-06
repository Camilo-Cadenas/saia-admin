package com.saia.db;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Administra el pool de conexiones a MySQL usando HikariCP.
 * Singleton: solo existe una instancia del {@link HikariDataSource} durante
 * toda la vida de la aplicación.
 *
 * <p>Uso:
 * <pre>
 *   try (Connection cn = ConnectionPool.getInstance().getConnection()) {
 *       // usar cn
 *   }
 * </pre>
 */
public final class ConnectionPool {

    private static ConnectionPool   instance;
    private final  HikariDataSource dataSource;

    private ConnectionPool() {
        DatabaseConfig cfg = DatabaseConfig.getInstance();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl          (cfg.getUrl());
        hikariConfig.setUsername         (cfg.getUser());
        hikariConfig.setPassword         (cfg.getPassword());
        hikariConfig.setMinimumIdle      (cfg.getMinIdle());
        hikariConfig.setMaximumPoolSize  (cfg.getMaxPoolSize());
        hikariConfig.setConnectionTimeout(cfg.getConnectionTimeout());
        hikariConfig.setIdleTimeout      (cfg.getIdleTimeout());
        hikariConfig.setMaxLifetime      (cfg.getMaxLifetime());
        hikariConfig.setPoolName         ("SAIA-Pool");

        // Consulta de validación de conexión viva
        hikariConfig.setConnectionTestQuery("SELECT 1");

        // Driver explícito para asegurar carga correcta
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

        this.dataSource = new HikariDataSource(hikariConfig);
    }

    /** Retorna la instancia única del pool. */
    public static ConnectionPool getInstance() {
        if (instance == null) {
            synchronized (ConnectionPool.class) {
                if (instance == null) instance = new ConnectionPool();
            }
        }
        return instance;
    }

    /**
     * Obtiene una conexión del pool.
     * Debe cerrarse con try-with-resources para devolverla al pool.
     *
     * @return conexión activa
     * @throws SQLException si no hay conexiones disponibles
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Cierra el pool y libera todos los recursos.
     * Se llama automáticamente cuando la aplicación termina
     * desde el shutdown hook registrado en {@link Main}.
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("[ConnectionPool] Pool de conexiones cerrado.");
        }
    }

    /** Comprueba si el pool está activo y tiene conexiones disponibles. */
    public boolean isHealthy() {
        try (Connection cn = getConnection()) {
            return cn != null && !cn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
