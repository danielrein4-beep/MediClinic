package com.mediclinic.controllers;

import com.mediclinic.dao.ConfigDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class ExchangeRateDialog {

    public static boolean showAndUpdateRates(ConfigDAO configDAO, Runnable onRatesUpdated) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Actualizar Tasas de Cambio del Día");
        dialog.setHeaderText(null);

        // Styling
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(ExchangeRateDialog.class.getResource("/css/style.css").toExternalForm());
        dialogPane.setStyle("-fx-background-color: #ffffff; -fx-padding: 10px;");

        // Current values
        double currentVes = configDAO.getDoubleValue("TASA_USD_VES", 38.50);
        double currentCop = configDAO.getDoubleValue("TASA_USD_COP", 4100.0);

        // Header Title
        Label lblTitle = new Label("💵 Actualización de Tasas Cambiarias");
        lblTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #0ea5e9;");

        Label lblSubtitle = new Label("Ingresa las tasas de cambio de referencia del día para la conversión multidivisa (USD / VES / COP).");
        lblSubtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        lblSubtitle.setWrapText(true);

        // Form Fields
        Label lblVes = new Label("Tasa Dólar a Bolívares (VES / USD):");
        lblVes.getStyleClass().add("field-label");
        TextField txtVes = new TextField(String.format("%.2f", currentVes).replace(",", "."));
        txtVes.setPromptText("Ej. 38.50");

        Label lblCop = new Label("Tasa Dólar a Pesos Colombianos (COP / USD):");
        lblCop.getStyleClass().add("field-label");
        TextField txtCop = new TextField(String.format("%.2f", currentCop).replace(",", "."));
        txtCop.setPromptText("Ej. 4100.00");

        Label lblError = new Label();
        lblError.getStyleClass().add("error-message");
        lblError.setVisible(false);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(14, 0, 10, 0));

        grid.add(lblVes, 0, 0);
        grid.add(txtVes, 0, 1);
        grid.add(lblCop, 0, 2);
        grid.add(txtCop, 0, 3);
        grid.add(lblError, 0, 4);

        VBox content = new VBox(10, lblTitle, lblSubtitle, grid);
        content.setPrefWidth(420);
        dialogPane.setContent(content);

        // Buttons
        ButtonType btnSaveType = new ButtonType("Guardar Tasas", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelType = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(btnSaveType, btnCancelType);

        Button btnSave = (Button) dialogPane.lookupButton(btnSaveType);
        btnSave.getStyleClass().add("btn-primary");
        Button btnCancel = (Button) dialogPane.lookupButton(btnCancelType);
        btnCancel.getStyleClass().add("btn-secondary");

        // Conversion logic on Save
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSaveType) {
                try {
                    String vesStr = txtVes.getText().trim().replace(",", ".");
                    String copStr = txtCop.getText().trim().replace(",", ".");

                    double newVes = Double.parseDouble(vesStr);
                    double newCop = Double.parseDouble(copStr);

                    if (newVes <= 0 || newCop <= 0) {
                        lblError.setText("Las tasas deben ser valores numéricos positivos mayores a 0.");
                        lblError.setVisible(true);
                        return false;
                    }

                    configDAO.setSetting("TASA_USD_VES", String.valueOf(newVes), "Tasa de cambio Dólar a Bolívares");
                    configDAO.setSetting("TASA_USD_COP", String.valueOf(newCop), "Tasa de cambio Dólar a Pesos Colombianos");

                    if (onRatesUpdated != null) {
                        onRatesUpdated.run();
                    }
                    return true;
                } catch (NumberFormatException e) {
                    lblError.setText("Formato numérico inválido. Ingrese valores válidos (ej. 38.50).");
                    lblError.setVisible(true);
                    return false;
                }
            }
            return false;
        });

        Optional<Boolean> result = dialog.showAndWait();
        return result.orElse(false);
    }
}
