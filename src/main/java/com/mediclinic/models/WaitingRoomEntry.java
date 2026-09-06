package com.mediclinic.models;

import java.time.LocalDateTime;

public class WaitingRoomEntry {
    private int id;
    private int patientId;
    private LocalDateTime arrivalTime;
    private String reason;
    private String status; // "EN_ESPERA", "EN_CONSULTA", "ATENDIDO", "CANCELADO"

    // Aux fields
    private String patientName;
    private String patientIdCard;
    private String patientPhone;
    private String patientOrigin;
    private String patientMedicalRecordNumber;
    private int dailyTurnNumber;

    public WaitingRoomEntry() {}

    public WaitingRoomEntry(int id, int patientId, LocalDateTime arrivalTime, String reason, String status) {
        this.id = id;
        this.patientId = patientId;
        this.arrivalTime = arrivalTime;
        this.reason = reason;
        this.status = status;
    }

    public WaitingRoomEntry(int patientId, String reason) {
        this(-1, patientId, LocalDateTime.now(), reason, "EN_ESPERA");
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientIdCard() { return patientIdCard; }
    public void setPatientIdCard(String patientIdCard) { this.patientIdCard = patientIdCard; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public String getPatientOrigin() { return patientOrigin; }
    public void setPatientOrigin(String patientOrigin) { this.patientOrigin = patientOrigin; }

    public String getPatientMedicalRecordNumber() { return patientMedicalRecordNumber; }
    public void setPatientMedicalRecordNumber(String patientMedicalRecordNumber) { this.patientMedicalRecordNumber = patientMedicalRecordNumber; }

    public int getDailyTurnNumber() { return dailyTurnNumber; }
    public void setDailyTurnNumber(int dailyTurnNumber) { this.dailyTurnNumber = dailyTurnNumber; }

    // Payment fields
    private boolean paid = false;
    private String paymentDetail;
    private String paymentMethod;
    private String paymentCurrency;
    private Double paymentAmount;

    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }

    public String getPaymentDetail() { return paymentDetail; }
    public void setPaymentDetail(String paymentDetail) { this.paymentDetail = paymentDetail; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentCurrency() { return paymentCurrency; }
    public void setPaymentCurrency(String paymentCurrency) { this.paymentCurrency = paymentCurrency; }

    public Double getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(Double paymentAmount) { this.paymentAmount = paymentAmount; }
}
