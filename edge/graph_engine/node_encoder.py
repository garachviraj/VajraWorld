"""
Node and graph topology encoder.
Transforms graph structural properties (in/out degree, centrality, clustering)
into numeric embeddings for the world model.
"""
from typing import Dict, Any, List
import numpy as np
import networkx as nx

class NodeEncoder:
    def encode_graph_features(self, graph: nx.DiGraph) -> np.ndarray:
        """Computes a 16-dimensional summary vector representing the network topology."""
        if graph.number_of_nodes() == 0:
            return np.zeros(16, dtype=np.float32)

        num_nodes = graph.number_of_nodes()
        num_edges = graph.number_of_edges()
        density = float(nx.density(graph))

        # Degree statistics
        in_degrees = [d for _, d in graph.in_degree()]
        out_degrees = [d for _, d in graph.out_degree()]
        max_in_deg = max(in_degrees) if in_degrees else 0
        max_out_deg = max(out_degrees) if out_degrees else 0
        avg_deg = (num_edges / num_nodes) if num_nodes > 0 else 0.0

        # High criticality connectivity
        crit_count = sum(1 for _, d in graph.nodes(data=True) if d.get("criticality") == "Critical")

        vec = np.array([
            np.log1p(num_nodes) / 5.0,
            np.log1p(num_edges) / 6.0,
            density,
            np.log1p(avg_deg) / 3.0,
            np.log1p(max_in_deg) / 4.0,
            np.log1p(max_out_deg) / 4.0,
            crit_count / max(num_nodes, 1),
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        ], dtype=np.float32)

        return vec
