"""
Flow-level feature extraction module for VajraWorld.
Extracts volume, rate, entropy, fan-out, and flag distributions from raw flow records.
"""
from typing import List, Dict, Any
import numpy as np
from collections import Counter
import math

def calculate_entropy(values: List[Any]) -> float:
    if not values:
        return 0.0
    total = len(values)
    counts = Counter(values)
    ent = 0.0
    for count in counts.values():
        p = count / total
        if p > 0:
            ent -= p * math.log2(p)
    return float(ent)

class FlowFeatureExtractor:
    def extract_flow_features(self, flows: List[Dict[str, Any]]) -> Dict[str, float]:
        """Extracts statistical summary and distribution features across a window of flows."""
        if not flows:
            return {
                "flow_count": 0.0,
                "bytes_per_sec": 0.0,
                "packets_per_sec": 0.0,
                "bidi_ratio": 0.0,
                "dst_port_entropy": 0.0,
                "src_port_entropy": 0.0,
                "syn_ratio": 0.0,
                "rst_ratio": 0.0,
                "connection_fan_out": 0.0,
                "failed_connection_ratio": 0.0
            }

        total_bytes = sum(f.get("bytes_fwd", 0) + f.get("bytes_bwd", 0) for f in flows)
        total_pkts = sum(f.get("pkts_fwd", 0) + f.get("pkts_bwd", 0) for f in flows)
        durations = [f.get("duration", 0.0) for f in flows if f.get("duration", 0.0) > 0]
        mean_dur = float(np.mean(durations)) if durations else 1.0
        if mean_dur <= 0:
            mean_dur = 1.0

        bytes_fwd = sum(f.get("bytes_fwd", 0) for f in flows)
        bytes_bwd = sum(f.get("bytes_bwd", 0) for f in flows)
        bidi_ratio = float(bytes_bwd / bytes_fwd) if bytes_fwd > 0 else 0.0

        dst_ports = [f.get("dst_port", 0) for f in flows]
        src_ports = [f.get("src_port", 0) for f in flows]
        dst_port_entropy = calculate_entropy(dst_ports)
        src_port_entropy = calculate_entropy(src_ports)

        # Flag ratios
        flags = [str(f.get("tcp_flags", "")) for f in flows]
        syn_count = sum(1 for fl in flags if "S" in fl)
        rst_count = sum(1 for fl in flags if "R" in fl)
        total_flags = max(len(flags), 1)

        # Fan-out: unique destinations contacted per source
        src_dst_map = {}
        for f in flows:
            s = f.get("src_ip", "")
            d = f.get("dst_ip", "")
            if s not in src_dst_map:
                src_dst_map[s] = set()
            src_dst_map[s].add(d)
        max_fan_out = max((len(dsts) for dsts in src_dst_map.values()), default=0)

        # Failed connections proxy (RST received or zero backward bytes)
        failed_count = sum(1 for f in flows if f.get("bytes_bwd", 0) == 0 or "R" in str(f.get("tcp_flags", "")))

        return {
            "flow_count": float(len(flows)),
            "bytes_per_sec": float(total_bytes / mean_dur),
            "packets_per_sec": float(total_pkts / mean_dur),
            "bidi_ratio": float(np.clip(bidi_ratio, 0.0, 10.0)),
            "dst_port_entropy": dst_port_entropy,
            "src_port_entropy": src_port_entropy,
            "syn_ratio": float(syn_count / total_flags),
            "rst_ratio": float(rst_count / total_flags),
            "connection_fan_out": float(max_fan_out),
            "failed_connection_ratio": float(failed_count / total_flags)
        }
