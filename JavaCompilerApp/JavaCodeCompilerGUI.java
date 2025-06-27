import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.tools.*;
import java.util.regex.*;
import java.util.concurrent.*;
import javax.swing.event.*; // Added for DocumentListener and DocumentEvent

public class JavaCodeCompilerGUI extends JFrame {
    private JTextPane inputArea;
    private JEditorPane outputArea;
    private JLabel statusLabel;
    private JTextField argsField;
    private File currentFile;
    private static final long PROCESS_TIMEOUT = 10; // seconds

    public JavaCodeCompilerGUI() {
        setTitle("Enhanced Java Code Compiler and Runner");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initializeGUI();
    }

    private void initializeGUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        
        // Input area with syntax highlighting
        inputArea = new JTextPane();
        inputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        setupSyntaxHighlighting();
        inputArea.setText("public class TempClass {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n    }\n}");
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(BorderFactory.createTitledBorder("Java Code"));
        inputScroll.setPreferredSize(new Dimension(0, 400));

        // Output area
        outputArea = new JEditorPane();
        outputArea.setContentType("text/html");
        outputArea.setEditable(false);
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Output"));
        outputScroll.setPreferredSize(new Dimension(0, 200));

        // Arguments input
        JPanel argsPanel = new JPanel(new BorderLayout(5, 5));
        argsField = new JTextField();
        argsField.setFont(new Font("Arial", Font.PLAIN, 14));
        argsPanel.add(new JLabel("Arguments: "), BorderLayout.WEST);
        argsPanel.add(argsField, BorderLayout.CENTER);
        argsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        JButton runButton = createStyledButton("Run", "Compile and Run Code", new Color(76, 175, 80));
        runButton.addActionListener(e -> compileAndRun(inputArea.getText()));
        
        JButton saveButton = createStyledButton("Save", "Save Code", new Color(33, 150, 243));
        saveButton.addActionListener(e -> saveFile());
        
        JButton loadButton = createStyledButton("Load", "Load Code", new Color(255, 193, 7));
        loadButton.addActionListener(e -> loadFile());
        
        JButton clearButton = createStyledButton("Clear", "Clear Output", new Color(244, 67, 54));
        clearButton.addActionListener(e -> {
            outputArea.setText("");
            statusLabel.setText("Output cleared");
        });
        
        toolBar.add(runButton);
        toolBar.add(saveButton);
        toolBar.add(loadButton);
        toolBar.add(clearButton);

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));

        mainPanel.add(toolBar, BorderLayout.NORTH);
        mainPanel.add(inputScroll, BorderLayout.CENTER);
        mainPanel.add(argsPanel, BorderLayout.SOUTH);
        mainPanel.add(outputScroll, BorderLayout.PAGE_END);
        add(mainPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JButton createStyledButton(String text, String tooltip, Color background) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        return button;
    }

    private void setupSyntaxHighlighting() {
        DefaultStyledDocument doc = new DefaultStyledDocument();
        inputArea.setDocument(doc);
        
        SimpleAttributeSet keywordStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(keywordStyle, new Color(0, 0, 128));
        StyleConstants.setBold(keywordStyle, true);
        
        SimpleAttributeSet stringStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(stringStyle, new Color(0, 128, 0));
        
        SimpleAttributeSet commentStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(commentStyle, new Color(128, 128, 128));
        StyleConstants.setItalic(commentStyle, true);
        
        inputArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { highlightSyntax(); }
            public void removeUpdate(DocumentEvent e) { highlightSyntax(); }
            public void changedUpdate(DocumentEvent e) { highlightSyntax(); }
            
            private void highlightSyntax() {
                try {
                    String text = inputArea.getText();
                    doc.setCharacterAttributes(0, text.length(), new SimpleAttributeSet(), true);
                    
                    // Keywords
                    String[] keywords = {"public", "class", "static", "void", "String", "int", "double", "if", "else", "for", "while"};
                    for (String keyword : keywords) {
                        Pattern p = Pattern.compile("\\b" + keyword + "\\b");
                        Matcher m = p.matcher(text);
                        while (m.find()) {
                            doc.setCharacterAttributes(m.start(), m.end() - m.start(), keywordStyle, false);
                        }
                    }
                    
                    // Strings
                    Pattern stringPattern = Pattern.compile("\".*?\"");
                    Matcher stringMatcher = stringPattern.matcher(text);
                    while (stringMatcher.find()) {
                        doc.setCharacterAttributes(stringMatcher.start(), stringMatcher.end() - stringMatcher.start(), stringStyle, false);
                    }
                    
                    // Comments
                    Pattern commentPattern = Pattern.compile("//.*?$|(?s)/\\*.*?\\*/", Pattern.MULTILINE);
                    Matcher commentMatcher = commentPattern.matcher(text);
                    while (commentMatcher.find()) {
                        doc.setCharacterAttributes(commentMatcher.start(), commentMatcher.end() - commentMatcher.start(), commentStyle, false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void saveFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".java");
            }
            public String getDescription() {
                return "Java Files (*.java)";
            }
        });
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            if (!currentFile.getName().endsWith(".java")) {
                currentFile = new File(currentFile.getPath() + ".java");
            }
            try (FileWriter writer = new FileWriter(currentFile)) {
                writer.write(inputArea.getText());
                statusLabel.setText("File saved: " + currentFile.getName());
            } catch (IOException e) {
                statusLabel.setText("Error saving file");
            }
        }
    }

    private void loadFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".java");
            }
            public String getDescription() {
                return "Java Files (*.java)";
            }
        });
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
                inputArea.setText("");
                String line;
                while ((line = reader.readLine()) != null) {
                    inputArea.setText(inputArea.getText() + line + "\n");
                }
                statusLabel.setText("File loaded: " + currentFile.getName());
            } catch (IOException e) {
                statusLabel.setText("Error loading file");
            }
        }
    }

    private void compileAndRun(String code) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> {
            try {
                // Create temporary directory for compilation
                File tempDir = new File(System.getProperty("java.io.tmpdir"), "javacompile_" + System.currentTimeMillis());
                tempDir.mkdir();
                
                // Extract class names and create files
                Pattern classPattern = Pattern.compile("public\\s+class\\s+(\\w+)");
                Matcher matcher = classPattern.matcher(code);
                String mainClassName = "TempClass";
                
                if (matcher.find()) {
                    mainClassName = matcher.group(1);
                }
                
                File sourceFile = new File(tempDir, mainClassName + ".java");
                try (FileWriter writer = new FileWriter(sourceFile)) {
                    writer.write(code);
                }

                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                ByteArrayOutputStream compileOutput = new ByteArrayOutputStream();
                int result = compiler.run(null, null, new PrintStream(compileOutput), sourceFile.getPath());

                if (result != 0) {
                    return "<html><body style='font-family: Monospaced; font-size: 14px; color: red;'>" +
                            compileOutput.toString().replace("\n", "<br>") + "</body></html>";
                }

                // Run with timeout
                ProcessBuilder pb = new ProcessBuilder();
                pb.command(buildCommand(tempDir, mainClassName));
                pb.redirectErrorStream(true);
                Process process = pb.start();

                StringBuilder runOutput = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        runOutput.append(line).append("<br>");
                    }
                }

                // Cleanup
                deleteDirectory(tempDir);
                
                return "<html><body style='font-family: Monospaced; font-size: 14px; color: green;'>" +
                        runOutput + "</body></html>";
                
            } catch (IOException e) {
                return "<html><body style='color:red;'>Error: " + e.getMessage() + "</body></html>";
            }
        });

        try {
            String result = future.get(PROCESS_TIMEOUT, TimeUnit.SECONDS);
            outputArea.setText(result);
            statusLabel.setText(result.contains("color: red") ? "Compilation/Execution failed" : "Execution complete");
        } catch (TimeoutException e) {
            outputArea.setText("<html><body style='color:red;'>Error: Process timed out after " + PROCESS_TIMEOUT + " seconds</body></html>");
            statusLabel.setText("Execution timed out");
            future.cancel(true);
        } catch (Exception e) {
            outputArea.setText("<html><body style='color:red;'>Error: " + e.getMessage() + "</body></html>");
            statusLabel.setText("Error");
        } finally {
            executor.shutdown();
        }
    }

    private String[] buildCommand(File tempDir, String mainClassName) {
        String[] baseCommand = {"java", "-cp", tempDir.getPath(), mainClassName};
        String args = argsField.getText().trim();
        if (!args.isEmpty()) {
            String[] argsArray = args.split("\\s+");
            String[] fullCommand = new String[baseCommand.length + argsArray.length];
            System.arraycopy(baseCommand, 0, fullCommand, 0, baseCommand.length);
            System.arraycopy(argsArray, 0, fullCommand, baseCommand.length, argsArray.length);
            return fullCommand;
        }
        return baseCommand;
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new JavaCodeCompilerGUI().setVisible(true));
    }
}