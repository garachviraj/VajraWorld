"""
Training pipeline for VajraWorld World Model.
Trains latent encoder, transition model, stage head, and reconstruction head
using CompositeWorldModelLoss on rolling temporal windows.
"""
import torch
import torch.optim as optim
import numpy as np
from training.datasets.synthetic_generator import generate_campaign_flows
from training.windowing.temporal_window import TemporalWindowPipeline
from edge.world_model.model import VajraWorldModel
from training.losses.composite_loss import CompositeWorldModelLoss

def train_world_model(epochs: int = 5, lr: float = 1e-3):
    print(f"[+] Generating training and validation campaigns...")
    flows_train = generate_campaign_flows(scenario_name="Train_Campaign_1", num_steps=200)
    pipeline = TemporalWindowPipeline(bin_seconds=5.0, window_seconds=30.0, horizon_steps=4)
    windows = pipeline.process_flow_sequence(flows_train)

    if len(windows) < 2:
        print("[!] Not enough windows generated.")
        return

    model = VajraWorldModel(input_dim=40, latent_dim=64)
    optimizer = optim.AdamW(model.parameters(), lr=lr, weight_decay=1e-4)
    criterion = CompositeWorldModelLoss()

    X = torch.tensor(np.array([w["state_vector"] for w in windows]), dtype=torch.float32)
    stages = torch.tensor([w["stage_index"] for w in windows], dtype=torch.long)
    future_attacks = torch.tensor([w["future_attack_targets"][0] if w["future_attack_targets"] else 0 for w in windows], dtype=torch.float32)

    print(f"[+] Training VajraWorld Model on {len(windows)} temporal windows across {epochs} epochs...")
    model.train()
    for epoch in range(1, epochs + 1):
        optimizer.zero_grad()
        out = model(X)
        
        # Target z_next is next step's latent representation
        with torch.no_grad():
            z_all = out["z"]
            target_z_next = torch.roll(z_all, -1, dims=0)

        losses = criterion(
            outputs=out,
            target_z_next=target_z_next,
            target_stage=stages,
            target_future_attack=future_attacks,
            target_state=X
        )

        losses["loss_total"].backward()
        optimizer.step()

        print(f"    Epoch {epoch}/{epochs} | Loss Total: {losses['loss_total']:.4f} | Stage: {losses['loss_stage']:.4f} | Trans: {losses['loss_transition']:.4f} | Brier: {losses['brier_score']:.4f}")

    print("[+] Model training completed successfully.")
    return model

if __name__ == "__main__":
    train_world_model()
