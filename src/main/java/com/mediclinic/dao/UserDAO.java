package com.mediclinic.dao;

import com.mediclinic.database.DatabaseConnection;
import com.mediclinic.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User authenticate(String username, String password) {
        String sql = "SELECT id, username, password_hash, nombre_completo, rol, titulo, especialidad, mpps_matricula " +
                     "FROM usuarios WHERE LOWER(username) = LOWER(?) AND password_hash = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en UserDAO.authenticate: " + e.getMessage());
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT id, username, password_hash, nombre_completo, rol, titulo, especialidad, mpps_matricula " +
                     "FROM usuarios WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en UserDAO.findById: " + e.getMessage());
        }
        return null;
    }

    public User findFirstDoctor() {
        String sql = "SELECT id, username, password_hash, nombre_completo, rol, titulo, especialidad, mpps_matricula " +
                     "FROM usuarios WHERE UPPER(rol) = 'DOCTOR' ORDER BY id ASC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error en UserDAO.findFirstDoctor: " + e.getMessage());
        }
        return null;
    }

    public User findFirstSecretary() {
        String sql = "SELECT id, username, password_hash, nombre_completo, rol, titulo, especialidad, mpps_matricula " +
                     "FROM usuarios WHERE UPPER(rol) = 'SECRETARIA' ORDER BY id ASC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error en UserDAO.findFirstSecretary: " + e.getMessage());
        }
        return null;
    }

    public boolean update(User user) {
        if (user == null) return false;

        // If ID is valid, update by ID
        if (user.getId() > 0) {
            String sql = "UPDATE usuarios SET nombre_completo = ?, titulo = COALESCE(?, titulo, 'Dr.'), especialidad = ?, mpps_matricula = ?, " +
                         "password_hash = CASE WHEN ? IS NOT NULL AND ? != '' THEN ? ELSE password_hash END " +
                         "WHERE id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user.getFullName());
                ps.setString(2, user.getTitle());
                ps.setString(3, user.getSpecialty());
                ps.setString(4, user.getMppsLicense());
                ps.setString(5, user.getPasswordHash());
                ps.setString(6, user.getPasswordHash());
                ps.setString(7, user.getPasswordHash());
                ps.setInt(8, user.getId());
                int affected = ps.executeUpdate();
                if (affected > 0) return true;
            } catch (SQLException e) {
                System.err.println("Error en UserDAO.update by id: " + e.getMessage());
            }
        }

        // Fallback update by username or role
        String fallbackSql = "UPDATE usuarios SET nombre_completo = ?, titulo = COALESCE(?, titulo, 'Dr.'), especialidad = ?, mpps_matricula = ?, " +
                             "password_hash = CASE WHEN ? IS NOT NULL AND ? != '' THEN ? ELSE password_hash END " +
                             "WHERE LOWER(username) = LOWER(?) OR UPPER(rol) = UPPER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(fallbackSql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getTitle());
            ps.setString(3, user.getSpecialty());
            ps.setString(4, user.getMppsLicense());
            ps.setString(5, user.getPasswordHash());
            ps.setString(6, user.getPasswordHash());
            ps.setString(7, user.getPasswordHash());
            ps.setString(8, user.getUsername() != null ? user.getUsername() : "doctor");
            ps.setString(9, user.getRole() != null ? user.getRole() : "DOCTOR");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en UserDAO.update fallback: " + e.getMessage());
        }

        return false;
    }

    public boolean updateProfile(User user) {
        return update(user);
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, password_hash, nombre_completo, rol, titulo, especialidad, mpps_matricula FROM usuarios";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error en UserDAO.findAll: " + e.getMessage());
        }
        return list;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("nombre_completo"),
                rs.getString("rol"),
                rs.getString("titulo"),
                rs.getString("especialidad"),
                rs.getString("mpps_matricula")
        );
    }
}
