package com.devcollab.dto.enums;

public enum ProjectRole {
    PROJECT_MANAGER("Project Manager"),
    TEAM_LEAD("Team Lead"),
    DEVELOPER("Developer"),
    MEMBER("Member");

    private final String displayName;

    ProjectRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
