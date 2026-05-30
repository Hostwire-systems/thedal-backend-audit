package com.thedal.thedal_app.settings.electionsettings;

public enum SchemeBy {
    UNION_GOVT("Union Govt."),
    STATE_GOVT("State Govt."),
    LOCAL_BODY("Local Body"),
    PARTY ("Party"),
    SELF("Self");

    private final String value;

    SchemeBy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
