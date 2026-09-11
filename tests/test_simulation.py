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
