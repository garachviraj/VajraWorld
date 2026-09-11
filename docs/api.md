# VajraWorld Edge REST API Reference

The Edge Intelligence Plane exposes a high-performance RESTful API compliant with OpenAPI 3.0.

Default Base URL: `http://localhost:8000/v1`

---

## Telemetry Endpoints

### `POST /v1/telemetry/flow`
Ingests a single network flow record.
```json
{
  "src_ip": "192.168.1.17",
  "dst_ip": "192.168.1.10",
  "src_port": 50122,
  "dst_port": 88,
  "protocol": "TCP",
  "tcp_flags": "PA",
  "bytes_fwd": 4200,
  "bytes_bwd": 120,
  "duration": 0.8
}
```

### `POST /v1/telemetry/batch`
Ingests a batch of flow records and updates the rolling state window.

### `POST /v1/telemetry/pcap/analyze`
Parses a PCAP file using Scapy/binary extractor and reconstructs flow sessions.

---

## State & Graph Endpoints

### `GET /v1/state/current`
Returns current rolling state window summary, node/edge counts, and mean risk.

### `GET /v1/graph/current`
Returns active network topology: nodes (hosts, servers, databases, OT assets) and directional edges (CONNECTS_TO, SCANS, ACCESSES, AUTHENTICATES_TO).

---

## Forecasting Endpoints

### `POST /v1/forecast`
Runs $K$-step stochastic rollouts from current network state.
```json
{
  "horizon_steps": 6,
  "rollouts": 32,
  "environment_profile": "enterprise"
}
```
**Response:**
```json
{
  "forecast_id": "fc_1726079000_a81f3b",
  "current_risk": 0.74,
  "horizon_risks": [0.74, 0.78, 0.82, 0.85, 0.89, 0.91, 0.92],
  "predicted_stage": "Lateral Movement",
  "stage_probability": 0.78,
  "confidence": 0.82,
  "uncertainty": 0.18,
  "lead_time_sec": 74,
  "critical_assets": ["asset-finance-db-02", "asset-ad-01"],
  "branches": [
    {
      "name": "lateral movement -> critical asset",
      "probability": 0.72,
      "trajectory_trend": "escalating",
      "terminal_stage": "Lateral Movement",
      "mean_final_risk": 0.84
    }
  ]
}
```

### `GET /v1/forecast/{id}/explanations`
Returns 5-level layered explanation: human narrative, SHAP feature attribution, temporal evidence timeline, minimal subgraph, and uncertainty warning.

---

## Counterfactual Defence Endpoints ("Test Defence")

### `POST /v1/simulation`
Simulates a defensive intervention on current world state.
```json
{
  "target_asset": "Host-17",
  "action_type": "ISOLATE_HOST",
  "parameters": {"method": "VLAN_QUARANTINE"}
}
```
**Response:**
```json
{
  "simulation_id": "sim_1726079010_e9c1",
  "target_asset": "Host-17",
  "action_type": "ISOLATE_HOST",
  "baseline_risk": 0.78,
  "post_action_risk": 0.23,
  "residual_risk": 0.23,
  "risk_reduction_pct": 55,
  "new_likely_stage": "Benign / Contained",
  "disruption_rating": "Medium",
  "utility_score": 0.42,
  "is_recommended": true,
  "read_only_mode": true
}
```

---

## Incident & Audit Endpoints

- `GET /v1/incidents` — List active incidents with risk and ETA.
- `POST /v1/incidents/{id}/ack` — Acknowledge and transition incident to INVESTIGATING.
- `GET /v1/audit` — Retrieve immutable audit trail.
- `GET /v1/model/status` — Operational health, calibration, lead time, and sensor coverage.
