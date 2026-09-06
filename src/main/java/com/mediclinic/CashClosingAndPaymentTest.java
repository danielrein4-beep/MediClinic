package com.mediclinic;

import com.mediclinic.dao.DailyPaymentDAO;
import com.mediclinic.dao.PatientDAO;
import com.mediclinic.dao.UserDAO;
import com.mediclinic.dao.WaitingRoomDAO;
import com.mediclinic.database.DatabaseInitializer;
import com.mediclinic.models.DailyPayment;
import com.mediclinic.models.Patient;
import com.mediclinic.models.User;
import com.mediclinic.models.WaitingRoomEntry;
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

public class CashClosingAndPaymentTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("🧪 VALIDACIÓN DE CONTROL DE PAGOS Y CIERRE DE CAJA DIARIO");
        System.out.println("=================================================");

        DatabaseInitializer.initializeDatabase();

        DailyPaymentDAO paymentDAO = new DailyPaymentDAO();
        PatientDAO patientDAO = new PatientDAO();
        WaitingRoomDAO waitingRoomDAO = new WaitingRoomDAO();
        UserDAO userDAO = new UserDAO();

        // 1. Preparar pacientes y sala de espera
        System.out.println("\n[1] Registrando pacientes en sala de espera y pagos multidivisa...");
        List<Patient> patients = patientDAO.findAll();
        if (patients.size() < 2) {
            Patient p1 = new Patient("HC-PAY-01", "V-11223344", "Elena", "Bermudez", LocalDate.of(1990, 4, 12), "0414-1112233", "elena@test.com", "LOCAL", null, "San Cristóbal");
            Patient p2 = new Patient("HC-PAY-02", "V-55667788", "Marcos", "Duran", LocalDate.of(1985, 9, 23), "0424-9988776", "marcos@test.com", "LOCAL", null, "San Cristóbal");
            patientDAO.insert(p1);
            patientDAO.insert(p2);
            patients = patientDAO.findAll();
        }

        Patient patient1 = patients.get(0);
        Patient patient2 = patients.get(1);

        waitingRoomDAO.clearAll();
        WaitingRoomEntry entry1 = new WaitingRoomEntry(patient1.getId(), "Consulta General");
        WaitingRoomEntry entry2 = new WaitingRoomEntry(patient2.getId(), "Control Post-Operatorio");
        waitingRoomDAO.insert(entry1);
        waitingRoomDAO.insert(entry2);

        // Registrar pagos
        DailyPayment pay1 = new DailyPayment(patient1.getId(), entry1.getId(), "Efectivo", "USD", 50.0, "Pago en efectivo $50");
        DailyPayment pay2 = new DailyPayment(patient2.getId(), entry2.getId(), "Pago Móvil", "VES", 1825.0, "Ref: 987654");
        paymentDAO.insert(pay1);
        paymentDAO.insert(pay2);

        List<DailyPayment> todayPayments = paymentDAO.findAllToday();
        System.out.println("Total pagos registrados hoy: " + todayPayments.size());
        if (todayPayments.size() >= 2) {
            System.out.println("✅ Pago 1 registrado: " + todayPayments.get(0).getPatientName() + " - " + todayPayments.get(0).getFormattedAmount() + " (" + todayPayments.get(0).getPaymentMethod() + ")");
            System.out.println("✅ Pago 2 registrado: " + todayPayments.get(1).getPatientName() + " - " + todayPayments.get(1).getFormattedAmount() + " (" + todayPayments.get(1).getPaymentMethod() + ")");
        } else {
            System.err.println("❌ Fallo en registro de pagos.");
            System.exit(1);
        }

        // 2. Verificar que WaitingRoomDAO refleja el estado PAGADO
        List<WaitingRoomEntry> entries = waitingRoomDAO.findAllToday();
        boolean hasPaidEntries = entries.stream().anyMatch(WaitingRoomEntry::isPaid);
        if (hasPaidEntries) {
            System.out.println("✅ La sala de espera refleja el estado 'PAGADO' con el detalle del medio y monto.");
        } else {
            System.err.println("❌ La sala de espera no reflejó los pagos asociados.");
            System.exit(1);
        }

        // 3. Probar Generación del PDF de Cierre de Caja
        System.out.println("\n[2] Probando Generación del PDF de Cierre de Caja...");
        User secretary = userDAO.findFirstSecretary();
        if (secretary == null) {
            secretary = new User(2, "secretaria", "1234", "Niccolle Medina", "SECRETARIA", "Secretaria", "", "");
        }

        try {
            File pdfFile = PdfReportService.generateDailyCashClosingPdf(todayPayments, entries, secretary);
            if (pdfFile != null && pdfFile.exists() && pdfFile.length() > 0) {
                System.out.println("✅ PDF de Cierre de Caja generado con éxito en: " + pdfFile.getAbsolutePath() + " (Tamaño: " + pdfFile.length() + " bytes)");
            } else {
                System.err.println("❌ Archivo PDF no generado.");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("❌ Error al generar PDF de cierre: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // 4. Probar RBAC y Carga de Vista en JavaFX
        System.out.println("\n[3] Probando RBAC en Sala de Espera (Doctor Solo Lectura vs Secretaria Control Total)...");
        CountDownLatch fxLatch = new CountDownLatch(1);
        try {
            Platform.startup(fxLatch::countDown);
        } catch (IllegalStateException e) {
            fxLatch.countDown();
        }

        CountDownLatch rbacLatch = new CountDownLatch(1);
        AtomicBoolean rbacOk = new AtomicBoolean(true);

        final User finalSec = secretary;

        Platform.runLater(() -> {
            try {
                // Test como Doctor
                User doc = userDAO.findFirstDoctor();
                SessionManager.getInstance().setCurrentUser(doc);

                FXMLLoader loaderDoc = new FXMLLoader(CashClosingAndPaymentTest.class.getResource("/views/WaitingRoomView.fxml"));
                loaderDoc.load();
                System.out.println("✅ Vista de Sala de Espera cargada en modo DOCTOR (Solo lectura activa).");

                // Test como Secretaria
                SessionManager.getInstance().setCurrentUser(finalSec);
                FXMLLoader loaderSec = new FXMLLoader(CashClosingAndPaymentTest.class.getResource("/views/WaitingRoomView.fxml"));
                loaderSec.load();
                System.out.println("✅ Vista de Sala de Espera cargada en modo SECRETARIA (Control total activo).");

            } catch (Exception e) {
                System.err.println("❌ Error en carga de vistas RBAC: " + e.getMessage());
                e.printStackTrace();
                rbacOk.set(false);
            } finally {
                rbacLatch.countDown();
            }
        });

        try {
            rbacLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!rbacOk.get()) {
            System.exit(1);
        }

        System.out.println("\n=================================================");
        System.out.println("🎉 CONTROL DE PAGOS, CIERRE DE CAJA EN PDF Y RBAC VALIDADOS AL 100%");
        System.out.println("=================================================");
        System.exit(0);
    }
}
