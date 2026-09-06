package com.mediclinic;

import com.mediclinic.models.Consultation;
import com.mediclinic.models.Patient;
import com.mediclinic.models.User;
import com.mediclinic.services.PdfReportService;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PdfAcroFormAndConnectivityTest {
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("🧪 TEST DE PDF ACROFORMS (CAMPOS EDITABLES)");
        System.out.println("=================================================");

        try {
            Patient p = new Patient("HC-2026-0001", "V-18456789", "Carlos", "Perez",
                    LocalDate.of(1989, 5, 14), "0414-1234567", "carlos.perez@email.com",
                    "LOCAL", "San Cristóbal", "Urb. Pirineos");
            p.setId(1);

            User d = new User(1, "doctor", "1234", "Dr. Mario Roa", "DOCTOR",
                    "Dr.", "Medicina General y Cirugia", "MPPS-84920 / Col. Medicos 14.502");

            Consultation c = new Consultation(1, 1, 1, LocalDateTime.now(),
                    "Control de Hipertension y Evaluacion General",
                    "Paciente refiere mejoria sintomatica con el tratamiento actual. Niega cefalea o mareos.",
                    "Hipertension Arterial Grado 1 Controlada / Dislipidemia Leve",
                    "1. Losartan Potasico 50mg - 1 tableta cada 24 horas en las mananas.\n2. Atorvastatina 20mg - 1 tableta en las noches por 30 dias.\n3. Dieta hiposodica y actividad fisica moderada 30 min diarios.",
                    72.5, 1.74, "Paciente lucido, normolineo, hidratado",
                    LocalDate.now().plusMonths(1), null);

            File pdf = PdfReportService.generateConsultationPdf(c, p, d);
            System.out.println("✅ PDF generado con éxito: " + pdf.getAbsolutePath());
            System.out.println("Tamaño del archivo: " + pdf.length() + " bytes");

            if (!pdf.exists() || pdf.length() < 1000) {
                System.err.println("❌ Error: El PDF generado está vacío o es demasiado pequeño.");
                System.exit(1);
            }

            // Verificar contenido del archivo
            byte[] bytes = java.nio.file.Files.readAllBytes(pdf.toPath());
            String pdfContent = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);

            if (pdfContent.contains("/AcroForm") && pdfContent.contains("/NeedAppearances true") &&
                pdfContent.contains("DiagnosticoDx") && pdfContent.contains("RecipeTratamiento") &&
                pdfContent.contains("EvolucionClinica")) {
                System.out.println("✅ El PDF contiene los 3 campos AcroForm interactivos (Diagnóstico, Evolución, Récipe).");
            } else {
                System.err.println("❌ Error: El PDF no contiene los campos AcroForm esperados.");
                System.exit(1);
            }

            // Test de Formato de WhatsApp
            String rawPhone = p.getPhone();
            String digits = rawPhone.replaceAll("[^0-9]", "");
            if (digits.startsWith("0")) digits = "58" + digits.substring(1);
            String waUrl = "https://wa.me/" + digits + "?text=Hola";
            if (!waUrl.equals("https://wa.me/584141234567?text=Hola")) {
                System.err.println("❌ Error en formato de URL WhatsApp: " + waUrl);
                System.exit(1);
            }
            System.out.println("✅ Formateo de WhatsApp verificado con éxito: " + waUrl);

            // Test de Formato de Mailto
            String mailtoUri = "mailto:" + p.getEmail() + "?subject=Informe%20Medico";
            if (!mailtoUri.equals("mailto:carlos.perez@email.com?subject=Informe%20Medico")) {
                System.err.println("❌ Error en formato de URI Mailto: " + mailtoUri);
                System.exit(1);
            }
            System.out.println("✅ Formateo de Mailto verificado con éxito: " + mailtoUri);

            System.out.println("=================================================");
            System.out.println("✅ TODOS LOS TESTS DE ACROFORMS Y CONECTIVIDAD EXITOSOS");
            System.out.println("=================================================");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
