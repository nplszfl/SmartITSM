package com.smartitsm.ai.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class RootCauseRequest {

    @NotBlank(message = "Ticket ID is required")
    private String ticketId;

    @NotBlank(message = "Description is required")
    private String description;

    private String category;
    private String symptoms;
    private String[] previousAttempts;
    private Map<String, Object> systemContext;

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public String[] getPreviousAttempts() {
        return previousAttempts;
    }

    public void setPreviousAttempts(String[] previousAttempts) {
        this.previousAttempts = previousAttempts;
    }

    public Map<String, Object> getSystemContext() {
        return systemContext;
    }

    public void setSystemContext(Map<String, Object> systemContext) {
        this.systemContext = systemContext;
    }
}