# SmartITSM AI Worker

AI processing service for IT Service Management with FastAPI endpoints for:
- **Ticket Classification** - Categorize and prioritize support tickets
- **Root Cause Analysis** - Identify underlying causes of IT issues  
- **Predictive Maintenance** - Predict equipment failures before they occur
- **Automated Resolution** - Suggest or auto-resolve tickets with known solutions

## Architecture

```
smartitsm/
├── main.py                      # FastAPI application entry point
├── config.py                    # Configuration management
├── config.yaml                  # Configuration file
├── requirements.txt             # Python dependencies
├── Dockerfile                   # Docker image definition
├── docker-compose.yml           # Docker Compose setup
├── llm_service.py               # LLM integration (DeepSeek/GPT)
├── redis_client.py              # Redis caching client
├── ticket_classification.py      # Ticket classification service
├── root_cause_analysis.py        # Root cause analysis service
├── predictive_maintenance.py     # Predictive maintenance & anomaly detection
├── automated_resolution.py       # Automated resolution & knowledge base
└── [existing Java services]      # Other SmartITSM microservices
```

## API Endpoints

### Health Check
```
GET /health
```

### Ticket Classification
```
POST /ai/ticket/classify
{
  "ticket_id": "TKT-12345",
  "title": "Laptop won't connect to WiFi",
  "description": "User reports laptop cannot connect...",
  "category_hint": "network",
  "historical_data": {...}
}

POST /ai/ticket/classify/batch  # Batch processing
```

### Root Cause Analysis
```
POST /ai/root-cause/analyze
{
  "ticket_id": "TKT-12345",
  "title": "Database connection errors",
  "description": "Application throwing connection exceptions...",
  "related_tickets": [...],
  "asset_data": {...},
  "historical_resolutions": [...]
}
```

### Predictive Maintenance
```
POST /ai/maintenance/predict-failure
{
  "asset_id": "SRV-001",
  "asset_name": "Production DB Server",
  "asset_type": "server",
  "age_days": 1450,
  "telemetry": {"cpu_usage": 85, "memory_usage": 92},
  "historical_failures": [...],
  "maintenance_history": [...]
}

POST /ai/maintenance/detect-anomaly  # Statistical anomaly detection
{
  "metric_name": "cpu_usage",
  "current_value": 95.5,
  "historical_values": [45, 52, 48, 51, 47, 89, 92, 95],
  "threshold_sigma": 2.0
}
```

### Automated Resolution
```
POST /ai/resolution/suggest
{
  "ticket_id": "TKT-12345",
  "title": "Password reset request",
  "description": "User forgot password...",
  "category": "account_access",
  "related_tickets": [...],
  "known_solutions": [...]
}

GET /ai/knowledge-base/search?query=password+reset&category=account_access
```

### Summary
```
GET /ai/summary/itsm  # Get AI capabilities summary
```

## Quick Start

### Local Development

```bash
# Install dependencies
pip install -r requirements.txt

# Configure environment
export LLM_DEEPSEEK_API_KEY="your-api-key"
export REDIS_HOST="localhost"

# Run server
python main.py
# or
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

### Docker

```bash
# Build and run
docker-compose up -d

# With custom API key
LLM_DEEPSEEK_API_KEY="your-key" docker-compose up -d
```

## Configuration

Configuration is loaded from `config.yaml` with environment variable override support:

| Setting | Env Variable | Default |
|---------|--------------|---------|
| LLM Provider | `LLM_PROVIDER` | `deepseek` |
| DeepSeek API Key | `LLM_DEEPSEEK_API_KEY` | - |
| DeepSeek Model | `LLM_DEEPSEEK_MODEL` | `deepseek-chat` |
| Request Timeout | `LLM_REQUEST_TIMEOUT` | 60 |
| Redis Host | `REDIS_HOST` | `localhost` |
| Redis Port | `REDIS_PORT` | 6379 |
| Redis Prefix | `REDIS_PREFIX` | `smartitsm:` |

## Features

### Ticket Classification
- Multi-category classification (hardware, software, network, security, etc.)
- Priority and urgency assignment
- Team assignment based on category
- SLA breach estimation
- Similar ticket detection

### Root Cause Analysis
- 5 Whys methodology
- Fishbone categorization
- Pattern matching against historical incidents
- Contributing factor identification
- Recurrence risk assessment

### Predictive Maintenance
- Asset failure probability prediction
- Risk level assessment (low/medium/high/critical)
- Estimated failure window
- Contributing factor analysis
- Maintenance recommendations
- Statistical anomaly detection (z-score based)

### Automated Resolution
- Knowledge base integration
- Known solution matching
- Auto-resolve eligibility (confidence > 0.8)
- Step-by-step resolution guidance
- Customer satisfaction prediction
- Batch processing support