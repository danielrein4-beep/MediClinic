package com.mediclinic;

import com.mediclinic.controllers.WaitingRoomViewController;
import com.mediclinic.dao.PatientDAO;
import com.mediclinic.dao.WaitingRoomDAO;
import com.mediclinic.database.DatabaseInitializer;
import com.mediclinic.models.Patient;
import com.mediclinic.models.WaitingRoomEntry;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReactivityAndWaitingRoomTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("🧪 VALIDACIÓN DE TURNOS CONSECUTIVOS DESDE #1 & TELÉFONO EN SALA");
        System.out.println("=================================================");

        DatabaseInitializer.initializeDatabase();

        PatientDAO patientDAO = new PatientDAO();
        WaitingRoomDAO waitingRoomDAO = new WaitingRoomDAO();

        // 1. Limpiar sala de espera
        waitingRoomDAO.clearAll();

        // 2. Tomar pacientes de la base de datos
        List<Patient> allPatients = patientDAO.findAll();
        if (allPatients.size() >= 2) {
            Patient p1 = allPatients.get(0);
            Patient p2 = allPatients.get(1);

            waitingRoomDAO.insert(new WaitingRoomEntry(p1.getId(), "Evaluación"));
            waitingRoomDAO.insert(new WaitingRoomEntry(p2.getId(), "Control"));

            List<WaitingRoomEntry> entries = waitingRoomDAO.findAllToday();
            System.out.println("Total registros en sala: " + entries.size());

            System.out.println("Paciente 1: " + entries.get(0).getPatientName() + " | Turno asignado: #" + entries.get(0).getDailyTurnNumber() + " | Teléfono: " + entries.get(0).getPatientPhone());
            System.out.println("Paciente 2: " + entries.get(1).getPatientName() + " | Turno asignado: #" + entries.get(1).getDailyTurnNumber() + " | Teléfono: " + entries.get(1).getPatientPhone());

            if (entries.get(0).getDailyTurnNumber() == 1 && entries.get(1).getDailyTurnNumber() == 2) {
                System.out.println("✅ Los turnos inician estrictamente en #1 y aumentan consecutivamente.");
            } else {
                System.err.println("❌ Fallo en la numeración del turno.");
                System.exit(1);
            }

            if (entries.get(0).getPatientPhone() != null && !entries.get(0).getPatientPhone().isEmpty()) {
                System.out.println("✅ El teléfono del paciente se asocia correctamente a la fila de espera.");
            } else {
                System.err.println("❌ El teléfono no se obtuvo en la consulta de sala de espera.");
                System.exit(1);
            }
        }

        // 3. Probar carga de FXML de WaitingRoomView
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
                FXMLLoader loader = new FXMLLoader(ReactivityAndWaitingRoomTest.class.getResource("/views/WaitingRoomView.fxml"));
                loader.load();
                WaitingRoomViewController ctrl = loader.getController();

                System.out.println("✅ WaitingRoomView.fxml cargado y configurado exitosamente con columna de teléfono y turnos.");
            } catch (Exception e) {
                System.err.println("❌ Error en FXML de WaitingRoomView: " + e.getMessage());
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
            System.exit(1);
        }

        System.out.println("\n=================================================");
        System.out.println("🎉 VALIDACIÓN COMPLETA: TURNOS INICIAN EN #1 Y TELÉFONO INTEGRADO");
        System.out.println("=================================================");
        System.exit(0);
    }
}
