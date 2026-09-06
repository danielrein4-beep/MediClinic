package com.mediclinic;

import com.mediclinic.dao.ConsultationDAO;
import com.mediclinic.dao.PatientDAO;
import com.mediclinic.dao.UserDAO;
import com.mediclinic.database.DatabaseInitializer;
import com.mediclinic.models.Consultation;
import com.mediclinic.models.Patient;
import com.mediclinic.models.User;
import com.mediclinic.services.PdfReportService;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class Phase4Test {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("🧪 MEDICLINIC PRO - VALIDACIÓN DE FASE 4");
        System.out.println("=================================================");

        try {
            // 1. Inicializar base de datos SQLite
            System.out.println("\n[1] Inicializando base de datos SQLite...");
            DatabaseInitializer.initializeDatabase();

            PatientDAO patientDAO = new PatientDAO();
            ConsultationDAO consultationDAO = new ConsultationDAO();
            UserDAO userDAO = new UserDAO();

            // 2. Obtener paciente y médico de prueba
            System.out.println("\n[2] Obteniendo paciente y médico titular para la consulta...");
            List<Patient> patients = patientDAO.findAll();
            if (patients.isEmpty()) {
                throw new RuntimeException("Error: No se encontraron pacientes en base de datos.");
            }
            Patient patient = patients.get(0);
            User doctor = userDAO.authenticate("doctor", "1234");
            if (doctor == null) {
                doctor = userDAO.findFirstDoctor();
            }

            System.out.println("✅ Paciente seleccionado: " + patient.getFullName() + " (C.I: " + patient.getIdCard() + " - " + patient.getFormattedOrigin() + ")");
            System.out.println("✅ Médico tratante: Dr. " + doctor.getFullName() + " (" + doctor.getSpecialty() + ")");

            // 3. Registrar Nueva Consulta con Datos Físicos, Diagnóstico, Récipe y Próxima Cita
            System.out.println("\n[3] Registrando nueva consulta médica con datos físicos y prescripción...");
            Consultation newConsult = new Consultation(
                    -1,
                    patient.getId(),
                    doctor.getId(),
                    LocalDateTime.now(),
                    "Control médico anual y evaluación digestiva",
                    "Paciente refiere epigastralgia leve postprandial. Sin fiebre ni vómitos. Abdomen blando, depresible, doloroso a la palpación en epigastrio.",
                    "Gastritis aguda leve / Dispepsia funcional",
                    "1. Omeprazol 20mg - 1 cápsula vía oral en ayunas por 14 días.\n2. Sucralfato 1g - 1 sobre 30 min antes de cada comida por 7 días.\n3. Dieta blanda fraccionada, evitar irritantes.",
                    74.5,
                    1.75,
                    "Contextura normolínea, hidratado, afebril, sin edemas.",
                    LocalDate.now().plusDays(15),
                    null
            );

            boolean inserted = consultationDAO.insert(newConsult);
            if (!inserted || newConsult.getId() <= 0) {
                throw new RuntimeException("Error al insertar la consulta en la base de datos.");
            }
            System.out.println("✅ Consulta guardada con éxito con ID: " + newConsult.getId());
            System.out.println("✅ IMC calculado: " + newConsult.calculateBMI() + " kg/m²");

            // 4. Validar Historial Cronológico de Consultas del Paciente
            System.out.println("\n[4] Consultando historial cronológico del paciente...");
            List<Consultation> history = consultationDAO.findByPatientId(patient.getId());
            if (history.isEmpty()) {
                throw new RuntimeException("Error: El historial de consultas del paciente no arrojó resultados.");
            }
            System.out.println("✅ Historial recuperado: " + history.size() + " consulta(s) encontrada(s).");
            Consultation latest = history.get(0);
            System.out.println("✅ Última consulta: " + latest.getReason() + " | Dx: " + latest.getDiagnosis() + " | Próx. Cita: " + latest.getNextAppointmentDate());

            // 5. Generar y Validar Documento PDF Automático
            System.out.println("\n[5] Generando Informe Médico / Récipe en formato PDF...");
            File pdfFile = PdfReportService.generateConsultationPdf(latest, patient, doctor);
            if (pdfFile == null || !pdfFile.exists() || pdfFile.length() == 0) {
                throw new RuntimeException("Error: El archivo PDF no se generó en disco.");
            }
            System.out.println("✅ Archivo PDF generado exitosamente en: " + pdfFile.getAbsolutePath());
            System.out.println("✅ Tamaño del archivo: " + pdfFile.length() + " bytes.");

            // Validar cabecera PDF
            byte[] header = new byte[8];
            try (FileInputStream fis = new FileInputStream(pdfFile)) {
                fis.read(header);
            }
            String headerStr = new String(header);
            if (!headerStr.startsWith("%PDF-")) {
                throw new RuntimeException("Error: La cabecera del archivo generado no corresponde a un PDF válido: " + headerStr);
            }
            System.out.println("✅ Cabecera PDF válida verificada (%PDF-1.4).");

            // Actualizar ruta en BD
            consultationDAO.updateIssuedDocumentPath(latest.getId(), pdfFile.getAbsolutePath());
            System.out.println("✅ Ruta del informe PDF registrada en la consulta.");

            // 6. Validar Recursos FXML
            System.out.println("\n[6] Validando recursos FXML y CSS de Fase 4...");
            checkResource("/views/ClinicalHistory.fxml");
            checkResource("/views/DoctorDashboard.fxml");
            checkResource("/views/PatientsView.fxml");
            checkResource("/css/style.css");

            System.out.println("\n=================================================");
            System.out.println("🎉 FASE 4 COMPLETADA Y VERIFICADA CON ÉXITO");
            System.out.println("=================================================");

        } catch (Exception e) {
            System.err.println("❌ ERROR EN VALIDACIÓN DE FASE 4:");
            e.printStackTrace();
        }
    }

    private static void checkResource(String path) {
        try (InputStream is = Phase4Test.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Recurso no encontrado en classpath: " + path);
            }
            System.out.println("✅ Recurso validado: " + path);
        } catch (Exception e) {
            throw new RuntimeException("Fallo al leer recurso: " + path, e);
        }
    }
}
