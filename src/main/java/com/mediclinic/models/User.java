package com.mediclinic.models;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String fullName;
    private String role; // "DOCTOR" or "SECRETARIA"
    private String title; // "Dr.", "Dra.", "Lic."
    private String specialty;
    private String mppsLicense; // Número de registro médico o matrícula

    public User() {}

    public User(int id, String username, String passwordHash, String fullName, String role, String title, String specialty, String mppsLicense) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.title = title;
        this.specialty = specialty;
        this.mppsLicense = mppsLicense;
    }

    public User(String username, String passwordHash, String fullName, String role, String title, String specialty, String mppsLicense) {
        this(-1, username, passwordHash, fullName, role, title, specialty, mppsLicense);
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getMppsLicense() { return mppsLicense; }
    public void setMppsLicense(String mppsLicense) { this.mppsLicense = mppsLicense; }

    /**
     * Retorna el saludo formal para la cabecera.
     * Ejemplo: "Dr. Mario Silva" o "Dra. Mary González"
     */
    public String getFormalGreetingName() {
        if (title != null && !title.trim().isEmpty()) {
            return title.trim() + " " + fullName;
        }
        return fullName;
    }

    public boolean isDoctor() {
        return "DOCTOR".equalsIgnoreCase(role);
    }

    public boolean isSecretary() {
        return "SECRETARIA".equalsIgnoreCase(role);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role='" + role + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
