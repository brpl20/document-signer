package com.example.documentsigner;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;

import com.example.documentsigner.api.dto.CertificateInfo;
import com.example.documentsigner.exception.ExpiredCertificateException;
import com.example.documentsigner.exception.InvalidPasswordException;

public class DocumentSignerUI {

    private static final String PREF_LAST_DIRECTORY = "lastDirectory";
    private static final String PREF_LAST_CERTIFICATE = "lastCertificate";

    private JFrame frame;
    private JTextField pfxPathField;
    private JPasswordField passwordField;
    private JTextArea logArea;
    private PdfSigner signer;
    private Preferences prefs;
    
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
        prefs = Preferences.userNodeForPackage(DocumentSignerUI.class);

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

        JButton checkCertButton = new JButton("Check Certificate");
        checkCertButton.addActionListener(e -> showCertificateDetails());
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        formPanel.add(checkCertButton, gbc);

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

        // Load saved certificate path
        String savedCertPath = prefs.get(PREF_LAST_CERTIFICATE, "");
        if (!savedCertPath.isEmpty() && new File(savedCertPath).exists()) {
            pfxPathField.setText(savedCertPath);
            log("Loaded saved certificate: " + savedCertPath);
        }

        // Setup drag-and-drop for the main panel
        setupDragAndDrop(mainPanel);

        frame.setVisible(true);
    }

    private void setupDragAndDrop(JPanel panel) {
        new DropTarget(panel, new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                    panel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0, 120, 215), 3),
                        new EmptyBorder(7, 17, 17, 17)
                    ));
                } else {
                    dtde.rejectDrag();
                }
            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                // Not needed
            }

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {
                // Not needed
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                panel.setBorder(new EmptyBorder(10, 20, 20, 20));
            }

            @Override
            @SuppressWarnings("unchecked")
            public void drop(DropTargetDropEvent dtde) {
                panel.setBorder(new EmptyBorder(10, 20, 20, 20));
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();
                    List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

                    if (!validateCertificateForSigning()) {
                        log("Please select a certificate and enter password before dropping files.");
                        dtde.dropComplete(false);
                        return;
                    }

                    int pdfCount = 0;
                    for (File file : files) {
                        if (file.isFile() && file.getName().toLowerCase().endsWith(".pdf")) {
                            signFile(file);
                            pdfCount++;
                        } else if (file.isDirectory()) {
                            File[] pdfFiles = file.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
                            if (pdfFiles != null) {
                                for (File pdfFile : pdfFiles) {
                                    signFile(pdfFile);
                                    pdfCount++;
                                }
                            }
                        }
                    }

                    if (pdfCount == 0) {
                        log("No PDF files found in dropped items.");
                    } else {
                        log("Processed " + pdfCount + " PDF file(s) via drag-and-drop.");
                    }
                    dtde.dropComplete(true);
                } catch (Exception e) {
                    log("Error processing dropped files: " + e.getMessage());
                    dtde.dropComplete(false);
                }
            }
        });
    }
    
    private void browsePfxFile() {
        // Create a file chooser that can navigate directories
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Certificate File");

        // Set initial directory from preferences
        String lastDir = prefs.get(PREF_LAST_DIRECTORY, System.getProperty("user.home"));
        fileChooser.setCurrentDirectory(new File(lastDir));

        // Add the PFX filter but keep "All Files" option enabled
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("PFX Files", "pfx"));
        fileChooser.setAcceptAllFileFilterUsed(true);

        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            pfxPathField.setText(selectedFile.getAbsolutePath());

            // Save the certificate path and directory to preferences
            prefs.put(PREF_LAST_CERTIFICATE, selectedFile.getAbsolutePath());
            prefs.put(PREF_LAST_DIRECTORY, selectedFile.getParent());
        }
    }
    
    private void signSingleFile() {
        if (!validateCertificateForSigning()) return;

        // Create a file chooser that can navigate directories
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select PDF to Sign");

        // Set initial directory from preferences
        String lastDir = prefs.get(PREF_LAST_DIRECTORY, System.getProperty("user.home"));
        fileChooser.setCurrentDirectory(new File(lastDir));

        // Add the PDF filter but keep "All Files" option enabled
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        fileChooser.setAcceptAllFileFilterUsed(true);

        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            // Save directory to preferences
            prefs.put(PREF_LAST_DIRECTORY, selectedFile.getParent());
            signFile(selectedFile);
        }
    }
    
    private void signMultipleFiles() {
        if (!validateCertificateForSigning()) return;

        // Create a file chooser that can navigate directories
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select PDFs to Sign");

        // Set initial directory from preferences
        String lastDir = prefs.get(PREF_LAST_DIRECTORY, System.getProperty("user.home"));
        fileChooser.setCurrentDirectory(new File(lastDir));

        // Add the PDF filter but keep "All Files" option enabled
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setMultiSelectionEnabled(true);

        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            if (selectedFiles.length > 0) {
                // Save directory to preferences
                prefs.put(PREF_LAST_DIRECTORY, selectedFiles[0].getParent());
            }
            for (File file : selectedFiles) {
                signFile(file);
            }
        }
    }
    
    private void signFolder() {
        if (!validateCertificateForSigning()) return;

        // Create a file chooser specifically for directories
        JFileChooser directoryChooser = new JFileChooser();
        directoryChooser.setDialogTitle("Select Folder with PDFs");
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        directoryChooser.setAcceptAllFileFilterUsed(true);

        // Set initial directory from preferences
        String lastDir = prefs.get(PREF_LAST_DIRECTORY, System.getProperty("user.home"));
        directoryChooser.setCurrentDirectory(new File(lastDir));

        if (directoryChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = directoryChooser.getSelectedFile();
            // Save directory to preferences
            prefs.put(PREF_LAST_DIRECTORY, selectedDirectory.getAbsolutePath());

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

    /**
     * Show certificate details in a dialog.
     */
    private void showCertificateDetails() {
        String pfxPath = pfxPathField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (pfxPath.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                "Please select a certificate file first.",
                "No Certificate Selected",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                "Please enter the certificate password.",
                "Password Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        File pfxFile = new File(pfxPath);
        if (!pfxFile.exists()) {
            JOptionPane.showMessageDialog(frame,
                "Certificate file not found: " + pfxPath,
                "File Not Found",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        CertificateInfo info = CertificateValidator.getCertificateInfo(pfxPath, password);

        if (!info.isValid()) {
            JOptionPane.showMessageDialog(frame,
                info.getError(),
                "Certificate Error",
                JOptionPane.ERROR_MESSAGE);
            log("Certificate check failed: " + info.getError());
            return;
        }

        // Format the certificate details
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        StringBuilder details = new StringBuilder();
        details.append("=== CERTIFICATE DETAILS ===\n\n");
        details.append("Common Name: ").append(info.getCommonName()).append("\n\n");
        details.append("Subject: ").append(info.getSubject()).append("\n\n");
        details.append("Issuer: ").append(info.getIssuer()).append("\n\n");
        details.append("Serial Number: ").append(info.getSerialNumber()).append("\n\n");
        details.append("Valid From: ").append(dateFormat.format(info.getNotBefore())).append("\n");
        details.append("Valid Until: ").append(dateFormat.format(info.getNotAfter())).append("\n\n");

        if (info.isExpired()) {
            details.append("⚠ STATUS: EXPIRED\n");
        } else if (info.getDaysUntilExpiry() <= 30) {
            details.append("⚠ STATUS: EXPIRING SOON (").append(info.getDaysUntilExpiry()).append(" days left)\n");
        } else {
            details.append("✓ STATUS: VALID (").append(info.getDaysUntilExpiry()).append(" days until expiry)\n");
        }

        details.append("\nAlgorithm: ").append(info.getAlgorithm());

        // Show in a scrollable dialog
        JTextArea textArea = new JTextArea(details.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 350));

        int messageType = info.isExpired() ? JOptionPane.ERROR_MESSAGE :
                          (info.getDaysUntilExpiry() <= 30 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);

        JOptionPane.showMessageDialog(frame, scrollPane, "Certificate Details", messageType);

        log("Certificate loaded: " + info.getCommonName() +
            (info.isExpired() ? " [EXPIRED]" : " [Valid for " + info.getDaysUntilExpiry() + " days]"));
    }

    /**
     * Validate certificate password and expiry before signing.
     * Returns true if certificate is valid and ready for signing.
     */
    private boolean validateCertificateForSigning() {
        String pfxPath = pfxPathField.getText().trim();
        String password = new String(passwordField.getPassword());

        // Basic validation
        if (!validateCertificate()) {
            return false;
        }

        try {
            // Validate password
            CertificateValidator.validatePassword(pfxPath, password);

            // Check expiry
            CertificateValidator.checkExpiry(pfxPath, password);

            return true;

        } catch (InvalidPasswordException e) {
            log("Error: Incorrect certificate password.");
            JOptionPane.showMessageDialog(frame,
                "The certificate password is incorrect.\nPlease check and try again.",
                "Invalid Password",
                JOptionPane.ERROR_MESSAGE);
            return false;

        } catch (ExpiredCertificateException e) {
            log("Error: Certificate has expired.");
            int choice = JOptionPane.showConfirmDialog(frame,
                "This certificate has expired!\n\n" + e.getMessage() +
                "\n\nExpired certificates should not be used for new signatures.\nDo you want to continue anyway?",
                "Certificate Expired",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            return choice == JOptionPane.YES_OPTION;

        } catch (Exception e) {
            log("Error validating certificate: " + e.getMessage());
            JOptionPane.showMessageDialog(frame,
                "Error validating certificate:\n" + e.getMessage(),
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}
