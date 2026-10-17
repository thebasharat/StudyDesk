
package com.studydesk;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.studydesk.services.FileUpload;
import com.studydesk.ui.HomePanel;
import com.studydesk.ui.OpenPDF;


public class Main extends JFrame {

    private final Color background = new Color(245, 247, 251);
    private final Color sidebarColor = new Color(24, 35, 58);
    private final Color primary = new Color(66, 99, 235);
    private final Color textColor = new Color(35, 45, 65);
    private final Color muted = new Color(120, 130, 150);

    private JPanel content;
    private JTextField searchField;
    private JLabel pageTitle;
    private JPanel recentList;
    private JLabel importedPdfsCount;
    private final FileUpload fileUpload = new FileUpload();
    private final List<Path> importedPdfs = new ArrayList<>();

private void loadSavedPdfs() {
    Path library = Path.of(
            System.getProperty("user.home"),
            "StudyDesk",
            "library"
    );

    try {
        Files.createDirectories(library);

        importedPdfs.clear();

        try (var files = Files.list(library)) {
            files.filter(Files::isRegularFile)
                 .filter(path -> path.getFileName()
                         .toString()
                         .toLowerCase()
                         .endsWith(".pdf"))
                 .sorted()
                 .forEach(importedPdfs::add);
        }
    } catch (IOException ex) {
        JOptionPane.showMessageDialog(
                this,
                "Could not load your saved PDFs:\n" + ex.getMessage(),
                "Library Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}


    
public Main() {
    setTitle("StudyDesk");
    setSize(1200, 760);
    setMinimumSize(new Dimension(900, 600));
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);
    setLayout(new BorderLayout());
    getContentPane().setBackground(background);

    add(createSidebar(), BorderLayout.WEST);
    add(createMainArea(), BorderLayout.CENTER);

    loadSavedPdfs();
    showPage("Home");
}


    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(sidebarColor);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(25, 16, 20, 16));

        JLabel logo = new JLabel("  StudyDesk");
        logo.setFont(new Font("SansSerif", Font.BOLD, 23));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tagline = new JLabel("  YOUR PERSONAL LIBRARY");
        tagline.setFont(new Font("SansSerif", Font.PLAIN, 9));
        tagline.setForeground(new Color(165, 180, 210));
        tagline.setBorder(new EmptyBorder(8, 0, 30, 0));
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(logo);
        sidebar.add(tagline);

        String[] items = {
                "Home", "Library", "Subjects", "Settings"
        };

        for (String item : items) {
            JButton button = new JButton("   " + item);
            button.setFont(new Font("SansSerif", Font.PLAIN, 15));
            button.setForeground(Color.WHITE);
            button.setBackground(sidebarColor);
            button.setBorder(new EmptyBorder(13, 8, 13, 8));
            button.setFocusPainted(false);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            button.addActionListener(e -> showPage(item));
            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(5));
        }

        sidebar.add(Box.createVerticalGlue());

        JLabel footer = new JLabel("StudyDesk  •  Version 1.0");
        footer.setFont(new Font("SansSerif", Font.PLAIN, 10));
        footer.setForeground(new Color(165, 180, 210));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(footer);

        return sidebar;
    }

    private JPanel createMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(background);
        main.setBorder(new EmptyBorder(24, 30, 24, 30));

        JPanel top = new JPanel(new BorderLayout(15, 0));
        top.setOpaque(false);

        pageTitle = new JLabel("Welcome back");
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 27));
        pageTitle.setForeground(textColor);

        searchField = new JTextField();
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        searchField.setPreferredSize(new Dimension(240, 42));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 240)),
                new EmptyBorder(8, 12, 8, 12)
        ));
        searchField.setToolTipText("Search your imported PDFs");
        searchField.addActionListener(e -> filterRecent(searchField.getText()));

        top.add(pageTitle, BorderLayout.WEST);
        top.add(searchField, BorderLayout.EAST);

        content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(25, 0, 0, 0));

        main.add(top, BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);

        return main;
    }

    private void showPage(String page) {
        pageTitle.setText(page.equals("Home") ? "Welcome back" : page);
        content.removeAll();

        switch (page) {
            case "Home" -> {
                HomePanel homePanel = new HomePanel(
                        muted, textColor, this::importPdfAndRefresh);
                recentList = homePanel.getRecentList();
                importedPdfsCount = homePanel.getImportedPdfsCountLabel();
                content.add(homePanel, BorderLayout.CENTER);
                refreshRecentPdfs();
            }
            case "Library" -> content.add(createLibraryPage(), BorderLayout.CENTER);
            case "Subjects" -> content.add(createSubjectsPage(), BorderLayout.CENTER);
            case "Settings" -> content.add(createSettingsPage(), BorderLayout.CENTER);
            default -> content.add(createHomePage(), BorderLayout.CENTER);
        }

        content.revalidate();
        content.repaint();
    }

    private JPanel createHomePage() {
        JPanel page = new JPanel();
        page.setOpaque(false);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));

        JLabel subtitle = new JLabel("Keep your learning organized.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 15));
        subtitle.setForeground(muted);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        page.add(subtitle);
        page.add(Box.createVerticalStrut(22));

        JPanel welcome = new JPanel(new BorderLayout(15, 10));
        welcome.setBackground(new Color(231, 237, 255));
        welcome.setBorder(new EmptyBorder(24, 25, 24, 25));
        welcome.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        JPanel welcomeText = new JPanel();
        welcomeText.setOpaque(false);
        welcomeText.setLayout(new BoxLayout(welcomeText, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("Your next chapter starts here");
        heading.setFont(new Font("SansSerif", Font.BOLD, 21));
        heading.setForeground(textColor);

        JLabel description = new JLabel(
                "Build your personal library and study at your own pace."
        );
        description.setFont(new Font("SansSerif", Font.PLAIN, 13));
        description.setForeground(muted);
        description.setBorder(new EmptyBorder(10, 0, 0, 0));

        welcomeText.add(heading);
        welcomeText.add(description);

        JButton importButton = createPrimaryButton("+  Import PDF");
        importButton.addActionListener(e -> {
            Path imported = fileUpload.importPdf(this);
        
            if (imported != null) {
                importedPdfs.add(imported);
                refreshRecentPdfs();
            }
        });

        welcome.add(welcomeText, BorderLayout.CENTER);
        welcome.add(importButton, BorderLayout.EAST);

        page.add(welcome);
        page.add(Box.createVerticalStrut(25));

        JLabel statsHeading = sectionHeading("Your library");
        page.add(statsHeading);
        page.add(Box.createVerticalStrut(12));

        JPanel stats = new JPanel(new GridLayout(1, 3, 14, 0));
        stats.setOpaque(false);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        stats.add(createStatCard(
            "Imported PDFs",
            String.valueOf(importedPdfs.size()),
            "Your study materials"
    ));
    
    stats.add(createStatCard(
            "Subjects",
            "0",
            "Organize your learning"
    ));
    
    stats.add(createStatCard(
            "Bookmarks",
            "0",
            "Save important pages"
    ));

        page.add(stats);
        page.add(Box.createVerticalStrut(25));

        JLabel recentHeading = sectionHeading("Recent PDFs");
        page.add(recentHeading);
        page.add(Box.createVerticalStrut(10));

        recentList = new JPanel(new BorderLayout());
        recentList.setBackground(Color.WHITE);
        recentList.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 234, 242)),
                new EmptyBorder(25, 15, 25, 15)
        ));

        JLabel empty = new JLabel(
                "No PDFs imported yet. Import a PDF to get started.",
                SwingConstants.CENTER
        );
        empty.setForeground(muted);
        empty.setFont(new Font("SansSerif", Font.PLAIN, 14));
        recentList.add(empty, BorderLayout.CENTER);

        page.add(recentList);

        return page;
    }

    
private JPanel createStatCard(String title, String number, String detail) {
    JPanel card = new JPanel();
    card.setBackground(Color.WHITE);
    card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
    card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 234, 242)),
            new EmptyBorder(14, 16, 14, 16)
    ));

    JLabel titleLabel = new JLabel(title);
    titleLabel.setForeground(muted);
    titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

    JLabel numberLabel = new JLabel(number);
    numberLabel.setForeground(textColor);
    numberLabel.setFont(new Font("SansSerif", Font.BOLD, 25));
    numberLabel.setBorder(new EmptyBorder(5, 0, 2, 0));

    if (title.equals("Imported PDFs")) {
        importedPdfsCount = numberLabel;
    }

    JLabel detailLabel = new JLabel(detail);
    detailLabel.setForeground(muted);
    detailLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));

    card.add(titleLabel);
    card.add(numberLabel);
    card.add(detailLabel);

    return card;
}

    private void importPdfAndRefresh() {
        Path imported = fileUpload.importPdf(this);
        if (imported != null) {
            importedPdfs.add(imported);
            refreshRecentPdfs();
        }
    }

    private JPanel createLibraryPage() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setOpaque(false);

        JLabel info = new JLabel(
                "<html><div style='text-align:center'>"
                + "<h2>Your PDF library</h2>"
                + "<p>Imported PDFs will appear here.</p>"
                + "</div></html>",
                SwingConstants.CENTER
        );
        info.setForeground(textColor);

        JButton importButton = createPrimaryButton("+  Import PDF");
        importButton.addActionListener(e -> {
            Path imported = fileUpload.importPdf(this);
        
            if (imported != null) {
                importedPdfs.add(imported);
                refreshRecentPdfs();
            }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        top.setOpaque(false);
        top.add(importButton);

        panel.add(top, BorderLayout.NORTH);
        panel.add(info, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSubjectsPage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel info = new JLabel(
                "<html><div style='text-align:center'>"
                + "<h2>Your subjects</h2>"
                + "<p>Organize your PDFs by subject in a future step.</p>"
                + "</div></html>",
                SwingConstants.CENTER
        );
        info.setForeground(textColor);
        panel.add(info, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSettingsPage() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel heading = sectionHeading("StudyDesk settings");
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel storage = new JLabel(
                "Library location: Not configured yet"
        );
        storage.setForeground(muted);
        storage.setBorder(new EmptyBorder(15, 0, 0, 0));
        storage.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel note = new JLabel(
                "External-drive storage will be configured in the import step."
        );
        note.setForeground(muted);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(heading);
        panel.add(storage);
        panel.add(note);

        return panel;
    }

    private JLabel sectionHeading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 18));
        label.setForeground(textColor);
        return label;
    }

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(primary);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(12, 18, 12, 18));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

   

    private void showImportMessage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a PDF to import");
        chooser.setFileFilter(
            new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf")
        );
    
        int result = chooser.showOpenDialog(this);
    
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
    
        Path source = chooser.getSelectedFile().toPath();
    
        if (!Files.isRegularFile(source)
                || !source.getFileName().toString()
                    .toLowerCase().endsWith(".pdf")) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a valid PDF file.",
                "Invalid File",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
    
        Path library = Path.of(
            System.getProperty("user.home"),
            "StudyDesk",
            "library"
        );
    
        try {
            Files.createDirectories(library);
    
            String fileName = source.getFileName().toString();
            Path destination = library.resolve(fileName);
    
            if (Files.exists(destination)) {
                String name = fileName.substring(
                    0, fileName.length() - 4
                );
                destination = library.resolve(
                    name + "_" + System.currentTimeMillis() + ".pdf"
                );
            }
    
            Files.copy(source, destination);
    
            importedPdfs.add(destination);
    
            refreshRecentPdfs();
    
            JOptionPane.showMessageDialog(
                this,
                "PDF imported successfully!\n\nSaved to:\n"
                    + destination,
                "Import Successful",
                JOptionPane.INFORMATION_MESSAGE
            );
    
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Could not import the PDF.\n\n" + ex.getMessage(),
                "Import Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    


    private void filterRecent(String query) {
        if (recentList == null) {
            return;
        }

        recentList.removeAll();

        String message = query.isBlank()
                ? "No PDFs imported yet. Import a PDF to get started."
                : "No matching PDFs found.";

        JLabel empty = new JLabel(message, SwingConstants.CENTER);
        empty.setForeground(muted);
        recentList.add(empty, BorderLayout.CENTER);

        recentList.revalidate();
        recentList.repaint();
    }

private void refreshRecentPdfs() {
    if (recentList == null) {
        return;
    }
    if (importedPdfsCount != null) {
        importedPdfsCount.setText(String.valueOf(importedPdfs.size()));
    }

    recentList.removeAll();

    if (importedPdfs.isEmpty()) {
        JLabel empty = new JLabel(
            "No PDFs imported yet. Import a PDF to get started.",
            SwingConstants.CENTER
        );
        empty.setForeground(muted);
        recentList.add(empty, BorderLayout.CENTER);
    } else {
        JPanel list = new JPanel();
        list.setBackground(Color.WHITE);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        
for (Path pdf : importedPdfs) {
    JPanel pdfRow = new JPanel(new BorderLayout(10, 0));
    pdfRow.setBackground(Color.WHITE);
    pdfRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
    pdfRow.setAlignmentX(Component.LEFT_ALIGNMENT);

    JButton pdfButton = new JButton(
            "📄  " + pdf.getFileName().toString()
    );
    pdfButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
    pdfButton.setHorizontalAlignment(SwingConstants.LEFT);
    pdfButton.setFocusPainted(false);

    pdfButton.addActionListener(e -> {
        OpenPDF pdfReader = new OpenPDF();
        pdfReader.openPdf(this, pdf.toFile());
    });

    JButton deleteButton = new JButton("Delete");
    deleteButton.setForeground(new Color(190, 45, 45));
    deleteButton.setToolTipText("Delete this PDF from your StudyDesk library");

    deleteButton.addActionListener(e -> {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Delete \"" + pdf.getFileName()
                        + "\" from your StudyDesk library?\n\n"
                        + "The original file you imported will not be deleted.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            try {
                Files.deleteIfExists(pdf);
                importedPdfs.remove(pdf);
                refreshRecentPdfs();

                JOptionPane.showMessageDialog(
                        this,
                        "PDF deleted from your StudyDesk library.",
                        "PDF Deleted",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Could not delete the PDF:\n" + ex.getMessage(),
                        "Delete Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    });

    pdfRow.add(pdfButton, BorderLayout.CENTER);
    pdfRow.add(deleteButton, BorderLayout.EAST);

    list.add(pdfRow);
    list.add(Box.createVerticalStrut(5));
}


        recentList.add(list, BorderLayout.CENTER);
    }

    recentList.revalidate();
    recentList.repaint();
}


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception ignored) {
                // Use the default Swing appearance.
            }

            new Main().setVisible(true);
        });
    }
}
