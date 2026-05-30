package com.thedal.thedal_app.volunteer;

public class MigrationResult {
    private int volunteersMigrated;
    private int activitiesMigrated;

    public MigrationResult(int volunteersMigrated, int activitiesMigrated) {
        this.volunteersMigrated = volunteersMigrated;
        this.activitiesMigrated = activitiesMigrated;
    }

    public int getVolunteersMigrated() { return volunteersMigrated; }
    public int getActivitiesMigrated() { return activitiesMigrated; }
}