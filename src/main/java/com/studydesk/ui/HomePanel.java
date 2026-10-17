package com.studydesk.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class HomePanel extends JPanel {
    private final Color muted;
    private final Color textColor;
    private final JPanel recentList;
    private final JLabel importedPdfsCount;

    public HomePanel(Color muted, Color textColor, Runnable onImport) {
        this.muted = muted;
        this.textColor = textColor;

        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel subtitle = new JLabel("Keep your learning organized.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 15));
        subtitle.setForeground(muted);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(subtitle);
        add(Box.createVerticalStrut(22));

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

        JButton importButton = new JButton("+  Import PDF");
        importButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        importButton.setForeground(Color.WHITE);
        importButton.setBackground(new Color(66, 99, 235));
        importButton.setFocusPainted(false);
        importButton.setBorder(new EmptyBorder(12, 18, 12, 18));
        importButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        importButton.addActionListener(event -> onImport.run());

        welcome.add(welcomeText, BorderLayout.CENTER);
        welcome.add(importButton, BorderLayout.EAST);
        add(welcome);
        add(Box.createVerticalStrut(25));

        JLabel statsHeading = new JLabel("Your library");
        statsHeading.setFont(new Font("SansSerif", Font.BOLD, 18));
        statsHeading.setForeground(textColor);
        add(statsHeading);
        add(Box.createVerticalStrut(12));

        JPanel stats = new JPanel(new GridLayout(1, 3, 14, 0));
        stats.setOpaque(false);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JPanel pdfCard = createStatCard(
                "Imported PDFs", "0", "Your study materials");
        importedPdfsCount = (JLabel) pdfCard.getComponent(1);
        stats.add(pdfCard);
        stats.add(createStatCard(
                "Subjects", "0", "Organize your learning"));
        stats.add(createStatCard(
                "Bookmarks", "0", "Save important pages"));

        add(stats);
        add(Box.createVerticalStrut(25));

        JLabel recentHeading = new JLabel("Recent PDFs");
        recentHeading.setFont(new Font("SansSerif", Font.BOLD, 18));
        recentHeading.setForeground(textColor);
        add(recentHeading);
        add(Box.createVerticalStrut(10));

        recentList = new JPanel(new BorderLayout());
        recentList.setBackground(Color.WHITE);
        recentList.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 234, 242)),
                new EmptyBorder(25, 15, 25, 15)
        ));
        recentList.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(recentList);
    }

    private JPanel createStatCard(
            String title, String number, String description) {
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
        numberLabel.setFont(new Font("SansSerif", Font.BOLD, 25));
        numberLabel.setForeground(textColor);
        numberLabel.setBorder(new EmptyBorder(5, 0, 2, 0));

        JLabel detailLabel = new JLabel(description);
        detailLabel.setForeground(muted);
        detailLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));

        card.add(titleLabel);
        card.add(numberLabel);
        card.add(detailLabel);
        return card;
    }

    public JPanel getRecentList() {
        return recentList;
    }

    public JLabel getImportedPdfsCountLabel() {
        return importedPdfsCount;
    }
}
