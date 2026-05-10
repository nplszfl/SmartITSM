"""
Unit tests for AutomatedResolutionService and KnowledgeBaseService.

Tests cover:
- Password reset resolution pattern
- Service restart resolution pattern
- Cache clear resolution pattern
- Knowledge base article search
- CSAT prediction
- Edge cases
"""

import pytest
from automated_resolution import AutomatedResolutionService, KnowledgeBaseService


class TestAutomatedResolutionServiceFallback:
    """Tests for AutomatedResolutionService fallback resolution patterns."""

    def setup_method(self):
        """Set up test fixtures."""
        self.service = AutomatedResolutionService()

    def test_fallback_password_reset(self):
        """Password-related tickets get password reset resolution."""
        result = self.service._fallback_resolution(
            ticket_id="ticket-001",
            title="Cannot login to system",
            description="I forgot my password and need to reset it",
            category=None,
        )
        
        assert result["resolution_available"] is True
        assert result["auto_resolve_eligible"] is True
        assert result["resolution_category"] == "password_reset"
        assert len(result["resolution_steps"]) > 0
        assert result["confidence_score"] == 0.85

    def test_fallback_password_lockout(self):
        """Account lockout triggers password reset resolution."""
        result = self.service._fallback_resolution(
            ticket_id="ticket-002",
            title="Account locked out",
            description="My account is locked after too many login attempts",
            category=None,
        )
        
        assert result["resolution_category"] == "password_reset"
        assert result["auto_resolve_eligible"] is True

    def test_fallback_service_restart(self):
        """Frozen/not responding tickets get restart resolution."""
        result = self.service._fallback_resolution(
            ticket_id="ticket-003",
            title="Application not responding",
            description="The application is frozen and will not respond to clicks",
            category=None,
        )
        
        assert result["resolution_available"] is True
        assert result["resolution_category"] == "restart_service"
        assert result["auto_resolve_eligible"] is False  # Manual intervention needed
        assert len(result["resolution_steps"]) > 0

    def test_fallback_cache_clear(self):
        """Performance tickets get cache clear resolution."""
        result = self.service._fallback_resolution(
            ticket_id="ticket-004",
            title="Browser is very slow",
            description="The browser is loading pages very slowly, need to clear cache",
            category=None,
        )
        
        assert result["resolution_available"] is True
        assert result["resolution_category"] == "cache_clear"
        assert result["auto_resolve_eligible"] is True
        assert result["estimated_time_minutes"] == 5

    def test_fallback_cache_browser_keywords(self):
        """Cache-related keywords trigger cache clear resolution."""
        keywords = ["cache", "slow", "browser"]
        for kw in keywords:
            result = self.service._fallback_resolution(
                ticket_id=f"ticket-{kw}",
                title=f"Issue with {kw}",
                description=f"User reported {kw} problem",
                category=None,
            )
            if kw in ["slow", "cache", "browser"]:
                assert result["resolution_category"] == "cache_clear", f"Failed for keyword: {kw}"

    def test_fallback_default_resolution(self):
        """Unknown issues get default fallback resolution."""
        result = self.service._fallback_resolution(
            ticket_id="ticket-005",
            title="Strange error message",
            description="An error appeared that I have never seen before",
            category=None,
        )
        
        assert result["resolution_available"] is True
        assert result["resolution_category"] == "other"
        assert result["auto_resolve_eligible"] is False
        assert result["confidence_score"] < 0.5
        assert result["fallback_action"] == "escalate_to_human"

    def test_fallback_empty_resolution(self):
        """Short descriptions return empty resolution."""
        result = self.service._empty_resolution(
            ticket_id="ticket-006",
            reason="Description too short",
        )
        
        assert result["resolution_available"] is False
        assert result["auto_resolve_eligible"] is False
        assert result["confidence_score"] == 0.0
        assert "Unable to generate resolution" in result["resolution_summary"]

    def test_predict_csat_high_confidence(self):
        """High confidence yields satisfied prediction."""
        result = self.service._predict_csat(confidence=0.9, history=None)
        assert result == "very_satisfied"

    def test_predict_csat_medium_confidence(self):
        """Medium confidence yields neutral prediction."""
        result = self.service._predict_csat(confidence=0.5, history=None)
        assert result == "neutral"

    def test_predict_csat_low_confidence(self):
        """Low confidence yields dissatisfied prediction."""
        result = self.service._predict_csat(confidence=0.2, history=None)
        assert result == "very_dissatisfied"

    def test_predict_csat_with_history(self):
        """History adjusts CSAT prediction."""
        # High confidence but poor history
        result = self.service._predict_csat(confidence=0.9, history=2.0)
        # (0.9*5 + 2.0) / 2 = 3.25 -> neutral
        assert result == "neutral"

    def test_predict_csat_boundary_very_satisfied(self):
        """Score >= 4.5 is very satisfied."""
        assert self.service._predict_csat(0.95, None) == "very_satisfied"
        assert self.service._predict_csat(0.90, None) == "very_satisfied"

    def test_predict_csat_boundary_satisfied(self):
        """Score >= 3.5 and < 4.5 is satisfied."""
        assert self.service._predict_csat(0.75, None) == "satisfied"
        assert self.service._predict_csat(0.70, None) == "satisfied"

    def test_predict_csat_boundary_neutral(self):
        """Score >= 2.5 and < 3.5 is neutral."""
        assert self.service._predict_csat(0.55, None) == "neutral"
        assert self.service._predict_csat(0.50, None) == "neutral"

    def test_predict_csat_boundary_dissatisfied(self):
        """Score >= 1.5 and < 2.5 is dissatisfied."""
        assert self.service._predict_csat(0.35, None) == "dissatisfied"
        assert self.service._predict_csat(0.30, None) == "dissatisfied"

    def test_fallback_resolution_complete_response(self):
        """Fallback resolution returns all required fields."""
        result = self.service._fallback_resolution(
            ticket_id="ticket-007",
            title="Test ticket",
            description="Test description for password reset issue",
            category=None,
        )
        
        required_fields = [
            "ticket_id", "resolution_available", "auto_resolve_eligible",
            "resolution_category", "resolution_steps", "estimated_time_minutes",
            "confidence_score", "tools_or_scripts", "verification_steps",
            "known_solution_match", "similar_ticket_resolutions",
            "customer_satisfaction_prediction", "fallback_action",
            "knowledge_base_article_ids", "resolution_summary"
        ]
        
        for field in required_fields:
            assert field in result, f"Missing field: {field}"

    def test_fallback_resolution_tools_are_strings(self):
        """Tools/scripts field contains string values."""
        result = self.service._fallback_resolution(
            ticket_id="ticket-008",
            title="Password issue",
            description="Need to reset password",
            category=None,
        )
        
        for tool in result["tools_or_scripts"]:
            assert isinstance(tool, str)

    def test_fallback_resolution_steps_are_strings(self):
        """Resolution steps field contains string values."""
        result = self.service._fallback_resolution(
            ticket_id="ticket-009",
            title="Application frozen",
            description="App not responding",
            category=None,
        )
        
        for step in result["resolution_steps"]:
            assert isinstance(step, str)


class TestKnowledgeBaseService:
    """Tests for KnowledgeBaseService article search functionality."""

    def setup_method(self):
        """Set up test fixtures."""
        self.service = KnowledgeBaseService()

    @pytest.mark.asyncio
    async def test_search_password_articles(self):
        """Search for password returns relevant articles."""
        results = await self.service.search_similar_articles(query="password reset")
        
        assert len(results) > 0
        assert any("password" in r["title"].lower() for r in results)

    @pytest.mark.asyncio
    async def test_search_cache_articles(self):
        """Search for cache returns KB-002."""
        results = await self.service.search_similar_articles(query="clear cache browser")
        
        assert len(results) > 0
        assert any("KB-002" in r["article_id"] for r in results)

    @pytest.mark.asyncio
    async def test_search_vpn_articles(self):
        """Search for VPN returns KB-003."""
        results = await self.service.search_similar_articles(query="vpn connection issues")
        
        assert len(results) > 0
        assert any("KB-003" in r["article_id"] for r in results)

    @pytest.mark.asyncio
    async def test_search_with_category_filter(self):
        """Category filter limits results to matching category."""
        results = await self.service.search_similar_articles(
            query="reset",
            category="account_access",
        )
        
        for r in results:
            assert r["category"] == "account_access"

    @pytest.mark.asyncio
    async def test_search_relevance_score(self):
        """Results are sorted by relevance score descending."""
        results = await self.service.search_similar_articles(query="password reset login")
        
        if len(results) >= 2:
            assert results[0]["relevance_score"] >= results[1]["relevance_score"]

    @pytest.mark.asyncio
    async def test_search_respects_limit(self):
        """Results are limited to specified count."""
        results = await self.service.search_similar_articles(query="reset", limit=1)
        
        assert len(results) <= 1

    @pytest.mark.asyncio
    async def test_search_no_matches(self):
        """No matches returns empty list."""
        results = await self.service.search_similar_articles(query="xyzabc123nonexistent")
        
        assert len(results) == 0

    @pytest.mark.asyncio
    async def test_search_returns_article_structure(self):
        """Results contain all expected fields."""
        results = await self.service.search_similar_articles(query="password")
        
        if len(results) > 0:
            result = results[0]
            assert "article_id" in result
            assert "title" in result
            assert "category" in result
            assert "relevance_score" in result
            assert "success_rate" in result
            assert "steps" in result

    @pytest.mark.asyncio
    async def test_search_steps_preview_limited(self):
        """Steps preview is limited to first 3 steps."""
        results = await self.service.search_similar_articles(query="password")
        
        if len(results) > 0:
            assert len(results[0]["steps"]) <= 3

    @pytest.mark.asyncio
    async def test_search_success_rate_range(self):
        """Success rates are between 0 and 1."""
        results = await self.service.search_similar_articles(query="reset")
        
        for r in results:
            assert 0 <= r["success_rate"] <= 1

    @pytest.mark.asyncio
    async def test_search_relevance_score_range(self):
        """Relevance scores are between 0 and 1."""
        results = await self.service.search_similar_articles(query="cache")
        
        for r in results:
            assert 0 <= r["relevance_score"] <= 1
