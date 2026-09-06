package com.mediclinic.controllers;

import com.mediclinic.dao.PatientDAO;
import com.mediclinic.models.Patient;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.function.Consumer;

public class PatientRegistrationController {

    @FXML
    private Label lblFormTitle;

    @FXML
    private Label lblHistoryNumber;

    @FXML
    private ComboBox<String> cbCedulaPrefix;

    @FXML
    private TextField txtCedula;

    @FXML
    private TextField txtFirstName;

    @FXML
    private TextField txtLastName;

    @FXML
    private RadioButton rbLocal;

    @FXML
    private RadioButton rbForaneo;

    @FXML
    private ToggleGroup originGroup;

    @FXML
    private VBox containerForaneoCity;

    @FXML
    private TextField txtOriginCity;

    @FXML
    private DatePicker dpBirthDate;

    @FXML
    private TextField txtPhone;

    @FXML
    private TextField txtEmail;

    @FXML
    private TextArea txtAddress;

    @FXML
    private Label lblFeedback;

    @FXML
    private HBox feedbackContainer;

    @FXML
    private Button btnSave;

    @FXML
    private Button btnClear;

    private final PatientDAO patientDAO = new PatientDAO();
    private Consumer<Patient> onPatientSavedListener;
    private Patient editingPatient = null;

    @FXML
    public void initialize() {
        // Inicializar prefijos de identificación flexibles (Venezuela, Colombia, Pasaportes y Sin Prefijo)
        if (cbCedulaPrefix != null) {
            cbCedulaPrefix.getItems().addAll("V-", "E-", "J-", "P-", "CC-", "TI-", "Sin prefijo");
            cbCedulaPrefix.setValue("V-");
            cbCedulaPrefix.setEditable(true);
        }

        // Toggle grupo de origen
        if (originGroup != null) {
            originGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                boolean isForaneo = rbForaneo.isSelected();
                if (containerForaneoCity != null) {
                    containerForaneoCity.setVisible(isForaneo);
                    containerForaneoCity.setManaged(isForaneo);
                }
            });
        }

        // Generar y previsualizar el próximo número de historia clínica
        refreshNextHistoryNumber();

        // Ocultar feedback inicialmente
        hideFeedback();
    }

    public void setOnPatientSavedListener(Consumer<Patient> listener) {
        this.onPatientSavedListener = listener;
    }

    private void refreshNextHistoryNumber() {
        if (editingPatient == null) {
            String nextHC = patientDAO.generateNextMedicalRecordNumber();
            lblHistoryNumber.setText("Expediente sugerido: " + nextHC);
        }
    }

    public void setEditingPatient(Patient patient) {
        this.editingPatient = patient;
        if (patient != null) {
            lblFormTitle.setText("Editar Expediente de Paciente");
            lblHistoryNumber.setText("Expediente: " + patient.getMedicalRecordNumber());

            // Separar prefijo y número de identificación si existe guión
            String rawId = patient.getIdCard() != null ? patient.getIdCard().trim() : "";
            if (rawId.contains("-")) {
                String[] parts = rawId.split("-", 2);
                String detectedPrefix = parts[0].toUpperCase() + "-";
                if (!cbCedulaPrefix.getItems().contains(detectedPrefix)) {
                    cbCedulaPrefix.getItems().add(detectedPrefix);
                }
                cbCedulaPrefix.setValue(detectedPrefix);
                txtCedula.setText(parts[1]);
            } else {
                if (!cbCedulaPrefix.getItems().contains("Sin prefijo")) {
                    cbCedulaPrefix.getItems().add("Sin prefijo");
                }
                cbCedulaPrefix.setValue("Sin prefijo");
                txtCedula.setText(rawId);
            }

            txtFirstName.setText(patient.getFirstName());
            txtLastName.setText(patient.getLastName());

            if (patient.isForaneo()) {
                rbForaneo.setSelected(true);
                txtOriginCity.setText(patient.getOriginCity());
            } else {
                rbLocal.setSelected(true);
            }

            dpBirthDate.setValue(patient.getBirthDate());
            txtPhone.setText(patient.getPhone());
            txtEmail.setText(patient.getEmail());
            txtAddress.setText(patient.getAddress());

            btnSave.setText("💾 Actualizar Paciente");
        }
    }

    @FXML
    public void handleSave() {
        hideFeedback();

        // 1. Obtener y estructurar documento de identidad con flexibilidad total
        String prefix = cbCedulaPrefix.getValue() != null ? cbCedulaPrefix.getValue().trim() : "V-";
        if (prefix.equalsIgnoreCase("Sin prefijo") || prefix.equalsIgnoreCase("Otro") || prefix.equalsIgnoreCase("N/A") || prefix.equals("-")) {
            prefix = "";
        }

        String rawCedulaNum = txtCedula.getText().trim();
        if (rawCedulaNum.isEmpty()) {
            showError("Debe ingresar el número de cédula o documento de identidad del paciente.");
            txtCedula.requestFocus();
            return;
        }

        String fullCedula;
        if (rawCedulaNum.contains("-")) {
            fullCedula = rawCedulaNum;
        } else if (!prefix.isEmpty()) {
            fullCedula = prefix.endsWith("-") ? prefix + rawCedulaNum : prefix + "-" + rawCedulaNum;
        } else {
            fullCedula = rawCedulaNum;
        }

        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String originType = rbForaneo.isSelected() ? "FORANEO" : "LOCAL";
        String originCity = rbForaneo.isSelected() ? txtOriginCity.getText().trim() : "Local";

        LocalDate birthDate = dpBirthDate.getValue();
        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim();
        String address = txtAddress.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty()) {
            showError("Los nombres y apellidos del paciente son obligatorios.");
            if (firstName.isEmpty()) txtFirstName.requestFocus();
            else txtLastName.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            showError("El número de teléfono es obligatorio.");
            txtPhone.requestFocus();
            return;
        }

        // 2. Validación de Cédula Única (No permitir duplicados)
        Patient existingWithCedula = patientDAO.findByIdCard(fullCedula);
        if (existingWithCedula != null) {
            // Si no estamos editando o si pertenece a otro paciente
            if (editingPatient == null || existingWithCedula.getId() != editingPatient.getId()) {
                showError("⚠️ Ya existe un paciente registrado con la cédula " + fullCedula + 
                        " (" + existingWithCedula.getFullName() + " - Expediente: " + existingWithCedula.getMedicalRecordNumber() + ").");
                txtCedula.requestFocus();
                return;
            }
        }

        // 3. Crear o actualizar objeto Patient
        if (editingPatient == null) {
            String historyNum = patientDAO.generateNextMedicalRecordNumber();
            Patient newPatient = new Patient(
                    historyNum,
                    fullCedula,
                    firstName,
                    lastName,
                    birthDate,
                    phone,
                    email,
                    originType,
                    originCity,
                    address
            );

            boolean success = patientDAO.insert(newPatient);
            if (success) {
                showSuccess("✅ ¡Paciente registrado con éxito! Expediente: " + newPatient.getMedicalRecordNumber());
                if (onPatientSavedListener != null) {
                    onPatientSavedListener.accept(newPatient);
                }
                clearForm();
                refreshNextHistoryNumber();
            } else {
                showError("Error al guardar el paciente en la base de datos.");
            }
        } else {
            editingPatient.setIdCard(fullCedula);
            editingPatient.setFirstName(firstName);
            editingPatient.setLastName(lastName);
            editingPatient.setOriginType(originType);
            editingPatient.setOriginCity(originCity);
            editingPatient.setBirthDate(birthDate);
            editingPatient.setPhone(phone);
            editingPatient.setEmail(email);
            editingPatient.setAddress(address);

            boolean success = patientDAO.update(editingPatient);
            if (success) {
                showSuccess("✅ ¡Expediente actualizado con éxito!");
                if (onPatientSavedListener != null) {
                    onPatientSavedListener.accept(editingPatient);
                }
            } else {
                showError("Error al actualizar los datos del paciente.");
            }
        }
    }

    @FXML
    public void handleClear() {
        clearForm();
        hideFeedback();
    }

    private void clearForm() {
        editingPatient = null;
        lblFormTitle.setText("Registrar Nuevo Paciente");
        btnSave.setText("💾 Guardar Paciente");
        txtCedula.clear();
        txtFirstName.clear();
        txtLastName.clear();
        rbLocal.setSelected(true);
        txtOriginCity.clear();
        dpBirthDate.setValue(null);
        txtPhone.clear();
        txtEmail.clear();
        txtAddress.clear();
        refreshNextHistoryNumber();
    }

    private void showError(String msg) {
        lblFeedback.setText(msg);
        feedbackContainer.setStyle("-fx-background-color: #fee2e2; -fx-border-color: #fca5a5; -fx-background-radius: 8px; -fx-padding: 10px;");
        lblFeedback.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 12px;");
        feedbackContainer.setVisible(true);
        feedbackContainer.setManaged(true);
    }

    private void showSuccess(String msg) {
        lblFeedback.setText(msg);
        feedbackContainer.setStyle("-fx-background-color: #d1fae5; -fx-border-color: #6ee7b7; -fx-background-radius: 8px; -fx-padding: 10px;");
        lblFeedback.setStyle("-fx-text-fill: #065f46; -fx-font-weight: bold; -fx-font-size: 12px;");
        feedbackContainer.setVisible(true);
        feedbackContainer.setManaged(true);
    }

    private void hideFeedback() {
        feedbackContainer.setVisible(false);
        feedbackContainer.setManaged(false);
    }
}
