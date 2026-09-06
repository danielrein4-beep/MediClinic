package com.mediclinic.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String DB_DIR = "data";
    private static final String DB_FILE = "mediclinic.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_DIR + File.separator + DB_FILE;

    private static Connection connection = null;

    private DatabaseConnection() {}

    /**
     * Obtiene una conexión activa a la base de datos SQLite.
     * Crea el directorio y la base de datos si no existen.
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            ensureDataDirectoryExists();
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite JDBC Driver no encontrado en el classpath.", e);
            }
            connection = DriverManager.getConnection(DB_URL);
            
            // Habilitar claves foráneas y modo WAL para máxima concurrencia y velocidad
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA journal_mode = WAL;");
                stmt.execute("PRAGMA synchronous = NORMAL;");
            }
        }
        return connection;
    }

    private static void ensureDataDirectoryExists() {
        File dir = new File(DB_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static String getDatabasePath() {
        return new File(DB_DIR, DB_FILE).getAbsolutePath();
    }

    public static synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Error al cerrar la conexión: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }
}
