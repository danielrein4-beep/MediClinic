package com.mediclinic.dao;

import com.mediclinic.database.DatabaseConnection;
import com.mediclinic.database.DateHelper;
import com.mediclinic.models.FinancialClosing;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FinancialClosingDAO {

    public boolean insert(FinancialClosing closing) {
        String sql = "INSERT INTO cierres_financieros (fecha_cierre, hora_cierre, total_usd, total_ves, total_cop, total_pacientes, cerrado_por, notas, creado_en) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, DateHelper.formatLocalDate(closing.getClosingDate()));
            ps.setString(2, closing.getClosingTime());
            ps.setDouble(3, closing.getTotalUsd());
            ps.setDouble(4, closing.getTotalVes());
            ps.setDouble(5, closing.getTotalCop());
            ps.setInt(6, closing.getTotalPatients());
            ps.setString(7, closing.getClosedBy());
            ps.setString(8, closing.getNotes());
            ps.setString(9, DateHelper.formatLocalDateTime(closing.getCreatedAt() != null ? closing.getCreatedAt() : LocalDateTime.now()));

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        closing.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error en FinancialClosingDAO.insert: " + e.getMessage());
        }
        return false;
    }

    public List<FinancialClosing> findAll() {
        List<FinancialClosing> list = new ArrayList<>();
        String sql = "SELECT id, fecha_cierre, hora_cierre, total_usd, total_ves, total_cop, total_pacientes, cerrado_por, notas, creado_en " +
                     "FROM cierres_financieros ORDER BY fecha_cierre DESC, id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error en FinancialClosingDAO.findAll: " + e.getMessage());
        }
        return list;
    }

    public FinancialClosing findByDate(LocalDate date) {
        String sql = "SELECT id, fecha_cierre, hora_cierre, total_usd, total_ves, total_cop, total_pacientes, cerrado_por, notas, creado_en " +
                     "FROM cierres_financieros WHERE fecha_cierre = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, DateHelper.formatLocalDate(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en FinancialClosingDAO.findByDate: " + e.getMessage());
        }
        return null;
    }

    public FinancialClosing findById(int id) {
        String sql = "SELECT id, fecha_cierre, hora_cierre, total_usd, total_ves, total_cop, total_pacientes, cerrado_por, notas, creado_en " +
                     "FROM cierres_financieros WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en FinancialClosingDAO.findById: " + e.getMessage());
        }
        return null;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM cierres_financieros WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en FinancialClosingDAO.delete: " + e.getMessage());
        }
        return false;
    }

    /**
     * Consulta cierres financieros filtrados por rango de tiempo usando la función DATE() de SQLite.
     * @param filter Filtro de tiempo: "Último Día", "Última Semana", "Último Mes", "Total Histórico"
     */
    public List<FinancialClosing> findByTimeFilter(String filter) {
        List<FinancialClosing> list = new ArrayList<>();
        String sql;

        if (filter == null || "Total Histórico".equalsIgnoreCase(filter) || "Total Historico".equalsIgnoreCase(filter) || "ALL".equalsIgnoreCase(filter)) {
            sql = "SELECT id, fecha_cierre, hora_cierre, total_usd, total_ves, total_cop, total_pacientes, cerrado_por, notas, creado_en " +
                  "FROM cierres_financieros ORDER BY fecha_cierre DESC, id DESC";
        } else if ("Último Día".equalsIgnoreCase(filter) || "Ultimo Dia".equalsIgnoreCase(filter) || "DAY".equalsIgnoreCase(filter)) {
            sql = "SELECT id, fecha_cierre, hora_cierre, total_usd, total_ves, total_cop, total_pacientes, cerrado_por, notas, creado_en " +
                  "FROM cierres_financieros WHERE DATE(fecha_cierre) = (SELECT MAX(DATE(fecha_cierre)) FROM cierres_financieros) " +
                  "ORDER BY fecha_cierre DESC, id DESC";
        } else if ("Última Semana".equalsIgnoreCase(filter) || "Ultima Semana".equalsIgnoreCase(filter) || "WEEK".equalsIgnoreCase(filter)) {
            sql = "SELECT id, fecha_cierre, hora_cierre, total_usd, total_ves, total_cop, total_pacientes, cerrado_por, notas, creado_en " +
                  "FROM cierres_financieros WHERE DATE(fecha_cierre) >= DATE('now', 'localtime', '-7 days') " +
                  "ORDER BY fecha_cierre DESC, id DESC";
        } else if ("Último Mes".equalsIgnoreCase(filter) || "Ultimo Mes".equalsIgnoreCase(filter) || "MONTH".equalsIgnoreCase(filter)) {
            sql = "SELECT id, fecha_cierre, hora_cierre, total_usd, total_ves, total_cop, total_pacientes, cerrado_por, notas, creado_en " +
                  "FROM cierres_financieros WHERE DATE(fecha_cierre) >= DATE('now', 'localtime', '-30 days') " +
                  "ORDER BY fecha_cierre DESC, id DESC";
        } else {
            sql = "SELECT id, fecha_cierre, hora_cierre, total_usd, total_ves, total_cop, total_pacientes, cerrado_por, notas, creado_en " +
                  "FROM cierres_financieros ORDER BY fecha_cierre DESC, id DESC";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error en FinancialClosingDAO.findByTimeFilter: " + e.getMessage());
        }
        return list;
    }

    /**
     * Calcula los totales consolidados (KPIs) directamente en SQLite usando la función DATE() y SUM().
     * @param filter Filtro de tiempo: "Último Día", "Última Semana", "Último Mes", "Total Histórico"
     */
    public FinancialSummaryTotals getTotalsByTimeFilter(String filter) {
        String sql;

        if (filter == null || "Total Histórico".equalsIgnoreCase(filter) || "Total Historico".equalsIgnoreCase(filter) || "ALL".equalsIgnoreCase(filter)) {
            sql = "SELECT COALESCE(SUM(total_usd), 0.0) AS sum_usd, " +
                  "COALESCE(SUM(total_ves), 0.0) AS sum_ves, " +
                  "COALESCE(SUM(total_cop), 0.0) AS sum_cop, " +
                  "COALESCE(SUM(total_pacientes), 0) AS sum_patients, " +
                  "COUNT(id) AS count_closings " +
                  "FROM cierres_financieros";
        } else if ("Último Día".equalsIgnoreCase(filter) || "Ultimo Dia".equalsIgnoreCase(filter) || "DAY".equalsIgnoreCase(filter)) {
            sql = "SELECT COALESCE(SUM(total_usd), 0.0) AS sum_usd, " +
                  "COALESCE(SUM(total_ves), 0.0) AS sum_ves, " +
                  "COALESCE(SUM(total_cop), 0.0) AS sum_cop, " +
                  "COALESCE(SUM(total_pacientes), 0) AS sum_patients, " +
                  "COUNT(id) AS count_closings " +
                  "FROM cierres_financieros WHERE DATE(fecha_cierre) = (SELECT MAX(DATE(fecha_cierre)) FROM cierres_financieros)";
        } else if ("Última Semana".equalsIgnoreCase(filter) || "Ultima Semana".equalsIgnoreCase(filter) || "WEEK".equalsIgnoreCase(filter)) {
            sql = "SELECT COALESCE(SUM(total_usd), 0.0) AS sum_usd, " +
                  "COALESCE(SUM(total_ves), 0.0) AS sum_ves, " +
                  "COALESCE(SUM(total_cop), 0.0) AS sum_cop, " +
                  "COALESCE(SUM(total_pacientes), 0) AS sum_patients, " +
                  "COUNT(id) AS count_closings " +
                  "FROM cierres_financieros WHERE DATE(fecha_cierre) >= DATE('now', 'localtime', '-7 days')";
        } else if ("Último Mes".equalsIgnoreCase(filter) || "Ultimo Mes".equalsIgnoreCase(filter) || "MONTH".equalsIgnoreCase(filter)) {
            sql = "SELECT COALESCE(SUM(total_usd), 0.0) AS sum_usd, " +
                  "COALESCE(SUM(total_ves), 0.0) AS sum_ves, " +
                  "COALESCE(SUM(total_cop), 0.0) AS sum_cop, " +
                  "COALESCE(SUM(total_pacientes), 0) AS sum_patients, " +
                  "COUNT(id) AS count_closings " +
                  "FROM cierres_financieros WHERE DATE(fecha_cierre) >= DATE('now', 'localtime', '-30 days')";
        } else {
            sql = "SELECT COALESCE(SUM(total_usd), 0.0) AS sum_usd, " +
                  "COALESCE(SUM(total_ves), 0.0) AS sum_ves, " +
                  "COALESCE(SUM(total_cop), 0.0) AS sum_cop, " +
                  "COALESCE(SUM(total_pacientes), 0) AS sum_patients, " +
                  "COUNT(id) AS count_closings " +
                  "FROM cierres_financieros";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return new FinancialSummaryTotals(
                        rs.getDouble("sum_usd"),
                        rs.getDouble("sum_ves"),
                        rs.getDouble("sum_cop"),
                        rs.getInt("sum_patients"),
                        rs.getInt("count_closings")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error en FinancialClosingDAO.getTotalsByTimeFilter: " + e.getMessage());
        }
        return new FinancialSummaryTotals(0.0, 0.0, 0.0, 0, 0);
    }

    private FinancialClosing mapResultSet(ResultSet rs) throws SQLException {
        return new FinancialClosing(
                rs.getInt("id"),
                DateHelper.parseLocalDate(rs, "fecha_cierre"),
                rs.getString("hora_cierre"),
                rs.getDouble("total_usd"),
                rs.getDouble("total_ves"),
                rs.getDouble("total_cop"),
                rs.getInt("total_pacientes"),
                rs.getString("cerrado_por"),
                rs.getString("notas"),
                DateHelper.parseLocalDateTime(rs, "creado_en")
        );
    }

    /**
     * Objeto contenedor para transferir métricas y KPIs calculados por periodo.
     */
    public static class FinancialSummaryTotals {
        private final double totalUsd;
        private final double totalVes;
        private final double totalCop;
        private final int totalPatients;
        private final int totalClosings;

        public FinancialSummaryTotals(double totalUsd, double totalVes, double totalCop, int totalPatients, int totalClosings) {
            this.totalUsd = totalUsd;
            this.totalVes = totalVes;
            this.totalCop = totalCop;
            this.totalPatients = totalPatients;
            this.totalClosings = totalClosings;
        }

        public double getTotalUsd() { return totalUsd; }
        public double getTotalVes() { return totalVes; }
        public double getTotalCop() { return totalCop; }
        public int getTotalPatients() { return totalPatients; }
        public int getTotalClosings() { return totalClosings; }
    }
}
