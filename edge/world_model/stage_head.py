"""
ATT&CK-aligned stage taxonomy and probability head for VajraWorld.
Maps latent world state z_t to cyber adversary progression stages.
"""
import torch
import torch.nn as nn
from typing import List

ATTACK_STAGES = [
    "Benign",
    "Reconnaissance",
    "Initial Access",
    "Discovery",
    "Credential Access",
    "Lateral Movement",
    "Command and Control",
    "Collection",
    "Exfiltration",
    "Impact"
]

class StageHead(nn.Module):
    def __init__(self, latent_dim: int = 64, num_stages: int = len(ATTACK_STAGES)):
        super().__init__()
        self.classifier = nn.Sequential(
            nn.Linear(latent_dim, 32),
            nn.LayerNorm(32),
            nn.GELU(),
            nn.Dropout(0.1),
            nn.Linear(32, num_stages)
        )

    def forward(self, z: torch.Tensor) -> torch.Tensor:
        """Returns logits for the 10 ATT&CK progression stages."""
        return self.classifier(z)
