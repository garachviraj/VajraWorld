import pytest
import numpy as np
from edge.feature_engine.flow_features import FlowFeatureExtractor
from edge.feature_engine.packet_features import PacketFeatureExtractor
from edge.feature_engine.temporal_features import TemporalFeatureExtractor
from edge.feature_engine.state_builder import StateBuilder, FEATURE_NAMES

def test_flow_features_extraction():
    extractor = FlowFeatureExtractor()
    sample_flows = [
        {"src_ip": "192.168.1.10", "dst_ip": "10.0.0.1", "src_port": 5000, "dst_port": 80, "tcp_flags": "S", "bytes_fwd": 60, "bytes_bwd": 0, "duration": 0.5},
        {"src_ip": "192.168.1.10", "dst_ip": "10.0.0.2", "src_port": 5001, "dst_port": 443, "tcp_flags": "SA", "bytes_fwd": 120, "bytes_bwd": 300, "duration": 1.0}
    ]
    features = extractor.extract_flow_features(sample_flows)
    assert features["flow_count"] == 2.0
    assert features["connection_fan_out"] == 2.0
    assert "dst_port_entropy" in features

def test_packet_features_extraction():
    extractor = PacketFeatureExtractor()
    sample_packets = [
        {"ttl": 64, "window_size": 65535, "is_fragment": False, "is_retransmission": False, "timestamp": 100.0, "tcp_flags": "S"},
        {"ttl": 64, "window_size": 32768, "is_fragment": False, "is_retransmission": True, "timestamp": 100.05, "tcp_flags": "A"}
    ]
    features = extractor.extract_packet_features(sample_packets)
    assert features["ttl_mean"] == 64.0
    assert features["retransmission_ratio"] == 0.5
    assert features["syn_burstiness"] >= 0.0

def test_state_builder():
    builder = StateBuilder()
    sample_flows = [{"src_ip": "192.168.1.10", "dst_ip": "10.0.0.1", "src_port": 5000, "dst_port": 80, "tcp_flags": "S", "bytes_fwd": 60, "bytes_bwd": 0, "duration": 0.5}]
    vector, raw = builder.build_state_vector(sample_flows)
    assert len(vector) == len(FEATURE_NAMES)
    assert vector.dtype == np.float32
