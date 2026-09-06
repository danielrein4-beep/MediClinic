package com.mediclinic.services;

import com.mediclinic.dao.ConfigDAO;
import javafx.scene.Scene;

public class ThemeManager {

    private static ThemeManager instance;
    private final ConfigDAO configDAO = new ConfigDAO();
    private String currentTheme = "LIGHT";

    private ThemeManager() {
        this.currentTheme = configDAO.getValue("TEMA_COLOR", "LIGHT");
    }

    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public String getCurrentTheme() {
        return this.currentTheme;
    }

    public boolean isDarkMode() {
        return "DARK".equalsIgnoreCase(this.currentTheme);
    }

    public void setTheme(String theme, Scene scene) {
        this.currentTheme = theme != null && theme.equalsIgnoreCase("DARK") ? "DARK" : "LIGHT";
        configDAO.setValue("TEMA_COLOR", this.currentTheme, "Preferencia de tema visual (LIGHT / DARK)");
        if (scene != null) {
            applyTheme(scene);
        }
    }

    public void toggleTheme(Scene scene) {
        if (isDarkMode()) {
            setTheme("LIGHT", scene);
        } else {
            setTheme("DARK", scene);
        }
    }

    public void applyTheme(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().clear();

        String lightCss = getClass().getResource("/css/style.css") != null
                ? getClass().getResource("/css/style.css").toExternalForm()
                : null;
        String darkCss = getClass().getResource("/css/dark-theme.css") != null
                ? getClass().getResource("/css/dark-theme.css").toExternalForm()
                : null;

        if (isDarkMode() && darkCss != null) {
            scene.getStylesheets().add(darkCss);
        } else if (lightCss != null) {
            scene.getStylesheets().add(lightCss);
        }
    }
}
