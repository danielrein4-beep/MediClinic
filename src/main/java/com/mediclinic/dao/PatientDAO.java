package com.mediclinic.dao;

import com.mediclinic.database.DatabaseConnection;
import com.mediclinic.database.DateHelper;
import com.mediclinic.models.Patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO {

    public boolean insert(Patient patient) {
        String sql = "INSERT INTO pacientes (numero_historia, cedula, nombres, apellidos, fecha_nacimiento, telefono, email, tipo_origen, ciudad_origen, direccion, creado_en) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, patient.getMedicalRecordNumber());
            ps.setString(2, patient.getIdCard());
            ps.setString(3, patient.getFirstName());
            ps.setString(4, patient.getLastName());
            ps.setString(5, DateHelper.formatLocalDate(patient.getBirthDate()));
            ps.setString(6, patient.getPhone());
            ps.setString(7, patient.getEmail());
            ps.setString(8, patient.getOriginType() != null ? patient.getOriginType() : "LOCAL");
            ps.setString(9, patient.getOriginCity());
            ps.setString(10, patient.getAddress());
            ps.setString(11, DateHelper.formatLocalDateTime(LocalDateTime.now()));

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        patient.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error en PatientDAO.insert: " + e.getMessage());
        }
        return false;
    }

    public boolean update(Patient patient) {
        String sql = "UPDATE pacientes SET cedula = ?, nombres = ?, apellidos = ?, fecha_nacimiento = ?, telefono = ?, email = ?, tipo_origen = ?, ciudad_origen = ?, direccion = ? " +
                     "WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patient.getIdCard());
            ps.setString(2, patient.getFirstName());
            ps.setString(3, patient.getLastName());
            ps.setString(4, DateHelper.formatLocalDate(patient.getBirthDate()));
            ps.setString(5, patient.getPhone());
            ps.setString(6, patient.getEmail());
            ps.setString(7, patient.getOriginType());
            ps.setString(8, patient.getOriginCity());
            ps.setString(9, patient.getAddress());
            ps.setInt(10, patient.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en PatientDAO.update: " + e.getMessage());
        }
        return false;
    }

    public Patient findById(int id) {
        String sql = "SELECT id, numero_historia, cedula, nombres, apellidos, fecha_nacimiento, telefono, email, tipo_origen, ciudad_origen, direccion, creado_en " +
                     "FROM pacientes WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPatient(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en PatientDAO.findById: " + e.getMessage());
        }
        return null;
    }

    public Patient findByIdCard(String idCard) {
        if (idCard == null || idCard.trim().isEmpty()) return null;
        String clean = idCard.trim().toLowerCase();

        // 1. Búsqueda exacta
        String sql = "SELECT id, numero_historia, cedula, nombres, apellidos, fecha_nacimiento, telefono, email, tipo_origen, ciudad_origen, direccion, creado_en " +
                     "FROM pacientes WHERE LOWER(cedula) = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clean);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPatient(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en PatientDAO.findByIdCard (exact): " + e.getMessage());
        }

        // 2. Búsqueda con prefijos comunes si el usuario escribió solo el número
        if (!clean.contains("-")) {
            String sqlWithPrefix = "SELECT id, numero_historia, cedula, nombres, apellidos, fecha_nacimiento, telefono, email, tipo_origen, ciudad_origen, direccion, creado_en " +
                                  "FROM pacientes WHERE LOWER(cedula) IN (?, ?, ?, ?, ?, ?) LIMIT 1";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlWithPrefix)) {
                ps.setString(1, "v-" + clean);
                ps.setString(2, "e-" + clean);
                ps.setString(3, "cc-" + clean);
                ps.setString(4, "ti-" + clean);
                ps.setString(5, "j-" + clean);
                ps.setString(6, "p-" + clean);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToPatient(rs);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error en PatientDAO.findByIdCard (prefixes): " + e.getMessage());
            }
        } else {
            // 3. Si el usuario ingresó con prefijo (ej. CC-1098765432 o V-30398619), buscar sin el prefijo por si se guardó plano
            String[] parts = clean.split("-", 2);
            if (parts.length > 1 && !parts[1].isEmpty()) {
                String sqlWithoutPrefix = "SELECT id, numero_historia, cedula, nombres, apellidos, fecha_nacimiento, telefono, email, tipo_origen, ciudad_origen, direccion, creado_en " +
                                          "FROM pacientes WHERE LOWER(cedula) = ? LIMIT 1";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sqlWithoutPrefix)) {
                    ps.setString(1, parts[1].trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return mapResultSetToPatient(rs);
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Error en PatientDAO.findByIdCard (without prefix): " + e.getMessage());
                }
            }
        }

        return null;
    }

    public List<Patient> search(String query) {
        List<Patient> list = new ArrayList<>();
        String sql = "SELECT id, numero_historia, cedula, nombres, apellidos, fecha_nacimiento, telefono, email, tipo_origen, ciudad_origen, direccion, creado_en " +
                     "FROM pacientes WHERE LOWER(cedula) LIKE ? OR LOWER(nombres) LIKE ? OR LOWER(apellidos) LIKE ? OR LOWER(numero_historia) LIKE ? " +
                     "ORDER BY id DESC LIMIT 50";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String term = "%" + query.trim().toLowerCase() + "%";
            ps.setString(1, term);
            ps.setString(2, term);
            ps.setString(3, term);
            ps.setString(4, term);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPatient(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en PatientDAO.search: " + e.getMessage());
        }
        return list;
    }

    public List<Patient> findAll() {
        List<Patient> list = new ArrayList<>();
        String sql = "SELECT id, numero_historia, cedula, nombres, apellidos, fecha_nacimiento, telefono, email, tipo_origen, ciudad_origen, direccion, creado_en " +
                     "FROM pacientes ORDER BY id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToPatient(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error en PatientDAO.findAll: " + e.getMessage());
        }
        return list;
    }

    public String generateNextMedicalRecordNumber() {
        int year = LocalDate.now().getYear();
        String prefix = "HC-" + year + "-";
        String sql = "SELECT COUNT(*) FROM pacientes";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int count = rs.getInt(1) + 1;
                return String.format("%s%04d", prefix, count);
            }
        } catch (SQLException e) {
            System.err.println("Error en PatientDAO.generateNextMedicalRecordNumber: " + e.getMessage());
        }
        return prefix + "0001";
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM pacientes";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error en PatientDAO.countAll: " + e.getMessage());
        }
        return 0;
    }

    public boolean delete(int id) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Eliminar de sala_espera
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sala_espera WHERE paciente_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 2. Eliminar de procedimientos_cotizaciones
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM procedimientos_cotizaciones WHERE paciente_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 3. Eliminar de consultas
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM consultas WHERE paciente_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 4. Eliminar el paciente
            boolean deleted = false;
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM pacientes WHERE id = ?")) {
                ps.setInt(1, id);
                deleted = ps.executeUpdate() > 0;
            }

            conn.commit();
            return deleted;
        } catch (SQLException e) {
            System.err.println("Error en PatientDAO.delete: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Error al hacer rollback: " + rollbackEx.getMessage());
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }

        }
        return false;
    }

    private Patient mapResultSetToPatient(ResultSet rs) throws SQLException {
        LocalDate bDate = DateHelper.parseLocalDate(rs, "fecha_nacimiento");
        LocalDateTime cDate = DateHelper.parseLocalDateTime(rs, "creado_en");
        return new Patient(
                rs.getInt("id"),
                rs.getString("numero_historia"),
                rs.getString("cedula"),
                rs.getString("nombres"),
                rs.getString("apellidos"),
                bDate,
                rs.getString("telefono"),
                rs.getString("email"),
                rs.getString("tipo_origen"),
                rs.getString("ciudad_origen"),
                rs.getString("direccion"),
                cDate
        );
    }
}
