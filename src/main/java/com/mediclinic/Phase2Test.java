package com.mediclinic;

import com.mediclinic.dao.UserDAO;
import com.mediclinic.database.DatabaseInitializer;
import com.mediclinic.models.User;
import com.mediclinic.services.SessionManager;

import java.io.InputStream;

public class Phase2Test {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("🧪 MEDICLINIC PRO - VALIDACIÓN DE FASE 2");
        System.out.println("=================================================");

        try {
            // 1. Inicializar base de datos
            System.out.println("\n[1] Inicializando base de datos SQLite...");
            DatabaseInitializer.initializeDatabase();

            // 2. Validar autenticación y SessionManager con Doctor
            System.out.println("\n[2] Validando autenticación y sesión del Doctor...");
            UserDAO userDAO = new UserDAO();
            User doctor = userDAO.authenticate("doctor", "1234");
            if (doctor == null || !doctor.isDoctor()) {
                throw new RuntimeException("Error: Doctor no encontrado o rol inválido.");
            }

            SessionManager session = SessionManager.getInstance();
            session.setCurrentUser(doctor);

            String docGreeting = session.getDynamicGreeting();
            System.out.println("✅ Usuario logueado: " + doctor.getFullName());
            System.out.println("✅ Rol: " + doctor.getRole());
            System.out.println("✅ Saludo dinámico generado: \"" + docGreeting + "\"");
            if (!docGreeting.equals("¡Bienvenido Dr. Mario Roa!")) {
                throw new RuntimeException("Fallo en el formato del saludo del doctor! Esperado: '¡Bienvenido Dr. Mario Roa!', Obtenido: '" + docGreeting + "'");
            }

            // 3. Validar autenticación y SessionManager con Secretaria
            System.out.println("\n[3] Validando autenticación y sesión de la Secretaria...");
            User secretary = userDAO.authenticate("secretaria", "1234");
            if (secretary == null || !secretary.isSecretary()) {
                throw new RuntimeException("Error: Secretaria no encontrada o rol inválido.");
            }

            session.setCurrentUser(secretary);
            String secGreeting = session.getDynamicGreeting();
            System.out.println("✅ Usuario logueado: " + secretary.getFullName());
            System.out.println("✅ Rol: " + secretary.getRole());
            System.out.println("✅ Saludo dinámico generado: \"" + secGreeting + "\"");
            if (!secGreeting.equals("¡Bienvenida Secretaria Niccolle Medina!")) {
                throw new RuntimeException("Fallo en el formato del saludo de la secretaria! Esperado: '¡Bienvenida Secretaria Niccolle Medina!', Obtenido: '" + secGreeting + "'");
            }

            // 4. Validar recursos FXML y CSS en el Classpath
            System.out.println("\n[4] Validando presencia e integridad de recursos FXML y CSS...");
            checkResource("/views/Login.fxml");
            checkResource("/views/DoctorDashboard.fxml");
            checkResource("/views/SecretaryDashboard.fxml");
            checkResource("/css/style.css");

            // 5. Validar logout
            session.logout();
            if (session.isLoggedIn()) {
                throw new RuntimeException("Error en logout: La sesión aún permanece activa.");
            }
            System.out.println("✅ Cierre de sesión (logout) verificado exitosamente.");

            System.out.println("\n=================================================");
            System.out.println("🎉 FASE 2 COMPLETADA Y VERIFICADA CON ÉXITO");
            System.out.println("=================================================");

        } catch (Exception e) {
            System.err.println("❌ ERROR EN VALIDACIÓN DE FASE 2:");
            e.printStackTrace();
        }
    }

    private static void checkResource(String path) {
        try (InputStream is = Phase2Test.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Recurso no encontrado en classpath: " + path);
            }
            System.out.println("✅ Recurso validado: " + path);
        } catch (Exception e) {
            throw new RuntimeException("Fallo al leer recurso: " + path, e);
        }
    }
}
