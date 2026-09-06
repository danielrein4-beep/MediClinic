package com.mediclinic;

import com.mediclinic.database.DatabaseInitializer;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class NavigationTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("🧪 MEDICLINIC PRO - TEST INTEGRAL DE NAVEGACIÓN");
        System.out.println("=================================================");

        DatabaseInitializer.initializeDatabase();

        // Inicializar JavaFX Toolkit
        CountDownLatch fxLatch = new CountDownLatch(1);
        try {
            Platform.startup(fxLatch::countDown);
        } catch (IllegalStateException e) {
            fxLatch.countDown();
        }

        try {
            if (!fxLatch.await(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Tiempo de espera agotado al inicializar JavaFX toolkit.");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        String[] fxmlViews = {
                "/views/Login.fxml",
                "/views/DoctorDashboard.fxml",
                "/views/SecretaryDashboard.fxml",
                "/views/PatientsView.fxml",
                "/views/ClinicalHistory.fxml",
                "/views/ProceduresView.fxml",
                "/views/SettingsView.fxml",
                "/views/WaitingRoomView.fxml",
                "/views/AgendaView.fxml"
        };

        AtomicBoolean allOk = new AtomicBoolean(true);

        CountDownLatch testLatch = new CountDownLatch(fxmlViews.length);

        for (String fxmlPath : fxmlViews) {
            Platform.runLater(() -> {
                try {
                    System.out.println("Cargando y parseando FXML: " + fxmlPath + "...");
                    FXMLLoader loader = new FXMLLoader(NavigationTest.class.getResource(fxmlPath));
                    Object root = loader.load();
                    Object controller = loader.getController();
                    if (root == null || controller == null) {
                        System.err.println("❌ Fallo en " + fxmlPath + ": root o controller es null.");
                        allOk.set(false);
                    } else {
                        System.out.println("✅ " + fxmlPath + " cargado con éxito. Controlador: " + controller.getClass().getSimpleName());
                    }
                } catch (Exception e) {
                    System.err.println("❌ ERROR al cargar FXML " + fxmlPath + ": " + e.getMessage());
                    e.printStackTrace();
                    allOk.set(false);
                } finally {
                    testLatch.countDown();
                }
            });
        }

        try {
            testLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\n=================================================");
        if (allOk.get()) {
            System.out.println("🎉 TODAS LAS VISTAS Y CONTROLADORES NAVEGAN PERFECTAMENTE");
        } else {
            System.err.println("❌ AL MENOS UNA VISTA FALLÓ EN LA CARGA");
            System.exit(1);
        }
        System.out.println("=================================================");
        System.exit(0);
    }
}
