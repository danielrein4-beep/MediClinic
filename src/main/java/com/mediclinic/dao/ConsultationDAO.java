package com.mediclinic.dao;

import com.mediclinic.database.DatabaseConnection;
import com.mediclinic.database.DateHelper;
import com.mediclinic.models.Consultation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConsultationDAO {

    public boolean insert(Consultation consultation) {
        String sql = "INSERT INTO consultas (paciente_id, doctor_id, fecha_hora, motivo_consulta, anotaciones_evolucion, " +
                     "diagnostico, tratamiento_receta, pa, fc, peso, talla, temperatura, proxima_cita, informe_pdf_path, observacion_fisica) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, consultation.getPatientId());
            ps.setInt(2, consultation.getDoctorId());
            ps.setString(3, DateHelper.formatLocalDateTime(consultation.getDateTime() != null ? 
                    consultation.getDateTime() : LocalDateTime.now()));
            ps.setString(4, consultation.getReason());
            ps.setString(5, consultation.getClinicalNotes());
            ps.setString(6, consultation.getDiagnosis());
            ps.setString(7, consultation.getTreatmentRx());
            ps.setString(8, consultation.getBloodPressure());
            
            if (consultation.getHeartRate() != null) ps.setInt(9, consultation.getHeartRate());
            else ps.setNull(9, java.sql.Types.INTEGER);
            
            if (consultation.getWeightKg() != null) ps.setDouble(10, consultation.getWeightKg());
            else ps.setNull(10, java.sql.Types.DOUBLE);
            
            if (consultation.getHeightM() != null) ps.setDouble(11, consultation.getHeightM());
            else ps.setNull(11, java.sql.Types.DOUBLE);
            
            if (consultation.getTemperature() != null) ps.setDouble(12, consultation.getTemperature());
            else ps.setNull(12, java.sql.Types.DOUBLE);
            
            ps.setString(13, DateHelper.formatLocalDate(consultation.getNextAppointmentDate()));
            ps.setString(14, consultation.getIssuedDocumentPath());
            ps.setString(15, consultation.getPhysicalNotes());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        consultation.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error en ConsultationDAO.insert: " + e.getMessage());
        }
        return false;
    }

    public List<Consultation> findByPatientId(int patientId) {
        List<Consultation> list = new ArrayList<>();
        String sql = "SELECT c.id, c.paciente_id, c.doctor_id, c.fecha_hora, c.motivo_consulta, c.anotaciones_evolucion, " +
                     "c.diagnostico, c.tratamiento_receta, c.pa, c.fc, c.peso, c.talla, c.temperatura, c.proxima_cita, c.informe_pdf_path, c.observacion_fisica, " +
                     "u.nombre_completo as doctor_nombre " +
                     "FROM consultas c " +
                     "LEFT JOIN usuarios u ON c.doctor_id = u.id " +
                     "WHERE c.paciente_id = ? " +
                     "ORDER BY c.fecha_hora DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Consultation c = mapResultSetToConsultation(rs);
                    c.setDoctorName(rs.getString("doctor_nombre"));
                    list.add(c);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en ConsultationDAO.findByPatientId: " + e.getMessage());
        }
        return list;
    }

    public int countConsultationsToday() {
        String sql = "SELECT COUNT(*) FROM consultas WHERE DATE(fecha_hora) = DATE('now', 'localtime')";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error en ConsultationDAO.countConsultationsToday: " + e.getMessage());
        }
        return 0;
    }

    public int countConsultationsThisWeek() {
        String sql = "SELECT COUNT(*) FROM consultas WHERE fecha_hora >= datetime('now', 'localtime', '-7 days')";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error en ConsultationDAO.countConsultationsThisWeek: " + e.getMessage());
        }
        return 0;
    }

    public boolean updateIssuedDocumentPath(int consultationId, String pdfPath) {
        String sql = "UPDATE consultas SET informe_pdf_path = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pdfPath);
            ps.setInt(2, consultationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en ConsultationDAO.updateIssuedDocumentPath: " + e.getMessage());
        }
        return false;
    }

    private Consultation mapResultSetToConsultation(ResultSet rs) throws SQLException {
        LocalDateTime fHora = DateHelper.parseLocalDateTime(rs, "fecha_hora");
        LocalDate pCita = DateHelper.parseLocalDate(rs, "proxima_cita");
        
        Integer fc = (Integer) rs.getObject("fc");
        Double peso = (Double) rs.getObject("peso");
        Double talla = (Double) rs.getObject("talla");
        Double temp = (Double) rs.getObject("temperatura");

        Consultation c = new Consultation(
                rs.getInt("id"),
                rs.getInt("paciente_id"),
                rs.getInt("doctor_id"),
                fHora,
                rs.getString("motivo_consulta"),
                rs.getString("anotaciones_evolucion"),
                rs.getString("diagnostico"),
                rs.getString("tratamiento_receta"),
                peso,
                talla,
                rs.getString("observacion_fisica"),
                pCita,
                rs.getString("informe_pdf_path")
        );
        c.setBloodPressure(rs.getString("pa"));
        c.setHeartRate(fc);
        c.setTemperature(temp);
        return c;
    }
}
