
package com.studydesk.services;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUpload {

    public Path importPdf(JFrame parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a PDF to import");
        chooser.setFileFilter(
            new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf")
        );

        int result = chooser.showOpenDialog(parent);

        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        Path source = chooser.getSelectedFile().toPath();

        if (!Files.isRegularFile(source)
                || !source.getFileName().toString()
                    .toLowerCase().endsWith(".pdf")) {
            JOptionPane.showMessageDialog(
                parent,
                "Please select a valid PDF file.",
                "Invalid File",
                JOptionPane.WARNING_MESSAGE
            );
            return null;
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

            JOptionPane.showMessageDialog(
                parent,
                "PDF imported successfully!\n\nSaved to:\n"
                    + destination,
                "Import Successful",
                JOptionPane.INFORMATION_MESSAGE
            );

            return destination;

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                parent,
                "Could not import the PDF.\n\n" + ex.getMessage(),
                "Import Error",
                JOptionPane.ERROR_MESSAGE
            );
            return null;
        }
    }
}

