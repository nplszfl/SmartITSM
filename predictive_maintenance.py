"""
Predictive Maintenance Service - Predict asset failures and maintenance needs.
"""

import logging
from typing import Optional
from datetime import datetime, timedelta

from llm_service import llm_service

logger = logging.getLogger(__name__)


class PredictiveMaintenanceService:
    """Service for predicting equipment failures and maintenance needs."""

    SYSTEM_PROMPT = """You are an expert IT infrastructure analyst specializing in predictive maintenance.
Analyze asset telemetry, historical data, and patterns to predict failures before they occur.

Consider:
- Age and lifecycle state of equipment
- Historical failure patterns
- Current performance metrics
- Environmental factors
- Maintenance history

Output actionable predictions with confidence levels and recommended actions."""

    async def predict_failure(
        self,
        asset_id: str,
        asset_name: str,
        asset_type: str,
        age_days: int,
        telemetry: Optional[dict] = None,
        historical_failures: Optional[list[dict]] = None,
        maintenance_history: Optional[list[dict]] = None,
        incident_tickets: Optional[list[dict]] = None,
    ) -> dict:
        """
        Predict likelihood of asset failure and recommended actions.
        """
        try:
            prompt = self._build_prediction_prompt(
                asset_id=asset_id,
                asset_name=asset_name,
                asset_type=asset_type,
                age_days=age_days,
                telemetry=telemetry,
                historical_failures=historical_failures,
                maintenance_history=maintenance_history,
                incident_tickets=incident_tickets,
            )

            result = await llm_service.complete_json(prompt, self.SYSTEM_PROMPT)

            response = {
                "asset_id": asset_id,
                "asset_name": asset_name,
                "failure_probability": result.get("failure_probability", 0.5),
                "risk_level": result.get("risk_level", "medium"),
                "predicted_failure_window": result.get("failure_window", "unknown"),
                "failure_type": result.get("predicted_failure_type", "unknown"),
                "confidence_score": result.get("confidence", 0.7),
                "contributing_factors": result.get("contributing_factors", []),
                "warning_signs_detected": result.get("warning_signs", []),
                "recommended_actions": result.get("recommended_actions", []),
                "maintenance_recommendations": result.get("maintenance", []),
                "estimated_downtime_impact_hours": result.get("downtime_estimate", 4),
                "replacement_cost_estimate": result.get("replacement_cost", "unknown"),
                "alternative_actions": result.get("alternatives", []),
            }

            return response

        except Exception as e:
            logger.error(f"Predictive maintenance analysis failed for {asset_id}: {e}")
            return self._fallback_prediction(asset_id, asset_name, age_days, telemetry)

    def _build_prediction_prompt(
        self,
        asset_id: str,
        asset_name: str,
        asset_type: str,
        age_days: int,
        telemetry: Optional[dict] = None,
        historical_failures: Optional[list[dict]] = None,
        maintenance_history: Optional[list[dict]] = None,
        incident_tickets: Optional[list[dict]] = None,
    ) -> str:
        age_section = f"Asset age: {age_days} days"
        
        # Calculate expected lifecycle based on asset type
        lifecycle_days = {
            "server": 1825,  # 5 years
            "laptop": 1095,  # 3 years
            "network_switch": 2555,  # 7 years
            "storage": 1825,  # 5 years
            "printer": 1460,  # 4 years
        }
        expected_lifecycle = lifecycle_days.get(asset_type.lower(), 1825)
        lifecycle_percentage = min(100, (age_days / expected_lifecycle) * 100)
        age_section += f"\nExpected lifecycle: {expected_lifecycle} days ({lifecycle_percentage:.1f}% of expected life used)"

        telemetry_section = ""
        if telemetry:
            telemetry_section = "\nCurrent telemetry:\n"
            for key, value in telemetry.items():
                telemetry_section += f"- {key}: {value}\n"

        failure_section = ""
        if historical_failures:
            failure_section = "\nHistorical failures:\n"
            for f in historical_failures[:3]:
                failure_section += f"- {f.get('date', 'unknown')}: {f.get('type', 'unknown')}, Days since last failure: {f.get('days_since', 'unknown')}\n"

        maintenance_section = ""
        if maintenance_history:
            maintenance_section = "\nMaintenance history (last 3):\n"
            for m in maintenance_history[:3]:
                maintenance_section += f"- {m.get('date', 'unknown')}: {m.get('type', 'unknown')}, Next due: {m.get('next_due', 'unknown')}\n"

        incident_section = ""
        if incident_tickets:
            incident_section = "\nRecent incident tickets:\n"
            for t in incident_tickets[:5]:
                incident_section += f"- {t.get('id', 'unknown')}: {t.get('title', 'unknown')} (created: {t.get('created', 'unknown')})\n"

        return f"""Predict asset failure risk and recommended maintenance:

ASSET ID: {asset_id}
ASSET NAME: {asset_name}
ASSET TYPE: {asset_type}
{age_section}
{telemetry_section}{failure_section}{maintenance_section}{incident_section}

Provide prediction in JSON format:
{{
    "failure_probability": <0.0 to 1.0>,
    "risk_level": "<low/medium/high/critical>",
    "failure_window": "<days/weeks/months or 'unknown'>",
    "predicted_failure_type": "<type of expected failure>",
    "confidence": <0.0 to 1.0>,
    "contributing_factors": ["<factor 1>", "<factor 2>"],
    "warning_signs": ["<sign 1>", "<sign 2>"],
    "recommended_actions": ["<action 1>", "<action 2>"],
    "maintenance": ["<maint action 1>", "<maint action 2>"],
    "downtime_estimate": <hours>,
    "replacement_cost": "<estimated cost or 'unknown'>",
    "alternatives": ["<alternative 1>", "<alternative 2>"]
}}

Consider equipment age, lifecycle stage, telemetry anomalies, and historical patterns."""

    def _fallback_prediction(
        self,
        asset_id: str,
        asset_name: str,
        age_days: int,
        telemetry: Optional[dict] = None,
    ) -> dict:
        """Fallback prediction based on age and basic telemetry."""
        # Simple age-based risk scoring
        risk_score = min(1.0, age_days / 1825)  # 5 year baseline
        
        if risk_score < 0.5:
            risk_level = "low"
            window = "6+ months"
        elif risk_score < 0.75:
            risk_level = "medium"
            window = "3-6 months"
        elif risk_score < 0.9:
            risk_level = "high"
            window = "1-3 months"
        else:
            risk_level = "critical"
            window = "within 30 days"

        # Check telemetry for anomalies
        warning_signs = []
        if telemetry:
            for metric, value in telemetry.items():
                if isinstance(value, (int, float)):
                    if value > 90:  # High utilization
                        warning_signs.append(f"High {metric}: {value}%")
                    elif value < 10:  # Low performance
                        warning_signs.append(f"Low {metric}: {value}")

        return {
            "asset_id": asset_id,
            "asset_name": asset_name,
            "failure_probability": risk_score,
            "risk_level": risk_level,
            "predicted_failure_window": window,
            "failure_type": "age-related degradation" if risk_score > 0.5 else "unknown",
            "confidence_score": 0.5,
            "contributing_factors": ["Asset age based estimation"],
            "warning_signs_detected": warning_signs,
            "recommended_actions": [
                "Schedule preventive maintenance check",
                "Monitor telemetry closely",
                "Review replacement budget for upcoming quarter",
            ] if risk_level in ["high", "critical"] else [
                "Continue regular monitoring",
                "Schedule routine maintenance",
            ],
            "maintenance_recommendations": ["Complete system health check"],
            "estimated_downtime_impact_hours": 4 if risk_level == "critical" else 2,
            "replacement_cost_estimate": "unknown",
            "alternative_actions": [
                "Increase monitoring frequency",
                "Prepare backup equipment",
            ],
        }


class AnomalyDetectionService:
    """Service for detecting anomalies in system metrics."""

    async def detect_anomalies(
        self,
        metric_name: str,
        current_value: float,
        historical_values: list[float],
        threshold_sigma: float = 2.0,
    ) -> dict:
        """
        Detect anomalies using statistical analysis.
        """
        if not historical_values or len(historical_values) < 5:
            return {
                "metric_name": metric_name,
                "is_anomaly": False,
                "confidence": 0.0,
                "reason": "Insufficient historical data",
            }

        import statistics
        
        mean = statistics.mean(historical_values)
        stdev = statistics.stdev(historical_values) if len(historical_values) > 1 else 0
        
        if stdev == 0:
            z_score = 0 if current_value == mean else 3  # Treat same as anomaly if constant
        else:
            z_score = abs((current_value - mean) / stdev)

        is_anomaly = z_score > threshold_sigma
        
        # Calculate trend
        recent = historical_values[-5:] if len(historical_values) >= 5 else historical_values
        if len(recent) >= 2:
            trend = "increasing" if recent[-1] > recent[0] else "decreasing" if recent[-1] < recent[0] else "stable"
        else:
            trend = "stable"

        return {
            "metric_name": metric_name,
            "current_value": current_value,
            "mean": round(mean, 2),
            "std_dev": round(stdev, 2),
            "z_score": round(z_score, 2),
            "is_anomaly": is_anomaly,
            "anomaly_severity": "high" if z_score > 3 else "medium" if z_score > 2 else "low",
            "trend": trend,
            "confidence": min(0.95, 0.5 + (z_score * 0.1)),
            "recommended_action": "Investigate" if is_anomaly else "Continue monitoring",
        }