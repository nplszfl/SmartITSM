"""
Automated Resolution Service - Suggest or auto-resolve tickets with known solutions.
"""

import logging
from typing import Optional

from llm_service import llm_service

logger = logging.getLogger(__name__)


class AutomatedResolutionService:
    """Service for automated ticket resolution using knowledge base and LLM."""

    SYSTEM_PROMPT = """You are an expert IT support specialist. Given a ticket description and context,
provide the most appropriate resolution. Use known solutions when available, or generate new ones.

Resolution should include:
- Step-by-step instructions
- Estimated time to resolve
- Tools/scripts to use
- Verification steps

Only suggest auto-resolve if confidence is high (>0.8)."""

    RESOLUTION_CATEGORIES = [
        "password_reset", "software_installation", "config_change", 
        "restart_service", "cache_clear", "permission_fix", "network_reset",
        "driver_update", "disk_cleanup", "other"
    ]

    async def suggest_resolution(
        self,
        ticket_id: str,
        title: str,
        description: str,
        category: Optional[str] = None,
        related_tickets: Optional[list[dict]] = None,
        known_solutions: Optional[list[dict]] = None,
        customer_satisfaction_history: Optional[float] = None,
    ) -> dict:
        """
        Suggest resolution for a ticket, optionally auto-resolving if confident.
        """
        if not description or len(description.strip()) < 10:
            return self._empty_resolution(ticket_id, "Description too short for resolution")

        try:
            prompt = self._build_resolution_prompt(
                ticket_id=ticket_id,
                title=title,
                description=description,
                category=category,
                related_tickets=related_tickets,
                known_solutions=known_solutions,
            )

            result = await llm_service.complete_json(prompt, self.SYSTEM_PROMPT)

            confidence = result.get("confidence", 0.7)
            
            response = {
                "ticket_id": ticket_id,
                "resolution_available": True,
                "auto_resolve_eligible": confidence > 0.8,
                "resolution_category": result.get("category", "other"),
                "resolution_steps": result.get("steps", []),
                "estimated_time_minutes": result.get("estimated_time", 15),
                "confidence_score": confidence,
                "tools_or_scripts": result.get("tools", []),
                "verification_steps": result.get("verification", []),
                "known_solution_match": result.get("known_solution_matched", False),
                "similar_ticket_resolutions": result.get("similar_resolutions", []),
                "customer_satisfaction_prediction": self._predict_csat(confidence, customer_satisfaction_history),
                "fallback_action": "escalate_to_human" if confidence < 0.6 else "provide_customer_guidance",
                "knowledge_base_article_ids": result.get("kb_articles", []),
                "resolution_summary": result.get("summary", "Resolution steps provided"),
            }

            return response

        except Exception as e:
            logger.error(f"Resolution suggestion failed for {ticket_id}: {e}")
            return self._fallback_resolution(ticket_id, title, description, category)

    def _build_resolution_prompt(
        self,
        ticket_id: str,
        title: str,
        description: str,
        category: Optional[str] = None,
        related_tickets: Optional[list[dict]] = None,
        known_solutions: Optional[list[dict]] = None,
    ) -> str:
        category_section = f"Detected category: {category}\n" if category else ""
        
        known_section = ""
        if known_solutions:
            known_section = "\nKnown solutions from knowledge base:\n"
            for sol in known_solutions[:3]:
                known_section += f"- Title: {sol.get('title', 'unknown')}\n"
                known_section += f"  Steps: {sol.get('steps', [])}\n"
                known_section += f"  Success rate: {sol.get('success_rate', 'unknown')}\n"

        related_section = ""
        if related_tickets:
            related_section = "\nSimilar resolved tickets:\n"
            for t in related_tickets[:3]:
                related_section += f"- {t.get('id', 'unknown')}: Resolution = {t.get('resolution', 'not available')}\n"

        return f"""Generate resolution for this IT support ticket:

TICKET ID: {ticket_id}
TITLE: {title}
DESCRIPTION: {description}
{category_section}{known_section}{related_section}

Provide resolution in JSON format:
{{
    "category": "<one of: {', '.join(self.RESOLUTION_CATEGORIES)}>",
    "confidence": <0.0 to 1.0>,
    "steps": ["<step 1>", "<step 2>", "<step 3>"],
    "estimated_time": <minutes>,
    "tools": ["<tool_or_script_1>", "<tool_or_script_2>"],
    "verification": ["<verify_step_1>", "<verify_step_2>"],
    "known_solution_matched": <true/false>,
    "similar_resolutions": ["<ticket_id1>", "<ticket_id2>"],
    "kb_articles": ["<article_id1>", "<article_id2>"],
    "summary": "<brief resolution summary>"
}}

Provide actionable, specific steps. Include actual commands/scripts when relevant."""

    def _predict_csat(self, confidence: float, history: Optional[float] = None) -> str:
        """Predict customer satisfaction based on resolution confidence."""
        base_score = confidence * 5  # 0-5 scale
        
        if history is not None:
            base_score = (base_score + history) / 2
        
        if base_score >= 4.5:
            return "very_satisfied"
        elif base_score >= 3.5:
            return "satisfied"
        elif base_score >= 2.5:
            return "neutral"
        elif base_score >= 1.5:
            return "dissatisfied"
        else:
            return "very_dissatisfied"

    def _fallback_resolution(
        self,
        ticket_id: str,
        title: str,
        description: str,
        category: Optional[str] = None,
    ) -> dict:
        """Fallback resolution using category-based patterns."""
        desc_lower = (title + " " + description).lower()
        
        # Password reset pattern
        if any(kw in desc_lower for kw in ["password", "forgot", "reset", "lock"]):
            return {
                "ticket_id": ticket_id,
                "resolution_available": True,
                "auto_resolve_eligible": True,
                "resolution_category": "password_reset",
                "resolution_steps": [
                    "Verify user identity via approved authentication method",
                    "Navigate to password reset portal",
                    "Generate temporary password following security policy",
                    "Send temporary password to verified email/phone",
                    "Prompt user to change password on next login",
                ],
                "estimated_time_minutes": 5,
                "confidence_score": 0.85,
                "tools_or_scripts": ["password_reset_tool.sh"],
                "verification_steps": [
                    "User confirms successful login",
                    "Verify password changed timestamp in logs",
                ],
                "known_solution_match": True,
                "similar_ticket_resolutions": [],
                "customer_satisfaction_prediction": "satisfied",
                "fallback_action": "provide_customer_guidance",
                "knowledge_base_article_ids": ["KB-001"],
                "resolution_summary": "Password reset completed via self-service portal",
            }
        
        # Restart/service pattern
        if any(kw in desc_lower for kw in ["restart", "reboot", "hang", "freeze", "not responding"]):
            return {
                "ticket_id": ticket_id,
                "resolution_available": True,
                "auto_resolve_eligible": False,
                "resolution_category": "restart_service",
                "resolution_steps": [
                    "Ask user to save all open work",
                    "Close any applications gracefully if possible",
                    "Restart the affected service/computer",
                    "Wait for system to fully boot",
                    "Verify service is running and accessible",
                ],
                "estimated_time_minutes": 10,
                "confidence_score": 0.7,
                "tools_or_scripts": ["restart_service.bat"],
                "verification_steps": [
                    "Confirm system/service responds",
                    "Check event logs for errors",
                ],
                "known_solution_match": False,
                "similar_ticket_resolutions": [],
                "customer_satisfaction_prediction": "satisfied",
                "fallback_action": "provide_customer_guidance",
                "knowledge_base_article_ids": [],
                "resolution_summary": "Service restart recommended to clear temporary issues",
            }
        
        # Cache clear pattern
        if any(kw in desc_lower for kw in ["slow", "cache", "clear", "browser"]):
            return {
                "ticket_id": ticket_id,
                "resolution_available": True,
                "auto_resolve_eligible": True,
                "resolution_category": "cache_clear",
                "resolution_steps": [
                    "Clear browser cache and cookies",
                    "Clear application temporary files",
                    "Restart the application or browser",
                    "Verify performance improvement",
                ],
                "estimated_time_minutes": 5,
                "confidence_score": 0.75,
                "tools_or_scripts": ["clear_cache.bat"],
                "verification_steps": [
                    "User confirms improved performance",
                ],
                "known_solution_match": True,
                "similar_ticket_resolutions": [],
                "customer_satisfaction_prediction": "satisfied",
                "fallback_action": "provide_customer_guidance",
                "knowledge_base_article_ids": ["KB-002"],
                "resolution_summary": "Cache cleared to improve performance",
            }

        # Default fallback
        return {
            "ticket_id": ticket_id,
            "resolution_available": True,
            "auto_resolve_eligible": False,
            "resolution_category": "other",
            "resolution_steps": [
                "Gather additional diagnostic information",
                "Consult knowledge base for similar issues",
                "Escalate to tier 2 support if issue persists",
            ],
            "estimated_time_minutes": 30,
            "confidence_score": 0.4,
            "tools_or_scripts": [],
            "verification_steps": [
                "Confirm issue resolved or properly escalated",
            ],
            "known_solution_match": False,
            "similar_ticket_resolutions": [],
            "customer_satisfaction_prediction": "neutral",
            "fallback_action": "escalate_to_human",
            "knowledge_base_article_ids": [],
            "resolution_summary": "General troubleshooting steps provided - manual review recommended",
        }

    def _empty_resolution(self, ticket_id: str, reason: str) -> dict:
        """Return empty resolution when input is insufficient."""
        return {
            "ticket_id": ticket_id,
            "resolution_available": False,
            "auto_resolve_eligible": False,
            "resolution_category": "unknown",
            "resolution_steps": [],
            "estimated_time_minutes": 0,
            "confidence_score": 0.0,
            "tools_or_scripts": [],
            "verification_steps": [],
            "known_solution_match": False,
            "similar_ticket_resolutions": [],
            "customer_satisfaction_prediction": "unknown",
            "fallback_action": "requires_more_information",
            "knowledge_base_article_ids": [],
            "resolution_summary": f"Unable to generate resolution: {reason}",
        }


class KnowledgeBaseService:
    """Service for managing and searching knowledge base articles."""

    # Simulated knowledge base - in production, this would be a proper database
    KNOWLEDGE_BASE = {
        "KB-001": {
            "title": "Password Reset Procedure",
            "category": "account_access",
            "steps": [
                "Verify user identity via approved authentication method",
                "Navigate to password reset portal",
                "Generate temporary password following security policy",
                "Send temporary password to verified email/phone",
                "Prompt user to change password on next login",
            ],
            "success_rate": 0.95,
            "tags": ["password", "reset", "account", "lockout"],
        },
        "KB-002": {
            "title": "Clear Browser Cache",
            "category": "performance",
            "steps": [
                "Open browser settings",
                "Navigate to privacy and security",
                "Select clear browsing data",
                "Choose time range and cache options",
                "Clear data and restart browser",
            ],
            "success_rate": 0.88,
            "tags": ["cache", "browser", "slow", "performance"],
        },
        "KB-003": {
            "title": "VPN Connection Troubleshooting",
            "category": "network",
            "steps": [
                "Check internet connectivity",
                "Verify VPN client installation",
                "Check VPN credentials",
                "Try reconnecting with admin privileges",
                "Review firewall settings",
            ],
            "success_rate": 0.82,
            "tags": ["vpn", "network", "connection", "remote"],
        },
    }

    async def search_similar_articles(
        self,
        query: str,
        category: Optional[str] = None,
        limit: int = 3,
    ) -> list[dict]:
        """
        Search knowledge base for relevant articles.
        """
        query_lower = query.lower()
        results = []
        
        for article_id, article in self.KNOWLEDGE_BASE.items():
            # Check category filter
            if category and article["category"] != category:
                continue
            
            # Simple relevance scoring
            score = 0
            for tag in article["tags"]:
                if tag in query_lower:
                    score += 1
            
            if score > 0:
                results.append({
                    "article_id": article_id,
                    "title": article["title"],
                    "category": article["category"],
                    "relevance_score": score / len(article["tags"]),
                    "success_rate": article["success_rate"],
                    "steps": article["steps"][:3],  # First 3 steps as preview
                })
        
        # Sort by relevance
        results.sort(key=lambda x: x["relevance_score"], reverse=True)
        return results[:limit]