package com.saia.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Lee y expone la configuración de la base de datos desde {@code db.properties}.
 * Sigue el patrón Singleton para cargar el archivo una sola vez.
 */
public final class DatabaseConfig {

    private static final String PROPERTIES_FILE = "/db.properties";

    private static DatabaseConfig instance;

    private final String url;
    private final String user;
    private final String password;
    private final int    minIdle;
    private final int    maxPoolSize;
    private final long   connectionTimeout;
    private final long   idleTimeout;
    private final long   maxLifetime;

    private DatabaseConfig() {
        Properties props = new Properties();
        try (InputStream is = DatabaseConfig.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (is == null) {
                throw new RuntimeException("No se encontró el archivo " + PROPERTIES_FILE
                        + " en el classpath. Verifique src/main/resources/");
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Error cargando la configuración de base de datos", e);
        }

        String host     = props.getProperty("db.host",     "localhost");
        String port     = props.getProperty("db.port",     "3306");
        String dbName   = props.getProperty("db.name",     "saia");

        // JDBC URL con parámetros de tiempo, charset y zona horaria
        this.url = String.format(
                "jdbc:mysql://%s:%s/%s"
                + "?useSSL=false"
                + "&serverTimezone=America/Bogota"
                + "&characterEncoding=UTF-8"
                + "&allowPublicKeyRetrieval=true"
                + "&connectTimeout=10000",
                host, port, dbName);

        this.user              = props.getProperty("db.user",     "root");
        this.password          = props.getProperty("db.password", "");
        this.minIdle           = Integer.parseInt(props.getProperty("db.pool.minimum-idle",       "2"));
        this.maxPoolSize       = Integer.parseInt(props.getProperty("db.pool.maximum-pool-size",  "10"));
        this.connectionTimeout = Long.parseLong  (props.getProperty("db.pool.connection-timeout", "30000"));
        this.idleTimeout       = Long.parseLong  (props.getProperty("db.pool.idle-timeout",       "600000"));
        this.maxLifetime       = Long.parseLong  (props.getProperty("db.pool.max-lifetime",       "1800000"));
    }

    /** Retorna la instancia única de la configuración. */
    public static DatabaseConfig getInstance() {
        if (instance == null) {
            synchronized (DatabaseConfig.class) {
                if (instance == null) instance = new DatabaseConfig();
            }
        }
        return instance;
    }

    public String getUrl()               { return url; }
    public String getUser()              { return user; }
    public String getPassword()          { return password; }
    public int    getMinIdle()           { return minIdle; }
    public int    getMaxPoolSize()       { return maxPoolSize; }
    public long   getConnectionTimeout() { return connectionTimeout; }
    public long   getIdleTimeout()       { return idleTimeout; }
    public long   getMaxLifetime()       { return maxLifetime; }
}
