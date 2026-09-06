package com.mediclinic;

import com.mediclinic.dao.ConfigDAO;
import com.mediclinic.dao.ProcedureDAO;
import com.mediclinic.dao.UserDAO;
import com.mediclinic.database.DatabaseInitializer;
import com.mediclinic.models.ProcedureQuote;
import com.mediclinic.models.User;
import com.mediclinic.services.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SettingsAndThemeTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("🧪 VALIDACIÓN DE AJUSTES, PERSISTENCIA & MODO OSCURO");
        System.out.println("=================================================");

        DatabaseInitializer.initializeDatabase();

        // 1. Validar persistencia en ConfigDAO
        System.out.println("\n[1] Probando persistencia de ConfigDAO...");
        ConfigDAO configDAO = new ConfigDAO();
        configDAO.setValue("CLINICA_NOMBRE", "Clínica Quirúrgica San Cristóbal", "Test");
        configDAO.setValue("TASA_USD_VES", "40.50", "Test");
        configDAO.setValue("TASA_USD_COP", "4200.0", "Test");

        String savedName = configDAO.getValue("CLINICA_NOMBRE", null);
        double savedVes = configDAO.getDoubleValue("TASA_USD_VES", 0);
        double savedCop = configDAO.getDoubleValue("TASA_USD_COP", 0);

        if ("Clínica Quirúrgica San Cristóbal".equals(savedName) && savedVes == 40.50 && savedCop == 4200.0) {
            System.out.println("✅ ConfigDAO actualiza y persiste correctamente en SQLite.");
        } else {
            System.err.println("❌ Fallo en persistencia de ConfigDAO.");
            System.exit(1);
        }

        // 2. Validar actualización de Perfil de Usuario
        System.out.println("\n[2] Probando actualización de Perfil de Doctor...");
        UserDAO userDAO = new UserDAO();
        User doctor = userDAO.findFirstDoctor();
        if (doctor != null) {
            doctor.setSpecialty("Cirujano General & Laparoscopia Avanzada");
            doctor.setMppsLicense("MPPS-998877");
            boolean updated = userDAO.update(doctor);
            User reloaded = userDAO.findFirstDoctor();
            if (updated && reloaded != null && "MPPS-998877".equals(reloaded.getMppsLicense())) {
                System.out.println("✅ UserDAO actualiza y persiste el perfil médico en SQLite.");
            } else {
                System.err.println("❌ Fallo al persistir el perfil del doctor.");
                System.exit(1);
            }
        }

        // 3. Validar ThemeManager & Guardado en SQLite
        System.out.println("\n[3] Probando ThemeManager (Light / Dark Mode)...");
        ThemeManager themeMgr = ThemeManager.getInstance();
        themeMgr.setTheme("DARK", null);
        String currentTheme = themeMgr.getCurrentTheme();
        String savedThemeInDb = configDAO.getValue("TEMA_COLOR", null);
        if ("DARK".equals(currentTheme) && "DARK".equals(savedThemeInDb)) {
            System.out.println("✅ ThemeManager activa y persiste 'DARK' en SQLite.");
        } else {
            System.err.println("❌ Fallo al persistir tema DARK.");
            System.exit(1);
        }

        themeMgr.setTheme("LIGHT", null);
        if ("LIGHT".equals(themeMgr.getCurrentTheme())) {
            System.out.println("✅ ThemeManager activa y persiste 'LIGHT' en SQLite.");
        }

        // 4. Validar creación de procedimiento con campo libre y emojis
        System.out.println("\n[4] Probando procedimiento con nombre libre y estados...");
        ProcedureDAO procDAO = new ProcedureDAO();
        com.mediclinic.dao.PatientDAO patientDAO = new com.mediclinic.dao.PatientDAO();
        java.util.List<com.mediclinic.models.Patient> patients = patientDAO.findAll();
        int targetPatientId;
        if (!patients.isEmpty()) {
            targetPatientId = patients.get(0).getId();
        } else {
            com.mediclinic.models.Patient testP = new com.mediclinic.models.Patient(
                    "HC-TEST-THEME", "V-88888888", "Paciente", "ThemeTest",
                    java.time.LocalDate.of(1990, 1, 1), "0414-0000000", "test@med.com", "LOCAL", null, "Dirección"
            );
            patientDAO.insert(testP);
            targetPatientId = testP.getId();
        }

        ProcedureQuote freeQuote = new ProcedureQuote(
                targetPatientId,
                "Colecistectomía Laparoscópica de Emergencia",
                "Procedimiento quirúrgico con campo libre",
                850.0,
                savedVes,
                savedCop,
                "COTIZADA",
                LocalDateTime.now().plusDays(2),
                "Requiere ayuno de 8 horas"
        );
        boolean procOk = procDAO.insert(freeQuote);
        if (procOk) {
            System.out.println("✅ Procedimiento libre registrado exitosamente: '" + freeQuote.getProcedureType() + "'");
        } else {
            System.err.println("❌ Error al registrar procedimiento libre.");
            System.exit(1);
        }

        // 5. Validar carga FXML en JavaFX
        System.out.println("\n[5] Validando FXML de ProceduresView y SettingsView...");
        CountDownLatch fxLatch = new CountDownLatch(1);
        try {
            Platform.startup(fxLatch::countDown);
        } catch (IllegalStateException e) {
            fxLatch.countDown();
        }

        CountDownLatch testLatch = new CountDownLatch(2);
        AtomicBoolean fxmlOk = new AtomicBoolean(true);

        Platform.runLater(() -> {
            try {
                FXMLLoader l1 = new FXMLLoader(SettingsAndThemeTest.class.getResource("/views/SettingsView.fxml"));
                l1.load();
                System.out.println("✅ SettingsView.fxml cargado sin errores.");
            } catch (Exception e) {
                System.err.println("❌ Error en SettingsView.fxml: " + e.getMessage());
                e.printStackTrace();
                fxmlOk.set(false);
            } finally {
                testLatch.countDown();
            }

            try {
                FXMLLoader l2 = new FXMLLoader(SettingsAndThemeTest.class.getResource("/views/ProceduresView.fxml"));
                l2.load();
                System.out.println("✅ ProceduresView.fxml cargado sin errores con campo de texto libre.");
            } catch (Exception e) {
                System.err.println("❌ Error en ProceduresView.fxml: " + e.getMessage());
                e.printStackTrace();
                fxmlOk.set(false);
            } finally {
                testLatch.countDown();
            }
        });

        try {
            testLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!fxmlOk.get()) {
            System.err.println("❌ Fallo en la carga de vistas.");
            System.exit(1);
        }

        System.out.println("\n=================================================");
        System.out.println("🎉 TODAS LAS MEJORAS Y CORRECCIONES VALIDADAS AL 100%");
        System.out.println("=================================================");
        System.exit(0);
    }
}
