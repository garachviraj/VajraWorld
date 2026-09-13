import pytest
import numpy as np
from edge.world_model.model import VajraWorldModel
from edge.simulation.counterfactual import CounterfactualSimulator

def test_counterfactual_simulation():
    model = VajraWorldModel()
    sim = CounterfactualSimulator(model)
    dummy_state = np.ones(40, dtype=np.float32) * 0.5

    result = sim.simulate_intervention(
        current_state_vector=dummy_state,
        action_type="ISOLATE_HOST",
        target_asset="Host-17"
    )

    assert result["target_asset"] == "Host-17"
    assert result["action_type"] == "ISOLATE_HOST"
    assert result["post_action_risk"] <= result["baseline_risk"]
    assert "utility_score" in result
    assert result["disruption_rating"] == "Medium"


def test_counterfactual_timeline_and_narrative():
    model = VajraWorldModel()
    sim = CounterfactualSimulator(model)
    dummy_state = np.ones(40, dtype=np.float32) * 0.65

    result = sim.simulate_intervention(
        current_state_vector=dummy_state,
        action_type="STORAGE_WRITE_LOCKDOWN",
        target_asset="Local Storage & User Documents"
    )

    # 1. Timeline verification (N=24 model-grounded points)
    timeline = result["timeline"]
    assert len(timeline) == 24, f"Expected 24 timeline samples, got {len(timeline)}"
    assert timeline[0]["t"] == 0.0
    assert timeline[-1]["t"] == 1.0

    # Verify timestamps are strictly increasing
    t_vals = [pt["t"] for pt in timeline]
    assert all(t_vals[i] <= t_vals[i+1] for i in range(len(t_vals)-1))

    # Verify each point has model-grounded risk, uncertainty, and stage
    for pt in timeline:
        assert 0.0 <= pt["risk"] <= 1.0
        assert 0.0 <= pt["uncertainty"] <= 1.0
        assert isinstance(pt["stage"], str) and len(pt["stage"]) > 0

    # Verify risk mitigation: risk at t=1.0 is lower than baseline at t=0.0
    assert timeline[-1]["risk"] < timeline[0]["risk"]
    assert result["residual_risk"] <= result["baseline_risk"]

    # 2. Narrative verification
    narrative = result["narrative"]
    assert len(narrative) >= 4
    assert narrative[0]["t"] == 0.0
    assert narrative[0]["mitre"] == "T1486" # Ransomware tactic
    assert "Local Storage" in narrative[0]["text"]
    assert any("engaged" in n["text"].lower() or "applied" in n["text"].lower() for n in narrative)
    assert any("containment" in n["text"].lower() or "stabilized" in n["text"].lower() for n in narrative)

    # 3. Factor attribution (SHAP explainability)
    factors = result["top_factors"]
    assert len(factors) >= 2
    for f in factors:
        assert "feature" in f and len(f["feature"]) > 0
        assert isinstance(f["impact"], (int, float))
        assert "description" in f and len(f["description"]) > 0


def test_counterfactual_actions_attributions():
    model = VajraWorldModel()
    sim = CounterfactualSimulator(model)
    dummy_state = np.ones(40, dtype=np.float32) * 0.55

    actions = [
        ("OVERLAY_PERMISSION_STRIP", "Financial Apps & Screen Surface", "T1056"),
        ("AUTONOMOUS_SOCKET_CONTAINMENT", "Android Kernel & Socket Stack", "T1548"),
        ("DNS_GATEWAY_SPOOF_BLOCK", "Network Interface & Socket Stack", "T1041"),
        ("EPHEMERAL_OTP_SHIELD", "Notification Privacy Vault", "T1114")
    ]

    for action, target, expected_mitre in actions:
        res = sim.simulate_intervention(
            current_state_vector=dummy_state,
            action_type=action,
            target_asset=target
        )
        assert len(res["timeline"]) == 24
        assert res["narrative"][0]["mitre"] == expected_mitre
        assert len(res["top_factors"]) >= 2
        assert res["intervention_t"] == 0.35

