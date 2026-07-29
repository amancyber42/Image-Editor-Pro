package com.mycompany.imageeditorpro;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;

/**
 * Image Editor Pro — Kompletni uređivač slika
 * Kompajliranje: javac ImageEditorPro.java
 * Pokretanje:    java ImageEditorPro
 */
public class ImageEditorPro extends JFrame {

    // ── Slike ─────────────────────────────────────────────────────────────────
    private BufferedImage originalImage;
    private BufferedImage currentImage;

    // ── Text overlay objekti ──────────────────────────────────────────────────
    private static class TextOverlay {
        String text;
        int x, y, fontSize;
        Color color;
        TextOverlay(String text, int x, int y, int fontSize, Color color) {
            this.text = text; this.x = x; this.y = y;
            this.fontSize = fontSize; this.color = color;
        }
    }
    private final java.util.List<TextOverlay> textOverlays = new ArrayList<>();

    // ── UI Komponente ─────────────────────────────────────────────────────────
    private ImagePanel imagePanel;
    private JLabel statusLabel;
    private JTextField widthField, heightField, mbField, cornerField, textInputField;
    private JSpinner fontSizeSpinner;
    private Color textColor = Color.WHITE;
    private JLabel colorSwatch;

    // ── Mod rada ──────────────────────────────────────────────────────────────
    private enum Mode { NONE, CROP, TEXT_PLACE }
    private Mode mode = Mode.NONE;

    // ── Crop state ────────────────────────────────────────────────────────────
    private Point cropStart, cropEnd;
    private Rectangle cropRect;

    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new ImageEditorPro().setVisible(true);
        });
    }

    public ImageEditorPro() {
        super("Image Editor Pro");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1340, 860);
        setMinimumSize(new Dimension(950, 650));
        setLocationRelativeTo(null);
        setIconImage(createIcon());
        buildUI();
    }

    private Image createIcon() {
        BufferedImage ico = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = ico.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0, 120, 215)); g.fillRoundRect(2, 2, 28, 28, 8, 8);
        g.setColor(Color.WHITE); g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("IE", 5, 23); g.dispose();
        return ico;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GRADNJA UI
    // ═════════════════════════════════════════════════════════════════════════

    private void buildUI() {
        getContentPane().setBackground(new Color(28, 28, 30));
        setLayout(new BorderLayout(0, 0));

        add(buildLeftPanel(), BorderLayout.WEST);

        imagePanel = new ImagePanel();
        add(imagePanel, BorderLayout.CENTER);

        // Status bar
        statusLabel = new JLabel("   Dobrodošli u Image Editor Pro  —  Učitajte sliku za početak.");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(200, 200, 205));
        statusLabel.setBackground(new Color(28, 28, 30));
        statusLabel.setOpaque(true);
        statusLabel.setBorder(new EmptyBorder(6, 10, 6, 10));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JScrollPane buildLeftPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(36, 36, 40));
        p.setBorder(new EmptyBorder(14, 11, 14, 11));

        // ── Naslov ──
        JLabel hdr = mkLabel("🖼  Image Editor Pro", 16, Font.BOLD, new Color(240, 240, 245));
        hdr.setAlignmentX(LEFT_ALIGNMENT);
        p.add(hdr);
        p.add(vgap(13));

        // ── DATOTEKA ──
        p.add(sectionHdr("DATOTEKA"));
        p.add(vgap(5));
        p.add(wBtn("📂  Učitaj sliku", new Color(0, 120, 215),  e -> loadImage()));
        p.add(vgap(4));
        p.add(wBtn("💾  Spremi sliku", new Color(14, 134, 58),  e -> saveImage()));
        p.add(vgap(15));

        // ── PROMJENA VELIČINE ──
        p.add(sectionHdr("PROMJENA VELIČINE"));
        p.add(vgap(5));
        widthField  = field("npr. 1920");
        heightField = field("npr. 1080");
        mbField     = field("npr. 2.5");
        p.add(row("Širina (px):", widthField));   p.add(vgap(3));
        p.add(row("Visina (px):", heightField));  p.add(vgap(3));
        p.add(row("MB limit:",    mbField));       p.add(vgap(6));
        p.add(wBtn("↔  Promijeni veličinu", new Color(82, 82, 168), e -> resizeImage()));
        p.add(vgap(15));

        // ── ROTACIJA ──
        p.add(sectionHdr("ROTACIJA"));
        p.add(vgap(5));
        JPanel rr = hRow();
        rr.add(btn("◀  Lijevo 90°",  new Color(148, 82, 18), e -> rotateImage(-90)));
        rr.add(btn("Desno 90°  ▶", new Color(148, 82, 18), e -> rotateImage(90)));
        p.add(rr);
        p.add(vgap(15));

        // ── ZAOBLJENJE / KRUG ──
        p.add(sectionHdr("ZAOBLJENI ČOŠKOVI / KRUG"));
        p.add(vgap(5));
        cornerField = field("npr. 30");
        p.add(row("Radius (px):", cornerField));
        p.add(vgap(6));
        p.add(wBtn("⬛  Zaobli čoškove",     new Color(52, 108, 152), e -> roundCorners()));
        p.add(vgap(4));
        p.add(wBtn("⭕  Napravi okruglu sliku", new Color(52, 108, 152), e -> makeCircle()));
        p.add(vgap(15));

        // ── IZREZIVANJE ──
        p.add(sectionHdr("IZREZIVANJE (CROP)"));
        p.add(vgap(5));
        p.add(wBtn("🔲  Odaberi područje mišem",  new Color(108, 44, 132), e -> startCropMode()));
        p.add(vgap(4));
        p.add(wBtn("✂  Izreži odabrano",           new Color(108, 44, 132), e -> applyCrop()));
        p.add(vgap(15));

        // ── TEKST NA SLIKU ──
        p.add(sectionHdr("TEKST NA SLIKU"));
        p.add(vgap(5));
        textInputField = field("Upiši tekst ovdje...");
        textInputField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        textInputField.setAlignmentX(LEFT_ALIGNMENT);
        p.add(textInputField);
        p.add(vgap(4));
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(32, 6, 300, 2));
        fontSizeSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        fontSizeSpinner.setAlignmentX(LEFT_ALIGNMENT);
        styleSpinner(fontSizeSpinner);
        p.add(row("Veličina fonta:", fontSizeSpinner));
        p.add(vgap(5));

        // Boja teksta
        colorSwatch = new JLabel("    ");
        colorSwatch.setOpaque(true);
        colorSwatch.setBackground(textColor);
        colorSwatch.setBorder(BorderFactory.createLineBorder(new Color(90, 90, 100), 1));
        colorSwatch.setPreferredSize(new Dimension(30, 28));
        colorSwatch.setMinimumSize(new Dimension(30, 28));
        colorSwatch.setMaximumSize(new Dimension(30, 28));
        JPanel clrRow = new JPanel(new BorderLayout(5, 0));
        clrRow.setOpaque(false);
        clrRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        clrRow.setAlignmentX(LEFT_ALIGNMENT);
        clrRow.add(btn("🎨  Odaberi boju teksta", new Color(128, 44, 104),
            e -> { Color c = JColorChooser.showDialog(this, "Boja teksta", textColor);
                   if (c != null) { textColor = c; colorSwatch.setBackground(c); } }),
            BorderLayout.CENTER);
        clrRow.add(colorSwatch, BorderLayout.EAST);
        p.add(clrRow);
        p.add(vgap(5));
        p.add(wBtn("✍  Postavi tekst (klikni na sliku)", new Color(172, 92, 0), e -> startTextMode()));
        p.add(vgap(4));
        p.add(wBtn("🗑  Obriši sav tekst",               new Color(155, 25, 25), e -> clearText()));
        p.add(vgap(15));

        // ── RESET ──
        p.add(sectionHdr("RESET"));
        p.add(vgap(5));
        p.add(wBtn("↩  Resetiraj sve promjene", new Color(182, 18, 18), e -> resetImage()));
        p.add(Box.createVerticalGlue());

        JScrollPane sp = new JScrollPane(p);
        sp.setPreferredSize(new Dimension(255, 0));
        sp.setBorder(null);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getViewport().setBackground(new Color(36, 36, 40));
        return sp;
    }

    // ── UI Pomoćne metode ─────────────────────────────────────────────────────

    private JLabel mkLabel(String t, int size, int style, Color fg) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", style, size));
        l.setForeground(fg);
        return l;
    }

    private Component sectionHdr(String t) {
        JPanel p2 = new JPanel();
        p2.setLayout(new BoxLayout(p2, BoxLayout.Y_AXIS));
        p2.setOpaque(false);
        p2.setAlignmentX(LEFT_ALIGNMENT);
        JLabel l = mkLabel(t, 10, Font.BOLD, new Color(98, 158, 218));
        l.setAlignmentX(LEFT_ALIGNMENT);
        p2.add(l);
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(68, 68, 80));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(LEFT_ALIGNMENT);
        p2.add(sep);
        return p2;
    }

    private Component vgap(int h) { return Box.createVerticalStrut(h); }

    private JTextField field(String hint) {
        final Color HINT = new Color(135, 135, 145);
        JTextField f = new JTextField();
        f.setBackground(new Color(56, 56, 62));
        f.setForeground(HINT);
        f.setCaretColor(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(72, 72, 84)),
            new EmptyBorder(3, 7, 3, 7)));
        f.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        f.setText(hint);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (f.getForeground().equals(HINT)) { f.setText(""); f.setForeground(new Color(220, 220, 228)); }
            }
            public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) { f.setText(hint); f.setForeground(HINT); }
            }
        });
        return f;
    }

    private String fVal(JTextField f) {
        if (f.getForeground().equals(new Color(135, 135, 145))) return "";
        return f.getText().trim();
    }

    private void styleSpinner(JSpinner s) {
        if (s.getEditor() instanceof JSpinner.DefaultEditor de) {
            JTextField tf = de.getTextField();
            tf.setBackground(new Color(56, 56, 62));
            tf.setForeground(new Color(220, 220, 228));
            tf.setCaretColor(Color.WHITE);
        }
    }

    private JPanel row(String lbl, Component c) {
        JPanel p2 = new JPanel(new BorderLayout(6, 0));
        p2.setOpaque(false);
        p2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        p2.setAlignmentX(LEFT_ALIGNMENT);
        JLabel l = mkLabel(lbl, 11, Font.PLAIN, new Color(172, 172, 180));
        l.setPreferredSize(new Dimension(92, 22));
        p2.add(l, BorderLayout.WEST);
        p2.add(c, BorderLayout.CENTER);
        return p2;
    }

    private JPanel hRow() {
        JPanel p2 = new JPanel(new GridLayout(1, 2, 5, 0));
        p2.setOpaque(false);
        p2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        p2.setAlignmentX(LEFT_ALIGNMENT);
        return p2;
    }

    private JButton wBtn(String text, Color bg, ActionListener al) {
        JButton b = makeBtn(text, bg);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.addActionListener(al);
        return b;
    }

    private JButton btn(String text, Color bg, ActionListener al) {
        JButton b = makeBtn(text, bg);
        b.addActionListener(al);
        return b;
    }

    private JButton makeBtn(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = getModel().isPressed() ? bg.darker().darker()
                        : getModel().isRollover() ? bg.brighter() : bg;
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(110, 32));
        return b;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // OPERACIJE SA SLIKAMA
    // ═════════════════════════════════════════════════════════════════════════

    private void loadImage() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Odaberi sliku");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Slike (JPG, PNG, GIF, BMP)", "jpg", "jpeg", "png", "gif", "bmp"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            BufferedImage raw = ImageIO.read(fc.getSelectedFile());
            if (raw == null) { err("Ne mogu pročitati datoteku kao sliku!"); return; }
            originalImage = toARGB(raw);
            currentImage  = toARGB(raw);
            textOverlays.clear();
            mode = Mode.NONE;
            cropStart = cropEnd = null;
            cropRect = null;
            status("✅  Učitano: " + fc.getSelectedFile().getName()
                + "   [" + currentImage.getWidth() + " × " + currentImage.getHeight() + " px]");
            imagePanel.repaint();
        } catch (Exception ex) { err("Greška pri učitavanju:\n" + ex.getMessage()); }
    }

    private void saveImage() {
        if (currentImage == null) { err("Nema učitane slike!"); return; }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Spremi sliku");
        var pngFilter = new javax.swing.filechooser.FileNameExtensionFilter("PNG slika (*.png)", "png");
        var jpgFilter = new javax.swing.filechooser.FileNameExtensionFilter("JPEG slika (*.jpg)", "jpg");
        fc.addChoosableFileFilter(pngFilter);
        fc.addChoosableFileFilter(jpgFilter);
        fc.setFileFilter(pngFilter);
        fc.setSelectedFile(new File("output.png"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            File f = fc.getSelectedFile();
            String name = f.getName().toLowerCase();
            boolean isJpeg = name.endsWith(".jpg") || name.endsWith(".jpeg");
            if (!name.endsWith(".png") && !isJpeg) f = new File(f.getAbsolutePath() + ".png");

            BufferedImage toSave = bakeText(currentImage);
            if (isJpeg) {
                BufferedImage rgb = new BufferedImage(toSave.getWidth(), toSave.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgb.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                g.drawImage(toSave, 0, 0, null);
                g.dispose();
                ImageIO.write(rgb, "jpg", f);
            } else {
                ImageIO.write(toSave, "png", f);
            }
            status("💾  Slika spremljena: " + f.getAbsolutePath());
        } catch (Exception ex) { err("Greška pri spremanju:\n" + ex.getMessage()); }
    }

    private void resizeImage() {
        if (currentImage == null) { err("Nema učitane slike!"); return; }
        try {
            String ws = fVal(widthField), hs = fVal(heightField), ms = fVal(mbField);
            int w = ws.isEmpty() ? currentImage.getWidth()  : Integer.parseInt(ws);
            int h = hs.isEmpty() ? currentImage.getHeight() : Integer.parseInt(hs);
            if (w <= 0 || h <= 0) { err("Širina i visina moraju biti > 0 px!"); return; }

            // MB limit
            if (!ms.isEmpty()) {
                double limitBytes = Double.parseDouble(ms) * 1024.0 * 1024.0;
                long raw = (long) w * h * 4;
                if (raw > limitBytes) {
                    double factor = Math.sqrt(limitBytes / raw);
                    w = Math.max(1, (int)(w * factor));
                    h = Math.max(1, (int)(h * factor));
                    status("⚠  MB limit: dimenzije su prilagođene na " + w + " × " + h + " px");
                }
            }
            BufferedImage r = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = r.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(currentImage, 0, 0, w, h, null);
            g2.dispose();
            currentImage = r;
            textOverlays.clear();
            status("↔  Veličina promijenjena na " + w + " × " + h + " px");
            imagePanel.repaint();
        } catch (NumberFormatException ex) { err("Unesite ispravne brojeve (samo cifre)!"); }
    }

    private void rotateImage(int degrees) {
        if (currentImage == null) { err("Nema učitane slike!"); return; }
        int w = currentImage.getWidth(), h = currentImage.getHeight();
        int nw = (Math.abs(degrees) == 90) ? h : w;
        int nh = (Math.abs(degrees) == 90) ? w : h;
        BufferedImage r = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = r.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(nw / 2.0, nh / 2.0);
        g2.rotate(Math.toRadians(degrees));
        g2.drawImage(currentImage, -w / 2, -h / 2, null);
        g2.dispose();
        currentImage = r;
        status("🔄  Slika rotirana za " + degrees + "°");
        imagePanel.repaint();
    }

    private void roundCorners() {
        if (currentImage == null) { err("Nema učitane slike!"); return; }
        String rs = fVal(cornerField);
        if (rs.isEmpty()) { err("Unesite radius zaobljenja u polje 'Radius (px)'!"); return; }
        try {
            int r = Integer.parseInt(rs);
            int w = currentImage.getWidth(), h = currentImage.getHeight();
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = out.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setComposite(AlphaComposite.Clear);
            g2.fillRect(0, 0, w, h);
            g2.setComposite(AlphaComposite.SrcOver);
            g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, r * 2, r * 2));
            g2.drawImage(currentImage, 0, 0, null);
            g2.dispose();
            currentImage = out;
            status("⬛  Čoškovi zaobljeni (radius = " + r + " px)  —  Spremi kao PNG za providnost!");
            imagePanel.repaint();
        } catch (NumberFormatException ex) { err("Radius mora biti cijeli broj!"); }
    }

    private void makeCircle() {
        if (currentImage == null) { err("Nema učitane slike!"); return; }
        int w = currentImage.getWidth(), h = currentImage.getHeight();
        int sz = Math.min(w, h);
        int sx = (w - sz) / 2, sy = (h - sz) / 2;
        BufferedImage out = new BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setComposite(AlphaComposite.Clear);
        g2.fillRect(0, 0, sz, sz);
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setClip(new Ellipse2D.Float(0, 0, sz, sz));
        g2.drawImage(currentImage, 0, 0, sz, sz, sx, sy, sx + sz, sy + sz, null);
        g2.dispose();
        currentImage = out;
        status("⭕  Slika pretvorena u krug  —  Spremi kao PNG za providnost!");
        imagePanel.repaint();
    }

    private void startCropMode() {
        if (currentImage == null) { err("Nema učitane slike!"); return; }
        mode = Mode.CROP;
        cropStart = cropEnd = null;
        cropRect = null;
        imagePanel.repaint();
        status("🔲  CROP MODE  —  Povuci mišem po slici da označiš područje za izrezivanje...");
    }

    private void applyCrop() {
        if (currentImage == null) { err("Nema učitane slike!"); return; }
        if (cropRect == null || cropRect.width <= 0 || cropRect.height <= 0) {
            err("Najprije odaberi područje!\nKlikni 'Odaberi područje mišem' pa povuci po slici."); return;
        }
        int x = Math.max(0, cropRect.x), y = Math.max(0, cropRect.y);
        int w = Math.min(currentImage.getWidth()  - x, cropRect.width);
        int h = Math.min(currentImage.getHeight() - y, cropRect.height);
        if (w <= 0 || h <= 0) { err("Odabrano područje je izvan granica slike!"); return; }

        BufferedImage sub = currentImage.getSubimage(x, y, w, h);
        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = copy.createGraphics();
        g2.drawImage(sub, 0, 0, null);
        g2.dispose();
        currentImage = copy;
        textOverlays.clear();
        mode = Mode.NONE;
        cropRect = null; cropStart = cropEnd = null;
        status("✂  Izrezano na " + w + " × " + h + " px");
        imagePanel.repaint();
    }

    private void startTextMode() {
        if (currentImage == null) { err("Nema učitane slike!"); return; }
        if (getTextInput().isEmpty()) { err("Unesite tekst u tekstualno polje!"); return; }
        mode = Mode.TEXT_PLACE;
        status("✍  TEXT MODE  —  Klikni na sliku na mjesto gdje želiš postaviti tekst...");
        imagePanel.repaint();
    }

    private void clearText() {
        textOverlays.clear();
        mode = Mode.NONE;
        status("🗑  Sav tekst je obrisan");
        imagePanel.repaint();
    }

    private void resetImage() {
        if (originalImage == null) { err("Nema učitane slike!"); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Sigurno želiš resetirati sve promjene na originalnu sliku?",
            "Potvrdi reset", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        currentImage = toARGB(originalImage);
        textOverlays.clear();
        mode = Mode.NONE;
        cropRect = null; cropStart = cropEnd = null;
        status("↩  Sve promjene su poništene — vraćena originalna slika");
        imagePanel.repaint();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // POMOĆNE METODE
    // ═════════════════════════════════════════════════════════════════════════

    private String getTextInput() {
        if (textInputField.getForeground().equals(new Color(135, 135, 145))) return "";
        return textInputField.getText().trim();
    }

    private BufferedImage toARGB(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    private BufferedImage bakeText(BufferedImage src) {
        if (textOverlays.isEmpty()) return src;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.drawImage(src, 0, 0, null);
        for (TextOverlay t : textOverlays) {
            g2.setFont(new Font("Arial", Font.BOLD, t.fontSize));
            g2.setColor(new Color(0, 0, 0, 130));
            g2.drawString(t.text, t.x + 2, t.y + 2);
            g2.setColor(t.color);
            g2.drawString(t.text, t.x, t.y);
        }
        g2.dispose();
        return out;
    }

    private void status(String msg) { statusLabel.setText("   " + msg); }
    private void err(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Greška", JOptionPane.ERROR_MESSAGE);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // IMAGE PANEL  (desni prozor za prikaz i uređivanje slike)
    // ═════════════════════════════════════════════════════════════════════════

    private class ImagePanel extends JPanel {
        private double scale = 1.0;
        private int offX = 0, offY = 0;

        ImagePanel() {
            setBackground(new Color(42, 42, 46));
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed (MouseEvent e) { onPress(e.getPoint()); }
                @Override public void mouseDragged (MouseEvent e) { onDrag (e.getPoint()); }
                @Override public void mouseReleased(MouseEvent e) { onRelease(e.getPoint()); }
                @Override public void mouseMoved   (MouseEvent e) { onMove (e.getPoint()); }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        // Pretvori koordinate panela u koordinate slike
        private Point toImg(Point p) {
            if (scale <= 0) return p;
            int ix = (int)((p.x - offX) / scale);
            int iy = (int)((p.y - offY) / scale);
            if (currentImage != null) {
                ix = Math.max(0, Math.min(currentImage.getWidth()  - 1, ix));
                iy = Math.max(0, Math.min(currentImage.getHeight() - 1, iy));
            }
            return new Point(ix, iy);
        }

        private boolean overImage(Point p) {
            return currentImage != null
                && p.x >= offX && p.x < offX + (int)(currentImage.getWidth()  * scale)
                && p.y >= offY && p.y < offY + (int)(currentImage.getHeight() * scale);
        }

        private void onMove(Point p) {
            if (mode == Mode.CROP || mode == Mode.TEXT_PLACE)
                setCursor(Cursor.getPredefinedCursor(
                    overImage(p) ? Cursor.CROSSHAIR_CURSOR : Cursor.DEFAULT_CURSOR));
            else
                setCursor(Cursor.getDefaultCursor());
        }

        private void onPress(Point p) {
            if (currentImage == null) return;
            Point ip = toImg(p);
            if (mode == Mode.CROP) {
                cropStart = ip; cropEnd = ip; cropRect = null;
            } else if (mode == Mode.TEXT_PLACE) {
                String t = getTextInput();
                if (!t.isEmpty()) {
                    int fs = (Integer) fontSizeSpinner.getValue();
                    textOverlays.add(new TextOverlay(t, ip.x, ip.y, fs, textColor));
                    mode = Mode.NONE;
                    status("✅  Tekst postavljen na (" + ip.x + ", " + ip.y + " px)");
                    repaint();
                }
            }
        }

        private void onDrag(Point p) {
            if (mode != Mode.CROP || cropStart == null) return;
            cropEnd = toImg(p);
            updateCropRect();
            repaint();
        }

        private void onRelease(Point p) {
            if (mode != Mode.CROP || cropStart == null) return;
            cropEnd = toImg(p);
            updateCropRect();
            if (cropRect != null && cropRect.width > 0 && cropRect.height > 0)
                status("✂  Odabrano: " + cropRect.width + " × " + cropRect.height + " px  —  Klikni 'Izreži odabrano'");
            repaint();
        }

        private void updateCropRect() {
            if (cropStart == null || cropEnd == null) return;
            cropRect = new Rectangle(
                Math.min(cropStart.x, cropEnd.x),
                Math.min(cropStart.y, cropEnd.y),
                Math.abs(cropEnd.x - cropStart.x),
                Math.abs(cropEnd.y - cropStart.y));
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,     RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (currentImage == null) {
                paintEmpty(g2); return;
            }

            // Izračunaj skaliranje da slika stane u panel
            int pw = getWidth(), ph = getHeight();
            int iw = currentImage.getWidth(), ih = currentImage.getHeight();
            double sx = (double) pw / iw, sy = (double) ph / ih;
            scale = Math.min(sx, sy);
            if (scale > 1.0) scale = 1.0; // ne povećavaj iznad 100%

            int dw = (int)(iw * scale), dh = (int)(ih * scale);
            offX = (pw - dw) / 2;
            offY = (ph - dh) / 2;

            // Šahovska ploča (za transparentnost)
            drawChecker(g2, offX, offY, dw, dh);

            // Crtaj sliku
            g2.drawImage(currentImage, offX, offY, dw, dh, null);

            // Tekst overlays (preview)
            for (TextOverlay t : textOverlays) {
                int tx = offX + (int)(t.x * scale);
                int ty = offY + (int)(t.y * scale);
                int tf = Math.max(6, (int)(t.fontSize * scale));
                g2.setFont(new Font("Arial", Font.BOLD, tf));
                g2.setColor(new Color(0, 0, 0, 145));
                g2.drawString(t.text, tx + 1, ty + 1);
                g2.setColor(t.color);
                g2.drawString(t.text, tx, ty);
            }

            // Crop selekcija
            if (cropRect != null && cropRect.width > 0 && cropRect.height > 0) {
                int cx = offX + (int)(cropRect.x * scale);
                int cy = offY + (int)(cropRect.y * scale);
                int cw = (int)(cropRect.width  * scale);
                int ch = (int)(cropRect.height * scale);

                // Zamutiti vanjski dio
                g2.setColor(new Color(0, 0, 0, 115));
                Area outer = new Area(new Rectangle(offX, offY, dw, dh));
                outer.subtract(new Area(new Rectangle(cx, cy, cw, ch)));
                g2.fill(outer);

                // Isprekidani obrub
                Stroke prev = g2.getStroke();
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{8, 5}, 0));
                g2.setColor(Color.WHITE);
                g2.drawRect(cx, cy, cw, ch);
                // Handles (kvadratići na uglovima)
                g2.setStroke(new BasicStroke(2f));
                int hs = 6;
                for (int[] pt : new int[][]{{cx,cy},{cx+cw,cy},{cx,cy+ch},{cx+cw,cy+ch}}) {
                    g2.setColor(Color.WHITE);
                    g2.fillRect(pt[0]-hs/2, pt[1]-hs/2, hs, hs);
                    g2.setColor(new Color(0,120,215));
                    g2.drawRect(pt[0]-hs/2, pt[1]-hs/2, hs, hs);
                }
                g2.setStroke(prev);

                // Oznaka dimenzija
                String dimStr = cropRect.width + " × " + cropRect.height + " px";
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int lw = fm.stringWidth(dimStr) + 10;
                int lx = cx + (cw - lw) / 2, ly = cy > 22 ? cy - 22 : cy + ch + 4;
                g2.setColor(new Color(0, 0, 0, 170));
                g2.fillRoundRect(lx, ly, lw, 18, 6, 6);
                g2.setColor(new Color(255, 230, 80));
                g2.drawString(dimStr, lx + 5, ly + 13);
            }

            // Banner (Crop / Text mod)
            if (mode != Mode.NONE) {
                String bText = mode == Mode.CROP
                    ? "  ✂  CROP MODE — povuci mišem po slici da odabereš područje  "
                    : "  ✍  TEXT MODE — klikni na sliku na željeno mjesto za tekst  ";
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                int bw = fm.stringWidth(bText) + 12, bh = 28;
                int bx = offX + (dw - bw) / 2, by = offY + 12;
                g2.setColor(new Color(0, 0, 0, 175));
                g2.fillRoundRect(bx, by, bw, bh, 10, 10);
                g2.setColor(new Color(255, 215, 55));
                g2.drawString(bText, bx + 6, by + 19);
            }

            // Zoom info (dolje desno)
            if (scale < 1.0) {
                String zs = String.format("%.0f%%", scale * 100);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                FontMetrics fm = g2.getFontMetrics();
                int zw = fm.stringWidth(zs) + 10;
                g2.setColor(new Color(0, 0, 0, 130));
                g2.fillRoundRect(offX + dw - zw - 4, offY + dh - 20, zw, 16, 6, 6);
                g2.setColor(new Color(200, 200, 210));
                g2.drawString(zs, offX + dw - zw, offY + dh - 7);
            }
        }

        private void paintEmpty(Graphics2D g2) {
            int cx = getWidth() / 2, cy = getHeight() / 2;
            // Centralni okvir
            g2.setColor(new Color(58, 58, 65));
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[]{10, 7}, 0));
            g2.drawRoundRect(cx - 160, cy - 100, 320, 200, 20, 20);
            g2.setStroke(new BasicStroke());

            g2.setColor(new Color(95, 95, 105));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 48));
            FontMetrics fm1 = g2.getFontMetrics();
            String icon = "🖼";
            g2.drawString(icon, cx - fm1.stringWidth(icon)/2, cy - 15);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 17));
            FontMetrics fm2 = g2.getFontMetrics();
            String msg1 = "Učitajte sliku za početak";
            g2.drawString(msg1, cx - fm2.stringWidth(msg1)/2, cy + 25);

            g2.setColor(new Color(75, 75, 85));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            FontMetrics fm3 = g2.getFontMetrics();
            String msg2 = "Klikni  📂 Učitaj sliku  (lijevo)";
            g2.drawString(msg2, cx - fm3.stringWidth(msg2)/2, cy + 50);
        }

        private void drawChecker(Graphics2D g2, int x, int y, int w, int h) {
            int sz = 12;
            for (int i = 0; i * sz < w; i++) {
                for (int j = 0; j * sz < h; j++) {
                    g2.setColor((i + j) % 2 == 0 ? new Color(185, 185, 185) : new Color(142, 142, 142));
                    g2.fillRect(x + i * sz, y + j * sz,
                        Math.min(sz, w - i * sz), Math.min(sz, h - j * sz));
                }
            }
        }
    }
}