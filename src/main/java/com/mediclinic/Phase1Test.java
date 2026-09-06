package com.mediclinic;

import com.mediclinic.dao.*;
import com.mediclinic.database.DatabaseConnection;
import com.mediclinic.database.DatabaseInitializer;
import com.mediclinic.models.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class Phase1Test {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("🧪 MEDICLINIC PRO - VALIDACIÓN DE FASE 1");
        System.out.println("=================================================");

        try {
            // 1. Inicializar base de datos SQLite
            System.out.println("\n[1] Inicializando base de datos SQLite...");
            DatabaseInitializer.initializeDatabase();
            System.out.println("✅ Base de datos creada en: " + DatabaseConnection.getDatabasePath());

            // 2. Probar DAO de Usuarios y Autenticación
            System.out.println("\n[2] Probando autenticación de usuarios...");
            UserDAO userDAO = new UserDAO();
            User doctor = userDAO.authenticate("doctor", "1234");
            if (doctor != null) {
                System.out.println("✅ Doctor autenticado: " + doctor.getFormalGreetingName() + " (" + doctor.getSpecialty() + ")");
            } else {
                throw new RuntimeException("Fallo en la autenticación del doctor!");
            }

            User secretary = userDAO.authenticate("secretaria", "1234");
            if (secretary != null) {
                System.out.println("✅ Secretaria autenticada: " + secretary.getFormalGreetingName() + " (" + secretary.getRole() + ")");
            } else {
                throw new RuntimeException("Fallo en la autenticación de la secretaria!");
            }

            // 3. Probar DAO de Pacientes y búsqueda por cédula
            System.out.println("\n[3] Probando registro y búsqueda de pacientes...");
            PatientDAO patientDAO = new PatientDAO();
            
            // Buscar paciente semilla
            Patient p1 = patientDAO.findByIdCard("V-18456789");
            if (p1 != null) {
                System.out.println("✅ Paciente encontrado por cédula: " + p1.getFullName() + " | HC: " + p1.getMedicalRecordNumber() + " | Origen: " + p1.getFormattedOrigin());
            }

            // Registrar nuevo paciente de prueba
            String nextHC = patientDAO.generateNextMedicalRecordNumber();
            String testCedula = "V-30" + (int)(Math.random() * 899999 + 100000);
            Patient newPatient = new Patient(nextHC, testCedula, "Gabriel", "Contreras",
                    LocalDate.of(2000, 3, 10), "0414-9988776", "gabriel@test.com", "FORANEO", "Caracas", "Altamira Norte");
            boolean inserted = patientDAO.insert(newPatient);
            System.out.println("✅ Inserción de nuevo paciente (" + testCedula + "): " + (inserted ? "EXITOSA con ID=" + newPatient.getId() : "FALLIDA"));

            // 4. Probar DAO de Consultas
            System.out.println("\n[4] Probando registro de consulta clínica y signos vitales...");
            ConsultationDAO consultationDAO = new ConsultationDAO();
            Consultation consult = new Consultation();
            consult.setPatientId(newPatient.getId());
            consult.setDoctorId(doctor.getId());
            consult.setDateTime(LocalDateTime.now());
            consult.setReason("Control de rutina y evaluación general");
            consult.setClinicalNotes("Paciente refiere bienestar general, asintomático.");
            consult.setDiagnosis("Z00.0 - Examen médico general");
            consult.setTreatmentRx("1. Hidratación adecuada.\n2. Complejo B 1 tableta diaria por 30 días.");
            consult.setBloodPressure("120/80");
            consult.setHeartRate(72);
            consult.setWeightKg(74.5);
            consult.setHeightM(1.78);
            consult.setTemperature(36.6);
            consult.setNextAppointmentDate(LocalDate.now().plusMonths(3));
            
            boolean consultSaved = consultationDAO.insert(consult);
            System.out.println("✅ Inserción de consulta: " + (consultSaved ? "EXITOSA con ID=" + consult.getId() : "FALLIDA"));
            System.out.println("   IMC Calculado: " + consult.calculateBMI() + " kg/m²");
            System.out.println("   Consultas registradas hoy: " + consultationDAO.countConsultationsToday());

            // 5. Probar DAO de Procedimientos y Cotizaciones Multidivisa
            System.out.println("\n[5] Probando cotización multidivisa (USD / VES / COP)...");
            ConfigDAO configDAO = new ConfigDAO();
            double tasaVes = configDAO.getDoubleValue("TASA_USD_VES", 38.50);
            double tasaCop = configDAO.getDoubleValue("TASA_USD_COP", 4100.0);

            ProcedureDAO procedureDAO = new ProcedureDAO();
            ProcedureQuote quote = new ProcedureQuote(
                    newPatient.getId(),
                    "Procedimiento Ambulatorio Menor",
                    "Cura quirúrgica menor y retiro de lesión dérmica",
                    120.0, // Monto en USD
                    tasaVes,
                    tasaCop,
                    "PLANIFICADA",
                    LocalDateTime.now().plusDays(2),
                    "Paciente debe acudir en ayunas"
            );
            boolean quoteSaved = procedureDAO.insert(quote);
            System.out.println("✅ Inserción de cotización: " + (quoteSaved ? "EXITOSA" : "FALLIDA"));
            System.out.println("   Monto USD: $" + String.format("%.2f", quote.getAmountUsd()));
            System.out.println("   Monto VES (Tasa " + tasaVes + "): Bs. " + String.format("%.2f", quote.getAmountVes()));
            System.out.println("   Monto COP (Tasa " + tasaCop + "): COP $" + String.format("%.2f", quote.getAmountCop()));

            List<ProcedureQuote> upcoming = procedureDAO.findUpcomingThisWeek();
            System.out.println("   Procedimientos planificados para esta semana: " + upcoming.size());

            System.out.println("\n=================================================");
            System.out.println("🎉 FASE 1 COMPLETADA Y VERIFICADA CON ÉXITO");
            System.out.println("=================================================");

        } catch (Exception e) {
            System.err.println("❌ ERROR DURANTE LA PRUEBA DE FASE 1:");
            e.printStackTrace();
        } finally {
            DatabaseConnection.closeConnection();
        }
    }
}
