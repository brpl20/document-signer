package com.example.documentsigner;

public class Main {
    public static void main(String[] args) {
        try {
            // Launch the Swing UI
            DocumentSignerUI.main(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
