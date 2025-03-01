package com.example.documentsigner;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class DocumentSignerUI extends Application {
    
    // Static main method to allow launching from non-JavaFX classes
    public static void main(String[] args) {
        launch(args);
    }
    
    private TextField pfxPathField;
    private PasswordField passwordField;
    private TextArea logArea;
    private PdfSigner signer;
    
    @Override
    public void start(Stage primaryStage) {
        signer = new PdfSigner();
        
        primaryStage.setTitle("Document Signer");
        
        // Create UI components
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20, 20, 20, 20));
        grid.setVgap(10);
        grid.setHgap(10);
        
        // Certificate file selection
        Label pfxLabel = new Label("Certificate File (.pfx):");
        grid.add(pfxLabel, 0, 0);
        
        pfxPathField = new TextField();
        pfxPathField.setPromptText("Select your .pfx certificate file");
        pfxPathField.setPrefWidth(300);
        grid.add(pfxPathField, 1, 0);
        
        Button pfxBrowseButton = new Button("Browse");
        pfxBrowseButton.setOnAction(e -> browsePfxFile(primaryStage));
        grid.add(pfxBrowseButton, 2, 0);
        
        // Password field
        Label passwordLabel = new Label("Certificate Password:");
        grid.add(passwordLabel, 0, 1);
        
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter your certificate password");
        grid.add(passwordField, 1, 1);
        
        // Sign single file button
        Button signFileButton = new Button("Sign File");
        signFileButton.setOnAction(e -> signSingleFile(primaryStage));
        
        // Sign multiple files button
        Button signMultipleButton = new Button("Sign Multiple Files");
        signMultipleButton.setOnAction(e -> signMultipleFiles(primaryStage));
        
        // Sign folder button
        Button signFolderButton = new Button("Sign Folder");
        signFolderButton.setOnAction(e -> signFolder(primaryStage));
        
        // Buttons layout
        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(signFileButton, signMultipleButton, signFolderButton);
        grid.add(buttonBox, 0, 2, 3, 1);
        
        // Log area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(200);
        logArea.setWrapText(true);
        grid.add(logArea, 0, 3, 3, 1);
        
        VBox root = new VBox(20);
        root.setPadding(new Insets(10));
        root.getChildren().add(grid);
        
        Scene scene = new Scene(root, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private void browsePfxFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Certificate File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PFX Files", "*.pfx")
        );
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            pfxPathField.setText(selectedFile.getAbsolutePath());
        }
    }
    
    private void signSingleFile(Stage stage) {
        if (!validateCertificate()) return;
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select PDF to Sign");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            signFile(selectedFile);
        }
    }
    
    private void signMultipleFiles(Stage stage) {
        if (!validateCertificate()) return;
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select PDFs to Sign");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);
        if (selectedFiles != null) {
            for (File file : selectedFiles) {
                signFile(file);
            }
        }
    }
    
    private void signFolder(Stage stage) {
        if (!validateCertificate()) return;
        
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder with PDFs");
        File selectedDirectory = directoryChooser.showDialog(stage);
        if (selectedDirectory != null) {
            File[] files = selectedDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            if (files != null && files.length > 0) {
                for (File file : files) {
                    signFile(file);
                }
                log("Processed " + files.length + " files from folder: " + selectedDirectory.getAbsolutePath());
            } else {
                log("No PDF files found in the selected folder.");
            }
        }
    }
    
    private boolean validateCertificate() {
        String pfxPath = pfxPathField.getText().trim();
        String password = passwordField.getText();
        
        if (pfxPath.isEmpty()) {
            log("Error: Please select a certificate file.");
            return false;
        }
        
        if (password.isEmpty()) {
            log("Error: Please enter the certificate password.");
            return false;
        }
        
        File pfxFile = new File(pfxPath);
        if (!pfxFile.exists() || !pfxFile.isFile()) {
            log("Error: Certificate file does not exist.");
            return false;
        }
        
        return true;
    }
    
    private void signFile(File pdfFile) {
        try {
            String inputPath = pdfFile.getAbsolutePath();
            String outputPath = inputPath + ".p7s";
            String pfxPath = pfxPathField.getText().trim();
            String password = passwordField.getText();
            
            signer.signPdf(inputPath, outputPath, pfxPath, password);
            log("Successfully signed: " + pdfFile.getName() + " -> " + new File(outputPath).getName());
        } catch (Exception e) {
            log("Error signing " + pdfFile.getName() + ": " + e.getMessage());
        }
    }
    
    private void log(String message) {
        logArea.appendText(message + "\n");
    }
}
