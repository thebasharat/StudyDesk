package com.studydesk.ui;

import java.io.File;
import javax.swing.JFrame;

/** Keeps the existing Main.java call compatible while using the new reader. */
public class OpenPDF {
    public void openPdf(JFrame parent, File pdfFile) {
        PDFReaderPanel.open(parent, pdfFile);
    }
}