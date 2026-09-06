package com.mediclinic.dao;

import com.mediclinic.database.DatabaseConnection;
import com.mediclinic.database.DateHelper;
import com.mediclinic.models.BlockedDate;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockedDateDAO {

    public boolean insert(LocalDate date, String reason, String blockedBy) {
        String sql = "INSERT OR REPLACE INTO agenda_bloqueos (fecha, motivo, bloqueado_por, creado_en) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, DateHelper.formatLocalDate(date));
            ps.setString(2, reason != null ? reason : "No disponible");
            ps.setString(3, blockedBy != null ? blockedBy : "Usuario");
            ps.setString(4, DateHelper.formatLocalDateTime(LocalDateTime.now()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en BlockedDateDAO.insert: " + e.getMessage());
        }
        return false;
    }

    public boolean delete(LocalDate date) {
        String sql = "DELETE FROM agenda_bloqueos WHERE fecha = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, DateHelper.formatLocalDate(date));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en BlockedDateDAO.delete: " + e.getMessage());
        }
        return false;
    }

    public BlockedDate findByDate(LocalDate date) {
        String sql = "SELECT id, fecha, motivo, bloqueado_por, creado_en FROM agenda_bloqueos WHERE fecha = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, DateHelper.formatLocalDate(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new BlockedDate(
                            rs.getInt("id"),
                            DateHelper.parseLocalDate(rs, "fecha"),
                            rs.getString("motivo"),
                            rs.getString("bloqueado_por"),
                            DateHelper.parseLocalDateTime(rs, "creado_en")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en BlockedDateDAO.findByDate: " + e.getMessage());
        }
        return null;
    }

    public boolean isDateBlocked(LocalDate date) {
        return findByDate(date) != null;
    }

    public Map<LocalDate, String> findByMonthYear(int month, int year) {
        Map<LocalDate, String> map = new HashMap<>();
        String prefix = String.format("%04d-%02d-", year, month);
        String sql = "SELECT fecha, motivo FROM agenda_bloqueos WHERE fecha LIKE ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate d = DateHelper.parseLocalDate(rs, "fecha");
                    if (d != null) {
                        map.put(d, rs.getString("motivo"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en BlockedDateDAO.findByMonthYear: " + e.getMessage());
        }
        return map;
    }

    public List<BlockedDate> findAll() {
        List<BlockedDate> list = new ArrayList<>();
        String sql = "SELECT id, fecha, motivo, bloqueado_por, creado_en FROM agenda_bloqueos ORDER BY fecha DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new BlockedDate(
                        rs.getInt("id"),
                        DateHelper.parseLocalDate(rs, "fecha"),
                        rs.getString("motivo"),
                        rs.getString("bloqueado_por"),
                        DateHelper.parseLocalDateTime(rs, "creado_en")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error en BlockedDateDAO.findAll: " + e.getMessage());
        }
        return list;
    }
}
