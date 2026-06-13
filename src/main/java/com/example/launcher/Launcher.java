package com.example.launcher;

import com.example.login.LoginApp;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        // Launch the login screen
        Application.launch(LoginApp.class, args);
    }
}