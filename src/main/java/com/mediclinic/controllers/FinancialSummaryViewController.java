package com.mediclinic.controllers;

import com.mediclinic.dao.DailyPaymentDAO;
import com.mediclinic.dao.FinancialClosingDAO;
import com.mediclinic.dao.WaitingRoomDAO;
import com.mediclinic.models.DailyPayment;
import com.mediclinic.models.FinancialClosing;
import com.mediclinic.models.User;
import com.mediclinic.models.WaitingRoomEntry;
import com.mediclinic.services.PdfReportService;
import com.mediclinic.services.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FinancialSummaryViewController {

    @FXML
    private ComboBox<String> cbTimeFilter;

    @FXML
    private Label lblTotalUsd;

    @FXML
    private Label lblUsdSub;

    @FXML
    private Label lblTotalVes;

    @FXML
    private Label lblVesSub;

    @FXML
    private Label lblTotalCop;

    @FXML
    private Label lblCopSub;

    @FXML
    private Label lblTotalClosings;

    @FXML
    private Label lblTotalPatients;

    @FXML
    private TextField txtSearch;

    @FXML
    private TableView<FinancialClosing> tableClosings;

    @FXML
    private TableColumn<FinancialClosing, String> colDate;

    @FXML
    private TableColumn<FinancialClosing, String> colTime;

    @FXML
    private TableColumn<FinancialClosing, String> colPatients;

    @FXML
    private TableColumn<FinancialClosing, String> colUsd;

    @FXML
    private TableColumn<FinancialClosing, String> colVes;

    @FXML
    private TableColumn<FinancialClosing, String> colCop;

    @FXML
    private TableColumn<FinancialClosing, String> colClosedBy;

    @FXML
    private TableColumn<FinancialClosing, Void> colActions;

    private final FinancialClosingDAO financialClosingDAO = new FinancialClosingDAO();
    private final DailyPaymentDAO dailyPaymentDAO = new DailyPaymentDAO();
    private final WaitingRoomDAO waitingRoomDAO = new WaitingRoomDAO();

    private final ObservableList<FinancialClosing> masterList = FXCollections.observableArrayList();
    private FilteredList<FinancialClosing> filteredList;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        setupTimeFilter();
        setupTable();
        setupSearchFilter();
        loadData();
    }

    private void setupTimeFilter() {
        if (cbTimeFilter != null) {
            cbTimeFilter.getItems().setAll("Total Histórico", "Último Día", "Última Semana", "Último Mes");
            cbTimeFilter.setValue("Total Histórico");
            cbTimeFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    loadData();
                }
            });
            cbTimeFilter.setOnAction(e -> loadData());
        }
    }

    private void setupTable() {
        colDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getClosingDate() != null) {
                return new SimpleStringProperty(cellData.getValue().getClosingDate().format(dateFormatter));
            }
            return new SimpleStringProperty("--/--/----");
        });

        colTime.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getClosingTime()));

        colPatients.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTotalPatients() + " pac."));

        colUsd.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedUsd()));

        colVes.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedVes()));

        colCop.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedCop()));

        colClosedBy.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getClosedBy()));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnPdf = new Button("📄 Ver PDF");
            private final Button btnDelete = new Button("🗑️");
            private final HBox container = new HBox(6, btnPdf, btnDelete);

            {
                container.setAlignment(Pos.CENTER);
                btnPdf.setStyle("-fx-background-color: linear-gradient(to bottom right, #0284c7, #0369a1); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4px 10px; -fx-background-radius: 6px; -fx-cursor: hand;");
                btnDelete.setStyle("-fx-background-color: linear-gradient(to bottom right, #e11d48, #be123c); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4px 8px; -fx-background-radius: 6px; -fx-cursor: hand;");
                btnDelete.setTooltip(new Tooltip("Eliminar este cierre de caja"));

                btnPdf.setOnAction(e -> {
                    FinancialClosing item = getTableView().getItems().get(getIndex());
                    handleGenerateClosingPdf(item);
                });

                btnDelete.setOnAction(e -> {
                    FinancialClosing item = getTableView().getItems().get(getIndex());
                    handleDeleteClosing(item);
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

        filteredList = new FilteredList<>(masterList, p -> true);
        SortedList<FinancialClosing> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(tableClosings.comparatorProperty());
        tableClosings.setItems(sortedList);
    }

    private void setupSearchFilter() {
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(closing -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    return true;
                }
                String lower = newVal.toLowerCase().trim();
                String dateStr = closing.getClosingDate() != null ? closing.getClosingDate().format(dateFormatter) : "";
                String closedBy = closing.getClosedBy() != null ? closing.getClosedBy().toLowerCase() : "";
                String notes = closing.getNotes() != null ? closing.getNotes().toLowerCase() : "";

                return dateStr.contains(lower) || closedBy.contains(lower) || notes.contains(lower);
            });
        });
    }

    public void loadData() {
        String filter = (cbTimeFilter != null && cbTimeFilter.getValue() != null) ? cbTimeFilter.getValue() : "Total Histórico";

        // 1. Obtener totales calculados en SQLite con función DATE()
        FinancialClosingDAO.FinancialSummaryTotals totals = financialClosingDAO.getTotalsByTimeFilter(filter);

        // 2. Obtener lista de cierres filtrados por rango de tiempo
        List<FinancialClosing> list = financialClosingDAO.findByTimeFilter(filter);
        masterList.setAll(list);

        // 3. Actualizar KPIs visuales
        lblTotalUsd.setText(String.format("$%.2f USD", totals.getTotalUsd()));
        lblTotalVes.setText(String.format("Bs. %.2f VES", totals.getTotalVes()));
        lblTotalCop.setText(String.format("$%,.0f COP", totals.getTotalCop()));
        lblTotalClosings.setText(totals.getTotalClosings() + (totals.getTotalClosings() == 1 ? " Cierre" : " Cierres"));
        lblTotalPatients.setText(totals.getTotalPatients() + " Pacientes atendidos");

        // 4. Actualizar subtítulos dinámicos de los KPIs
        String subDesc;
        switch (filter) {
            case "Último Día":
                subDesc = "Cierres del último día";
                break;
            case "Última Semana":
                subDesc = "Últimos 7 días";
                break;
            case "Último Mes":
                subDesc = "Últimos 30 días";
                break;
            case "Total Histórico":
            default:
                subDesc = "Consolidado total";
                break;
        }

        if (lblUsdSub != null) lblUsdSub.setText("Dólares (" + subDesc + ")");
        if (lblVesSub != null) lblVesSub.setText("Bolívares (" + subDesc + ")");
        if (lblCopSub != null) lblCopSub.setText("Pesos (" + subDesc + ")");
    }

    @FXML
    public void handleRefresh() {
        loadData();
    }

    private void handleGenerateClosingPdf(FinancialClosing closing) {
        if (closing == null || closing.getClosingDate() == null) return;

        try {
            List<DailyPayment> payments = dailyPaymentDAO.findByDate(closing.getClosingDate());
            List<WaitingRoomEntry> entries = waitingRoomDAO.findAllToday(); // fallback
            User user = SessionManager.getInstance().getCurrentUser();

            File pdfFile = PdfReportService.generateDailyCashClosingPdf(payments, entries, user);
            PdfReportService.openPdfFile(pdfFile);

            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Informe de Cierre");
            successAlert.setHeaderText("PDF Generado para la fecha: " + closing.getClosingDate().format(dateFormatter));
            successAlert.setContentText("El informe financiero se ha generado y abierto exitosamente:\n" + pdfFile.getAbsolutePath());
            successAlert.showAndWait();

        } catch (Exception e) {
            System.err.println("Error al visualizar PDF de cierre: " + e.getMessage());
            e.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR, "No se pudo generar el informe PDF: " + e.getMessage());
            err.showAndWait();
        }
    }

    private void handleDeleteClosing(FinancialClosing closing) {
        if (closing == null) return;

        String dateStr = closing.getClosingDate() != null ? closing.getClosingDate().format(dateFormatter) : "N/A";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Eliminar Cierre de Caja");
        alert.setHeaderText("¿Eliminar Cierre del " + dateStr + " (" + closing.getClosingTime() + ")?");
        alert.setContentText("Total Recaudado: " + closing.getFormattedUsd() + " | " + closing.getFormattedVes() + " | " + closing.getFormattedCop() +
                "\nResponsable: " + closing.getClosedBy() +
                "\n\n⚠️ ADVERTENCIA: Esta acción es irreversible y eliminará este registro de auditoría del sistema.");

        ButtonType btnYes = new ButtonType("🗑️ Sí, Eliminar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnYes, btnNo);

        java.util.Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == btnYes) {
            boolean deleted = financialClosingDAO.delete(closing.getId());
            if (deleted) {
                loadData();
                Alert success = new Alert(Alert.AlertType.INFORMATION, "El registro de cierre de caja ha sido eliminado correctamente.");
                success.setHeaderText("Cierre Eliminado");
                success.showAndWait();
            } else {
                Alert err = new Alert(Alert.AlertType.ERROR, "No se pudo eliminar el registro de la base de datos.");
                err.showAndWait();
            }
        }
    }
}
