package com.mediclinic;

import com.mediclinic.dao.ConfigDAO;
import com.mediclinic.dao.PatientDAO;
import com.mediclinic.dao.ProcedureDAO;
import com.mediclinic.database.DatabaseInitializer;
import com.mediclinic.models.Patient;
import com.mediclinic.models.ProcedureQuote;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

public class Phase5Test {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("🧪 MEDICLINIC PRO - VALIDACIÓN DE FASE 5");
        System.out.println("Módulo de Procedimientos & Cotizador Multimoneda");
        System.out.println("=================================================");

        try {
            // 1. Inicializar base de datos
            System.out.println("\n[1] Inicializando base de datos SQLite...");
            DatabaseInitializer.initializeDatabase();

            ConfigDAO configDAO = new ConfigDAO();
            PatientDAO patientDAO = new PatientDAO();
            ProcedureDAO procedureDAO = new ProcedureDAO();

            // 2. Verificar lectura de tasas de cambio
            System.out.println("\n[2] Verificando tasas de cambio activas en base de datos...");
            double tasaVes = configDAO.getDoubleValue("TASA_USD_VES", 38.50);
            double tasaCop = configDAO.getDoubleValue("TASA_USD_COP", 4100.0);
            System.out.printf("✅ Tasa USD -> VES: Bs. %.2f%n", tasaVes);
            System.out.printf("✅ Tasa USD -> COP: $ %,.0f%n", tasaCop);

            if (tasaVes <= 0 || tasaCop <= 0) {
                throw new RuntimeException("Error: Tasas de cambio no válidas.");
            }

            // 3. Obtener paciente de prueba
            System.out.println("\n[3] Seleccionando paciente de prueba...");
            List<Patient> patients = patientDAO.findAll();
            if (patients.isEmpty()) {
                throw new RuntimeException("Error: No se encontraron pacientes en base de datos.");
            }
            Patient patient = patients.get(0);
            System.out.println("✅ Paciente seleccionado: " + patient.getFullName() + " (C.I: " + patient.getIdCard() + ")");

            // 4. Crear Nueva Cotización de Procedimiento Quirúrgico en USD
            System.out.println("\n[4] Creando nueva cotización multimoneda en tiempo real...");
            double baseUsd = 250.00;
            double expectedVes = baseUsd * tasaVes;
            double expectedCop = baseUsd * tasaCop;

            ProcedureQuote quote = new ProcedureQuote(
                    patient.getId(),
                    "Cirugía Menor - Extirpación de Lipoma Dorsal",
                    "Procedimiento bajo anestesia local en quirófano ambulatorio",
                    baseUsd,
                    tasaVes,
                    tasaCop,
                    "COTIZADA",
                    null,
                    "Incluye honorarios quirúrgicos, material estéril y control postoperatorio a los 7 días."
            );

            System.out.printf("   Monto Base USD: $ %.2f%n", quote.getAmountUsd());
            System.out.printf("   Monto Calculado VES: Bs. %.2f (Esperado: Bs. %.2f)%n", quote.getAmountVes(), expectedVes);
            System.out.printf("   Monto Calculado COP: $ %,.0f (Esperado: $ %,.0f)%n", quote.getAmountCop(), expectedCop);

            if (Math.abs(quote.getAmountVes() - expectedVes) > 0.01 || Math.abs(quote.getAmountCop() - expectedCop) > 0.01) {
                throw new RuntimeException("Error: El cálculo multidivisa no coincide exactamente con las tasas.");
            }

            boolean inserted = procedureDAO.insert(quote);
            if (!inserted || quote.getId() <= 0) {
                throw new RuntimeException("Error al insertar cotización en la base de datos.");
            }
            System.out.println("✅ Cotización registrada con ID #" + quote.getId() + " en estado: " + quote.getStatus());

            // 5. Transición de Estado 1: COTIZADA ➔ PLANIFICADA (con fecha agendada)
            System.out.println("\n[5] Probando flujo de estado: COTIZADA ➔ PLANIFICADA...");
            LocalDateTime plannedDate = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0);
            boolean plannedOk = procedureDAO.updateStatus(quote.getId(), "PLANIFICADA", plannedDate);
            if (!plannedOk) {
                throw new RuntimeException("Error al cambiar estado a PLANIFICADA.");
            }
            System.out.println("✅ Estado actualizado a 'PLANIFICADA' para la fecha: " + plannedDate);

            // 6. Transición de Estado 2: PLANIFICADA ➔ REALIZADA
            System.out.println("\n[6] Probando flujo de estado: PLANIFICADA ➔ REALIZADA...");
            boolean realizedOk = procedureDAO.updateStatus(quote.getId(), "REALIZADA");
            if (!realizedOk) {
                throw new RuntimeException("Error al cambiar estado a REALIZADA.");
            }
            System.out.println("✅ Estado actualizado a 'REALIZADA' satisfactoriamente.");

            // 7. Validar Búsqueda y Filtros en Procedimientos
            System.out.println("\n[7] Validando filtros por estado y búsquedas...");
            List<ProcedureQuote> allQuotes = procedureDAO.findAll();
            List<ProcedureQuote> completedQuotes = procedureDAO.findByStatus("REALIZADA");
            List<ProcedureQuote> searchResults = procedureDAO.search(patient.getIdCard(), "TODOS");

            System.out.println("✅ Total cotizaciones/procedimientos en BD: " + allQuotes.size());
            System.out.println("✅ Total realizados en BD: " + completedQuotes.size());
            System.out.println("✅ Búsqueda por cédula '" + patient.getIdCard() + "': " + searchResults.size() + " coincidencia(s)");

            if (allQuotes.isEmpty() || completedQuotes.isEmpty() || searchResults.isEmpty()) {
                throw new RuntimeException("Error: Los filtros o búsquedas de procedimientos no arrojaron resultados.");
            }

            // 8. Validar Recursos FXML y CSS
            System.out.println("\n[8] Validando recursos FXML y CSS de Fase 5...");
            checkResource("/views/ProceduresView.fxml");
            checkResource("/views/DoctorDashboard.fxml");
            checkResource("/views/SecretaryDashboard.fxml");
            checkResource("/views/PatientsView.fxml");
            checkResource("/css/style.css");

            System.out.println("\n=================================================");
            System.out.println("🎉 FASE 5 COMPLETADA Y VERIFICADA CON ÉXITO");
            System.out.println("=================================================");

        } catch (Exception e) {
            System.err.println("❌ ERROR EN VALIDACIÓN DE FASE 5:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void checkResource(String path) {
        try (InputStream is = Phase5Test.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Recurso no encontrado en classpath: " + path);
            }
            System.out.println("✅ Recurso validado: " + path);
        } catch (Exception e) {
            throw new RuntimeException("Fallo al leer recurso: " + path, e);
        }
    }
}
