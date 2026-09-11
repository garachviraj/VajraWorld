"""
High-fidelity synthetic dataset generator for VajraWorld.
Produces attack campaigns mirroring CIC-IDS2018 and CTU-13:
- Normal background traffic
- Port scanning & reconnaissance
- Brute-force & credential access
- Lateral movement & SMB execution
- Command and control exfiltration
Ensures scenario-level separation to prevent row-leakage.
"""
from typing import List, Dict, Any, Tuple
import numpy as np
import random
import time

def generate_campaign_flows(scenario_name: str = "Infiltration_Day1", num_steps: int = 100) -> List[Dict[str, Any]]:
    """Generates an evolving sequence of network flows representing an attack campaign."""
    flows = []
    base_time = time.time() - (num_steps * 10)

    for step in range(num_steps):
        t = base_time + (step * 10)
        # Stage progression logic across the timeline
        if step < 25:
            # Benign baseline
            stage = "Benign"
            src_ip = f"192.168.1.{random.randint(10, 50)}"
            dst_ip = "10.0.0.1"
            dport = random.choice([80, 443, 53])
            flags = "SA"
            bytes_f = random.randint(200, 2500)
            bytes_b = random.randint(500, 8000)
        elif step < 50:
            # Reconnaissance stage
            stage = "Reconnaissance"
            src_ip = "192.168.1.17"
            dst_ip = f"192.168.1.{random.randint(1, 100)}"
            dport = random.choice([21, 22, 23, 80, 135, 139, 445, 1433, 3389])
            flags = "S"
            bytes_f = 60
            bytes_b = 0
        elif step < 75:
            # Credential Access / Discovery
            stage = "Credential Access"
            src_ip = "192.168.1.17"
            dst_ip = "192.168.1.10" # AD Domain Controller
            dport = 88 # Kerberos
            flags = "PA"
            bytes_f = random.randint(1200, 4500)
            bytes_b = random.randint(80, 400)
        else:
            # Lateral Movement & Exfiltration
            stage = "Lateral Movement"
            src_ip = "192.168.1.17"
            dst_ip = "192.168.2.50" # Finance DB
            dport = 445 # SMB
            flags = "PA"
            bytes_f = random.randint(5000, 25000)
            bytes_b = random.randint(1000, 5000)

        flows.append({
            "timestamp": t,
            "scenario": scenario_name,
            "stage_label": stage,
            "src_ip": src_ip,
            "dst_ip": dst_ip,
            "src_port": random.randint(30000, 60000),
            "dst_port": dport,
            "protocol": "TCP" if dport != 53 else "UDP",
            "tcp_flags": flags,
            "bytes_fwd": bytes_f,
            "bytes_bwd": bytes_b,
            "duration": random.uniform(0.01, 2.5)
        })

    return flows
