package com.mediclinic.controllers;

import com.mediclinic.dao.UserDAO;
import com.mediclinic.models.User;
import com.mediclinic.services.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Label lblError;

    @FXML
    private Button btnLogin;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        // Limpiar errores al escribir
        txtUsername.textProperty().addListener((obs, oldV, newV) -> hideError());
        txtPassword.textProperty().addListener((obs, oldV, newV) -> hideError());
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = txtUsername.getText() != null ? txtUsername.getText().trim() : "";
        String password = txtPassword.getText() != null ? txtPassword.getText().trim() : "";

        if (username.isEmpty() || password.isEmpty()) {
            showError("Por favor, ingresa tu usuario y contraseña.");
            return;
        }

        User user = userDAO.authenticate(username, password);

        if (user != null) {
            // Guardar usuario autenticado en el SessionManager Singleton
            SessionManager.getInstance().setCurrentUser(user);

            // Redirigir según el rol
            String fxmlTarget = user.isDoctor() ? "/views/DoctorDashboard.fxml" : "/views/SecretaryDashboard.fxml";
            String title = user.isDoctor() ? "MediClinic Pro - Portal del Doctor" : "MediClinic Pro - Recepción y Asistencia";

            navigateTo(event, fxmlTarget, title);
        } else {
            showError("Usuario o contraseña incorrectos. Intenta nuevamente.");
        }
    }

    @FXML
    public void fillDoctorDemo() {
        txtUsername.setText("doctor");
        txtPassword.setText("1234");
        hideError();
    }

    @FXML
    public void fillSecretaryDemo() {
        txtUsername.setText("secretaria");
        txtPassword.setText("1234");
        hideError();
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
    }

    private void hideError() {
        lblError.setVisible(false);
    }

    private void navigateTo(ActionEvent event, String fxmlPath, String windowTitle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1180, 760);

            // Cargar estilos CSS con tema guardado
            com.mediclinic.services.ThemeManager.getInstance().applyTheme(scene);

            stage.setTitle(windowTitle);
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(1080);
            stage.setMinHeight(720);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            showError("Error al cargar la interfaz: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
