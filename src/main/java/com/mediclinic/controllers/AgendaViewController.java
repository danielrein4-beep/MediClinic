package com.mediclinic.controllers;

import com.mediclinic.dao.AppointmentDAO;
import com.mediclinic.dao.BlockedDateDAO;
import com.mediclinic.dao.PatientDAO;
import com.mediclinic.models.Appointment;
import com.mediclinic.models.BlockedDate;
import com.mediclinic.models.Patient;
import com.mediclinic.models.User;
import com.mediclinic.services.SessionManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

public class AgendaViewController {

    @FXML
    private Label lblMonthYearTitle;

    @FXML
    private GridPane gridWeekdays;

    @FXML
    private GridPane gridCalendarDays;

    @FXML
    private Label lblSelectedDateHeader;

    @FXML
    private Label lblSelectedDateSub;

    @FXML
    private Label lblDayAppointmentCount;

    @FXML
    private VBox boxBlockedBanner;

    @FXML
    private Label lblBlockedReason;

    @FXML
    private HBox boxBlockActionRow;

    @FXML
    private Button btnBlockDate;

    @FXML
    private VBox containerDayAppointments;

    @FXML
    private VBox boxNoAppointments;

    @FXML
    private VBox cardQuickAdd;

    // Quick Add Form
    @FXML
    private TextField txtCedula;

    @FXML
    private Label lblPatientFoundHint;

    @FXML
    private TextField txtFirstName;

    @FXML
    private TextField txtLastName;

    @FXML
    private TextField txtPhone;

    @FXML
    private ComboBox<String> cbAppointmentTime;

    @FXML
    private TextField txtReason;

    @FXML
    private Label lblFormFeedback;

    private static AgendaViewController instance;

    public static AgendaViewController getInstance() {
        return instance;
    }

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final PatientDAO patientDAO = new PatientDAO();
    private final BlockedDateDAO blockedDateDAO = new BlockedDateDAO();

    private YearMonth currentYearMonth = YearMonth.now();
    private LocalDate selectedDate = LocalDate.now();
    private Integer foundPatientId = null;

    private Timeline autoRefreshTimeline;
    private final Locale esLocale = new Locale("es", "ES");
    private final DateTimeFormatter headerDateFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", esLocale);

    @FXML
    public void initialize() {
        instance = this;

        // Configurar encabezados de días de la semana
        setupWeekdays();

        // Configurar horas disponibles para agendamiento
        setupTimeComboBox();

        // Listener de búsqueda automática de cédula al escribir
        txtCedula.textProperty().addListener((obs, oldVal, newVal) -> {
            autoSearchPatient(newVal);
        });

        // Renderizar calendario inicial y citas del día
        renderCalendar();
        loadDayAppointments();

        // Iniciar sincronización reactiva periódica
        startAutoRefresh();
    }

    private void setupWeekdays() {
        if (gridWeekdays == null) return;
        gridWeekdays.getChildren().clear();
        String[] days = {"LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM"};
        for (int i = 0; i < days.length; i++) {
            Label lbl = new Label(days[i]);
            lbl.setStyle("-fx-font-weight: 800; -fx-font-size: 11px; -fx-text-fill: #64748b; -fx-padding: 6px 0;");
            lbl.setAlignment(Pos.CENTER);
            lbl.setMaxWidth(Double.MAX_VALUE);
            gridWeekdays.add(lbl, i, 0);
        }
    }

    private void setupTimeComboBox() {
        if (cbAppointmentTime == null) return;
        List<String> hours = new ArrayList<>();
        String[] amHours = {"08:00 AM", "08:30 AM", "09:00 AM", "09:30 AM", "10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM"};
        String[] pmHours = {"12:00 PM", "12:30 PM", "01:00 PM", "01:30 PM", "02:00 PM", "02:30 PM", "03:00 PM", "03:30 PM", "04:00 PM", "04:30 PM", "05:00 PM", "05:30 PM", "06:00 PM"};
        hours.addAll(Arrays.asList(amHours));
        hours.addAll(Arrays.asList(pmHours));
        cbAppointmentTime.setItems(FXCollections.observableArrayList(hours));
        cbAppointmentTime.setValue("09:00 AM");
    }

    public void renderCalendar() {
        if (gridCalendarDays == null) return;
        gridCalendarDays.getChildren().clear();

        // Título del Mes y Año
        String monthName = currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, esLocale);
        monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
        lblMonthYearTitle.setText(monthName + " " + currentYearMonth.getYear());

        // Obtener citas del mes agrupadas por fecha
        List<Appointment> monthAppointments = appointmentDAO.findByMonthYear(currentYearMonth.getMonthValue(), currentYearMonth.getYear());
        Map<LocalDate, Integer> appointmentCounts = new HashMap<>();
        for (Appointment a : monthAppointments) {
            if (!"CANCELADA".equalsIgnoreCase(a.getStatus())) {
                LocalDate d = a.getAppointmentDate();
                appointmentCounts.put(d, appointmentCounts.getOrDefault(d, 0) + 1);
            }
        }

        // Obtener fechas bloqueadas del mes
        Map<LocalDate, String> blockedDates = blockedDateDAO.findByMonthYear(currentYearMonth.getMonthValue(), currentYearMonth.getYear());

        LocalDate firstDayOfMonth = currentYearMonth.atDay(1);
        int dayOfWeekOffset = firstDayOfMonth.getDayOfWeek().getValue() - 1; // 0 = Lunes, 6 = Domingo
        int daysInMonth = currentYearMonth.lengthOfMonth();

        int row = 0;
        int col = dayOfWeekOffset;

        // Días del mes anterior (Relleno sutil)
        if (dayOfWeekOffset > 0) {
            YearMonth prevMonth = currentYearMonth.minusMonths(1);
            int prevMonthDays = prevMonth.lengthOfMonth();
            for (int i = 0; i < dayOfWeekOffset; i++) {
                int dayNum = prevMonthDays - dayOfWeekOffset + 1 + i;
                LocalDate prevDate = prevMonth.atDay(dayNum);
                VBox cell = createDayCell(prevDate, dayNum, false, 0, null);
                gridCalendarDays.add(cell, i, 0);
            }
        }

        // Días del mes actual
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentYearMonth.atDay(day);
            int count = appointmentCounts.getOrDefault(date, 0);
            String blockedReason = blockedDates.get(date);
            VBox cell = createDayCell(date, day, true, count, blockedReason);
            gridCalendarDays.add(cell, col, row);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }

        // Días del mes siguiente (Relleno sutil)
        if (row < 6) {
            YearMonth nextMonth = currentYearMonth.plusMonths(1);
            int nextDay = 1;
            while (row < 6) {
                while (col <= 6) {
                    LocalDate nextDate = nextMonth.atDay(nextDay);
                    VBox cell = createDayCell(nextDate, nextDay, false, 0, null);
                    gridCalendarDays.add(cell, col, row);
                    nextDay++;
                    col++;
                }
                col = 0;
                row++;
            }
        }
    }

    private VBox createDayCell(LocalDate date, int dayNumber, boolean isCurrentMonth, int appointmentCount, String blockedReason) {
        VBox cell = new VBox(3);
        cell.setAlignment(Pos.TOP_LEFT);
        cell.setStyle("-fx-padding: 6px; -fx-background-radius: 10px; -fx-cursor: hand;");
        GridPane.setHgrow(cell, Priority.ALWAYS);
        GridPane.setVgrow(cell, Priority.ALWAYS);

        boolean isToday = date.equals(LocalDate.now());
        boolean isSelected = date.equals(selectedDate);
        boolean isBlocked = blockedReason != null;

        // Estilos base y Glassmorphism sutil
        if (isBlocked) {
            cell.setStyle(cell.getStyle() + "-fx-background-color: rgba(241, 245, 249, 0.95); -fx-border-color: #fca5a5; -fx-border-width: 1.5px; -fx-border-radius: 10px;");
        } else if (isSelected) {
            cell.setStyle(cell.getStyle() + "-fx-background-color: #e0f2fe; -fx-border-color: #0284c7; -fx-border-width: 2px; -fx-border-radius: 10px;");
        } else if (isToday) {
            cell.setStyle(cell.getStyle() + "-fx-background-color: #f0fdf4; -fx-border-color: #10b981; -fx-border-width: 1.5px; -fx-border-radius: 10px;");
        } else if (isCurrentMonth) {
            cell.setStyle(cell.getStyle() + "-fx-background-color: #ffffff; -fx-border-color: #e2e8f0; -fx-border-width: 1px; -fx-border-radius: 10px;");
        } else {
            cell.setStyle(cell.getStyle() + "-fx-background-color: #f8fafc; -fx-border-color: #f1f5f9; -fx-border-width: 1px; -fx-border-radius: 10px;");
        }

        // Número del día
        Label lblDay = new Label(String.valueOf(dayNumber));
        if (isBlocked) {
            lblDay.setStyle("-fx-font-weight: 800; -fx-font-size: 12px; -fx-text-fill: #e11d48;");
        } else if (isSelected) {
            lblDay.setStyle("-fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: #0369a1;");
        } else if (isToday) {
            lblDay.setStyle("-fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: #059669;");
        } else if (isCurrentMonth) {
            lblDay.setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-text-fill: #1e293b;");
        } else {
            lblDay.setStyle("-fx-font-weight: 500; -fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        }

        cell.getChildren().add(lblDay);

        // Indicador de Bloqueo
        if (isBlocked && isCurrentMonth) {
            Label badgeBlocked = new Label("🔒 Bloqueado");
            badgeBlocked.setMaxWidth(Double.MAX_VALUE);
            badgeBlocked.setAlignment(Pos.CENTER);
            badgeBlocked.setStyle("-fx-background-color: #ffe4e6; -fx-text-fill: #be123c; -fx-font-size: 8.5px; -fx-font-weight: bold; -fx-padding: 2px 4px; -fx-background-radius: 6px; -fx-border-color: #fecdd3; -fx-border-radius: 6px;");
            cell.getChildren().add(badgeBlocked);
        } else if (isCurrentMonth && appointmentCount > 0) {
            Label badge = new Label("📅 " + appointmentCount + (appointmentCount == 1 ? " cita" : " citas"));
            badge.setMaxWidth(Double.MAX_VALUE);
            badge.setAlignment(Pos.CENTER);
            badge.setStyle("-fx-background-color: #0284c7; -fx-text-fill: #ffffff; -fx-font-size: 8.5px; -fx-font-weight: bold; -fx-padding: 2px 4px; -fx-background-radius: 6px;");
            cell.getChildren().add(badge);
        }

        // Evento al hacer clic en un día
        cell.setOnMouseClicked(event -> {
            this.selectedDate = date;
            if (!date.getMonth().equals(currentYearMonth.getMonth())) {
                currentYearMonth = YearMonth.from(date);
            }
            renderCalendar();
            loadDayAppointments();
        });

        return cell;
    }

    public void loadDayAppointments() {
        if (lblSelectedDateHeader == null) return;

        String formattedDate = selectedDate.format(headerDateFormatter);
        formattedDate = formattedDate.substring(0, 1).toUpperCase() + formattedDate.substring(1);
        lblSelectedDateHeader.setText(formattedDate);
        lblSelectedDateSub.setText("Gestión de citas para el " + selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        // Verificar si la fecha está bloqueada
        BlockedDate blocked = blockedDateDAO.findByDate(selectedDate);
        if (blocked != null) {
            if (boxBlockedBanner != null) {
                boxBlockedBanner.setVisible(true);
                boxBlockedBanner.setManaged(true);
            }
            if (lblBlockedReason != null) {
                lblBlockedReason.setText("Motivo: " + blocked.getReason() + " (Por: " + blocked.getBlockedBy() + ")");
            }
            if (boxBlockActionRow != null) {
                boxBlockActionRow.setVisible(false);
                boxBlockActionRow.setManaged(false);
            }
            if (cardQuickAdd != null) {
                cardQuickAdd.setDisable(true);
            }
            if (lblFormFeedback != null) {
                lblFormFeedback.setText("🔒 La fecha seleccionada está bloqueada. No se pueden agendar citas.");
                lblFormFeedback.setStyle("-fx-text-fill: #be123c;");
            }
        } else {
            if (boxBlockedBanner != null) {
                boxBlockedBanner.setVisible(false);
                boxBlockedBanner.setManaged(false);
            }
            if (boxBlockActionRow != null) {
                boxBlockActionRow.setVisible(true);
                boxBlockActionRow.setManaged(true);
            }
            if (cardQuickAdd != null) {
                cardQuickAdd.setDisable(false);
            }
            if (lblFormFeedback != null) {
                lblFormFeedback.setText("");
            }
        }

        List<Appointment> dayList = appointmentDAO.findByDate(selectedDate);
        containerDayAppointments.getChildren().clear();

        long activeCount = dayList.stream().filter(a -> !"CANCELADA".equalsIgnoreCase(a.getStatus())).count();
        lblDayAppointmentCount.setText(activeCount + (activeCount == 1 ? " cita" : " citas"));

        if (dayList.isEmpty()) {
            boxNoAppointments.setVisible(true);
            boxNoAppointments.setManaged(true);
        } else {
            boxNoAppointments.setVisible(false);
            boxNoAppointments.setManaged(false);

            for (Appointment app : dayList) {
                VBox card = createAppointmentCard(app);
                containerDayAppointments.getChildren().add(card);
            }
        }
    }

    private VBox createAppointmentCard(Appointment app) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-width: 1px 1px 1px 4px; -fx-border-color: #0284c7; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10px 12px;");

        // Fila 1: Hora, Nombre y Botón Eliminar
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label lblTime = new Label("⏰ " + app.getAppointmentTime());
        lblTime.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3px 8px; -fx-background-radius: 6px;");

        Label lblName = new Label(app.getFullName());
        lblName.setStyle("-fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: #0f172a;");
        HBox.setHgrow(lblName, Priority.ALWAYS);

        Button btnDelete = new Button("🗑️");
        btnDelete.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-size: 11px; -fx-padding: 4px 8px; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnDelete.setTooltip(new Tooltip("Cancelar y eliminar cita"));
        btnDelete.setOnAction(e -> promptDeleteAppointment(app));

        topRow.getChildren().addAll(lblTime, lblName, btnDelete);

        // Fila 2: Cédula y Teléfono
        HBox infoRow = new HBox(12);
        infoRow.setAlignment(Pos.CENTER_LEFT);

        Label lblId = new Label("C.I: " + app.getIdCard());
        lblId.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #475569;");

        Label lblPhone = new Label("📞 " + (app.getPhone() != null && !app.getPhone().isEmpty() ? app.getPhone() : "Sin teléfono"));
        lblPhone.setStyle("-fx-font-size: 11px; -fx-text-fill: #166534; -fx-font-weight: 600;");

        infoRow.getChildren().addAll(lblId, lblPhone);

        // Fila 3: Motivo
        if (app.getReason() != null && !app.getReason().trim().isEmpty()) {
            Label lblReason = new Label("Motivo: " + app.getReason());
            lblReason.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-style: italic;");
            card.getChildren().addAll(topRow, infoRow, lblReason);
        } else {
            card.getChildren().addAll(topRow, infoRow);
        }

        return card;
    }

    @FXML
    public void handlePromptBlockDate() {
        TextInputDialog dialog = new TextInputDialog("Vacaciones / Congreso Médico");
        dialog.setTitle("Bloquear Fecha en Agenda");
        dialog.setHeaderText("Bloquear el día: " + selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dialog.setContentText("Motivo del Bloqueo:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(reason -> {
            String trimmed = reason.trim();
            if (trimmed.isEmpty()) trimmed = "No disponible / Vacaciones";

            User currentUser = SessionManager.getInstance().getCurrentUser();
            String blockerName = currentUser != null ? currentUser.getFullName() : "Doctor";

            boolean ok = blockedDateDAO.insert(selectedDate, trimmed, blockerName);
            if (ok) {
                renderCalendar();
                loadDayAppointments();
                notifySync();
            } else {
                Alert err = new Alert(Alert.AlertType.ERROR, "No se pudo bloquear la fecha.");
                err.showAndWait();
            }
        });
    }

    @FXML
    public void handleUnblockDate() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Desbloquear Fecha");
        alert.setHeaderText("¿Desbloquear la fecha " + selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "?");
        alert.setContentText("Al desbloquear esta fecha, se habilitará nuevamente el agendamiento de citas médicas.");

        ButtonType btnYes = new ButtonType("🔓 Sí, Desbloquear", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnYes, btnNo);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == btnYes) {
            boolean ok = blockedDateDAO.delete(selectedDate);
            if (ok) {
                renderCalendar();
                loadDayAppointments();
                notifySync();
            } else {
                Alert err = new Alert(Alert.AlertType.ERROR, "No se pudo desbloquear la fecha.");
                err.showAndWait();
            }
        }
    }

    private void autoSearchPatient(String query) {
        if (query == null || query.trim().length() < 3) {
            lblPatientFoundHint.setVisible(false);
            lblPatientFoundHint.setManaged(false);
            foundPatientId = null;
            return;
        }

        String clean = query.trim();
        Patient p = patientDAO.findByIdCard(clean);
        if (p == null && !clean.toUpperCase().startsWith("V-") && !clean.toUpperCase().startsWith("E-")) {
            p = patientDAO.findByIdCard("V-" + clean);
        }

        if (p != null) {
            this.foundPatientId = p.getId();
            txtFirstName.setText(p.getFirstName());
            txtLastName.setText(p.getLastName());
            if (p.getPhone() != null && !p.getPhone().isEmpty()) {
                txtPhone.setText(p.getPhone());
            }
            lblPatientFoundHint.setText("✅ Paciente encontrado: " + p.getFullName() + " (Exp: " + p.getMedicalRecordNumber() + ")");
            lblPatientFoundHint.setVisible(true);
            lblPatientFoundHint.setManaged(true);
        } else {
            this.foundPatientId = null;
            lblPatientFoundHint.setVisible(false);
            lblPatientFoundHint.setManaged(false);
        }
    }

    @FXML
    public void handleSearchPatientByIdCard() {
        autoSearchPatient(txtCedula.getText());
    }

    @FXML
    public void handleSaveAppointment() {
        lblFormFeedback.setText("");

        if (blockedDateDAO.isDateBlocked(selectedDate)) {
            lblFormFeedback.setText("🔒 La fecha está bloqueada. No se pueden agendar citas.");
            lblFormFeedback.setStyle("-fx-text-fill: #ef4444;");
            return;
        }

        String cedula = txtCedula.getText().trim();
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String phone = txtPhone.getText().trim();
        String time = cbAppointmentTime.getValue();
        String reason = txtReason.getText().trim();

        if (cedula.isEmpty()) {
            lblFormFeedback.setText("⚠️ Ingrese la cédula de identidad del paciente.");
            lblFormFeedback.setStyle("-fx-text-fill: #ef4444;");
            txtCedula.requestFocus();
            return;
        }

        if (firstName.isEmpty() || lastName.isEmpty()) {
            lblFormFeedback.setText("⚠️ Ingrese los nombres y apellidos del paciente.");
            lblFormFeedback.setStyle("-fx-text-fill: #ef4444;");
            return;
        }

        if (phone.isEmpty()) {
            lblFormFeedback.setText("⚠️ Ingrese el número de teléfono de contacto.");
            lblFormFeedback.setStyle("-fx-text-fill: #ef4444;");
            txtPhone.requestFocus();
            return;
        }

        if (time == null || time.isEmpty()) {
            time = "09:00 AM";
        }

        Appointment appointment = new Appointment(
                foundPatientId,
                cedula,
                firstName,
                lastName,
                phone,
                selectedDate,
                time,
                reason,
                ""
        );

        boolean ok = appointmentDAO.insert(appointment);
        if (ok) {
            lblFormFeedback.setText("✅ Cita agendada para el " + selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " a las " + time);
            lblFormFeedback.setStyle("-fx-text-fill: #10b981;");
            handleClearForm();
            renderCalendar();
            loadDayAppointments();
            notifySync();
        } else {
            lblFormFeedback.setText("❌ Error al guardar la cita en la base de datos.");
            lblFormFeedback.setStyle("-fx-text-fill: #ef4444;");
        }
    }

    public void promptDeleteAppointment(Appointment app) {
        if (app == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancelar Cita Médica");
        alert.setHeaderText("Cancelar cita de: " + app.getFullName() + "\nFecha: " + app.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " - Hora: " + app.getAppointmentTime());
        alert.setContentText("¿Está seguro de que desea cancelar y eliminar esta cita de la agenda? Esta acción no se puede deshacer.");

        ButtonType btnYes = new ButtonType("Sí, Eliminar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnYes, btnNo);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == btnYes) {
            boolean ok = appointmentDAO.delete(app.getId());
            if (ok) {
                renderCalendar();
                loadDayAppointments();
                notifySync();
            } else {
                Alert err = new Alert(Alert.AlertType.ERROR, "No se pudo eliminar la cita.");
                err.showAndWait();
            }
        }
    }

    @FXML
    public void handleClearForm() {
        txtCedula.clear();
        txtFirstName.clear();
        txtLastName.clear();
        txtPhone.clear();
        txtReason.clear();
        foundPatientId = null;
        lblPatientFoundHint.setVisible(false);
        lblPatientFoundHint.setManaged(false);
        lblFormFeedback.setText("");
    }

    @FXML
    public void handlePrevMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
        renderCalendar();
    }

    @FXML
    public void handleNextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
        renderCalendar();
    }

    @FXML
    public void handleToday() {
        currentYearMonth = YearMonth.now();
        selectedDate = LocalDate.now();
        renderCalendar();
        loadDayAppointments();
    }

    public void refreshLive() {
        Platform.runLater(() -> {
            renderCalendar();
            loadDayAppointments();
        });
    }

    private void notifySync() {
        if (DoctorDashboardController.getInstance() != null) {
            DoctorDashboardController.getInstance().refreshMetrics();
        }
        if (SecretaryDashboardController.getInstance() != null) {
            SecretaryDashboardController.getInstance().refreshMetrics();
        }
    }

    private void startAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            renderCalendar();
            loadDayAppointments();
        }));
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }
}
