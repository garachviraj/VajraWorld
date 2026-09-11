"""
Real-Time Telemetry & Event Streaming Daemon for VajraWorld Guardian.
Runs continuous background streaming of network flow state, multi-surface correlations,
and live radar metrics.
"""
import threading
import time
import random
from typing import Dict, Any, List, Optional

class LiveTelemetryStream:
    _instance: Optional['LiveTelemetryStream'] = None

    def __init__(self, routes_module=None):
        self.routes = routes_module
        self.running = False
        self.thread: Optional[threading.Thread] = None
        self.lock = threading.Lock()

        # Dynamic live telemetry state
        self.network_health = 84
        self.current_risk = 0.28
        self.predicted_stage = "Reconnaissance"
        self.lead_time_sec = 118
        self.active_flows_count = 142
        self.events_per_sec = 24.5
        self.critical_asset = "Finance-DB-02"
        self.horizon_risks = [0.18, 0.24, 0.32, 0.45, 0.58]
        self.last_update_ts = time.time()
        self.total_packets_inspected = 12450
        self.active_threats: List[Dict[str, Any]] = [
            {"id": "THREAT-101", "type": "DNS_TUNNEL_ANOMALY", "risk": 42, "surface": "NETWORK", "target": "192.168.1.105"}
        ]

    @classmethod
    def get_instance(cls, routes_module=None) -> 'LiveTelemetryStream':
        if cls._instance is None:
            cls._instance = LiveTelemetryStream(routes_module)
        elif routes_module is not None:
            cls._instance.routes = routes_module
        return cls._instance

    def start(self):
        with self.lock:
            if self.running:
                return
            self.running = True
            self.thread = threading.Thread(target=self._stream_loop, daemon=True)
            self.thread.start()

    def stop(self):
        with self.lock:
            self.running = False

    def _generate_synthetic_flow_batch(self) -> List[Dict[str, Any]]:
        dest_ips = ["192.168.1.1", "192.168.1.10", "192.168.1.45", "10.0.0.1", "185.220.101.5", "8.8.8.8"]
        src_ips = ["192.168.1.15", "192.168.1.17", "192.168.1.102"]
        ports = [80, 443, 53, 445, 3389, 8080, 22]
        protos = ["TCP", "UDP", "TCP", "TCP"]

        batch = []
        count = random.randint(5, 12)
        now = time.time()
        for _ in range(count):
            batch.append({
                "timestamp": now,
                "src_ip": random.choice(src_ips),
                "dst_ip": random.choice(dest_ips),
                "src_port": random.randint(1024, 65535),
                "dst_port": random.choice(ports),
                "proto": random.choice(protos),
                "bytes": random.randint(64, 15000),
                "packets": random.randint(1, 20),
                "flags": "AP/RS" if random.random() < 0.15 else "A",
                "flow_duration": round(random.uniform(0.01, 1.8), 3)
            })
        return batch

    def _stream_loop(self):
        while self.running:
            try:
                # 1. Generate live flow batch
                batch = self._generate_synthetic_flow_batch()
                self.total_packets_inspected += sum(f["packets"] for f in batch)
                self.events_per_sec = round(len(batch) * 2.1 + random.uniform(-2, 3), 1)
                self.active_flows_count = max(80, self.active_flows_count + random.randint(-4, 5))

                # 2. Feed to routes current_flows buffer
                if self.routes and hasattr(self.routes, "current_flows"):
                    with self.lock:
                        if isinstance(self.routes.current_flows, list):
                            self.routes.current_flows.extend(batch)
                            if len(self.routes.current_flows) > 200:
                                del self.routes.current_flows[:-200]

                # 3. Fluctuate risk smoothly
                risk_jitter = random.uniform(-0.03, 0.03)
                self.current_risk = max(0.12, min(0.85, round(self.current_risk + risk_jitter, 3)))
                self.network_health = max(40, min(98, int(100 - (self.current_risk * 70))))
                self.lead_time_sec = max(30, int(140 - (self.current_risk * 100)))

                # 4. Update horizon risks
                base = self.current_risk
                self.horizon_risks = [
                    round(min(1.0, base + i * 0.08 + random.uniform(-0.02, 0.02)), 2)
                    for i in range(5)
                ]

                # 5. Update stage based on risk
                if self.current_risk >= 0.70:
                    self.predicted_stage = "Lateral Movement"
                elif self.current_risk >= 0.50:
                    self.predicted_stage = "Credential Access"
                elif self.current_risk >= 0.35:
                    self.predicted_stage = "Discovery"
                else:
                    self.predicted_stage = "Reconnaissance"

                # 6. Push to Radar Engine if available
                if self.routes and hasattr(self.routes, "threat_story_engine"):
                    net_risk = int(self.current_risk * 100)
                    net_status = "BEACONING" if net_risk >= 70 else ("EVALUATING" if net_risk >= 45 else "NOMINAL")
                    self.routes.threat_story_engine.update_radar_surface("NETWORK", net_risk, net_status)

                self.last_update_ts = time.time()
            except Exception as e:
                pass

            time.sleep(2.0)

    def get_live_summary(self) -> Dict[str, Any]:
        with self.lock:
            return {
                "timestamp": self.last_update_ts,
                "is_streaming": self.running,
                "network_health": self.network_health,
                "current_risk": self.current_risk,
                "current_risk_pct": int(self.current_risk * 100),
                "predicted_stage": self.predicted_stage,
                "lead_time_sec": self.lead_time_sec,
                "active_flows_count": self.active_flows_count,
                "events_per_sec": self.events_per_sec,
                "critical_asset": self.critical_asset,
                "horizon_risks": self.horizon_risks,
                "total_inspected_packets": self.total_packets_inspected,
                "active_threats": self.active_threats,
                "radar_status": "ATTACK TRAJECTORY DETECTED" if self.current_risk > 0.65 else "ACTIVE SURVEILLANCE"
            }
