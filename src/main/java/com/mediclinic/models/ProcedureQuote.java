package com.mediclinic.models;

import java.time.LocalDateTime;

public class ProcedureQuote {
    private int id;
    private int patientId;
    private String procedureType;        // ej. "Cirugía Menor", "Endoscopia", "Biopsia"
    private String description;
    private double amountUsd;            // Monto base en USD ($)
    private double rateVes;              // Tasa de cambio USD -> Bolívares
    private double amountVes;            // Monto calculado en Bolívares (Bs.)
    private double rateCop;              // Tasa de cambio USD -> Pesos Colombianos
    private double amountCop;            // Monto calculado en COP ($)
    private String status;               // "COTIZADA", "PLANIFICADA", "REALIZADA", "CANCELADA"
    private LocalDateTime plannedDate;   // Fecha y hora planificada
    private String notes;
    private LocalDateTime createdAt;
    
    // Datos auxiliares de presentación
    private String patientName;
    private String patientIdCard;
    private String patientOrigin;

    public ProcedureQuote() {}

    public ProcedureQuote(int id, int patientId, String procedureType, String description,
                          double amountUsd, double rateVes, double amountVes,
                          double rateCop, double amountCop, String status,
                          LocalDateTime plannedDate, String notes, LocalDateTime createdAt) {
        this.id = id;
        this.patientId = patientId;
        this.procedureType = procedureType;
        this.description = description;
        this.amountUsd = amountUsd;
        this.rateVes = rateVes;
        this.amountVes = amountVes;
        this.rateCop = rateCop;
        this.amountCop = amountCop;
        this.status = status;
        this.plannedDate = plannedDate;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public ProcedureQuote(int patientId, String procedureType, String description,
                          double amountUsd, double rateVes, double rateCop,
                          String status, LocalDateTime plannedDate, String notes) {
        this(-1, patientId, procedureType, description, amountUsd, rateVes,
                amountUsd * rateVes, rateCop, amountUsd * rateCop,
                status, plannedDate, notes, LocalDateTime.now());
    }

    // Recalcula conversiones
    public void recalculateAmounts() {
        this.amountVes = this.amountUsd * this.rateVes;
        this.amountCop = this.amountUsd * this.rateCop;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public String getProcedureType() { return procedureType; }
    public void setProcedureType(String procedureType) { this.procedureType = procedureType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getAmountUsd() { return amountUsd; }
    public void setAmountUsd(double amountUsd) { 
        this.amountUsd = amountUsd;
        recalculateAmounts();
    }

    public double getRateVes() { return rateVes; }
    public void setRateVes(double rateVes) { 
        this.rateVes = rateVes;
        recalculateAmounts();
    }

    public double getAmountVes() { return amountVes; }
    public void setAmountVes(double amountVes) { this.amountVes = amountVes; }

    public double getRateCop() { return rateCop; }
    public void setRateCop(double rateCop) { 
        this.rateCop = rateCop;
        recalculateAmounts();
    }

    public double getAmountCop() { return amountCop; }
    public void setAmountCop(double amountCop) { this.amountCop = amountCop; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getPlannedDate() { return plannedDate; }
    public void setPlannedDate(LocalDateTime plannedDate) { this.plannedDate = plannedDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientIdCard() { return patientIdCard; }
    public void setPatientIdCard(String patientIdCard) { this.patientIdCard = patientIdCard; }

    public String getPatientOrigin() { return patientOrigin; }
    public void setPatientOrigin(String patientOrigin) { this.patientOrigin = patientOrigin; }
}
