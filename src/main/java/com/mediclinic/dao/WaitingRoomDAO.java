package com.mediclinic.dao;

import com.mediclinic.database.DatabaseConnection;
import com.mediclinic.database.DateHelper;
import com.mediclinic.models.WaitingRoomEntry;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WaitingRoomDAO {

    public boolean insert(WaitingRoomEntry entry) {
        String sql = "INSERT INTO sala_espera (paciente_id, fecha_llegada, motivo, estado) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entry.getPatientId());
            ps.setString(2, DateHelper.formatLocalDateTime(entry.getArrivalTime() != null ? entry.getArrivalTime() : LocalDateTime.now()));
            ps.setString(3, entry.getReason());
            ps.setString(4, entry.getStatus() != null ? entry.getStatus() : "EN_ESPERA");

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        entry.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error en WaitingRoomDAO.insert: " + e.getMessage());
        }
        return false;
    }

    public boolean updateStatus(int id, String newStatus) {
        String sql = "UPDATE sala_espera SET estado = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en WaitingRoomDAO.updateStatus: " + e.getMessage());
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM sala_espera WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en WaitingRoomDAO.delete: " + e.getMessage());
        }
        return false;
    }

    public List<WaitingRoomEntry> findAllToday() {
        List<WaitingRoomEntry> list = new ArrayList<>();
        String sql = "SELECT s.id, s.paciente_id, s.fecha_llegada, s.motivo, s.estado, " +
                     "p.nombres, p.apellidos, p.cedula, p.telefono, p.numero_historia, p.tipo_origen, " +
                     "pd.id AS pago_id, pd.medio_pago, pd.moneda, pd.monto " +
                     "FROM sala_espera s " +
                     "INNER JOIN pacientes p ON s.paciente_id = p.id " +
                     "LEFT JOIN pagos_diarios pd ON (pd.sala_espera_id = s.id OR (pd.paciente_id = s.paciente_id AND pd.fecha_pago = date('now', 'localtime'))) " +
                     "ORDER BY s.id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int turnCounter = 1;
            while (rs.next()) {
                WaitingRoomEntry entry = new WaitingRoomEntry(
                        rs.getInt("id"),
                        rs.getInt("paciente_id"),
                        DateHelper.parseLocalDateTime(rs, "fecha_llegada"),
                        rs.getString("motivo"),
                        rs.getString("estado")
                );
                entry.setPatientName(rs.getString("nombres") + " " + rs.getString("apellidos"));
                entry.setPatientIdCard(rs.getString("cedula"));
                entry.setPatientPhone(rs.getString("telefono"));
                entry.setPatientMedicalRecordNumber(rs.getString("numero_historia"));
                entry.setPatientOrigin(rs.getString("tipo_origen"));
                entry.setDailyTurnNumber(turnCounter++);

                int pagoId = rs.getInt("pago_id");
                if (!rs.wasNull() && pagoId > 0) {
                    entry.setPaid(true);
                    String medio = rs.getString("medio_pago");
                    String moneda = rs.getString("moneda");
                    double monto = rs.getDouble("monto");
                    entry.setPaymentMethod(medio);
                    entry.setPaymentCurrency(moneda);
                    entry.setPaymentAmount(monto);
                    String fmtAmt = "USD".equalsIgnoreCase(moneda) ? String.format("$%.2f USD", monto) :
                                    ("VES".equalsIgnoreCase(moneda) ? String.format("Bs. %.2f VES", monto) : String.format("$%,.0f COP", monto));
                    entry.setPaymentDetail(fmtAmt + " (" + medio + ")");
                } else {
                    entry.setPaid(false);
                    entry.setPaymentDetail("Pendiente");
                }

                list.add(entry);
            }
        } catch (SQLException e) {
            System.err.println("Error en WaitingRoomDAO.findAllToday: " + e.getMessage());
        }
        return list;
    }

    public int countWaiting() {
        String sql = "SELECT COUNT(*) FROM sala_espera WHERE estado = 'EN_ESPERA'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error en WaitingRoomDAO.countWaiting: " + e.getMessage());
        }
        return 0;
    }

    public boolean clearAll() {
        String sql = "DELETE FROM sala_espera";
        String sqlSeq = "DELETE FROM sqlite_sequence WHERE name = 'sala_espera'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            try {
                stmt.executeUpdate(sqlSeq);
            } catch (SQLException ignored) {}
            return true;
        } catch (SQLException e) {
            System.err.println("Error en WaitingRoomDAO.clearAll: " + e.getMessage());
        }
        return false;
    }
}
