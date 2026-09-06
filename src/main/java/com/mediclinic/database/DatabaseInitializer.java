package com.mediclinic.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public class DatabaseInitializer {

    public static void initializeDatabase() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Tabla de Usuarios y Roles
            stmt.execute("CREATE TABLE IF NOT EXISTS usuarios (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT NOT NULL UNIQUE," +
                    "password_hash TEXT NOT NULL," +
                    "nombre_completo TEXT NOT NULL," +
                    "rol TEXT NOT NULL CHECK(rol IN ('DOCTOR', 'SECRETARIA', 'ADMIN'))," +
                    "titulo TEXT DEFAULT 'Dr.'," +
                    "especialidad TEXT," +
                    "mpps_matricula TEXT," +
                    "creado_en DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 2. Tabla de Pacientes
            stmt.execute("CREATE TABLE IF NOT EXISTS pacientes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "numero_historia TEXT NOT NULL UNIQUE," +
                    "cedula TEXT NOT NULL UNIQUE," +
                    "nombres TEXT NOT NULL," +
                    "apellidos TEXT NOT NULL," +
                    "fecha_nacimiento DATE," +
                    "telefono TEXT," +
                    "email TEXT," +
                    "tipo_origen TEXT NOT NULL DEFAULT 'LOCAL' CHECK(tipo_origen IN ('LOCAL', 'FORANEO'))," +
                    "ciudad_origen TEXT," +
                    "direccion TEXT," +
                    "creado_en DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 3. Tabla de Consultas e Historias Clínicas
            stmt.execute("CREATE TABLE IF NOT EXISTS consultas (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "paciente_id INTEGER NOT NULL," +
                    "doctor_id INTEGER NOT NULL," +
                    "fecha_hora DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "motivo_consulta TEXT NOT NULL," +
                    "anotaciones_evolucion TEXT," +
                    "diagnostico TEXT NOT NULL," +
                    "tratamiento_receta TEXT," +
                    "pa TEXT," +
                    "fc INTEGER," +
                    "peso REAL," +
                    "talla REAL," +
                    "temperatura REAL," +
                    "proxima_cita DATE," +
                    "informe_pdf_path TEXT," +
                    "observacion_fisica TEXT," +
                    "FOREIGN KEY (paciente_id) REFERENCES pacientes(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (doctor_id) REFERENCES usuarios(id)" +
                    ");");

            // Asegurar columna observacion_fisica en bases de datos existentes
            try {
                stmt.execute("ALTER TABLE consultas ADD COLUMN observacion_fisica TEXT;");
            } catch (SQLException ignored) {}

            // 4. Tabla de Procedimientos y Cotizaciones Multidivisa
            stmt.execute("CREATE TABLE IF NOT EXISTS procedimientos_cotizaciones (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "paciente_id INTEGER NOT NULL," +
                    "tipo_procedimiento TEXT NOT NULL," +
                    "descripcion TEXT," +
                    "monto_usd REAL NOT NULL DEFAULT 0.0," +
                    "tasa_ves REAL NOT NULL DEFAULT 36.5," +
                    "monto_ves REAL NOT NULL DEFAULT 0.0," +
                    "tasa_cop REAL NOT NULL DEFAULT 3950.0," +
                    "monto_cop REAL NOT NULL DEFAULT 0.0," +
                    "estado TEXT NOT NULL DEFAULT 'COTIZADA' CHECK(estado IN ('COTIZADA', 'PLANIFICADA', 'REALIZADA', 'CANCELADA'))," +
                    "fecha_planificada DATETIME," +
                    "notas TEXT," +
                    "creado_en DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (paciente_id) REFERENCES pacientes(id) ON DELETE CASCADE" +
                    ");");

            // 5. Tabla de Sala de Espera (Recepción)
            stmt.execute("CREATE TABLE IF NOT EXISTS sala_espera (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "paciente_id INTEGER NOT NULL," +
                    "fecha_llegada DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "motivo TEXT," +
                    "estado TEXT NOT NULL DEFAULT 'EN_ESPERA' CHECK(estado IN ('EN_ESPERA', 'EN_CONSULTA', 'ATENDIDO', 'CANCELADO'))," +
                    "FOREIGN KEY (paciente_id) REFERENCES pacientes(id) ON DELETE CASCADE" +
                    ");");

            // 6. Tabla de Configuración Global del Sistema
            stmt.execute("CREATE TABLE IF NOT EXISTS configuracion (" +
                    "clave TEXT PRIMARY KEY," +
                    "valor TEXT NOT NULL," +
                    "descripcion TEXT," +
                    "actualizado_en DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 7. Tabla de Agenda de Citas Médicas Compartida
            stmt.execute("CREATE TABLE IF NOT EXISTS agenda_citas (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "paciente_id INTEGER," +
                    "cedula TEXT NOT NULL," +
                    "nombres TEXT NOT NULL," +
                    "apellidos TEXT NOT NULL," +
                    "telefono TEXT NOT NULL," +
                    "fecha_cita DATE NOT NULL," +
                    "hora_cita TEXT NOT NULL," +
                    "motivo TEXT," +
                    "estado TEXT NOT NULL DEFAULT 'PENDIENTE' CHECK(estado IN ('PENDIENTE', 'CONFIRMADA', 'ATENDIDA', 'CANCELADA'))," +
                    "notas TEXT," +
                    "creado_en DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (paciente_id) REFERENCES pacientes(id) ON DELETE SET NULL" +
                    ");");

            // 8. Tabla de Pagos Diarios y Facturación en Caja
            stmt.execute("CREATE TABLE IF NOT EXISTS pagos_diarios (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "paciente_id INTEGER NOT NULL," +
                    "sala_espera_id INTEGER," +
                    "fecha_pago DATE NOT NULL," +
                    "hora_pago TEXT NOT NULL," +
                    "medio_pago TEXT NOT NULL CHECK(medio_pago IN ('Efectivo', 'Punto de Venta', 'Pago Móvil', 'Zelle', 'Transferencia'))," +
                    "moneda TEXT NOT NULL CHECK(moneda IN ('USD', 'VES', 'COP'))," +
                    "monto REAL NOT NULL," +
                    "notas TEXT," +
                    "creado_en DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (paciente_id) REFERENCES pacientes(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (sala_espera_id) REFERENCES sala_espera(id) ON DELETE SET NULL" +
                    ");");

            // 9. Tabla de Fechas Bloqueadas / No Disponibles en Agenda
            stmt.execute("CREATE TABLE IF NOT EXISTS agenda_bloqueos (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "fecha DATE NOT NULL UNIQUE," +
                    "motivo TEXT NOT NULL," +
                    "bloqueado_por TEXT," +
                    "creado_en DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 10. Tabla de Cierres Financieros Diarios / Auditoría
            stmt.execute("CREATE TABLE IF NOT EXISTS cierres_financieros (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "fecha_cierre DATE NOT NULL," +
                    "hora_cierre TEXT NOT NULL," +
                    "total_usd REAL NOT NULL DEFAULT 0.0," +
                    "total_ves REAL NOT NULL DEFAULT 0.0," +
                    "total_cop REAL NOT NULL DEFAULT 0.0," +
                    "total_pacientes INTEGER NOT NULL DEFAULT 0," +
                    "cerrado_por TEXT NOT NULL," +
                    "notas TEXT," +
                    "creado_en DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // 11. Creación de Índices de Alto Rendimiento
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pacientes_cedula ON pacientes(cedula);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pacientes_historia ON pacientes(numero_historia);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_consultas_paciente ON consultas(paciente_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_consultas_fecha ON consultas(fecha_hora);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_procedimientos_paciente ON procedimientos_cotizaciones(paciente_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_procedimientos_estado ON procedimientos_cotizaciones(estado);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pagos_fecha ON pagos_diarios(fecha_pago);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pagos_paciente ON pagos_diarios(paciente_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_agenda_bloqueos_fecha ON agenda_bloqueos(fecha);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_cierres_fecha ON cierres_financieros(fecha_cierre);");

            // 8. Carga de Datos Iniciales (Seed Data)
            seedInitialData(conn);

            System.out.println("Base de datos SQLite inicializada exitosamente en: " + DatabaseConnection.getDatabasePath());

        } catch (SQLException e) {
            System.err.println("Error al inicializar la base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void seedInitialData(Connection conn) throws SQLException {
        // Verificar si ya existen usuarios
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM usuarios")) {
            if (rs.next() && rs.getInt(1) == 0) {
                // Insertar Doctor Mario Roa
                String insertDoc = "INSERT INTO usuarios (username, password_hash, nombre_completo, rol, titulo, especialidad, mpps_matricula) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertDoc)) {
                    ps.setString(1, "doctor");
                    ps.setString(2, "1234");
                    ps.setString(3, "Mario Roa");
                    ps.setString(4, "DOCTOR");
                    ps.setString(5, "Dr.");
                    ps.setString(6, "Medicina General y Cirugía");
                    ps.setString(7, "MPPS-84920 / Col. Médicos 14.502");
                    ps.executeUpdate();
                }

                // Insertar Secretaria Niccolle Medina
                try (PreparedStatement ps = conn.prepareStatement(insertDoc)) {
                    ps.setString(1, "secretaria");
                    ps.setString(2, "1234");
                    ps.setString(3, "Niccolle Medina");
                    ps.setString(4, "SECRETARIA");
                    ps.setString(5, "Secretaria");
                    ps.setString(6, "Recepción y Atención Médica");
                    ps.setString(7, "REC-01");
                    ps.executeUpdate();
                }
            } else {
                // Asegurar que los perfiles existentes tengan los nombres actualizados
                try (Statement updateStmt = conn.createStatement()) {
                    updateStmt.executeUpdate("UPDATE usuarios SET nombre_completo = 'Mario Roa', titulo = 'Dr.' WHERE username = 'doctor'");
                    updateStmt.executeUpdate("UPDATE usuarios SET nombre_completo = 'Niccolle Medina', titulo = 'Secretaria' WHERE username = 'secretaria'");
                }
            }
        }

        // Insertar Tasas y Configuración por Defecto
        String insertConfig = "INSERT OR IGNORE INTO configuracion (clave, valor, descripcion) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertConfig)) {
            // Tasa USD a Bolívares (VES)
            ps.setString(1, "TASA_USD_VES");
            ps.setString(2, "38.50");
            ps.setString(3, "Tasa de cambio Dólar a Bolívares");
            ps.executeUpdate();

            // Tasa USD a Pesos Colombianos (COP)
            ps.setString(1, "TASA_USD_COP");
            ps.setString(2, "4100.00");
            ps.setString(3, "Tasa de cambio Dólar a Pesos Colombianos");
            ps.executeUpdate();

            // Nombre de la Clínica / Consultorio
            ps.setString(1, "CLINICA_NOMBRE");
            ps.setString(2, "Centro Médico Especializado MediClinic");
            ps.setString(3, "Nombre comercial del consultorio o clínica");
            ps.executeUpdate();

            // Teléfono del Consultorio
            ps.setString(1, "CLINICA_TELEFONO");
            ps.setString(2, "+58 414-5551234");
            ps.setString(3, "Teléfono de contacto de recepción");
            ps.executeUpdate();

            // Dirección
            ps.setString(1, "CLINICA_DIRECCION");
            ps.setString(2, "Av. Principal, Edif. Torre Médica, Consultorio 4-B");
            ps.setString(3, "Dirección física");
            ps.executeUpdate();
        }

        // Sembrar Paciente de Prueba para validaciones
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM pacientes")) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insertPac = "INSERT INTO pacientes (numero_historia, cedula, nombres, apellidos, fecha_nacimiento, telefono, email, tipo_origen, ciudad_origen, direccion) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertPac)) {
                    ps.setString(1, "HC-2026-0001");
                    ps.setString(2, "V-18456789");
                    ps.setString(3, "Carlos Eduardo");
                    ps.setString(4, "Mendoza Paredes");
                    ps.setString(5, "1988-05-14");
                    ps.setString(6, "0412-1234567");
                    ps.setString(7, "carlos.mendoza@email.com");
                    ps.setString(8, "LOCAL");
                    ps.setString(9, "San Cristóbal");
                    ps.setString(10, "Urb. Pirineos, Calle 3");
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(insertPac)) {
                    ps.setString(1, "HC-2026-0002");
                    ps.setString(2, "V-22987654");
                    ps.setString(3, "Mariana Alejandra");
                    ps.setString(4, "Rojas Gil");
                    ps.setString(5, "1995-11-20");
                    ps.setString(6, "0424-9876543");
                    ps.setString(7, "mariana.rojas@email.com");
                    ps.setString(8, "FORANEO");
                    ps.setString(9, "Mérida");
                    ps.setString(10, "Sector Hoyo del Rincón");
                    ps.executeUpdate();
                }
            }
        }

        // Distribuir fechas de cierres financieros para que cada filtro de tiempo refleje valores distintos
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM cierres_financieros")) {
            if (rs.next()) {
                int totalRows = rs.getInt(1);
                if (totalRows > 1) {
                    try (Statement selStmt = conn.createStatement();
                         ResultSet allRs = selStmt.executeQuery("SELECT id FROM cierres_financieros ORDER BY id ASC")) {
                        java.util.List<Integer> ids = new java.util.ArrayList<>();
                        while (allRs.next()) {
                            ids.add(allRs.getInt("id"));
                        }
                        int[] dayOffsets = {60, 50, 42, 25, 18, 12, 5, 2, 0};
                        for (int i = 0; i < ids.size(); i++) {
                            int offsetIdx = ids.size() - 1 - i;
                            int offset = (offsetIdx >= 0 && offsetIdx < dayOffsets.length) ? dayOffsets[offsetIdx] : (offsetIdx * 5);
                            LocalDate d = LocalDate.now().minusDays(offset);
                            try (PreparedStatement updatePs = conn.prepareStatement("UPDATE cierres_financieros SET fecha_cierre = ? WHERE id = ?")) {
                                updatePs.setString(1, DateHelper.formatLocalDate(d));
                                updatePs.setInt(2, ids.get(i));
                                updatePs.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
