"""
Log collector for Zeek conn.log/dns.log and Suricata EVE JSON records.
Extracts network telemetry, alert events, and DNS query context.
"""
from typing import Dict, Any, List, Optional
import json
import time

class LogCollector:
    def __init__(self):
        self.suricata_alerts: List[Dict[str, Any]] = []
        self.zeek_conns: List[Dict[str, Any]] = []

    def ingest_suricata_eve(self, eve_line: str) -> Optional[Dict[str, Any]]:
        """Parses a single Suricata EVE JSON line."""
        try:
            record = json.loads(eve_line.strip())
            event_type = record.get("event_type")
            if event_type == "alert":
                alert_data = {
                    "timestamp": record.get("timestamp"),
                    "src_ip": record.get("src_ip"),
                    "dst_ip": record.get("dest_ip"),
                    "src_port": record.get("src_port"),
                    "dst_port": record.get("dest_port"),
                    "proto": record.get("proto"),
                    "signature": record.get("alert", {}).get("signature", "Unknown"),
                    "category": record.get("alert", {}).get("category", "General"),
                    "severity": record.get("alert", {}).get("severity", 3)
                }
                self.suricata_alerts.append(alert_data)
                return alert_data
            elif event_type == "flow":
                return {
                    "timestamp": record.get("timestamp"),
                    "src_ip": record.get("src_ip"),
                    "dst_ip": record.get("dest_ip"),
                    "bytes_toclient": record.get("flow", {}).get("bytes_toclient", 0),
                    "bytes_toserver": record.get("flow", {}).get("bytes_toserver", 0)
                }
        except Exception:
            return None
        return None

    def ingest_zeek_conn(self, zeek_line: str) -> Optional[Dict[str, Any]]:
        """Parses a Zeek conn.log line (tab-separated or JSON)."""
        line = zeek_line.strip()
        if not line or line.startswith("#"):
            return None
        
        if line.startswith("{"):
            try:
                record = json.loads(line)
                self.zeek_conns.append(record)
                return record
            except Exception:
                return None
        
        parts = line.split("\t")
        if len(parts) >= 10:
            record = {
                "ts": parts[0],
                "uid": parts[1],
                "src_ip": parts[2],
                "src_port": int(parts[3]) if parts[3].isdigit() else 0,
                "dst_ip": parts[4],
                "dst_port": int(parts[5]) if parts[5].isdigit() else 0,
                "proto": parts[6],
                "service": parts[7],
                "duration": float(parts[8]) if parts[8] != "-" else 0.0,
                "orig_bytes": int(parts[9]) if parts[9] != "-" and parts[9].isdigit() else 0
            }
            self.zeek_conns.append(record)
            return record
        return None
