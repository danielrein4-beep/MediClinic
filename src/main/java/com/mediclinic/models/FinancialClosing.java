package com.mediclinic.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FinancialClosing {
    private int id;
    private LocalDate closingDate;
    private String closingTime;
    private double totalUsd;
    private double totalVes;
    private double totalCop;
    private int totalPatients;
    private String closedBy;
    private String notes;
    private LocalDateTime createdAt;

    public FinancialClosing() {}

    public FinancialClosing(int id, LocalDate closingDate, String closingTime, double totalUsd,
                            double totalVes, double totalCop, int totalPatients,
                            String closedBy, String notes, LocalDateTime createdAt) {
        this.id = id;
        this.closingDate = closingDate != null ? closingDate : LocalDate.now();
        this.closingTime = closingTime;
        this.totalUsd = totalUsd;
        this.totalVes = totalVes;
        this.totalCop = totalCop;
        this.totalPatients = totalPatients;
        this.closedBy = closedBy != null ? closedBy : "Secretaria";
        this.notes = notes;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public FinancialClosing(LocalDate closingDate, String closingTime, double totalUsd,
                            double totalVes, double totalCop, int totalPatients,
                            String closedBy, String notes) {
        this(-1, closingDate, closingTime, totalUsd, totalVes, totalCop, totalPatients, closedBy, notes, LocalDateTime.now());
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getClosingDate() { return closingDate; }
    public void setClosingDate(LocalDate closingDate) { this.closingDate = closingDate; }

    public String getClosingTime() { return closingTime; }
    public void setClosingTime(String closingTime) { this.closingTime = closingTime; }

    public double getTotalUsd() { return totalUsd; }
    public void setTotalUsd(double totalUsd) { this.totalUsd = totalUsd; }

    public double getTotalVes() { return totalVes; }
    public void setTotalVes(double totalVes) { this.totalVes = totalVes; }

    public double getTotalCop() { return totalCop; }
    public void setTotalCop(double totalCop) { this.totalCop = totalCop; }

    public int getTotalPatients() { return totalPatients; }
    public void setTotalPatients(int totalPatients) { this.totalPatients = totalPatients; }

    public String getClosedBy() { return closedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getFormattedUsd() {
        return String.format("$%.2f USD", totalUsd);
    }

    public String getFormattedVes() {
        return String.format("Bs. %.2f VES", totalVes);
    }

    public String getFormattedCop() {
        return String.format("$%,.0f COP", totalCop);
    }
}
