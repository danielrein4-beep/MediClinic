package com.mediclinic.dao;

import com.mediclinic.database.DatabaseConnection;
import com.mediclinic.database.DateHelper;
import com.mediclinic.models.DailyPayment;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DailyPaymentDAO {

    public boolean insert(DailyPayment payment) {
        String sql = "INSERT INTO pagos_diarios (paciente_id, sala_espera_id, fecha_pago, hora_pago, medio_pago, moneda, monto, notas, creado_en) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, payment.getPatientId());
            if (payment.getWaitingRoomId() != null && payment.getWaitingRoomId() > 0) {
                ps.setInt(2, payment.getWaitingRoomId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, DateHelper.formatLocalDate(payment.getPaymentDate()));
            ps.setString(4, payment.getPaymentTime());
            ps.setString(5, payment.getPaymentMethod());
            ps.setString(6, payment.getCurrency());
            ps.setDouble(7, payment.getAmount());
            ps.setString(8, payment.getNotes());
            ps.setString(9, DateHelper.formatLocalDateTime(payment.getCreatedAt() != null ? payment.getCreatedAt() : LocalDateTime.now()));

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        payment.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error en DailyPaymentDAO.insert: " + e.getMessage());
        }
        return false;
    }

    public List<DailyPayment> findByDate(LocalDate date) {
        List<DailyPayment> list = new ArrayList<>();
        String sql = "SELECT pd.id, pd.paciente_id, pd.sala_espera_id, pd.fecha_pago, pd.hora_pago, pd.medio_pago, " +
                     "pd.moneda, pd.monto, pd.notas, pd.creado_en, " +
                     "p.nombres, p.apellidos, p.cedula, p.numero_historia " +
                     "FROM pagos_diarios pd " +
                     "INNER JOIN pacientes p ON pd.paciente_id = p.id " +
                     "WHERE pd.fecha_pago = ? " +
                     "ORDER BY pd.id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, DateHelper.formatLocalDate(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en DailyPaymentDAO.findByDate: " + e.getMessage());
        }
        return list;
    }

    public List<DailyPayment> findAllToday() {
        return findByDate(LocalDate.now());
    }

    public DailyPayment findByWaitingRoomId(int waitingRoomId) {
        String sql = "SELECT pd.id, pd.paciente_id, pd.sala_espera_id, pd.fecha_pago, pd.hora_pago, pd.medio_pago, " +
                     "pd.moneda, pd.monto, pd.notas, pd.creado_en, " +
                     "p.nombres, p.apellidos, p.cedula, p.numero_historia " +
                     "FROM pagos_diarios pd " +
                     "INNER JOIN pacientes p ON pd.paciente_id = p.id " +
                     "WHERE pd.sala_espera_id = ? " +
                     "ORDER BY pd.id DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waitingRoomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en DailyPaymentDAO.findByWaitingRoomId: " + e.getMessage());
        }
        return null;
    }

    public DailyPayment findByPatientToday(int patientId) {
        String sql = "SELECT pd.id, pd.paciente_id, pd.sala_espera_id, pd.fecha_pago, pd.hora_pago, pd.medio_pago, " +
                     "pd.moneda, pd.monto, pd.notas, pd.creado_en, " +
                     "p.nombres, p.apellidos, p.cedula, p.numero_historia " +
                     "FROM pagos_diarios pd " +
                     "INNER JOIN pacientes p ON pd.paciente_id = p.id " +
                     "WHERE pd.paciente_id = ? AND pd.fecha_pago = ? " +
                     "ORDER BY pd.id DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setString(2, DateHelper.formatLocalDate(LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en DailyPaymentDAO.findByPatientToday: " + e.getMessage());
        }
        return null;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM pagos_diarios WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en DailyPaymentDAO.delete: " + e.getMessage());
        }
        return false;
    }

    private DailyPayment mapResultSet(ResultSet rs) throws SQLException {
        Integer sId = rs.getInt("sala_espera_id");
        if (rs.wasNull()) {
            sId = null;
        }
        DailyPayment p = new DailyPayment(
                rs.getInt("id"),
                rs.getInt("paciente_id"),
                sId,
                DateHelper.parseLocalDate(rs, "fecha_pago"),
                rs.getString("hora_pago"),
                rs.getString("medio_pago"),
                rs.getString("moneda"),
                rs.getDouble("monto"),
                rs.getString("notas"),
                DateHelper.parseLocalDateTime(rs, "creado_en")
        );
        p.setPatientName(rs.getString("nombres") + " " + rs.getString("apellidos"));
        p.setPatientIdCard(rs.getString("cedula"));
        p.setPatientMedicalRecordNumber(rs.getString("numero_historia"));
        return p;
    }
}
