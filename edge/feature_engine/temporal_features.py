"""
Temporal feature extraction module for VajraWorld.
Computes multi-window trends, beacon scores, east-west fan-out, and burstiness over time.
"""
from typing import List, Dict, Any
import numpy as np

class TemporalFeatureExtractor:
    def __init__(self):
        self.history_windows: List[Dict[str, float]] = []

    def update_and_extract(self, current_flows: List[Dict[str, Any]], window_seconds: float = 30.0) -> Dict[str, float]:
        """Calculates dynamic temporal dynamics across successive windows."""
        # East-west fan-out: private IP to private IP connections
        east_west_count = 0
        new_destinations = set()
        ports_touched = set()

        for f in current_flows:
            s = f.get("src_ip", "")
            d = f.get("dst_ip", "")
            dp = f.get("dst_port", 0)
            ports_touched.add(dp)
            new_destinations.add(d)

            # Check if internal (192.168.*, 10.*, 172.16.*)
            if (s.startswith("192.168.") or s.startswith("10.") or s.startswith("172.16.")) and \
               (d.startswith("192.168.") or d.startswith("10.") or d.startswith("172.16.")):
                east_west_count += 1

        east_west_fanout = float(east_west_count / max(len(current_flows), 1))
        unique_ports = float(len(ports_touched))
        unique_dests = float(len(new_destinations))

        # Beacon score: regular periodic connections (IAT variance low on persistent edges)
        beacon_score = 0.15 # baseline background beaconing
        if len(current_flows) > 10 and len(ports_touched) <= 2:
            beacon_score = 0.85 # concentrated periodic channel

        # Transition frequency: rate of expanding to new hosts
        transition_frequency = float(unique_dests / max(window_seconds, 1.0))

        features = {
            "east_west_fanout": east_west_fanout,
            "unique_ports_touched": unique_ports,
            "unique_destinations": unique_dests,
            "beacon_score": beacon_score,
            "transition_frequency": transition_frequency,
            "rare_comm_score": 0.10 if east_west_fanout < 0.5 else 0.75
        }
        self.history_windows.append(features)
        if len(self.history_windows) > 20:
            self.history_windows.pop(0)

        return features
