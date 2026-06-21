package com.studentregistrationsystem.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Student Model Class - Represents a student entity
 * Handles data storage and CSV conversion
 */
public class Student {
    // Instance variables
    private int id;
    private String studentId;        // New unique student ID field
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String department;
    private String profilePicturePath;
    private LocalDate registrationDate;

    // Formatters
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Constructor - initializes registration date
    public Student() {
        this.registrationDate = LocalDate.now();
    }

    // ========== GETTERS ==========
    public int getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return firstName + " " + lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getDepartment() { return department; }
    public String getProfilePicturePath() { return profilePicturePath; }
    public LocalDate getRegistrationDate() { return registrationDate; }

    // ========== SETTERS ==========
    public void setId(int id) { this.id = id; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public void setDepartment(String department) { this.department = department; }
    public void setProfilePicturePath(String profilePicturePath) { this.profilePicturePath = profilePicturePath; }
    public void setRegistrationDate(LocalDate registrationDate) { this.registrationDate = registrationDate; }

    // ========== CSV CONVERSION ==========
    /**
     * Converts student object to CSV string format
     */
    public String toCSV() {
        return String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                id,
                escapeCSV(studentId),
                escapeCSV(firstName),
                escapeCSV(lastName),
                escapeCSV(email),
                escapeCSV(phone != null ? phone : ""),
                dateOfBirth != null ? dateOfBirth.format(DATE_FORMATTER) : "",
                escapeCSV(department != null ? department : ""),
                escapeCSV(profilePicturePath != null ? profilePicturePath : ""),
                registrationDate != null ? registrationDate.format(DATE_FORMATTER) : ""
        );
    }

    /**
     * Escapes special characters for CSV format
     */
    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Creates Student object from CSV line
     */
    public static Student fromCSV(String line) {
        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        if (parts.length < 10) return null;

        Student student = new Student();
        try {
            student.setId(Integer.parseInt(parts[0].trim()));
            student.setStudentId(unescapeCSV(parts[1].trim()));
            student.setFirstName(unescapeCSV(parts[2].trim()));
            student.setLastName(unescapeCSV(parts[3].trim()));
            student.setEmail(unescapeCSV(parts[4].trim()));
            student.setPhone(unescapeCSV(parts[5].trim()));
            if (parts[6].trim().length() > 0) {
                student.setDateOfBirth(LocalDate.parse(parts[6].trim(), DATE_FORMATTER));
            }
            student.setDepartment(unescapeCSV(parts[7].trim()));
            student.setProfilePicturePath(unescapeCSV(parts[8].trim()));
            if (parts[9].trim().length() > 0) {
                student.setRegistrationDate(LocalDate.parse(parts[9].trim(), DATE_FORMATTER));
            }
        } catch (Exception e) {
            return null;
        }
        return student;
    }

    /**
     * Unescapes CSV values
     */
    private static String unescapeCSV(String value) {
        if (value == null) return null;
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
            value = value.replace("\"\"", "\"");
        }
        return value;
    }

    /**
     * Generates a unique student ID
     * Format: STU-YYYY-XXXX (e.g., STU-2024-0001)
     */
    public static String generateStudentId(int sequentialId) {
        int year = LocalDate.now().getYear();
        return String.format("STU-%d-%04d", year, sequentialId);
    }
}