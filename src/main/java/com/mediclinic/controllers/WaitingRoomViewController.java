package com.mediclinic.controllers;

import com.mediclinic.dao.DailyPaymentDAO;
import com.mediclinic.dao.PatientDAO;
import com.mediclinic.dao.WaitingRoomDAO;
import com.mediclinic.models.DailyPayment;
import com.mediclinic.models.Patient;
import com.mediclinic.models.User;
import com.mediclinic.models.WaitingRoomEntry;
import com.mediclinic.services.PdfReportService;
import com.mediclinic.services.SessionManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class WaitingRoomViewController {

    @FXML
    private Label lblCountWaiting;

    @FXML
    private Label lblCountInConsultation;

    @FXML
    private Label lblCountDone;

    // Admission form
    @FXML
    private VBox cardAdmission;

    @FXML
    private TextField txtSearchPatient;

    @FXML
    private ListView<Patient> listSuggestions;

    @FXML
    private VBox cardSelectedPatient;

    @FXML
    private Label lblPatientName;

    @FXML
    private Label lblPatientIdCard;

    @FXML
    private Label lblPatientPhone;

    @FXML
    private TextField txtReason;

    @FXML
    private TextField txtContactPhone;

    @FXML
    private Label lblAdmissionFeedback;

    @FXML
    private Button btnCloseDay;

    // Table
    @FXML
    private TableView<WaitingRoomEntry> tableWaiting;

    @FXML
    private TableColumn<WaitingRoomEntry, String> colTurn;

    @FXML
    private TableColumn<WaitingRoomEntry, String> colPatient;

    @FXML
    private TableColumn<WaitingRoomEntry, String> colPhone;

    @FXML
    private TableColumn<WaitingRoomEntry, String> colArrival;

    @FXML
    private TableColumn<WaitingRoomEntry, String> colReason;

    @FXML
    private TableColumn<WaitingRoomEntry, String> colStatus;

    @FXML
    private TableColumn<WaitingRoomEntry, String> colPayment;

    @FXML
    private TableColumn<WaitingRoomEntry, Void> colActions;

    private static WaitingRoomViewController instance;

    public static WaitingRoomViewController getInstance() {
        return instance;
    }

    private final WaitingRoomDAO waitingRoomDAO = new WaitingRoomDAO();
    private final PatientDAO patientDAO = new PatientDAO();
    private final DailyPaymentDAO dailyPaymentDAO = new DailyPaymentDAO();
    private final com.mediclinic.dao.FinancialClosingDAO financialClosingDAO = new com.mediclinic.dao.FinancialClosingDAO();

    private final ObservableList<WaitingRoomEntry> waitingList = FXCollections.observableArrayList();
    private final ObservableList<Patient> suggestedPatients = FXCollections.observableArrayList();

    private Patient selectedPatientForAdmission = null;
    private final DateTimeFormatter dtfTime = DateTimeFormatter.ofPattern("hh:mm a");
    private Timeline autoRefreshTimeline;

    @FXML
    public void initialize() {
        instance = this;
        // Configure list suggestions
        listSuggestions.setItems(suggestedPatients);
        listSuggestions.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Patient p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                } else {
                    String phoneText = (p.getPhone() != null && !p.getPhone().isEmpty()) ? " | 📞 " + p.getPhone() : "";
                    setText(p.getFullName() + " | C.I: " + p.getIdCard() + phoneText + " | HC: " + p.getMedicalRecordNumber());
                }
            }
        });

        listSuggestions.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectPatientForAdmission(newVal);
            }
        });

        txtSearchPatient.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                suggestedPatients.clear();
                listSuggestions.setVisible(false);
                listSuggestions.setManaged(false);
            } else {
                List<Patient> found = patientDAO.search(newVal.trim());
                suggestedPatients.setAll(found);
                listSuggestions.setVisible(!found.isEmpty());
                listSuggestions.setManaged(!found.isEmpty());
            }
        });

        setupTable();
        applyRoleRestrictions();
        loadWaitingRoom();
        startAutoRefresh();
    }

    private void applyRoleRestrictions() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        boolean isDoctor = currentUser != null && currentUser.isDoctor();

        if (isDoctor) {
            // El doctor solo tiene vista de solo lectura en vivo
            if (cardAdmission != null) {
                cardAdmission.setVisible(false);
                cardAdmission.setManaged(false);
            }
            if (colActions != null) {
                colActions.setVisible(false);
            }
            if (btnCloseDay != null) {
                btnCloseDay.setVisible(false);
                btnCloseDay.setManaged(false);
            }
        }
    }

    private void selectPatientForAdmission(Patient patient) {
        this.selectedPatientForAdmission = patient;
        txtSearchPatient.setText(patient.getFullName());
        lblPatientName.setText(patient.getFullName());
        lblPatientIdCard.setText("C.I: " + patient.getIdCard() + " | Exp: " + patient.getMedicalRecordNumber());
        
        String phone = patient.getPhone();
        if (lblPatientPhone != null) {
            lblPatientPhone.setText("📞 Tel / WhatsApp: " + (phone != null && !phone.isEmpty() ? phone : "Sin registrar"));
        }
        if (txtContactPhone != null) {
            txtContactPhone.setText(phone != null ? phone : "");
        }

        cardSelectedPatient.setVisible(true);
        cardSelectedPatient.setManaged(true);
        listSuggestions.setVisible(false);
        listSuggestions.setManaged(false);
    }

    @FXML
    public void handleClearPatient() {
        this.selectedPatientForAdmission = null;
        txtSearchPatient.clear();
        if (txtContactPhone != null) {
            txtContactPhone.clear();
        }
        cardSelectedPatient.setVisible(false);
        cardSelectedPatient.setManaged(false);
        listSuggestions.setVisible(false);
        listSuggestions.setManaged(false);
    }

    @FXML
    public void handleAdmitPatient() {
        lblAdmissionFeedback.setText("");
        if (selectedPatientForAdmission == null) {
            lblAdmissionFeedback.setText("⚠️ Debe seleccionar un paciente.");
            lblAdmissionFeedback.setStyle("-fx-text-fill: #ef4444;");
            return;
        }

        String phone = txtContactPhone != null ? txtContactPhone.getText().trim() : "";
        if (phone.isEmpty()) {
            lblAdmissionFeedback.setText("⚠️ Ingrese el número de teléfono / WhatsApp del paciente.");
            lblAdmissionFeedback.setStyle("-fx-text-fill: #ef4444;");
            if (txtContactPhone != null) txtContactPhone.requestFocus();
            return;
        }

        // Si el teléfono se ingresó o actualizó, persistirlo en el paciente
        if (!phone.equals(selectedPatientForAdmission.getPhone())) {
            selectedPatientForAdmission.setPhone(phone);
            patientDAO.update(selectedPatientForAdmission);
        }

        String reason = txtReason.getText().trim();
        if (reason.isEmpty()) {
            reason = "Consulta General / Evaluación";
        }

        WaitingRoomEntry entry = new WaitingRoomEntry(selectedPatientForAdmission.getId(), reason);
        boolean ok = waitingRoomDAO.insert(entry);
        if (ok) {
            lblAdmissionFeedback.setText("✅ Paciente ingresado a sala de espera.");
            lblAdmissionFeedback.setStyle("-fx-text-fill: #10b981;");
            handleClearPatient();
            txtReason.clear();
            if (txtContactPhone != null) txtContactPhone.clear();
            loadWaitingRoom();
            notifyDashboards();
        } else {
            lblAdmissionFeedback.setText("❌ Error al registrar en sala de espera.");
            lblAdmissionFeedback.setStyle("-fx-text-fill: #ef4444;");
        }
    }

    @FXML
    public void handleCloseDayAndCashRegister() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cerrar Caja y Jornada Diaria");
        alert.setHeaderText("¿Cerrar Caja y Finalizar Jornada Médica?");
        alert.setContentText("Esta acción calculará los totales de recaudación del día, registrará el Cierre Financiero en la base de datos y vaciará la sala de espera para la siguiente jornada.");

        ButtonType btnYes = new ButtonType("📊 Sí, Cerrar Caja", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnYes, btnNo);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == btnYes) {
            try {
                List<DailyPayment> payments = dailyPaymentDAO.findAllToday();
                List<WaitingRoomEntry> entries = waitingRoomDAO.findAllToday();
                User user = SessionManager.getInstance().getCurrentUser();
                String closedByName = user != null ? user.getFullName() : "Secretaria";

                double totalUsd = 0.0;
                double totalVes = 0.0;
                double totalCop = 0.0;
                for (DailyPayment p : payments) {
                    if ("USD".equalsIgnoreCase(p.getCurrency())) {
                        totalUsd += p.getAmount();
                    } else if ("VES".equalsIgnoreCase(p.getCurrency())) {
                        totalVes += p.getAmount();
                    } else if ("COP".equalsIgnoreCase(p.getCurrency())) {
                        totalCop += p.getAmount();
                    }
                }

                String timeNow = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));
                com.mediclinic.models.FinancialClosing closing = new com.mediclinic.models.FinancialClosing(
                        LocalDate.now(),
                        timeNow,
                        totalUsd,
                        totalVes,
                        totalCop,
                        entries.size(),
                        closedByName,
                        "Cierre de Caja Diario"
                );
                financialClosingDAO.insert(closing);

                // Vaciar sala de espera
                waitingRoomDAO.clearAll();
                loadWaitingRoom();
                notifyDashboards();

                // Modal de Opciones: Generar PDF vs Solo Enviar a Resúmenes
                showPostClosingOptionsDialog(payments, entries, user, closing);

            } catch (Exception e) {
                System.err.println("Error al generar cierre de caja: " + e.getMessage());
                e.printStackTrace();
                Alert err = new Alert(Alert.AlertType.ERROR, "Error al procesar el cierre de caja: " + e.getMessage());
                err.showAndWait();
            }
        }
    }

    private void showPostClosingOptionsDialog(List<DailyPayment> payments, List<WaitingRoomEntry> entries, User user, com.mediclinic.models.FinancialClosing closing) {
        Dialog<String> optDialog = new Dialog<>();
        optDialog.setTitle("Cierre de Caja Registrado con Éxito");
        optDialog.setHeaderText("📊 Cierre de Caja Guardado en el Sistema\n" +
                "Recaudación: " + closing.getFormattedUsd() + " | " + closing.getFormattedVes() + " | " + closing.getFormattedCop());

        ButtonType btnPdf = new ButtonType("🖨️ Generar PDF e Imprimir", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnSendOnly = new ButtonType("📤 Solo Enviar a Resúmenes del Doctor", ButtonBar.ButtonData.OTHER);
        optDialog.getDialogPane().getButtonTypes().addAll(btnPdf, btnSendOnly);

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setStyle("-fx-background-color: rgba(248, 250, 252, 0.95); -fx-background-radius: 10px;");

        Label lblMsg = new Label("La jornada ha sido cerrada y los datos financieros están guardados en la base de datos.\n¿Cómo desea proceder ahora?");
        lblMsg.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");
        lblMsg.setWrapText(true);

        content.getChildren().add(lblMsg);
        optDialog.getDialogPane().setContent(content);

        optDialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnPdf) {
                return "PDF";
            } else if (dialogButton == btnSendOnly) {
                return "SEND_ONLY";
            }
            return null;
        });

        Optional<String> chosen = optDialog.showAndWait();
        if (chosen.isPresent()) {
            if ("PDF".equals(chosen.get())) {
                try {
                    File pdfFile = PdfReportService.generateDailyCashClosingPdf(payments, entries, user);
                    PdfReportService.openPdfFile(pdfFile);

                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Informe PDF Generado");
                    successAlert.setHeaderText("¡PDF Creado con Éxito!");
                    successAlert.setContentText("El informe financiero se ha guardado en:\n" + pdfFile.getAbsolutePath());
                    successAlert.showAndWait();
                } catch (Exception ex) {
                    Alert err = new Alert(Alert.AlertType.ERROR, "No se pudo generar el PDF: " + ex.getMessage());
                    err.showAndWait();
                }
            } else if ("SEND_ONLY".equals(chosen.get())) {
                Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                infoAlert.setTitle("Enviado al Doctor");
                infoAlert.setHeaderText("¡Resumen Financiero Disponible para el Doctor!");
                infoAlert.setContentText("El cierre ha sido registrado y está disponible en la vista de Resúmenes y Auditoría del Doctor.");
                infoAlert.showAndWait();
            }
        }
    }

    @FXML
    public void handleClearWaitingRoom() {
        handleCloseDayAndCashRegister();
    }

    private void notifyDashboards() {
        if (DoctorDashboardController.getInstance() != null) {
            DoctorDashboardController.getInstance().refreshMetrics();
        }
        if (SecretaryDashboardController.getInstance() != null) {
            SecretaryDashboardController.getInstance().refreshMetrics();
        }
    }

    private void setupTable() {
        // Numeración de turno consecutiva
        colTurn.setCellValueFactory(cellData -> {
            int turn = cellData.getValue().getDailyTurnNumber();
            if (turn <= 0) {
                turn = tableWaiting.getItems().indexOf(cellData.getValue()) + 1;
            }
            return new SimpleStringProperty("#" + turn);
        });
        
        colPatient.setCellValueFactory(cellData -> {
            WaitingRoomEntry e = cellData.getValue();
            return new SimpleStringProperty(e.getPatientName() + " (" + e.getPatientIdCard() + ")");
        });

        colPhone.setCellValueFactory(cellData -> {
            String ph = cellData.getValue().getPatientPhone();
            return new SimpleStringProperty(ph != null && !ph.isEmpty() ? ph : "Sin teléfono");
        });

        colArrival.setCellValueFactory(cellData -> {
            LocalDateTime dt = cellData.getValue().getArrivalTime();
            return new SimpleStringProperty(dt != null ? dt.format(dtfTime) : "");
        });

        colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label();
                    badge.setMaxWidth(Double.MAX_VALUE);
                    badge.setAlignment(Pos.CENTER);
                    switch (status.toUpperCase()) {
                        case "EN_ESPERA":
                            badge.setText("⏳ En Espera");
                            badge.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-font-weight: bold; -fx-padding: 3px 6px; -fx-background-radius: 10px; -fx-font-size: 10px;");
                            break;
                        case "EN_CONSULTA":
                            badge.setText("🩺 En Consulta");
                            badge.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #3730a3; -fx-font-weight: bold; -fx-padding: 3px 6px; -fx-background-radius: 10px; -fx-font-size: 10px;");
                            break;
                        case "ATENDIDO":
                            badge.setText("✅ Atendido");
                            badge.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-weight: bold; -fx-padding: 3px 6px; -fx-background-radius: 10px; -fx-font-size: 10px;");
                            break;
                        default:
                            badge.setText("❌ Cancelado");
                            badge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-font-weight: bold; -fx-padding: 3px 6px; -fx-background-radius: 10px; -fx-font-size: 10px;");
                            break;
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // Columna de Estado de Pago
        colPayment.setCellValueFactory(cellData -> {
            WaitingRoomEntry e = cellData.getValue();
            return new SimpleStringProperty(e.isPaid() ? e.getPaymentDetail() : "Pendiente");
        });
        colPayment.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String paymentDetail, boolean empty) {
                super.updateItem(paymentDetail, empty);
                if (empty || paymentDetail == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label();
                    badge.setMaxWidth(Double.MAX_VALUE);
                    badge.setAlignment(Pos.CENTER);
                    if (!"Pendiente".equalsIgnoreCase(paymentDetail)) {
                        badge.setText("✅ " + paymentDetail);
                        badge.setStyle("-fx-background-color: #ecfdf5; -fx-text-fill: #047857; -fx-font-weight: bold; -fx-padding: 3px 6px; -fx-background-radius: 10px; -fx-font-size: 10px; -fx-border-color: #a7f3d0; -fx-border-radius: 10px;");
                    } else {
                        badge.setText("⏳ Pendiente");
                        badge.setStyle("-fx-background-color: #fffbeb; -fx-text-fill: #b45309; -fx-font-weight: bold; -fx-padding: 3px 6px; -fx-background-radius: 10px; -fx-font-size: 10px; -fx-border-color: #fde68a; -fx-border-radius: 10px;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // Columna de Acciones (Solo visible para secretaria)
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnPay = new Button("💵 Pago");
            private final Button btnCall = new Button("🔔");
            private final Button btnFinish = new Button("✅");
            private final Button btnRemove = new Button("❌");
            private final HBox container = new HBox(4);

            {
                container.setAlignment(Pos.CENTER);
                btnPay.setStyle("-fx-font-size: 10px; -fx-padding: 3px 6px; -fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-cursor: hand;");
                btnCall.setStyle("-fx-font-size: 10px; -fx-padding: 3px 6px; -fx-background-color: #e0f2fe; -fx-text-fill: #0284c7; -fx-background-radius: 4px; -fx-cursor: hand;");
                btnFinish.setStyle("-fx-font-size: 10px; -fx-padding: 3px 6px; -fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-background-radius: 4px; -fx-cursor: hand;");
                btnRemove.setStyle("-fx-font-size: 10px; -fx-padding: 3px 6px; -fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-background-radius: 4px; -fx-cursor: hand;");

                btnPay.setOnAction(e -> {
                    WaitingRoomEntry item = getTableView().getItems().get(getIndex());
                    openPaymentModal(item);
                });

                btnCall.setOnAction(e -> {
                    WaitingRoomEntry item = getTableView().getItems().get(getIndex());
                    waitingRoomDAO.updateStatus(item.getId(), "EN_CONSULTA");
                    loadWaitingRoom();
                    notifyDashboards();
                });

                btnFinish.setOnAction(e -> {
                    WaitingRoomEntry item = getTableView().getItems().get(getIndex());
                    waitingRoomDAO.updateStatus(item.getId(), "ATENDIDO");
                    loadWaitingRoom();
                    notifyDashboards();
                });

                btnRemove.setOnAction(e -> {
                    WaitingRoomEntry item = getTableView().getItems().get(getIndex());
                    waitingRoomDAO.updateStatus(item.getId(), "CANCELADO");
                    loadWaitingRoom();
                    notifyDashboards();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    WaitingRoomEntry entry = getTableView().getItems().get(getIndex());
                    container.getChildren().clear();
                    
                    // Botón de pago siempre disponible si no ha pagado
                    if (!entry.isPaid()) {
                        container.getChildren().add(btnPay);
                    }

                    if ("EN_ESPERA".equalsIgnoreCase(entry.getStatus())) {
                        container.getChildren().addAll(btnCall, btnRemove);
                    } else if ("EN_CONSULTA".equalsIgnoreCase(entry.getStatus())) {
                        container.getChildren().addAll(btnFinish, btnRemove);
                    } else {
                        container.getChildren().add(btnRemove);
                    }
                    setGraphic(container);
                }
            }
        });

        tableWaiting.setItems(waitingList);
    }

    public void openPaymentModal(WaitingRoomEntry entry) {
        Dialog<DailyPayment> dialog = new Dialog<>();
        dialog.setTitle("Registrar Pago de Consulta");
        dialog.setHeaderText("Registrar Cobro para: " + entry.getPatientName() + "\nC.I: " + entry.getPatientIdCard());

        ButtonType btnSave = new ButtonType("💾 Guardar Pago", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSave, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15, 20, 10, 10));

        ComboBox<String> cbMethod = new ComboBox<>(FXCollections.observableArrayList(
                "Efectivo", "Punto de Venta", "Pago Móvil", "Zelle", "Transferencia"
        ));
        cbMethod.setValue("Efectivo");
        cbMethod.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cbCurrency = new ComboBox<>(FXCollections.observableArrayList(
                "USD", "VES", "COP"
        ));
        cbCurrency.setValue("USD");
        cbCurrency.setMaxWidth(Double.MAX_VALUE);

        TextField txtAmount = new TextField();
        txtAmount.setPromptText("Ej. 50.00");

        TextField txtNotes = new TextField();
        txtNotes.setPromptText("Notas / Referencia bancaria (opcional)");

        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-font-weight: bold;");

        grid.add(new Label("Medio de Pago *:"), 0, 0);
        grid.add(cbMethod, 1, 0);
        grid.add(new Label("Moneda *:"), 0, 1);
        grid.add(cbCurrency, 1, 1);
        grid.add(new Label("Cantidad / Monto *:"), 0, 2);
        grid.add(txtAmount, 1, 2);
        grid.add(new Label("Referencia / Notas:"), 0, 3);
        grid.add(txtNotes, 1, 3);
        grid.add(lblErr, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSave) {
                String amtStr = txtAmount.getText().trim().replace(",", ".");
                if (amtStr.isEmpty()) {
                    return null;
                }
                try {
                    double amount = Double.parseDouble(amtStr);
                    if (amount <= 0) return null;
                    return new DailyPayment(
                            entry.getPatientId(),
                            entry.getId(),
                            cbMethod.getValue(),
                            cbCurrency.getValue(),
                            amount,
                            txtNotes.getText().trim()
                    );
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            return null;
        });

        Optional<DailyPayment> res = dialog.showAndWait();
        res.ifPresent(payment -> {
            boolean ok = dailyPaymentDAO.insert(payment);
            if (ok) {
                loadWaitingRoom();
                notifyDashboards();
            } else {
                Alert err = new Alert(Alert.AlertType.ERROR, "No se pudo registrar el pago en la base de datos.");
                err.showAndWait();
            }
        });
    }

    public void loadWaitingRoom() {
        List<WaitingRoomEntry> list = waitingRoomDAO.findAllToday();
        waitingList.setAll(list);

        long waiting = list.stream().filter(e -> "EN_ESPERA".equalsIgnoreCase(e.getStatus())).count();
        long inConsult = list.stream().filter(e -> "EN_CONSULTA".equalsIgnoreCase(e.getStatus())).count();
        long done = list.stream().filter(e -> "ATENDIDO".equalsIgnoreCase(e.getStatus())).count();

        lblCountWaiting.setText(String.valueOf(waiting));
        lblCountInConsultation.setText(String.valueOf(inConsult));
        lblCountDone.setText(String.valueOf(done));
    }

    private void startAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> loadWaitingRoom()));
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }
}
