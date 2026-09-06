package com.mediclinic.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Consultation {
    private int id;
    private int patientId;
    private int doctorId;
    private LocalDateTime dateTime;
    private String reason;                // Motivo de consulta
    private String clinicalNotes;         // Anotaciones y evolución médica
    private String diagnosis;             // Diagnóstico principal
    private String treatmentRx;           // Tratamiento y prescripción farmacológica (récipe)
    
    // Datos físicos
    private Double weightKg;              // Peso en kg
    private Double heightM;               // Talla/Altura en metros
    private String physicalNotes;         // Observación de examen físico
    
    // Signos vitales legacy (compatibilidad)
    private String bloodPressure;         // Presión Arterial
    private Integer heartRate;            // Frecuencia Cardíaca
    private Double temperature;           // Temperatura en °C
    
    private LocalDate nextAppointmentDate;// Fecha de próxima cita
    private String issuedDocumentPath;    // Ruta del informe médico PDF generado
    
    // Propiedades adicionales para visualización en tablas / vistas
    private String patientName;
    private String patientIdCard;
    private String doctorName;

    public Consultation() {}

    public Consultation(int id, int patientId, int doctorId, LocalDateTime dateTime, String reason,
                        String clinicalNotes, String diagnosis, String treatmentRx,
                        Double weightKg, Double heightM, String physicalNotes,
                        LocalDate nextAppointmentDate, String issuedDocumentPath) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.dateTime = dateTime;
        this.reason = reason;
        this.clinicalNotes = clinicalNotes;
        this.diagnosis = diagnosis;
        this.treatmentRx = treatmentRx;
        this.weightKg = weightKg;
        this.heightM = heightM;
        this.physicalNotes = physicalNotes;
        this.nextAppointmentDate = nextAppointmentDate;
        this.issuedDocumentPath = issuedDocumentPath;
    }

    public Consultation(int id, int patientId, int doctorId, LocalDateTime dateTime, String reason,
                        String clinicalNotes, String diagnosis, String treatmentRx,
                        String bloodPressure, Integer heartRate, Double weightKg, Double heightM,
                        Double temperature, LocalDate nextAppointmentDate, String issuedDocumentPath) {
        this(id, patientId, doctorId, dateTime, reason, clinicalNotes, diagnosis, treatmentRx, weightKg, heightM, null, nextAppointmentDate, issuedDocumentPath);
        this.bloodPressure = bloodPressure;
        this.heartRate = heartRate;
        this.temperature = temperature;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }

    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getClinicalNotes() { return clinicalNotes; }
    public void setClinicalNotes(String clinicalNotes) { this.clinicalNotes = clinicalNotes; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getTreatmentRx() { return treatmentRx; }
    public void setTreatmentRx(String treatmentRx) { this.treatmentRx = treatmentRx; }

    public String getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }

    public Integer getHeartRate() { return heartRate; }
    public void setHeartRate(Integer heartRate) { this.heartRate = heartRate; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public Double getHeightM() { return heightM; }
    public void setHeightM(Double heightM) { this.heightM = heightM; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public String getPhysicalNotes() { return physicalNotes; }
    public void setPhysicalNotes(String physicalNotes) { this.physicalNotes = physicalNotes; }

    public LocalDate getNextAppointmentDate() { return nextAppointmentDate; }
    public void setNextAppointmentDate(LocalDate nextAppointmentDate) { this.nextAppointmentDate = nextAppointmentDate; }

    public String getIssuedDocumentPath() { return issuedDocumentPath; }
    public void setIssuedDocumentPath(String issuedDocumentPath) { this.issuedDocumentPath = issuedDocumentPath; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientIdCard() { return patientIdCard; }
    public void setPatientIdCard(String patientIdCard) { this.patientIdCard = patientIdCard; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    /**
     * Calcula el Índice de Masa Corporal (IMC) automáticamente
     */
    public Double calculateBMI() {
        if (weightKg != null && heightM != null && heightM > 0) {
            double bmi = weightKg / (heightM * heightM);
            return Math.round(bmi * 10.0) / 10.0;
        }
        return null;
    }

    public boolean isToday() {
        return dateTime != null && dateTime.toLocalDate().equals(LocalDate.now());
    }
}
