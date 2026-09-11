"""
FastAPI route handlers for VajraWorld Edge Intelligence Plane.
Matches all REST endpoints described in Blueprint Section 19.
"""
from fastapi import APIRouter, HTTPException, Depends, status
from typing import List, Dict, Any, Optional
import time
import uuid

from edge.api.schemas import (
    FlowTelemetryItem, FlowBatchRequest, PcapAnalyzeRequest,
    StateCurrentResponse, ForecastRequest, ForecastResponse,
    SimulationRequest, SimulationResponse, IncidentItem,
    ModelStatusResponse, AuditItem
)
from edge.storage.db import DatabaseManager
from edge.world_model.model import VajraWorldModel
from edge.forecasting.rollout_engine import ForecastEngine
from edge.simulation.counterfactual import CounterfactualSimulator
from edge.explainability.layered_explainer import LayeredExplainer
from edge.policy.action_policy import ActionPolicyEngine
from edge.feature_engine.state_builder import StateBuilder
from edge.graph_engine.dynamic_graph import DynamicGraph
from edge.graph_engine.node_encoder import NodeEncoder
from edge.collectors.pcap_collector import PcapCollector

router = APIRouter(prefix="/v1")

# Singletons shared across route lifetime
db = DatabaseManager()
world_model = VajraWorldModel()
world_model.eval()
forecast_engine = ForecastEngine(world_model)
counterfactual_sim = CounterfactualSimulator(world_model)
explainer = LayeredExplainer()
policy_engine = ActionPolicyEngine()
state_builder = StateBuilder()
dynamic_graph = DynamicGraph()
node_encoder = NodeEncoder()
pcap_collector = PcapCollector()

# In-memory buffer for active flow window
current_flows: List[Dict[str, Any]] = []

def _get_current_state_vector():
    flow_vec, raw_features = state_builder.build_state_vector(current_flows)
    graph_vec = node_encoder.encode_graph_features(dynamic_graph.graph)
    state_vector = np.concatenate([flow_vec, graph_vec])
    return state_vector, raw_features

import numpy as np

# ----------------- TELEMETRY -----------------
@router.post("/telemetry/flow")
def ingest_single_flow(item: FlowTelemetryItem):
    d = item.model_dump()
    current_flows.append(d)
    if len(current_flows) > 500:
        current_flows.pop(0)
    dynamic_graph.build_from_flows([d])
    return {"status": "ingested", "active_flows": len(current_flows)}

@router.post("/telemetry/batch")
def ingest_batch_flows(batch: FlowBatchRequest):
    flows_data = [f.model_dump() for f in batch.flows]
    current_flows.extend(flows_data)
    while len(current_flows) > 2000:
        current_flows.pop(0)
    dynamic_graph.build_from_flows(flows_data)
    
    # Auto-update latest state window in DB
    state_vec, raw_feats = _get_current_state_vector()
    window_id = f"w_{int(time.time())}"
    db.save_state_window(
        window_id=window_id,
        nodes_count=dynamic_graph.graph.number_of_nodes(),
        edges_count=dynamic_graph.graph.number_of_edges(),
        mean_risk=float(np.mean(state_vec[:5])),
        stage="Observed",
        raw_features=raw_feats
    )
    return {"status": "batch_ingested", "count": len(batch.flows), "window_id": window_id}

@router.post("/telemetry/pcap/analyze")
def analyze_pcap(req: PcapAnalyzeRequest):
    try:
        events = pcap_collector.parse_pcap(req.pcap_path, max_packets=req.max_packets)
        # Convert events to flow representations
        flows = []
        for ev in events:
            flows.append({
                "src_ip": ev.src_ip,
                "dst_ip": ev.dst_ip,
                "src_port": ev.src_port,
                "dst_port": ev.dst_port,
                "protocol": ev.protocol,
                "tcp_flags": ev.tcp_flags,
                "bytes_fwd": ev.payload_len,
                "bytes_bwd": 0,
                "duration": 0.1
            })
        current_flows.extend(flows)
        dynamic_graph.build_from_flows(flows)
        report = pcap_collector.get_summary_report()
        return {"status": "success", "report": report, "flows_generated": len(flows)}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

# ----------------- STATE & GRAPH -----------------
@router.get("/state/current", response_model=StateCurrentResponse)
def get_current_state():
    latest = db.get_latest_state()
    if latest:
        return StateCurrentResponse(
            window_id=latest["window_id"],
            timestamp=latest["timestamp"],
            nodes_count=latest["nodes_count"],
            edges_count=latest["edges_count"],
            mean_risk=latest["mean_risk"],
            predicted_stage=latest["predicted_stage"],
            raw_features=latest["raw_features"]
        )
    # Default initial state if DB has no windows yet
    state_vec, raw_feats = _get_current_state_vector()
    return StateCurrentResponse(
        window_id=f"w_{int(time.time())}",
        timestamp=time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        nodes_count=dynamic_graph.graph.number_of_nodes(),
        edges_count=dynamic_graph.graph.number_of_edges(),
        mean_risk=0.12,
        predicted_stage="Benign",
        raw_features=raw_feats
    )

@router.get("/graph/current")
def get_current_graph():
    return dynamic_graph.to_dict()

# ----------------- FORECAST -----------------
@router.post("/forecast", response_model=ForecastResponse)
def create_forecast(req: ForecastRequest):
    state_vec, raw_feats = _get_current_state_vector()
    state_id = req.state_id or f"state_{int(time.time())}"
    
    fc = forecast_engine.generate_forecast(
        state_id=state_id,
        state_vector=state_vec,
        horizon_steps=req.horizon_steps,
        rollouts=req.rollouts,
        environment_profile=req.environment_profile
    )
    db.save_forecast(fc)
    
    # Check if this forecast warrants auto-creating an incident
    if fc["current_risk"] >= 0.50:
        inc_id = f"INC-{int(time.time()) % 10000}"
        db.save_incident({
            "incident_id": inc_id,
            "title": f"Predicted progression: {fc['predicted_stage']} alert",
            "status": "OPEN",
            "severity": "CRITICAL" if fc["current_risk"] >= 0.7 else "HIGH",
            "risk": fc["current_risk"],
            "confidence": fc["confidence"],
            "eta_seconds": fc["lead_time_sec"],
            "predicted_stage": fc["predicted_stage"],
            "affected_assets": fc["critical_assets"],
            "evidence": fc["drivers"],
            "recommended_action": "Isolate Host-17"
        })

    return ForecastResponse(**fc)

@router.get("/forecast/{forecast_id}")
def get_forecast(forecast_id: str):
    fc = db.get_forecast_by_id(forecast_id)
    if not fc:
        raise HTTPException(status_code=404, detail="Forecast not found")
    return fc

@router.get("/forecast/{forecast_id}/explanations")
def get_forecast_explanations(forecast_id: str):
    fc = db.get_forecast_by_id(forecast_id)
    risk = fc["current_risk"] if fc else 0.74
    stage = fc["predicted_stage"] if fc else "Lateral Movement"
    _, raw_feats = _get_current_state_vector()
    return explainer.generate_explanation(
        forecast_id=forecast_id,
        current_risk=risk,
        predicted_stage=stage,
        features=raw_feats
    )

# ----------------- SIMULATION ("TEST DEFENCE") -----------------
@router.post("/simulation", response_model=SimulationResponse)
def run_simulation(req: SimulationRequest):
    state_vec, _ = _get_current_state_vector()
    sim_result = counterfactual_sim.simulate_intervention(
        current_state_vector=state_vec,
        action_type=req.action_type,
        target_asset=req.target_asset,
        current_graph=dynamic_graph,
        parameters=req.parameters
    )
    db.save_simulation(sim_result)
    db.log_audit(
        audit_id=f"aud_{int(time.time())}",
        actor="Defender Analyst",
        action_type=f"SIMULATE_{req.action_type}",
        target=req.target_asset,
        details=sim_result
    )
    return SimulationResponse(**sim_result)

@router.get("/simulation/{simulation_id}")
def get_simulation(simulation_id: str):
    sim = db.get_simulation_by_id(simulation_id)
    if not sim:
        raise HTTPException(status_code=404, detail="Simulation not found")
    return sim

# ----------------- INCIDENTS -----------------
@router.get("/incidents", response_model=List[IncidentItem])
def list_incidents(limit: int = 50):
    incidents = db.get_incidents(limit=limit)
    if not incidents:
        # Seed default demonstration incident matching blueprint Section 17 Screen 5
        sample = {
            "incident_id": "INC-2041",
            "title": "Predicted lateral movement toward Finance-DB-02",
            "status": "OPEN",
            "severity": "CRITICAL",
            "risk": 0.81,
            "confidence": 0.74,
            "eta_seconds": 134,
            "predicted_stage": "Lateral Movement",
            "affected_assets": ["asset-host-17", "asset-ad-01", "asset-finance-db-02"],
            "evidence": [
                {"source": "Host-17", "event": "Rapid port fan-out to 31 IPs in 45s", "severity": "HIGH"},
                {"source": "Host-17 -> AD-01", "event": "Kerberos ticket abuse pattern on port 88", "severity": "HIGH"}
            ],
            "recommended_action": "Isolate Host-17 (estimated risk reduction 58%)",
            "acknowledged": False,
            "created_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
        }
        db.save_incident(sample)
        incidents = db.get_incidents()
    return incidents

@router.get("/incidents/{incident_id}")
def get_incident(incident_id: str):
    inc = db.get_incident_by_id(incident_id)
    if not inc:
        raise HTTPException(status_code=404, detail="Incident not found")
    return inc

@router.post("/incidents/{incident_id}/ack")
def acknowledge_incident(incident_id: str):
    success = db.acknowledge_incident(incident_id)
    if not success:
        raise HTTPException(status_code=404, detail="Incident not found")
    db.log_audit(
        audit_id=f"aud_{int(time.time())}",
        actor="Security Operator",
        action_type="ACKNOWLEDGE_INCIDENT",
        target=incident_id,
        details={"status": "INVESTIGATING"}
    )
    return {"status": "acknowledged", "incident_id": incident_id}

# ----------------- MODEL HEALTH & ASSETS -----------------
@router.get("/model/status", response_model=ModelStatusResponse)
def get_model_status():
    return db.get_model_status()

@router.get("/model/versions")
def get_model_versions():
    with db.get_connection() as conn:
        cur = conn.cursor()
        cur.execute("SELECT * FROM model_versions")
        return [dict(r) for r in cur.fetchall()]

@router.get("/assets")
def list_assets():
    return db.get_assets()

@router.get("/assets/{asset_id}")
def get_asset(asset_id: str):
    asset = db.get_asset_by_id(asset_id)
    if not asset:
        raise HTTPException(status_code=404, detail="Asset not found")
    return asset

@router.get("/audit")
def list_audit_events(limit: int = 50):
    return db.get_audit_log(limit=limit)
