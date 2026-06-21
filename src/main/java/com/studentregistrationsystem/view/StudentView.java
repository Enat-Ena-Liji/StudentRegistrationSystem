package com.studentregistrationsystem.view;

import com.studentregistrationsystem.controller.StudentController;
import com.studentregistrationsystem.model.Student;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Main View Class - Handles all UI components
 * Manages table display, forms, and user interactions
 */
public class StudentView {
    // Core components
    private final Stage stage;
    private final StudentController controller;

    // UI Components
    private TableView<Student> tableView;
    private TextField searchField;
    private Label pageInfoLabel;
    private ComboBox<Integer> pageSizeCombo;
    private Button firstBtn, prevBtn, nextBtn, lastBtn;
    private Label totalLabel;

    // State variables
    private int currentPage = 0;
    private int pageSize = 10;
    private ObservableList<Student> currentDisplayList;

    // Constants
    private static final String IMAGE_DIR = "student_photos";

    public StudentView(Stage stage, StudentController controller) {
        this.stage = stage;
        this.controller = controller;
        this.currentDisplayList = controller.getStudentList();
        createImageDirectory();
        initUI();
    }

    private void createImageDirectory() {
        File dir = new File(IMAGE_DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    private void initUI() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #667eea 0%, #764ba2 100%);");

        root.setTop(createHeader());
        root.setCenter(createTableSection());
        root.setBottom(createPaginationBar());

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double width = screenBounds.getWidth() * 0.9;
        double height = screenBounds.getHeight() * 0.85;

        Scene scene = new Scene(root, width, height);

        stage.setX((screenBounds.getWidth() - width) / 2);
        stage.setY((screenBounds.getHeight() - height) / 2);

        String cssPath = getClass().getResource("/styles.css").toExternalForm();
        if (cssPath != null) scene.getStylesheets().add(cssPath);

        stage.setTitle("Student Registration System");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.show();

        loadPageData();

        controller.getStudentList().addListener((javafx.collections.ListChangeListener.Change<? extends Student> c) -> {
            updateTotalLabel();
        });
        updateTotalLabel();
    }

    private void updateTotalLabel() {
        totalLabel.setText("Total Students: " + controller.getStudentList().size());
    }

    private VBox createHeader() {
        VBox header = new VBox(15);
        header.setStyle("-fx-background-color: white; -fx-padding: 20 30; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 5);");

        HBox titleBox = new HBox(15);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("🎓");
        iconLabel.setStyle("-fx-font-size: 40px;");

        VBox titleText = new VBox(5);
        Label mainTitle = new Label("STUDENT REGISTRATION SYSTEM");
        mainTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label subtitle = new Label("Manage student records efficiently");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        titleText.getChildren().addAll(mainTitle, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("+ Add New Student");
        addBtn.setStyle("-fx-background-color: linear-gradient(to right, #27ae60, #2ecc71); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 25; -fx-background-radius: 25; -fx-font-size: 14px; -fx-cursor: hand;");
        addBtn.setOnAction(e -> showAddForm());

        titleBox.getChildren().addAll(iconLabel, titleText, spacer, addBtn);

        HBox searchBox = new HBox(15);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(10, 0, 0, 0));

        Label searchLabel = new Label("🔍 Search:");
        searchLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        searchField = new TextField();
        searchField.setPromptText("Type to search by Student ID, Name, Email or Department...");
        searchField.setStyle("-fx-padding: 10; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-font-size: 13px;");
        searchField.setPrefWidth(400);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> performSearch());

        totalLabel = new Label();
        totalLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 20 0 0;");

        Region searchSpacer = new Region();
        HBox.setHgrow(searchSpacer, Priority.ALWAYS);

        searchBox.getChildren().addAll(searchLabel, searchField, searchSpacer, totalLabel);

        header.getChildren().addAll(titleBox, new Separator(), searchBox);
        return header;
    }

    private VBox createTableSection() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: transparent;");

        tableView = new TableView<>();
        tableView.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10;");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        setupTableColumns();

        container.getChildren().add(tableView);
        return container;
    }

    @SuppressWarnings("unchecked")
    private void setupTableColumns() {
        TableColumn<Student, Integer> idCol = new TableColumn<>("#");
        idCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().getId()).asObject());
        idCol.setPrefWidth(50);
        idCol.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");

        TableColumn<Student, String> studentIdCol = new TableColumn<>("Student ID");
        studentIdCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStudentId()));
        studentIdCol.setPrefWidth(140);
        studentIdCol.setStyle("-fx-alignment: CENTER; -fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TableColumn<Student, String> nameCol = new TableColumn<>("Full Name");
        nameCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getFullName()));
        nameCol.setPrefWidth(200);
        nameCol.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        TableColumn<Student, String> emailCol = new TableColumn<>("Email / Photo");
        emailCol.setPrefWidth(300);
        emailCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getEmail()));
        emailCol.setCellFactory(col -> new TableCell<>() {
            private final HBox container = new HBox(12);
            private final ImageView profileImage = new ImageView();
            private final Label emailLabel = new Label();
            private final Circle clip = new Circle();

            {
                profileImage.setFitHeight(35);
                profileImage.setFitWidth(35);
                clip.setCenterX(17.5);
                clip.setCenterY(17.5);
                clip.setRadius(17.5);
                profileImage.setClip(clip);
                container.setAlignment(Pos.CENTER_LEFT);
                container.getChildren().addAll(profileImage, emailLabel);
            }

            @Override
            protected void updateItem(String email, boolean empty) {
                super.updateItem(email, empty);
                if (empty || email == null) {
                    setGraphic(null);
                } else {
                    emailLabel.setText(email);
                    emailLabel.setStyle("-fx-text-fill: #3498db; -fx-font-size: 12px;");
                    Student student = getTableRow().getItem();
                    if (student != null) loadProfileImage(student);
                    setGraphic(container);
                }
            }

            private void loadProfileImage(Student student) {
                String path = student.getProfilePicturePath();
                if (path != null && new File(path).exists()) {
                    try {
                        profileImage.setImage(new Image(new File(path).toURI().toString()));
                        return;
                    } catch (Exception e) {}
                }
                profileImage.setImage(null);
                profileImage.setStyle("-fx-background-color: #3498db; -fx-background-radius: 17.5;");
            }
        });

        TableColumn<Student, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getPhone()));
        phoneCol.setPrefWidth(120);

        TableColumn<Student, String> deptCol = new TableColumn<>("Department");
        deptCol.setPrefWidth(170);
        deptCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getDepartment()));
        deptCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String dept, boolean empty) {
                super.updateItem(dept, empty);
                if (empty || dept == null) {
                    setGraphic(null);
                } else {
                    Label label = new Label(dept);
                    String color = getDeptColor(dept);
                    label.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-padding: 5 12; -fx-background-radius: 15; -fx-font-size: 11px; -fx-font-weight: bold;");
                    setGraphic(label);
                }
            }

            private String getDeptColor(String dept) {
                switch (dept) {
                    case "Computer Science": return "#3498db";
                    case "Engineering": return "#e74c3c";
                    case "Business": return "#2ecc71";
                    case "Mathematics": return "#f39c12";
                    case "Physics": return "#9b59b6";
                    case "Chemistry": return "#1abc9c";
                    default: return "#95a5a6";
                }
            }
        });

        TableColumn<Student, String> regDateCol = new TableColumn<>("Enrolled");
        regDateCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getRegistrationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        regDateCol.setPrefWidth(100);
        regDateCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Student, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(260);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = createActionBtn("✏ Edit", "#3498db");
            private final Button detailBtn = createActionBtn("ℹ Detail", "#f39c12");
            private final Button deleteBtn = createActionBtn("🗑 Delete", "#e74c3c");
            private final HBox buttons = new HBox(8, editBtn, detailBtn, deleteBtn);
            {
                buttons.setAlignment(Pos.CENTER);
                editBtn.setOnAction(e -> showEditForm(getTableRow().getItem()));
                detailBtn.setOnAction(e -> showDetails(getTableRow().getItem()));
                deleteBtn.setOnAction(e -> deleteStudent(getTableRow().getItem()));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                setGraphic(empty ? null : buttons);
            }
        });

        tableView.getColumns().addAll(idCol, studentIdCol, nameCol, emailCol, phoneCol, deptCol, regDateCol, actionsCol);
    }

    private Button createActionBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 11px;");
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle() + "-fx-opacity: 0.9;"));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace("-fx-opacity: 0.9;", "")));
        return btn;
    }

    private HBox createPaginationBar() {
        HBox bar = new HBox(20);
        bar.setAlignment(Pos.CENTER);
        bar.setStyle("-fx-background-color: white; -fx-padding: 15 20; -fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");

        Label sizeLabel = new Label("Rows per page:");
        sizeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        pageSizeCombo = new ComboBox<>();
        pageSizeCombo.getItems().addAll(5, 10, 15, 20, 25, 50);
        pageSizeCombo.setValue(pageSize);
        pageSizeCombo.setOnAction(e -> { pageSize = pageSizeCombo.getValue(); currentPage = 0; loadPageData(); });

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        firstBtn = createPageBtn("« First");
        prevBtn = createPageBtn("‹ Prev");
        pageInfoLabel = new Label("Page 1 of 1");
        pageInfoLabel.setMinWidth(120);
        pageInfoLabel.setAlignment(Pos.CENTER);
        pageInfoLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");
        nextBtn = createPageBtn("Next ›");
        lastBtn = createPageBtn("Last »");

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        bar.getChildren().addAll(sizeLabel, pageSizeCombo, spacer1, firstBtn, prevBtn, pageInfoLabel, nextBtn, lastBtn, spacer2);
        return bar;
    }

    private Button createPageBtn(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 5; -fx-cursor: hand; -fx-text-fill: #3498db; -fx-font-weight: bold;");
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle() + "-fx-background-color: #ecf0f1;"));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace("-fx-background-color: #ecf0f1;", "")));
        btn.setOnAction(e -> {
            if (text.equals("« First")) currentPage = 0;
            else if (text.equals("‹ Prev") && currentPage > 0) currentPage--;
            else if (text.equals("Next ›") && currentPage < getTotalPages() - 1) currentPage++;
            else if (text.equals("Last »")) currentPage = getTotalPages() - 1;
            loadPageData();
        });
        return btn;
    }

    private void performSearch() {
        currentDisplayList = controller.searchStudents(searchField.getText());
        currentPage = 0;
        loadPageData();
    }

    private void resetSearch() {
        searchField.clear();
        currentDisplayList = controller.getStudentList();
        currentPage = 0;
        loadPageData();
    }

    private void loadPageData() {
        int totalPages = controller.getTotalPages(currentDisplayList, pageSize);
        if (currentPage >= totalPages && totalPages > 0) currentPage = totalPages - 1;

        List<Student> pageData = controller.getPaginatedStudents(currentDisplayList, currentPage, pageSize);
        tableView.getItems().setAll(pageData);

        pageInfoLabel.setText("Page " + (currentPage + 1) + " of " + Math.max(1, totalPages));

        firstBtn.setDisable(currentPage == 0);
        prevBtn.setDisable(currentPage == 0);
        nextBtn.setDisable(currentPage >= totalPages - 1);
        lastBtn.setDisable(currentPage >= totalPages - 1);
    }

    private int getTotalPages() {
        return controller.getTotalPages(currentDisplayList, pageSize);
    }

    private void showAddForm() { showStudentForm(null); }

    private void showEditForm(Student student) { if (student != null) showStudentForm(student); }

    private String saveProfileImage(File sourceFile) {
        try {
            String ext = sourceFile.getName().substring(sourceFile.getName().lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + ext;
            Path dest = Paths.get(IMAGE_DIR, fileName);
            Files.copy(sourceFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            return dest.toAbsolutePath().toString();
        } catch (Exception e) { return null; }
    }

    /**
     * Shows user-friendly student form with editable Student ID
     */
    private void showStudentForm(Student existingStudent) {
        Stage formStage = new Stage();
        formStage.initModality(Modality.APPLICATION_MODAL);
        formStage.setTitle(existingStudent == null ? "📝 Register New Student" : "✏️ Edit Student Information");

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox contentBox = new VBox(20);
        contentBox.setPadding(new Insets(30));
        contentBox.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f8f9fa);");

        // Header with icon
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label headerIcon = new Label(existingStudent == null ? "🎓" : "✏️");
        headerIcon.setStyle("-fx-font-size: 32px;");
        Label headerLabel = new Label(existingStudent == null ? "Register New Student" : "Edit Student Information");
        headerLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        headerBox.getChildren().addAll(headerIcon, headerLabel);

        // Info note
        Label infoNote = new Label("💡 Tip: Fields marked with * are required");
        infoNote.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px; -fx-padding: 0 0 10 0;");

        // Form Grid
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 10, 20, 10));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(30);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(70);
        grid.getColumnConstraints().addAll(col1, col2);

        int row = 0;

        // Student ID - Now EDITABLE with helpful info
        Label studentIdLabel = new Label("Student ID *");
        studentIdLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Tooltip studentIdTooltip = new Tooltip("Enter a unique Student ID\nFormat example: STU-2024-0001 or any custom format");
        studentIdLabel.setTooltip(studentIdTooltip);

        TextField studentIdField = new TextField();
        if (existingStudent != null && existingStudent.getStudentId() != null) {
            studentIdField.setText(existingStudent.getStudentId());
        } else {
            // Suggest a default format but let user edit
            int nextSeq = controller.getNextSequentialNumber();
            studentIdField.setText(Student.generateStudentId(nextSeq));
        }
        studentIdField.setPromptText("e.g., STU-2024-0001 or STUDENT001");
        studentIdField.setStyle("-fx-padding: 10; -fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #3498db; -fx-border-width: 2; -fx-font-family: 'Monospaced';");

        // Add helper text below Student ID field
        Label studentIdHelper = new Label("✓ Choose any unique identifier for the student");
        studentIdHelper.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 11px; -fx-padding: 5 0 0 0;");

        VBox studentIdBox = new VBox(5);
        studentIdBox.getChildren().addAll(studentIdField, studentIdHelper);

        grid.add(studentIdLabel, 0, row);
        grid.add(studentIdBox, 1, row++);

        // First Name
        Label firstNameLabel = new Label("First Name *");
        firstNameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        TextField firstNameField = new TextField(existingStudent != null ? existingStudent.getFirstName() : "");
        firstNameField.setPromptText("Enter first name (letters only, 2-50 characters)");
        firstNameField.setStyle("-fx-padding: 10; -fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #bdc3c7; -fx-border-width: 1;");
        grid.add(firstNameLabel, 0, row);
        grid.add(firstNameField, 1, row++);

        // Last Name
        Label lastNameLabel = new Label("Last Name *");
        lastNameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        TextField lastNameField = new TextField(existingStudent != null ? existingStudent.getLastName() : "");
        lastNameField.setPromptText("Enter last name (letters only, 2-50 characters)");
        lastNameField.setStyle("-fx-padding: 10; -fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #bdc3c7; -fx-border-width: 1;");
        grid.add(lastNameLabel, 0, row);
        grid.add(lastNameField, 1, row++);

        // Email
        Label emailLabel = new Label("Email *");
        emailLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        TextField emailField = new TextField(existingStudent != null ? existingStudent.getEmail() : "");
        emailField.setPromptText("example@domain.com");
        emailField.setStyle("-fx-padding: 10; -fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #bdc3c7; -fx-border-width: 1;");
        grid.add(emailLabel, 0, row);
        grid.add(emailField, 1, row++);

        // Phone with helper
        Label phoneLabel = new Label("Phone Number");
        phoneLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        TextField phoneField = new TextField(existingStudent != null ? existingStudent.getPhone() : "");
        phoneField.setPromptText("e.g., +251912345678 or 0912345678");
        phoneField.setStyle("-fx-padding: 10; -fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #bdc3c7; -fx-border-width: 1;");

        Label phoneHelper = new Label("✓ Supported formats: +251XXXXXXXXX, 09XXXXXXXX, 0XX-XXX-XXXX");
        phoneHelper.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 10px; -fx-padding: 5 0 0 0;");

        VBox phoneBox = new VBox(5);
        phoneBox.getChildren().addAll(phoneField, phoneHelper);

        grid.add(phoneLabel, 0, row);
        grid.add(phoneBox, 1, row++);

        // Date of Birth
        Label dobLabel = new Label("Date of Birth *");
        dobLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        DatePicker dobPicker = new DatePicker(existingStudent != null ? existingStudent.getDateOfBirth() : null);
        dobPicker.setPromptText("YYYY-MM-DD");
        dobPicker.setStyle("-fx-padding: 10; -fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #bdc3c7; -fx-border-width: 1;");

        Label dobHelper = new Label("✓ Student must be at least 5 years old");
        dobHelper.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 10px; -fx-padding: 5 0 0 0;");

        VBox dobBox = new VBox(5);
        dobBox.getChildren().addAll(dobPicker, dobHelper);

        grid.add(dobLabel, 0, row);
        grid.add(dobBox, 1, row++);

        // Department
        Label deptLabel = new Label("Department *");
        deptLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        ComboBox<String> deptCombo = new ComboBox<>();
        deptCombo.getItems().addAll("Computer Science", "Engineering", "Business", "Mathematics", "Physics", "Chemistry");
        deptCombo.setValue(existingStudent != null ? existingStudent.getDepartment() : null);
        deptCombo.setPromptText("Select department");
        deptCombo.setPrefWidth(Double.MAX_VALUE);
        deptCombo.setStyle("-fx-padding: 10; -fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #bdc3c7; -fx-border-width: 1;");
        grid.add(deptLabel, 0, row);
        grid.add(deptCombo, 1, row++);

        // Profile Picture with preview
        Label photoLabel = new Label("Profile Photo");
        photoLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox photoBox = new HBox(10);
        photoBox.setAlignment(Pos.CENTER_LEFT);

        TextField photoField = new TextField();
        photoField.setEditable(false);
        photoField.setPromptText("No photo selected");
        photoField.setStyle("-fx-padding: 10; -fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #bdc3c7; -fx-border-width: 1;");
        HBox.setHgrow(photoField, Priority.ALWAYS);

        Button uploadBtn = new Button("📷 Choose Photo");
        uploadBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5; -fx-cursor: hand;");

        ImageView previewImage = new ImageView();
        previewImage.setFitHeight(50);
        previewImage.setFitWidth(50);
        previewImage.setPreserveRatio(true);
        previewImage.setStyle("-fx-background-radius: 25; -fx-border-radius: 25; -fx-border-color: #bdc3c7; -fx-border-width: 1;");

        if (existingStudent != null && existingStudent.getProfilePicturePath() != null) {
            File pf = new File(existingStudent.getProfilePicturePath());
            if (pf.exists()) {
                try {
                    previewImage.setImage(new Image(pf.toURI().toString()));
                    photoField.setText(pf.getName());
                } catch (Exception e) {}
            }
        }

        uploadBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Profile Picture");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            File file = fc.showOpenDialog(formStage);
            if (file != null) {
                try {
                    Image img = new Image(file.toURI().toString());
                    previewImage.setImage(img);
                    photoField.setText(file.getName());
                    photoField.setUserData(file);
                } catch (Exception ex) {}
            }
        });

        photoBox.getChildren().addAll(photoField, uploadBtn, previewImage);
        grid.add(photoLabel, 0, row);
        grid.add(photoBox, 1, row++);

        // Action Buttons
        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(20, 0, 10, 0));

        Button saveBtn = new Button(existingStudent == null ? "✅ Register Student" : "💾 Update Student");
        saveBtn.setStyle("-fx-background-color: linear-gradient(to right, #27ae60, #2ecc71); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 35; -fx-background-radius: 25; -fx-font-size: 14px; -fx-cursor: hand;");

        Button clearBtn = new Button("🗑 Clear Form");
        clearBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 25; -fx-background-radius: 25; -fx-font-size: 14px; -fx-cursor: hand;");
        clearBtn.setOnAction(ev -> {
            // Clear all fields for new entry
            if (existingStudent == null) {
                studentIdField.setText(Student.generateStudentId(controller.getNextSequentialNumber()));
                firstNameField.clear();
                lastNameField.clear();
                emailField.clear();
                phoneField.clear();
                dobPicker.setValue(null);
                deptCombo.setValue(null);
                photoField.clear();
                previewImage.setImage(null);
                photoField.setUserData(null);
            }
        });

        Button cancelBtn = new Button("❌ Cancel");
        cancelBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 25; -fx-background-radius: 25; -fx-font-size: 14px; -fx-cursor: hand;");
        cancelBtn.setOnAction(ev -> formStage.close());

        btnBox.getChildren().addAll(saveBtn, clearBtn, cancelBtn);

        contentBox.getChildren().addAll(headerBox, infoNote, new Separator(), grid, btnBox);

        scrollPane.setContent(contentBox);

        double formWidth = Math.min(700, screen.getWidth() * 0.9);
        double formHeight = Math.min(800, screen.getHeight() * 0.85);

        Scene scene = new Scene(scrollPane, formWidth, formHeight);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        formStage.setScene(scene);

        formStage.setX((screen.getWidth() - formWidth) / 2);
        formStage.setY((screen.getHeight() - formHeight) / 2);

        // Save action
        saveBtn.setOnAction(ev -> {
            // Validate all fields
            String studentId = studentIdField.getText().trim();
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            LocalDate dob = dobPicker.getValue();
            String dept = deptCombo.getValue();

            if (studentId.isEmpty()) { showAlert("❌ Student ID is required"); return; }
            if (firstName.isEmpty()) { showAlert("❌ First name required"); return; }
            if (!firstName.matches("^[A-Za-z]{2,50}$")) { showAlert("❌ First name must contain only letters (2-50 chars)"); return; }
            if (lastName.isEmpty()) { showAlert("❌ Last name required"); return; }
            if (!lastName.matches("^[A-Za-z]{2,50}$")) { showAlert("❌ Last name must contain only letters (2-50 chars)"); return; }
            if (email.isEmpty()) { showAlert("❌ Email required"); return; }
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) { showAlert("❌ Valid email required (e.g., name@domain.com)"); return; }
            if (dob == null) { showAlert("❌ Date of birth required"); return; }
            if (dob.isAfter(LocalDate.now().minusYears(5))) { showAlert("❌ Student must be at least 5 years old"); return; }
            if (dept == null) { showAlert("❌ Department required"); return; }

            Student student = new Student();
            if (existingStudent != null) student.setId(existingStudent.getId());
            student.setStudentId(studentId);
            student.setFirstName(firstName);
            student.setLastName(lastName);
            student.setEmail(email);
            student.setPhone(phone);
            student.setDateOfBirth(dob);
            student.setDepartment(dept);

            File uploaded = (File) photoField.getUserData();
            if (uploaded != null) student.setProfilePicturePath(saveProfileImage(uploaded));
            else if (existingStudent != null) student.setProfilePicturePath(existingStudent.getProfilePicturePath());

            boolean success = (existingStudent == null) ? controller.addStudent(student) : controller.updateStudent(student);
            if (success) {
                formStage.close();
                resetSearch();
            }
        });

        formStage.showAndWait();
    }

    private void showDetails(Student student) {
        if (student == null) return;

        Stage detailStage = new Stage();
        detailStage.initModality(Modality.APPLICATION_MODAL);
        detailStage.setTitle("Student Details - " + student.getFullName());

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double width = Math.min(550, screen.getWidth() * 0.8);
        double height = Math.min(600, screen.getHeight() * 0.7);

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f8f9fa);");

        // Header with photo
        HBox headerBox = new HBox(20);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        ImageView photoView = new ImageView();
        photoView.setFitHeight(80);
        photoView.setFitWidth(80);
        Circle clip = new Circle(40, 40, 40);
        photoView.setClip(clip);

        if (student.getProfilePicturePath() != null && new File(student.getProfilePicturePath()).exists()) {
            try {
                photoView.setImage(new Image(new File(student.getProfilePicturePath()).toURI().toString()));
            } catch (Exception e) { setDefaultPhoto(photoView); }
        } else { setDefaultPhoto(photoView); }

        VBox infoBox = new VBox(5);
        Label nameLabel = new Label(student.getFullName());
        nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label studentIdLabel = new Label("🆔 " + student.getStudentId());
        studentIdLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #3498db; -fx-font-family: 'Monospaced'; -fx-font-weight: bold;");
        infoBox.getChildren().addAll(nameLabel, studentIdLabel);

        headerBox.getChildren().addAll(photoView, infoBox);

        // Details grid
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 0, 20, 0));
        grid.setStyle("-fx-background-color: #f8f9fa; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 15;");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        int row = 0;
        grid.add(createBoldLabel("📧 Email:"), 0, row);
        grid.add(new Label(student.getEmail() != null ? student.getEmail() : "N/A"), 1, row++);
        grid.add(createBoldLabel("📞 Phone:"), 0, row);
        grid.add(new Label(student.getPhone() != null ? student.getPhone() : "N/A"), 1, row++);
        grid.add(createBoldLabel("🎂 Date of Birth:"), 0, row);
        grid.add(new Label(student.getDateOfBirth() != null ? student.getDateOfBirth().format(fmt) : "N/A"), 1, row++);
        grid.add(createBoldLabel("🏫 Department:"), 0, row);
        grid.add(new Label(student.getDepartment() != null ? student.getDepartment() : "N/A"), 1, row++);
        grid.add(createBoldLabel("📅 Enrolled:"), 0, row);
        grid.add(new Label(student.getRegistrationDate() != null ? student.getRegistrationDate().format(fmt) : "N/A"), 1, row++);

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30; -fx-background-radius: 25; -fx-font-size: 14px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> detailStage.close());

        root.getChildren().addAll(headerBox, new Separator(), grid, closeBtn);

        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        detailStage.setScene(scene);

        detailStage.setX((screen.getWidth() - width) / 2);
        detailStage.setY((screen.getHeight() - height) / 2);

        detailStage.showAndWait();
    }

    private Label createBoldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        return label;
    }

    private void setDefaultPhoto(ImageView view) {
        view.setImage(null);
        view.setStyle("-fx-background-color: #3498db; -fx-background-radius: 40;");
    }

    private void deleteStudent(Student student) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Student Record");
        confirm.setContentText("Are you sure you want to delete " + student.getFullName() + "?\nStudent ID: " + student.getStudentId());
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                controller.deleteStudent(student);
                resetSearch();
            }
        });
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}