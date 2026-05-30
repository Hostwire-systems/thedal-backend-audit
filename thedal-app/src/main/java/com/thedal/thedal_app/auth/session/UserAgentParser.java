package com.thedal.thedal_app.auth.session;

/** Minimal heuristic UA parser to avoid external dependencies for basic fields. */
public class UserAgentParser {
    public static ParsedUA parse(String ua) {
        ParsedUA p = new ParsedUA();
        if (ua == null) return p;
        String lower = ua.toLowerCase();
        // Browser
        if (lower.contains("edg/")) p.browser = "Edge";
        else if (lower.contains("chrome/")) p.browser = "Chrome";
        else if (lower.contains("firefox/")) p.browser = "Firefox";
        else if (lower.contains("safari") && !lower.contains("chrome")) p.browser = "Safari";
        else if (lower.contains("opera") || lower.contains("opr/")) p.browser = "Opera";
        else if (lower.contains("trident") || lower.contains("msie")) p.browser = "IE";
        // Platform
        if (lower.contains("windows")) p.platform = "Windows";
        else if (lower.contains("android")) p.platform = "Android";
        else if (lower.contains("iphone") || lower.contains("ipad")) p.platform = "iOS";
        else if (lower.contains("mac os x")) p.platform = "macOS";
        else if (lower.contains("linux")) p.platform = "Linux";
        // Device name friendly
        if (p.browser != null && p.platform != null) p.deviceName = p.browser + " on " + p.platform;
        return p;
    }

    public static class ParsedUA {
        public String browser;
        public String platform;
        public String deviceName;
    }
}
