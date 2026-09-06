package com.mediclinic.controllers;

import com.mediclinic.dao.ConfigDAO;
import com.mediclinic.dao.PatientDAO;
import com.mediclinic.dao.ProcedureDAO;
import com.mediclinic.models.Patient;
import com.mediclinic.models.ProcedureQuote;
import com.mediclinic.models.User;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ProceduresViewController {

    // Header & Currency Rates Display
    @FXML
    private Label lblHeaderTasaVes;

    @FXML
    private Label lblHeaderTasaCop;

    // Left Form ScrollPane (Hidden for Secretary)
    @FXML
    private ScrollPane leftFormScrollPane;

    // Search and Selected Patient
    @FXML
    private TextField txtPatientSearch;

    @FXML
    private ListView<Patient> listPatientSuggestions;

    @FXML
    private VBox cardSelectedPatient;

    @FXML
    private Label lblPatientName;

    @FXML
    private Label lblPatientIdCard;

    @FXML
    private Label lblPatientHistory;

    @FXML
    private Label lblPatientOrigin;

    // Form inputs
    @FXML
    private TextField txtProcedureType;

    @FXML
    private TextArea txtDescription;

    @FXML
    private TextField txtAmountUsd;

    @FXML
    private DatePicker dpPlannedDate;

    @FXML
    private ComboBox<String> cbInitialStatus;

    @FXML
    private TextArea txtNotes;

    @FXML
    private Label lblFormFeedback;

    // Live Multi-Currency Conversion Cards
    @FXML
    private Label lblConvertedUsd;

    @FXML
    private Label lblConvertedVes;

    @FXML
    private Label lblConvertedCop;

    @FXML
    private Label lblRateVesSub;

    @FXML
    private Label lblRateCopSub;

    // Table and Filters
    @FXML
    private TextField txtTableSearch;

    @FXML
    private Button btnFilterAll;

    @FXML
    private Button btnFilterQuoted;

    @FXML
    private Button btnFilterPlanned;

    @FXML
    private Button btnFilterCompleted;

    @FXML
    private TableView<ProcedureQuote> tableQuotes;

    @FXML
    private TableColumn<ProcedureQuote, String> colId;

    @FXML
    private TableColumn<ProcedureQuote, String> colPatient;

    @FXML
    private TableColumn<ProcedureQuote, String> colProcedure;

    @FXML
    private TableColumn<ProcedureQuote, String> colUsd;

    @FXML
    private TableColumn<ProcedureQuote, String> colVes;

    @FXML
    private TableColumn<ProcedureQuote, String> colCop;

    @FXML
    private TableColumn<ProcedureQuote, String> colStatus;

    @FXML
    private TableColumn<ProcedureQuote, String> colPlannedDate;

    @FXML
    private TableColumn<ProcedureQuote, Void> colActions;

    // Detail Panel
    @FXML
    private VBox detailPanel;

    @FXML
    private Label lblDetailTitle;

    @FXML
    private Label lblDetailPatient;

    @FXML
    private Label lblDetailStatus;

    @FXML
    private Label lblDetailUsd;

    @FXML
    private Label lblDetailVes;

    @FXML
    private Label lblDetailCop;

    @FXML
    private Label lblDetailPlannedDate;

    @FXML
    private Label lblDetailNotes;

    @FXML
    private Button btnDetailPlan;

    @FXML
    private Button btnDetailComplete;

    @FXML
    private Button btnDetailCancel;

    // DAOs and Data
    private final ProcedureDAO procedureDAO = new ProcedureDAO();
    private final PatientDAO patientDAO = new PatientDAO();
    private final ConfigDAO configDAO = new ConfigDAO();

    private final ObservableList<ProcedureQuote> quoteList = FXCollections.observableArrayList();
    private final ObservableList<Patient> suggestedPatients = FXCollections.observableArrayList();

    private Patient currentSelectedPatient = null;
    private ProcedureQuote selectedQuote = null;
    private String currentStatusFilter = "TODOS";

    private double currentRateVes = 38.50;
    private double currentRateCop = 4100.0;

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final DateTimeFormatter dfDate = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String formatUsd(double amount) {
        return "$ " + String.format(Locale.US, "%,.2f", amount);
    }

    private String formatVes(double amount) {
        return "Bs. " + String.format(Locale.US, "%,.2f", amount);
    }

    private String formatCop(double amount) {
        return "$ " + String.format(Locale.US, "%,.0f", amount);
    }

    @FXML
    public void initialize() {
        // Cargar tasas iniciales
        loadExchangeRates();

        // Control de Acceso por Roles (RBAC): Vista de solo lectura para la Secretaria
        User currentUser = SessionManager.getInstance().getCurrentUser();
        boolean isSecretary = currentUser != null && currentUser.isSecretary();

        if (isSecretary) {
            if (leftFormScrollPane != null) {
                leftFormScrollPane.setVisible(false);
                leftFormScrollPane.setManaged(false);
            }
            if (colActions != null) {
                colActions.setVisible(false);
            }
        }

        // Configurar estados iniciales
        if (cbInitialStatus != null) {
            cbInitialStatus.setItems(FXCollections.observableArrayList("COTIZADA", "PLANIFICADA", "REALIZADA"));
            cbInitialStatus.setValue("COTIZADA");
        }

        // Configurar sugerencias de pacientes
        if (listPatientSuggestions != null) {
            listPatientSuggestions.setItems(suggestedPatients);
            listPatientSuggestions.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Patient p, boolean empty) {
                    super.updateItem(p, empty);
                    if (empty || p == null) {
                        setText(null);
                    } else {
                        setText(p.getFullName() + " | C.I: " + p.getIdCard() + " | HC: " + p.getMedicalRecordNumber());
                    }
                }
            });

            listPatientSuggestions.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    selectPatient(newVal);
                    listPatientSuggestions.setVisible(false);
                    listPatientSuggestions.setManaged(false);
                }
            });
        }

        // Listener para búsqueda en vivo de pacientes
        if (txtPatientSearch != null) {
            txtPatientSearch.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    suggestedPatients.clear();
                    listPatientSuggestions.setVisible(false);
                    listPatientSuggestions.setManaged(false);
                } else {
                    List<Patient> found = patientDAO.search(newVal.trim());
                    suggestedPatients.setAll(found);
                    listPatientSuggestions.setVisible(!found.isEmpty());
                    listPatientSuggestions.setManaged(!found.isEmpty());
                }
            });
        }

        // Conversor Multi-Moneda Reactivo en Vivo
        if (txtAmountUsd != null) {
            txtAmountUsd.textProperty().addListener((obs, oldVal, newVal) -> {
                calculateLiveConversions();
            });
        }

        // Configuración de Tabla
        setupTableColumns();
        loadQuotes();

        // Listener de Selección en Tabla
        tableQuotes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            showQuoteDetails(newVal);
        });

        // Listener de búsqueda en tabla
        if (txtTableSearch != null) {
            txtTableSearch.textProperty().addListener((obs, oldVal, newVal) -> {
                filterQuotes();
            });
        }
    }

    public void loadExchangeRates() {
        currentRateVes = configDAO.getDoubleValue("TASA_USD_VES", 38.50);
        currentRateCop = configDAO.getDoubleValue("TASA_USD_COP", 4100.0);

        if (lblHeaderTasaVes != null) {
            lblHeaderTasaVes.setText("VES: Bs. " + String.format(Locale.US, "%.2f", currentRateVes));
        }
        if (lblHeaderTasaCop != null) {
            lblHeaderTasaCop.setText("COP: $" + String.format(Locale.US, "%,.0f", currentRateCop));
        }

        if (lblRateVesSub != null) {
            lblRateVesSub.setText("Tasa: 1 USD = Bs. " + String.format(Locale.US, "%.2f", currentRateVes));
        }
        if (lblRateCopSub != null) {
            lblRateCopSub.setText("Tasa: 1 USD = $" + String.format(Locale.US, "%,.0f", currentRateCop));
        }

        calculateLiveConversions();
    }

    private void calculateLiveConversions() {
        String text = txtAmountUsd != null ? txtAmountUsd.getText() : null;
        if (text == null || text.trim().isEmpty()) {
            if (lblConvertedUsd != null) lblConvertedUsd.setText("$ 0.00");
            if (lblConvertedVes != null) lblConvertedVes.setText("Bs. 0.00");
            if (lblConvertedCop != null) lblConvertedCop.setText("COP 0");
            return;
        }

        try {
            String clean = text.replace(",", ".").trim();
            double usd = Double.parseDouble(clean);
            if (usd < 0) usd = 0;

            double ves = usd * currentRateVes;
            double cop = usd * currentRateCop;

            if (lblConvertedUsd != null) lblConvertedUsd.setText(formatUsd(usd));
            if (lblConvertedVes != null) lblConvertedVes.setText(formatVes(ves));
            if (lblConvertedCop != null) lblConvertedCop.setText(formatCop(cop));
        } catch (NumberFormatException e) {
            if (lblConvertedUsd != null) lblConvertedUsd.setText("Inválido");
            if (lblConvertedVes != null) lblConvertedVes.setText("Inválido");
            if (lblConvertedCop != null) lblConvertedCop.setText("Inválido");
        }
    }

    public void selectPatient(Patient patient) {
        if (patient == null) return;
        this.currentSelectedPatient = patient;
        if (txtPatientSearch != null) txtPatientSearch.setText(patient.getFullName());
        if (lblPatientName != null) lblPatientName.setText(patient.getFullName());
        if (lblPatientIdCard != null) lblPatientIdCard.setText("C.I: " + patient.getIdCard());
        if (lblPatientHistory != null) lblPatientHistory.setText("Exp: " + patient.getMedicalRecordNumber());

        if (lblPatientOrigin != null) {
            if (patient.isForaneo()) {
                lblPatientOrigin.setText("✈️ Foráneo (" + (patient.getOriginCity() != null ? patient.getOriginCity() : "Otra ciudad") + ")");
                lblPatientOrigin.setStyle("-fx-background-color: #ede9fe; -fx-text-fill: #6366f1; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 10px; -fx-font-size: 11px;");
            } else {
                lblPatientOrigin.setText("🏠 Local");
                lblPatientOrigin.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 10px; -fx-font-size: 11px;");
            }
        }

        if (cardSelectedPatient != null) {
            cardSelectedPatient.setVisible(true);
            cardSelectedPatient.setManaged(true);
        }
        if (listPatientSuggestions != null) {
            listPatientSuggestions.setVisible(false);
            listPatientSuggestions.setManaged(false);
        }
    }

    @FXML
    public void handleClearPatient() {
        this.currentSelectedPatient = null;
        if (txtPatientSearch != null) txtPatientSearch.clear();
        if (cardSelectedPatient != null) {
            cardSelectedPatient.setVisible(false);
            cardSelectedPatient.setManaged(false);
        }
        if (listPatientSuggestions != null) {
            listPatientSuggestions.setVisible(false);
            listPatientSuggestions.setManaged(false);
        }
    }

    @FXML
    public void handleOpenRatesModal() {
        ExchangeRateDialog.showAndUpdateRates(configDAO, this::loadExchangeRates);
    }

    @FXML
    public void handleSaveQuote() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.isSecretary()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Acceso restringido: Las secretarias solo tienen permiso de lectura en cotizaciones y procedimientos.");
            alert.setHeaderText("Permiso Denegado");
            alert.showAndWait();
            return;
        }

        if (lblFormFeedback != null) lblFormFeedback.setText("");

        if (currentSelectedPatient == null) {
            if (lblFormFeedback != null) {
                lblFormFeedback.setText("⚠️ Debes seleccionar un paciente.");
                lblFormFeedback.setStyle("-fx-text-fill: #ef4444;");
            }
            return;
        }

        String procType = txtProcedureType != null ? txtProcedureType.getText().trim() : "";
        if (procType.isEmpty()) {
            if (lblFormFeedback != null) {
                lblFormFeedback.setText("⚠️ Ingresa el nombre de la cirugía o procedimiento.");
                lblFormFeedback.setStyle("-fx-text-fill: #ef4444;");
            }
            return;
        }

        String amountStr = txtAmountUsd != null ? txtAmountUsd.getText() : "";
        if (amountStr == null || amountStr.trim().isEmpty()) {
            if (lblFormFeedback != null) {
                lblFormFeedback.setText("⚠️ Ingresa el precio base en USD.");
                lblFormFeedback.setStyle("-fx-text-fill: #ef4444;");
            }
            return;
        }

        double usd;
        try {
            usd = Double.parseDouble(amountStr.replace(",", ".").trim());
            if (usd <= 0) {
                if (lblFormFeedback != null) {
                    lblFormFeedback.setText("⚠️ El precio en USD debe ser mayor a 0.");
                    lblFormFeedback.setStyle("-fx-text-fill: #ef4444;");
                }
                return;
            }
        } catch (NumberFormatException e) {
            if (lblFormFeedback != null) {
                lblFormFeedback.setText("⚠️ Monto USD inválido. Ej. 150.00");
                lblFormFeedback.setStyle("-fx-text-fill: #ef4444;");
            }
            return;
        }

        String description = txtDescription != null ? txtDescription.getText() : "";
        String notes = txtNotes != null ? txtNotes.getText() : "";
        String status = (cbInitialStatus != null && cbInitialStatus.getValue() != null) ? cbInitialStatus.getValue() : "COTIZADA";

        LocalDateTime plannedDateTime = null;
        if (dpPlannedDate != null && dpPlannedDate.getValue() != null) {
            plannedDateTime = dpPlannedDate.getValue().atTime(9, 0); // Hora por defecto 9:00 AM
        } else if ("PLANIFICADA".equals(status)) {
            plannedDateTime = LocalDateTime.now().plusDays(1);
        }

        ProcedureQuote quote = new ProcedureQuote(
                currentSelectedPatient.getId(),
                procType,
                description,
                usd,
                currentRateVes,
                currentRateCop,
                status,
                plannedDateTime,
                notes
        );

        boolean ok = procedureDAO.insert(quote);
        if (ok) {
            if (lblFormFeedback != null) {
                lblFormFeedback.setText("✅ Cotización guardada con éxito (ID #" + quote.getId() + ")");
                lblFormFeedback.setStyle("-fx-text-fill: #10b981;");
            }
            handleClearForm();
            loadQuotes();
            tableQuotes.getSelectionModel().select(quote);
        } else {
            if (lblFormFeedback != null) {
                lblFormFeedback.setText("❌ Error al registrar en la base de datos.");
                lblFormFeedback.setStyle("-fx-text-fill: #ef4444;");
            }
        }
    }

    @FXML
    public void handleClearForm() {
        handleClearPatient();
        if (txtProcedureType != null) {
            txtProcedureType.clear();
        }
        if (txtDescription != null) txtDescription.clear();
        if (txtAmountUsd != null) txtAmountUsd.clear();
        if (dpPlannedDate != null) dpPlannedDate.setValue(null);
        if (cbInitialStatus != null) cbInitialStatus.setValue("COTIZADA");
        if (txtNotes != null) txtNotes.clear();
        if (lblFormFeedback != null) lblFormFeedback.setText("");
        calculateLiveConversions();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(cellData -> new SimpleStringProperty("#" + cellData.getValue().getId()));
        
        colPatient.setCellValueFactory(cellData -> {
            ProcedureQuote q = cellData.getValue();
            return new SimpleStringProperty(q.getPatientName() + " (" + q.getPatientIdCard() + ")");
        });

        colProcedure.setCellValueFactory(new PropertyValueFactory<>("procedureType"));
        
        colUsd.setCellValueFactory(cellData -> new SimpleStringProperty(formatUsd(cellData.getValue().getAmountUsd())));
        colVes.setCellValueFactory(cellData -> new SimpleStringProperty(formatVes(cellData.getValue().getAmountVes())));
        colCop.setCellValueFactory(cellData -> new SimpleStringProperty(formatCop(cellData.getValue().getAmountCop())));

        colPlannedDate.setCellValueFactory(cellData -> {
            LocalDateTime dt = cellData.getValue().getPlannedDate();
            if (dt != null) {
                return new SimpleStringProperty(dt.format(dfDate));
            }
            return new SimpleStringProperty("Sin agendar");
        });

        // Celda personalizada para el Estado con Emojis Específicos
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new TableCell<>() {
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
                        case "COTIZADA":
                            badge.setText("💲 Cotizada");
                            badge.getStyleClass().add("badge-status-quoted");
                            break;
                        case "PLANIFICADA":
                            badge.setText("📝 Planificada");
                            badge.getStyleClass().add("badge-status-planned");
                            break;
                        case "REALIZADA":
                            badge.setText("✅ Realizada");
                            badge.getStyleClass().add("badge-status-completed");
                            break;
                        case "CANCELADA":
                            badge.setText("🔴 Cancelada");
                            badge.getStyleClass().add("badge-status-cancelled");
                            break;
                        default:
                            badge.setText(status);
                            break;
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // Columna de Acciones Rápidas (Solo habilitada para DOCTOR)
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnPlan = new Button("📝 Planificar");
            private final Button btnDone = new Button("✅ Realizar");
            private final Button btnMenu = new Button("⚙️");
            private final HBox container = new HBox(4, btnMenu);

            {
                container.setAlignment(Pos.CENTER);
                btnPlan.setStyle("-fx-font-size: 10px; -fx-padding: 3px 6px; -fx-background-color: #e0e7ff; -fx-text-fill: #3730a3; -fx-background-radius: 4px; -fx-cursor: hand;");
                btnDone.setStyle("-fx-font-size: 10px; -fx-padding: 3px 6px; -fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-background-radius: 4px; -fx-cursor: hand;");
                btnMenu.setStyle("-fx-font-size: 10px; -fx-padding: 3px 6px; -fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 4px; -fx-cursor: hand;");

                btnPlan.setOnAction(e -> {
                    ProcedureQuote q = getTableView().getItems().get(getIndex());
                    promptPlanDateAndAdvance(q);
                });

                btnDone.setOnAction(e -> {
                    ProcedureQuote q = getTableView().getItems().get(getIndex());
                    advanceStatus(q, "REALIZADA");
                });

                btnMenu.setOnAction(e -> {
                    ProcedureQuote q = getTableView().getItems().get(getIndex());
                    showQuoteOptionsMenu(q, btnMenu);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User currentUser = SessionManager.getInstance().getCurrentUser();
                    if (currentUser != null && currentUser.isSecretary()) {
                        setGraphic(null);
                        return;
                    }

                    ProcedureQuote q = getTableView().getItems().get(getIndex());
                    container.getChildren().clear();
                    if ("COTIZADA".equalsIgnoreCase(q.getStatus())) {
                        container.getChildren().addAll(btnPlan, btnMenu);
                    } else if ("PLANIFICADA".equalsIgnoreCase(q.getStatus())) {
                        container.getChildren().addAll(btnDone, btnMenu);
                    } else {
                        container.getChildren().add(btnMenu);
                    }
                    setGraphic(container);
                }
            }
        });

        tableQuotes.setItems(quoteList);
    }

    private void showQuoteOptionsMenu(ProcedureQuote quote, Button anchor) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.isSecretary()) {
            return;
        }

        ContextMenu menu = new ContextMenu();

        MenuItem itemQuoted = new MenuItem("💲 Marcar como Cotizada");
        itemQuoted.setOnAction(e -> advanceStatus(quote, "COTIZADA"));

        MenuItem itemPlanned = new MenuItem("📝 Marcar como Planificada...");
        itemPlanned.setOnAction(e -> promptPlanDateAndAdvance(quote));

        MenuItem itemDone = new MenuItem("✅ Marcar como Realizada");
        itemDone.setOnAction(e -> advanceStatus(quote, "REALIZADA"));

        MenuItem itemCancel = new MenuItem("🔴 Marcar como Cancelada");
        itemCancel.setOnAction(e -> advanceStatus(quote, "CANCELADA"));

        MenuItem itemDelete = new MenuItem("🗑️ Eliminar Registro");
        itemDelete.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Seguro que deseas eliminar la cotización #" + quote.getId() + "?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText("Eliminar Cotización");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    procedureDAO.delete(quote.getId());
                    loadQuotes();
                }
            });
        });

        menu.getItems().addAll(itemQuoted, itemPlanned, itemDone, itemCancel, new SeparatorMenuItem(), itemDelete);
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void promptPlanDateAndAdvance(ProcedureQuote quote) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.isSecretary()) {
            return;
        }

        Dialog<LocalDate> dialog = new Dialog<>();
        dialog.setTitle("Planificar Procedimiento #" + quote.getId());
        dialog.setHeaderText("Programar fecha de realización para:\n" + quote.getProcedureType() + " - " + quote.getPatientName());

        ButtonType btnOk = new ButtonType("Planificar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, ButtonType.CANCEL);

        DatePicker dp = new DatePicker(quote.getPlannedDate() != null ? quote.getPlannedDate().toLocalDate() : LocalDate.now().plusDays(1));
        VBox box = new VBox(10, new Label("Fecha estimada de procedimiento:"), dp);
        box.setStyle("-fx-padding: 16px;");
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == btnOk) {
                return dp.getValue();
            }
            return null;
        });

        Optional<LocalDate> result = dialog.showAndWait();
        result.ifPresent(date -> {
            LocalDateTime planned = date.atTime(9, 0);
            quote.setStatus("PLANIFICADA");
            quote.setPlannedDate(planned);
            procedureDAO.updateStatus(quote.getId(), "PLANIFICADA", planned);
            loadQuotes();
            showQuoteDetails(quote);
        });
    }

    private void advanceStatus(ProcedureQuote quote, String newStatus) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.isSecretary()) {
            return;
        }

        quote.setStatus(newStatus);
        procedureDAO.updateStatus(quote.getId(), newStatus);
        loadQuotes();
        showQuoteDetails(quote);
    }

    public void loadQuotes() {
        String query = txtTableSearch != null ? txtTableSearch.getText() : "";
        List<ProcedureQuote> all = procedureDAO.search(query, currentStatusFilter);
        quoteList.setAll(all);
        updateFilterButtonCounts();
        if (selectedQuote != null) {
            for (ProcedureQuote q : quoteList) {
                if (q.getId() == selectedQuote.getId()) {
                    tableQuotes.getSelectionModel().select(q);
                    showQuoteDetails(q);
                    break;
                }
            }
        }
    }

    private void filterQuotes() {
        String query = txtTableSearch != null ? txtTableSearch.getText() : "";
        List<ProcedureQuote> list = procedureDAO.search(query, currentStatusFilter);
        quoteList.setAll(list);
    }

    @FXML
    public void handleFilterAll() {
        setActiveFilter(btnFilterAll, "TODOS");
    }

    @FXML
    public void handleFilterQuoted() {
        setActiveFilter(btnFilterQuoted, "COTIZADA");
    }

    @FXML
    public void handleFilterPlanned() {
        setActiveFilter(btnFilterPlanned, "PLANIFICADA");
    }

    @FXML
    public void handleFilterCompleted() {
        setActiveFilter(btnFilterCompleted, "REALIZADA");
    }

    private void setActiveFilter(Button activeBtn, String status) {
        if (btnFilterAll != null) btnFilterAll.getStyleClass().remove("active");
        if (btnFilterQuoted != null) btnFilterQuoted.getStyleClass().remove("active");
        if (btnFilterPlanned != null) btnFilterPlanned.getStyleClass().remove("active");
        if (btnFilterCompleted != null) btnFilterCompleted.getStyleClass().remove("active");

        if (activeBtn != null && !activeBtn.getStyleClass().contains("active")) {
            activeBtn.getStyleClass().add("active");
        }

        this.currentStatusFilter = status;
        filterQuotes();
    }

    private void updateFilterButtonCounts() {
        List<ProcedureQuote> all = procedureDAO.findAll();
        long countAll = all.size();
        long countQuoted = all.stream().filter(q -> "COTIZADA".equalsIgnoreCase(q.getStatus())).count();
        long countPlanned = all.stream().filter(q -> "PLANIFICADA".equalsIgnoreCase(q.getStatus())).count();
        long countCompleted = all.stream().filter(q -> "REALIZADA".equalsIgnoreCase(q.getStatus())).count();

        if (btnFilterAll != null) btnFilterAll.setText("Todos (" + countAll + ")");
        if (btnFilterQuoted != null) btnFilterQuoted.setText("💲 Cotizadas (" + countQuoted + ")");
        if (btnFilterPlanned != null) btnFilterPlanned.setText("📝 Planificadas (" + countPlanned + ")");
        if (btnFilterCompleted != null) btnFilterCompleted.setText("✅ Realizadas (" + countCompleted + ")");
    }

    private void showQuoteDetails(ProcedureQuote quote) {
        this.selectedQuote = quote;
        if (quote == null) {
            if (detailPanel != null) {
                detailPanel.setVisible(false);
                detailPanel.setManaged(false);
            }
            return;
        }

        if (detailPanel != null) {
            detailPanel.setVisible(true);
            detailPanel.setManaged(true);
        }

        if (lblDetailTitle != null) lblDetailTitle.setText(quote.getProcedureType() + " (#" + quote.getId() + ")");
        if (lblDetailPatient != null) lblDetailPatient.setText(quote.getPatientName() + " | C.I: " + quote.getPatientIdCard() + (quote.getPatientOrigin() != null ? " (" + quote.getPatientOrigin() + ")" : ""));
        if (lblDetailStatus != null) lblDetailStatus.setText(quote.getStatus());

        User currentUser = SessionManager.getInstance().getCurrentUser();
        boolean isSecretary = currentUser != null && currentUser.isSecretary();

        if (lblDetailStatus != null) {
            switch (quote.getStatus().toUpperCase()) {
                case "COTIZADA":
                    lblDetailStatus.setText("💲 Cotizada");
                    lblDetailStatus.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 10px;");
                    break;
                case "PLANIFICADA":
                    lblDetailStatus.setText("📝 Planificada");
                    lblDetailStatus.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #3730a3; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 10px;");
                    break;
                case "REALIZADA":
                    lblDetailStatus.setText("✅ Realizada");
                    lblDetailStatus.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 10px;");
                    break;
                default:
                    lblDetailStatus.setText("🔴 Cancelada");
                    lblDetailStatus.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 10px;");
                    break;
            }
        }

        // Restricciones de Estado para Secretaria (Ocultar botones de modificación)
        if (isSecretary) {
            if (btnDetailPlan != null) { btnDetailPlan.setVisible(false); btnDetailPlan.setManaged(false); }
            if (btnDetailComplete != null) { btnDetailComplete.setVisible(false); btnDetailComplete.setManaged(false); }
            if (btnDetailCancel != null) { btnDetailCancel.setVisible(false); btnDetailCancel.setManaged(false); }
        } else {
            switch (quote.getStatus().toUpperCase()) {
                case "COTIZADA":
                    if (btnDetailPlan != null) { btnDetailPlan.setVisible(true); btnDetailPlan.setManaged(true); }
                    if (btnDetailComplete != null) { btnDetailComplete.setVisible(true); btnDetailComplete.setManaged(true); }
                    if (btnDetailCancel != null) { btnDetailCancel.setVisible(true); btnDetailCancel.setManaged(true); }
                    break;
                case "PLANIFICADA":
                    if (btnDetailPlan != null) { btnDetailPlan.setVisible(false); btnDetailPlan.setManaged(false); }
                    if (btnDetailComplete != null) { btnDetailComplete.setVisible(true); btnDetailComplete.setManaged(true); }
                    if (btnDetailCancel != null) { btnDetailCancel.setVisible(true); btnDetailCancel.setManaged(true); }
                    break;
                case "REALIZADA":
                    if (btnDetailPlan != null) { btnDetailPlan.setVisible(false); btnDetailPlan.setManaged(false); }
                    if (btnDetailComplete != null) { btnDetailComplete.setVisible(false); btnDetailComplete.setManaged(false); }
                    if (btnDetailCancel != null) { btnDetailCancel.setVisible(false); btnDetailCancel.setManaged(false); }
                    break;
                default:
                    if (btnDetailPlan != null) { btnDetailPlan.setVisible(true); btnDetailPlan.setManaged(true); }
                    if (btnDetailComplete != null) { btnDetailComplete.setVisible(false); btnDetailComplete.setManaged(false); }
                    if (btnDetailCancel != null) { btnDetailCancel.setVisible(false); btnDetailCancel.setManaged(false); }
                    break;
            }
        }

        if (lblDetailUsd != null) lblDetailUsd.setText(formatUsd(quote.getAmountUsd()));
        if (lblDetailVes != null) lblDetailVes.setText(formatVes(quote.getAmountVes()));
        if (lblDetailCop != null) lblDetailCop.setText(formatCop(quote.getAmountCop()));

        if (lblDetailPlannedDate != null) {
            if (quote.getPlannedDate() != null) {
                lblDetailPlannedDate.setText(quote.getPlannedDate().format(dtf));
            } else {
                lblDetailPlannedDate.setText("No programada");
            }
        }

        String notes = (quote.getDescription() != null && !quote.getDescription().isEmpty() ? "Descripción: " + quote.getDescription() + "\n" : "") +
                (quote.getNotes() != null && !quote.getNotes().isEmpty() ? "Notas: " + quote.getNotes() : "");
        if (lblDetailNotes != null) {
            lblDetailNotes.setText(notes.isEmpty() ? "Sin observaciones adicionales." : notes);
        }
    }

    @FXML
    public void handleDetailPlan() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.isSecretary()) return;

        if (selectedQuote != null) {
            promptPlanDateAndAdvance(selectedQuote);
        }
    }

    @FXML
    public void handleDetailComplete() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.isSecretary()) return;

        if (selectedQuote != null) {
            advanceStatus(selectedQuote, "REALIZADA");
        }
    }

    @FXML
    public void handleDetailCancel() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.isSecretary()) return;

        if (selectedQuote != null) {
            advanceStatus(selectedQuote, "CANCELADA");
        }
    }
}
