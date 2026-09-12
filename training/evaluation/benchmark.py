"""
Benchmarking evaluation script for VajraWorld.
Compares baseline models (LogReg, LSTM, Temporal Transformer) against VajraWorld World Model
on authentic, measured metrics:
- F1 Score
- Brier Calibration Score
- Average Lead Time (seconds before confirmed lateral compromise)
"""
from typing import Dict, Any, List
import numpy as np
import torch
import torch.nn as nn
from training.datasets.synthetic_generator import generate_campaign_flows
from training.windowing.temporal_window import TemporalWindowPipeline
from training.models.baselines import LogisticRegressionBaseline, LSTMBaseline
from training.models.transformer_model import TemporalTransformer
from edge.world_model.model import VajraWorldModel
from training.losses.composite_loss import CompositeWorldModelLoss
from edge.storage.db import DatabaseManager

def compute_f1(preds: np.ndarray, targets: np.ndarray, threshold: float = 0.5) -> float:
    """Computes true binary F1 score without approximations."""
    binary_preds = (preds >= threshold).astype(int)
    tp = int(np.sum((binary_preds == 1) & (targets == 1)))
    fp = int(np.sum((binary_preds == 1) & (targets == 0)))
    fn = int(np.sum((binary_preds == 0) & (targets == 1)))
    precision = tp / (tp + fp) if (tp + fp) > 0 else 0.0
    recall = tp / (tp + fn) if (tp + fn) > 0 else 0.0
    if precision + recall == 0.0:
        return 0.0
    return float(2.0 * (precision * recall) / (precision + recall))

def compute_brier(preds: np.ndarray, targets: np.ndarray) -> float:
    """Computes genuine Brier calibration error."""
    return float(np.mean((preds - targets) ** 2))

def compute_empirical_lead_time(preds: np.ndarray, test_windows: List[Dict[str, Any]], threshold: float = 0.5) -> float:
    """Computes measured advance warning lead time (in seconds) before confirmed lateral compromise."""
    lateral_ts = [w["timestamp"] for w in test_windows if w.get("stage_name") == "Lateral Movement"]
    if not lateral_ts:
        return 0.0
    t_compromise = min(lateral_ts)
    lead_times = [
        t_compromise - w["timestamp"]
        for i, w in enumerate(test_windows)
        if w["timestamp"] < t_compromise and preds[i] >= threshold
    ]
    return float(np.mean(lead_times)) if lead_times else 0.0

def run_benchmark() -> Dict[str, Dict[str, float]]:
    print("=" * 70)
    print("VAJRAWORLD PREDICTIVE CYBER DEFENCE BENCHMARK (AUTHENTIC EVALUATION)")
    print("=" * 70)

    # 1. Generate campaign flows & extract temporal windows
    print("[*] Generating test campaign flows (150 steps)...")
    flows = generate_campaign_flows(scenario_name="Evaluation_Campaign", num_steps=150)
    pipeline = TemporalWindowPipeline(bin_seconds=5.0, window_seconds=30.0, horizon_steps=4)
    windows = pipeline.process_flow_sequence(flows)

    # Split windows into train (60%) and test (40%)
    split_idx = int(len(windows) * 0.6)
    train_windows = windows[:split_idx]
    test_windows = windows[split_idx:]
    print(f"[*] Extracted {len(windows)} temporal windows: {len(train_windows)} train (60%), {len(test_windows)} test (40%).")

    X_train = np.array([w["state_vector"] for w in train_windows], dtype=np.float32)
    y_train = np.array([w["future_attack_targets"][0] if w["future_attack_targets"] else w["is_attack_now"] for w in train_windows], dtype=np.float32)
    stages_train = np.array([w["stage_index"] for w in train_windows], dtype=np.int64)

    X_test = np.array([w["state_vector"] for w in test_windows], dtype=np.float32)
    y_test = np.array([w["future_attack_targets"][0] if w["future_attack_targets"] else w["is_attack_now"] for w in test_windows], dtype=np.float32)

    # 2. Evaluate Baseline 1: Logistic Regression
    print("[*] Training Baseline 1: Logistic Regression...")
    logreg = LogisticRegressionBaseline(input_dim=40)
    logreg.fit(X_train, y_train, epochs=100, lr=0.05)
    logreg_preds = logreg.predict_proba(X_test)
    logreg_f1 = compute_f1(logreg_preds, y_test)
    logreg_brier = compute_brier(logreg_preds, y_test)
    logreg_lead_time = compute_empirical_lead_time(logreg_preds, test_windows)

    # 3. Evaluate Baseline 2: LSTM
    print("[*] Training Baseline 2: LSTM Temporal Sequence Model...")
    lstm = LSTMBaseline(input_dim=40, hidden_dim=64, num_layers=2)
    opt_lstm = torch.optim.Adam(lstm.parameters(), lr=0.01)
    t_X_tr = torch.from_numpy(X_train).unsqueeze(1)
    t_y_tr = torch.from_numpy(y_train).unsqueeze(1)
    for _ in range(25):
        opt_lstm.zero_grad()
        loss_lstm = nn.functional.binary_cross_entropy(lstm(t_X_tr), t_y_tr)
        loss_lstm.backward()
        opt_lstm.step()

    lstm.eval()
    with torch.no_grad():
        lstm_preds = lstm(torch.from_numpy(X_test).unsqueeze(1)).squeeze(-1).numpy()
    lstm_f1 = compute_f1(lstm_preds, y_test)
    lstm_brier = compute_brier(lstm_preds, y_test)
    lstm_lead_time = compute_empirical_lead_time(lstm_preds, test_windows)

    # 4. Evaluate Baseline 3: Temporal Transformer
    print("[*] Training Baseline 3: Temporal Self-Attention Transformer...")
    transformer = TemporalTransformer(input_dim=40, d_model=64, nhead=4, num_layers=2)
    opt_trans = torch.optim.Adam(transformer.parameters(), lr=0.003)
    for _ in range(35):
        opt_trans.zero_grad()
        loss_trans = nn.functional.binary_cross_entropy(transformer(t_X_tr), t_y_tr)
        loss_trans.backward()
        opt_trans.step()

    transformer.eval()
    with torch.no_grad():
        transformer_preds = transformer(torch.from_numpy(X_test).unsqueeze(1)).squeeze(-1).numpy()
    transformer_f1 = compute_f1(transformer_preds, y_test)
    transformer_brier = compute_brier(transformer_preds, y_test)
    transformer_lead_time = compute_empirical_lead_time(transformer_preds, test_windows)

    # 5. Evaluate VajraWorld Hybrid World Model
    print("[*] Training VajraWorld Hybrid World Model with Composite Loss...")
    world_model = VajraWorldModel(input_dim=40, latent_dim=64)
    opt_wm = torch.optim.AdamW(world_model.parameters(), lr=0.005, weight_decay=1e-4)
    criterion = CompositeWorldModelLoss()

    world_model.train()
    for _ in range(15):
        opt_wm.zero_grad()
        out_wm = world_model(torch.from_numpy(X_train))
        target_z_next = torch.roll(out_wm["z"], -1, dims=0)
        loss_wm = criterion(
            outputs=out_wm,
            target_z_next=target_z_next,
            target_stage=torch.from_numpy(stages_train),
            target_future_attack=torch.from_numpy(y_train),
            target_state=torch.from_numpy(X_train)
        )
        loss_wm["loss_total"].backward()
        opt_wm.step()

    world_model.eval()
    with torch.no_grad():
        wm_out = world_model(torch.from_numpy(X_test))
        wm_risks = wm_out["risk"].numpy()

    wm_f1 = compute_f1(wm_risks, y_test)
    wm_brier = compute_brier(wm_risks, y_test)
    wm_lead_time = compute_empirical_lead_time(wm_risks, test_windows)

    results = {
        "Logistic Regression": {
            "F1_Score": round(logreg_f1, 3),
            "Brier_Score": round(logreg_brier, 3),
            "Lead_Time_Sec": round(logreg_lead_time, 1)
        },
        "LSTM Baseline": {
            "F1_Score": round(lstm_f1, 3),
            "Brier_Score": round(lstm_brier, 3),
            "Lead_Time_Sec": round(lstm_lead_time, 1)
        },
        "Temporal Transformer": {
            "F1_Score": round(transformer_f1, 3),
            "Brier_Score": round(transformer_brier, 3),
            "Lead_Time_Sec": round(transformer_lead_time, 1)
        },
        "VajraWorld World Model": {
            "F1_Score": round(wm_f1, 3),
            "Brier_Score": round(wm_brier, 3),
            "Lead_Time_Sec": round(wm_lead_time, 1)
        }
    }

    # Record measured metrics in database
    try:
        db = DatabaseManager()
        db.update_model_metrics(
            model_id="m-vw-hybrid-0.8.0",
            version="vw-0.8.0",
            accuracy=round(wm_f1, 3),
            brier_score=round(wm_brier, 3),
            lead_time_sec=round(wm_lead_time, 1),
            status="ACTIVE_BENCHMARKED"
        )
        print("[+] Authentic measured metrics recorded to edge database successfully.")
    except Exception as e:
        print(f"[!] Warning: Could not update database metrics: {e}")

    print("\n" + f"{'Model Architecture':<26} | {'F1 Score':<10} | {'Brier (Calib)':<14} | {'Lead Time (sec)':<15}")
    print("-" * 72)
    for model_name, metrics in results.items():
        print(f"{model_name:<26} | {metrics['F1_Score']:<10.3f} | {metrics['Brier_Score']:<14.3f} | {metrics['Lead_Time_Sec']:<15.1f}")
    print("=" * 72)
    print(f"[+] Dynamic Evaluation Complete: VajraWorld Model F1: {wm_f1:.3f}, Brier: {wm_brier:.3f}, Lead Time: {wm_lead_time:.1f}s")

    return results

if __name__ == "__main__":
    run_benchmark()
