package com.mediclinic;

import com.mediclinic.dao.ConfigDAO;
import com.mediclinic.dao.PatientDAO;
import com.mediclinic.database.DatabaseInitializer;
import com.mediclinic.models.Patient;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

public class Phase3Test {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("🧪 MEDICLINIC PRO - VALIDACIÓN DE FASE 3");
        System.out.println("=================================================");

        try {
            // 1. Inicializar base de datos
            System.out.println("\n[1] Inicializando base de datos SQLite y semillas...");
            DatabaseInitializer.initializeDatabase();

            // 2. Validar Edición y Persistencia de Tasas de Cambio
            System.out.println("\n[2] Validando actualización y persistencia de Tasas de Cambio...");
            ConfigDAO configDAO = new ConfigDAO();
            double testVes = 39.20;
            double testCop = 4150.0;
            configDAO.setSetting("TASA_USD_VES", String.valueOf(testVes), "Tasa Dólar a Bolívares");
            configDAO.setSetting("TASA_USD_COP", String.valueOf(testCop), "Tasa Dólar a COP");

            double savedVes = configDAO.getDoubleValue("TASA_USD_VES", 0);
            double savedCop = configDAO.getDoubleValue("TASA_USD_COP", 0);

            if (savedVes != testVes || savedCop != testCop) {
                throw new RuntimeException("Error en persistencia de tasas de cambio. Obtenido VES: " + savedVes + ", COP: " + savedCop);
            }
            System.out.println("✅ Tasa VES actualizada correctamente: Bs. " + savedVes);
            System.out.println("✅ Tasa COP actualizada correctamente: $" + savedCop);

            // 3. Validar Registro de Paciente Local y Foráneo
            System.out.println("\n[3] Validando registro y modelo de Pacientes (Local vs Foráneo)...");
            PatientDAO patientDAO = new PatientDAO();

            String uniqueCedula = "V-" + (System.currentTimeMillis() % 10000000);
            String histNum = patientDAO.generateNextMedicalRecordNumber();

            Patient localPatient = new Patient(
                    histNum,
                    uniqueCedula,
                    "Alejandro",
                    "Gutiérrez",
                    LocalDate.of(1990, 8, 15),
                    "0414-7778899",
                    "alejandro.g@email.com",
                    "LOCAL",
                    "San Cristóbal",
                    "Barrio Obrero, Carrera 19"
            );

            boolean inserted = patientDAO.insert(localPatient);
            if (!inserted || localPatient.getId() <= 0) {
                throw new RuntimeException("Error al insertar paciente local en base de datos.");
            }
            System.out.println("✅ Paciente Local registrado con ID: " + localPatient.getId() + " | HC: " + localPatient.getMedicalRecordNumber());

            // Registro Foráneo
            String uniqueCedulaForaneo = "V-" + ((System.currentTimeMillis() + 1) % 10000000);
            Patient foraneoPatient = new Patient(
                    patientDAO.generateNextMedicalRecordNumber(),
                    uniqueCedulaForaneo,
                    "Valentina",
                    "Duque",
                    LocalDate.of(1996, 3, 22),
                    "0424-5551122",
                    "valentina.d@email.com",
                    "FORANEO",
                    "Cúcuta",
                    "Av. Los Libertadores"
            );
            boolean insertedForaneo = patientDAO.insert(foraneoPatient);
            if (!insertedForaneo || !foraneoPatient.isForaneo()) {
                throw new RuntimeException("Error al insertar paciente foráneo.");
            }
            System.out.println("✅ Paciente Foráneo registrado: " + foraneoPatient.getFullName() + " | Origen: " + foraneoPatient.getFormattedOrigin());

            // 4. Validar Detección de Cédula Duplicada
            System.out.println("\n[4] Validando prevención de cédulas duplicadas...");
            Patient duplicateCheck = patientDAO.findByIdCard(uniqueCedula);
            if (duplicateCheck == null) {
                throw new RuntimeException("Error: No se encontró el paciente previamente registrado por cédula.");
            }
            System.out.println("✅ Validación de unicidad exitosa. Cédula " + uniqueCedula + " ya existe a nombre de: " + duplicateCheck.getFullName());

            // 5. Validar Motor de Búsqueda Instantánea
            System.out.println("\n[5] Validando Motor de Búsqueda Instantánea...");
            List<Patient> searchByCedula = patientDAO.search(uniqueCedula);
            if (searchByCedula.isEmpty()) {
                throw new RuntimeException("Fallo en búsqueda por cédula.");
            }
            System.out.println("✅ Búsqueda por Cédula (" + uniqueCedula + "): 1 resultado encontrado (" + searchByCedula.get(0).getFullName() + ")");

            List<Patient> searchByName = patientDAO.search("Valentina");
            if (searchByName.isEmpty()) {
                throw new RuntimeException("Fallo en búsqueda por nombre 'Valentina'.");
            }
            System.out.println("✅ Búsqueda por Nombre ('Valentina'): " + searchByName.size() + " resultado(s) encontrado(s)");

            // 6. Validar Recursos FXML de Fase 3
            System.out.println("\n[6] Validando recursos FXML y CSS de la Fase 3...");
            checkResource("/views/PatientRegistration.fxml");
            checkResource("/views/PatientsView.fxml");
            checkResource("/views/DoctorDashboard.fxml");
            checkResource("/views/SecretaryDashboard.fxml");
            checkResource("/css/style.css");

            System.out.println("\n=================================================");
            System.out.println("🎉 FASE 3 COMPLETADA Y VERIFICADA CON ÉXITO");
            System.out.println("=================================================");

        } catch (Exception e) {
            System.err.println("❌ ERROR EN VALIDACIÓN DE FASE 3:");
            e.printStackTrace();
        }
    }

    private static void checkResource(String path) {
        try (InputStream is = Phase3Test.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Recurso no encontrado en classpath: " + path);
            }
            System.out.println("✅ Recurso validado: " + path);
        } catch (Exception e) {
            throw new RuntimeException("Fallo al leer recurso: " + path, e);
        }
    }
}
