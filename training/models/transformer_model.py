"""
Temporal Transformer sequence model for multi-step cyber forecast benchmarking.
Uses self-attention over historical time windows to capture long-range progression dynamics.
"""
import torch
import torch.nn as nn

class TemporalTransformer(nn.Module):
    def __init__(self, input_dim: int = 40, d_model: int = 64, nhead: int = 4, num_layers: int = 2):
        super().__init__()
        self.input_proj = nn.Linear(input_dim, d_model)
        encoder_layer = nn.TransformerEncoderLayer(d_model=d_model, nhead=nhead, dim_feedforward=128, batch_first=True)
        self.transformer = nn.TransformerEncoder(encoder_layer, num_layers=num_layers)
        self.head = nn.Sequential(
            nn.Linear(d_model, 32),
            nn.ReLU(),
            nn.Linear(32, 1),
            nn.Sigmoid()
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """x: [batch, seq_len, input_dim]"""
        h = self.input_proj(x)
        out = self.transformer(h)
        return self.head(out[:, -1, :])
