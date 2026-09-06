package com.mediclinic;

import com.mediclinic.controllers.AgendaViewController;
import com.mediclinic.dao.AppointmentDAO;
import com.mediclinic.dao.PatientDAO;
import com.mediclinic.database.DatabaseInitializer;
import com.mediclinic.models.Appointment;
import com.mediclinic.models.Patient;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AgendaIntegrationTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("🧪 VALIDACIÓN DEL MÓDULO DE AGENDA INTERACTIVA & CITAS");
        System.out.println("=================================================");

        DatabaseInitializer.initializeDatabase();

        AppointmentDAO appointmentDAO = new AppointmentDAO();
        PatientDAO patientDAO = new PatientDAO();

        // 1. Probar inserción y consulta en AppointmentDAO
        System.out.println("\n[1] Probando persistencia de citas en agenda_citas...");
        Appointment testApp = new Appointment(
                null,
                "V-77889900",
                "Rodrigo",
                "Mendoza",
                "0414-5556677",
                LocalDate.now(),
                "10:30 AM",
                "Consulta Cardiológica",
                "Primera visita"
        );

        boolean inserted = appointmentDAO.insert(testApp);
        if (!inserted || testApp.getId() <= 0) {
            System.err.println("❌ Error al insertar cita en agenda_citas.");
            System.exit(1);
        }
        System.out.println("✅ Cita insertada exitosamente con ID #" + testApp.getId());

        List<Appointment> todayApps = appointmentDAO.findByDate(LocalDate.now());
        boolean foundToday = todayApps.stream().anyMatch(a -> a.getId() == testApp.getId());
        if (foundToday) {
            System.out.println("✅ Cita encontrada en consulta por fecha (Hoy): " + testApp.getFullName() + " a las " + testApp.getAppointmentTime());
        } else {
            System.err.println("❌ Cita no encontrada en findByDate.");
            System.exit(1);
        }

        List<Appointment> monthApps = appointmentDAO.findByMonthYear(LocalDate.now().getMonthValue(), LocalDate.now().getYear());
        boolean foundMonth = monthApps.stream().anyMatch(a -> a.getId() == testApp.getId());
        if (foundMonth) {
            System.out.println("✅ Cita encontrada en consulta mensual (Mes actual). Total citas en el mes: " + monthApps.size());
        } else {
            System.err.println("❌ Cita no encontrada en findByMonthYear.");
            System.exit(1);
        }

        // 2. Probar eliminación de cita
        System.out.println("\n[2] Probando eliminación de cita...");
        boolean deleted = appointmentDAO.delete(testApp.getId());
        Appointment checkDeleted = appointmentDAO.findById(testApp.getId());
        if (deleted && checkDeleted == null) {
            System.out.println("✅ Cita eliminada exitosamente de la base de datos.");
        } else {
            System.err.println("❌ Error al eliminar cita.");
            System.exit(1);
        }

        // 3. Probar autocompletado de paciente por cédula
        System.out.println("\n[3] Probando autocompletado de datos por cédula...");
        List<Patient> patients = patientDAO.findAll();
        if (!patients.isEmpty()) {
            Patient p = patients.get(0);
            Patient found = patientDAO.findByIdCard(p.getIdCard());
            if (found != null && found.getFullName().equals(p.getFullName())) {
                System.out.println("✅ Paciente autocompletado correctamente: " + found.getFullName() + " (" + found.getIdCard() + ") | Tel: " + found.getPhone());
            } else {
                System.err.println("❌ Fallo en autocompletado por cédula.");
                System.exit(1);
            }
        }

        // 4. Probar carga de FXML de AgendaView en JavaFX
        System.out.println("\n[4] Probando carga de FXML y Controlador de Agenda...");
        CountDownLatch fxLatch = new CountDownLatch(1);
        try {
            Platform.startup(fxLatch::countDown);
        } catch (IllegalStateException e) {
            fxLatch.countDown();
        }

        CountDownLatch viewLatch = new CountDownLatch(1);
        AtomicBoolean viewOk = new AtomicBoolean(true);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(AgendaIntegrationTest.class.getResource("/views/AgendaView.fxml"));
                loader.load();
                AgendaViewController controller = loader.getController();

                // Test métodos del controlador
                controller.handleToday();
                controller.renderCalendar();
                controller.loadDayAppointments();

                System.out.println("✅ AgendaView.fxml y AgendaViewController inicializados y ejecutados correctamente.");
            } catch (Exception e) {
                System.err.println("❌ Error al cargar AgendaView.fxml: " + e.getMessage());
                e.printStackTrace();
                viewOk.set(false);
            } finally {
                viewLatch.countDown();
            }
        });

        try {
            viewLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!viewOk.get()) {
            System.err.println("❌ Error en la carga de la vista de Agenda.");
            System.exit(1);
        }

        System.out.println("\n=================================================");
        System.out.println("🎉 MÓDULO DE AGENDA INTERACTIVA Y SINCRONIZADA VALIDADO AL 100%");
        System.out.println("=================================================");
        System.exit(0);
    }
}
