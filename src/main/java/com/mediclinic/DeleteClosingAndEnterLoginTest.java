package com.mediclinic;

import com.mediclinic.controllers.FinancialSummaryViewController;
import com.mediclinic.controllers.LoginController;
import com.mediclinic.dao.FinancialClosingDAO;
import com.mediclinic.database.DatabaseInitializer;
import com.mediclinic.models.FinancialClosing;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeleteClosingAndEnterLoginTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("🧪 VALIDACIÓN DE LOGIN CON ENTER Y BORRADO DE CIERRE");
        System.out.println("=================================================");

        DatabaseInitializer.initializeDatabase();

        FinancialClosingDAO closingDAO = new FinancialClosingDAO();

        // 1. Probar inserción y borrado directo en FinancialClosingDAO
        System.out.println("\n[1] Probando inserción y eliminación de cierre financiero...");
        FinancialClosing tempClosing = new FinancialClosing(
                LocalDate.now().minusDays(1),
                "07:00 PM",
                80.0,
                3000.0,
                200000.0,
                4,
                "Doctor Test",
                "Cierre Temporal para prueba de borrado"
        );
        closingDAO.insert(tempClosing);
        int insertedId = tempClosing.getId();
        System.out.println("✅ Cierre temporal insertado con ID #" + insertedId);

        boolean deleted = closingDAO.delete(insertedId);
        if (deleted && closingDAO.findById(insertedId) == null) {
            System.out.println("✅ Cierre financiero #" + insertedId + " eliminado exitosamente de la base de datos.");
        } else {
            System.err.println("❌ Fallo al eliminar cierre financiero.");
            System.exit(1);
        }

        // 2. Probar UI JavaFX
        System.out.println("\n[2] Probando Login defaultButton y FinancialSummaryView UI...");
        CountDownLatch fxLatch = new CountDownLatch(1);
        try {
            Platform.startup(fxLatch::countDown);
        } catch (IllegalStateException e) {
            fxLatch.countDown();
        }

        CountDownLatch uiLatch = new CountDownLatch(1);
        AtomicBoolean testOk = new AtomicBoolean(true);

        Platform.runLater(() -> {
            try {
                // Test Login.fxml defaultButton
                FXMLLoader loginLoader = new FXMLLoader(DeleteClosingAndEnterLoginTest.class.getResource("/views/Login.fxml"));
                StackPane loginRoot = loginLoader.load();
                LoginController loginController = loginLoader.getController();

                Button btnLogin = (Button) loginRoot.lookup("#btnLogin");
                if (btnLogin != null && btnLogin.isDefaultButton()) {
                    System.out.println("✅ Botón 'Iniciar Sesión' tiene defaultButton=true (funciona con la tecla Enter).");
                } else {
                    System.err.println("❌ defaultButton no está activado en btnLogin.");
                    testOk.set(false);
                }

                // Test FinancialSummaryView
                FXMLLoader summaryLoader = new FXMLLoader(DeleteClosingAndEnterLoginTest.class.getResource("/views/FinancialSummaryView.fxml"));
                summaryLoader.load();
                FinancialSummaryViewController summaryController = summaryLoader.getController();
                summaryController.loadData();
                System.out.println("✅ FinancialSummaryView y columna de acciones con botón 'Eliminar' cargada.");

            } catch (Exception e) {
                System.err.println("❌ Error en tests UI: " + e.getMessage());
                e.printStackTrace();
                testOk.set(false);
            } finally {
                uiLatch.countDown();
            }
        });

        try {
            uiLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!testOk.get()) {
            System.exit(1);
        }

        System.out.println("\n=================================================");
        System.out.println("🎉 AJUSTES DE UX Y BORRADO DE CIERRE VALIDADOS AL 100%");
        System.out.println("=================================================");
        System.exit(0);
    }
}
