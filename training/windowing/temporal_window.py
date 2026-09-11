"""
Temporal windowing pipeline for VajraWorld.
Transforms raw packet/flow event streams into 5s bins and 30s rolling state windows
with multi-step future progression labels.
"""
from typing import List, Dict, Any, Tuple
import numpy as np
from edge.feature_engine.state_builder import StateBuilder
from edge.world_model.stage_head import ATTACK_STAGES

class TemporalWindowPipeline:
    def __init__(self, bin_seconds: float = 5.0, window_seconds: float = 30.0, horizon_steps: int = 4):
        self.bin_seconds = bin_seconds
        self.window_seconds = window_seconds
        self.horizon_steps = horizon_steps
        self.state_builder = StateBuilder()

    def process_flow_sequence(self, flows: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Splits flow stream into temporal rolling windows with future stage progression targets."""
        if not flows:
            return []

        sorted_flows = sorted(flows, key=lambda x: x["timestamp"])
        start_time = sorted_flows[0]["timestamp"]
        end_time = sorted_flows[-1]["timestamp"]

        step_size = self.bin_seconds
        window_size = self.window_seconds

        windows = []
        current_t = start_time

        while current_t + window_size <= end_time:
            w_flows = [f for f in sorted_flows if current_t <= f["timestamp"] < current_t + window_size]
            if w_flows:
                # Build state vector for this window
                s_vec, raw_feats = self.state_builder.build_state_vector(w_flows, window_seconds=window_size)
                # Determine current label
                labels = [f.get("stage_label", "Benign") for f in w_flows]
                current_stage = max(set(labels), key=labels.count)
                stage_idx = ATTACK_STAGES.index(current_stage) if current_stage in ATTACK_STAGES else 0

                # Determine future labels (progression targets)
                future_targets = []
                for h in range(1, self.horizon_steps + 1):
                    h_start = current_t + (h * window_size)
                    h_end = h_start + window_size
                    future_flows = [f for f in sorted_flows if h_start <= f["timestamp"] < h_end]
                    if future_flows:
                        flabels = [f.get("stage_label", "Benign") for f in future_flows]
                        fstage = max(set(flabels), key=flabels.count)
                        future_targets.append(1 if fstage != "Benign" else 0)
                    else:
                        future_targets.append(0)

                # Pad with 16 dims for graph embeddings -> total 40
                full_s_vec = np.pad(s_vec, (0, 16), mode='constant')

                windows.append({
                    "timestamp": current_t,
                    "state_vector": full_s_vec,
                    "stage_index": stage_idx,
                    "stage_name": current_stage,
                    "future_attack_targets": future_targets,
                    "is_attack_now": 1 if current_stage != "Benign" else 0
                })

            current_t += step_size

        return windows
