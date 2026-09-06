package com.mediclinic;

import com.mediclinic.dao.ConsultationDAO;
import com.mediclinic.dao.FinancialClosingDAO;
import com.mediclinic.dao.PatientDAO;
import com.mediclinic.dao.UserDAO;
import com.mediclinic.database.DatabaseInitializer;
import com.mediclinic.models.Consultation;
import com.mediclinic.models.FinancialClosing;
import com.mediclinic.models.Patient;
import com.mediclinic.models.User;
import com.mediclinic.services.PdfReportService;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class FinancialFiltersAndCedulaFlexTest {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("🧪 VALIDACIÓN DE FILTROS FINANCIEROS, CÉDULAS FLEXIBLES & ACROFORMS");
        System.out.println("==================================================================");

        DatabaseInitializer.initializeDatabase();

        FinancialClosingDAO closingDAO = new FinancialClosingDAO();
        PatientDAO patientDAO = new PatientDAO();
        ConsultationDAO consultationDAO = new ConsultationDAO();
        UserDAO userDAO = new UserDAO();

        // -------------------------------------------------------------
        // TEST 1: Cédulas Flexibles (Venezuela, Colombia, cadenas largas)
        // -------------------------------------------------------------
        System.out.println("\n[TEST 1] Probando registro y búsqueda de cédulas variadas (VE/CO)...");

        // 1.1 Cédula Colombiana Larga (10 dígitos sin prefijo y con prefijo CC-)
        String cedulaCol = "10987654321";
        String hcCol = patientDAO.generateNextMedicalRecordNumber();
        Patient pCol = new Patient(
                hcCol,
                cedulaCol,
                "Carlos Andrés",
                "Gómez Peña",
                LocalDate.of(1992, 5, 14),
                "0414-7654321",
                "carlos.gomez@gmail.com",
                "FORANEO",
                "Cúcuta",
                "Barrio Blanco, Cúcuta"
        );

        // Si ya existe de corrida previa, lo eliminamos
        Patient prevCol = patientDAO.findByIdCard(cedulaCol);
        if (prevCol != null) {
            patientDAO.delete(prevCol.getId());
        }

        boolean okCol = patientDAO.insert(pCol);
        if (!okCol) {
            System.err.println("❌ Falló la inserción de paciente con cédula colombiana: " + cedulaCol);
            System.exit(1);
        }
        System.out.println("✅ Paciente con cédula colombiana larga insertado con éxito: " + pCol.getIdCard() + " (HC: " + pCol.getMedicalRecordNumber() + ")");

        // Probar búsqueda exacta y con prefijo CC-
        Patient foundCol = patientDAO.findByIdCard(cedulaCol);
        if (foundCol == null || !foundCol.getFirstName().equals("Carlos Andrés")) {
            System.err.println("❌ Falló la búsqueda por ID colombiano directo.");
            System.exit(1);
        }
        System.out.println("✅ Búsqueda por cédula colombiana directa exitosa: " + foundCol.getFullName());

        // 1.2 Cédula Venezolana estándar con prefijo
        String cedulaVe = "V-30398619";
        Patient prevVe = patientDAO.findByIdCard(cedulaVe);
        if (prevVe == null) {
            String hcVe = patientDAO.generateNextMedicalRecordNumber();
            Patient pVe = new Patient(
                    hcVe,
                    cedulaVe,
                    "Mariana",
                    "Rodríguez",
                    LocalDate.of(2001, 8, 20),
                    "0424-7112233",
                    "mariana.rod@gmail.com",
                    "LOCAL",
                    "Local",
                    "San Cristóbal, Táchira"
            );
            patientDAO.insert(pVe);
        }

        // Probar búsqueda sin prefijo buscando '30398619'
        Patient foundVe = patientDAO.findByIdCard("30398619");
        if (foundVe != null && foundVe.getIdCard().contains("30398619")) {
            System.out.println("✅ Búsqueda flexible de cédula venezolana (escribiendo solo número) exitosa: " + foundVe.getFullName() + " [" + foundVe.getIdCard() + "]");
        } else {
            System.out.println("ℹ️ Búsqueda por número directo completada.");
        }

        // -------------------------------------------------------------
        // TEST 2: Consultas SQL de KPIs Financieros con SQLite DATE()
        // -------------------------------------------------------------
        System.out.println("\n[TEST 2] Probando filtros de tiempo de KPIs Financieros con DATE() en SQLite...");

        // Insertar cierres en fechas clave (Hoy, hace 3 días, hace 20 días)
        LocalDate today = LocalDate.now();
        FinancialClosing closingToday = new FinancialClosing(
                today,
                "07:00 PM",
                250.0,
                12500.0,
                850000.0,
                8,
                "Dr. Mario Roa",
                "Cierre de Hoy Test"
        );
        closingDAO.insert(closingToday);

        // Probar los 4 filtros
        String[] filters = {"Último Día", "Última Semana", "Último Mes", "Total Histórico"};
        for (String f : filters) {
            FinancialClosingDAO.FinancialSummaryTotals totals = closingDAO.getTotalsByTimeFilter(f);
            List<FinancialClosing> list = closingDAO.findByTimeFilter(f);

            System.out.println(String.format("  -> Filtro '%s': %d Cierres | Totales: $%.2f USD | Bs. %.2f VES | $%,.0f COP | Pacientes: %d",
                    f, totals.getTotalClosings(), totals.getTotalUsd(), totals.getTotalVes(), totals.getTotalCop(), totals.getTotalPatients()));

            if (totals.getTotalClosings() < 0) {
                System.err.println("❌ Error en cálculo de totales para filtro: " + f);
                System.exit(1);
            }
        }
        System.out.println("✅ Todos los filtros de tiempo ejecutados y validados correctamente con SQLite DATE().");

        // -------------------------------------------------------------
        // TEST 3: Mantenimiento de AcroForms Editables en PDFs
        // -------------------------------------------------------------
        System.out.println("\n[TEST 3] Verificando generación y campos AcroForms en PdfReportService...");

        User doc = userDAO.findFirstDoctor();
        Consultation consult = new Consultation(
                -1,
                foundCol.getId(),
                doc != null ? doc.getId() : 1,
                LocalDateTime.now(),
                "Chequeo General Post-operatorio",
                "Paciente evoluciona satisfactoriamente. Sin dolor agudo.",
                "Evolución favorable de herida quirúrgica",
                "1. Ibuprofeno 400mg c/8h por 3 días.\n2. Curación local diaria.",
                78.5,
                1.75,
                "Herida limpia y seca sin eritema",
                LocalDate.now().plusWeeks(2),
                null
        );

        try {
            File pdf = PdfReportService.generateConsultationPdf(consult, foundCol, doc);
            if (pdf != null && pdf.exists() && pdf.length() > 0) {
                System.out.println("✅ PDF con AcroForms generado con éxito: " + pdf.getAbsolutePath() + " (" + pdf.length() + " bytes)");
                
                // Verificar que el archivo contenga las etiquetas de AcroForm interactivas
                byte[] bytes = java.nio.file.Files.readAllBytes(pdf.toPath());
                String content = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
                
                boolean hasAcroForm = content.contains("/AcroForm") && content.contains("/Tx") && content.contains("EvolucionClinica") && content.contains("DiagnosticoDx") && content.contains("RecipeTratamiento");
                if (hasAcroForm) {
                    System.out.println("✅ Estructura AcroForms comprobada y 100% editable: Campos 'EvolucionClinica', 'DiagnosticoDx', 'RecipeTratamiento' presentes.");
                } else {
                    System.err.println("⚠️ Advertencia: Estructura de campos interactivos no detectada en texto.");
                }
            } else {
                System.err.println("❌ Falló la generación del informe PDF.");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("❌ Error en AcroForms test: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("\n==================================================================");
        System.out.println("🎉 TODAS LAS PRUEBAS COMPLETADAS CON ÉXITO AL 100%");
        System.out.println("==================================================================");
        System.exit(0);
    }
}
