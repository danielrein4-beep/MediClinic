package com.mediclinic.controllers;

import com.mediclinic.dao.PatientDAO;
import com.mediclinic.models.Patient;
import com.mediclinic.models.User;
import com.mediclinic.services.SessionManager;
import com.mediclinic.services.ThemeManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class PatientsViewController {

    @FXML
    private TextField txtSearch;

    @FXML
    private TableView<Patient> tablePatients;

    @FXML
    private TableColumn<Patient, String> colHistoryNumber;

    @FXML
    private TableColumn<Patient, String> colIdCard;

    @FXML
    private TableColumn<Patient, String> colFullName;

    @FXML
    private TableColumn<Patient, String> colOrigin;

    @FXML
    private TableColumn<Patient, String> colPhone;

    @FXML
    private TableColumn<Patient, String> colBirthDate;

    @FXML
    private TableColumn<Patient, Void> colActions;

    @FXML
    private Label lblTotalCount;

    @FXML
    private VBox patientDetailsCard;

    @FXML
    private Label lblDetailName;

    @FXML
    private Label lblDetailHistory;

    @FXML
    private Label lblDetailIdCard;

    @FXML
    private Label lblDetailOrigin;

    @FXML
    private Label lblDetailPhone;

    @FXML
    private Label lblDetailEmail;

    @FXML
    private Label lblDetailAge;

    @FXML
    private Label lblDetailAddress;

    @FXML
    private Button btnStartClinical;

    @FXML
    private Button btnQuoteProcedure;

    @FXML
    private Button btnDeletePatient;

    @FXML
    private StackPane subviewContainer;

    private final PatientDAO patientDAO = new PatientDAO();
    private final ObservableList<Patient> patientList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Control de Acceso por Roles (RBAC): Restricciones para Secretaria
        User currentUser = SessionManager.getInstance().getCurrentUser();
        boolean isSecretary = currentUser != null && currentUser.isSecretary();

        if (isSecretary && btnStartClinical != null) {
            btnStartClinical.setVisible(false);
            btnStartClinical.setManaged(false);
        }

        // Configurar columnas de la tabla
        colHistoryNumber.setCellValueFactory(new PropertyValueFactory<>("medicalRecordNumber"));
        colIdCard.setCellValueFactory(new PropertyValueFactory<>("idCard"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colPhone.setCellValueFactory(cellData -> {
            String ph = cellData.getValue().getPhone();
            return new SimpleStringProperty(ph != null && !ph.isEmpty() ? ph : "Sin teléfono");
        });

        // Columna de Origen con formato y estilo visual
        colOrigin.setCellValueFactory(cellData -> {
            Patient p = cellData.getValue();
            return new SimpleStringProperty(p.getFormattedOrigin());
        });

        // Columna de Fecha de Nacimiento / Edad
        colBirthDate.setCellValueFactory(cellData -> {
            Patient p = cellData.getValue();
            if (p.getBirthDate() != null) {
                int age = Period.between(p.getBirthDate(), LocalDate.now()).getYears();
                return new SimpleStringProperty(p.getBirthDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " (" + age + " años)");
            }
            return new SimpleStringProperty("No registrada");
        });

        // Columna de Acciones Rápidas (Editar / Eliminar)
        setupActionsColumn();

        // Listener de selección en la tabla
        tablePatients.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showPatientDetails(newSelection);
            }
        });

        // Búsqueda en tiempo real al escribir
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filterPatients(newVal);
        });

        // Cargar pacientes desde la base de datos
        loadPatients();
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("✏️");
            private final Button btnDel = new Button("🗑️");
            private final HBox container = new HBox(4, btnEdit, btnDel);

            {
                container.setAlignment(Pos.CENTER);
                btnEdit.getStyleClass().add("btn-secondary");
                btnEdit.setStyle("-fx-font-size: 10px; -fx-padding: 3px 6px; -fx-cursor: hand;");
                btnEdit.setTooltip(new Tooltip("Editar datos del paciente"));
                btnEdit.setOnAction(event -> {
                    Patient patient = getTableView().getItems().get(getIndex());
                    openEditPatientForm(patient);
                });

                btnDel.getStyleClass().add("btn-danger");
                btnDel.setStyle("-fx-font-size: 10px; -fx-padding: 3px 6px; -fx-cursor: hand;");
                btnDel.setTooltip(new Tooltip("Eliminar paciente"));
                btnDel.setOnAction(event -> {
                    Patient patient = getTableView().getItems().get(getIndex());
                    promptDeletePatient(patient);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
    }

    public void loadPatients() {
        patientList.clear();
        List<Patient> all = patientDAO.findAll();
        patientList.addAll(all);
        tablePatients.setItems(patientList);
        lblTotalCount.setText("Total en archivo: " + all.size() + " pacientes");

        if (!all.isEmpty()) {
            tablePatients.getSelectionModel().selectFirst();
        } else {
            patientDetailsCard.setVisible(false);
        }
    }

    private void filterPatients(String query) {
        if (query == null || query.trim().isEmpty()) {
            tablePatients.setItems(patientList);
            lblTotalCount.setText("Total en archivo: " + patientList.size() + " pacientes");
        } else {
            List<Patient> results = patientDAO.search(query);
            tablePatients.setItems(FXCollections.observableArrayList(results));
            lblTotalCount.setText("Resultados encontrados: " + results.size());
        }
    }

    private void showPatientDetails(Patient p) {
        if (p == null) return;
        patientDetailsCard.setVisible(true);
        lblDetailName.setText(p.getFullName());
        lblDetailHistory.setText("Historia N° " + p.getMedicalRecordNumber());
        lblDetailIdCard.setText("C.I: " + p.getIdCard());

        if (p.isForaneo()) {
            lblDetailOrigin.setText("✈️ Foráneo (" + (p.getOriginCity() != null ? p.getOriginCity() : "Otra ciudad") + ")");
            lblDetailOrigin.setStyle("-fx-background-color: #ede9fe; -fx-text-fill: #6366f1; -fx-font-weight: bold; -fx-padding: 4px 10px; -fx-background-radius: 12px;");
        } else {
            lblDetailOrigin.setText("🏠 Local");
            lblDetailOrigin.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-weight: bold; -fx-padding: 4px 10px; -fx-background-radius: 12px;");
        }

        lblDetailPhone.setText(p.getPhone() != null && !p.getPhone().isEmpty() ? p.getPhone() : "No registrado");
        lblDetailEmail.setText(p.getEmail() != null && !p.getEmail().isEmpty() ? p.getEmail() : "No registrado");

        if (p.getBirthDate() != null) {
            int age = Period.between(p.getBirthDate(), LocalDate.now()).getYears();
            lblDetailAge.setText(p.getBirthDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " (" + age + " años)");
        } else {
            lblDetailAge.setText("No registrada");
        }

        lblDetailAddress.setText(p.getAddress() != null && !p.getAddress().isEmpty() ? p.getAddress() : "Sin dirección física registrada");
    }

    @FXML
    public void openNewPatientForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/PatientRegistration.fxml"));
            Parent root = loader.load();
            PatientRegistrationController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Registrar Nuevo Paciente");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setResizable(true);
            stage.setMinWidth(760);
            stage.setMinHeight(560);

            Scene scene = new Scene(root, 840, 640);
            ThemeManager.getInstance().applyTheme(scene);
            stage.setScene(scene);
            stage.centerOnScreen();

            // Callback cuando se guarde el paciente
            controller.setOnPatientSavedListener(savedPatient -> {
                loadPatients();
                tablePatients.getSelectionModel().select(savedPatient);
                stage.close();
            });

            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openEditPatientForm(Patient patient) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/PatientRegistration.fxml"));
            Parent root = loader.load();
            PatientRegistrationController controller = loader.getController();
            controller.setEditingPatient(patient);

            Stage stage = new Stage();
            stage.setTitle("Editar Paciente - " + patient.getFullName());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setResizable(true);
            stage.setMinWidth(760);
            stage.setMinHeight(560);

            Scene scene = new Scene(root, 840, 640);
            ThemeManager.getInstance().applyTheme(scene);
            stage.setScene(scene);
            stage.centerOnScreen();

            controller.setOnPatientSavedListener(savedPatient -> {
                loadPatients();
                tablePatients.getSelectionModel().select(savedPatient);
                stage.close();
            });

            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleDeletePatient() {
        Patient selected = tablePatients.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Selecciona un paciente de la lista para eliminar.");
            alert.setHeaderText("Ningún paciente seleccionado");
            alert.showAndWait();
            return;
        }
        promptDeletePatient(selected);
    }

    public void promptDeletePatient(Patient patient) {
        if (patient == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminación de Paciente");
        alert.setHeaderText("Eliminar a: " + patient.getFullName() + " (C.I: " + patient.getIdCard() + ")");
        alert.setContentText("¿Está seguro de que desea eliminar a este paciente? Esta acción borrará todo su historial médico y cotizaciones, y no se puede deshacer.");

        ButtonType btnYes = new ButtonType("Sí, Eliminar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnYes, btnNo);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == btnYes) {
            boolean ok = patientDAO.delete(patient.getId());
            if (ok) {
                loadPatients();
                Alert success = new Alert(Alert.AlertType.INFORMATION, "El paciente y todos sus registros asociados han sido eliminados de forma segura.");
                success.setHeaderText("Paciente Eliminado");
                success.showAndWait();
            } else {
                Alert error = new Alert(Alert.AlertType.ERROR, "No se pudo eliminar el paciente de la base de datos.");
                error.setHeaderText("Error al eliminar");
                error.showAndWait();
            }
        }
    }

    @FXML
    public void handleOpenClinicalHistory() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.isSecretary()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Acceso restringido: Las secretarias no tienen autorización para acceder a historias clínicas ni diagnósticos médicos.");
            alert.setHeaderText("Permiso Denegado");
            alert.showAndWait();
            return;
        }

        Patient selected = tablePatients.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ClinicalHistory.fxml"));
            Parent root = loader.load();
            ClinicalHistoryController controller = loader.getController();
            controller.selectPatient(selected);

            Stage stage = new Stage();
            stage.setTitle("Historia Clínica y Consultas - " + selected.getFullName());
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.setResizable(true);
            stage.setMinWidth(980);
            stage.setMinHeight(680);

            Scene scene = new Scene(root, 1140, 760);
            ThemeManager.getInstance().applyTheme(scene);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleOpenProcedureQuote() {
        Patient selected = tablePatients.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ProceduresView.fxml"));
            Parent root = loader.load();
            ProceduresViewController controller = loader.getController();
            controller.selectPatient(selected);

            Stage stage = new Stage();
            stage.setTitle("Cotizador de Procedimientos - " + selected.getFullName());
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.setResizable(true);
            stage.setMinWidth(980);
            stage.setMinHeight(680);

            Scene scene = new Scene(root, 1180, 760);
            ThemeManager.getInstance().applyTheme(scene);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleClearSearch() {
        txtSearch.clear();
        loadPatients();
    }
}
