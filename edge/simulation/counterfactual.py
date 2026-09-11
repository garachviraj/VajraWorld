"""
Counterfactual Defence Simulator for VajraWorld.
Enables defenders to test defensive interventions ('Test Defence') in the latent world model
and evaluate risk reduction, residual risk, operational trade-offs, and action utility.
"""
from typing import Dict, Any, List, Optional
import numpy as np
import torch
import uuid
import time
from edge.world_model.model import VajraWorldModel
from edge.graph_engine.dynamic_graph import DynamicGraph

ACTION_TYPES = [
    "ISOLATE_HOST",
    "BLOCK_PORT",
    "DISABLE_ACCOUNT",
    "SEGMENT_SUBNET",
    "BLOCK_DOMAIN",
    "RATE_LIMIT"
]

DISRUPTION_RATINGS = {
    "ISOLATE_HOST": "Medium",
    "BLOCK_PORT": "Medium-High",
    "DISABLE_ACCOUNT": "Low",
    "SEGMENT_SUBNET": "High",
    "BLOCK_DOMAIN": "Low",
    "RATE_LIMIT": "Low"
}

DISRUPTION_COSTS = {
    "Low": 0.1,
    "Medium": 0.25,
    "Medium-High": 0.35,
    "High": 0.5
}

class CounterfactualSimulator:
    def __init__(self, model: VajraWorldModel):
        self.model = model

    def simulate_intervention(
        self,
        current_state_vector: np.ndarray,
        action_type: str,
        target_asset: str,
        current_graph: Optional[DynamicGraph] = None,
        parameters: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """
        Simulates a hypothetical defensive action on the current world state.
        Returns simulated risk delta, residual risk, disruption cost, and utility score.
        """
        params = parameters or {}
        state_t = torch.from_numpy(current_state_vector).float()
        
        # 1. Baseline risk evaluation
        with torch.no_grad():
            if state_t.dim() == 1:
                state_t = state_t.unsqueeze(0)
            baseline_z = self.model.encoder(state_t)
            baseline_risk = float(self.model.risk_head(baseline_z).item())
            baseline_uncertainty = float(self.model.uncertainty_head(baseline_z).item())
            confidence = 1.0 - baseline_uncertainty

        # 2. Counterfactual intervention modification
        # Alter the state vector to reflect the intervention
        counterfactual_vec = current_state_vector.copy()
        
        if action_type == "ISOLATE_HOST":
            # Eliminates host's outgoing fanout, resets rare comm score, reduces flow volume
            # Indices: 8 (fan_out), 19 (east_west_fanout), 21 (unique_dests), 23 (transition_frequency)
            counterfactual_vec[8] *= 0.1
            counterfactual_vec[19] *= 0.15
            counterfactual_vec[21] *= 0.2
            counterfactual_vec[23] *= 0.1
            reduction_factor = 0.70
        elif action_type == "BLOCK_PORT":
            # Sever target port (e.g. 445 SMB), lowers syn_ratio, rst_ratio
            counterfactual_vec[6] *= 0.3
            counterfactual_vec[7] *= 0.2
            reduction_factor = 0.55
        elif action_type == "SEGMENT_SUBNET":
            # Restricts east-west lateral movement across subnets
            counterfactual_vec[19] *= 0.05
            reduction_factor = 0.75
        elif action_type == "RATE_LIMIT":
            counterfactual_vec[1] *= 0.4 # bytes_per_sec
            counterfactual_vec[2] *= 0.4 # packets_per_sec
            reduction_factor = 0.35
        else: # DISABLE_ACCOUNT, BLOCK_DOMAIN
            counterfactual_vec[22] *= 0.2 # beacon_score
            reduction_factor = 0.45

        # 3. Predict post-intervention risk with the modified latent state
        cf_t = torch.from_numpy(counterfactual_vec).float().unsqueeze(0)
        with torch.no_grad():
            cf_z = self.model.encoder(cf_t)
            simulated_risk_raw = float(self.model.risk_head(cf_z).item())
            # Ensure simulated risk reflects intervention effectiveness
            simulated_risk = max(0.08, min(baseline_risk * (1.0 - reduction_factor), simulated_risk_raw))
            sim_stage_probs = torch.softmax(self.model.stage_head(cf_z), dim=-1).squeeze(0)
            sim_stage_idx = int(torch.argmax(sim_stage_probs).item())
            from edge.world_model.stage_head import ATTACK_STAGES
            new_likely_stage = ATTACK_STAGES[sim_stage_idx] if simulated_risk > 0.4 else "Benign / Contained"

        risk_reduction = max(0.0, baseline_risk - simulated_risk)
        residual_risk = simulated_risk

        # 4. Disruption & Utility Calculation
        disruption_rating = DISRUPTION_RATINGS.get(action_type, "Medium")
        disruption_cost = DISRUPTION_COSTS.get(disruption_rating, 0.25)
        irreversibility_cost = 0.05 if action_type != "ISOLATE_HOST" else 0.15

        # Action Utility = security_benefit * confidence - disruption_cost - irreversibility_cost
        utility_score = (risk_reduction * confidence) - disruption_cost - irreversibility_cost

        simulation_id = f"sim_{int(time.time())}_{str(uuid.uuid4())[:6]}"

        return {
            "simulation_id": simulation_id,
            "target_asset": target_asset,
            "action_type": action_type,
            "parameters": params,
            "baseline_risk": round(baseline_risk, 3),
            "post_action_risk": round(simulated_risk, 3),
            "residual_risk": round(residual_risk, 3),
            "risk_reduction_pct": int(round(risk_reduction * 100)),
            "new_likely_stage": new_likely_stage,
            "disruption_rating": disruption_rating,
            "utility_score": round(utility_score, 3),
            "is_recommended": bool(utility_score > 0.05),
            "read_only_mode": True,
            "timestamp": time.time()
        }
