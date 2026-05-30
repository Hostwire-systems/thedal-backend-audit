package com.thedal.thedal_app.voter.familyexport;

import com.thedal.thedal_app.voter.VoterEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.Color;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Minimal PDF writer producing simple card layout per voter.
 * Future: replace with richer UI (fonts for Tamil, icons, grid layout).
 */
public class FamilyVoterCardPdfWriter {
    private static final Logger log = LoggerFactory.getLogger(FamilyVoterCardPdfWriter.class);

    private static final float MARGIN = 36f; // half inch
    private static final float CARD_HEIGHT = 130f;
    private static final float CARD_WIDTH = 265f; // for multi-column
    private static final float CARD_PADDING = 8f;
    private static final float LINE_HEIGHT = 11.5f;
    private static final float COLUMN_GAP = 14f;
    private static final float HEADER_BAR_HEIGHT = 20f;
    private static final float FOOTER_BAR_HEIGHT = 16f;

    // Color palette (can be replaced with exact UI hex codes)
    private static final Color PRIMARY = hex("1976D2");
    private static final Color PRIMARY_DARK = hex("0D47A1");
    private static final Color ACCENT = hex("FB8C00");
    private static final Color BORDER_COLOR = hex("B0BEC5");
    private static final Color CARD_BG = Color.WHITE;
    private static final Color FOOTER_BG = hex("ECEFF1");
    private static final Color TEXT_PRIMARY = hex("263238");
    private static final Color TEXT_SECONDARY = hex("546E7A");
    private static final Color BADGE_BG = hex("FFF3E0");
    private static final Color BADGE_BORDER = ACCENT;

    public static void write(List<VoterEntity> voters, OutputStream out) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDType0Font unicodeFont = loadUnicodeFont(doc);
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            float usableWidth = page.getMediaBox().getWidth() - 2*MARGIN;
            int columns = Math.max(1, (int) ((usableWidth + COLUMN_GAP) / (CARD_WIDTH + COLUMN_GAP)) );
            float actualCardWidth = (usableWidth - (columns-1)*COLUMN_GAP) / columns;
            drawHeader(cs, page, unicodeFont);
            float y = page.getMediaBox().getHeight() - MARGIN - 25f; // below header
            int col = 0;
            for (VoterEntity v : voters) {
                if (y - CARD_HEIGHT < MARGIN) { // new row overflow triggers new page
                    // move to next column if possible
                    col++;
                    if (col >= columns) {
                        cs.close();
                        page = new PDPage(PDRectangle.A4);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        drawHeader(cs, page, unicodeFont);
                        y = page.getMediaBox().getHeight() - MARGIN - 25f;
                        col = 0;
                    } else {
                        y = page.getMediaBox().getHeight() - MARGIN - 25f;
                    }
                }
                float x = MARGIN + col * (actualCardWidth + COLUMN_GAP);
                drawCard(cs, page, v, x, y - CARD_HEIGHT, actualCardWidth, unicodeFont);
                y -= (CARD_HEIGHT + 6f);
            }
            cs.close();
            doc.save(out);
        }
    }

    private static void drawHeader(PDPageContentStream cs, PDPage page, PDType0Font unicodeFont) throws IOException {
        if (unicodeFont != null) {
            cs.setFont(unicodeFont, 10);
        } else {
            cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 10);
        }
        cs.beginText();
        cs.newLineAtOffset(MARGIN, page.getMediaBox().getHeight() - MARGIN - 12);
        cs.showText("Family Voter Cards Export - Generated " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(LocalDateTime.now()));
        cs.endText();
    }

    private static void drawCard(PDPageContentStream cs, PDPage page, VoterEntity v, float x, float yBottom, float width, PDType0Font unicodeFont) throws IOException {
    // Outer border & background
    cs.setLineWidth(0.8f);
    cs.setStrokingColor(BORDER_COLOR);
    cs.setNonStrokingColor(CARD_BG);
    cs.addRect(x, yBottom, width, CARD_HEIGHT);
    cs.fillAndStroke();

    // Header band (gradient simulation with two rectangles)
    cs.setNonStrokingColor(PRIMARY);
    cs.addRect(x, yBottom + CARD_HEIGHT - HEADER_BAR_HEIGHT, width, HEADER_BAR_HEIGHT);
    cs.fill();
    cs.setNonStrokingColor(PRIMARY_DARK);
    cs.addRect(x, yBottom + CARD_HEIGHT - HEADER_BAR_HEIGHT, width, 4f);
    cs.fill();

    // Footer band
    cs.setNonStrokingColor(FOOTER_BG);
    cs.addRect(x, yBottom, width, FOOTER_BAR_HEIGHT);
    cs.fill();

    float textX = x + CARD_PADDING;
    float headerMidY = yBottom + CARD_HEIGHT - HEADER_BAR_HEIGHT/2f - (LINE_HEIGHT/2f) + 3f;
    setFont(cs, unicodeFont, 9, true);
    cs.setNonStrokingColor(Color.WHITE);
    beginLine(cs, textX, headerMidY, labelPair("Serial", nz(v.getSerialNo())) + space() + labelPair("Part", nz(v.getPartNo())));
    cs.setNonStrokingColor(TEXT_PRIMARY);
    float textY = yBottom + CARD_HEIGHT - HEADER_BAR_HEIGHT - CARD_PADDING - 2f; // below header

        setFont(cs, unicodeFont, 10, false);
        beginLine(cs, textX, textY, buildName(v.getVoterFnameEn(), v.getVoterLnameEn()));
        textY -= LINE_HEIGHT;
        beginLine(cs, textX, textY, buildName(v.getVoterFnameL1(), v.getVoterLnameL1()));
        textY -= LINE_HEIGHT;
        beginLine(cs, textX, textY, buildName(v.getRlnFnameEn(), v.getRlnLnameEn()));
        textY -= LINE_HEIGHT;
        beginLine(cs, textX, textY, buildName(v.getRlnFnameL1(), v.getRlnLnameL1()));
        textY -= LINE_HEIGHT;

        beginLine(cs, textX, textY, ageRelation(v));
        textY -= LINE_HEIGHT;

    // Address wrapping
    textY = multiline(cs, unicodeFont, textX, textY, 9, nz(v.getFullAddress()), width - 2*CARD_PADDING, 2) - LINE_HEIGHT/3f;

    String caste = v.getCaste()!=null? nz(v.getCaste().getCasteName()):"";
    String party = v.getParty()!=null? nz(v.getParty().getPartyName()):"";
        String phone = nz(v.getMobileNo());
        beginLine(cs, textX, textY, labelPair("Phone", phone) + space() + labelPair("Caste", caste));
        textY -= LINE_HEIGHT;
    // Footer content
    setFont(cs, unicodeFont, 9, false);
    float footerTextY = yBottom + (FOOTER_BAR_HEIGHT/2f) - (LINE_HEIGHT/2f) + 2f;
    beginLine(cs, textX, footerTextY, labelPair("Family", nz(v.getFamilyCount())) + space() + labelPair("Party", party));

    // Icon cluster at right side header (religion / availability / party badges)
    float iconSize = 12f;
    float iconY = yBottom + CARD_HEIGHT - HEADER_BAR_HEIGHT + (HEADER_BAR_HEIGHT - iconSize)/2f;
    float rightEdge = x + width - CARD_PADDING - iconSize;
    // Party
    drawRoundedBadge(cs, rightEdge, iconY, iconSize, iconSize, 'P');
    rightEdge -= (iconSize + 4f);
    // Availability
    drawRoundedBadge(cs, rightEdge, iconY, iconSize, iconSize, 'A');
    rightEdge -= (iconSize + 4f);
    // Religion
    drawRoundedBadge(cs, rightEdge, iconY, iconSize, iconSize, 'R');

    // If we have images (religionImage / availabilityImage / party logo) attempt to draw them over badges
    try { drawEntityImage(cs, v, x + width - CARD_PADDING - iconSize, iconY, iconSize, iconSize); } catch (Exception ignored) {}
    }

    private static void drawIcon(PDPageContentStream cs, float x, float y, float size, char letter) throws IOException {
        cs.setLineWidth(0.4f);
        cs.addRect(x, y, size, size);
        cs.stroke();
        cs.beginText();
        cs.newLineAtOffset(x+2f, y+1.5f);
        cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 6);
        cs.showText(String.valueOf(letter));
        cs.endText();
    }

    private static void drawRoundedBadge(PDPageContentStream cs, float x, float y, float w, float h, char letter) throws IOException {
        cs.setLineWidth(0.6f);
        cs.setStrokingColor(BADGE_BORDER);
        cs.setNonStrokingColor(BADGE_BG);
        // Approximate rounded rect: just draw a normal rect (simplification)
        cs.addRect(x, y, w, h);
        cs.fillAndStroke();
        cs.beginText();
        cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 7);
        cs.setNonStrokingColor(ACCENT.darker());
        cs.newLineAtOffset(x + w/2f - 2.5f, y + h/2f - 3f);
        cs.showText(String.valueOf(letter));
        cs.endText();
        cs.setNonStrokingColor(Color.BLACK);
    }

    /**
     * Attempt to draw images for religion / availability / party if image names are present.
     * Placeholders: looks for resource paths:
     *  /icons/religion/<religionImage>
     *  /icons/availability/<availabilityImage>
     *  /icons/party/<partyImage>
     */
    private static void drawEntityImage(PDPageContentStream cs, VoterEntity v, float x, float y, float w, float h) throws IOException {
        // Expand if we later chain multiple images; currently stub for single party image overlay
    // Placeholder: no party logo field available; future enhancement could map partyName -> asset file.
    }

    private static boolean safe(String s) { return s != null && !s.isBlank(); }

    private static org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject loadPng(String resourcePath) {
        try (var in = FamilyVoterCardPdfWriter.class.getResourceAsStream(resourcePath)) {
            if (in == null) return null;
            return org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject.createFromByteArray(new org.apache.pdfbox.pdmodel.PDDocument(), in.readAllBytes(), resourcePath);
        } catch (Exception e) {
            log.debug("Icon load failed {}: {}", resourcePath, e.getMessage());
            return null;
        }
    }

    private static void setFont(PDPageContentStream cs, PDType0Font f, int size, boolean bold) throws IOException {
        if (f != null) {
            cs.setFont(f, size);
        } else {
            cs.setFont(bold? org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD : org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, size);
        }
    }

    private static String labelPair(String label, String value) {
        if (isEmpty(value)) return label+":"; // keep alignment
        return label+": "+value;
    }

    private static String space() { return "  "; }

    /**
     * Wrap text into multiple lines up to maxLines, returns new Y position after last printed line (minus one line height already consumed).
     */
    private static float multiline(PDPageContentStream cs, PDType0Font font, float x, float startY, int fontSize, String text, float maxWidth, int maxLines) throws IOException {
        if (text == null || text.isBlank()) return startY;
        setFont(cs, font, fontSize, false);
        String[] words = text.replace('\n',' ').split(" ");
        StringBuilder line = new StringBuilder();
        float y = startY;
        int lineCount = 0;
        for (String w : words) {
            String candidate = line.length()==0? w : line+" "+w;
            if (stringWidth(font, candidate, fontSize) > maxWidth) {
                // flush current line
                if (line.length()>0) {
                    beginLine(cs, x, y, line.toString());
                    y -= LINE_HEIGHT;
                    lineCount++;
                    if (lineCount>=maxLines) return y;
                    line.setLength(0);
                }
                // If single word longer than width, hard split
                if (stringWidth(font, w, fontSize) > maxWidth) {
                    String truncated = hardTruncate(font, w, fontSize, maxWidth);
                    beginLine(cs, x, y, truncated + (lineCount==maxLines-1?"":""));
                    y -= LINE_HEIGHT;
                    lineCount++;
                    if (lineCount>=maxLines) return y;
                } else {
                    line.append(w);
                }
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length()>0 && lineCount < maxLines) {
            beginLine(cs, x, y, line.toString());
            y -= LINE_HEIGHT;
        }
        return y;
    }

    private static float stringWidth(PDType0Font font, String s, int fontSize) throws IOException {
        if (font == null) { // approximate Helvetica width 0.5 average char * size
            return s.length() * fontSize * 0.5f;
        }
        return font.getStringWidth(s) / 1000f * fontSize;
    }

    private static String hardTruncate(PDType0Font font, String word, int fontSize, float maxWidth) throws IOException {
        for (int i=word.length(); i>0; i--) {
            String sub = word.substring(0,i) + "...";
            if (stringWidth(font, sub, fontSize) <= maxWidth) return sub;
        }
        return word.substring(0, Math.min(3, word.length()));
    }

    private static Color hex(String six) {
        return new Color(
                Integer.valueOf(six.substring(0,2),16),
                Integer.valueOf(six.substring(2,4),16),
                Integer.valueOf(six.substring(4,6),16));
    }

    private static void beginLine(PDPageContentStream cs, float x, float y, String text) throws IOException {
        if (text == null) text = "";
        cs.beginText();
        cs.newLineAtOffset(x, y);
        try {
            cs.showText(text);
        } catch (IllegalArgumentException e) {
            // Fallback: replace unsupported chars with '?'
            String sanitized = text.codePoints()
                    .map(cp -> cp <= 0x00FF ? cp : '?')
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString();
            cs.showText(sanitized);
            log.warn("PDF font missing glyphs. Sanitized line. Original='{}' Sanitized='{}'", text, sanitized);
        }
        cs.endText();
    }

    private static String buildName(String f, String l) {
        if (isEmpty(f) && isEmpty(l)) return "";
        return nz(f) + (isEmpty(l)?"":" "+l);
    }

    private static String ageRelation(VoterEntity v) {
        String age = v.getAge() != null ? v.getAge().toString() : "";
        return (age + (isEmpty(age)?"":" ") + nz(v.getRlnType())).trim();
    }

    private static boolean isEmpty(String s) { return s == null || s.isBlank(); }
    private static String nz(Object o) { return o == null ? "" : o.toString(); }
    private static String truncate(String s, int max) { if (s==null) return ""; return s.length()<=max? s : s.substring(0,max-3)+"..."; }

    /**
     * Attempts to load a Unicode capable font for Tamil + Latin. Order:
     *  1) Bundled resource /fonts/NotoSansTamil-Regular.ttf (recommended; add file there)
     *  2) Windows common fonts (Nirmala.ttf, Nirmala UI, Latha.ttf, Vijaya.ttf)
     * If none found, returns null and caller will fall back to Helvetica (will trigger per-line sanitization).
     */
    private static PDType0Font loadUnicodeFont(PDDocument doc) {
        // 1) Resource
        try {
            var in = FamilyVoterCardPdfWriter.class.getResourceAsStream("/fonts/NotoSansTamil-Regular.ttf");
            if (in != null) {
                PDType0Font f = PDType0Font.load(doc, in, false);
                log.info("Loaded Unicode font: NotoSansTamil-Regular.ttf from resources.");
                return f;
            }
        } catch (Exception e) {
            log.warn("Failed loading NotoSansTamil-Regular.ttf from resources: {}", e.getMessage());
        }
        // 2) Windows fonts directory fallbacks
        String winFonts = System.getenv("WINDIR");
        if (winFonts != null) {
            String base = winFonts + "\\Fonts\\";
            String[] candidates = {"Nirmala.ttf", "NirmalaUI.ttf", "Latha.ttf", "Vijaya.ttf"};
            for (String c : candidates) {
                java.io.File f = new java.io.File(base + c);
                if (f.exists()) {
                    try {
                        PDType0Font font = PDType0Font.load(doc, f);
                        log.info("Loaded Unicode font from system: {}", f.getName());
                        return font;
                    } catch (Exception ex) {
                        log.debug("Failed to load candidate font {}: {}", c, ex.getMessage());
                    }
                }
            }
        }
        log.warn("No Unicode Tamil font found. Tamil glyphs will be replaced with '?' in export. Add NotoSansTamil-Regular.ttf to resources/fonts.");
        return null;
    }
}
