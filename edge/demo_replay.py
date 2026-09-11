"""
VajraWorld Competition Demo Scenario Replay Engine.
Simulates the exact 2-minute attack trajectory described in Blueprint Section 38:
- Phase 1 (0:00 - 0:20): Normal network activity (Risk 12%, stable)
- Phase 2 (0:20 - 0:40): Reconnaissance scanning burst (SYN bursts, entropy shifts)
- Phase 3 (0:40 - 1:00): Progression forecast (Recon -> Discovery / Creds, ETA ~60s, Risk 74%)
- Phase 4 (1:00 - 1:20): Lateral Movement probing toward AD-01 & Finance-DB-02
- Phase 5 (1:20 - 1:40): Counterfactual defence simulation (Isolate Host-17 -> Risk drops to 21%)
- Phase 6 (1:40 - 2:00): Containment verification and audit trail.
"""
import time
import requests
import sys

API_BASE = "http://127.0.0.1:8000/v1"

def run_scenario(api_url: str = API_BASE, fast_mode: bool = True):
    delay = 1.0 if fast_mode else 5.0
    print(f"[+] Starting VajraWorld Demo Replay against {api_url} (step delay: {delay}s)...")

    # Phase 1: Normal Network
    print("\n--- [Minute 0:00 - 0:20] Normal Baseline Network Activity ---")
    normal_flows = [
        {"src_ip": "192.168.1.17", "dst_ip": "10.0.0.1", "src_port": 51234, "dst_port": 443, "protocol": "TCP", "tcp_flags": "SA", "bytes_fwd": 1420, "bytes_bwd": 8420, "duration": 1.2},
        {"src_ip": "192.168.1.22", "dst_ip": "10.0.0.1", "src_port": 51235, "dst_port": 53, "protocol": "UDP", "tcp_flags": "", "bytes_fwd": 68, "bytes_bwd": 182, "duration": 0.05},
        {"src_ip": "192.168.1.10", "dst_ip": "192.168.1.17", "src_port": 88, "dst_port": 49200, "protocol": "TCP", "tcp_flags": "PA", "bytes_fwd": 850, "bytes_bwd": 420, "duration": 0.5}
    ]
    res = requests.post(f"{api_url}/telemetry/batch", json={"flows": normal_flows})
    print(f"[*] Ingested normal baseline: {res.json()}")
    fc = requests.post(f"{api_url}/forecast", json={"horizon_steps": 6, "rollouts": 32}).json()
    print(f"[*] Current Risk: {int(fc['current_risk'] * 100)}% | Stage: {fc['predicted_stage']} | Status: STABLE")
    time.sleep(delay)

    # Phase 2: Reconnaissance Port Scanning
    print("\n--- [Minute 0:20 - 0:40] Reconnaissance Surge from Host-17 ---")
    recon_flows = []
    for port in [21, 22, 23, 80, 135, 139, 445, 1433, 3389, 8080]:
        recon_flows.append({
            "src_ip": "192.168.1.17",
            "dst_ip": f"192.168.1.{port % 25 + 10}",
            "src_port": 40000 + port,
            "dst_port": port,
            "protocol": "TCP",
            "tcp_flags": "S",
            "bytes_fwd": 60,
            "bytes_bwd": 0,
            "duration": 0.01
        })
    res = requests.post(f"{api_url}/telemetry/batch", json={"flows": recon_flows})
    print(f"[*] Ingested {len(recon_flows)} SYN scan flows across subnet 192.168.1.0/24")
    time.sleep(delay)

    # Phase 3: Progression Forecast
    print("\n--- [Minute 0:40 - 1:00] Progression Trajectory Warning ---")
    fc = requests.post(f"{api_url}/forecast", json={"horizon_steps": 6, "rollouts": 64}).json()
    print(f"[!] Trajectory Warning: Risk surged to {int(fc['current_risk'] * 100)}%!")
    print(f"[!] Predicted Stage: {fc['predicted_stage']} (Prob: {int(fc['stage_probability'] * 100)}%) | ETA: ~{fc['lead_time_sec']}s")
    print(f"[!] Critical Assets exposed: {fc['critical_assets']}")
    for b in fc["branches"]:
        print(f"    - Branch: '{b['name']}' ({int(b['probability']*100)}%) -> Final Risk: {int(b['mean_final_risk']*100)}%")
    time.sleep(delay)

    # Phase 4: Lateral Movement Probing
    print("\n--- [Minute 1:00 - 1:20] Lateral Movement Toward AD-01 and Finance-DB-02 ---")
    lateral_flows = [
        {"src_ip": "192.168.1.17", "dst_ip": "192.168.1.10", "src_port": 50122, "dst_port": 88, "protocol": "TCP", "tcp_flags": "PA", "bytes_fwd": 4200, "bytes_bwd": 120, "duration": 0.8},
        {"src_ip": "192.168.1.17", "dst_ip": "192.168.2.50", "src_port": 50123, "dst_port": 445, "protocol": "TCP", "tcp_flags": "S", "bytes_fwd": 180, "bytes_bwd": 0, "duration": 0.05}
    ]
    requests.post(f"{api_url}/telemetry/batch", json={"flows": lateral_flows})
    incidents = requests.get(f"{api_url}/incidents").json()
    latest_inc = incidents[0] if incidents else {}
    print(f"[!] Active Alert: {latest_inc.get('incident_id')} - {latest_inc.get('title')}")
    print(f"    Recommended Action: {latest_inc.get('recommended_action')}")
    time.sleep(delay)

    # Phase 5: Counterfactual Defence Simulation ("Test Defence")
    print("\n--- [Minute 1:20 - 1:40] Testing Defence: 'Isolate Host-17' ---")
    sim = requests.post(f"{api_url}/simulation", json={
        "target_asset": "Host-17",
        "action_type": "ISOLATE_HOST",
        "parameters": {"method": "VLAN_QUARANTINE"}
    }).json()
    print(f"[*] Simulation Complete:")
    print(f"    Baseline Risk:      {int(sim['baseline_risk'] * 100)}%")
    print(f"    Post-Action Risk:   {int(sim['post_action_risk'] * 100)}% (Drop: -{sim['risk_reduction_pct']}%)")
    print(f"    Residual Risk:      {int(sim['residual_risk'] * 100)}%")
    print(f"    Disruption Rating:  {sim['disruption_rating']}")
    print(f"    Action Utility:     {sim['utility_score']} (Recommended: {sim['is_recommended']})")
    time.sleep(delay)

    # Phase 6: Acknowledge & Verify Audit
    print("\n--- [Minute 1:40 - 2:00] Acknowledge Incident & Review Audit Trail ---")
    if latest_inc.get("incident_id"):
        ack = requests.post(f"{api_url}/incidents/{latest_inc['incident_id']}/ack").json()
        print(f"[*] Incident acknowledged: {ack}")
    audits = requests.get(f"{api_url}/audit").json()
    print(f"[*] Verified {len(audits)} immutable audit records.")
    print("\n[VajraWorld Demo Scenario Completed Successfully!]")

if __name__ == "__main__":
    run_scenario(fast_mode=True)
