package com.mediclinic.dao;

import com.mediclinic.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ConfigDAO {

    public String getValue(String key, String defaultValue) {
        String sql = "SELECT valor FROM configuracion WHERE clave = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("valor");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en ConfigDAO.getValue: " + e.getMessage());
        }
        return defaultValue;
    }

    public double getDoubleValue(String key, double defaultValue) {
        String val = getValue(key, null);
        if (val != null) {
            try {
                return Double.parseDouble(val);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public boolean setSetting(String key, String value, String description) {
        String updateSql = "UPDATE configuracion SET valor = ?, descripcion = COALESCE(?, descripcion) WHERE clave = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, value);
            ps.setString(2, description);
            ps.setString(3, key);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error en ConfigDAO.update: " + e.getMessage());
        }

        String insertSql = "INSERT INTO configuracion (clave, valor, descripcion) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, description);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en ConfigDAO.insert: " + e.getMessage());
        }
        return false;
    }

    public boolean setValue(String key, String value, String description) {
        return setSetting(key, value, description);
    }

    public boolean setValue(String key, String value) {
        return setSetting(key, value, null);
    }

    public Map<String, String> getAllSettings() {
        Map<String, String> map = new HashMap<>();
        String sql = "SELECT clave, valor FROM configuracion";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("clave"), rs.getString("valor"));
            }
        } catch (SQLException e) {
            System.err.println("Error en ConfigDAO.getAllSettings: " + e.getMessage());
        }
        return map;
    }
}
