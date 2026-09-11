"""
Latent state encoder module for VajraWorld.
Maps high-dimensional multi-modal telemetry S_t into a compact latent world state z_t.
"""
import torch
import torch.nn as nn

class LatentStateEncoder(nn.Module):
    def __init__(self, input_dim: int = 40, latent_dim: int = 64):
        super().__init__()
        self.encoder = nn.Sequential(
            nn.Linear(input_dim, 128),
            nn.LayerNorm(128),
            nn.GELU(),
            nn.Dropout(0.1),
            nn.Linear(128, latent_dim),
            nn.LayerNorm(latent_dim)
        )

    def forward(self, state_vector: torch.Tensor) -> torch.Tensor:
        """Projects state vector S_t -> z_t."""
        return self.encoder(state_vector)
