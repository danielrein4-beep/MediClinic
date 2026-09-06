package com.mediclinic.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateHelper {

    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static LocalDate parseLocalDate(ResultSet rs, String columnName) {
        try {
            String val = rs.getString(columnName);
            if (val == null || val.trim().isEmpty()) return null;
            val = val.trim();
            if (val.contains(" ")) {
                val = val.split(" ")[0];
            }
            if (val.contains("T")) {
                val = val.split("T")[0];
            }
            return LocalDate.parse(val, ISO_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    public static LocalDateTime parseLocalDateTime(ResultSet rs, String columnName) {
        try {
            String val = rs.getString(columnName);
            if (val == null || val.trim().isEmpty()) return null;
            val = val.trim();
            if (val.contains("T")) {
                return LocalDateTime.parse(val);
            }
            if (val.length() == 19) {
                return LocalDateTime.parse(val, ISO_LOCAL_DATE_TIME);
            }
            if (val.length() == 10) {
                return LocalDate.parse(val, ISO_DATE).atStartOfDay();
            }
            return LocalDateTime.parse(val.replace(" ", "T"));
        } catch (Exception e) {
            return null;
        }
    }

    public static String formatLocalDateTime(LocalDateTime ldt) {
        if (ldt == null) return null;
        return ldt.format(ISO_LOCAL_DATE_TIME);
    }

    public static String formatLocalDate(LocalDate ld) {
        if (ld == null) return null;
        return ld.format(ISO_DATE);
    }
}
