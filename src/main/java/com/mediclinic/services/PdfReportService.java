package com.mediclinic.services;

import com.mediclinic.models.Consultation;
import com.mediclinic.models.Patient;
import com.mediclinic.models.User;

import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to generate professional medical report & prescription PDFs.
 * Produces strictly compliant standard PDF 1.4 documents with header,
 * physical data, diagnosis, Rx and doctor signature.
 */
public class PdfReportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Generates a PDF medical report / prescription for a given consultation and patient.
     */
    public static File generateConsultationPdf(Consultation consultation, Patient patient, User doctor) throws IOException {
        File reportsDir = new File("reports");
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }

        String safePatient = (patient.getFullName() != null ? patient.getFullName() : "Paciente")
                .replaceAll("[^a-zA-Z0-9_-]", "_");
        String filename = String.format("Informe_%s_%s_%d.pdf",
                patient.getMedicalRecordNumber() != null ? patient.getMedicalRecordNumber() : "HC",
                safePatient,
                System.currentTimeMillis() % 100000);
        File pdfFile = new File(reportsDir, filename);

        byte[] pdfBytes = buildPdfContent(consultation, patient, doctor);

        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            fos.write(pdfBytes);
        }

        return pdfFile;
    }

    /**
     * Opens the generated PDF in the system's default PDF viewer.
     */
    public static void openPdfFile(File file) {
        if (file == null || !file.exists()) return;
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
            } else {
                new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath()).start();
            }
        } catch (Exception e) {
            System.err.println("No se pudo abrir el visor de PDF automáticamente: " + e.getMessage());
        }
    }

    /**
     * Generates a professional Daily Cash Register Closing & Medical Day Report in PDF.
     */
    public static File generateDailyCashClosingPdf(List<com.mediclinic.models.DailyPayment> payments,
                                                  List<com.mediclinic.models.WaitingRoomEntry> waitingList,
                                                  User user) throws IOException {
        File reportsDir = new File("reports");
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String filename = String.format("Cierre_Caja_%s.pdf", timestamp);
        File pdfFile = new File(reportsDir, filename);

        byte[] pdfBytes = buildCashClosingPdfContent(payments, waitingList, user);

        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            fos.write(pdfBytes);
        }

        return pdfFile;
    }

    private static byte[] buildCashClosingPdfContent(List<com.mediclinic.models.DailyPayment> payments,
                                                    List<com.mediclinic.models.WaitingRoomEntry> waitingList,
                                                    User user) throws IOException {
        PdfBuilder builder = new PdfBuilder();

        // 1. Header Box
        builder.drawFilledRect(36, 695, 540, 80, 0.94f, 0.97f, 0.99f);
        builder.drawRect(36, 695, 540, 80, 0.05f, 0.65f, 0.91f, 1.5f);

        builder.drawText("CENTRO MEDICO MEDICLINIC", 48, 755, 12, true, 0.05f, 0.65f, 0.91f);
        builder.drawText("INFORME OFICIAL DE CIERRE DE CAJA & JORNADA MEDICA", 48, 740, 9.5f, true, 0.09f, 0.58f, 0.53f);
        builder.drawText("Control y Balance de Cobranza Diaria en Moneda Multiple", 48, 727, 8.5f, false, 0.28f, 0.33f, 0.41f);
        builder.drawText("Edif. Torre Medica | San Cristobal, Tachira", 48, 712, 8, false, 0.39f, 0.45f, 0.55f);

        String userName = user != null ? user.getFullName() : "Secretaria / Administrador";
        String userRole = user != null ? user.getRole() : "RECEPCION";
        String nowStr = LocalDateTime.now().format(DATE_TIME_FORMATTER);

        builder.drawText("Fecha y Hora Cierre: " + nowStr, 340, 755, 8.5f, true, 0.06f, 0.09f, 0.16f);
        builder.drawText("Responsable: " + sanitizeText(userName), 340, 741, 8.5f, false, 0.20f, 0.25f, 0.33f);
        builder.drawText("Rol en Turno: " + sanitizeText(userRole), 340, 728, 8.5f, false, 0.20f, 0.25f, 0.33f);
        builder.drawText("Pacientes en Sala: " + (waitingList != null ? waitingList.size() : 0), 340, 715, 8.5f, true, 0.05f, 0.65f, 0.91f);

        // 2. Table Header
        int currentY = 665;
        builder.drawFilledRect(36, currentY, 540, 22, 0.05f, 0.65f, 0.91f);
        builder.drawText("DETALLE DE PAGOS Y TRANSACCIONES REGISTRADAS", 46, currentY + 7, 9, true, 1.0f, 1.0f, 1.0f);

        currentY -= 20;
        builder.drawFilledRect(36, currentY, 540, 18, 0.90f, 0.93f, 0.96f);
        builder.drawRect(36, currentY, 540, 18, 0.80f, 0.85f, 0.90f, 0.8f);

        builder.drawText("#", 44, currentY + 5, 8, true, 0.10f, 0.15f, 0.20f);
        builder.drawText("HORA", 64, currentY + 5, 8, true, 0.10f, 0.15f, 0.20f);
        builder.drawText("PACIENTE / CEDULA", 115, currentY + 5, 8, true, 0.10f, 0.15f, 0.20f);
        builder.drawText("MEDIO DE PAGO", 300, currentY + 5, 8, true, 0.10f, 0.15f, 0.20f);
        builder.drawText("MONEDA", 410, currentY + 5, 8, true, 0.10f, 0.15f, 0.20f);
        builder.drawText("MONTO", 490, currentY + 5, 8, true, 0.10f, 0.15f, 0.20f);

        // Calculations
        double totalUsd = 0.0;
        double totalVes = 0.0;
        double totalCop = 0.0;

        java.util.Map<String, Double> methodUsd = new java.util.HashMap<>();
        java.util.Map<String, Double> methodVes = new java.util.HashMap<>();
        java.util.Map<String, Double> methodCop = new java.util.HashMap<>();

        int rowCount = 0;
        if (payments != null && !payments.isEmpty()) {
            for (com.mediclinic.models.DailyPayment p : payments) {
                currentY -= 17;
                rowCount++;

                // Background alternate color
                if (rowCount % 2 == 0) {
                    builder.drawFilledRect(36, currentY, 540, 16, 0.97f, 0.98f, 0.99f);
                }
                builder.drawRect(36, currentY, 540, 16, 0.90f, 0.92f, 0.95f, 0.5f);

                String hora = p.getPaymentTime() != null ? p.getPaymentTime() : "--:--";
                String patientStr = sanitizeText(p.getPatientName() != null ? p.getPatientName() : "Paciente");
                String cedulaStr = p.getPatientIdCard() != null ? p.getPatientIdCard() : "";
                String medio = p.getPaymentMethod() != null ? p.getPaymentMethod() : "Efectivo";
                String moneda = p.getCurrency() != null ? p.getCurrency() : "USD";
                double monto = p.getAmount();

                builder.drawText(String.valueOf(rowCount), 44, currentY + 4, 7.5f, false, 0.20f, 0.25f, 0.33f);
                builder.drawText(hora, 64, currentY + 4, 7.5f, false, 0.20f, 0.25f, 0.33f);
                builder.drawText(patientStr + " (" + cedulaStr + ")", 115, currentY + 4, 7.5f, true, 0.06f, 0.09f, 0.16f);
                builder.drawText(medio, 300, currentY + 4, 7.5f, false, 0.20f, 0.25f, 0.33f);
                builder.drawText(moneda, 410, currentY + 4, 7.5f, true, 0.05f, 0.65f, 0.91f);

                String formattedMonto;
                if ("USD".equalsIgnoreCase(moneda)) {
                    formattedMonto = String.format("$%.2f", monto);
                    totalUsd += monto;
                    methodUsd.put(medio, methodUsd.getOrDefault(medio, 0.0) + monto);
                } else if ("VES".equalsIgnoreCase(moneda)) {
                    formattedMonto = String.format("Bs. %.2f", monto);
                    totalVes += monto;
                    methodVes.put(medio, methodVes.getOrDefault(medio, 0.0) + monto);
                } else {
                    formattedMonto = String.format("$%,.0f", monto);
                    totalCop += monto;
                    methodCop.put(medio, methodCop.getOrDefault(medio, 0.0) + monto);
                }

                builder.drawText(formattedMonto, 490, currentY + 4, 7.5f, true, 0.06f, 0.58f, 0.53f);

                if (currentY < 320) break; // Limit single page
            }
        } else {
            currentY -= 20;
            builder.drawText("No se registraron transacciones de pago en esta jornada.", 50, currentY + 5, 8.5f, false, 0.50f, 0.50f, 0.50f);
        }

        // 3. Financial Summary Box (Resumen Financiero Agrupado)
        currentY -= 35;
        builder.drawFilledRect(36, currentY - 100, 540, 115, 0.96f, 0.99f, 0.98f);
        builder.drawRect(36, currentY - 100, 540, 115, 0.06f, 0.58f, 0.53f, 1.2f);

        builder.drawText("RESUMEN FINANCIERO Y TOTALES DE RECAUDACION", 48, currentY + 3, 9.5f, true, 0.06f, 0.58f, 0.53f);

        // Subtotales por Moneda (Left)
        builder.drawText("TOTAL POR MONEDA:", 48, currentY - 15, 8.5f, true, 0.06f, 0.09f, 0.16f);
        builder.drawText("Total Dólares (USD): " + String.format("$%.2f USD", totalUsd), 55, currentY - 30, 8.5f, true, 0.09f, 0.58f, 0.53f);
        builder.drawText("Total Bolívares (VES): " + String.format("Bs. %.2f VES", totalVes), 55, currentY - 45, 8.5f, true, 0.05f, 0.65f, 0.91f);
        builder.drawText("Total Pesos (COP): " + String.format("$%,.0f COP", totalCop), 55, currentY - 60, 8.5f, true, 0.39f, 0.40f, 0.95f);

        // Subtotales por Medio de Pago (Right)
        builder.drawText("TOTALES POR MEDIO DE PAGO:", 310, currentY - 15, 8.5f, true, 0.06f, 0.09f, 0.16f);
        int my = currentY - 30;
        String[] methods = {"Efectivo", "Punto de Venta", "Pago Móvil", "Zelle", "Transferencia"};
        for (String m : methods) {
            double u = methodUsd.getOrDefault(m, 0.0);
            double v = methodVes.getOrDefault(m, 0.0);
            double c = methodCop.getOrDefault(m, 0.0);
            if (u > 0 || v > 0 || c > 0) {
                StringBuilder sb = new StringBuilder(m).append(": ");
                if (u > 0) sb.append(String.format("$%.2f USD ", u));
                if (v > 0) sb.append(String.format("Bs. %.2f VES ", v));
                if (c > 0) sb.append(String.format("$%,.0f COP", c));
                builder.drawText(sb.toString(), 315, my, 7.5f, false, 0.20f, 0.25f, 0.33f);
                my -= 13;
            }
        }
        if (my == currentY - 30) {
            builder.drawText("Sin movimientos por medios de pago.", 315, my, 7.5f, false, 0.50f, 0.50f, 0.50f);
        }

        // 4. Signatures
        currentY = currentY - 100 - 45;
        builder.drawLine(60, currentY, 230, currentY, 0.40f, 0.45f, 0.55f, 0.8f);
        builder.drawText("FIRMA DE RECEPCION / CAJA", 75, currentY - 12, 8, true, 0.30f, 0.35f, 0.45f);
        builder.drawText(sanitizeText(userName), 85, currentY - 22, 7.5f, false, 0.45f, 0.50f, 0.60f);

        builder.drawLine(380, currentY, 550, currentY, 0.40f, 0.45f, 0.55f, 0.8f);
        builder.drawText("CONFORMIDAD MEDICA / AUDITORIA", 380, currentY - 12, 8, true, 0.30f, 0.35f, 0.45f);
        builder.drawText("MediClinic Pro v2.0 - Sistema Seguro", 395, currentY - 22, 7.5f, false, 0.45f, 0.50f, 0.60f);

        // Footer
        builder.drawText("Documento emitido conforme a las normas de contabilidad clinica interna.",
                140, 25, 7.5f, false, 0.55f, 0.60f, 0.68f);

        return builder.build();
    }

    private static byte[] buildPdfContent(Consultation c, Patient p, User d) throws IOException {
        PdfBuilder builder = new PdfBuilder();

        // 1. Clinic Header Box (Teal accent background)
        builder.drawFilledRect(36, 695, 540, 80, 0.94f, 0.97f, 0.99f);
        builder.drawRect(36, 695, 540, 80, 0.05f, 0.65f, 0.91f, 1.5f);

        builder.drawText("CENTRO MEDICO MEDICLINIC", 48, 755, 12, true, 0.05f, 0.65f, 0.91f);
        builder.drawText("Atencion Medica Integral & Especializada", 48, 740, 8.5f, false, 0.28f, 0.33f, 0.41f);
        builder.drawText("Edif. Torre Medica, Consultorio 4-B | Tel: 0414-5551234", 48, 727, 8, false, 0.39f, 0.45f, 0.55f);
        builder.drawText("INFORME MEDICO & RECIPE OFICIAL (EDITABLE)", 48, 710, 9.5f, true, 0.09f, 0.58f, 0.53f);

        // 2. Doctor Badge
        String docName = (d != null && d.getFullName() != null) ? d.getFullName() : "Mario Roa";
        String docSpec = (d != null && d.getSpecialty() != null) ? d.getSpecialty() : "Medicina General y Cirugia";
        String docMpps = (d != null && d.getMppsLicense() != null) ? d.getMppsLicense() : "MPPS-84920 / Col. Medicos 14.502";

        builder.drawText("Dr. " + sanitizeText(docName), 365, 755, 10, true, 0.06f, 0.09f, 0.16f);
        builder.drawText(sanitizeText(docSpec), 365, 742, 8, false, 0.39f, 0.45f, 0.55f);
        builder.drawText("Matricula: " + sanitizeText(docMpps), 365, 730, 8, false, 0.39f, 0.45f, 0.55f);

        // 3. Patient Info Section
        builder.drawFilledRect(36, 600, 540, 85, 0.98f, 0.98f, 0.99f);
        builder.drawRect(36, 600, 540, 85, 0.88f, 0.91f, 0.94f, 1.0f);

        builder.drawText("DATOS DEL PACIENTE", 48, 672, 9.5f, true, 0.05f, 0.65f, 0.91f);

        builder.drawText("Nombre Completo: " + sanitizeText(p.getFullName()), 48, 655, 9, true, 0.06f, 0.09f, 0.16f);
        builder.drawText("Cedula de Identidad: " + sanitizeText(p.getIdCard()), 48, 641, 8.5f, false, 0.20f, 0.25f, 0.33f);
        builder.drawText("Historia Clinica Nro: " + sanitizeText(p.getMedicalRecordNumber()), 48, 627, 8.5f, false, 0.20f, 0.25f, 0.33f);
        builder.drawText("Telefono: " + (p.getPhone() != null ? sanitizeText(p.getPhone()) : "No registrado"), 48, 613, 8, false, 0.39f, 0.45f, 0.55f);

        String originStr = p.isForaneo() ? "FORANEO (" + sanitizeText(p.getOriginCity()) + ")" : "LOCAL";
        builder.drawText("Condicion de Origen: " + originStr, 340, 655, 8.5f, true, p.isForaneo() ? 0.39f : 0.06f, p.isForaneo() ? 0.40f : 0.72f, p.isForaneo() ? 0.95f : 0.51f);

        String consultDateStr = c.getDateTime() != null ? c.getDateTime().format(DATE_TIME_FORMATTER) : LocalDateTime.now().format(DATE_TIME_FORMATTER);
        builder.drawText("Fecha de Consulta: " + consultDateStr, 340, 641, 8.5f, false, 0.20f, 0.25f, 0.33f);

        if (p.getBirthDate() != null) {
            int age = java.time.Period.between(p.getBirthDate(), LocalDate.now()).getYears();
            builder.drawText("Edad: " + age + " anos (" + p.getBirthDate().format(DATE_FORMATTER) + ")", 340, 630, 9, false, 0.20f, 0.25f, 0.33f);
        }

        // 4. Datos Fisicos Section (Talla, Peso, IMC, Observacion)
        int currentY = 572;
        builder.drawFilledRect(36, currentY, 540, 26, 0.95f, 0.96f, 0.98f);
        builder.drawRect(36, currentY, 540, 26, 0.88f, 0.91f, 0.94f, 0.8f);

        String tallaStr = c.getHeightM() != null ? String.format("%.2f m", c.getHeightM()) : "No registrada";
        String pesoStr = c.getWeightKg() != null ? String.format("%.1f kg", c.getWeightKg()) : "No registrado";
        String imcStr = c.calculateBMI() != null ? String.format("%.1f kg/m2", c.calculateBMI()) : "N/A";
        String obsFisica = (c.getPhysicalNotes() != null && !c.getPhysicalNotes().trim().isEmpty()) ?
                sanitizeText(c.getPhysicalNotes().trim()) : "Sin observaciones fisicas adicionales";

        String physicalSummary = String.format("DATOS FISICOS -> Talla: %s  |  Peso: %s  |  IMC: %s",
                tallaStr, pesoStr, imcStr);
        builder.drawText(physicalSummary, 46, currentY + 14, 8, true, 0.05f, 0.65f, 0.91f);
        builder.drawText("Obs. Fisica: " + obsFisica, 46, currentY + 3, 8, false, 0.35f, 0.40f, 0.50f);

        // 5. Motivo de Consulta & Evolucion (Editable AcroForm)
        currentY -= 36;
        builder.drawText("MOTIVO DE CONSULTA & EVOLUCION CLINICA (CAMPO EDITABLE)", 36, currentY + 15, 9.5f, true, 0.05f, 0.65f, 0.91f);
        String reason = c.getReason() != null ? c.getReason() : "Consulta Medica General";
        builder.drawText("Motivo: " + sanitizeText(reason), 46, currentY, 8.5f, true, 0.15f, 0.20f, 0.28f);

        String evolution = (c.getClinicalNotes() != null && !c.getClinicalNotes().trim().isEmpty())
                ? c.getClinicalNotes().trim()
                : "Evolucion clinica favorable sin complicaciones registradas.";

        // AcroForm Field 1: EvolucionClinica (Multiline)
        builder.addFormField(new PdfFormField(
                "EvolucionClinica",
                "Evolucion y Anotaciones Medicas",
                evolution,
                36, currentY - 48, 540, 44,
                true, false, 8.5f,
                0.15f, 0.20f, 0.28f, // text color
                0.88f, 0.91f, 0.94f, // border color
                0.98f, 0.98f, 0.99f, // fill color
                1.0f
        ));

        // 6. Diagnostico Clinico (Editable AcroForm)
        currentY -= 65;
        builder.drawText("DIAGNOSTICO CLINICO - Dx (CAMPO EDITABLE)", 36, currentY + 15, 9.5f, true, 0.75f, 0.15f, 0.15f);
        String diag = (c.getDiagnosis() != null && !c.getDiagnosis().trim().isEmpty())
                ? c.getDiagnosis().trim()
                : "En evaluacion clinica";

        // AcroForm Field 2: DiagnosticoDx (Single line bold)
        builder.addFormField(new PdfFormField(
                "DiagnosticoDx",
                "Diagnostico Clinico Dx",
                diag,
                36, currentY - 12, 540, 24,
                false, true, 9.5f,
                0.75f, 0.15f, 0.15f, // text color
                0.98f, 0.80f, 0.80f, // border color
                0.99f, 0.95f, 0.95f, // fill color
                1.0f
        ));

        // 7. RECIPE MEDICO & TRATAMIENTO (Editable AcroForm)
        currentY -= 35;
        builder.drawText("PRESCRIPCION FARMACOLOGICA & TRATAMIENTO - RECIPE (CAMPO EDITABLE)", 36, currentY + 15, 9.5f, true, 0.06f, 0.58f, 0.53f);

        String rx = (c.getTreatmentRx() != null && !c.getTreatmentRx().trim().isEmpty())
                ? c.getTreatmentRx().trim()
                : "1. Tratamiento farmacologico segun indicaciones clinicas.";

        // AcroForm Field 3: RecipeTratamiento (Multiline)
        int rxBoxHeight = 135;
        builder.addFormField(new PdfFormField(
                "RecipeTratamiento",
                "Prescripcion Farmacologica y Recipe Medico",
                rx,
                36, currentY - rxBoxHeight, 540, rxBoxHeight + 8,
                true, false, 9.0f,
                0.06f, 0.09f, 0.16f, // text color
                0.06f, 0.58f, 0.53f, // border color
                0.96f, 0.99f, 0.98f, // fill color
                1.2f
        ));

        // 8. Proxima Cita Box
        currentY = currentY - rxBoxHeight - 20;
        if (c.getNextAppointmentDate() != null) {
            builder.drawFilledRect(36, currentY, 540, 24, 0.94f, 0.97f, 1.0f);
            builder.drawRect(36, currentY, 540, 24, 0.39f, 0.40f, 0.95f, 1.0f);
            builder.drawText("PROXIMA CITA DE CONTROL AGENDADA: " + c.getNextAppointmentDate().format(DATE_FORMATTER),
                    48, currentY + 7, 9, true, 0.39f, 0.40f, 0.95f);
            currentY -= 15;
        }

        // 9. Doctor Signature & Stamp Area
        currentY -= 45;
        builder.drawLine(340, currentY, 520, currentY, 0.20f, 0.25f, 0.33f, 1.0f);
        builder.drawText("Dr. " + sanitizeText(docName), 370, currentY - 12, 9, true, 0.06f, 0.09f, 0.16f);
        builder.drawText("Firma y Sello Medico Autorizado", 355, currentY - 24, 8, false, 0.39f, 0.45f, 0.55f);

        // 10. Document Footer
        builder.drawText("Documento generado electronicamente por MediClinic Pro v2.0 | Formato interactivo AcroForm con edicion directa habilitada.",
                36, 30, 7, false, 0.60f, 0.65f, 0.70f);

        return builder.build();
    }

    private static String sanitizeText(String text) {
        if (text == null) return "";
        return text.replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                .replace("Á", "A").replace("É", "E").replace("Í", "I").replace("Ó", "O").replace("Ú", "U")
                .replace("ñ", "n").replace("Ñ", "N").replace("°", " ").replace("—", "-");
    }

    private static String escapePdfString(String str) {
        if (str == null) return "";
        return sanitizeText(str)
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("\r\n", "\r")
                .replace("\n", "\r");
    }

    private static List<String> wrapText(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        String[] rawLines = text.split("\n");
        for (String raw : rawLines) {
            if (raw.length() <= maxChars) {
                lines.add(raw);
            } else {
                String[] words = raw.split(" ");
                StringBuilder current = new StringBuilder();
                for (String word : words) {
                    if (current.length() + word.length() + 1 > maxChars) {
                        lines.add(current.toString());
                        current = new StringBuilder(word);
                    } else {
                        if (current.length() > 0) current.append(" ");
                        current.append(word);
                    }
                }
                if (current.length() > 0) {
                    lines.add(current.toString());
                }
            }
        }
        return lines;
    }

    /**
     * Data structure representing an interactive PDF Form Field (AcroForm Widget).
     */
    public static class PdfFormField {
        public String name;
        public String title;
        public String value;
        public float x, y, width, height;
        public boolean multiline;
        public boolean bold;
        public float fontSize;
        public float textColorR, textColorG, textColorB;
        public float borderColorR, borderColorG, borderColorB;
        public float fillColorR, fillColorG, fillColorB;
        public float borderWidth;

        public PdfFormField(String name, String title, String value, float x, float y, float width, float height,
                            boolean multiline, boolean bold, float fontSize,
                            float textColorR, float textColorG, float textColorB,
                            float borderColorR, float borderColorG, float borderColorB,
                            float fillColorR, float fillColorG, float fillColorB,
                            float borderWidth) {
            this.name = name;
            this.title = title;
            this.value = value;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.multiline = multiline;
            this.bold = bold;
            this.fontSize = fontSize;
            this.textColorR = textColorR;
            this.textColorG = textColorG;
            this.textColorB = textColorB;
            this.borderColorR = borderColorR;
            this.borderColorG = borderColorG;
            this.borderColorB = borderColorB;
            this.fillColorR = fillColorR;
            this.fillColorG = fillColorG;
            this.fillColorB = fillColorB;
            this.borderWidth = borderWidth;
        }
    }

    /**
     * Standalone strictly-compliant PDF 1.4 builder supporting AcroForms.
     * Guaranteed sequential object numbering and xref table offset alignment.
     */
    private static class PdfBuilder {
        private final StringBuilder contentStream = new StringBuilder();
        private final List<PdfFormField> formFields = new ArrayList<>();

        public void addFormField(PdfFormField field) {
            this.formFields.add(field);
        }

        public void drawFilledRect(float x, float y, float w, float h, float r, float g, float b) {
            contentStream.append(String.format(java.util.Locale.US, "%.3f %.3f %.3f rg\n", r, g, b));
            contentStream.append(String.format(java.util.Locale.US, "%.2f %.2f %.2f %.2f re\nf\n", x, y, w, h));
        }

        public void drawRect(float x, float y, float w, float h, float r, float g, float b, float lineWidth) {
            contentStream.append(String.format(java.util.Locale.US, "%.2f w\n", lineWidth));
            contentStream.append(String.format(java.util.Locale.US, "%.3f %.3f %.3f RG\n", r, g, b));
            contentStream.append(String.format(java.util.Locale.US, "%.2f %.2f %.2f %.2f re\nS\n", x, y, w, h));
        }

        public void drawLine(float x1, float y1, float x2, float y2, float r, float g, float b, float lineWidth) {
            contentStream.append(String.format(java.util.Locale.US, "%.2f w\n", lineWidth));
            contentStream.append(String.format(java.util.Locale.US, "%.3f %.3f %.3f RG\n", r, g, b));
            contentStream.append(String.format(java.util.Locale.US, "%.2f %.2f m\n%.2f %.2f l\nS\n", x1, y1, x2, y2));
        }

        public void drawText(String text, float x, float y, float fontSize, boolean bold, float r, float g, float b) {
            String escaped = escapePdfString(text);
            String font = bold ? "/F2" : "/F1";
            contentStream.append("BT\n");
            contentStream.append(String.format(java.util.Locale.US, "%s %.2f Tf\n", font, fontSize));
            contentStream.append(String.format(java.util.Locale.US, "%.3f %.3f %.3f rg\n", r, g, b));
            contentStream.append(String.format(java.util.Locale.US, "%.2f %.2f Td\n", x, y));
            contentStream.append("(").append(escaped).append(") Tj\n");
            contentStream.append("ET\n");
        }

        public byte[] build() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int fieldCount = formFields.size();
            int totalObjects = fieldCount > 0 ? (7 + fieldCount) : 6;
            int[] offsets = new int[totalObjects + 1];

            baos.write("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n".getBytes(StandardCharsets.ISO_8859_1));

            // Object 1: Catalog
            offsets[1] = baos.size();
            if (fieldCount > 0) {
                baos.write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R /AcroForm 7 0 R >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));
            } else {
                baos.write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));
            }

            // Object 2: Pages
            offsets[2] = baos.size();
            baos.write("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));

            // Object 3: Page
            offsets[3] = baos.size();
            StringBuilder annotsRef = new StringBuilder();
            if (fieldCount > 0) {
                annotsRef.append(" /Annots [");
                for (int i = 0; i < fieldCount; i++) {
                    annotsRef.append(8 + i).append(" 0 R ");
                }
                annotsRef.append("]");
            }
            String pageObj = String.format(java.util.Locale.US,
                    "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 6 0 R /Resources << /Font << /F1 4 0 R /F2 5 0 R >> >>%s >>\nendobj\n",
                    annotsRef.toString());
            baos.write(pageObj.getBytes(StandardCharsets.US_ASCII));

            // Object 4: Font F1 (Helvetica Normal)
            offsets[4] = baos.size();
            baos.write("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));

            // Object 5: Font F2 (Helvetica Bold)
            offsets[5] = baos.size();
            baos.write("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));

            // Object 6: Contents Stream
            byte[] streamBytes = contentStream.toString().getBytes(StandardCharsets.ISO_8859_1);
            offsets[6] = baos.size();
            baos.write(String.format(java.util.Locale.US, "6 0 obj\n<< /Length %d >>\nstream\n", streamBytes.length).getBytes(StandardCharsets.US_ASCII));
            baos.write(streamBytes);
            baos.write("\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII));

            // If AcroForms are present:
            if (fieldCount > 0) {
                // Object 7: AcroForm Dictionary
                offsets[7] = baos.size();
                StringBuilder fieldsRef = new StringBuilder();
                for (int i = 0; i < fieldCount; i++) {
                    fieldsRef.append(8 + i).append(" 0 R ");
                }
                String acroFormObj = String.format(java.util.Locale.US,
                        "7 0 obj\n<< /Fields [%s] /DR << /Font << /F1 4 0 R /F2 5 0 R >> >> /DA (/F1 9 Tf 0 0 0 rg) /NeedAppearances true >>\nendobj\n",
                        fieldsRef.toString());
                baos.write(acroFormObj.getBytes(StandardCharsets.US_ASCII));

                // Objects 8.. (Field Annotations)
                for (int i = 0; i < fieldCount; i++) {
                    int objNum = 8 + i;
                    offsets[objNum] = baos.size();
                    PdfFormField f = formFields.get(i);
                    String fontCode = f.bold ? "/F2" : "/F1";
                    String da = String.format(java.util.Locale.US, "(%s %.2f Tf %.3f %.3f %.3f rg)",
                            fontCode, f.fontSize, f.textColorR, f.textColorG, f.textColorB);

                    String escapedVal = escapePdfString(f.value);
                    String flags = f.multiline ? " /Ff 4096" : "";

                    String fieldObj = String.format(java.util.Locale.US,
                            "%d 0 obj\n<< /Type /Annot /Subtype /Widget /FT /Tx /T (%s) /TU (%s) /V (%s) /DV (%s) /Rect [%.2f %.2f %.2f %.2f] /F 4%s /DA %s /BS << /W %.2f /S /S >> /MK << /BC [%.3f %.3f %.3f] /BG [%.3f %.3f %.3f] >> >>\nendobj\n",
                            objNum,
                            f.name,
                            escapePdfString(f.title),
                            escapedVal,
                            escapedVal,
                            f.x, f.y, f.x + f.width, f.y + f.height,
                            flags,
                            da,
                            f.borderWidth,
                            f.borderColorR, f.borderColorG, f.borderColorB,
                            f.fillColorR, f.fillColorG, f.fillColorB);

                    baos.write(fieldObj.getBytes(StandardCharsets.US_ASCII));
                }
            }

            // Cross-Reference Table
            int xrefStart = baos.size();
            baos.write(String.format(java.util.Locale.US, "xref\n0 %d\n0000000000 65535 f \n", totalObjects + 1).getBytes(StandardCharsets.US_ASCII));
            for (int i = 1; i <= totalObjects; i++) {
                baos.write(String.format(java.util.Locale.US, "%010d 00000 n \n", offsets[i]).getBytes(StandardCharsets.US_ASCII));
            }

            // Trailer
            baos.write(String.format(java.util.Locale.US, "trailer\n<< /Size %d /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF\n", totalObjects + 1, xrefStart).getBytes(StandardCharsets.US_ASCII));

            return baos.toByteArray();
        }
    }
}
