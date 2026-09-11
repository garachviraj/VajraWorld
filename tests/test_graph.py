import pytest
from edge.graph_engine.dynamic_graph import DynamicGraph
from edge.graph_engine.node_encoder import NodeEncoder

def test_dynamic_graph_building():
    g = DynamicGraph()
    flows = [
        {"src_ip": "192.168.1.17", "dst_ip": "192.168.1.10", "dst_port": 88, "protocol": "TCP", "tcp_flags": "PA", "bytes_bwd": 100},
        {"src_ip": "192.168.1.17", "dst_ip": "192.168.2.50", "dst_port": 445, "protocol": "TCP", "tcp_flags": "S", "bytes_bwd": 0}
    ]
    g.build_from_flows(flows)
    d = g.to_dict()
    assert d["node_count"] >= 3
    assert d["edge_count"] == 2
    
    # Check subgraph extraction
    sub = g.extract_subgraph("192.168.1.17")
    assert len(sub["nodes"]) >= 3

def test_graph_isolation():
    g = DynamicGraph()
    g.add_edge_event("Host-A", "Host-B", "CONNECTS_TO", port=80)
    g.add_edge_event("Host-C", "Host-A", "CONNECTS_TO", port=80)
    assert g.graph.number_of_edges() == 2
    g.remove_node_edges("Host-A")
    assert g.graph.number_of_edges() == 0

def test_node_encoder():
    g = DynamicGraph()
    g.add_edge_event("Host-A", "Host-B", "CONNECTS_TO", port=80)
    encoder = NodeEncoder()
    vec = encoder.encode_graph_features(g.graph)
    assert len(vec) == 16
