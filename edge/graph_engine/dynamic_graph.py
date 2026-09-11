"""
Dynamic entity graph builder for VajraWorld.
Maintains temporal topological states with typed nodes (Host, Server, Service, OT-Asset)
and directional typed edges (CONNECTS_TO, SCANS, ACCESSES, AUTHENTICATES_TO).
"""
from typing import Dict, Any, List, Set, Tuple, Optional
import networkx as nx
import time

NODE_TYPES = ["Host", "Server", "User", "Service", "Subnet", "Domain", "External-IP", "Device", "OT-Asset"]
EDGE_TYPES = ["CONNECTS_TO", "AUTHENTICATES_TO", "RESOLVES_TO", "SCANS", "ACCESSES", "TRANSFERS_TO", "MEMBER_OF", "DEPENDS_ON"]

class DynamicGraph:
    def __init__(self):
        self.graph = nx.DiGraph()
        self.last_update = time.time()

    def add_or_update_node(self, node_id: str, node_type: str = "Host", criticality: str = "Medium", metadata: Optional[Dict[str, Any]] = None):
        meta = metadata or {}
        if not self.graph.has_node(node_id):
            self.graph.add_node(
                node_id,
                node_type=node_type,
                criticality=criticality,
                degree=0,
                risk_score=0.1,
                last_seen=time.time(),
                **meta
            )
        else:
            self.graph.nodes[node_id]["last_seen"] = time.time()
            if criticality != "Medium":
                self.graph.nodes[node_id]["criticality"] = criticality

    def add_edge_event(self, src: str, dst: str, edge_type: str = "CONNECTS_TO", weight: float = 1.0, port: int = 0, protocol: str = "TCP"):
        self.add_or_update_node(src)
        self.add_or_update_node(dst)

        if self.graph.has_edge(src, dst):
            self.graph[src][dst]["weight"] += weight
            self.graph[src][dst]["last_seen"] = time.time()
            self.graph[src][dst]["count"] = self.graph[src][dst].get("count", 1) + 1
        else:
            self.graph.add_edge(
                src,
                dst,
                edge_type=edge_type,
                weight=weight,
                port=port,
                protocol=protocol,
                count=1,
                last_seen=time.time()
            )
        self.last_update = time.time()

    def build_from_flows(self, flows: List[Dict[str, Any]]):
        for f in flows:
            src = f.get("src_ip", "0.0.0.0")
            dst = f.get("dst_ip", "0.0.0.0")
            port = f.get("dst_port", 0)
            proto = f.get("protocol", "TCP")
            flags = str(f.get("tcp_flags", ""))

            # Infer edge type
            edge_type = "CONNECTS_TO"
            if "S" in flags and ("R" in flags or f.get("bytes_bwd", 0) == 0):
                edge_type = "SCANS"
            elif port in (445, 139, 3389, 22):
                edge_type = "ACCESSES"
            elif port in (88, 389, 636):
                edge_type = "AUTHENTICATES_TO"

            self.add_edge_event(src, dst, edge_type=edge_type, weight=1.0, port=port, protocol=proto)

    def extract_subgraph(self, center_node: str, radius: int = 2) -> Dict[str, Any]:
        """Extracts minimal explanatory subgraph around a suspicious or critical entity."""
        if not self.graph.has_node(center_node):
            return {"nodes": [], "edges": []}

        sub = nx.ego_graph(self.graph, center_node, radius=radius, undirected=False)
        nodes = []
        for n, data in sub.nodes(data=True):
            nodes.append({
                "id": n,
                "label": n,
                "type": data.get("node_type", "Host"),
                "criticality": data.get("criticality", "Medium"),
                "risk_score": data.get("risk_score", 0.1)
            })

        edges = []
        for u, v, data in sub.edges(data=True):
            edges.append({
                "source": u,
                "target": v,
                "type": data.get("edge_type", "CONNECTS_TO"),
                "weight": data.get("weight", 1.0),
                "port": data.get("port", 0)
            })

        return {"nodes": nodes, "edges": edges}

    def to_dict(self) -> Dict[str, Any]:
        """Serializes the entire graph topology for Android UI visualization."""
        nodes = []
        for n, data in self.graph.nodes(data=True):
            nodes.append({
                "id": n,
                "label": n,
                "type": data.get("node_type", "Host"),
                "criticality": data.get("criticality", "Medium"),
                "risk_score": data.get("risk_score", 0.1)
            })
        edges = []
        for u, v, data in self.graph.edges(data=True):
            edges.append({
                "source": u,
                "target": v,
                "type": data.get("edge_type", "CONNECTS_TO"),
                "weight": data.get("weight", 1.0),
                "port": data.get("port", 0)
            })
        return {
            "node_count": len(nodes),
            "edge_count": len(edges),
            "nodes": nodes,
            "edges": edges,
            "timestamp": self.last_update
        }

    def remove_node_edges(self, node_id: str):
        """Simulates isolation of an asset by severing all incoming and outgoing connections."""
        if self.graph.has_node(node_id):
            in_edges = list(self.graph.in_edges(node_id))
            out_edges = list(self.graph.out_edges(node_id))
            self.graph.remove_edges_from(in_edges + out_edges)

    def block_port_edges(self, port: int):
        """Simulates firewall port block by severing edges targeting a port."""
        edges_to_remove = [
            (u, v) for u, v, d in self.graph.edges(data=True) if d.get("port") == port
        ]
        self.graph.remove_edges_from(edges_to_remove)
