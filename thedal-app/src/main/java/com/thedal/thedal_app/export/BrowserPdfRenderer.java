package com.thedal.thedal_app.export;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Browser-based PDF renderer using Chrome headless for proper Tamil script support.
 * This bypasses OpenHTMLtoPDF limitations with complex script shaping.
 */
@Component
public class BrowserPdfRenderer {
    
    @Value("${chrome.path:#{null}}")
    private String chromePath;
    
    @Value("${chrome.timeout:60}")  // Increased from 30 to 60 seconds for large datasets
    private int chromeTimeoutSeconds;

    /**
     * Render HTML string directly to PDF (used by FamilyVoterCardHtmlPdfRenderer)
     */
    public byte[] renderToPdf(String html) throws Exception {
        // Calculate dynamic timeout based on HTML size for large datasets
        int dynamicTimeout = calculateTimeout(html.length());
        
        String chromeExecutable = findChromeExecutable();
        if (chromeExecutable == null) {
            throw new RuntimeException("Chrome/Chromium not found. Please install Chrome or set chrome.path property. " +
                                     "Searched paths: Chrome Program Files, Chromium Program Files, PATH variable");
        }
        
        // Create temporary directory for HTML and icons
        Path tempDir = Files.createTempDirectory("voter-cards-");
        Path tempHtml = tempDir.resolve("index.html");
        Path tempPdf = Files.createTempFile("voter-cards-", ".pdf");
        Path iconsDir = tempDir.resolve("icons");
        
        try {
            // Create icons directory and copy icon files
            Files.createDirectory(iconsDir);
            copyIconsToTempDir(iconsDir);
            
            // Enhanced HTML for better Tamil rendering
            String enhancedHtml = wrapHtmlWithTamilFonts(html);
            
            // Write HTML to temporary file
            Files.write(tempHtml, enhancedHtml.getBytes(StandardCharsets.UTF_8));
            
            // Build Chrome command for PDF generation
            ProcessBuilder pb = new ProcessBuilder(
                chromeExecutable,
                "--headless",
                "--disable-gpu",
                "--disable-software-rasterizer",
                "--disable-dev-shm-usage",
                "--no-sandbox",
                "--disable-extensions",
                "--disable-plugins",
                "--disable-images=false", // Enable images for voter photos
                "--run-all-compositor-stages-before-draw",
                "--virtual-time-budget=10000", // Allow time for font loading
                "--print-to-pdf=" + tempPdf.toAbsolutePath(),
                "--print-to-pdf-no-header",
                "file://" + tempHtml.toAbsolutePath()
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // Capture output for debugging
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            boolean finished = process.waitFor(dynamicTimeout, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Chrome PDF generation timed out after " + dynamicTimeout + "s (HTML size: " + html.length() + " chars)");
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("Chrome PDF generation failed with exit code " + exitCode + 
                                         ". Output: " + output.toString());
            }
            
            if (!Files.exists(tempPdf) || Files.size(tempPdf) == 0) {
                throw new RuntimeException("Chrome failed to generate PDF file or file is empty");
            }
            
            // Read generated PDF
            return Files.readAllBytes(tempPdf);
            
        } finally {
            // Cleanup temporary files and directory
            try { Files.deleteIfExists(tempPdf); } catch (Exception ignore) {}
            try { deleteDirectory(tempDir); } catch (Exception ignore) {}
        }
    }
    
    /**
     * Copy icon files from resources to temporary directory for Chrome access
     */
    private void copyIconsToTempDir(Path iconsDir) throws Exception {
        // Copy icons from classpath resources to temp directory
        String[] iconFiles = {"phone.png", "star.png", "family.png"};
        
        for (String iconFile : iconFiles) {
            try (var inputStream = getClass().getClassLoader().getResourceAsStream("icons/" + iconFile)) {
                if (inputStream != null) {
                    Path iconPath = iconsDir.resolve(iconFile);
                    Files.copy(inputStream, iconPath);
                }
            }
        }
    }
    
    /**
     * Recursively delete a directory and its contents
     */
    private void deleteDirectory(Path dir) throws Exception {
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted((a, b) -> -a.compareTo(b)) // Delete files before directories
                      .forEach(path -> {
                          try { Files.deleteIfExists(path); } catch (Exception e) { /* ignore */ }
                      });
            }
        }
    }
    
    private String findChromeExecutable() {
        // Check configured path first
        if (chromePath != null && !chromePath.trim().isEmpty()) {
            if (Files.exists(Paths.get(chromePath))) {
                return chromePath;
            }
        }
        
        // Common Chrome locations on Windows
        String[] windowsPaths = {
            "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
            "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
            "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\Google\\Chrome\\Application\\chrome.exe"
        };
        
        for (String path : windowsPaths) {
            if (Files.exists(Paths.get(path))) {
                return path;
            }
        }
        
        // Try environment PATH
        try {
            Process proc = new ProcessBuilder("where", "chrome").start();
            proc.waitFor(2, TimeUnit.SECONDS);
            if (proc.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                    String path = reader.readLine();
                    if (path != null && Files.exists(Paths.get(path))) {
                        return path;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore and try next option
        }
        
        return null; // Chrome not found
    }
    
    private String wrapHtmlWithTamilFonts(String bodyContent) {
        return """
            <!DOCTYPE html>
            <html lang="ta">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Family Voter Cards</title>
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link href="https://fonts.googleapis.com/css2?family=Noto+Sans+Tamil:wght@400;500;600;700&family=Noto+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
                <style>
                    @page { 
                        size: A4; 
                        margin: 15mm; 
                    }
                    
                    body { 
                        font-family: 'Noto Sans Tamil', 'Noto Sans', 'Latha', 'Vijaya', Arial, sans-serif; 
                        font-size: 10.5px; 
                        margin: 0; 
                        color: #1e3242;
                        -webkit-font-feature-settings: "kern" 1, "liga" 1;
                        font-feature-settings: "kern" 1, "liga" 1;
                        text-rendering: optimizeLegibility;
                        -webkit-text-size-adjust: 100%;
                    }
                    
                    /* Enhanced Tamil text rendering */
                    .name-local, .rel-local, .addr, 
                    [class*="tamil"], [lang="ta"] { 
                        font-family: 'Noto Sans Tamil', 'Latha', 'Vijaya', Arial, sans-serif !important;
                        direction: ltr !important;
                        unicode-bidi: normal !important;
                        text-rendering: optimizeLegibility !important;
                        -webkit-font-feature-settings: "kern" 1, "liga" 1, "calt" 1;
                        font-feature-settings: "kern" 1, "liga" 1, "calt" 1;
                        font-variant-ligatures: common-ligatures contextual;
                        -webkit-font-smoothing: antialiased;
                        -moz-osx-font-smoothing: grayscale;
                    }
                    
                    /* Existing styles will be inherited from body content */
                    
                    /* Print optimizations */
                    * {
                        -webkit-print-color-adjust: exact !important;
                        color-adjust: exact !important;
                        print-color-adjust: exact !important;
                    }
                    
                    /* Ensure fonts load before rendering */
                    .font-loading {
                        opacity: 0;
                        transition: opacity 0.3s;
                    }
                    
                    .font-loaded {
                        opacity: 1;
                    }
                </style>
                <script>
                    // Ensure fonts are loaded before PDF generation
                    document.addEventListener('DOMContentLoaded', function() {
                        document.fonts.ready.then(function() {
                            document.body.classList.remove('font-loading');
                            document.body.classList.add('font-loaded');
                            console.log('Tamil fonts loaded successfully');
                        });
                    });
                </script>
            </head>
            <body class="font-loading">
            """ + bodyContent + """
            </body>
            </html>
            """;
    }
    
    /**
     * Check if the input contains Tamil characters
     */
    public static boolean containsTamilText(List<Map<String, Object>> voters) {
        for (Map<String, Object> voter : voters) {
            if (containsTamilChars(voter.get("voterFnameL1")) ||
                containsTamilChars(voter.get("voterLnameL1")) ||
                containsTamilChars(voter.get("rlnFnameL1")) ||
                containsTamilChars(voter.get("rlnLnameL1")) ||
                containsTamilChars(voter.get("fullAddress"))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if the HTML string contains Tamil characters
     */
    public static boolean containsTamilText(String html) {
        return containsTamilChars(html);
    }
    
    private static boolean containsTamilChars(Object text) {
        if (text == null) return false;
        String str = text.toString();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            // Tamil Unicode range: U+0B80–U+0BFF
            if (c >= 0x0B80 && c <= 0x0BFF) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Calculate dynamic timeout based on HTML content size
     */
    private int calculateTimeout(int htmlLength) {
        // Base timeout + additional time for large content
        // Every 100KB of HTML gets additional 10 seconds
        int additionalTime = (htmlLength / 100000) * 10;
        int dynamicTimeout = chromeTimeoutSeconds + additionalTime;
        
        // Cap at maximum 5 minutes for very large datasets
        return Math.min(dynamicTimeout, 300);
    }
}
