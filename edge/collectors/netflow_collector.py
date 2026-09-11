"""
NetFlow / IPFIX flow collector for VajraWorld.
Normalizes standard NetFlow v5/v9 and IPFIX records into unified flow dictionaries.
"""
from dataclasses import dataclass
from typing import Dict, Any, List, Optional
import time

@dataclass
class FlowRecord:
    flow_id: str
    timestamp: float
    src_ip: str
    dst_ip: str
    src_port: int
    dst_port: int
    protocol: str
    tcp_flags: str
    bytes_fwd: int
    bytes_bwd: int
    pkts_fwd: int
    pkts_bwd: int
    duration: float

class NetFlowCollector:
    def __init__(self):
        self.flow_cache: Dict[str, FlowRecord] = {}

    def ingest_netflow_record(self, raw_record: Dict[str, Any]) -> FlowRecord:
        """Normalizes heterogeneous flow inputs (NetFlow v5/v9, IPFIX) into FlowRecord."""
        src_ip = raw_record.get("ipv4_src_addr") or raw_record.get("src_ip", "0.0.0.0")
        dst_ip = raw_record.get("ipv4_dst_addr") or raw_record.get("dst_ip", "0.0.0.0")
        src_port = int(raw_record.get("l4_src_port") or raw_record.get("src_port", 0))
        dst_port = int(raw_record.get("l4_dst_port") or raw_record.get("dst_port", 0))
        proto_num = int(raw_record.get("protocol", 6))
        proto = "TCP" if proto_num == 6 else ("UDP" if proto_num == 17 else "OTHER")
        
        flow_id = f"{src_ip}:{src_port}->{dst_ip}:{dst_port}_{proto}"
        ts = float(raw_record.get("timestamp", time.time()))
        bytes_fwd = int(raw_record.get("in_bytes", 0))
        bytes_bwd = int(raw_record.get("out_bytes", 0))
        pkts_fwd = int(raw_record.get("in_pkts", 1))
        pkts_bwd = int(raw_record.get("out_pkts", 0))
        duration = float(raw_record.get("duration", 0.0))
        tcp_flags = str(raw_record.get("tcp_flags", ""))

        record = FlowRecord(
            flow_id=flow_id,
            timestamp=ts,
            src_ip=src_ip,
            dst_ip=dst_ip,
            src_port=src_port,
            dst_port=dst_port,
            protocol=proto,
            tcp_flags=tcp_flags,
            bytes_fwd=bytes_fwd,
            bytes_bwd=bytes_bwd,
            pkts_fwd=pkts_fwd,
            pkts_bwd=pkts_bwd,
            duration=duration
        )
        self.flow_cache[flow_id] = record
        return record

    def get_active_flows(self) -> List[FlowRecord]:
        return list(self.flow_cache.values())
