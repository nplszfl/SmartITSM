package com.smartitsm.ai.dto;

import jakarta.validation.constraints.NotBlank;

public class ResolutionSuggestRequest {

    @NotBlank(message = "Ticket ID is required")
    private String ticketId;

    @NotBlank(message = "Description is required")
    private String description;

    private String category;
    private String subcategory;
    private String[] previousResolutions;
    private Map<String, Object> context;

    public static class Map {
    }

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

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public String[] getPreviousResolutions() {
        return previousResolutions;
    }

    public void setPreviousResolutions(String[] previousResolutions) {
        this.previousResolutions = previousResolutions;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
}