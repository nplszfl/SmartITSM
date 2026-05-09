"""
Root Cause Analysis Service - Identify underlying causes of IT issues.
"""

import logging
from typing import Optional

from llm_service import llm_service

logger = logging.getLogger(__name__)


class RootCauseAnalysisService:
    """Service for analyzing tickets to identify root causes."""

    SYSTEM_PROMPT = """You are an expert IT problem analyst specializing in root cause analysis (RCA).
Analyze incident descriptions to identify underlying causes, not just symptoms.

Techniques to apply:
- 5 Whys analysis
- Fishbone (Ishikawa) diagram categories
- Common failure patterns in IT infrastructure

Output structured findings with confidence levels."""

    CATEGORIES = [
        "infrastructure", "configuration", "human_error", "software_bug",
        "capacity", "third_party", "security", "process", "unknown"
    ]

    async def analyze_root_cause(
        self,
        ticket_id: str,
        title: str,
        description: str,
        related_tickets: Optional[list[dict]] = None,
        asset_data: Optional[dict] = None,
        historical_resolutions: Optional[list[dict]] = None,
    ) -> dict:
        """
        Perform root cause analysis on a ticket.
        """
        if not description or len(description.strip()) < 20:
            return self._empty_analysis(ticket_id, "Description too short for analysis")

        try:
            prompt = self._build_rca_prompt(
                ticket_id=ticket_id,
                title=title,
                description=description,
                related_tickets=related_tickets,
                asset_data=asset_data,
                historical_resolutions=historical_resolutions,
            )

            result = await llm_service.complete_json(prompt, self.SYSTEM_PROMPT)

            response = {
                "ticket_id": ticket_id,
                "primary_root_cause": result.get("primary_cause", "unknown"),
                "root_cause_category": result.get("cause_category", "unknown"),
                "confidence_score": result.get("confidence", 0.7),
                "contributing_factors": result.get("contributing_factors", []),
                "affected_components": result.get("affected_components", []),
                "impact_scope": result.get("impact_scope", "single_user"),
                "recurrence_risk": result.get("recurrence_risk", "medium"),
                "similar_past_incidents": result.get("similar_incidents", []),
                "fix_recommendations": result.get("recommendations", []),
                "prevention_recommendations": result.get("prevention", []),
                "analysis_method": result.get("method", "llm_analysis"),
                "why_chain": result.get("why_chain", []),
            }

            return response

        except Exception as e:
            logger.error(f"Root cause analysis failed for {ticket_id}: {e}")
            return self._fallback_analysis(ticket_id, title, description)

    def _build_rca_prompt(
        self,
        ticket_id: str,
        title: str,
        description: str,
        related_tickets: Optional[list[dict]] = None,
        asset_data: Optional[dict] = None,
        historical_resolutions: Optional[list[dict]] = None,
    ) -> str:
        related_section = ""
        if related_tickets:
            tickets_str = "\n".join([
                f"- {t.get('id', 'unknown')}: {t.get('title', '')} (resolved: {t.get('resolution', 'n/a')})"
                for t in related_tickets[:5]
            ])
            related_section = f"\nRelated past tickets:\n{tickets_str}\n"

        asset_section = ""
        if asset_data:
            asset_section = f"\nAffected asset(s):\n- Name: {asset_data.get('name', 'unknown')}\n"
            asset_section += f"- Type: {asset_data.get('type', 'unknown')}\n"
            asset_section += f"- Age: {asset_data.get('age_days', 'unknown')} days\n"
            asset_section += f"- Last maintenance: {asset_data.get('last_maintenance', 'unknown')}\n"

        historical_section = ""
        if historical_resolutions:
            historical_section = f"\nHistorical resolution patterns:\n"
            for res in historical_resolutions[:3]:
                historical_section += f"- Cause: {res.get('cause', 'unknown')} -> Resolution: {res.get('resolution', 'unknown')}\n"

        return f"""Perform root cause analysis on this IT incident:

TICKET ID: {ticket_id}
TITLE: {title}
DESCRIPTION: {description}
{related_section}{asset_section}{historical_section}

Provide RCA findings in JSON format:
{{
    "primary_cause": "<main root cause description>",
    "cause_category": "<one of: {', '.join(self.CATEGORIES)}>",
    "confidence": <0.0 to 1.0>,
    "contributing_factors": ["<factor 1>", "<factor 2>"],
    "affected_components": ["<component 1>", "<component 2>"],
    "impact_scope": "<single_user/single_department/multiple_departments/enterprise>",
    "recurrence_risk": "<low/medium/high>",
    "similar_incidents": ["<ticket_id1>", "<ticket_id2>"],
    "recommendations": ["<fix action 1>", "<fix action 2>"],
    "prevention": ["<prevention measure 1>", "<prevention measure 2>"],
    "method": "<analysis technique used>",
    "why_chain": ["<why 1>", "<why 2>", "<why 3>", "<why 4>", "<why 5>"]
}}

Apply 5 Whys methodology and identify patterns from similar past incidents."""

    def _fallback_analysis(self, ticket_id: str, title: str, description: str) -> dict:
        """Fallback analysis using pattern matching."""
        desc_lower = (title + " " + description).lower()

        # Pattern-based root cause detection
        if any(kw in desc_lower for kw in ["outage", "down", "unavailable"]):
            cause = "Service availability disruption"
            category = "infrastructure"
            confidence = 0.6
        elif any(kw in desc_lower for kw in ["slow", "performance", "latency", "delay"]):
            cause = "Performance degradation"
            category = "capacity"
            confidence = 0.65
        elif any(kw in desc_lower for kw in ["error", "crash", "bug", "exception"]):
            cause = "Software error condition"
            category = "software_bug"
            confidence = 0.7
        elif any(kw in desc_lower for kw in ["config", "setting", "change"]):
            cause = "Configuration issue"
            category = "configuration"
            confidence = 0.6
        elif any(kw in desc_lower for kw in ["forgot", "accidentally", "mistake"]):
            cause = "Human error during operation"
            category = "human_error"
            confidence = 0.75
        else:
            cause = "Undetermined - requires further investigation"
            category = "unknown"
            confidence = 0.3

        return {
            "ticket_id": ticket_id,
            "primary_root_cause": cause,
            "root_cause_category": category,
            "confidence_score": confidence,
            "contributing_factors": ["Manual analysis required due to LLM failure"],
            "affected_components": [],
            "impact_scope": "unknown",
            "recurrence_risk": "medium",
            "similar_past_incidents": [],
            "fix_recommendations": ["Collect more diagnostic information"],
            "prevention_recommendations": ["Implement better monitoring"],
            "analysis_method": "fallback_keyword_matching",
            "why_chain": [cause],
        }

    def _empty_analysis(self, ticket_id: str, reason: str) -> dict:
        """Return empty analysis when input is insufficient."""
        return {
            "ticket_id": ticket_id,
            "primary_root_cause": "unknown",
            "root_cause_category": "unknown",
            "confidence_score": 0.0,
            "contributing_factors": [],
            "affected_components": [],
            "impact_scope": "unknown",
            "recurrence_risk": "unknown",
            "similar_past_incidents": [],
            "fix_recommendations": [f"Unable to analyze: {reason}"],
            "prevention_recommendations": [],
            "analysis_method": "none",
            "why_chain": [],
        }