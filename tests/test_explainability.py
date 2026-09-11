import pytest
from edge.explainability.layered_explainer import LayeredExplainer

def test_layered_explanation():
    explainer = LayeredExplainer()
    features = {"connection_fan_out": 31, "dst_port_entropy": 3.4}
    res = explainer.generate_explanation(
        forecast_id="fc_test_01",
        current_risk=0.81,
        predicted_stage="Lateral Movement",
        features=features,
        center_node="Host-17"
    )

    assert "level1_narrative" in res
    assert len(res["level2_feature_attribution"]) >= 5
    assert len(res["level3_temporal_evidence"]) >= 5
    assert res["level4_graph_evidence"]["center_node"] == "Host-17"
    assert "level5_uncertainty" in res
