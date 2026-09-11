"""
Uncertainty and calibration head for VajraWorld.
Produces calibrated confidence scores, prediction intervals, and telemetry missingness penalties.
"""
import torch
import torch.nn as nn

class UncertaintyHead(nn.Module):
    def __init__(self, latent_dim: int = 64):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(latent_dim, 32),
            nn.ReLU(),
            nn.Linear(32, 2) # [log_variance (aleatoric), ood_score]
        )

    def forward(self, z: torch.Tensor, missingness_ratio: float = 0.0) -> torch.Tensor:
        """
        Outputs calibrated uncertainty in [0, 1].
        Applies missingness penalty when sensor coverage is degraded.
        """
        out = self.net(z)
        log_var = out[:, 0]
        ood_raw = out[:, 1]

        # Convert log_var to variance proxy
        aleatoric = torch.sigmoid(log_var)
        ood = torch.sigmoid(ood_raw)

        # Base uncertainty combination
        uncertainty = 0.6 * aleatoric + 0.4 * ood
        # Penalize if telemetry is missing
        penalty = float(missingness_ratio) * 0.35
        calibrated_uncertainty = torch.clamp(uncertainty + penalty, 0.0, 0.95)

        return calibrated_uncertainty
