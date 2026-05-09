package com.smartitsm.ai.dto;

import jakarta.validation.constraints.NotBlank;

public class MaintenancePredictRequest {

    @NotBlank(message = "System ID is required")
    private String systemId;

    private String systemType;
    private MetricData[] recentMetrics;
    private String[] recentIncidents;
    private Map<String, Object> environmentFactors;

    public static class MetricData {
        private String metricName;
        private double value;
        private String unit;
        private String timestamp;

        public String getMetricName() {
            return metricName;
        }

        public void setMetricName(String metricName) {
            this.metricName = metricName;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }

    public static class Map {
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getSystemType() {
        return systemType;
    }

    public void setSystemType(String systemType) {
        this.systemType = systemType;
    }

    public MetricData[] getRecentMetrics() {
        return recentMetrics;
    }

    public void setRecentMetrics(MetricData[] recentMetrics) {
        this.recentMetrics = recentMetrics;
    }

    public String[] getRecentIncidents() {
        return recentIncidents;
    }

    public void setRecentIncidents(String[] recentIncidents) {
        this.recentIncidents = recentIncidents;
    }

    public Map<String, Object> getEnvironmentFactors() {
        return environmentFactors;
    }

    public void setEnvironmentFactors(Map<String, Object> environmentFactors) {
        this.environmentFactors = environmentFactors;
    }
}