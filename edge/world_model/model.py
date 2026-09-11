"""
Integrated VajraWorld Hybrid Temporal Graph World Model.
Combines state encoding, transition dynamics, ATT&CK stage progression,
and uncertainty calibration into an operational PyTorch model.
"""
import torch
import torch.nn as nn
from typing import Dict, Any, List, Tuple
from edge.world_model.encoder import LatentStateEncoder
from edge.world_model.transition_model import LatentTransitionModel
from edge.world_model.stage_head import StageHead, ATTACK_STAGES
from edge.world_model.uncertainty_head import UncertaintyHead

class VajraWorldModel(nn.Module):
    def __init__(self, input_dim: int = 40, latent_dim: int = 64):
        super().__init__()
        self.input_dim = input_dim
        self.latent_dim = latent_dim

        self.encoder = LatentStateEncoder(input_dim=input_dim, latent_dim=latent_dim)
        self.transition = LatentTransitionModel(latent_dim=latent_dim)
        self.stage_head = StageHead(latent_dim=latent_dim, num_stages=len(ATTACK_STAGES))
        self.uncertainty_head = UncertaintyHead(latent_dim=latent_dim)

        # Risk scoring head: probability of malicious progression
        self.risk_head = nn.Sequential(
            nn.Linear(latent_dim, 32),
            nn.ReLU(),
            nn.Linear(32, 1),
            nn.Sigmoid()
        )

        # Reconstruction head for representation self-supervision
        self.decoder = nn.Sequential(
            nn.Linear(latent_dim, 128),
            nn.ReLU(),
            nn.Linear(128, input_dim)
        )

    def forward(self, state_vec: torch.Tensor, missingness_ratio: float = 0.0) -> Dict[str, torch.Tensor]:
        """Runs a single-step forward pass."""
        z = self.encoder(state_vec)
        mu_next, logvar_next = self.transition(z)
        stage_logits = self.stage_head(z)
        stage_probs = torch.softmax(stage_logits, dim=-1)
        risk = self.risk_head(z)
        uncertainty = self.uncertainty_head(z, missingness_ratio)
        s_recon = self.decoder(z)

        return {
            "z": z,
            "mu_next": mu_next,
            "logvar_next": logvar_next,
            "stage_logits": stage_logits,
            "stage_probs": stage_probs,
            "risk": risk.squeeze(-1),
            "uncertainty": uncertainty,
            "reconstruction": s_recon
        }

    def rollout(self, state_vec: torch.Tensor, horizon_steps: int = 6, num_rollouts: int = 16, temperature: float = 0.8) -> Dict[str, Any]:
        """
        Performs K-step stochastic forward simulation from initial state vector S_t.
        Returns multi-horizon risk paths, branching future states, and expected stage.
        """
        self.eval()
        with torch.no_grad():
            if state_vec.dim() == 1:
                state_vec = state_vec.unsqueeze(0)

            z0 = self.encoder(state_vec) # [1, latent_dim]
            current_risk = float(self.risk_head(z0).item())
            current_stage_probs = torch.softmax(self.stage_head(z0), dim=-1).squeeze(0).tolist()
            base_uncertainty = float(self.uncertainty_head(z0).item())

            # Expand for stochastic rollouts
            z_batch = z0.repeat(num_rollouts, 1) # [N, latent_dim]
            all_rollout_risks = [] # [N, horizon_steps]
            all_rollout_stages = [] # [N, horizon_steps]

            step_risks = [current_risk]

            for step in range(horizon_steps):
                z_batch = self.transition.sample_next_state(z_batch, temperature=temperature)
                r = self.risk_head(z_batch).squeeze(-1) # [N]
                stg = torch.argmax(self.stage_head(z_batch), dim=-1) # [N]
                all_rollout_risks.append(r.cpu().numpy())
                all_rollout_stages.append(stg.cpu().numpy())
                step_risks.append(float(r.mean().item()))

            # Transpose to [N, horizon_steps]
            import numpy as np
            rollout_matrix = np.array(all_rollout_risks).T # [N, horizon_steps]
            stages_matrix = np.array(all_rollout_stages).T # [N, horizon_steps]

            # Future branches clustering (e.g., 3 dominant paths)
            final_risks = rollout_matrix[:, -1]
            high_idx = np.where(final_risks >= 0.6)[0]
            med_idx = np.where((final_risks >= 0.3) & (final_risks < 0.6))[0]
            low_idx = np.where(final_risks < 0.3)[0]

            total_n = max(num_rollouts, 1)
            branches = [
                {
                    "name": "lateral movement -> critical asset",
                    "probability": round(len(high_idx) / total_n, 2),
                    "trajectory_trend": "escalating",
                    "terminal_stage": "Lateral Movement",
                    "mean_final_risk": round(float(final_risks[high_idx].mean()), 2) if len(high_idx) > 0 else 0.75
                },
                {
                    "name": "credential abuse & discovery loop",
                    "probability": round(len(med_idx) / total_n, 2),
                    "trajectory_trend": "contained",
                    "terminal_stage": "Discovery",
                    "mean_final_risk": round(float(final_risks[med_idx].mean()), 2) if len(med_idx) > 0 else 0.45
                },
                {
                    "name": "benign stabilization",
                    "probability": round(len(low_idx) / total_n, 2),
                    "trajectory_trend": "stabilizing",
                    "terminal_stage": "Benign",
                    "mean_final_risk": round(float(final_risks[low_idx].mean()), 2) if len(low_idx) > 0 else 0.15
                }
            ]

            # Highest probability terminal stage
            most_likely_stage_idx = int(np.argmax(current_stage_probs))
            predicted_stage_name = ATTACK_STAGES[most_likely_stage_idx]

            return {
                "current_risk": round(current_risk, 3),
                "horizon_risks": [round(x, 3) for x in step_risks],
                "confidence": round(1.0 - base_uncertainty, 3),
                "uncertainty": round(base_uncertainty, 3),
                "predicted_stage": predicted_stage_name,
                "stage_probability": round(current_stage_probs[most_likely_stage_idx], 3),
                "stage_distribution": {ATTACK_STAGES[i]: round(current_stage_probs[i], 3) for i in range(len(ATTACK_STAGES))},
                "branches": branches
            }
