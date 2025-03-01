package com.example.documentsigner;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class DocumentSignerUI {
    
    private JFrame frame;
    private JTextField pfxPathField;
    private JPasswordField passwordField;
    private JTextArea logArea;
    private PdfSigner signer;
    
    public static void main(String[] args) {
        // Set the look and feel to the system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Create and display the UI on the EDT
        SwingUtilities.invokeLater(() -> {
            DocumentSignerUI ui = new DocumentSignerUI();
            ui.createAndShowGUI();
        });
    }
    
    public void createAndShowGUI() {
        signer = new PdfSigner();
        
        // Create the main frame
        frame = new JFrame("ProcStudio Signer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        
        // Create the header panel with title
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(0, 51, 102)); // Dark blue background
        
        // Try to load images from both current directory and resources
        boolean imagesLoaded = false;
        
        // First try to load from current directory
        File iconFile = new File("procstudio_símbolo_sem_fundo.png");
        File logoFile = new File("procstudio_logotipo_horizontal_fundo_azul.png");
        
        if (iconFile.exists() && logoFile.exists()) {
            try {
                // Set application icon
                Image appIcon = ImageIO.read(iconFile);
                frame.setIconImage(appIcon);
                
                // Load and scale logo
                ImageIcon logoIcon = new ImageIcon(logoFile.getAbsolutePath());
                Image scaledImage = logoIcon.getImage().getScaledInstance(300, -1, Image.SCALE_SMOOTH);
                JLabel logoLabel = new JLabel(new ImageIcon(scaledImage));
                logoLabel.setBorder(new EmptyBorder(10, 20, 10, 0));
                headerPanel.add(logoLabel, BorderLayout.WEST);
                
                imagesLoaded = true;
            } catch (Exception e) {
                System.err.println("Error loading images from files: " + e.getMessage());
            }
        }
        
        // If images weren't loaded, just show the title
        if (!imagesLoaded) {
            JLabel titleLabel = new JLabel("ProcStudio Signer");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setBorder(new EmptyBorder(10, 20, 10, 0));
            headerPanel.add(titleLabel, BorderLayout.CENTER);
        } else {
            // Add title on the right if images were loaded
            JLabel titleLabel = new JLabel("ProcStudio Signer");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setBorder(new EmptyBorder(0, 20, 0, 0));
            headerPanel.add(titleLabel, BorderLayout.EAST);
        }
        
        // Create the main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 20, 20, 20));
        
        // Create the form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Certificate file selection
        JLabel pfxLabel = new JLabel("Certificate File (.pfx):");
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(pfxLabel, gbc);
        
        pfxPathField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        formPanel.add(pfxPathField, gbc);
        
        JButton pfxBrowseButton = new JButton("Browse");
        pfxBrowseButton.addActionListener(e -> browsePfxFile());
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        formPanel.add(pfxBrowseButton, gbc);
        
        // Password field
        JLabel passwordLabel = new JLabel("Certificate Password:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(passwordLabel, gbc);
        
        passwordField = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        formPanel.add(passwordField, gbc);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        
        JButton signFileButton = new JButton("Sign File");
        signFileButton.addActionListener(e -> signSingleFile());
        buttonPanel.add(signFileButton);
        
        JButton signMultipleButton = new JButton("Sign Multiple Files");
        signMultipleButton.addActionListener(e -> signMultipleFiles());
        buttonPanel.add(signMultipleButton);
        
        JButton signFolderButton = new JButton("Sign Folder");
        signFolderButton.addActionListener(e -> signFolder());
        buttonPanel.add(signFolderButton);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        formPanel.add(buttonPanel, gbc);
        
        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(500, 200));
        
        // Add components to the main panel
        mainPanel.add(formPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Add header and main panel to the frame
        frame.getContentPane().add(headerPanel, BorderLayout.NORTH);
        frame.getContentPane().add(mainPanel, BorderLayout.CENTER);
        frame.setLocationRelativeTo(null); // Center on screen
        frame.setVisible(true);
    }
    
    private void browsePfxFile() {
        // Create a file chooser that can navigate directories
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Certificate File");
        
        // Add the PFX filter but keep "All Files" option enabled
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("PFX Files", "pfx"));
        fileChooser.setAcceptAllFileFilterUsed(true);
        
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            pfxPathField.setText(selectedFile.getAbsolutePath());
        }
    }
    
    private void signSingleFile() {
        if (!validateCertificate()) return;
        
        // Create a file chooser that can navigate directories
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select PDF to Sign");
        
        // Add the PDF filter but keep "All Files" option enabled
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        fileChooser.setAcceptAllFileFilterUsed(true);
        
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            signFile(selectedFile);
        }
    }
    
    private void signMultipleFiles() {
        if (!validateCertificate()) return;
        
        // Create a file chooser that can navigate directories
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select PDFs to Sign");
        
        // Add the PDF filter but keep "All Files" option enabled
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setMultiSelectionEnabled(true);
        
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            for (File file : selectedFiles) {
                signFile(file);
            }
        }
    }
    
    private void signFolder() {
        if (!validateCertificate()) return;
        
        // Create a file chooser specifically for directories
        JFileChooser directoryChooser = new JFileChooser();
        directoryChooser.setDialogTitle("Select Folder with PDFs");
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        directoryChooser.setAcceptAllFileFilterUsed(true);
        
        if (directoryChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = directoryChooser.getSelectedFile();
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
        String password = new String(passwordField.getPassword());
        
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
            String password = new String(passwordField.getPassword());
            
            signer.signPdf(inputPath, outputPath, pfxPath, password);
            log("Successfully signed: " + pdfFile.getName() + " -> " + new File(outputPath).getName());
        } catch (Exception e) {
            log("Error signing " + pdfFile.getName() + ": " + e.getMessage());
        }
    }
    
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            // Auto-scroll to the bottom
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
