package com.mediclinic.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DailyPayment {
    private int id;
    private int patientId;
    private Integer waitingRoomId;
    private LocalDate paymentDate;
    private String paymentTime;
    private String paymentMethod; // 'Efectivo', 'Punto de Venta', 'Pago Móvil', 'Zelle', 'Transferencia'
    private String currency;      // 'USD', 'VES', 'COP'
    private double amount;
    private String notes;
    private LocalDateTime createdAt;

    // Aux fields for display
    private String patientName;
    private String patientIdCard;
    private String patientMedicalRecordNumber;

    public DailyPayment() {}

    public DailyPayment(int id, int patientId, Integer waitingRoomId, LocalDate paymentDate,
                        String paymentTime, String paymentMethod, String currency,
                        double amount, String notes, LocalDateTime createdAt) {
        this.id = id;
        this.patientId = patientId;
        this.waitingRoomId = waitingRoomId;
        this.paymentDate = paymentDate != null ? paymentDate : LocalDate.now();
        this.paymentTime = paymentTime;
        this.paymentMethod = paymentMethod;
        this.currency = currency;
        this.amount = amount;
        this.notes = notes;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public DailyPayment(int patientId, Integer waitingRoomId, String paymentMethod,
                        String currency, double amount, String notes) {
        this(-1, patientId, waitingRoomId, LocalDate.now(),
             java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")),
             paymentMethod, currency, amount, notes, LocalDateTime.now());
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public Integer getWaitingRoomId() { return waitingRoomId; }
    public void setWaitingRoomId(Integer waitingRoomId) { this.waitingRoomId = waitingRoomId; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getPaymentTime() { return paymentTime; }
    public void setPaymentTime(String paymentTime) { this.paymentTime = paymentTime; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientIdCard() { return patientIdCard; }
    public void setPatientIdCard(String patientIdCard) { this.patientIdCard = patientIdCard; }

    public String getPatientMedicalRecordNumber() { return patientMedicalRecordNumber; }
    public void setPatientMedicalRecordNumber(String patientMedicalRecordNumber) { this.patientMedicalRecordNumber = patientMedicalRecordNumber; }

    public String getFormattedAmount() {
        if ("USD".equalsIgnoreCase(currency)) {
            return String.format("$%.2f USD", amount);
        } else if ("VES".equalsIgnoreCase(currency)) {
            return String.format("Bs. %.2f VES", amount);
        } else {
            return String.format("$%,.0f COP", amount);
        }
    }
}
