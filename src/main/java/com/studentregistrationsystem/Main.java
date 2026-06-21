package com.studentregistrationsystem;

import com.studentregistrationsystem.controller.StudentController;
import com.studentregistrationsystem.view.StudentView;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        StudentController controller = new StudentController();
        new StudentView(primaryStage, controller);
    }

    public static void main(String[] args) {
        launch(args);
    }
}