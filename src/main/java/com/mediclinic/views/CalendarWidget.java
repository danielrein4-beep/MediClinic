package com.mediclinic.views;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.skin.DatePickerSkin;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Modern interactive calendar widget to embed directly into dashboard sidebars or side panels.
 */
public class CalendarWidget extends VBox {

    private final DatePicker datePicker;
    private final Label lblSelectedDate;

    public CalendarWidget() {
        setAlignment(Pos.TOP_CENTER);
        setSpacing(8.0);
        getStyleClass().add("calendar-card");

        // Header with today's date formatted in Spanish
        LocalDate today = LocalDate.now();
        DateTimeFormatter dayMonthFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", new Locale("es", "ES"));
        String dateString = today.format(dayMonthFormatter);
        dateString = dateString.substring(0, 1).toUpperCase() + dateString.substring(1);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(6.0);

        Label lblTitle = new Label("📅 Calendario");
        lblTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #0ea5e9; -fx-text-transform: uppercase;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblYear = new Label(String.valueOf(today.getYear()));
        lblYear.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");

        header.getChildren().addAll(lblTitle, spacer, lblYear);

        lblSelectedDate = new Label(dateString);
        lblSelectedDate.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #475569; -fx-padding: 0 0 4px 0;");

        // JavaFX DatePicker popup content as embedded calendar node
        datePicker = new DatePicker(today);
        DatePickerSkin skin = new DatePickerSkin(datePicker);
        Node popupContent = skin.getPopupContent();
        popupContent.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        // Event listener when clicking dates
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String formatted = newVal.format(dayMonthFormatter);
                formatted = formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
                lblSelectedDate.setText(formatted);
            }
        });

        getChildren().addAll(header, lblSelectedDate, popupContent);
    }

    public DatePicker getDatePicker() {
        return datePicker;
    }
}
