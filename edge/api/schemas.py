"""
Pydantic API request and response schemas for VajraWorld Edge API.
Matches Blueprint Section 19.
"""
from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional

# Telemetry
class FlowTelemetryItem(BaseModel):
    src_ip: str
    dst_ip: str
    src_port: int
    dst_port: int
    protocol: str = "TCP"
    tcp_flags: str = ""
    bytes_fwd: int = 0
    bytes_bwd: int = 0
    pkts_fwd: int = 1
    pkts_bwd: int = 0
    duration: float = 0.0

class FlowBatchRequest(BaseModel):
    flows: List[FlowTelemetryItem]

class PcapAnalyzeRequest(BaseModel):
    pcap_path: str
    max_packets: int = 10000

# State
class StateCurrentResponse(BaseModel):
    window_id: str
    timestamp: str
    nodes_count: int
    edges_count: int
    mean_risk: float
    predicted_stage: str
    raw_features: Dict[str, Any]

# Forecast
class ForecastRequest(BaseModel):
    state_id: Optional[str] = None
    horizon_steps: int = Field(default=6, ge=1, le=24)
    rollouts: int = Field(default=32, ge=1, le=128)
    environment_profile: str = "enterprise"

class DriverItem(BaseModel):
    feature: str
    impact: float
    direction: Optional[str] = "up"

class FutureBranchItem(BaseModel):
    name: str
    probability: float
    trajectory_trend: str
    terminal_stage: str
    mean_final_risk: float

class ForecastResponse(BaseModel):
    forecast_id: str
    state_id: str
    horizon_steps: int
    current_risk: float
    horizon_risks: List[float]
    predicted_stage: str
    stage_probability: float
    confidence: float
    uncertainty: float
    lead_time_sec: int
    critical_assets: List[str]
    drivers: List[DriverItem]
    branches: List[FutureBranchItem]
    model_version: str
    timestamp: float

# Simulation
class SimulationRequest(BaseModel):
    target_asset: str
    action_type: str
    parameters: Optional[Dict[str, Any]] = None

class SimulationResponse(BaseModel):
    simulation_id: str
    target_asset: str
    action_type: str
    baseline_risk: float
    post_action_risk: float
    residual_risk: float
    risk_reduction_pct: int
    new_likely_stage: str
    disruption_rating: str
    utility_score: float
    is_recommended: bool
    read_only_mode: bool
    timestamp: float

# Incidents
class IncidentItem(BaseModel):
    incident_id: str
    title: str
    status: str
    severity: str
    risk: float
    confidence: float
    eta_seconds: int
    predicted_stage: str
    affected_assets: List[str]
    evidence: List[Dict[str, Any]]
    recommended_action: str
    acknowledged: bool
    created_at: str

# Model status
class ModelStatusResponse(BaseModel):
    model_version: str
    active_model_id: str
    status: str
    calibration: str
    accuracy: float
    brier_score: float
    lead_time_sec: float
    sensor_coverage: float
    telemetry_freshness_sec: float
    ood_rate: float
    drift_score: float
    inference_latency_ms: float
    environment_profile: str

# Audit
class AuditItem(BaseModel):
    audit_id: str
    timestamp: str
    actor: str
    action_type: str
    target: str
    details_json: Optional[str] = None
    result: str
