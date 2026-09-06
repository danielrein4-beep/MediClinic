package com.mediclinic.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BlockedDate {
    private int id;
    private LocalDate date;
    private String reason;
    private String blockedBy;
    private LocalDateTime createdAt;

    public BlockedDate() {}

    public BlockedDate(int id, LocalDate date, String reason, String blockedBy, LocalDateTime createdAt) {
        this.id = id;
        this.date = date;
        this.reason = reason;
        this.blockedBy = blockedBy;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public BlockedDate(LocalDate date, String reason, String blockedBy) {
        this(-1, date, reason, blockedBy, LocalDateTime.now());
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getBlockedBy() { return blockedBy; }
    public void setBlockedBy(String blockedBy) { this.blockedBy = blockedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
