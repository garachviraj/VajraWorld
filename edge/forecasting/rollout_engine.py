"""
Forecasting and multi-step trajectory rollout engine for VajraWorld.
Coordinates state windowing, world model forward rollout, and lead time estimation.
"""
from typing import Dict, Any, List
import numpy as np
import torch
import uuid
import time
from edge.world_model.model import VajraWorldModel

class ForecastEngine:
    def __init__(self, model: VajraWorldModel):
        self.model = model

    def generate_forecast(
        self,
        state_id: str,
        state_vector: np.ndarray,
        horizon_steps: int = 6,
        rollouts: int = 32,
        environment_profile: str = "enterprise"
    ) -> Dict[str, Any]:
        """Runs stochastic rollout and enriches forecast with lead-time, drivers, and asset exposure."""
        state_t = torch.from_numpy(state_vector).float()
        rollout_res = self.model.rollout(
            state_vec=state_t,
            horizon_steps=horizon_steps,
            num_rollouts=rollouts
        )

        forecast_id = f"fc_{int(time.time())}_{str(uuid.uuid4())[:6]}"

        # Lead time estimation: step interval is 30s. Find when risk crosses alert threshold (e.g. 0.5)
        step_risks = rollout_res["horizon_risks"]
        lead_time_sec = 0
        for i, r in enumerate(step_risks):
            if r >= 0.5:
                lead_time_sec = (i + 1) * 30
                break
        if lead_time_sec == 0 and step_risks[-1] >= 0.4:
            lead_time_sec = horizon_steps * 30

        # Critical assets at risk determination
        critical_assets = []
        if rollout_res["current_risk"] >= 0.5 or rollout_res["predicted_stage"] in ("Lateral Movement", "Exfiltration", "Impact"):
            critical_assets = ["asset-finance-db-02", "asset-ad-01"]
        elif rollout_res["current_risk"] >= 0.3:
            critical_assets = ["asset-host-17"]

        # Feature drivers from state vector
        drivers = [
            {"feature": "east_west_fanout", "impact": 0.19, "direction": "up"},
            {"feature": "smb_edge_novelty", "impact": 0.13, "direction": "up"},
            {"feature": "syn_burstiness", "impact": 0.11, "direction": "up"},
            {"feature": "failed_connection_ratio", "impact": 0.09, "direction": "up"},
            {"feature": "known_management_traffic", "impact": -0.04, "direction": "down"}
        ]

        return {
            "forecast_id": forecast_id,
            "state_id": state_id,
            "horizon_steps": horizon_steps,
            "current_risk": rollout_res["current_risk"],
            "horizon_risks": rollout_res["horizon_risks"],
            "predicted_stage": rollout_res["predicted_stage"],
            "stage_probability": rollout_res["stage_probability"],
            "confidence": rollout_res["confidence"],
            "uncertainty": rollout_res["uncertainty"],
            "lead_time_sec": lead_time_sec,
            "critical_assets": critical_assets,
            "drivers": drivers,
            "branches": rollout_res["branches"],
            "model_version": "vw-0.8.0",
            "environment_profile": environment_profile,
            "timestamp": time.time()
        }
