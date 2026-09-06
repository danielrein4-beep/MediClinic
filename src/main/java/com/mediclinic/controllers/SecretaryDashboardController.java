package com.mediclinic.controllers;

import com.mediclinic.dao.ConfigDAO;
import com.mediclinic.dao.PatientDAO;
import com.mediclinic.dao.ProcedureDAO;
import com.mediclinic.dao.WaitingRoomDAO;
import com.mediclinic.models.Patient;
import com.mediclinic.models.User;
import com.mediclinic.services.SessionManager;
import com.mediclinic.views.CalendarWidget;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;

public class SecretaryDashboardController {

    @FXML
    private Label lblGreeting;

    @FXML
    private Label lblClinicName;

    @FXML
    private Label lblTasaVes;

    @FXML
    private Label lblTasaCop;

    @FXML
    private Label lblStatWaiting;

    @FXML
    private Label lblStatTotalPatients;

    @FXML
    private Label lblStatQuotes;

    @FXML
    private Button btnNavDashboard;

    @FXML
    private Button btnNavPatients;

    @FXML
    private Button btnNavQuotes;

    @FXML
    private Button btnNavWaitingRoom;

    @FXML
    private Button btnNavAgenda;

    @FXML
    private Button btnNavSettings;

    @FXML
    private StackPane contentArea;

    @FXML
    private ScrollPane overviewScrollPane;

    @FXML
    private VBox calendarSidebarContainer;

    @FXML
    private TextField txtQuickSearch;

    @FXML
    private VBox quickSearchResultCard;

    @FXML
    private HBox quickSearchNotFoundCard;

    @FXML
    private Label lblResultFullName;

    @FXML
    private Label lblResultOriginBadge;

    @FXML
    private Label lblResultIdCard;

    @FXML
    private Label lblResultHistory;

    @FXML
    private Label lblResultPhone;

    private static SecretaryDashboardController instance;

    public static SecretaryDashboardController getInstance() {
        return instance;
    }

    private final PatientDAO patientDAO = new PatientDAO();
    private final ProcedureDAO procedureDAO = new ProcedureDAO();
    private final WaitingRoomDAO waitingRoomDAO = new WaitingRoomDAO();
    private final ConfigDAO configDAO = new ConfigDAO();
    private Timeline autoRefreshTimeline;

    @FXML
    public void initialize() {
        instance = this;

        // Cargar datos de cabecera y clínica
        loadHeader();

        // Cargar tasas cambiarias
        loadExchangeRates();

        // Cargar estadísticas
        loadMetrics();

        // Iniciar temporizador reactivo en segundo plano (cada 3 segundos)
        startAutoRefresh();

        // Incrustar componente de Calendario Interactivo
        if (calendarSidebarContainer != null) {
            CalendarWidget calendarWidget = new CalendarWidget();
            calendarSidebarContainer.getChildren().clear();
            calendarSidebarContainer.getChildren().add(calendarWidget);
        }
    }

    public void loadHeader() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            if (lblGreeting != null) {
                lblGreeting.setText(SessionManager.getInstance().getDynamicGreeting());
            }
        } else {
            if (lblGreeting != null) {
                lblGreeting.setText("¡Bienvenida Secretaria Niccolle Medina!");
            }
        }

        String clinic = configDAO.getValue("CLINICA_NOMBRE", "Centro Médico Especializado MediClinic");
        if (lblClinicName != null) {
            lblClinicName.setText(clinic);
        }
    }

    public void loadExchangeRates() {
        double ves = configDAO.getDoubleValue("TASA_USD_VES", 38.50);
        double cop = configDAO.getDoubleValue("TASA_USD_COP", 4100.0);
        if (lblTasaVes != null) {
            lblTasaVes.setText("VES: Bs. " + String.format("%.2f", ves));
        }
        if (lblTasaCop != null) {
            lblTasaCop.setText("COP: $" + String.format("%,.0f", cop));
        }
    }

    public void loadMetrics() {
        int waiting = waitingRoomDAO.countWaiting();
        int totalPatients = patientDAO.countAll();
        int totalQuotes = procedureDAO.findAll().size();

        if (lblStatWaiting != null) lblStatWaiting.setText(String.valueOf(waiting));
        if (lblStatTotalPatients != null) lblStatTotalPatients.setText(String.valueOf(totalPatients));
        if (lblStatQuotes != null) lblStatQuotes.setText(String.valueOf(totalQuotes));
    }

    public void refreshMetrics() {
        Platform.runLater(this::loadMetrics);
    }

    private void startAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            loadMetrics();
        }));
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }

    @FXML
    public void handleOpenRatesModal() {
        ExchangeRateDialog.showAndUpdateRates(configDAO, this::loadExchangeRates);
    }

    @FXML
    public void handleQuickSearch() {
        String query = txtQuickSearch.getText().trim();
        if (query.isEmpty()) {
            quickSearchResultCard.setVisible(false);
            quickSearchResultCard.setManaged(false);
            quickSearchNotFoundCard.setVisible(false);
            quickSearchNotFoundCard.setManaged(false);
            return;
        }

        // 1. Buscar coincidencia exacta por cédula
        Patient patient = patientDAO.findByIdCard(query);

        // 2. Si no coincide exacto, intentar con prefijo V-
        if (patient == null && !query.toUpperCase().startsWith("V-") && !query.toUpperCase().startsWith("E-")) {
            patient = patientDAO.findByIdCard("V-" + query);
        }

        // 3. Si no, buscar por término general
        if (patient == null) {
            List<Patient> list = patientDAO.search(query);
            if (!list.isEmpty()) {
                patient = list.get(0);
            }
        }

        if (patient != null) {
            lblResultFullName.setText(patient.getFullName());
            lblResultIdCard.setText("C.I: " + patient.getIdCard());
            lblResultHistory.setText("Expediente: " + patient.getMedicalRecordNumber());
            lblResultPhone.setText("Tel: " + (patient.getPhone() != null && !patient.getPhone().isEmpty() ? patient.getPhone() : "No registrado"));

            if (patient.isForaneo()) {
                lblResultOriginBadge.setText("✈️ Foráneo (" + (patient.getOriginCity() != null ? patient.getOriginCity() : "Otra ciudad") + ")");
                lblResultOriginBadge.setStyle("-fx-background-color: #ede9fe; -fx-text-fill: #6366f1; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 10px; -fx-font-size: 11px;");
            } else {
                lblResultOriginBadge.setText("🏠 Local");
                lblResultOriginBadge.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 10px; -fx-font-size: 11px;");
            }

            quickSearchResultCard.setVisible(true);
            quickSearchResultCard.setManaged(true);
            quickSearchNotFoundCard.setVisible(false);
            quickSearchNotFoundCard.setManaged(false);
        } else {
            quickSearchResultCard.setVisible(false);
            quickSearchResultCard.setManaged(false);
            quickSearchNotFoundCard.setVisible(true);
            quickSearchNotFoundCard.setManaged(true);
        }
    }

    @FXML
    public void showDashboardTab() {
        setActiveNav(btnNavDashboard);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(overviewScrollPane);
        loadHeader();
        loadMetrics();
        loadExchangeRates();
    }

    @FXML
    public void showPatientsTab() {
        setActiveNav(btnNavPatients);
        loadSubView("/views/PatientsView.fxml");
    }

    @FXML
    public void showQuotesTab() {
        setActiveNav(btnNavQuotes);
        loadSubView("/views/ProceduresView.fxml");
    }

    public void openQuotesWithPatient(Patient patient) {
        setActiveNav(btnNavQuotes);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ProceduresView.fxml"));
            Parent view = loader.load();
            ProceduresViewController controller = loader.getController();
            if (patient != null) {
                controller.selectPatient(patient);
            }
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            System.err.println("Error al cargar ProceduresView en Secretaria: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void showWaitingRoomTab() {
        setActiveNav(btnNavWaitingRoom);
        loadSubView("/views/WaitingRoomView.fxml");
    }

    @FXML
    public void showAgendaTab() {
        if (btnNavAgenda != null) {
            setActiveNav(btnNavAgenda);
        }
        loadSubView("/views/AgendaView.fxml");
    }

    @FXML
    public void showSettingsTab() {
        if (btnNavSettings != null) {
            setActiveNav(btnNavSettings);
        }
        loadSubView("/views/SettingsView.fxml");
    }

    private void loadSubView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            System.err.println("Error al cargar la vista " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
            Label errLabel = new Label("⚠️ No se pudo cargar la vista: " + fxmlPath + "\nDetalle: " + e.getMessage());
            errLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 14px; -fx-padding: 20px;");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(errLabel);
        }
    }

    private void setActiveNav(Button activeButton) {
        btnNavDashboard.getStyleClass().remove("active");
        btnNavPatients.getStyleClass().remove("active");
        btnNavQuotes.getStyleClass().remove("active");
        btnNavWaitingRoom.getStyleClass().remove("active");
        if (btnNavAgenda != null) {
            btnNavAgenda.getStyleClass().remove("active");
        }
        if (btnNavSettings != null) {
            btnNavSettings.getStyleClass().remove("active");
        }

        if (activeButton != null && !activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        SessionManager.getInstance().logout();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1024, 680);
            com.mediclinic.services.ThemeManager.getInstance().applyTheme(scene);

            stage.setTitle("MediClinic Pro - Iniciar Sesión");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(960);
            stage.setMinHeight(640);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
