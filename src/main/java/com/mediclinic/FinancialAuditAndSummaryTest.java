package com.mediclinic;

import com.mediclinic.controllers.DoctorDashboardController;
import com.mediclinic.controllers.FinancialSummaryViewController;
import com.mediclinic.dao.DailyPaymentDAO;
import com.mediclinic.dao.FinancialClosingDAO;
import com.mediclinic.dao.UserDAO;
import com.mediclinic.database.DatabaseInitializer;
import com.mediclinic.models.DailyPayment;
import com.mediclinic.models.FinancialClosing;
import com.mediclinic.models.User;
import com.mediclinic.services.PdfReportService;
import com.mediclinic.services.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FinancialAuditAndSummaryTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("🧪 VALIDACIÓN DE AUDITORÍA Y RESÚMENES FINANCIEROS");
        System.out.println("=================================================");

        DatabaseInitializer.initializeDatabase();

        FinancialClosingDAO closingDAO = new FinancialClosingDAO();
        DailyPaymentDAO paymentDAO = new DailyPaymentDAO();
        UserDAO userDAO = new UserDAO();

        // 1. Probar inserción y consulta en cierres_financieros
        System.out.println("\n[1] Probando persistencia en cierres_financieros...");
        LocalDate today = LocalDate.now();
        FinancialClosing closing = new FinancialClosing(
                today,
                "06:30 PM",
                150.0,
                5500.0,
                360000.0,
                6,
                "Niccolle Medina",
                "Cierre de Jornada de Prueba"
        );

        boolean inserted = closingDAO.insert(closing);
        if (inserted && closing.getId() > 0) {
            System.out.println("✅ Cierre financiero insertado con ID #" + closing.getId());
        } else {
            System.err.println("❌ Fallo al insertar cierre financiero.");
            System.exit(1);
        }

        List<FinancialClosing> allClosings = closingDAO.findAll();
        if (!allClosings.isEmpty()) {
            FinancialClosing first = allClosings.get(0);
            System.out.println("✅ Consulta de cierres exitosa. Total registros: " + allClosings.size() +
                    " | Último: " + first.getFormattedUsd() + " / " + first.getFormattedVes() + " / " + first.getFormattedCop());
        } else {
            System.err.println("❌ No se encontraron cierres financieros.");
            System.exit(1);
        }

        // 2. Probar generación de PDF desde un cierre
        System.out.println("\n[2] Probando generación de PDF de cierre para auditoría...");
        User doc = userDAO.findFirstDoctor();
        try {
            List<DailyPayment> payments = paymentDAO.findAllToday();
            File pdf = PdfReportService.generateDailyCashClosingPdf(payments, List.of(), doc);
            if (pdf != null && pdf.exists() && pdf.length() > 0) {
                System.out.println("✅ PDF de Cierre Financiero generado exitosamente: " + pdf.getAbsolutePath() + " (" + pdf.length() + " bytes)");
            } else {
                System.err.println("❌ Fallo al generar archivo PDF.");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("❌ Error en PDF: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // 3. Probar carga de vistas JavaFX
        System.out.println("\n[3] Probando carga de FinancialSummaryView.fxml y DoctorDashboard...");
        CountDownLatch fxLatch = new CountDownLatch(1);
        try {
            Platform.startup(fxLatch::countDown);
        } catch (IllegalStateException e) {
            fxLatch.countDown();
        }

        CountDownLatch uiLatch = new CountDownLatch(1);
        AtomicBoolean uiOk = new AtomicBoolean(true);

        Platform.runLater(() -> {
            try {
                SessionManager.getInstance().setCurrentUser(doc);

                // Cargar FinancialSummaryView
                FXMLLoader loaderSummary = new FXMLLoader(FinancialAuditAndSummaryTest.class.getResource("/views/FinancialSummaryView.fxml"));
                loaderSummary.load();
                FinancialSummaryViewController summaryController = loaderSummary.getController();
                summaryController.loadData();
                System.out.println("✅ FinancialSummaryView y su controlador inicializados correctamente.");

                // Cargar DoctorDashboard y probar botón Resúmenes
                FXMLLoader loaderDoc = new FXMLLoader(FinancialAuditAndSummaryTest.class.getResource("/views/DoctorDashboard.fxml"));
                loaderDoc.load();
                DoctorDashboardController docController = loaderDoc.getController();
                docController.showFinancialSummaryTab();
                System.out.println("✅ Navegación a 'Resúmenes Financieros' en DoctorDashboard validada.");

            } catch (Exception e) {
                System.err.println("❌ Error en carga de vistas UI: " + e.getMessage());
                e.printStackTrace();
                uiOk.set(false);
            } finally {
                uiLatch.countDown();
            }
        });

        try {
            uiLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!uiOk.get()) {
            System.exit(1);
        }

        System.out.println("\n=================================================");
        System.out.println("🎉 MÓDULO DE AUDITORÍA Y RESÚMENES FINANCIEROS VALIDADO AL 100%");
        System.out.println("=================================================");
        System.exit(0);
    }
}
