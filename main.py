"""
SmartITSM AI Worker Service
Core AI processing service for ticket classification, root cause analysis,
predictive maintenance, and automated resolution.
"""

import os
import logging
from contextlib import asynccontextmanager
from typing import Optional

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from redis_client import redis_client
from llm_service import llm_service
from ticket_classification import TicketClassificationService
from root_cause_analysis import RootCauseAnalysisService
from predictive_maintenance import PredictiveMaintenanceService, AnomalyDetectionService
from automated_resolution import AutomatedResolutionService, KnowledgeBaseService

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# ============================================================================
# Pydantic Models - Ticket Classification
# ============================================================================

class TicketClassifyRequest(BaseModel):
    ticket_id: str
    title: str
    description: str
    category_hint: Optional[str] = None
    historical_data: Optional[dict] = None


class TicketClassifyResponse(BaseModel):
    ticket_id: str
    category: str
    priority: str
    urgency: str
    impact: str
    confidence_score: float
    assigned_team: str
    sla_breached_hours: int
    similar_ticket_ids: list[str]
    reasoning: str


# ============================================================================
# Pydantic Models - Root Cause Analysis
# ============================================================================

class RootCauseAnalyzeRequest(BaseModel):
    ticket_id: str
    title: str
    description: str
    related_tickets: Optional[list[dict]] = None
    asset_data: Optional[dict] = None
    historical_resolutions: Optional[list[dict]] = None


class RootCauseAnalyzeResponse(BaseModel):
    ticket_id: str
    primary_root_cause: str
    root_cause_category: str
    confidence_score: float
    contributing_factors: list[str]
    affected_components: list[str]
    impact_scope: str
    recurrence_risk: str
    similar_past_incidents: list[str]
    fix_recommendations: list[str]
    prevention_recommendations: list[str]
    analysis_method: str
    why_chain: list[str]


# ============================================================================
# Pydantic Models - Predictive Maintenance
# ============================================================================

class PredictFailureRequest(BaseModel):
    asset_id: str
    asset_name: str
    asset_type: str
    age_days: int
    telemetry: Optional[dict] = None
    historical_failures: Optional[list[dict]] = None
    maintenance_history: Optional[list[dict]] = None
    incident_tickets: Optional[list[dict]] = None


class PredictFailureResponse(BaseModel):
    asset_id: str
    asset_name: str
    failure_probability: float
    risk_level: str
    predicted_failure_window: str
    failure_type: str
    confidence_score: float
    contributing_factors: list[str]
    warning_signs_detected: list[str]
    recommended_actions: list[str]
    maintenance_recommendations: list[str]
    estimated_downtime_impact_hours: int
    replacement_cost_estimate: str
    alternative_actions: list[str]


class DetectAnomalyRequest(BaseModel):
    metric_name: str
    current_value: float
    historical_values: list[float]
    threshold_sigma: float = 2.0


class DetectAnomalyResponse(BaseModel):
    metric_name: str
    current_value: float
    mean: float
    std_dev: float
    z_score: float
    is_anomaly: bool
    anomaly_severity: str
    trend: str
    confidence: float
    recommended_action: str


# ============================================================================
# Pydantic Models - Automated Resolution
# ============================================================================

class SuggestResolutionRequest(BaseModel):
    ticket_id: str
    title: str
    description: str
    category: Optional[str] = None
    related_tickets: Optional[list[dict]] = None
    known_solutions: Optional[list[dict]] = None
    customer_satisfaction_history: Optional[float] = None


class SuggestResolutionResponse(BaseModel):
    ticket_id: str
    resolution_available: bool
    auto_resolve_eligible: bool
    resolution_category: str
    resolution_steps: list[str]
    estimated_time_minutes: int
    confidence_score: float
    tools_or_scripts: list[str]
    verification_steps: list[str]
    known_solution_match: bool
    similar_ticket_resolutions: list[str]
    customer_satisfaction_prediction: str
    fallback_action: str
    knowledge_base_article_ids: list[str]
    resolution_summary: str


# ============================================================================
# Lifespan
# ============================================================================

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting SmartITSM AI Worker Service")
    await redis_client.connect()
    yield
    logger.info("Shutting down SmartITSM AI Worker Service")
    await redis_client.disconnect()
    await llm_service.close()


# ============================================================================
# App
# ============================================================================

app = FastAPI(
    title="SmartITSM AI Worker",
    description="AI processing service for IT Service Management - classification, RCA, predictive maintenance, resolution",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize services
classification_service = TicketClassificationService()
rca_service = RootCauseAnalysisService()
predictive_service = PredictiveMaintenanceService()
anomaly_service = AnomalyDetectionService()
resolution_service = AutomatedResolutionService()
kb_service = KnowledgeBaseService()


# ============================================================================
# Health Check
# ============================================================================

@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "smartitsm-ai-worker"}


# ============================================================================
# AI Endpoints - Ticket Classification
# ============================================================================

@app.post("/ai/ticket/classify", response_model=TicketClassifyResponse)
async def classify_ticket(request: TicketClassifyRequest):
    """Classify a support ticket by category, priority, and urgency."""
    try:
        result = await classification_service.classify_ticket(
            ticket_id=request.ticket_id,
            title=request.title,
            description=request.description,
            category_hint=request.category_hint,
            historical_data=request.historical_data,
        )
        return result
    except Exception as e:
        logger.error(f"Ticket classification failed for {request.ticket_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ============================================================================
# AI Endpoints - Root Cause Analysis
# ============================================================================

@app.post("/ai/root-cause/analyze", response_model=RootCauseAnalyzeResponse)
async def analyze_root_cause(request: RootCauseAnalyzeRequest):
    """Perform root cause analysis on a ticket to identify underlying issues."""
    try:
        result = await rca_service.analyze_root_cause(
            ticket_id=request.ticket_id,
            title=request.title,
            description=request.description,
            related_tickets=request.related_tickets,
            asset_data=request.asset_data,
            historical_resolutions=request.historical_resolutions,
        )
        return result
    except Exception as e:
        logger.error(f"Root cause analysis failed for {request.ticket_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ============================================================================
# AI Endpoints - Predictive Maintenance
# ============================================================================

@app.post("/ai/maintenance/predict-failure", response_model=PredictFailureResponse)
async def predict_failure(request: PredictFailureRequest):
    """Predict equipment failure and recommend maintenance actions."""
    try:
        result = await predictive_service.predict_failure(
            asset_id=request.asset_id,
            asset_name=request.asset_name,
            asset_type=request.asset_type,
            age_days=request.age_days,
            telemetry=request.telemetry,
            historical_failures=request.historical_failures,
            maintenance_history=request.maintenance_history,
            incident_tickets=request.incident_tickets,
        )
        return result
    except Exception as e:
        logger.error(f"Predictive maintenance failed for {request.asset_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/ai/maintenance/detect-anomaly", response_model=DetectAnomalyResponse)
async def detect_anomaly(request: DetectAnomalyRequest):
    """Detect anomalies in system metrics using statistical analysis."""
    try:
        result = await anomaly_service.detect_anomalies(
            metric_name=request.metric_name,
            current_value=request.current_value,
            historical_values=request.historical_values,
            threshold_sigma=request.threshold_sigma,
        )
        return result
    except Exception as e:
        logger.error(f"Anomaly detection failed for {request.metric_name}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ============================================================================
# AI Endpoints - Automated Resolution
# ============================================================================

@app.post("/ai/resolution/suggest", response_model=SuggestResolutionResponse)
async def suggest_resolution(request: SuggestResolutionRequest):
    """Suggest or auto-resolve tickets using knowledge base and LLM."""
    try:
        result = await resolution_service.suggest_resolution(
            ticket_id=request.ticket_id,
            title=request.title,
            description=request.description,
            category=request.category,
            related_tickets=request.related_tickets,
            known_solutions=request.known_solutions,
            customer_satisfaction_history=request.customer_satisfaction_history,
        )
        return result
    except Exception as e:
        logger.error(f"Resolution suggestion failed for {request.ticket_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/ai/knowledge-base/search")
async def search_knowledge_base(query: str, category: Optional[str] = None, limit: int = 3):
    """Search knowledge base for relevant articles."""
    try:
        results = await kb_service.search_similar_articles(
            query=query,
            category=category,
            limit=limit,
        )
        return {"query": query, "results": results}
    except Exception as e:
        logger.error(f"Knowledge base search failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ============================================================================
# Batch Processing Endpoints
# ============================================================================

class BatchClassifyRequest(BaseModel):
    tickets: list[TicketClassifyRequest]


class BatchResolutionRequest(BaseModel):
    tickets: list[SuggestResolutionRequest]


@app.post("/ai/ticket/classify/batch")
async def classify_tickets_batch(request: BatchClassifyRequest):
    """Classify multiple tickets in batch."""
    results = []
    for ticket in request.tickets:
        try:
            result = await classification_service.classify_ticket(
                ticket_id=ticket.ticket_id,
                title=ticket.title,
                description=ticket.description,
                category_hint=ticket.category_hint,
                historical_data=ticket.historical_data,
            )
            results.append(result)
        except Exception as e:
            logger.error(f"Batch classification failed for {ticket.ticket_id}: {e}")
            results.append({
                "ticket_id": ticket.ticket_id,
                "error": str(e),
                "category": "unknown",
                "priority": "medium",
                "urgency": "medium",
                "impact": "medium",
                "confidence_score": 0.0,
                "assigned_team": "support_team",
                "sla_breached_hours": 24,
                "similar_ticket_ids": [],
                "reasoning": f"Failed: {str(e)}",
            })
    return {"total": len(request.tickets), "results": results}


@app.post("/ai/resolution/suggest/batch")
async def suggest_resolutions_batch(request: BatchResolutionRequest):
    """Suggest resolutions for multiple tickets in batch."""
    results = []
    for ticket in request.tickets:
        try:
            result = await resolution_service.suggest_resolution(
                ticket_id=ticket.ticket_id,
                title=ticket.title,
                description=ticket.description,
                category=ticket.category,
                related_tickets=ticket.related_tickets,
                known_solutions=ticket.known_solutions,
                customer_satisfaction_history=ticket.customer_satisfaction_history,
            )
            results.append(result)
        except Exception as e:
            logger.error(f"Batch resolution failed for {ticket.ticket_id}: {e}")
            results.append({
                "ticket_id": ticket.ticket_id,
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
                "fallback_action": "escalate_to_human",
                "knowledge_base_article_ids": [],
                "resolution_summary": f"Failed: {str(e)}",
            })
    return {"total": len(request.tickets), "results": results}


# ============================================================================
# Analytics / Summary Endpoints
# ============================================================================

@app.get("/ai/summary/itsm")
async def get_ai_summary():
    """Get summary of AI capabilities and configuration."""
    return {
        "service": "SmartITSM AI Worker",
        "version": "1.0.0",
        "capabilities": {
            "ticket_classification": {
                "enabled": True,
                "categories": ["hardware", "software", "network", "security", "account_access", "performance", "documentation", "other"],
                "priorities": ["critical", "high", "medium", "low"],
            },
            "root_cause_analysis": {
                "enabled": True,
                "methods": ["5_whys", "fishbone", "pattern_matching", "llm_analysis"],
                "categories": ["infrastructure", "configuration", "human_error", "software_bug", "capacity", "third_party", "security", "process", "unknown"],
            },
            "predictive_maintenance": {
                "enabled": True,
                "asset_types": ["server", "laptop", "network_switch", "storage", "printer"],
                "anomaly_detection": True,
            },
            "automated_resolution": {
                "enabled": True,
                "auto_resolve_threshold": 0.8,
                "knowledge_base_articles": 3,
            },
        },
        "llm_provider": os.getenv("LLM_LLM_PROVIDER", "deepseek"),
        "redis_enabled": True,
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)