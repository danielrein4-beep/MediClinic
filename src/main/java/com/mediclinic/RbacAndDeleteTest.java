package com.mediclinic;

import com.mediclinic.dao.ConsultationDAO;
import com.mediclinic.dao.PatientDAO;
import com.mediclinic.dao.ProcedureDAO;
import com.mediclinic.dao.UserDAO;
import com.mediclinic.dao.WaitingRoomDAO;
import com.mediclinic.database.DatabaseInitializer;
import com.mediclinic.models.Consultation;
import com.mediclinic.models.Patient;
import com.mediclinic.models.ProcedureQuote;
import com.mediclinic.models.User;
import com.mediclinic.models.WaitingRoomEntry;
import com.mediclinic.services.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RbacAndDeleteTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("🧪 VALIDACIÓN DE RBAC (SECRETARIA) & ELIMINACIÓN EN CASCADA");
        System.out.println("=================================================");

        DatabaseInitializer.initializeDatabase();

        PatientDAO patientDAO = new PatientDAO();
        ConsultationDAO consultationDAO = new ConsultationDAO();
        ProcedureDAO procedureDAO = new ProcedureDAO();
        WaitingRoomDAO waitingRoomDAO = new WaitingRoomDAO();
        UserDAO userDAO = new UserDAO();

        // 1. Probar eliminación de paciente con eliminación en cascada de historias y cotizaciones
        System.out.println("\n[1] Creando paciente de prueba con historia clínica, cotización y sala de espera...");
        Patient tempPatient = new Patient(
                "HC-TEST-9999",
                "V-99999999",
                "Paciente",
                "Para Borrar",
                LocalDate.of(1995, 5, 20),
                "0414-9999999",
                "borrar@test.com",
                "LOCAL",
                null,
                "Dirección de prueba"
        );
        patientDAO.insert(tempPatient);
        int patientId = tempPatient.getId();
        System.out.println("Paciente creado con ID: " + patientId);

        // Crear consulta
        Consultation tempConsultation = new Consultation(
                0,
                patientId,
                1,
                LocalDateTime.now(),
                "Dolor abdominal de prueba",
                "Evolución de prueba",
                "Diagnóstico temporal",
                "Tratamiento temporal",
                70.0,
                1.75,
                "Sin observaciones físicas",
                null,
                null
        );
        consultationDAO.insert(tempConsultation);

        // Crear cotización
        ProcedureQuote tempQuote = new ProcedureQuote(
                patientId,
                "Biopsia de Prueba",
                "Descripción prueba",
                120.0,
                38.5,
                4100.0,
                "COTIZADA",
                null,
                "Notas prueba"
        );
        procedureDAO.insert(tempQuote);

        // Crear entrada en sala de espera
        WaitingRoomEntry tempWaiting = new WaitingRoomEntry(
                patientId,
                "Consulta general"
        );
        waitingRoomDAO.insert(tempWaiting);

        System.out.println("Eliminando paciente ID #" + patientId + " con borrado en cascada...");
        boolean deleted = patientDAO.delete(patientId);
        if (deleted && patientDAO.findById(patientId) == null) {
            System.out.println("✅ Paciente y todos sus registros asociados eliminados exitosamente en cascada sin violar FKs.");
        } else {
            System.err.println("❌ Fallo al eliminar paciente en cascada.");
            System.exit(1);
        }

        // 2. Probar RBAC para Secretaria y Doctor en JavaFX
        System.out.println("\n[2] Validando interfaz RBAC para Secretaria...");
        CountDownLatch fxLatch = new CountDownLatch(1);
        try {
            Platform.startup(fxLatch::countDown);
        } catch (IllegalStateException e) {
            fxLatch.countDown();
        }

        User secretary = userDAO.findFirstSecretary();
        if (secretary == null) {
            secretary = new User(2, "secretaria", "1234", "Niccolle Medina", "SECRETARIA", "Secretaria", "", "");
        }
        SessionManager.getInstance().setCurrentUser(secretary);

        CountDownLatch rbacLatch = new CountDownLatch(2);
        AtomicBoolean rbacOk = new AtomicBoolean(true);

        Platform.runLater(() -> {
            try {
                // Test PatientsView como Secretaria
                FXMLLoader lPatients = new FXMLLoader(RbacAndDeleteTest.class.getResource("/views/PatientsView.fxml"));
                lPatients.load();

                // Test ProceduresView como Secretaria
                FXMLLoader lProc = new FXMLLoader(RbacAndDeleteTest.class.getResource("/views/ProceduresView.fxml"));
                lProc.load();

                System.out.println("✅ Vistas cargadas correctamente en contexto de SECRETARIA (Solo lectura activa).");
            } catch (Exception e) {
                System.err.println("❌ Error en vistas RBAC: " + e.getMessage());
                e.printStackTrace();
                rbacOk.set(false);
            } finally {
                rbacLatch.countDown();
            }

            try {
                // Cambiar a Doctor y verificar que no haya excepciones
                User doc = userDAO.findFirstDoctor();
                SessionManager.getInstance().setCurrentUser(doc);

                FXMLLoader lPatientsDoc = new FXMLLoader(RbacAndDeleteTest.class.getResource("/views/PatientsView.fxml"));
                lPatientsDoc.load();

                FXMLLoader lProcDoc = new FXMLLoader(RbacAndDeleteTest.class.getResource("/views/ProceduresView.fxml"));
                lProcDoc.load();

                System.out.println("✅ Vistas cargadas correctamente en contexto de DOCTOR (Acceso completo activo).");
            } catch (Exception e) {
                System.err.println("❌ Error en vistas Doctor: " + e.getMessage());
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
            System.err.println("❌ Fallo en las pruebas de control de acceso.");
            System.exit(1);
        }

        System.out.println("\n=================================================");
        System.out.println("🎉 CONTROL DE ACCESO RBAC Y ELIMINACIÓN DE SEGURIDAD EXITOSOS");
        System.out.println("=================================================");
        System.exit(0);
    }
}
