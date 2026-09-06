package com.mediclinic.dao;

import com.mediclinic.database.DatabaseConnection;
import com.mediclinic.database.DateHelper;
import com.mediclinic.models.Appointment;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDAO {

    public boolean insert(Appointment appointment) {
        String sql = "INSERT INTO agenda_citas (paciente_id, cedula, nombres, apellidos, telefono, fecha_cita, hora_cita, motivo, estado, notas, creado_en) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (appointment.getPatientId() != null && appointment.getPatientId() > 0) {
                ps.setInt(1, appointment.getPatientId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, appointment.getIdCard());
            ps.setString(3, appointment.getFirstName());
            ps.setString(4, appointment.getLastName());
            ps.setString(5, appointment.getPhone());
            ps.setString(6, DateHelper.formatLocalDate(appointment.getAppointmentDate()));
            ps.setString(7, appointment.getAppointmentTime());
            ps.setString(8, appointment.getReason());
            ps.setString(9, appointment.getStatus() != null ? appointment.getStatus() : "PENDIENTE");
            ps.setString(10, appointment.getNotes());
            ps.setString(11, DateHelper.formatLocalDateTime(appointment.getCreatedAt() != null ? appointment.getCreatedAt() : LocalDateTime.now()));

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        appointment.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error en AppointmentDAO.insert: " + e.getMessage());
        }
        return false;
    }

    public boolean update(Appointment appointment) {
        String sql = "UPDATE agenda_citas SET paciente_id = ?, cedula = ?, nombres = ?, apellidos = ?, telefono = ?, fecha_cita = ?, hora_cita = ?, motivo = ?, estado = ?, notas = ? " +
                     "WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (appointment.getPatientId() != null && appointment.getPatientId() > 0) {
                ps.setInt(1, appointment.getPatientId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, appointment.getIdCard());
            ps.setString(3, appointment.getFirstName());
            ps.setString(4, appointment.getLastName());
            ps.setString(5, appointment.getPhone());
            ps.setString(6, DateHelper.formatLocalDate(appointment.getAppointmentDate()));
            ps.setString(7, appointment.getAppointmentTime());
            ps.setString(8, appointment.getReason());
            ps.setString(9, appointment.getStatus());
            ps.setString(10, appointment.getNotes());
            ps.setInt(11, appointment.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en AppointmentDAO.update: " + e.getMessage());
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM agenda_citas WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en AppointmentDAO.delete: " + e.getMessage());
        }
        return false;
    }

    public Appointment findById(int id) {
        String sql = "SELECT id, paciente_id, cedula, nombres, apellidos, telefono, fecha_cita, hora_cita, motivo, estado, notas, creado_en " +
                     "FROM agenda_citas WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAppointment(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en AppointmentDAO.findById: " + e.getMessage());
        }
        return null;
    }

    public List<Appointment> findByDate(LocalDate date) {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT id, paciente_id, cedula, nombres, apellidos, telefono, fecha_cita, hora_cita, motivo, estado, notas, creado_en " +
                     "FROM agenda_citas WHERE fecha_cita = ? ORDER BY hora_cita ASC, id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, DateHelper.formatLocalDate(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAppointment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en AppointmentDAO.findByDate: " + e.getMessage());
        }
        return list;
    }

    public List<Appointment> findByMonthYear(int month, int year) {
        List<Appointment> list = new ArrayList<>();
        String prefix = String.format("%04d-%02d-", year, month);
        String sql = "SELECT id, paciente_id, cedula, nombres, apellidos, telefono, fecha_cita, hora_cita, motivo, estado, notas, creado_en " +
                     "FROM agenda_citas WHERE fecha_cita LIKE ? ORDER BY fecha_cita ASC, hora_cita ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAppointment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en AppointmentDAO.findByMonthYear: " + e.getMessage());
        }
        return list;
    }

    public List<Appointment> findAll() {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT id, paciente_id, cedula, nombres, apellidos, telefono, fecha_cita, hora_cita, motivo, estado, notas, creado_en " +
                     "FROM agenda_citas ORDER BY fecha_cita DESC, hora_cita ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToAppointment(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error en AppointmentDAO.findAll: " + e.getMessage());
        }
        return list;
    }

    public int countAppointmentsToday() {
        String sql = "SELECT COUNT(*) FROM agenda_citas WHERE fecha_cita = ? AND estado != 'CANCELADA'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, DateHelper.formatLocalDate(LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en AppointmentDAO.countAppointmentsToday: " + e.getMessage());
        }
        return 0;
    }

    private Appointment mapResultSetToAppointment(ResultSet rs) throws SQLException {
        Integer patId = rs.getInt("paciente_id");
        if (rs.wasNull()) {
            patId = null;
        }
        LocalDate fCita = DateHelper.parseLocalDate(rs, "fecha_cita");
        LocalDateTime cEn = DateHelper.parseLocalDateTime(rs, "creado_en");

        return new Appointment(
                rs.getInt("id"),
                patId,
                rs.getString("cedula"),
                rs.getString("nombres"),
                rs.getString("apellidos"),
                rs.getString("telefono"),
                fCita,
                rs.getString("hora_cita"),
                rs.getString("motivo"),
                rs.getString("estado"),
                rs.getString("notas"),
                cEn
        );
    }
}
