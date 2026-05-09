package com.smartitsm.ai.dto;

public class MaintenancePredictResponse {

    private String systemId;
    private FailurePrediction prediction;
    private RiskFactor[] riskFactors;
    private MaintenanceRecommendation[] recommendations;
    private java.time.Instant predictedAt;

    public static class FailurePrediction {
        private boolean failureLikely;
        private double probability;
        private String predictedFailureType;
        private String estimatedTimeToFailure;
        private int confidencePercent;

        public boolean isFailureLikely() {
            return failureLikely;
        }

        public void setFailureLikely(boolean failureLikely) {
            this.failureLikely = failureLikely;
        }

        public double getProbability() {
            return probability;
        }

        public void setProbability(double probability) {
            this.probability = probability;
        }

        public String getPredictedFailureType() {
            return predictedFailureType;
        }

        public void setPredictedFailureType(String predictedFailureType) {
            this.predictedFailureType = predictedFailureType;
        }

        public String getEstimatedTimeToFailure() {
            return estimatedTimeToFailure;
        }

        public void setEstimatedTimeToFailure(String estimatedTimeToFailure) {
            this.estimatedTimeToFailure = estimatedTimeToFailure;
        }

        public int getConfidencePercent() {
            return confidencePercent;
        }

        public void setConfidencePercent(int confidencePercent) {
            this.confidencePercent = confidencePercent;
        }
    }

    public static class RiskFactor {
        private String factor;
        private double contribution;
        private String severity;

        public String getFactor() {
            return factor;
        }

        public void setFactor(String factor) {
            this.factor = factor;
        }

        public double getContribution() {
            return contribution;
        }

        public void setContribution(double contribution) {
            this.contribution = contribution;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }
    }

    public static class MaintenanceRecommendation {
        private String action;
        private String priority;
        private String estimatedDuration;
        private String[] requiredResources;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public String getEstimatedDuration() {
            return estimatedDuration;
        }

        public void setEstimatedDuration(String estimatedDuration) {
            this.estimatedDuration = estimatedDuration;
        }

        public String[] getRequiredResources() {
            return requiredResources;
        }

        public void setRequiredResources(String[] requiredResources) {
            this.requiredResources = requiredResources;
        }
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public FailurePrediction getPrediction() {
        return prediction;
    }

    public void setPrediction(FailurePrediction prediction) {
        this.prediction = prediction;
    }

    public RiskFactor[] getRiskFactors() {
        return riskFactors;
    }

    public void setRiskFactors(RiskFactor[] riskFactors) {
        this.riskFactors = riskFactors;
    }

    public MaintenanceRecommendation[] getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(MaintenanceRecommendation[] recommendations) {
        this.recommendations = recommendations;
    }

    public java.time.Instant getPredictedAt() {
        return predictedAt;
    }

    public void setPredictedAt(java.time.Instant predictedAt) {
        this.predictedAt = predictedAt;
    }
}