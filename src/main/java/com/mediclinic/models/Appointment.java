package com.mediclinic.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Appointment {
    private int id;
    private Integer patientId;
    private String idCard;
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate appointmentDate;
    private String appointmentTime;
    private String reason;
    private String status; // "PENDIENTE", "CONFIRMADA", "ATENDIDA", "CANCELADA"
    private String notes;
    private LocalDateTime createdAt;

    public Appointment() {}

    public Appointment(int id, Integer patientId, String idCard, String firstName, String lastName,
                       String phone, LocalDate appointmentDate, String appointmentTime,
                       String reason, String status, String notes, LocalDateTime createdAt) {
        this.id = id;
        this.patientId = patientId;
        this.idCard = idCard;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.reason = reason;
        this.status = status != null ? status : "PENDIENTE";
        this.notes = notes;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Appointment(Integer patientId, String idCard, String firstName, String lastName,
                       String phone, LocalDate appointmentDate, String appointmentTime,
                       String reason, String notes) {
        this(-1, patientId, idCard, firstName, lastName, phone, appointmentDate, appointmentTime, reason, "PENDIENTE", notes, LocalDateTime.now());
    }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getPatientId() { return patientId; }
    public void setPatientId(Integer patientId) { this.patientId = patientId; }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(String appointmentTime) { this.appointmentTime = appointmentTime; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
