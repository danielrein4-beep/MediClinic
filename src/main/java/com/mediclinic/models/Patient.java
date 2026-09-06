package com.mediclinic.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Patient {
    private int id;
    private String medicalRecordNumber; // ej. HC-2026-0001
    private String idCard;              // Cédula de identidad (V-12345678)
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String phone;
    private String email;
    private String originType;          // "LOCAL" o "FORANEO"
    private String originCity;          // Ciudad o estado de procedencia
    private String address;
    private LocalDateTime createdAt;

    public Patient() {}

    public Patient(int id, String medicalRecordNumber, String idCard, String firstName, String lastName,
                   LocalDate birthDate, String phone, String email, String originType,
                   String originCity, String address, LocalDateTime createdAt) {
        this.id = id;
        this.medicalRecordNumber = medicalRecordNumber;
        this.idCard = idCard;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.phone = phone;
        this.email = email;
        this.originType = originType;
        this.originCity = originCity;
        this.address = address;
        this.createdAt = createdAt;
    }

    public Patient(String medicalRecordNumber, String idCard, String firstName, String lastName,
                   LocalDate birthDate, String phone, String email, String originType,
                   String originCity, String address) {
        this(-1, medicalRecordNumber, idCard, firstName, lastName, birthDate, phone, email, originType, originCity, address, LocalDateTime.now());
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMedicalRecordNumber() { return medicalRecordNumber; }
    public void setMedicalRecordNumber(String medicalRecordNumber) { this.medicalRecordNumber = medicalRecordNumber; }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() {
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOriginType() { return originType; }
    public void setOriginType(String originType) { this.originType = originType; }

    public String getOriginCity() { return originCity; }
    public void setOriginCity(String originCity) { this.originCity = originCity; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isLocal() {
        return "LOCAL".equalsIgnoreCase(originType);
    }

    public boolean isForaneo() {
        return "FORANEO".equalsIgnoreCase(originType);
    }

    public String getFormattedOrigin() {
        if (isForaneo()) {
            return "Foráneo (" + (originCity != null && !originCity.isEmpty() ? originCity : "Otra ciudad") + ")";
        }
        return "Local" + (originCity != null && !originCity.isEmpty() ? " - " + originCity : "");
    }

    @Override
    public String toString() {
        return getFullName() + " (C.I: " + idCard + " | HC: " + medicalRecordNumber + ")";
    }
}
