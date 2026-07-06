package com.saia;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.saia.db.ConnectionPool;
import com.saia.db.DatabaseConfig;
import com.saia.presentation.login.LoginFrame;
import com.saia.util.SessionManager;

/**
 * Punto de entrada del sistema SAIA - Panel Administrativo.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Verificar la conexión a la base de datos antes de mostrar la UI.</li>
 *   <li>Registrar un shutdown hook para cerrar el pool y la sesión al salir.</li>
 *   <li>Lanzar la pantalla de login en el Event Dispatch Thread.</li>
 * </ul>
 */
public class Main {

    public static void main(String[] args) {

        // 1. Look & Feel nativo del sistema operativo
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException
                 | IllegalAccessException | UnsupportedLookAndFeelException e) {
            System.err.println("[Main] Look & Feel no disponible: " + e.getMessage());
        }

        // 2. Verificar conectividad con la BD antes de mostrar la UI
        if (!checkDatabaseConnection()) {
            JOptionPane.showMessageDialog(null,
                    "<html><b>No se pudo conectar a la base de datos.</b><br><br>"
                    + "Verifique la configuración en:<br>"
                    + "<code>src/main/resources/db.properties</code><br><br>"
                    + "Parámetros actuales:<br>"
                    + "• URL: " + DatabaseConfig.getInstance().getUrl() + "<br>"
                    + "• Usuario: " + DatabaseConfig.getInstance().getUser() + "</html>",
                    "Error de conexión — SAIA",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // 3. Shutdown hook: cerrar sesión y pool al terminar
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Main] Cerrando aplicación SAIA...");
            if (SessionManager.getInstance().isSessionActive()) {
                SessionManager.getInstance().endSession();
            }
            ConnectionPool.getInstance().shutdown();
            System.out.println("[Main] Recursos liberados. Hasta luego.");
        }, "SAIA-ShutdownHook"));

        // 4. Lanzar la UI en el EDT
        SwingUtilities.invokeLater(LoginFrame::new);
    }

    /**
     * Intenta obtener una conexión del pool para verificar que la BD responde.
     *
     * @return true si la conexión es exitosa
     */
    private static boolean checkDatabaseConnection() {
        try {
            System.out.println("[Main] Verificando conexión a la base de datos...");
            boolean ok = ConnectionPool.getInstance().isHealthy();
            if (ok) {
                System.out.println("[Main] Conexión a BD establecida correctamente.");
            } else {
                System.err.println("[Main] La base de datos no responde.");
            }
            return ok;
        } catch (Exception e) {
            System.err.println("[Main] Error al conectar con la BD: " + e.getMessage());
            return false;
        }
    }
}
