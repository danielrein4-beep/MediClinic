package com.mediclinic.controllers;

import com.mediclinic.dao.ConfigDAO;
import com.mediclinic.dao.UserDAO;
import com.mediclinic.models.User;
import com.mediclinic.services.SessionManager;
import com.mediclinic.services.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.Locale;

public class SettingsViewController {

    // User Profile
    @FXML
    private Label lblUserFullName;

    @FXML
    private Label lblUserRole;

    @FXML
    private TextField txtUserName;

    @FXML
    private TextField txtSpecialty;

    @FXML
    private TextField txtLicense;

    @FXML
    private PasswordField txtNewPassword;

    @FXML
    private PasswordField txtConfirmPassword;

    // Clinic Settings
    @FXML
    private TextField txtClinicName;

    @FXML
    private TextField txtClinicAddress;

    @FXML
    private TextField txtClinicPhone;

    // Exchange Rates
    @FXML
    private TextField txtTasaVes;

    @FXML
    private TextField txtTasaCop;

    // Theme Toggle
    @FXML
    private Button btnToggleTheme;

    @FXML
    private Label lblCurrentThemeStatus;

    @FXML
    private Label lblFeedback;

    private final ConfigDAO configDAO = new ConfigDAO();
    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        loadData();
        updateThemeButtonUI();
    }

    public void loadData() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            currentUser = userDAO.findFirstDoctor();
            if (currentUser != null) {
                SessionManager.getInstance().setCurrentUser(currentUser);
            }
        }

        if (currentUser != null) {
            lblUserFullName.setText(currentUser.getFullName());
            lblUserRole.setText("Rol: " + currentUser.getRole());
            txtUserName.setText(currentUser.getFullName());
            txtSpecialty.setText(currentUser.getSpecialty() != null ? currentUser.getSpecialty() : "");
            txtLicense.setText(currentUser.getMppsLicense() != null ? currentUser.getMppsLicense() : "");
        } else {
            lblUserFullName.setText("Dr. Mario Roa");
            lblUserRole.setText("Rol: DOCTOR");
            txtUserName.setText("Mario Roa");
            txtSpecialty.setText("Medicina General y Cirugía");
            txtLicense.setText("MPPS-84920 / Col. Médicos 14.502");
        }

        txtClinicName.setText(configDAO.getValue("CLINICA_NOMBRE", "Centro Médico Especializado MediClinic"));
        txtClinicAddress.setText(configDAO.getValue("CLINICA_DIRECCION", "Av. Principal Los Pirineos, San Cristóbal, Táchira"));
        txtClinicPhone.setText(configDAO.getValue("CLINICA_TELEFONO", "+58 276 355-1234"));

        double ves = configDAO.getDoubleValue("TASA_USD_VES", 38.50);
        double cop = configDAO.getDoubleValue("TASA_USD_COP", 4100.0);

        txtTasaVes.setText(String.format(Locale.US, "%.2f", ves));
        txtTasaCop.setText(String.format(Locale.US, "%.0f", cop));

        updateThemeButtonUI();
    }

    private void updateThemeButtonUI() {
        if (btnToggleTheme == null) return;
        boolean dark = ThemeManager.getInstance().isDarkMode();
        if (dark) {
            btnToggleTheme.setText("☀️ Cambiar a Modo Claro");
            btnToggleTheme.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: #000000; -fx-font-weight: bold; -fx-padding: 8px 16px; -fx-background-radius: 8px; -fx-cursor: hand;");
            if (lblCurrentThemeStatus != null) {
                lblCurrentThemeStatus.setText("Modo Oscuro (Dark Mode) Activo 🌙");
                lblCurrentThemeStatus.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
            }
        } else {
            btnToggleTheme.setText("🌙 Cambiar a Modo Oscuro");
            btnToggleTheme.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-padding: 8px 16px; -fx-background-radius: 8px; -fx-cursor: hand;");
            if (lblCurrentThemeStatus != null) {
                lblCurrentThemeStatus.setText("Modo Claro (Light Mode) Activo ☀️");
                lblCurrentThemeStatus.setStyle("-fx-text-fill: #0f172a; -fx-font-weight: bold;");
            }
        }
    }

    @FXML
    public void handleToggleTheme() {
        ThemeManager.getInstance().toggleTheme(btnToggleTheme.getScene());
        updateThemeButtonUI();
        lblFeedback.setText("🎨 Tema visual actualizado: " + (ThemeManager.getInstance().isDarkMode() ? "Modo Oscuro" : "Modo Claro"));
        lblFeedback.setStyle("-fx-text-fill: #10b981;");
    }

    @FXML
    public void handleSaveClinicSettings() {
        lblFeedback.setText("");
        String name = txtClinicName.getText().trim();
        String address = txtClinicAddress.getText().trim();
        String phone = txtClinicPhone.getText().trim();

        if (name.isEmpty()) {
            lblFeedback.setText("⚠️ El nombre de la clínica no puede estar vacío.");
            lblFeedback.setStyle("-fx-text-fill: #ef4444;");
            return;
        }

        configDAO.setValue("CLINICA_NOMBRE", name, "Nombre oficial de la clínica");
        configDAO.setValue("CLINICA_DIRECCION", address, "Dirección física de la clínica");
        configDAO.setValue("CLINICA_TELEFONO", phone, "Teléfono de contacto");

        lblFeedback.setText("✅ Datos de la clínica guardados en base de datos.");
        lblFeedback.setStyle("-fx-text-fill: #10b981;");

        if (SecretaryDashboardController.getInstance() != null) {
            SecretaryDashboardController.getInstance().loadHeader();
        }

        loadData();
    }

    @FXML
    public void handleSaveRates() {
        lblFeedback.setText("");
        try {
            double ves = Double.parseDouble(txtTasaVes.getText().replace(",", ".").trim());
            double cop = Double.parseDouble(txtTasaCop.getText().replace(",", ".").trim());

            if (ves <= 0 || cop <= 0) {
                lblFeedback.setText("⚠️ Las tasas deben ser mayores a 0.");
                lblFeedback.setStyle("-fx-text-fill: #ef4444;");
                return;
            }

            configDAO.setValue("TASA_USD_VES", String.valueOf(ves), "Tasa de cambio USD a Bolívares (VES)");
            configDAO.setValue("TASA_USD_COP", String.valueOf(cop), "Tasa de cambio USD a Pesos Colombianos (COP)");

            lblFeedback.setText("✅ Tasas de cambio guardadas correctamente (VES: " + ves + " | COP: " + cop + ").");
            lblFeedback.setStyle("-fx-text-fill: #10b981;");

            if (DoctorDashboardController.getInstance() != null) {
                DoctorDashboardController.getInstance().loadExchangeRates();
            }
            if (SecretaryDashboardController.getInstance() != null) {
                SecretaryDashboardController.getInstance().loadExchangeRates();
            }

            loadData();
        } catch (NumberFormatException e) {
            lblFeedback.setText("⚠️ Ingrese valores numéricos válidos para las tasas.");
            lblFeedback.setStyle("-fx-text-fill: #ef4444;");
        }
    }

    @FXML
    public void handleSaveProfile() {
        lblFeedback.setText("");
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            currentUser = userDAO.findFirstDoctor();
            if (currentUser != null) {
                SessionManager.getInstance().setCurrentUser(currentUser);
            }
        }

        if (currentUser == null) {
            lblFeedback.setText("⚠️ No se pudo determinar el usuario a actualizar.");
            lblFeedback.setStyle("-fx-text-fill: #ef4444;");
            return;
        }

        String newName = txtUserName.getText().trim();
        String newSpecialty = txtSpecialty.getText().trim();
        String newLicense = txtLicense.getText().trim();

        if (newName.isEmpty()) {
            lblFeedback.setText("⚠️ El nombre del usuario no puede estar vacío.");
            lblFeedback.setStyle("-fx-text-fill: #ef4444;");
            return;
        }

        currentUser.setFullName(newName);
        currentUser.setSpecialty(newSpecialty);
        currentUser.setMppsLicense(newLicense);

        String p1 = txtNewPassword.getText();
        String p2 = txtConfirmPassword.getText();

        if (!p1.isEmpty()) {
            if (!p1.equals(p2)) {
                lblFeedback.setText("⚠️ Las contraseñas no coinciden.");
                lblFeedback.setStyle("-fx-text-fill: #ef4444;");
                return;
            }
            currentUser.setPasswordHash(p1);
        }

        boolean ok = userDAO.update(currentUser);
        if (ok) {
            SessionManager.getInstance().setCurrentUser(currentUser);
            lblFeedback.setText("✅ Perfil de usuario actualizado y persistido en SQLite.");
            lblFeedback.setStyle("-fx-text-fill: #10b981;");
            lblUserFullName.setText(newName);
            txtNewPassword.clear();
            txtConfirmPassword.clear();

            if (DoctorDashboardController.getInstance() != null) {
                DoctorDashboardController.getInstance().loadDoctorHeader();
            }
            if (SecretaryDashboardController.getInstance() != null) {
                SecretaryDashboardController.getInstance().loadHeader();
            }

            loadData();
        } else {
            lblFeedback.setText("❌ Error al actualizar el perfil en la base de datos.");
            lblFeedback.setStyle("-fx-text-fill: #ef4444;");
        }
    }
}
