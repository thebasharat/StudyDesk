package com.studydesk.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

/**
 * StudyDesk's standalone PDF reader window.
 * Page rendering and text-position extraction happen off the Swing UI thread.
 * Highlights are visual overlays for this reading session; they are not written
 * into the PDF as real highlight annotations, so they persist after closing.
 */
public final class PDFReaderPanel {
    private static final Color APP_BACKGROUND = new Color(242, 245, 249);
    private static final Color TOOLBAR_BACKGROUND = Color.WHITE;
    private static final Color ACCENT = new Color(65, 105, 225);
    private static final Color HIGHLIGHT = new Color(255, 224, 70, 125);

    private final JFrame frame;
    private final PDDocument document;
    private final PDFRenderer renderer;
    private final File pdfFile;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "StudyDesk-PDF-Renderer");
        thread.setDaemon(true);
        return thread;
    });

    private final PageCanvas pageCanvas = new PageCanvas();
    private final JLabel pageLabel = new JLabel("Loading PDF…");
    private final JButton previousButton = new JButton("‹  Previous");
    private final JButton nextButton = new JButton("Next  ›");
    private final JButton highlightButton = new JButton("Highlight selection");
    private final JLabel statusLabel = new JLabel("Preparing reader…");
    private final JLabel pageCountLabel = new JLabel("Page — of —");
    private final JSlider zoomSlider = new JSlider(75, 200, 110);

    private final Map<Integer, List<Glyph>> glyphsByPage = new HashMap<>();
    private final Map<Integer, List<HighlightRange>> highlightsByPage = new HashMap<>();
    private int currentPage = 0;
    private int zoomPercent = 110;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private boolean renderInProgress = false;
    private int renderRequestId = 0;

    private PDFReaderPanel(JFrame parent, File pdfFile, PDDocument document) {
        this.pdfFile = pdfFile;
        this.document = document;
        this.renderer = new PDFRenderer(document);
        this.frame = new JFrame("StudyDesk  •  " + pdfFile.getName());
        buildInterface(parent);
        renderCurrentPage();
        preloadTextPositions();
    }

    /** Opens the professional reader window for the supplied PDF file. */
    public static void open(JFrame parent, File pdfFile) {
        if (pdfFile == null || !pdfFile.isFile()) {
            JOptionPane.showMessageDialog(parent, "The PDF file could not be found.",
                    "Open PDF", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Loading a large PDF can take time, so do it off the UI thread too.
        Thread loader = new Thread(() -> {
            PDDocument loaded = null;
            try {
                loaded = Loader.loadPDF(pdfFile);
                PDDocument readyDocument = loaded;
                SwingUtilities.invokeLater(() -> {
                    PDFReaderPanel reader = new PDFReaderPanel(parent, pdfFile, readyDocument);
                    reader.frame.setVisible(true);
                });
            } catch (IOException ex) {
                if (loaded != null) {
                    try { loaded.close(); } catch (IOException ignored) { }
                }
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                        "Could not open this PDF:\n" + ex.getMessage(),
                        "PDF Error", JOptionPane.ERROR_MESSAGE));
            }
        }, "StudyDesk-PDF-Loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void buildInterface(JFrame parent) {
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(760, 600));
        frame.setSize(1100, 820);
        frame.setLocationRelativeTo(parent);
        frame.getContentPane().setBackground(APP_BACKGROUND);
        frame.setLayout(new BorderLayout(0, 0));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        toolbar.setBackground(TOOLBAR_BACKGROUND);
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 230, 237)));

        styleButton(previousButton);
        styleButton(nextButton);
        styleButton(highlightButton);
        highlightButton.setBackground(new Color(255, 246, 190));
        highlightButton.setToolTipText("Select text by dragging across it, then click to highlight");
        highlightButton.setEnabled(false);

        zoomSlider.setPreferredSize(new Dimension(120, 28));
        zoomSlider.setOpaque(false);
        zoomSlider.setToolTipText("Zoom");
        JLabel zoomLabel = new JLabel("Zoom");
        zoomLabel.setForeground(new Color(80, 90, 105));

        toolbar.add(previousButton);
        toolbar.add(pageCountLabel);
        toolbar.add(nextButton);
        toolbar.add(new JLabel("   "));
        toolbar.add(zoomLabel);
        toolbar.add(zoomSlider);
        toolbar.add(highlightButton);

        pageCanvas.setBackground(APP_BACKGROUND);
        pageCanvas.setOpaque(true);
        pageCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        JScrollPane scrollPane = new JScrollPane(pageCanvas);
        scrollPane.getViewport().setBackground(APP_BACKGROUND);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        statusLabel.setForeground(new Color(100, 110, 125));
        statusLabel.setFont(statusLabel.getFont().deriveFont(12f));
        footer.add(statusLabel, BorderLayout.WEST);
        JLabel tip = new JLabel("Tip: drag across text, then choose Highlight selection");
        tip.setForeground(new Color(125, 133, 145));
        tip.setFont(tip.getFont().deriveFont(11f));
        footer.add(tip, BorderLayout.EAST);

        frame.add(toolbar, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(footer, BorderLayout.SOUTH);

        previousButton.addActionListener(e -> changePage(-1));
        nextButton.addActionListener(e -> changePage(1));
        highlightButton.addActionListener(e -> saveSelectionHighlight());
        zoomSlider.addChangeListener(e -> {
            zoomPercent = zoomSlider.getValue();
            if (!zoomSlider.getValueIsAdjusting()) renderCurrentPage();
        });

        pageCanvas.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int index = findGlyphAt(e.getX(), e.getY());
                selectionStart = index;
                selectionEnd = index;
                pageCanvas.repaint();
                updateHighlightButton();
            }
            @Override public void mouseReleased(MouseEvent e) {
                updateHighlightButton();
            }
        });
        pageCanvas.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                selectionEnd = findGlyphAt(e.getX(), e.getY());
                pageCanvas.repaint();
                updateHighlightButton();
            }
        });

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                worker.shutdownNow();
                try { document.close(); }
                catch (IOException ex) { System.err.println("Could not close PDF: " + ex.getMessage()); }
            }
        });
    }

    private void styleButton(JButton button) {
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.PLAIN, 12.5f));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 235)),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        button.setBackground(Color.WHITE);
        button.setForeground(new Color(45, 55, 70));
    }

    private void changePage(int delta) {
        int target = currentPage + delta;
        if (target >= 0 && target < document.getNumberOfPages()) {
            currentPage = target;
            selectionStart = selectionEnd = -1;
            pageCanvas.setImage(null);
            pageCanvas.revalidate();
            pageCanvas.repaint();
            renderCurrentPage();
            updateHighlightButton();
        }
    }

    private void renderCurrentPage() {
        final int page = currentPage;
        final int request = ++renderRequestId;
        final float scale = zoomPercent / 100f;
        renderInProgress = true;
        statusLabel.setText("Rendering page " + (page + 1) + "…");
        pageLabel.setText("Loading page…");
        previousButton.setEnabled(page > 0);
        nextButton.setEnabled(page < document.getNumberOfPages() - 1);
        pageCountLabel.setText("Page " + (page + 1) + " of " + document.getNumberOfPages());

        worker.submit(() -> {
            try {
                BufferedImage image = renderer.renderImage(page, scale);
                SwingUtilities.invokeLater(() -> {
                    if (!frame.isDisplayable() || request != renderRequestId) return;
                    pageCanvas.setImage(image);
                    pageCanvas.revalidate();
                    pageCanvas.repaint();
                    renderInProgress = false;
                    statusLabel.setText("Ready  •  " + pdfFile.getName());
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    if (request != renderRequestId) return;
                    renderInProgress = false;
                    statusLabel.setText("Could not render this page");
                    JOptionPane.showMessageDialog(frame, "Could not display this page:\n" + ex.getMessage(),
                            "PDF Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    private void preloadTextPositions() {
        worker.submit(() -> {
            try {
                for (int page = 0; page < document.getNumberOfPages(); page++) {
                    final int pageIndex = page;
                    List<Glyph> glyphs = extractGlyphs(pageIndex);
                    SwingUtilities.invokeLater(() -> {
                        glyphsByPage.put(pageIndex, glyphs);
                        if (pageIndex == currentPage) updateHighlightButton();
                    });
                }
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("Text selection is unavailable for this PDF"));
            }
        });
    }

    private List<Glyph> extractGlyphs(int pageIndex) throws IOException {
        List<Glyph> result = new ArrayList<>();
        PDFTextStripper stripper = new PDFTextStripper() {
            @Override protected void writeString(String text, List<TextPosition> positions) {
                for (TextPosition position : positions) {
                    String unicode = position.getUnicode();
                    if (unicode == null || unicode.isEmpty()) continue;
                    // Most PDF fonts provide one TextPosition per character. When a position
                    // contains multiple characters, use one shared box for that text run.
                    result.add(new Glyph(unicode, position.getXDirAdj(),
                            Math.max(0, position.getYDirAdj() - position.getHeightDir()),
                            Math.max(1, position.getWidthDirAdj()), Math.max(1, position.getHeightDir())));
                }
            }
        };
        stripper.setStartPage(pageIndex + 1);
        stripper.setEndPage(pageIndex + 1);
        stripper.getText(document);
        return result;
    }

    private int findGlyphAt(int screenX, int screenY) {
        List<Glyph> glyphs = glyphsByPage.get(currentPage);
        if (glyphs == null || glyphs.isEmpty()) return -1;
        double scale = zoomPercent / 100.0;
        double x = (screenX - pageCanvas.imageX) / scale;
        double y = (screenY - pageCanvas.imageY) / scale;
        int nearest = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < glyphs.size(); i++) {
            Glyph glyph = glyphs.get(i);
            Rectangle bounds = glyph.bounds();
            if (bounds.contains(x, y)) return i;
            double cx = bounds.getCenterX();
            double cy = bounds.getCenterY();
            double distance = Math.pow(cx - x, 2) + Math.pow(cy - y, 2);
            if (distance < bestDistance) { bestDistance = distance; nearest = i; }
        }
        return bestDistance < 900 ? nearest : -1;
    }

    private void updateHighlightButton() {
        boolean valid = selectionStart >= 0 && selectionEnd >= 0
                && selectionStart != selectionEnd
                && glyphsByPage.containsKey(currentPage);
        highlightButton.setEnabled(valid);
    }

    private void saveSelectionHighlight() {
        if (selectionStart < 0 || selectionEnd < 0 || selectionStart == selectionEnd) return;
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        List<Glyph> glyphs = glyphsByPage.get(currentPage);
        if (glyphs == null || glyphs.isEmpty()) return;

        try {
            addPersistentHighlight(currentPage, glyphs, start, end);
            highlightsByPage.computeIfAbsent(currentPage, key -> new ArrayList<>())
                    .add(new HighlightRange(start, end));
            savePdfSafely();
            selectionStart = selectionEnd = -1;
            pageCanvas.repaint();
            updateHighlightButton();
            statusLabel.setText("Highlight saved in PDF");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame,
                    "The highlight could not be saved:\n" + ex.getMessage(),
                    "Highlight Error", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("Could not save highlight");
        }
    }

    /** Adds a PDF-standard highlight annotation so it survives closing the reader. */
    private void addPersistentHighlight(int pageIndex, List<Glyph> glyphs, int start, int end)
            throws IOException {
        PDPage page = document.getPage(pageIndex);
        float pageHeight = page.getMediaBox().getHeight();
        List<Float> quadPoints = new ArrayList<>();
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

        int last = Math.min(end, glyphs.size() - 1);
        for (int i = Math.max(0, start); i <= last; i++) {
            Rectangle r = glyphs.get(i).bounds();
            float left = r.x;
            float right = r.x + r.width;
            float top = pageHeight - r.y;
            float bottom = pageHeight - (r.y + r.height);
            // PDF highlight quad order: upper-left, upper-right, lower-left, lower-right.
            quadPoints.add(left); quadPoints.add(top);
            quadPoints.add(right); quadPoints.add(top);
            quadPoints.add(left); quadPoints.add(bottom);
            quadPoints.add(right); quadPoints.add(bottom);
            minX = Math.min(minX, left); maxX = Math.max(maxX, right);
            minY = Math.min(minY, bottom); maxY = Math.max(maxY, top);
        }
        if (quadPoints.isEmpty()) return;
        float[] quads = new float[quadPoints.size()];
        for (int i = 0; i < quadPoints.size(); i++) quads[i] = quadPoints.get(i);
COSDictionary dictionary = new COSDictionary();
dictionary.setItem(COSName.TYPE, COSName.ANNOT);
dictionary.setName(COSName.SUBTYPE, "Highlight");

PDAnnotationTextMarkup annotation =
        (PDAnnotationTextMarkup)
                PDAnnotation.createAnnotation(dictionary);

        annotation.setQuadPoints(quads);
        annotation.setRectangle(new PDRectangle(minX, minY,
                Math.max(1, maxX - minX), Math.max(1, maxY - minY)));
        annotation.setColor(new PDColor(new float[] {1f, 0.88f, 0.15f}, PDDeviceRGB.INSTANCE));
        annotation.setConstantOpacity(0.45f);
        annotation.setContents("StudyDesk highlight");
        page.getAnnotations().add(annotation);
    }

    /** Saves to a temporary file first, then replaces the imported PDF copy. */
    private void savePdfSafely() throws IOException {
        Path source = pdfFile.toPath().toAbsolutePath();
        Path parent = source.getParent();
        Path temp = Files.createTempFile(parent, "studydesk-highlight-", ".pdf");
        boolean moved = false;
        try {
            document.save(temp.toFile());
            try {
                Files.move(temp, source, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveNotSupported) {
                Files.move(temp, source, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) Files.deleteIfExists(temp);
        }
    }

    private final class PageCanvas extends JPanel {
        private BufferedImage image;
        private int imageX = 24;
        private int imageY = 24;

        PageCanvas() { setLayout(null); }
        void setImage(BufferedImage image) { this.image = image; }

        @Override public Dimension getPreferredSize() {
            if (image == null) return new Dimension(850, 700);
            return new Dimension(image.getWidth() + 48, image.getHeight() + 48);
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (image == null) {
                g.setColor(new Color(95, 105, 120));
                g.setFont(getFont().deriveFont(15f));
                g.drawString("Loading page…", 32, 42);
                g.dispose();
                return;
            }
            imageX = Math.max(24, (getWidth() - image.getWidth()) / 2);
            imageY = 24;
            g.setColor(new Color(0, 0, 0, 24));
            g.fillRoundRect(imageX + 3, imageY + 5, image.getWidth(), image.getHeight(), 4, 4);
            g.drawImage(image, imageX, imageY, null);

            List<Glyph> glyphs = glyphsByPage.get(currentPage);
            if (glyphs != null) {
                List<HighlightRange> ranges = new ArrayList<>(highlightsByPage.getOrDefault(currentPage, List.of()));
                if (selectionStart >= 0 && selectionEnd >= 0 && selectionStart != selectionEnd) {
                    ranges.add(new HighlightRange(Math.min(selectionStart, selectionEnd), Math.max(selectionStart, selectionEnd), true));
                }
                double scale = zoomPercent / 100.0;
                for (HighlightRange range : ranges) {
                    g.setColor(range.isSelection ? new Color(70, 145, 255, 90) : HIGHLIGHT);
                    int end = Math.min(range.end, glyphs.size() - 1);
                    for (int i = Math.max(0, range.start); i <= end; i++) {
                        Rectangle r = glyphs.get(i).bounds();
                        int x = imageX + (int) Math.floor(r.x * scale);
                        int y = imageY + (int) Math.floor(r.y * scale);
                        int w = Math.max(2, (int) Math.ceil(r.width * scale));
                        int h = Math.max(3, (int) Math.ceil(r.height * scale));
                        g.fillRoundRect(x, y, w, h, 2, 2);
                    }
                }
            }
            g.dispose();
        }
    }

    private static final class Glyph {
        final String text;
        final double x, y, width, height;
        Glyph(String text, double x, double y, double width, double height) {
            this.text = text; this.x = x; this.y = y; this.width = width; this.height = height;
        }
        Rectangle bounds() { return new Rectangle((int) Math.floor(x), (int) Math.floor(y),
                Math.max(1, (int) Math.ceil(width)), Math.max(1, (int) Math.ceil(height))); }
    }

    private static final class HighlightRange {
        final int start, end;
        final boolean isSelection;
        HighlightRange(int start, int end) { this(start, end, false); }
        HighlightRange(int start, int end, boolean isSelection) {
            this.start = start; this.end = end; this.isSelection = isSelection;
        }
    }
}
