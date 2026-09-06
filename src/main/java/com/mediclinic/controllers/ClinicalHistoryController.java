package com.mediclinic.controllers;

import com.mediclinic.dao.ConsultationDAO;
import com.mediclinic.dao.PatientDAO;
import com.mediclinic.dao.UserDAO;
import com.mediclinic.models.Consultation;
import com.mediclinic.models.Patient;
import com.mediclinic.models.User;
import com.mediclinic.services.PdfReportService;
import com.mediclinic.services.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public class ClinicalHistoryController {

    // Búsqueda y Selección de Paciente
    @FXML
    private TextField txtSearchPatient;

    @FXML
    private ComboBox<Patient> cbSelectPatient;

    // Header Info Paciente
    @FXML
    private Label lblPatientName;

    @FXML
    private Label lblPatientIdCard;

    @FXML
    private Label lblPatientHistoryNum;

    @FXML
    private Label lblPatientOriginBadge;

    @FXML
    private Label lblPatientAge;

    @FXML
    private Label lblPatientPhone;

    // Tabla de Historial Cronológico
    @FXML
    private TableView<Consultation> tableConsultations;

    @FXML
    private TableColumn<Consultation, String> colDate;

    @FXML
    private TableColumn<Consultation, String> colReason;

    @FXML
    private TableColumn<Consultation, String> colDiagnosis;

    @FXML
    private TableColumn<Consultation, String> colNextApp;

    @FXML
    private TableColumn<Consultation, Void> colActions;

    @FXML
    private Label lblHistoryCount;

    // Detalle de Consulta Seleccionada
    @FXML
    private VBox selectedConsultationCard;

    @FXML
    private Label lblDetailConsultDate;

    @FXML
    private Label lblDetailPhysical;

    @FXML
    private Label lblDetailReason;

    @FXML
    private Label lblDetailEvolution;

    @FXML
    private Label lblDetailDiagnosis;

    @FXML
    private Label lblDetailRx;

    @FXML
    private Label lblDetailNextApp;

    @FXML
    private Button btnDownloadPdf;

    @FXML
    private Button btnWhatsApp;

    @FXML
    private Button btnEmail;

    // Formulario de Nueva Consulta
    @FXML
    private TextField txtReason;

    // Datos Físicos (Reemplazo de Signos Vitales)
    @FXML
    private TextField txtHeight;

    @FXML
    private TextField txtWeight;

    @FXML
    private TextField txtPhysicalNotes;

    @FXML
    private Label lblBmiPreview;

    @FXML
    private TextArea txtEvolution;

    @FXML
    private TextField txtDiagnosis;

    @FXML
    private TextArea txtTreatmentRx;

    @FXML
    private DatePicker dpNextAppointment;

    @FXML
    private HBox feedbackContainer;

    @FXML
    private Label lblFeedback;

    private final PatientDAO patientDAO = new PatientDAO();
    private final ConsultationDAO consultationDAO = new ConsultationDAO();
    private final UserDAO userDAO = new UserDAO();

    private final ObservableList<Consultation> consultationList = FXCollections.observableArrayList();
    private Patient currentPatient;
    private Consultation currentSelectedConsultation;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        // Configurar Columnas de la Tabla de Consultas
        colDate.setCellValueFactory(cellData -> {
            LocalDateTime dt = cellData.getValue().getDateTime();
            return new SimpleStringProperty(dt != null ? dt.format(DATE_TIME_FORMATTER) : "N/A");
        });

        colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
        colDiagnosis.setCellValueFactory(new PropertyValueFactory<>("diagnosis"));

        colNextApp.setCellValueFactory(cellData -> {
            LocalDate next = cellData.getValue().getNextAppointmentDate();
            return new SimpleStringProperty(next != null ? next.format(DATE_FORMATTER) : "Sin agendar");
        });

        setupActionsColumn();

        // Listener de selección en tabla
        tableConsultations.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showConsultationDetails(newVal);
            }
        });

        // Cálculo dinámico de IMC al tipear peso o talla
        txtWeight.textProperty().addListener((obs, o, n) -> calculateBmiPreview());
        txtHeight.textProperty().addListener((obs, o, n) -> calculateBmiPreview());

        // Cargar lista de pacientes en el selector
        loadPatientsSelector();

        // Ocultar feedback
        hideFeedback();
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnPdf = new Button("📄");
            private final Button btnWa = new Button("💬");
            private final Button btnMail = new Button("✉️");
            private final HBox container = new HBox(6, btnPdf, btnWa, btnMail);

            {
                container.setAlignment(Pos.CENTER);
                btnPdf.setStyle("-fx-background-color: linear-gradient(to bottom right, #0284c7, #0369a1); -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4px 8px; -fx-background-radius: 6px; -fx-cursor: hand;");
                btnPdf.setTooltip(new Tooltip("Ver / Abrir Informe PDF"));
                btnPdf.setOnAction(event -> {
                    Consultation c = getTableView().getItems().get(getIndex());
                    generateAndOpenPdf(c);
                });

                btnWa.setStyle("-fx-background-color: linear-gradient(to bottom right, #25D366, #128C7E); -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4px 8px; -fx-background-radius: 6px; -fx-cursor: hand;");
                btnWa.setTooltip(new Tooltip("Enviar por WhatsApp"));
                btnWa.setOnAction(event -> {
                    Consultation c = getTableView().getItems().get(getIndex());
                    sendConsultationViaWhatsApp(c);
                });

                btnMail.setStyle("-fx-background-color: linear-gradient(to bottom right, #ea4335, #c5221f); -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4px 8px; -fx-background-radius: 6px; -fx-cursor: hand;");
                btnMail.setTooltip(new Tooltip("Enviar por Gmail"));
                btnMail.setOnAction(event -> {
                    Consultation c = getTableView().getItems().get(getIndex());
                    sendConsultationViaEmail(c);
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

    private void loadPatientsSelector() {
        List<Patient> patients = patientDAO.findAll();
        cbSelectPatient.setItems(FXCollections.observableArrayList(patients));

        cbSelectPatient.getSelectionModel().selectedItemProperty().addListener((obs, oldP, newP) -> {
            if (newP != null) {
                selectPatient(newP);
            }
        });

        if (!patients.isEmpty() && currentPatient == null) {
            cbSelectPatient.getSelectionModel().selectFirst();
        }
    }

    public void selectPatient(Patient patient) {
        if (patient == null) return;
        this.currentPatient = patient;

        // Actualizar datos del header
        lblPatientName.setText(patient.getFullName());
        lblPatientIdCard.setText("C.I: " + patient.getIdCard());
        lblPatientHistoryNum.setText("Expediente: " + patient.getMedicalRecordNumber());

        if (patient.isForaneo()) {
            lblPatientOriginBadge.setText("✈️ Foráneo (" + (patient.getOriginCity() != null ? patient.getOriginCity() : "Otra ciudad") + ")");
            lblPatientOriginBadge.setStyle("-fx-background-color: #ede9fe; -fx-text-fill: #6366f1; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 10px; -fx-font-size: 11px;");
        } else {
            lblPatientOriginBadge.setText("🏠 Local");
            lblPatientOriginBadge.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 10px; -fx-font-size: 11px;");
        }

        if (patient.getBirthDate() != null) {
            int age = Period.between(patient.getBirthDate(), LocalDate.now()).getYears();
            lblPatientAge.setText("Edad: " + age + " años");
        } else {
            lblPatientAge.setText("Edad: N/A");
        }

        lblPatientPhone.setText("Tel: " + (patient.getPhone() != null && !patient.getPhone().isEmpty() ? patient.getPhone() : "Sin teléfono"));

        // Cargar historial de consultas
        loadConsultationHistory();
    }

    public void selectPatientByIdCard(String idCard) {
        Patient p = patientDAO.findByIdCard(idCard);
        if (p == null && !idCard.toUpperCase().startsWith("V-") && !idCard.toUpperCase().startsWith("E-")) {
            p = patientDAO.findByIdCard("V-" + idCard);
        }
        if (p != null) {
            cbSelectPatient.getSelectionModel().select(p);
            selectPatient(p);
        }
    }

    private void loadConsultationHistory() {
        if (currentPatient == null) return;
        consultationList.clear();
        List<Consultation> consultations = consultationDAO.findByPatientId(currentPatient.getId());
        consultationList.addAll(consultations);
        tableConsultations.setItems(consultationList);
        lblHistoryCount.setText(consultations.size() + " consulta(s) registrada(s)");

        if (!consultations.isEmpty()) {
            tableConsultations.getSelectionModel().selectFirst();
            showConsultationDetails(consultations.get(0));
        } else {
            selectedConsultationCard.setVisible(false);
        }
    }

    private void showConsultationDetails(Consultation c) {
        this.currentSelectedConsultation = c;
        selectedConsultationCard.setVisible(true);

        String dateStr = c.getDateTime() != null ? c.getDateTime().format(DATE_TIME_FORMATTER) : "N/A";
        lblDetailConsultDate.setText("Consulta del " + dateStr);

        String talla = c.getHeightM() != null ? String.format("%.2f m", c.getHeightM()) : "N/A";
        String peso = c.getWeightKg() != null ? String.format("%.1f kg", c.getWeightKg()) : "N/A";
        String imc = c.calculateBMI() != null ? String.format("%.1f", c.calculateBMI()) : "N/A";
        String obs = (c.getPhysicalNotes() != null && !c.getPhysicalNotes().isEmpty()) ? c.getPhysicalNotes() : "Sin observaciones físicas";

        lblDetailPhysical.setText(String.format("Talla: %s | Peso: %s | IMC: %s | Obs: %s",
                talla, peso, imc, obs));

        lblDetailReason.setText(c.getReason() != null ? c.getReason() : "Consulta General");
        lblDetailEvolution.setText(c.getClinicalNotes() != null && !c.getClinicalNotes().isEmpty() ? c.getClinicalNotes() : "Sin anotaciones adicionales.");
        lblDetailDiagnosis.setText(c.getDiagnosis() != null ? c.getDiagnosis() : "En observación");
        lblDetailRx.setText(c.getTreatmentRx() != null && !c.getTreatmentRx().isEmpty() ? c.getTreatmentRx() : "Sin récipe prescrito.");

        if (c.getNextAppointmentDate() != null) {
            lblDetailNextApp.setText("📅 Próxima Cita Agendada: " + c.getNextAppointmentDate().format(DATE_FORMATTER));
            lblDetailNextApp.setVisible(true);
        } else {
            lblDetailNextApp.setVisible(false);
        }
    }

    private void calculateBmiPreview() {
        try {
            double w = Double.parseDouble(txtWeight.getText().trim().replace(",", "."));
            double h = Double.parseDouble(txtHeight.getText().trim().replace(",", "."));
            if (h > 0) {
                double bmi = Math.round((w / (h * h)) * 10.0) / 10.0;
                lblBmiPreview.setText("IMC: " + bmi + " kg/m²");
                return;
            }
        } catch (Exception ignored) {}
        lblBmiPreview.setText("");
    }

    @FXML
    public void handleSearchPatient() {
        String query = txtSearchPatient.getText().trim();
        if (query.isEmpty()) return;

        Patient p = patientDAO.findByIdCard(query);
        if (p == null && !query.toUpperCase().startsWith("V-")) {
            p = patientDAO.findByIdCard("V-" + query);
        }
        if (p == null) {
            List<Patient> list = patientDAO.search(query);
            if (!list.isEmpty()) p = list.get(0);
        }

        if (p != null) {
            cbSelectPatient.getSelectionModel().select(p);
            selectPatient(p);
        } else {
            showError("No se encontró ningún paciente con el término: " + query);
        }
    }

    @FXML
    public void handleSaveConsultation() {
        saveConsultationProcess(false);
    }

    @FXML
    public void handleSaveAndGeneratePdf() {
        saveConsultationProcess(true);
    }

    private void saveConsultationProcess(boolean openPdf) {
        hideFeedback();

        if (currentPatient == null) {
            showError("Debe seleccionar un paciente para registrar la consulta.");
            return;
        }

        String reason = txtReason.getText().trim();
        String diagnosis = txtDiagnosis.getText().trim();
        String treatmentRx = txtTreatmentRx.getText().trim();
        String evolution = txtEvolution.getText().trim();
        String physicalNotes = txtPhysicalNotes.getText().trim();

        if (reason.isEmpty()) {
            showError("El motivo de consulta es obligatorio.");
            txtReason.requestFocus();
            return;
        }

        if (diagnosis.isEmpty()) {
            showError("Debe indicar el diagnóstico clínico del paciente.");
            txtDiagnosis.requestFocus();
            return;
        }

        // Obtener médico en sesión
        User doctor = SessionManager.getInstance().getCurrentUser();
        int doctorId = (doctor != null && doctor.getId() > 0) ? doctor.getId() : 1;

        // Parsear datos físicos
        Double weight = parseDouble(txtWeight.getText().trim());
        Double height = parseDouble(txtHeight.getText().trim());
        LocalDate nextApp = dpNextAppointment.getValue();

        Consultation newConsultation = new Consultation(
                -1,
                currentPatient.getId(),
                doctorId,
                LocalDateTime.now(),
                reason,
                evolution,
                diagnosis,
                treatmentRx,
                weight,
                height,
                physicalNotes,
                nextApp,
                null
        );

        boolean success = consultationDAO.insert(newConsultation);
        if (success) {
            showSuccess("✅ ¡Consulta médica registrada exitosamente en el expediente del paciente!");
            loadConsultationHistory();
            clearForm();

            if (openPdf) {
                generateAndOpenPdf(newConsultation);
            }
        } else {
            showError("Error al registrar la consulta en la base de datos.");
        }
    }

    @FXML
    public void handleGeneratePdfCurrent() {
        if (currentSelectedConsultation != null) {
            generateAndOpenPdf(currentSelectedConsultation);
        }
    }

    @FXML
    public void handleSendWhatsAppCurrent() {
        if (currentSelectedConsultation != null) {
            sendConsultationViaWhatsApp(currentSelectedConsultation);
        } else {
            showError("Debe seleccionar una consulta para enviar el informe por WhatsApp.");
        }
    }

    @FXML
    public void handleSendEmailCurrent() {
        if (currentSelectedConsultation != null) {
            sendConsultationViaEmail(currentSelectedConsultation);
        } else {
            showError("Debe seleccionar una consulta para enviar el informe por correo electrónico.");
        }
    }

    public void sendConsultationViaWhatsApp(Consultation consultation) {
        if (currentPatient == null || consultation == null) {
            showError("No hay un paciente o consulta seleccionada.");
            return;
        }

        try {
            // 1. Obtener o solicitar teléfono
            String phone = currentPatient.getPhone();
            if (phone == null || phone.replaceAll("[^0-9]", "").isEmpty()) {
                TextInputDialog phoneDialog = new TextInputDialog("0414-");
                phoneDialog.setTitle("WhatsApp del Paciente");
                phoneDialog.setHeaderText("El paciente " + currentPatient.getFullName() + " no tiene un teléfono registrado.");
                phoneDialog.setContentText("Ingresa el número de WhatsApp:");
                Optional<String> res = phoneDialog.showAndWait();
                if (res.isPresent() && !res.get().trim().isEmpty()) {
                    phone = res.get().trim();
                    currentPatient.setPhone(phone);
                    patientDAO.update(currentPatient);
                } else {
                    showError("Se requiere el número de teléfono para enviar por WhatsApp.");
                    return;
                }
            }

            // 2. Limpiar y formatear número internacional
            String digits = phone.replaceAll("[^0-9]", "");
            if (digits.startsWith("0")) {
                digits = "58" + digits.substring(1);
            } else if (digits.length() == 10 && digits.startsWith("3")) {
                digits = "57" + digits;
            } else if (!digits.startsWith("58") && !digits.startsWith("57") && !digits.startsWith("1") && digits.length() <= 10) {
                digits = "58" + digits;
            }

            // 3. Generar PDF si aún no existe
            File pdfFile = getOrGeneratePdfFile(consultation);

            // 4. Copiar archivo al portapapeles
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putFiles(java.util.Arrays.asList(new java.io.File(pdfFile.getAbsolutePath())));
            Clipboard.getSystemClipboard().setContent(clipboardContent);

            // 5. Preparar mensaje y abrir WhatsApp
            User doctor = SessionManager.getInstance().getCurrentUser();
            String docName = doctor != null ? doctor.getFullName() : "Dr. Mario Roa";
            String dateStr = consultation.getDateTime() != null ? consultation.getDateTime().format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER);

            String message = String.format("¡Hola %s! Le saludamos del Centro Médico MediClinic. Le hacemos entrega de su Informe Médico y Récipe de la consulta del %s emitida por el %s.",
                    currentPatient.getFullName(), dateStr, docName);

            String waUrl = "https://wa.me/" + digits + "?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(waUrl));
            } else {
                new ProcessBuilder("cmd", "/c", "start", "", waUrl).start();
            }

            // 6. Alerta informativa amigable y estilizada
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("WhatsApp Abierto con Éxito");
            alert.setHeaderText("💬 Conversación de WhatsApp iniciada con: +" + digits);
            alert.setContentText("Se ha abierto WhatsApp con el mensaje pre-cargado para el paciente.\n\n" +
                    "📄 El archivo PDF se ha COPIADO AUTOMÁTICAMENTE a su portapapeles.\n" +
                    "Al abrir la conversación en WhatsApp, simplemente presione 'Ctrl + V' para adjuntar el informe directamente como documento.");

            ButtonType btnOpenFolder = new ButtonType("📂 Abrir Carpeta con PDF", ButtonBar.ButtonData.OTHER);
            ButtonType btnOk = new ButtonType("✅ Entendido", ButtonBar.ButtonData.OK_DONE);
            alert.getButtonTypes().setAll(btnOpenFolder, btnOk);

            try {
                DialogPane dp = alert.getDialogPane();
                dp.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
                dp.getStyleClass().add("custom-alert-dialog");
                dp.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-padding: 8px; -fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-width: 1px; -fx-border-radius: 10px;");

                Button btnOkNode = (Button) dp.lookupButton(btnOk);
                if (btnOkNode != null) {
                    btnOkNode.setStyle("-fx-background-color: linear-gradient(to bottom right, #25D366, #128C7E); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7px 18px; -fx-background-radius: 8px; -fx-cursor: hand;");
                }
                Button btnFolderNode = (Button) dp.lookupButton(btnOpenFolder);
                if (btnFolderNode != null) {
                    btnFolderNode.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-weight: 600; -fx-padding: 7px 14px; -fx-background-radius: 8px; -fx-border-color: #cbd5e1; -fx-cursor: hand;");
                }
            } catch (Exception ignored) {}

            Optional<ButtonType> clickRes = alert.showAndWait();
            if (clickRes.isPresent() && clickRes.get() == btnOpenFolder) {
                openFolderWithFile(pdfFile);
            }

            showSuccess("✅ WhatsApp abierto para " + currentPatient.getFullName() + ". Archivo PDF copiado al portapapeles (Ctrl+V para adjuntar).");

        } catch (Exception e) {
            showError("Error al abrir WhatsApp: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendConsultationViaEmail(Consultation consultation) {
        if (currentPatient == null || consultation == null) {
            showError("No hay un paciente o consulta seleccionada.");
            return;
        }

        try {
            // 1. Obtener o solicitar correo
            String email = currentPatient.getEmail();
            if (email == null || email.trim().isEmpty() || !email.contains("@")) {
                TextInputDialog emailDialog = new TextInputDialog(email != null ? email : "");
                emailDialog.setTitle("Correo Electrónico del Paciente");
                emailDialog.setHeaderText("Ingrese o confirme el correo electrónico del paciente:");
                emailDialog.setContentText("Email:");
                Optional<String> res = emailDialog.showAndWait();
                if (res.isPresent() && res.get().contains("@")) {
                    email = res.get().trim();
                    currentPatient.setEmail(email);
                    patientDAO.update(currentPatient);
                } else {
                    showError("Debe ingresar una dirección de correo electrónico válida.");
                    return;
                }
            }

            // 2. Generar PDF
            File pdfFile = getOrGeneratePdfFile(consultation);

            // 3. Copiar archivo al portapapeles
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putFiles(java.util.Arrays.asList(new java.io.File(pdfFile.getAbsolutePath())));
            Clipboard.getSystemClipboard().setContent(clipboardContent);

            // 4. Preparar Asunto y Cuerpo
            User doctor = SessionManager.getInstance().getCurrentUser();
            String docName = doctor != null ? doctor.getFullName() : "Dr. Mario Roa";
            String dateStr = consultation.getDateTime() != null ? consultation.getDateTime().format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER);

            String subject = "Informe Médico y Récipe - " + currentPatient.getFullName() + " - MediClinic Pro";
            String body = String.format("Estimado(a) %s,\n\n" +
                    "Esperamos que se encuentre bien. Le hacemos entrega de su Informe Médico y Prescripción Farmacológica correspondiente a su consulta del día %s con el %s.\n\n" +
                    "Documento generado: %s\n" +
                    "Ubicación local del archivo: %s\n\n" +
                    "Atentamente,\n" +
                    "Centro Médico Especializado MediClinic\n" +
                    "Edif. Torre Médica | Tel: 0414-5551234",
                    currentPatient.getFullName(), dateStr, docName, pdfFile.getName(), pdfFile.getAbsolutePath());

            // 5. Intentar abrir cliente de correo del sistema (mailto)
            String mailtoUri = "mailto:" + email +
                    "?subject=" + URLEncoder.encode(subject, StandardCharsets.UTF_8).replace("+", "%20") +
                    "&body=" + URLEncoder.encode(body, StandardCharsets.UTF_8).replace("+", "%20");

            boolean mailtoOpened = false;
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
                    Desktop.getDesktop().mail(new URI(mailtoUri));
                    mailtoOpened = true;
                } else {
                    new ProcessBuilder("cmd", "/c", "start", "", mailtoUri).start();
                    mailtoOpened = true;
                }
            } catch (Exception ex) {
                System.err.println("Aviso: No se pudo abrir el cliente de correo nativo vía mailto: " + ex.getMessage());
            }

            // 6. Alerta informativa con detalles y botón de abrir carpeta
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Envío por Correo Electrónico (Gmail)");
            alert.setHeaderText("✉️ Correo preparado para: " + email);
            alert.setContentText((mailtoOpened ? "Se ha abierto su cliente de correo predeterminado.\n\n" : "") +
                    "📄 El archivo PDF generado se ha COPIADO AUTOMÁTICAMENTE al portapapeles.\n" +
                    "Puede adjuntarlo con 'Ctrl + V' o arrastrando el archivo desde la carpeta.");

            ButtonType btnOpenFolder = new ButtonType("📂 Abrir Carpeta con PDF", ButtonBar.ButtonData.OTHER);
            ButtonType btnOk = new ButtonType("✅ Listo", ButtonBar.ButtonData.OK_DONE);
            alert.getButtonTypes().setAll(btnOpenFolder, btnOk);

            try {
                DialogPane dp = alert.getDialogPane();
                dp.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
                dp.getStyleClass().add("custom-alert-dialog");
                dp.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-padding: 8px; -fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-width: 1px; -fx-border-radius: 10px;");

                Button btnOkNode = (Button) dp.lookupButton(btnOk);
                if (btnOkNode != null) {
                    btnOkNode.setStyle("-fx-background-color: linear-gradient(to bottom right, #ea4335, #c5221f); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7px 18px; -fx-background-radius: 8px; -fx-cursor: hand;");
                }
                Button btnFolderNode = (Button) dp.lookupButton(btnOpenFolder);
                if (btnFolderNode != null) {
                    btnFolderNode.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-weight: 600; -fx-padding: 7px 14px; -fx-background-radius: 8px; -fx-border-color: #cbd5e1; -fx-cursor: hand;");
                }
            } catch (Exception ignored) {}

            Optional<ButtonType> clickRes = alert.showAndWait();
            if (clickRes.isPresent() && clickRes.get() == btnOpenFolder) {
                openFolderWithFile(pdfFile);
            }

            showSuccess("✅ Correo preparado para " + email + ". Ruta del PDF copiada al portapapeles.");

        } catch (Exception e) {
            showError("Error al procesar el envío por correo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private File getOrGeneratePdfFile(Consultation consultation) throws IOException {
        User doctor = SessionManager.getInstance().getCurrentUser();
        if (doctor == null) {
            doctor = userDAO.findById(consultation.getDoctorId());
            if (doctor == null) doctor = userDAO.findFirstDoctor();
        }
        File pdfFile = PdfReportService.generateConsultationPdf(consultation, currentPatient, doctor);
        consultationDAO.updateIssuedDocumentPath(consultation.getId(), pdfFile.getAbsolutePath());
        return pdfFile;
    }

    private void openFolderWithFile(File file) {
        try {
            if (file != null && file.exists()) {
                new ProcessBuilder("explorer.exe", "/select,", file.getAbsolutePath()).start();
            } else {
                File dir = new File("reports");
                if (!dir.exists()) dir.mkdirs();
                new ProcessBuilder("explorer.exe", dir.getAbsolutePath()).start();
            }
        } catch (Exception e) {
            System.err.println("No se pudo abrir la carpeta del explorador: " + e.getMessage());
        }
    }

    private void generateAndOpenPdf(Consultation consultation) {
        try {
            File pdfFile = getOrGeneratePdfFile(consultation);
            showSuccess("✅ Documento PDF interactivo generado exitosamente: " + pdfFile.getName());
            PdfReportService.openPdfFile(pdfFile);

        } catch (Exception e) {
            showError("Error al generar el archivo PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleClearForm() {
        clearForm();
        hideFeedback();
    }

    private void clearForm() {
        txtReason.clear();
        txtHeight.clear();
        txtWeight.clear();
        txtPhysicalNotes.clear();
        lblBmiPreview.setText("");
        txtEvolution.clear();
        txtDiagnosis.clear();
        txtTreatmentRx.clear();
        dpNextAppointment.setValue(null);
    }

    private Double parseDouble(String val) {
        try {
            return Double.parseDouble(val.trim().replace(",", "."));
        } catch (Exception ignored) {
            return null;
        }
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
