"""
Baseline models for intrusion and progression benchmarking.
Includes:
- Logistic Regression Baseline
- Random Forest Baseline
- LSTM Temporal Sequence Baseline
"""
import torch
import torch.nn as nn
import numpy as np

class LogisticRegressionBaseline:
    def __init__(self, input_dim: int = 40):
        self.weights = np.zeros(input_dim)
        self.bias = 0.0

    def fit(self, X: np.ndarray, y: np.ndarray, epochs: int = 50, lr: float = 0.05):
        for _ in range(epochs):
            preds = 1.0 / (1.0 + np.exp(-np.clip(X @ self.weights + self.bias, -10, 10)))
            err = preds - y
            grad_w = (X.T @ err) / len(y)
            grad_b = np.mean(err)
            self.weights -= lr * grad_w
            self.bias -= lr * grad_b

    def predict_proba(self, X: np.ndarray) -> np.ndarray:
        return 1.0 / (1.0 + np.exp(-np.clip(X @ self.weights + self.bias, -10, 10)))

class LSTMBaseline(nn.Module):
    def __init__(self, input_dim: int = 40, hidden_dim: int = 64, num_layers: int = 2):
        super().__init__()
        self.lstm = nn.LSTM(input_dim, hidden_dim, num_layers, batch_first=True)
        self.classifier = nn.Sequential(
            nn.Linear(hidden_dim, 32),
            nn.ReLU(),
            nn.Linear(32, 1),
            nn.Sigmoid()
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """x shape: [batch_size, seq_len, input_dim]"""
        lstm_out, _ = self.lstm(x)
        last_hidden = lstm_out[:, -1, :]
        return self.classifier(last_hidden)
