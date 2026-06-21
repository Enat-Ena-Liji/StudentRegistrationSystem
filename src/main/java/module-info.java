module com.studentregistrationsystem.studentregistrationsystem {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;

    opens com.studentregistrationsystem to javafx.fxml;
    opens com.studentregistrationsystem.controller to javafx.fxml;
    opens com.studentregistrationsystem.view to javafx.fxml;
    opens com.studentregistrationsystem.model to javafx.base;

    exports com.studentregistrationsystem;
    exports com.studentregistrationsystem.controller;
    exports com.studentregistrationsystem.view;
    exports com.studentregistrationsystem.model;
}