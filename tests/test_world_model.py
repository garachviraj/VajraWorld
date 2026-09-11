import pytest
import torch
from edge.world_model.model import VajraWorldModel
from edge.world_model.stage_head import ATTACK_STAGES

def test_world_model_forward():
    model = VajraWorldModel(input_dim=40, latent_dim=64)
    model.eval()
    x = torch.randn(2, 40)
    out = model(x)
    assert out["z"].shape == (2, 64)
    assert out["mu_next"].shape == (2, 64)
    assert out["stage_probs"].shape == (2, len(ATTACK_STAGES))
    assert out["risk"].shape == (2,)
    assert out["reconstruction"].shape == (2, 40)

def test_world_model_rollout():
    model = VajraWorldModel(input_dim=40, latent_dim=64)
    x = torch.randn(40)
    res = model.rollout(x, horizon_steps=6, num_rollouts=16)
    assert "current_risk" in res
    assert len(res["horizon_risks"]) == 7 # 0 + 6 steps
    assert res["predicted_stage"] in ATTACK_STAGES
    assert len(res["branches"]) == 3
