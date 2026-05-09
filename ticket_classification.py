"""
Ticket Classification Service - Categorize and prioritize support tickets using LLM.
"""

import logging
from typing import Optional

from llm_service import llm_service

logger = logging.getLogger(__name__)


class TicketClassificationService:
    """Service for classifying and prioritizing IT support tickets."""

    SYSTEM_PROMPT = """You are an expert IT Service Management (ITSM) analyst. Classify support tickets accurately 
by analyzing their content. Assign categories, priority levels, and identify key entities.

Categories: hardware, software, network, security, account_access, performance, other
Priorities: critical, high, medium, low
Urgency: critical, high, medium, low
Impact: high, medium, low"""

    CATEGORIES = [
        "hardware", "software", "network", "security", 
        "account_access", "performance", "documentation", "other"
    ]

    PRIORITIES = ["critical", "high", "medium", "low"]

    async def classify_ticket(
        self,
        ticket_id: str,
        title: str,
        description: str,
        category_hint: Optional[str] = None,
        historical_data: Optional[dict] = None,
    ) -> dict:
        """
        Classify a support ticket with category, priority, and urgency.
        """
        if not description or len(description.strip()) < 10:
            return self._empty_classification(ticket_id, "Description too short for classification")

        try:
            prompt = self._build_classification_prompt(
                ticket_id=ticket_id,
                title=title,
                description=description,
                category_hint=category_hint,
                historical_data=historical_data,
            )

            result = await llm_service.complete_json(prompt, self.SYSTEM_PROMPT)

            response = {
                "ticket_id": ticket_id,
                "category": result.get("category", "other"),
                "priority": result.get("priority", "medium"),
                "urgency": result.get("urgency", "medium"),
                "impact": result.get("impact", "medium"),
                "confidence_score": result.get("confidence", 0.8),
                "assigned_team": result.get("assigned_team", self._default_team(result.get("category", "other"))),
                "sl breached_hours": self._estimate_sla_hours(result.get("priority", "medium")),
                "similar_ticket_ids": result.get("similar_tickets", []),
                "reasoning": result.get("reasoning", "Classification based on ticket content analysis"),
            }

            return response

        except Exception as e:
            logger.error(f"Ticket classification failed for {ticket_id}: {e}")
            return self._fallback_classification(ticket_id, title, description)

    def _build_classification_prompt(
        self,
        ticket_id: str,
        title: str,
        description: str,
        category_hint: Optional[str] = None,
        historical_data: Optional[dict] = None,
    ) -> str:
        hint_section = f"Category hint from submitter: {category_hint}\n" if category_hint else ""
        
        history_section = ""
        if historical_data:
            history_section = f"\nHistorical context:\n- Similar tickets: {historical_data.get('similar_tickets_count', 0)}\n"
            history_section += f"- Average resolution time: {historical_data.get('avg_resolution_hours', 'unknown')} hours\n"

        return f"""Classify this IT support ticket:

TICKET ID: {ticket_id}
TITLE: {title}
DESCRIPTION: {description}
{hint_section}{history_section}

Provide classification in JSON format:
{{
    "category": "<one of: {', '.join(self.CATEGORIES)}>",
    "priority": "<one of: {', '.join(self.PRIORITIES)}>",
    "urgency": "<one of: critical, high, medium, low>",
    "impact": "<one of: high, medium, low>",
    "confidence": <0.0 to 1.0>,
    "assigned_team": "<team name>",
    "similar_tickets": ["<ticket_id1>", "<ticket_id2>"],
    "reasoning": "<brief explanation>"
}}

Consider:
- Security issues = critical priority
- Multiple users affected = high urgency
- Clear reproduction steps = higher confidence
- Historical patterns indicate expected category"""

    def _default_team(self, category: str) -> str:
        team_mapping = {
            "hardware": "infrastructure_team",
            "software": "application_team",
            "network": "network_team",
            "security": "security_team",
            "account_access": "identity_team",
            "performance": "performance_team",
            "documentation": "docs_team",
            "other": "support_team",
        }
        return team_mapping.get(category, "support_team")

    def _estimate_sla_hours(self, priority: str) -> int:
        sla_mapping = {
            "critical": 1,
            "high": 4,
            "medium": 24,
            "low": 72,
        }
        return sla_mapping.get(priority, 24)

    def _fallback_classification(self, ticket_id: str, title: str, description: str) -> dict:
        """Fallback classification using keyword analysis."""
        desc_lower = (title + " " + description).lower()
        
        # Auto-detect security issues
        if any(kw in desc_lower for kw in ["hack", "breach", "malware", "phishing", "unauthorized"]):
            category = "security"
            priority = "critical"
        # Auto-detect hardware
        elif any(kw in desc_lower for kw in ["laptop", "monitor", "keyboard", "mouse", "printer", "hardware"]):
            category = "hardware"
            priority = "medium"
        # Auto-detect network
        elif any(kw in desc_lower for kw in ["network", "wifi", "connectivity", "vpn", "connection"]):
            category = "network"
            priority = "medium"
        # Auto-detect account issues
        elif any(kw in desc_lower for kw in ["password", "login", "access", "account", "locked"]):
            category = "account_access"
            priority = "high"
        else:
            category = "software"
            priority = "medium"

        return {
            "ticket_id": ticket_id,
            "category": category,
            "priority": priority,
            "urgency": priority,  # urgency often mirrors priority
            "impact": "medium",
            "confidence_score": 0.5,
            "assigned_team": self._default_team(category),
            "sl breached_hours": self._estimate_sla_hours(priority),
            "similar_ticket_ids": [],
            "reasoning": "Fallback classification based on keyword analysis",
        }

    def _empty_classification(self, ticket_id: str, reason: str) -> dict:
        """Return empty classification when input is insufficient."""
        return {
            "ticket_id": ticket_id,
            "category": "other",
            "priority": "medium",
            "urgency": "medium",
            "impact": "medium",
            "confidence_score": 0.0,
            "assigned_team": "support_team",
            "sl breached_hours": 24,
            "similar_ticket_ids": [],
            "reasoning": f"Unable to classify: {reason}",
        }