package com.mediclinic;

import com.mediclinic.database.DatabaseConnection;
import com.mediclinic.database.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void init() throws Exception {
        // Inicializar la base de datos SQLite y datos iniciales antes de renderizar la UI
        DatabaseInitializer.initializeDatabase();
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1024, 680);
            
            // Cargar estilos CSS respetando el tema guardado
            com.mediclinic.services.ThemeManager.getInstance().applyTheme(scene);

            primaryStage.setTitle("MediClinic Pro - Iniciar Sesión");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.setMinWidth(960);
            primaryStage.setMinHeight(640);
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("Error crítico al iniciar la aplicación JavaFX: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        // Cerrar conexión a SQLite de forma segura al salir
        DatabaseConnection.closeConnection();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
