package com.thedal.thedal_app.user;

public class Filter {
    private String type; // "username", "mobileNumber", or "name"
    private String value;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public boolean isValid() {
        if (type == null || value == null || type.trim().isEmpty() || value.trim().isEmpty()) {
            return false;
        }
        // Ensure only one valid filter type is provided
        return type.equalsIgnoreCase("username") || type.equalsIgnoreCase("mobileNumber") || type.equalsIgnoreCase("name");
    }
}