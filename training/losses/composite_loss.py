"""
Composite loss objective for VajraWorld World Model training.
Implements:
L_total = lambda1 * L_transition + lambda2 * L_stage + lambda3 * L_future + lambda4 * L_recon + lambda5 * L_calib
Matches Blueprint Section 10.
"""
import torch
import torch.nn as nn
import torch.nn.functional as F

class CompositeWorldModelLoss(nn.Module):
    def __init__(
        self,
        lambda_transition: float = 1.0,
        lambda_stage: float = 1.0,
        lambda_future: float = 0.8,
        lambda_recon: float = 0.5,
        lambda_calib: float = 0.3
    ):
        super().__init__()
        self.l_trans = lambda_transition
        self.l_stage = lambda_stage
        self.l_future = lambda_future
        self.l_recon = lambda_recon
        self.l_calib = lambda_calib

    def forward(
        self,
        outputs: dict,
        target_z_next: torch.Tensor,
        target_stage: torch.Tensor,
        target_future_attack: torch.Tensor,
        target_state: torch.Tensor
    ) -> dict:
        # 1. Transition Negative Log-Likelihood (Gaussian NLL)
        mu = outputs["mu_next"]
        logvar = outputs["logvar_next"]
        # Gaussian NLL = 0.5 * (logvar + (target - mu)^2 / exp(logvar))
        inv_var = torch.exp(-logvar)
        loss_transition = 0.5 * torch.mean(logvar + (target_z_next - mu) ** 2 * inv_var)

        # 2. ATT&CK Stage Cross-Entropy
        loss_stage = F.cross_entropy(outputs["stage_logits"], target_stage)

        # 3. Future Attack Prediction Loss (BCE)
        loss_future = F.binary_cross_entropy(outputs["risk"], target_future_attack)

        # 4. State Reconstruction Loss (MSE)
        loss_recon = F.mse_loss(outputs["reconstruction"], target_state)

        # 5. Calibration penalty (Brier score proxy)
        brier = torch.mean((outputs["risk"] - target_future_attack) ** 2)

        total_loss = (
            self.l_trans * loss_transition +
            self.l_stage * loss_stage +
            self.l_future * loss_future +
            self.l_recon * loss_recon +
            self.l_calib * brier
        )

        return {
            "loss_total": total_loss,
            "loss_transition": loss_transition.item(),
            "loss_stage": loss_stage.item(),
            "loss_future": loss_future.item(),
            "loss_recon": loss_recon.item(),
            "brier_score": brier.item()
        }
