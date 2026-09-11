"""
Latent transition dynamics model for VajraWorld.
Learns P(z_t+1 | z_t, context) parameterized by mean mu and standard deviation sigma.
Enables forward stochastic rollouts into multiple hypothetical futures.
"""
import torch
import torch.nn as nn
from typing import Tuple

class LatentTransitionModel(nn.Module):
    def __init__(self, latent_dim: int = 64):
        super().__init__()
        self.transition_net = nn.Sequential(
            nn.Linear(latent_dim, 128),
            nn.LayerNorm(128),
            nn.GELU(),
            nn.Linear(128, 128),
            nn.LayerNorm(128),
            nn.GELU()
        )
        self.mu_head = nn.Linear(128, latent_dim)
        self.logvar_head = nn.Linear(128, latent_dim)

    def forward(self, z: torch.Tensor) -> Tuple[torch.Tensor, torch.Tensor]:
        """Returns distribution parameters (mu, logvar) for the next latent state."""
        h = self.transition_net(z)
        mu = self.mu_head(h)
        logvar = torch.clamp(self.logvar_head(h), -4.0, 2.0)
        return mu, logvar

    def sample_next_state(self, z: torch.Tensor, temperature: float = 1.0) -> torch.Tensor:
        """Draws a stochastic sample z_t+1 ~ N(mu, (temp * sigma)^2)."""
        mu, logvar = self.forward(z)
        std = torch.exp(0.5 * logvar) * temperature
        eps = torch.randn_like(std)
        return mu + eps * std
