package com.devcollab.util;

public class ColorUtil {

    public static String getStatusColor(String status) {
        switch (status.toUpperCase()) {
            case "DONE":
            case "COMPLETED":
                return "#008080";
            case "IN_PROGRESS":
                return "#4169e1";
            case "ON_HOLD":
                return "#ff8c00";
            case "TODO":
            case "PENDING":
                return "#dc143c";
            default:
                return "#6b7280";
        }
    }
}