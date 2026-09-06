package com.mediclinic.dao;

import com.mediclinic.database.DatabaseConnection;
import com.mediclinic.database.DateHelper;
import com.mediclinic.models.ProcedureQuote;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProcedureDAO {

    public boolean insert(ProcedureQuote quote) {
        String sql = "INSERT INTO procedimientos_cotizaciones (paciente_id, tipo_procedimiento, descripcion, monto_usd, " +
                     "tasa_ves, monto_ves, tasa_cop, monto_cop, estado, fecha_planificada, notas, creado_en) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, quote.getPatientId());
            ps.setString(2, quote.getProcedureType());
            ps.setString(3, quote.getDescription());
            ps.setDouble(4, quote.getAmountUsd());
            ps.setDouble(5, quote.getRateVes());
            ps.setDouble(6, quote.getAmountVes());
            ps.setDouble(7, quote.getRateCop());
            ps.setDouble(8, quote.getAmountCop());
            ps.setString(9, quote.getStatus() != null ? quote.getStatus() : "COTIZADA");
            ps.setString(10, DateHelper.formatLocalDateTime(quote.getPlannedDate()));
            ps.setString(11, quote.getNotes());
            ps.setString(12, DateHelper.formatLocalDateTime(LocalDateTime.now()));

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        quote.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error en ProcedureDAO.insert: " + e.getMessage());
        }
        return false;
    }

    public boolean update(ProcedureQuote quote) {
        String sql = "UPDATE procedimientos_cotizaciones SET paciente_id = ?, tipo_procedimiento = ?, descripcion = ?, " +
                     "monto_usd = ?, tasa_ves = ?, monto_ves = ?, tasa_cop = ?, monto_cop = ?, estado = ?, " +
                     "fecha_planificada = ?, notas = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quote.getPatientId());
            ps.setString(2, quote.getProcedureType());
            ps.setString(3, quote.getDescription());
            ps.setDouble(4, quote.getAmountUsd());
            ps.setDouble(5, quote.getRateVes());
            ps.setDouble(6, quote.getAmountVes());
            ps.setDouble(7, quote.getRateCop());
            ps.setDouble(8, quote.getAmountCop());
            ps.setString(9, quote.getStatus());
            ps.setString(10, DateHelper.formatLocalDateTime(quote.getPlannedDate()));
            ps.setString(11, quote.getNotes());
            ps.setInt(12, quote.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en ProcedureDAO.update: " + e.getMessage());
        }
        return false;
    }

    public boolean updateStatus(int quoteId, String newStatus) {
        String sql = "UPDATE procedimientos_cotizaciones SET estado = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, quoteId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en ProcedureDAO.updateStatus: " + e.getMessage());
        }
        return false;
    }

    public boolean updateStatus(int quoteId, String newStatus, LocalDateTime plannedDate) {
        String sql = "UPDATE procedimientos_cotizaciones SET estado = ?, fecha_planificada = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, DateHelper.formatLocalDateTime(plannedDate));
            ps.setInt(3, quoteId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en ProcedureDAO.updateStatus: " + e.getMessage());
        }
        return false;
    }

    public boolean delete(int quoteId) {
        String sql = "DELETE FROM procedimientos_cotizaciones WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quoteId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en ProcedureDAO.delete: " + e.getMessage());
        }
        return false;
    }

    public List<ProcedureQuote> findUpcomingThisWeek() {
        List<ProcedureQuote> list = new ArrayList<>();
        String sql = "SELECT pr.id, pr.paciente_id, pr.tipo_procedimiento, pr.descripcion, pr.monto_usd, " +
                     "pr.tasa_ves, pr.monto_ves, pr.tasa_cop, pr.monto_cop, pr.estado, pr.fecha_planificada, pr.notas, pr.creado_en, " +
                     "p.nombres, p.apellidos, p.cedula, p.tipo_origen " +
                     "FROM procedimientos_cotizaciones pr " +
                     "INNER JOIN pacientes p ON pr.paciente_id = p.id " +
                     "WHERE pr.estado = 'PLANIFICADA' AND pr.fecha_planificada >= datetime('now', 'localtime') " +
                     "AND pr.fecha_planificada <= datetime('now', 'localtime', '+7 days') " +
                     "ORDER BY pr.fecha_planificada ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ProcedureQuote q = mapResultSetToQuote(rs);
                q.setPatientName(rs.getString("nombres") + " " + rs.getString("apellidos"));
                q.setPatientIdCard(rs.getString("cedula"));
                q.setPatientOrigin(rs.getString("tipo_origen"));
                list.add(q);
            }
        } catch (SQLException e) {
            System.err.println("Error en ProcedureDAO.findUpcomingThisWeek: " + e.getMessage());
        }
        return list;
    }

    public List<ProcedureQuote> findAll() {
        List<ProcedureQuote> list = new ArrayList<>();
        String sql = "SELECT pr.id, pr.paciente_id, pr.tipo_procedimiento, pr.descripcion, pr.monto_usd, " +
                     "pr.tasa_ves, pr.monto_ves, pr.tasa_cop, pr.monto_cop, pr.estado, pr.fecha_planificada, pr.notas, pr.creado_en, " +
                     "p.nombres, p.apellidos, p.cedula, p.tipo_origen " +
                     "FROM procedimientos_cotizaciones pr " +
                     "INNER JOIN pacientes p ON pr.paciente_id = p.id " +
                     "ORDER BY pr.id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ProcedureQuote q = mapResultSetToQuote(rs);
                q.setPatientName(rs.getString("nombres") + " " + rs.getString("apellidos"));
                q.setPatientIdCard(rs.getString("cedula"));
                q.setPatientOrigin(rs.getString("tipo_origen"));
                list.add(q);
            }
        } catch (SQLException e) {
            System.err.println("Error en ProcedureDAO.findAll: " + e.getMessage());
        }
        return list;
    }

    public List<ProcedureQuote> findByStatus(String status) {
        List<ProcedureQuote> list = new ArrayList<>();
        String sql = "SELECT pr.id, pr.paciente_id, pr.tipo_procedimiento, pr.descripcion, pr.monto_usd, " +
                     "pr.tasa_ves, pr.monto_ves, pr.tasa_cop, pr.monto_cop, pr.estado, pr.fecha_planificada, pr.notas, pr.creado_en, " +
                     "p.nombres, p.apellidos, p.cedula, p.tipo_origen " +
                     "FROM procedimientos_cotizaciones pr " +
                     "INNER JOIN pacientes p ON pr.paciente_id = p.id " +
                     "WHERE pr.estado = ? " +
                     "ORDER BY pr.id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProcedureQuote q = mapResultSetToQuote(rs);
                    q.setPatientName(rs.getString("nombres") + " " + rs.getString("apellidos"));
                    q.setPatientIdCard(rs.getString("cedula"));
                    q.setPatientOrigin(rs.getString("tipo_origen"));
                    list.add(q);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en ProcedureDAO.findByStatus: " + e.getMessage());
        }
        return list;
    }

    public List<ProcedureQuote> findByPatientId(int patientId) {
        List<ProcedureQuote> list = new ArrayList<>();
        String sql = "SELECT pr.id, pr.paciente_id, pr.tipo_procedimiento, pr.descripcion, pr.monto_usd, " +
                     "pr.tasa_ves, pr.monto_ves, pr.tasa_cop, pr.monto_cop, pr.estado, pr.fecha_planificada, pr.notas, pr.creado_en, " +
                     "p.nombres, p.apellidos, p.cedula, p.tipo_origen " +
                     "FROM procedimientos_cotizaciones pr " +
                     "INNER JOIN pacientes p ON pr.paciente_id = p.id " +
                     "WHERE pr.paciente_id = ? " +
                     "ORDER BY pr.id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProcedureQuote q = mapResultSetToQuote(rs);
                    q.setPatientName(rs.getString("nombres") + " " + rs.getString("apellidos"));
                    q.setPatientIdCard(rs.getString("cedula"));
                    q.setPatientOrigin(rs.getString("tipo_origen"));
                    list.add(q);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en ProcedureDAO.findByPatientId: " + e.getMessage());
        }
        return list;
    }

    public List<ProcedureQuote> search(String query, String statusFilter) {
        List<ProcedureQuote> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT pr.id, pr.paciente_id, pr.tipo_procedimiento, pr.descripcion, pr.monto_usd, " +
                "pr.tasa_ves, pr.monto_ves, pr.tasa_cop, pr.monto_cop, pr.estado, pr.fecha_planificada, pr.notas, pr.creado_en, " +
                "p.nombres, p.apellidos, p.cedula, p.tipo_origen " +
                "FROM procedimientos_cotizaciones pr " +
                "INNER JOIN pacientes p ON pr.paciente_id = p.id WHERE 1=1 ");

        if (statusFilter != null && !statusFilter.isEmpty() && !statusFilter.equalsIgnoreCase("TODOS")) {
            sql.append("AND pr.estado = ? ");
        }

        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (p.nombres LIKE ? OR p.apellidos LIKE ? OR p.cedula LIKE ? OR pr.tipo_procedimiento LIKE ? OR pr.descripcion LIKE ?) ");
        }

        sql.append("ORDER BY pr.id DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIdx = 1;
            if (statusFilter != null && !statusFilter.isEmpty() && !statusFilter.equalsIgnoreCase("TODOS")) {
                ps.setString(paramIdx++, statusFilter);
            }
            if (query != null && !query.trim().isEmpty()) {
                String pattern = "%" + query.trim() + "%";
                ps.setString(paramIdx++, pattern);
                ps.setString(paramIdx++, pattern);
                ps.setString(paramIdx++, pattern);
                ps.setString(paramIdx++, pattern);
                ps.setString(paramIdx++, pattern);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProcedureQuote q = mapResultSetToQuote(rs);
                    q.setPatientName(rs.getString("nombres") + " " + rs.getString("apellidos"));
                    q.setPatientIdCard(rs.getString("cedula"));
                    q.setPatientOrigin(rs.getString("tipo_origen"));
                    list.add(q);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en ProcedureDAO.search: " + e.getMessage());
        }
        return list;
    }

    private ProcedureQuote mapResultSetToQuote(ResultSet rs) throws SQLException {
        LocalDateTime planned = DateHelper.parseLocalDateTime(rs, "fecha_planificada");
        LocalDateTime created = DateHelper.parseLocalDateTime(rs, "creado_en");
        return new ProcedureQuote(
                rs.getInt("id"),
                rs.getInt("paciente_id"),
                rs.getString("tipo_procedimiento"),
                rs.getString("descripcion"),
                rs.getDouble("monto_usd"),
                rs.getDouble("tasa_ves"),
                rs.getDouble("monto_ves"),
                rs.getDouble("tasa_cop"),
                rs.getDouble("monto_cop"),
                rs.getString("estado"),
                planned,
                rs.getString("notas"),
                created
        );
    }
}
