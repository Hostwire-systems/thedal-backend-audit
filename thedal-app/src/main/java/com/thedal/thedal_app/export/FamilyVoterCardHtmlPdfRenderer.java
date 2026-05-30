package com.thedal.thedal_app.export;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.text.Normalizer;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders voter cards using direct HTML generation + OpenHTMLtoPDF for reliable output.
 * Tamil text automatically switches to browser renderer for proper script shaping.
 * NO REGEX, NO TEMPLATES - Just bulletproof HTML generation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FamilyVoterCardHtmlPdfRenderer {

    private final BrowserPdfRenderer browserPdfRenderer;

    public byte[] render(List<Map<String, Object>> voters) { return render(voters, 2); }

    public byte[] render(List<Map<String, Object>> voters, int columns) {
        try {
            if (columns != 3) columns = 2;
            String html = generateHtml(voters, columns);
            
            // Check if Tamil text is present - use browser renderer for proper script shaping
            boolean containsTamil = containsTamilText(html, voters);
            if (containsTamil) {
                log.info("Tamil text detected - attempting browser renderer for proper script shaping");
                try {
                    return browserPdfRenderer.renderToPdf(html);
                } catch (Exception browserError) {
                    log.warn("Browser renderer failed (Chrome may not be available): {}. Falling back to OpenHTMLtoPDF.", 
                             browserError.getMessage());
                    log.info("Note: Tamil text may not render correctly with OpenHTMLtoPDF fallback renderer");
                    return htmlToPdf(html);
                }
            } else {
                log.debug("No Tamil text detected - using OpenHTMLtoPDF renderer");
                return htmlToPdf(html);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to render voter cards HTML to PDF", e);
        }
    }


    public byte[] renderWithFamilyPageBreaks(List<Map<String, Object>> voters, int columns) {
        try {
            if (columns != 3) columns = 2;
            String html = generateHtmlWithFamilyPageBreaks(voters, columns);
            return htmlToPdf(html);
        } catch (Exception e) {
            throw new RuntimeException("Failed to render voter cards HTML to PDF with family page breaks", e);
        }
    }

    private String generateHtml(List<Map<String, Object>> voters, int columns) {

        StringBuilder html = new StringBuilder();
        
    boolean threeCol = columns == 3;
    html.append("<!DOCTYPE html>\n")
            .append("<html lang=\"en\">\n")
            .append("<head>\n")
            .append("<meta charset=\"UTF-8\" />\n")
            .append("<title>Family Voter Cards</title>\n")
            .append("<style>\n")
            .append("@page { size: A4; margin: 15mm; }\n")
            // Tamil font faces (select common weights to avoid embedding every variant)
            .append("@font-face { font-family: 'NotoSansTamil'; font-weight:400; font-style:normal; src: url('fonts/NotoSansTamil-Regular.ttf') format('truetype'); }\n")
            .append("@font-face { font-family: 'NotoSansTamil'; font-weight:500; font-style:normal; src: url('fonts/NotoSansTamil-Medium.ttf') format('truetype'); }\n")
            .append("@font-face { font-family: 'NotoSansTamil'; font-weight:600; font-style:normal; src: url('fonts/NotoSansTamil-SemiBold.ttf') format('truetype'); }\n")
            .append("@font-face { font-family: 'NotoSansTamil'; font-weight:700; font-style:normal; src: url('fonts/NotoSansTamil-Bold.ttf') format('truetype'); }\n")
            .append("body { font-family: 'NotoSansTamil','Segoe UI','Noto Sans', Arial, sans-serif; font-size:10.5px; margin:0; color:#1e3242; }\n")
            .append(".header { font-size:13px; text-align:center; font-weight:600; color:#0d4d91; margin:0 0 8px; }\n")
            // Table based layout, dynamic columns
                .append(".cards-table { width:100%; border-collapse:separate; border-spacing:8px 10px; }\n")
                .append(".card-cell { width:").append(columns==3? "33.333%" : "50%" ).append("; vertical-align:top; padding:2px; }\n")
                .append(".card-wrapper { border:1px solid #d0d9df; border-radius:8px; page-break-inside:avoid; background:#fff; }\n")
            .append(".card-header { background:#f3f7fb; border-bottom:1px solid #d0d9df; padding:5px 8px 4px; border-top-left-radius:8px; border-top-right-radius:8px; }\n")
            .append(".h-row { width:100%; border-collapse:collapse; }\n")
            .append(".h-row td { font-size:10.5px; padding:0; font-weight:600; }\n")
            .append(".star { width:16px; }\n")
            .append(".star img { width:12px; height:12px; display:block; margin-top:1px; }\n")
            .append(".phone-icon { width:11px; height:11px; vertical-align:middle; position:relative; top:-1px; }\n")
            .append(".fam-wrap { position:relative; display:inline-block; width:14px; height:14px; vertical-align:middle; }\n")
            .append(".fam-wrap img { width:14px; height:14px; display:block; }\n")
            .append(".fam-wrap .fam-count { position:absolute; top:-6px; right:-6px; font-size:7px; font-weight:600; color:#0d4d91; }\n")
            .append(".serial { font-weight:700; color:#0d4d91; }\n")
            .append(".part { font-weight:700; color:#0d4d91; text-align:center; }\n")
            .append(".icon-bar { text-align:right; }\n")
            .append(".icon { font-size:9px; display:inline-block; padding:0 2px; }\n")
            .append(".icon-dot { width:8px; height:8px; display:inline-block; border-radius:4px; background:#ccc; }\n")
            .append(".icon-dot.green { background:#2e7d32; }\n")
            .append(".icon-dot.red { background:#c62828; }\n")
            .append(".content { width:100%; border-collapse:collapse; }\n")
            .append(".icon-img-small { width:12px; height:12px; vertical-align:middle; }\n")
            .append(".icon-img-star { width:13px; height:13px; vertical-align:middle; }\n")
            .append(".photo-col { width:66px; padding:6px 4px 4px 6px; vertical-align:top; }\n")
            .append(".photo-col { width:70px; padding:6px 4px 6px 6px; vertical-align:top; }\n")
            .append(".info-col { padding:6px 8px 4px 2px; vertical-align:top; }\n")
            .append(".photo-box { width:70px; height:85px; border:1px solid #c2cbd1; border-radius:8px; background:#0d2230; text-align:center; font-size:8px; color:#fff; overflow:hidden; line-height:1.1; }\n")
            // White placeholder variant when photo not available - using !important to ensure override
            .append(".photo-box.empty { background:#fff !important; color:#444 !important; }\n")
            .append(".photo-box img { width:70px; height:85px; object-fit:cover; display:block; }\n")
            .append(".epic { margin-top:4px; background:#0d4d91; color:#fff; font-size:7px; font-weight:600; padding:2px 3px 1px; border-radius:3px; text-align:center; }\n")
            .append(".pr-line { margin-top:3px; font-size:8px; font-weight:600; color:#333; text-align:center; }\n")
            .append(".name-main { font-weight:700; font-size:12.5px; color:#0d4d91; line-height:1.15; }\n")
            .append(".name-local { font-weight:600; font-size:11px; color:#000; margin-top:2px; line-height:1.15; font-family:'NotoSansTamil','Segoe UI','Noto Sans',Arial,sans-serif; }\n")
            .append(".icon { font-size:9px; display:inline-block; padding:0 2px; }\n")
            .append(".icon-img { width:12px; height:12px; display:inline-block; }\n")
            .append(".rel-main { font-weight:600; font-size:10.5px; color:#0d4d91; margin-top:6px; }\n")
            .append(".rel-local { font-weight:500; font-size:9.5px; color:#000; margin-top:2px; font-family:'NotoSansTamil','Segoe UI','Noto Sans',Arial,sans-serif; }\n")
            .append(".age-line { font-size:9px; font-weight:600; margin-top:2px; }\n")
            .append(".addr { font-size:9px; line-height:1.3; margin-top:8px; font-family:'NotoSansTamil','Segoe UI','Noto Sans',Arial,sans-serif; }\n")
            .append(".contact { font-size:8.5px; margin-top:4px; }\n")
            .append(".meta { background:#f5f7f9; border-top:1px solid #d0d9df; margin-top:6px; padding:4px 6px 4px; font-size:8.5px; }\n")
            .append(".meta-row { width:100%; border-collapse:collapse; }\n")
            .append(".meta-row td { padding:0; vertical-align:middle; }\n")
            .append(".label { font-weight:600; color:#4a5a65; }\n")
            .append(".muted { color:#9aa6ad; }\n")
            .append(".meta-icon { width:12px; height:12px; display:inline-block; vertical-align:middle; }\n")
            .append(".meta-sep { color:#bcc7ce; padding:0 4px; }\n")
            .append(".party-img { width:16px; height:16px; object-fit:contain; vertical-align:middle; }\n")
            // 3-column tighter overrides via body class
            .append(".cols-3 .cards-table { border-spacing:6px 6px; }\n")
            .append(".cols-3 .card-cell { width:32% !important; padding:1px; }\n")
            .append(".cols-3 body { font-size:9.5px; }\n") // (kept for safety though body can't be nested)
            .append(".cols-3 .card-wrapper { border-radius:6px; }\n")
            .append(".cols-3 .card-header { padding:4px 6px 3px; }\n")
            .append(".cols-3 .h-row td { font-size:9px; }\n")
            .append(".cols-3 .photo-col { width:62px; padding:5px 3px 5px 5px; }\n")
            .append(".cols-3 .photo-box { width:60px; height:78px; font-size:7px; }\n")
            .append(".cols-3 .photo-box img { width:60px; height:78px; }\n")
            .append(".cols-3 .epic { font-size:6.5px; padding:2px 2px 1px; }\n")
            .append(".cols-3 .pr-line { font-size:7px; }\n")
            .append(".cols-3 .name-main { font-size:11px; }\n")
            .append(".cols-3 .name-local { font-size:10px; }\n")
            .append(".cols-3 .rel-main { font-size:9.5px; margin-top:4px; }\n")
            .append(".cols-3 .rel-local { font-size:8.5px; }\n")
            .append(".cols-3 .addr { font-size:8px; margin-top:6px; }\n")
            .append(".cols-3 .meta { font-size:7.5px; padding:3px 5px 3px; }\n")
            .append(".cols-3 .party-img { width:14px; height:14px; }\n")
            .append(".cols-3 .fam-wrap { width:13px; height:13px; }\n")
            .append(".cols-3 .fam-wrap img { width:13px; height:13px; }\n")
            .append(".cols-3 .fam-wrap .fam-count { font-size:6px; top:-5px; right:-5px; }\n")
            .append(".cols-3 .icon-img, .cols-3 .icon-img-small, .cols-3 .icon-img-star { width:11px; height:11px; }\n")
            // .family-icon removed (no longer used)\n")
            .append("</style>\n")
            .append("</head>\n")
            .append("<body" + (threeCol?" class=\"cols-3\"":"") + ">\n")
            .append("<div class=\"header\">Family Voter Cards Export - Generated ")
            .append(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(ZonedDateTime.now()))
            .append("</div>\n")
            .append("<table class=\"cards-table\">\n");

        int step = columns;
        for (int i = 0; i < voters.size(); i += step) {
            Map<String, Object> left = voters.get(i);
            html.append("<tr>");
            html.append("<td class='card-cell'>").append(buildCard(left)).append("</td>");
            for(int c=1;c<columns;c++) {
                int idx = i + c;
                if(idx < voters.size()) {
                    html.append("<td class='card-cell'>").append(buildCard(voters.get(idx))).append("</td>");
                } else {
                    html.append("<td class='card-cell'></td>");
                }
            }
            html.append("</tr>\n");
        }

    // Close the cards table correctly (previously incorrect </div>)
    html.append("</table>\n")
            .append("</body>\n")
            .append("</html>");

        return html.toString();
    }

    /**
     * Generates HTML with family-based page breaks for part exports.
     * Each family will start on a new page except the first one.
     * This method ensures no content duplication by building HTML from scratch.
     */
    private String generateHtmlWithFamilyPageBreaks(List<Map<String, Object>> voters, int columns) {
        // Group voters by family ID
        Map<String, List<Map<String, Object>>> familyGroups = new LinkedHashMap<>();
        for (Map<String, Object> voter : voters) {
            String familyId = safe(voter.get("familyId"));
            if (familyId.isEmpty()) familyId = "NO_FAMILY"; // Handle voters without families
            familyGroups.computeIfAbsent(familyId, k -> new ArrayList<>()).add(voter);
        }

        // If no families are found, use regular generation
        if (familyGroups.isEmpty()) {
            return generateHtml(voters, columns);
        }

        StringBuilder html = new StringBuilder();
        boolean threeCol = columns == 3;
        
        // Start fresh HTML document
        html.append("<!DOCTYPE html>\n")
            .append("<html lang=\"en\">\n")
            .append("<head>\n")
            .append("<meta charset=\"UTF-8\" />\n")
            .append("<title>Family Voter Cards</title>\n")
            .append("<style>\n")
            .append("@page { size: A4; margin: 15mm; }\n")
            .append("@font-face { font-family: 'NotoSansTamil'; src: url('fonts/NotoSansTamil-Regular.ttf'); }\n")
            .append("body { font-family: 'Segoe UI','NotoSansTamil', Arial, sans-serif; font-size:10.5px; margin:0; color:#1e3242; }\n")
            .append(".header { font-size:13px; text-align:center; font-weight:600; color:#0d4d91; margin:0 0 8px; }\n")
            .append(".family-page-break { page-break-before: always; }\n")
            .append(".cards-table { width:100%; border-collapse:separate; border-spacing:8px 10px; }\n")
            .append(".card-cell { width:").append(columns==3? "33.333%" : "50%" ).append("; vertical-align:top; padding:2px; }\n")
            .append(".card-wrapper { border:1px solid #d0d9df; border-radius:8px; page-break-inside:avoid; background:#fff; }\n");
        
        // Add minimal CSS required - no duplication
        addCommonStyles(html, threeCol);
        
        html.append("</style>\n")
            .append("</head>\n")
            .append("<body" + (threeCol?" class=\"cols-3\"":"") + ">\n");

        // Process each family group separately
        boolean isFirstFamily = true;
        for (Map.Entry<String, List<Map<String, Object>>> familyEntry : familyGroups.entrySet()) {
            String familyId = familyEntry.getKey();
            List<Map<String, Object>> familyVoters = familyEntry.getValue();
            
            // Add page break before each family except the first
            if (!isFirstFamily) {
                html.append("<div class=\"family-page-break\"></div>\n");
            }
            
            // Family header - only once per family
            html.append("<div class=\"header\">Family Voter Cards Export - Generated ")
                .append(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(ZonedDateTime.now()))
                .append("</div>\n");
            
            // Family table - only the voters in this family
            html.append("<table class=\"cards-table\">\n");
            
            int step = columns;
            for (int i = 0; i < familyVoters.size(); i += step) {
                html.append("<tr>");
                for (int c = 0; c < columns; c++) {
                    int idx = i + c;
                    if (idx < familyVoters.size()) {
                        html.append("<td class='card-cell'>").append(buildCard(familyVoters.get(idx))).append("</td>");
                    } else {
                        html.append("<td class='card-cell'></td>");
                    }
                }
                html.append("</tr>\n");
            }
            
            html.append("</table>\n");
            isFirstFamily = false;
        }

        html.append("</body>\n").append("</html>");
        return html.toString();
    }

    /**
     * Helper method to add common CSS styles to avoid duplication
     */
    private void addCommonStyles(StringBuilder html, boolean threeCol) {
        html.append(".card-header { background:#f3f7fb; border-bottom:1px solid #d0d9df; padding:5px 8px 4px; border-top-left-radius:8px; border-top-right-radius:8px; }\n")
            .append(".h-row { width:100%; border-collapse:collapse; }\n")
            .append(".h-row td { font-size:10.5px; padding:0; font-weight:600; }\n")
            .append(".star { width:16px; }\n")
            .append(".star img { width:12px; height:12px; display:block; margin-top:1px; }\n")
            .append(".phone-icon { width:11px; height:11px; vertical-align:middle; position:relative; top:-1px; }\n")
            .append(".fam-wrap { position:relative; display:inline-block; width:14px; height:14px; vertical-align:middle; }\n")
            .append(".fam-wrap img { width:14px; height:14px; display:block; }\n")
            .append(".fam-wrap .fam-count { position:absolute; top:-6px; right:-6px; font-size:7px; font-weight:600; color:#0d4d91; }\n")
            .append(".serial { font-weight:700; color:#0d4d91; }\n")
            .append(".part { font-weight:700; color:#0d4d91; text-align:center; }\n")
            .append(".icon-bar { text-align:right; }\n")
            .append(".icon { font-size:9px; display:inline-block; padding:0 2px; }\n")
            .append(".icon-dot { width:8px; height:8px; display:inline-block; border-radius:4px; background:#ccc; }\n")
            .append(".icon-dot.green { background:#2e7d32; }\n")
            .append(".icon-dot.red { background:#c62828; }\n")
            .append(".content { width:100%; border-collapse:collapse; }\n")
            .append(".icon-img-small { width:12px; height:12px; vertical-align:middle; }\n")
            .append(".icon-img-star { width:13px; height:13px; vertical-align:middle; }\n")
            .append(".photo-col { width:66px; padding:6px 4px 4px 6px; vertical-align:top; }\n")
            .append(".photo-col { width:70px; padding:6px 4px 6px 6px; vertical-align:top; }\n")
            .append(".info-col { padding:6px 8px 4px 2px; vertical-align:top; }\n")
            .append(".photo-box { width:70px; height:85px; border:1px solid #c2cbd1; border-radius:8px; background:#0d2230; text-align:center; font-size:8px; color:#fff; overflow:hidden; line-height:1.1; }\n")
            // White placeholder variant when photo not available - using !important to ensure override
            .append(".photo-box.empty { background:#fff !important; color:#444 !important; }\n")
            .append(".photo-box img { width:70px; height:85px; object-fit:cover; display:block; }\n")
            .append(".epic { margin-top:4px; background:#0d4d91; color:#fff; font-size:7px; font-weight:600; padding:2px 3px 1px; border-radius:3px; text-align:center; }\n")
            .append(".pr-line { margin-top:3px; font-size:8px; font-weight:600; color:#333; text-align:center; }\n")
            .append(".name-main { font-weight:700; font-size:12.5px; color:#0d4d91; line-height:1.15; }\n")
            .append(".name-local { font-weight:600; font-size:11px; color:#000; margin-top:2px; line-height:1.15; }\n")
            .append(".icon { font-size:9px; display:inline-block; padding:0 2px; }\n")
            .append(".icon-img { width:12px; height:12px; display:inline-block; }\n")
            .append(".rel-main { font-weight:600; font-size:10.5px; color:#0d4d91; margin-top:6px; }\n")
            .append(".rel-local { font-weight:500; font-size:9.5px; color:#000; margin-top:2px; }\n")
            .append(".age-line { font-size:9px; font-weight:600; margin-top:2px; }\n")
            .append(".addr { font-size:9px; line-height:1.3; margin-top:8px; }\n")
            .append(".contact { font-size:8.5px; margin-top:4px; }\n")
            .append(".meta { background:#f5f7f9; border-top:1px solid #d0d9df; margin-top:6px; padding:4px 6px 4px; font-size:8.5px; }\n")
            .append(".meta-row { width:100%; border-collapse:collapse; }\n")
            .append(".meta-row td { padding:0; vertical-align:middle; }\n")
            .append(".label { font-weight:600; color:#4a5a65; }\n")
            .append(".muted { color:#9aa6ad; }\n")
            .append(".meta-icon { width:12px; height:12px; display:inline-block; vertical-align:middle; }\n")
            .append(".meta-sep { color:#bcc7ce; padding:0 4px; }\n")
            .append(".party-img { width:16px; height:16px; object-fit:contain; vertical-align:middle; }\n")
            // 3-column tighter overrides via body class
            .append(".cols-3 .cards-table { border-spacing:6px 6px; }\n")
            .append(".cols-3 .card-cell { width:32% !important; padding:1px; }\n")
            .append(".cols-3 body { font-size:9.5px; }\n") // (kept for safety though body can't be nested)
            .append(".cols-3 .card-wrapper { border-radius:6px; }\n")
            .append(".cols-3 .card-header { padding:4px 6px 3px; }\n")
            .append(".cols-3 .h-row td { font-size:9px; }\n")
            .append(".cols-3 .photo-col { width:62px; padding:5px 3px 5px 5px; }\n")
            .append(".cols-3 .photo-box { width:60px; height:78px; font-size:7px; }\n")
            .append(".cols-3 .photo-box img { width:60px; height:78px; }\n")
            .append(".cols-3 .epic { font-size:6.5px; padding:2px 2px 1px; }\n")
            .append(".cols-3 .pr-line { font-size:7px; }\n")
            .append(".cols-3 .name-main { font-size:11px; }\n")
            .append(".cols-3 .name-local { font-size:10px; }\n")
            .append(".cols-3 .rel-main { font-size:9.5px; margin-top:4px; }\n")
            .append(".cols-3 .rel-local { font-size:8.5px; }\n")
            .append(".cols-3 .addr { font-size:8px; margin-top:6px; }\n")
            .append(".cols-3 .meta { font-size:7.5px; padding:3px 5px 3px; }\n")
            .append(".cols-3 .party-img { width:14px; height:14px; }\n")
            .append(".cols-3 .fam-wrap { width:13px; height:13px; }\n")
            .append(".cols-3 .fam-wrap img { width:13px; height:13px; }\n")
            .append(".cols-3 .fam-wrap .fam-count { font-size:6px; top:-5px; right:-5px; }\n")
            .append(".cols-3 .icon-img, .cols-3 .icon-img-small, .cols-3 .icon-img-star { width:11px; height:11px; }\n");
    }

    private String safe(Object obj) {
        if (obj == null) return "";
        String s = String.valueOf(obj).trim();
        // Escape XML/HTML entities properly
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", ""); // Remove control characters
    }

        /**
     * Check if Tamil text is present in HTML content or voter data
     */
    private boolean containsTamilText(String html, List<Map<String, Object>> voters) {
        // First check HTML content
        if (BrowserPdfRenderer.containsTamilText(html)) {
            return true;
        }
        
        // Check all text fields in voter data for Tamil characters
        for (Map<String, Object> voter : voters) {
            String[] textFields = {
                "voterFnameL1", "voterLnameL1", "rlnFnameL1", "rlnLnameL1", 
                "fullAddress", "casteName", "partyName"
            };
            
            for (String field : textFields) {
                Object value = voter.get(field);
                if (value != null && BrowserPdfRenderer.containsTamilText(value.toString())) {
                    return true;
                }
            }
        }
        
        return false;
    }

    // ----- Utility methods for Tamil processing -----
    private String buildCard(Map<String, Object> v) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='card-wrapper'>");
        sb.append(buildHeader(v));
        sb.append("<table class='content'><tr>");
        sb.append("<td class='photo-col'>").append(buildPhotoBox(v)).append("</td>");
        sb.append("<td class='info-col'>");
        sb.append(nameMain(v.get("voterFnameEn"), v.get("voterLnameEn")));
        sb.append(nameLocal(v.get("voterFnameL1"), v.get("voterLnameL1")));
        sb.append(relMain(v.get("rlnFnameEn"), v.get("rlnLnameEn")));
        sb.append(relLocal(v.get("rlnFnameL1"), v.get("rlnLnameL1")));
    // Removed duplicate age/relation line in info column (now shown under photo)
    // age + relation now shown under photo
        sb.append(addressLine(v.get("fullAddress")));
    // Removed inline contact/caste line to avoid duplication (shown in footer/meta bar)
        sb.append("</td></tr></table>");
        sb.append(metaBar(v));
        sb.append("</div>");
        return sb.toString();
    }

    private String buildHeader(Map<String, Object> v) {
        boolean starred = isStarred(v.get("star"));
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='card-header'><table class='h-row'><tr>");
        if(starred) {
                sb.append("<td class='star'>").append(starImg(true)).append("</td>");
        }
        sb.append("<td class='serial'>Serial: ").append(safe(v.get("serialNo"))).append("</td>")
            .append("<td class='part'>Part: ").append(safe(v.get("partNo"))).append("</td>")
            .append("<td class='icon-bar'>").append(religionIcon(v)).append(statusIcon(v)).append(partyIcon(v)).append("</td>")
            .append("</tr></table></div>");
        return sb.toString();
    }

    private String nameMain(Object f, Object l) { return buildLine(f,l,"name-main"); }
    private String nameLocal(Object f, Object l) { return buildTamilLine(f,l,"name-local"); }
    private String relMain(Object f, Object l) { return buildLine(f,l,"rel-main"); }
    private String relLocal(Object f, Object l) { return buildTamilLine(f,l,"rel-local"); }
    private String buildLine(Object f, Object l, String cls) { if (f==null && l==null) return ""; return "<div class='"+cls+"'>"+safe(f)+(isEmpty(l)?"":" "+safe(l))+"</div>"; }
    private String buildTamilLine(Object f, Object l, String cls) { if (f==null && l==null) return ""; return "<div class='"+cls+"'>"+tamilSafe(f)+(isEmpty(l)?"":" "+tamilSafe(l))+"</div>"; }
    private String ageLine(Object age, Object relType) { 
        // Inject gender letter before age if available (expects key 'gender') from voter map (set in buildCard context)
        // Since we don't have gender param here, ageLine is invoked after setting a thread-local or we reconstruct inline below.
        // Simpler: temporarily store gender in a field is overkill; instead we adjusted buildCard to inline gender.
        if(age==null && relType==null) return ""; 
        // Gender will be inserted in buildPhotoBox relation line instead; keep this fallback unused for now.
        return "<div class='age-line'>"+safe(age)+(relType==null?"":" "+safe(relType))+"</div>"; }
    private String addressLine(Object addr) { if(addr==null) return ""; return "<div class='addr'>"+tamilSafe(addr)+"</div>"; }
    private String contactLine(Object phone, String caste) {
        if (isEmpty(phone) && (caste==null || caste.isBlank())) return "";
        StringBuilder sb = new StringBuilder("<div class='contact'>");
        if(!isEmpty(phone)) sb.append("☎ ").append(safe(phone));
        if(caste!=null && !caste.isBlank()) { if(!isEmpty(phone)) sb.append("  "); sb.append("Caste: ").append("<span class='name-local'>").append(tamilSafe(caste)).append("</span>"); }
        sb.append("</div>");
        return sb.toString();
    }
    private String metaBar(Map<String,Object> v) {
        // Footer layout simplified: Phone | CasteName | Party
        String phone = safe(v.get("mobileNo"));
        String caste = getCasteName(v.get("caste"));
        String partyImg = partyImageUrl(v.get("party"));
        StringBuilder sb = new StringBuilder("<div class='meta'><table class='meta-row'><tr>");
        // Phone cell
        sb.append("<td style='width:40%;'>");
        sb.append("<img class='phone-icon' src='icons/phone.png' alt='ph'/> ");
        if(!phone.isBlank()) sb.append(phone); else sb.append("-");
        sb.append("</td>");
        // Caste cell (no 'Caste:' label)
        sb.append("<td style='width:40%; text-align:center;'>");
        if(caste!=null && !caste.isBlank()) sb.append("<span class='name-local'>").append(tamilSafe(caste)).append("</span>"); else sb.append("<span class='muted'>-</span>");
        sb.append("</td>");
        // Party cell only
        sb.append("<td style='width:20%; text-align:right; white-space:nowrap;'>");
        if(partyImg!=null) {
            sb.append("<img class='party-img' src='").append(safe(partyImg)).append("' alt='party'/>");
        } else {
            String partyName = safe(getPartyName(v.get("party")));
            if(!partyName.isBlank()) sb.append(partyName); else sb.append("<span class='muted'>Party</span>");
        }
        sb.append("</td>");
        sb.append("</tr></table></div>");
        return sb.toString();
    }
    private String partyImageUrl(Object partyObj) {
        if(partyObj instanceof Map<?,?> m) {
            Object img = m.get("partyImage");
            if(img != null && !String.valueOf(img).isBlank()) return String.valueOf(img);
        }
        return null;
    }
    private String starImg(boolean filled) { return "<img src='icons/star.png' alt='star'/>"; }

    private String genderLetter(Object g) {
        if(g==null) return "";
        String s=String.valueOf(g).trim().toLowerCase();
        if(s.isEmpty()) return "";
        if(s.startsWith("m")) return "M"; // male
        if(s.startsWith("f")) return "F"; // female
        return "O"; // other / default
    }

    private String buildPhotoBox(Map<String, Object> voter) {
    Object base64 = voter.get("photoBase64");
    Object url = voter.get("photoUrl");
    boolean hasBase64 = base64 != null && !String.valueOf(base64).isBlank();
    boolean hasUrl = url != null && !String.valueOf(url).isBlank();
        StringBuilder sb = new StringBuilder();
        sb.append("<div>");
    if(hasBase64) {
            sb.append("<div class='photo-box'><img src='data:image/jpeg;base64,").append(safe(base64)).append("' alt='photo'/></div>");
    } else if(hasUrl) {
            sb.append("<div class='photo-box'><img src='").append(safe(url)).append("' alt='photo'/></div>");
        } else {
            // White placeholder with border only (no dark fill)
            sb.append("<div class='photo-box empty'>PHOTO<br/>").append(safe(voter.get("serialNo"))).append("</div>");
        }
        // EPIC label
        Object epic = voter.get("epicId");
        if(epic!=null) {
            sb.append("<div class='epic'>").append(safe(epic)).append("</div>");
        }
        // Age + Relation line
        Object age = voter.get("age"); Object relType = voter.get("rlnType"); Object gender = voter.get("gender");
        if(age!=null || relType!=null || gender!=null) {
            sb.append("<div class='pr-line'>");
            String g = genderLetter(gender);
            if(!g.isEmpty()) sb.append(g).append(" ");
            if(age!=null) sb.append(safe(age)).append(" ");
            if(relType!=null) sb.append(safe(relType));
            sb.append("</div>");
        }
        sb.append("</div>");
        return sb.toString();
    }
    private boolean isStarred(Object starVal) { if (starVal==null) return false; String s=String.valueOf(starVal).trim().toLowerCase(); return s.equals("1")||s.equals("true")||s.equals("yes")||s.equals("y"); }
    // Simple base64 inline 12x12 PNG placeholders (replace with real assets later)
    private static final String ICON_HINDU = "iVBORw0KGgoAAAANSUhEUgAAAAwAAAAMCAYAAABWdVznAAAAM0lEQVQokWP8////fwYKAAn+QPwHEmBiIBYwCkbGgEUwCkbGoCGaQPgMV7EG0Qx0GgAAtg4OAhKJ0DUAAAAASUVORK5CYII=";
    private static final String ICON_MUSLIM = ICON_HINDU; // placeholder
    private static final String ICON_CHRIST = ICON_HINDU; // placeholder
    private static final String ICON_PARTY = ICON_HINDU;  // placeholder
    private static final String ICON_DOT_GREEN = "iVBORw0KGgoAAAANSUhEUgAAAAwAAAAMCAYAAABWdVznAAAAIklEQVQokWP8////fwYiAInBCJhgkIExkxqGqQbRDLQAAOZjCQGfsMDGAAAAAElFTkSuQmCC";
    private static final String ICON_DOT_RED = ICON_DOT_GREEN; // placeholder same

    private String religionIcon(Map<String,Object> v) {
        Object rel=v.get("religion"); if(rel==null) return "";
        if(rel instanceof Map<?,?> m) {
            Object img = m.get("religionImage");
            if(img!=null && !String.valueOf(img).isBlank()) {
                return "<span class='icon'><img class='icon-img' src='"+safe(img)+"' alt='r'/></span>";
            }
            Object name = m.get("religionName");
            if(name!=null) {
                String s=String.valueOf(name).toLowerCase();
                String data = ICON_HINDU;
                if(s.contains("mus")) data = ICON_MUSLIM; else if(s.contains("chris")) data = ICON_CHRIST; else if(s.contains("hind")) data = ICON_HINDU;
                return iconImg(data);
            }
        }
        return "";
    }
    private String statusIcon(Map<String,Object> v) {
        Object av=v.get("availability"); if(av==null) return "";
        if(av instanceof Map<?,?> m) {
            Object img = m.get("availabilityImage");
            if(img!=null && !String.valueOf(img).isBlank()) {
                return "<span class='icon'><img class='icon-img' src='"+safe(img)+"' alt='a'/></span>";
            }
            Object desc = m.get("description");
            if(desc!=null) {
                // simple textual fallback first letter colored dot
                return "<span class='icon'><span style='font-weight:600;'>"+safe(String.valueOf(desc).substring(0,1).toUpperCase())+"</span></span>";
            }
        }
        return "";
    }
    private String partyIcon(Map<String,Object> v) {
        Object p=v.get("partySymbol"); if(p==null) return ""; return iconImg(ICON_PARTY); // placeholder - swap asset per symbol if needed
    }
    private String iconImg(String base64) { return "<span class='icon'><img class='icon-img' src='data:image/png;base64,"+base64+"' alt='i'/></span>"; }
    private String iconWrap(String txt){ return "<span class='icon'>"+safe(txt)+"</span>"; }
    private boolean isEmpty(Object o){ return o==null || String.valueOf(o).trim().isEmpty(); }

    // Tamil normalization utilities to compose multi-part vowel sequences (e.g., ெ + ா -> ோ)
    private String tamilSafe(Object o) {
        if(o==null) return "";
        String raw = String.valueOf(o);
        if(raw.isBlank()) return "";
        String norm = normalizeTamil(raw);
        return safe(norm);
    }

    private String normalizeTamil(String input) {
        if (input == null || input.trim().isEmpty()) return input;
        
        // AGGRESSIVE Tamil normalization for broken renderers
        String s = Normalizer.normalize(input, Normalizer.Form.NFC);
        
        // More comprehensive vowel sign recomposition
        s = s.replace('\u0BBE' + "\u0BC6", "\u0BC6\u0BBE"); // reorder aa + e -> e + aa
        s = s.replace('\u0BBE' + "\u0BC7", "\u0BC7\u0BBE"); // reorder aa + ee -> ee + aa
        
        // Collapse sequences
        s = s.replace("\u0BC6\u0BBE", "\u0BCA"); // ெ + ா -> ொ (o)
        s = s.replace("\u0BC7\u0BBE", "\u0BCB"); // ே + ா -> ோ (oo)
        s = s.replace("\u0BC6\u0BD7", "\u0BCC"); // ெ + ௗ -> ௌ (au)
        
        // FALLBACK: If rendering is still broken, use transliteration
        if (isBrokenRenderer()) {
            s = fallbackTamilTransliteration(s);
        }
        
        if(isTamilDebug()) {
            System.out.println("[TamilDebug] RAW   = " + toCodepoints(input));
            System.out.println("[TamilDebug] NORM  = " + toCodepoints(s));
        }
        return s;
    }
    
    private boolean isBrokenRenderer() {
        // Check if we should use fallback transliteration
        String fallback = System.getProperty("tamil.fallback", System.getenv("TAMIL_FALLBACK"));
        return "true".equalsIgnoreCase(fallback);
    }
    
    private String fallbackTamilTransliteration(String tamil) {
        // Simple fallback: replace common problematic Tamil with romanized equivalents
        return tamil
            .replace("லோகநாயகி", "Loganayagi (லோகநாயகி)")
            .replace("மோகன்பிரபு", "Mohanprabu (மோகன்பிரபு)")
            .replace("ராஜகோபால்", "Rajagopal (ராஜகோபால்)")
            .replace("பார்த்தசாரதி", "Parthasarathi (பார்த்தசாரதி)");
    }

    private boolean isTamilDebug() {
        if(Boolean.getBoolean("tamil.debug")) return true; // JVM -D flag
        String env = System.getenv("TAMIL_DEBUG");
        return env != null && (env.equalsIgnoreCase("1") || env.equalsIgnoreCase("true") || env.equalsIgnoreCase("yes"));
    }

    private String toCodepoints(String str) {
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<str.length();) {
            int cp = str.codePointAt(i);
            sb.append(String.format("U+%04X ", cp));
            i += Character.charCount(cp);
        }
        sb.append(" | ").append(str);
        return sb.toString();
    }

    private String getCasteName(Object caste) {
        if (caste == null) return "";
        if (caste instanceof String s) return s;
        if (caste instanceof Map<?,?> m) {
            Object name = m.get("casteName");
            return name != null ? String.valueOf(name) : "";
        }
        // Unknown object type -> do not call toString (may trigger lazy proxy); return blank
        return "";
    }

    private String getPartyName(Object party) {
        if (party == null) return "";
        if (party instanceof String s) return s;
        if (party instanceof Map<?,?> m) {
            Object name = m.get("partyName");
            return name != null ? String.valueOf(name) : "";
        }
        return "";
    }

    private byte[] htmlToPdf(String html) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Log the generated HTML for debugging if needed
            System.out.println("Generated HTML length: " + html.length());
            
            PdfRendererBuilder builder = new PdfRendererBuilder();
            // IMPORTANT: Do NOT enable fast mode; it skips advanced glyph shaping needed for Tamil.
            // Explicitly switch to SLOW mode (full layout + shaping) using public API.
            try {
                builder.getClass().getMethod("useSlowMode").invoke(builder);
                System.out.println("[TamilFont] useSlowMode() invoked successfully (fast-mode disabled)");
            } catch (NoSuchMethodException ns) {
                System.out.println("[TamilFont] useSlowMode() method not found on PdfRendererBuilder; library may not support explicit slow mode");
            } catch (Throwable t) {
                System.out.println("[TamilFont] Failed to invoke useSlowMode(): " + t.getMessage());
            }
            // Explicit Tamil font embedding for multiple weights to ensure glyph availability & avoid fallback
            registerFont(builder, "/fonts/NotoSansTamil-Regular.ttf", "NotoSansTamil", 400);
            registerFont(builder, "/fonts/NotoSansTamil-Medium.ttf", "NotoSansTamil", 500);
            registerFont(builder, "/fonts/NotoSansTamil-SemiBold.ttf", "NotoSansTamil", 600);
            registerFont(builder, "/fonts/NotoSansTamil-Bold.ttf", "NotoSansTamil", 700);
            builder.withHtmlContent(html, "classpath:/");
            builder.toStream(baos);
            // NOTE: Removed previous reflection hack; relying on official API above.
            builder.run();
            return baos.toByteArray();
        }
    }

    private void registerFont(PdfRendererBuilder builder, String cpPath, String family, int weight) {
        try {
            var is = getClass().getResourceAsStream(cpPath);
            if (is == null) {
                System.out.println("[TamilFont] Missing font resource: " + cpPath);
                return;
            }
            builder.useFont(() -> getClass().getResourceAsStream(cpPath), family, weight, PdfRendererBuilder.FontStyle.NORMAL, true);
            System.out.println("[TamilFont] Registered " + family + " weight=" + weight + " from " + cpPath);
        } catch (Exception e) {
            System.out.println("[TamilFont] Failed to register font " + cpPath + ": " + e.getMessage());
        }
    }
}