"""
Decision and action policy engine for VajraWorld.
Enforces CII/OT safety guardrails, environment profiles, and action ranking:
1. Observe -> 2. Confirm -> 3. Contain with reversible action -> 4. Escalate -> 5. Destructive action with human approval.
"""
from typing import Dict, Any, List

ENVIRONMENT_PROFILES = [
    "Enterprise IT",
    "Data Center",
    "Cloud / Hybrid",
    "IoT",
    "OT / ICS",
    "Critical Infrastructure",
    "Campus / SME"
]

class ActionPolicyEngine:
    def __init__(self, profile: str = "Enterprise IT"):
        self.profile = profile

    def evaluate_recommended_action(
        self,
        risk: float,
        predicted_stage: str,
        target_asset_criticality: str = "Medium",
        is_ot_environment: bool = False
    ) -> Dict[str, Any]:
        """
        Determines the safest optimal recommendation according to defense policies and environment profile.
        """
        # In OT/ICS environments: strict safety guardrail - no automated disruption
        if is_ot_environment or self.profile == "OT / ICS":
            return {
                "action": "SEGMENT_SUBNET",
                "mode": "POLICY_APPROVAL_REQUIRED",
                "reversible": True,
                "summary": "OT Safety Guardrail active: Verify with plant operator before executing boundary segmentation.",
                "steps": ["Observe telemetry", "Verify sensor health", "Request human-in-the-loop authorization"],
                "disruption_risk": "High (Industrial process)"
            }

        # Enterprise IT action selection hierarchy
        if risk >= 0.70:
            if target_asset_criticality == "Critical":
                return {
                    "action": "ISOLATE_HOST",
                    "mode": "CONTAIN_REVERSIBLE",
                    "reversible": True,
                    "summary": f"High risk progression toward critical asset. Recommend isolating source host immediately.",
                    "steps": ["Revoke active Kerberos sessions", "Isolate source MAC/IP on switch", "Capture memory snapshot"],
                    "disruption_risk": "Medium"
                }
            else:
                return {
                    "action": "BLOCK_PORT",
                    "mode": "CONTAIN_REVERSIBLE",
                    "reversible": True,
                    "summary": f"Probing detected. Block destination port 445 on perimeter firewalls.",
                    "steps": ["Deploy temporary ACL", "Monitor residual telemetry"],
                    "disruption_risk": "Low-Medium"
                }
        elif risk >= 0.40:
            return {
                "action": "ENHANCE_MONITORING",
                "mode": "CONFIRM",
                "reversible": True,
                "summary": "Suspicious reconnaissance trajectory. Initiate detailed PCAP buffering and notify SOC team.",
                "steps": ["Enable deep packet capture for target subnet", "Query endpoint EDR logs"],
                "disruption_risk": "None"
            }
        else:
            return {
                "action": "OBSERVE",
                "mode": "OBSERVE",
                "reversible": True,
                "summary": "Normal baseline state. Continue continuous world model forecasting.",
                "steps": ["Maintain rolling state window"],
                "disruption_risk": "None"
            }
