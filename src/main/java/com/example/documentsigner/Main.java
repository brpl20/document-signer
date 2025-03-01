package com.example.documentsigner;

import javafx.application.Application;

public class Main {
    public static void main(String[] args) {
        try {
            // This approach helps avoid module-related issues
            DocumentSignerUI.main(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
