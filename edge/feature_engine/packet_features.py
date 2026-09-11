"""
Packet-level feature extraction module for VajraWorld.
Extracts TTL variance, window size distribution, fragment flags, and inter-arrival timing.
"""
from typing import List, Dict, Any
import numpy as np

class PacketFeatureExtractor:
    def extract_packet_features(self, packet_events: List[Any]) -> Dict[str, float]:
        """Extracts statistical features from a list of PacketEvent objects or dicts."""
        if not packet_events:
            return {
                "ttl_mean": 64.0,
                "ttl_variance": 0.0,
                "window_size_mean": 65535.0,
                "fragment_ratio": 0.0,
                "retransmission_ratio": 0.0,
                "syn_burstiness": 0.0,
                "iat_mean_ms": 10.0,
                "iat_variance_ms": 0.0,
                "directional_asymmetry": 0.0
            }

        ttls = []
        windows = []
        frag_count = 0
        retrans_count = 0
        timestamps = []
        syn_count = 0

        for pkt in packet_events:
            if hasattr(pkt, "ttl"):
                ttls.append(pkt.ttl)
                windows.append(pkt.window_size)
                if pkt.is_fragment:
                    frag_count += 1
                if pkt.is_retransmission:
                    retrans_count += 1
                timestamps.append(pkt.timestamp)
                if "S" in pkt.tcp_flags:
                    syn_count += 1
            elif isinstance(pkt, dict):
                ttls.append(pkt.get("ttl", 64))
                windows.append(pkt.get("window_size", 65535))
                if pkt.get("is_fragment", False):
                    frag_count += 1
                if pkt.get("is_retransmission", False):
                    retrans_count += 1
                timestamps.append(pkt.get("timestamp", 0.0))
                if "S" in pkt.get("tcp_flags", ""):
                    syn_count += 1

        n = max(len(packet_events), 1)
        ttl_mean = float(np.mean(ttls)) if ttls else 64.0
        ttl_var = float(np.var(ttls)) if ttls else 0.0
        win_mean = float(np.mean(windows)) if windows else 65535.0

        # Calculate Inter-Arrival Time (IAT)
        iat_mean = 10.0
        iat_var = 0.0
        if len(timestamps) > 1:
            sorted_ts = sorted(timestamps)
            iats = [sorted_ts[i] - sorted_ts[i-1] for i in range(1, len(sorted_ts))]
            iat_mean = float(np.mean(iats) * 1000.0) # to ms
            iat_var = float(np.var(iats) * 1000000.0)

        # SYN burstiness score
        syn_burst = float(syn_count / (iat_mean + 1.0))

        return {
            "ttl_mean": ttl_mean,
            "ttl_variance": ttl_var,
            "window_size_mean": win_mean,
            "fragment_ratio": float(frag_count / n),
            "retransmission_ratio": float(retrans_count / n),
            "syn_burstiness": float(np.clip(syn_burst, 0.0, 50.0)),
            "iat_mean_ms": iat_mean,
            "iat_variance_ms": iat_var,
            "directional_asymmetry": 0.25 # baseline directional balance
        }
