"""
State builder module for VajraWorld.
Transforms raw telemetry, flow features, packet features, and graph topology into
an integrated observable network state vector S_t.
"""
from typing import List, Dict, Any, Tuple
import numpy as np
from edge.feature_engine.flow_features import FlowFeatureExtractor
from edge.feature_engine.packet_features import PacketFeatureExtractor
from edge.feature_engine.temporal_features import TemporalFeatureExtractor

# State Vector schema: 24 normalized continuous features
FEATURE_NAMES = [
    "flow_count",
    "bytes_per_sec",
    "packets_per_sec",
    "bidi_ratio",
    "dst_port_entropy",
    "src_port_entropy",
    "syn_ratio",
    "rst_ratio",
    "connection_fan_out",
    "failed_connection_ratio",
    "ttl_mean",
    "ttl_variance",
    "window_size_mean",
    "fragment_ratio",
    "retransmission_ratio",
    "syn_burstiness",
    "iat_mean_ms",
    "iat_variance_ms",
    "directional_asymmetry",
    "east_west_fanout",
    "unique_ports_touched",
    "unique_destinations",
    "beacon_score",
    "transition_frequency"
]

class StateBuilder:
    def __init__(self):
        self.flow_extractor = FlowFeatureExtractor()
        self.packet_extractor = PacketFeatureExtractor()
        self.temporal_extractor = TemporalFeatureExtractor()

    def build_state_vector(
        self,
        flows: List[Dict[str, Any]],
        packets: List[Any] = None,
        window_seconds: float = 30.0
    ) -> Tuple[np.ndarray, Dict[str, float]]:
        """Constructs a normalized state vector S_t from telemetry streams."""
        packets = packets or []
        ff = self.flow_extractor.extract_flow_features(flows)
        pf = self.packet_extractor.extract_packet_features(packets)
        tf = self.temporal_extractor.update_and_extract(flows, window_seconds)

        merged = {}
        merged.update(ff)
        merged.update(pf)
        merged.update(tf)

        # Build ordered vector
        vector = np.zeros(len(FEATURE_NAMES), dtype=np.float32)
        for i, name in enumerate(FEATURE_NAMES):
            val = merged.get(name, 0.0)
            # Safe robust clipping & normalizations
            if name == "bytes_per_sec":
                val = np.log1p(val) / 15.0
            elif name == "packets_per_sec":
                val = np.log1p(val) / 10.0
            elif name == "ttl_mean":
                val = val / 128.0
            elif name == "window_size_mean":
                val = val / 65535.0
            elif name == "iat_mean_ms":
                val = np.log1p(val) / 10.0
            elif name in ("connection_fan_out", "unique_ports_touched", "unique_destinations"):
                val = np.log1p(val) / 5.0
            elif name == "flow_count":
                val = np.log1p(val) / 8.0
            
            vector[i] = float(np.clip(val, 0.0, 5.0))

        return vector, merged
