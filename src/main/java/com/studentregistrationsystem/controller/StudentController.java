package com.studentregistrationsystem.controller;

import com.studentregistrationsystem.model.Student;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;

import java.io.*;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Student Controller - Handles all business logic
 * Manages CRUD operations, validation, file I/O, and search
 */
public class StudentController {
    // Data storage
    private ObservableList<Student> studentList;
    private static final String CSV_FILE = "students.csv";
    private int nextId = 1;
    private int nextSequentialNumber = 1;

    // Validation patterns
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z]{2,50}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^STU-\\d{4}-\\d{4}$");
    // Phone pattern: supports multiple formats
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(\\+251|0)?[9]\\d{8}$|^[0-9]{10,15}$|^\\(\\+251\\)\\s?[9]\\d{8}$|^[0-9]{3}[-]?[0-9]{3}[-]?[0-9]{4}$"
    );

    // Constructor
    public StudentController() {
        this.studentList = FXCollections.observableArrayList();
        loadData();
    }

    // ========== DATA ACCESS ==========
    public ObservableList<Student> getStudentList() { return studentList; }

    // ========== VALIDATION METHODS ==========

    /**
     * Validates all student data before save/update
     * @return error message or null if valid
     */
    public String validateStudent(Student student) {
        // First Name validation
        if (student.getFirstName() == null || student.getFirstName().trim().isEmpty()) {
            return "First name is required.";
        }
        if (!NAME_PATTERN.matcher(student.getFirstName()).matches()) {
            return "First name must contain only letters (2-50 characters).";
        }

        // Last Name validation
        if (student.getLastName() == null || student.getLastName().trim().isEmpty()) {
            return "Last name is required.";
        }
        if (!NAME_PATTERN.matcher(student.getLastName()).matches()) {
            return "Last name must contain only letters (2-50 characters).";
        }

        // Student ID validation - check for duplicates
        if (student.getStudentId() == null || student.getStudentId().trim().isEmpty()) {
            return "Student ID is required.";
        }
        if (!STUDENT_ID_PATTERN.matcher(student.getStudentId()).matches()) {
            return "Invalid Student ID format. Expected format: STU-YYYY-XXXX (e.g., STU-2024-0001)";
        }
        if (isStudentIdExists(student.getStudentId(), student.getId())) {
            return "Student ID already exists. Please use a unique Student ID.";
        }

        // Email validation
        if (student.getEmail() == null || student.getEmail().trim().isEmpty()) {
            return "Email address is required.";
        }
        if (!EMAIL_PATTERN.matcher(student.getEmail()).matches()) {
            return "Please enter a valid email address (e.g., name@domain.com).";
        }
        if (isEmailExists(student.getEmail(), student.getId())) {
            return "Email address already exists in the system.";
        }

        // Phone validation (optional but must be valid if provided)
        if (student.getPhone() != null && !student.getPhone().trim().isEmpty()) {
            String phone = student.getPhone().replaceAll("\\s", ""); // Remove spaces
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                return "Invalid phone number. Valid formats: +2519XXXXXXXX, +251-9XXXXXXXX, 09XXXXXXXX, 09X-XXX-XXXX, or 10-15 digits.";
            }
        }

        // Date of Birth validation
        if (student.getDateOfBirth() == null) {
            return "Date of birth is required.";
        }
        if (student.getDateOfBirth().isAfter(LocalDate.now())) {
            return "Date of birth cannot be in the future.";
        }
        if (student.getDateOfBirth().isAfter(LocalDate.now().minusYears(5))) {
            return "Student must be at least 5 years old.";
        }
        if (student.getDateOfBirth().isBefore(LocalDate.now().minusYears(100))) {
            return "Please enter a valid date of birth.";
        }

        // Department validation
        if (student.getDepartment() == null || student.getDepartment().trim().isEmpty()) {
            return "Department is required.";
        }

        return null; // All validations passed
    }

    /**
     * Checks if student ID already exists (excluding current student for update)
     */
    private boolean isStudentIdExists(String studentId, int excludeId) {
        return studentList.stream()
                .anyMatch(s -> s.getStudentId().equalsIgnoreCase(studentId) && s.getId() != excludeId);
    }

    /**
     * Checks if email already exists (excluding current student for update)
     */
    private boolean isEmailExists(String email, int excludeId) {
        return studentList.stream()
                .anyMatch(s -> s.getEmail().equalsIgnoreCase(email) && s.getId() != excludeId);
    }

    /**
     * Generates next sequential number for Student ID
     */
    public int getNextSequentialNumber() {
        return nextSequentialNumber;
    }

    // ========== CRUD OPERATIONS ==========

    /**
     * Adds new student to the system
     */
    public boolean addStudent(Student student) {
        String validationError = validateStudent(student);
        if (validationError != null) {
            showAlert("Validation Error", validationError, Alert.AlertType.ERROR);
            return false;
        }

        student.setId(nextId++);
        studentList.add(student);
        saveData();
        showAlert("Success", "Student added successfully!", Alert.AlertType.INFORMATION);
        return true;
    }

    /**
     * Updates existing student
     */
    public boolean updateStudent(Student student) {
        String validationError = validateStudent(student);
        if (validationError != null) {
            showAlert("Validation Error", validationError, Alert.AlertType.ERROR);
            return false;
        }

        int index = findStudentIndex(student.getId());
        if (index != -1) {
            studentList.set(index, student);
            saveData();
            showAlert("Success", "Student updated successfully!", Alert.AlertType.INFORMATION);
            return true;
        }
        return false;
    }

    /**
     * Deletes student from system
     */
    public boolean deleteStudent(Student student) {
        boolean removed = studentList.remove(student);
        if (removed) {
            saveData();
            showAlert("Success", "Student deleted successfully!", Alert.AlertType.INFORMATION);
            return true;
        }
        return false;
    }

    /**
     * Finds student index by ID
     */
    private int findStudentIndex(int id) {
        for (int i = 0; i < studentList.size(); i++) {
            if (studentList.get(i).getId() == id) return i;
        }
        return -1;
    }

    // ========== SEARCH FUNCTIONALITY ==========

    /**
     * Searches students by keyword (ID, Student ID, name, email, department)
     */
    public ObservableList<Student> searchStudents(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return studentList;
        }

        String lowerKeyword = keyword.toLowerCase().trim();
        return FXCollections.observableArrayList(
                studentList.stream()
                        .filter(s -> s.getFullName().toLowerCase().contains(lowerKeyword) ||
                                s.getEmail().toLowerCase().contains(lowerKeyword) ||
                                (s.getDepartment() != null && s.getDepartment().toLowerCase().contains(lowerKeyword)) ||
                                String.valueOf(s.getId()).contains(lowerKeyword) ||
                                (s.getStudentId() != null && s.getStudentId().toLowerCase().contains(lowerKeyword)))
                        .collect(Collectors.toList())
        );
    }

    // ========== PAGINATION ==========

    /**
     * Returns paginated subset of students
     */
    public List<Student> getPaginatedStudents(ObservableList<Student> source, int page, int pageSize) {
        int start = page * pageSize;
        int end = Math.min(start + pageSize, source.size());
        return (start >= source.size()) ? List.of() : source.subList(start, end);
    }

    /**
     * Calculates total pages based on page size
     */
    public int getTotalPages(ObservableList<Student> source, int pageSize) {
        return (int) Math.ceil((double) source.size() / pageSize);
    }

    // ========== FILE I/O ==========

    /**
     * Loads student data from CSV file
     */
    private void loadData() {
        File file = new File(CSV_FILE);
        if (!file.exists()) {
            createCSVHeader();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean firstLine = true;
            int maxId = 0;
            int maxSequential = 0;

            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }
                Student student = Student.fromCSV(line);
                if (student != null) {
                    studentList.add(student);
                    maxId = Math.max(maxId, student.getId());

                    // Extract sequential number from Student ID
                    if (student.getStudentId() != null && student.getStudentId().matches(".*-(\\d{4})$")) {
                        String[] parts = student.getStudentId().split("-");
                        if (parts.length >= 3) {
                            try {
                                int seq = Integer.parseInt(parts[2]);
                                maxSequential = Math.max(maxSequential, seq);
                            } catch (NumberFormatException e) {}
                        }
                    }
                }
            }
            nextId = maxId + 1;
            nextSequentialNumber = maxSequential + 1;
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Creates CSV file with header
     */
    private void createCSVHeader() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_FILE))) {
            writer.println("ID,StudentID,FirstName,LastName,Email,Phone,DateOfBirth,Department,ProfilePicturePath,RegistrationDate");
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Saves student data to CSV file
     */
    private void saveData() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_FILE))) {
            writer.println("ID,StudentID,FirstName,LastName,Email,Phone,DateOfBirth,Department,ProfilePicturePath,RegistrationDate");
            for (Student student : studentList) {
                writer.println(student.toCSV());
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ========== UTILITY METHODS ==========

    /**
     * Shows alert dialog
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}