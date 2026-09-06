package com.mediclinic.services;

import com.mediclinic.models.User;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return this.currentUser;
    }

    public boolean isLoggedIn() {
        return this.currentUser != null;
    }

    public void logout() {
        this.currentUser = null;
    }

    /**
     * Retorna el saludo dinámico personalizado para el usuario actual.
     * Ejemplo: "¡Bienvenido Dr. Mario Roa!" o "¡Bienvenida Secretaria Niccolle Medina!"
     */
    public String getDynamicGreeting() {
        if (currentUser == null) {
            return "¡Bienvenido a MediClinic Pro!";
        }

        String fullName = currentUser.getFullName() != null ? currentUser.getFullName().trim() : currentUser.getUsername();
        String role = currentUser.getRole() != null ? currentUser.getRole().toUpperCase() : "";

        if ("SECRETARIA".equals(role)) {
            return "¡Bienvenida Secretaria " + fullName + "!";
        } else if ("DOCTOR".equals(role)) {
            String title = (currentUser.getTitle() != null && !currentUser.getTitle().trim().isEmpty())
                    ? currentUser.getTitle().trim()
                    : "Dr.";
            return "¡Bienvenido " + title + " " + fullName + "!";
        }

        return "¡Bienvenido, " + fullName + "!";
    }
}

