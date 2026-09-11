import pytest
from fastapi.testclient import TestClient
from edge.api.server import app

client = TestClient(app)

def test_api_health():
    response = client.get("/")
    assert response.status_code == 200
    assert response.json()["status"] == "ONLINE"

def test_telemetry_and_state():
    # Ingest a flow
    flow_payload = {
        "src_ip": "192.168.1.17",
        "dst_ip": "10.0.0.1",
        "src_port": 50123,
        "dst_port": 443,
        "protocol": "TCP",
        "tcp_flags": "S",
        "bytes_fwd": 60,
        "bytes_bwd": 0,
        "duration": 0.05
    }
    resp = client.post("/v1/telemetry/flow", json=flow_payload)
    assert resp.status_code == 200

    # Get current state
    st_resp = client.get("/v1/state/current")
    assert st_resp.status_code == 200
    assert "window_id" in st_resp.json()

    # Get current graph
    g_resp = client.get("/v1/graph/current")
    assert g_resp.status_code == 200
    assert "nodes" in g_resp.json()

def test_forecast_and_simulation():
    fc_resp = client.post("/v1/forecast", json={"horizon_steps": 4, "rollouts": 16})
    assert fc_resp.status_code == 200
    fc_data = fc_resp.json()
    assert "forecast_id" in fc_data
    assert "predicted_stage" in fc_data
    assert "branches" in fc_data

    # Test simulation
    sim_resp = client.post("/v1/simulation", json={
        "target_asset": "Host-17",
        "action_type": "ISOLATE_HOST"
    })
    assert sim_resp.status_code == 200
    sim_data = sim_resp.json()
    assert sim_data["target_asset"] == "Host-17"
    assert sim_data["action_type"] == "ISOLATE_HOST"
    assert sim_data["residual_risk"] <= sim_data["baseline_risk"]

def test_incidents_and_model_status():
    inc_resp = client.get("/v1/incidents")
    assert inc_resp.status_code == 200
    assert isinstance(inc_resp.json(), list)

    status_resp = client.get("/v1/model/status")
    assert status_resp.status_code == 200
    assert status_resp.json()["status"] == "HEALTHY"

def test_live_streaming_summary():
    resp = client.get("/v1/live/summary")
    assert resp.status_code == 200
    data = resp.json()
    assert "network_health" in data
    assert "current_risk" in data
    assert "predicted_stage" in data
    assert "active_flows_count" in data
    assert "radar_status" in data

