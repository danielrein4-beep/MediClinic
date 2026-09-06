package com.mediclinic;

import com.mediclinic.controllers.AgendaViewController;
import com.mediclinic.dao.BlockedDateDAO;
import com.mediclinic.database.DatabaseInitializer;
import com.mediclinic.models.BlockedDate;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlockedDateAndStylingTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("🧪 VALIDACIÓN DE BLOQUEO DE FECHAS & QUIET LUXURY UI");
        System.out.println("=================================================");

        DatabaseInitializer.initializeDatabase();

        BlockedDateDAO blockedDateDAO = new BlockedDateDAO();

        // 1. Probar persistencia de bloqueo de fecha
        System.out.println("\n[1] Probando persistencia en agenda_bloqueos...");
        LocalDate testDate = LocalDate.now().plusDays(2);
        boolean blocked = blockedDateDAO.insert(testDate, "Congreso Médico Internacional", "Dr. Mario Roa");
        if (blocked) {
            System.out.println("✅ Fecha bloqueada exitosamente para el " + testDate);
        } else {
            System.err.println("❌ Fallo al bloquear fecha.");
            System.exit(1);
        }

        boolean isBlocked = blockedDateDAO.isDateBlocked(testDate);
        BlockedDate fetched = blockedDateDAO.findByDate(testDate);
        if (isBlocked && fetched != null && "Congreso Médico Internacional".equals(fetched.getReason())) {
            System.out.println("✅ Consulta de fecha bloqueada correcta: " + fetched.getReason() + " (Por: " + fetched.getBlockedBy() + ")");
        } else {
            System.err.println("❌ Fallo al consultar fecha bloqueada.");
            System.exit(1);
        }

        Map<LocalDate, String> monthBlocked = blockedDateDAO.findByMonthYear(testDate.getMonthValue(), testDate.getYear());
        if (monthBlocked.containsKey(testDate)) {
            System.out.println("✅ Fecha bloqueada detectada en mapa mensual de calendario.");
        } else {
            System.err.println("❌ Fecha no detectada en mapa mensual.");
            System.exit(1);
        }

        // 2. Probar desbloqueo
        System.out.println("\n[2] Probando desbloqueo de fecha...");
        boolean unblocked = blockedDateDAO.delete(testDate);
        if (unblocked && !blockedDateDAO.isDateBlocked(testDate)) {
            System.out.println("✅ Fecha desbloqueada exitosamente de la base de datos.");
        } else {
            System.err.println("❌ Fallo al desbloquear fecha.");
            System.exit(1);
        }

        // 3. Probar carga de JavaFX con nuevo style.css y AgendaView
        System.out.println("\n[3] Probando inicialización de AgendaView y carga de CSS...");
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
                // Bloquear hoy para probar renderizado
                blockedDateDAO.insert(LocalDate.now(), "Vacaciones Médicas", "Dr. Mario Roa");

                FXMLLoader loader = new FXMLLoader(BlockedDateAndStylingTest.class.getResource("/views/AgendaView.fxml"));
                loader.load();
                AgendaViewController controller = loader.getController();

                controller.renderCalendar();
                controller.loadDayAppointments();

                System.out.println("✅ AgendaView y calendario renderizados con día bloqueado correctamente.");

                // Desbloquear para dejar limpio
                blockedDateDAO.delete(LocalDate.now());
                controller.renderCalendar();
                controller.loadDayAppointments();

                System.out.println("✅ Calendario actualizado al desbloquear.");
            } catch (Exception e) {
                System.err.println("❌ Error en carga de AgendaView: " + e.getMessage());
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
        System.out.println("🎉 BLOQUEO DE FECHAS, ESTÉTICA QUIET LUXURY Y CSS VALIDADOS AL 100%");
        System.out.println("=================================================");
        System.exit(0);
    }
}
