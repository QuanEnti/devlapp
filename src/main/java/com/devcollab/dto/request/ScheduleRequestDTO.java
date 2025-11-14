package com.devcollab.dto.request;

import lombok.Data;

@Data
public class ScheduleRequestDTO {
    private String title;
    private String note;
    private String meetingLink;
    private String datetime; // ISO string from frontend
}