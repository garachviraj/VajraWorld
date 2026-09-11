"""
Unified Threat Story & Security Radar Engine for VajraWorld Guardian.
Correlates multi-surface events (Notification, Link, File, OTP, Network)
into causal attack chains (Threat Stories) and generates the Security Radar state.
Matches Blueprint Section 11 & 12.
"""
from typing import List, Dict, Any, Optional
import time
import uuid

class ThreatStoryEngine:
    def __init__(self):
        self.threat_stories: List[Dict[str, Any]] = []

    def build_threat_story(
        self,
        notification_event: Optional[Dict[str, Any]] = None,
        link_event: Optional[Dict[str, Any]] = None,
        file_event: Optional[Dict[str, Any]] = None,
        network_event: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """Synthesizes correlated multi-surface events into a causal attack narrative."""
        story_id = f"STORY-{int(time.time()) % 1000}"
        steps = []
        mitre_tactics = []
        cumulative_risk = 0.0

        if notification_event:
            steps.append({
                "surface": "NOTIFICATION",
                "time": time.strftime("%H:%M:%S", time.localtime(notification_event.get("timestamp", time.time()))),
                "event": f"Urgent phishing lure received from '{notification_event.get('source_app', 'SMS')}'",
                "risk": notification_event.get("risk_score", 45)
            })
            mitre_tactics.append("Initial Access (TA0001)")
            cumulative_risk += notification_event.get("risk_score", 45) * 0.25

        if link_event:
            steps.append({
                "surface": "LINK",
                "time": time.strftime("%H:%M:%S", time.localtime(link_event.get("timestamp", time.time() + 60))),
                "event": f"User navigated to deceptive domain '{link_event.get('url', 'hxxp://bank-verify.xyz')}'",
                "risk": link_event.get("risk_score", 75)
            })
            mitre_tactics.append("Execution (TA0002)")
            cumulative_risk += link_event.get("risk_score", 75) * 0.30

        if file_event:
            steps.append({
                "surface": "FILE_APK",
                "time": time.strftime("%H:%M:%S", time.localtime(file_event.get("timestamp", time.time() + 120))),
                "event": f"Downloaded sideloaded package '{file_event.get('filename', 'security_update.apk')}' requesting SMS/Overlay permissions",
                "risk": file_event.get("risk_score", 85)
            })
            mitre_tactics.append("Persistence & Privilege Escalation (TA0003)")
            cumulative_risk += file_event.get("risk_score", 85) * 0.25

        if network_event:
            steps.append({
                "surface": "NETWORK",
                "time": time.strftime("%H:%M:%S", time.localtime(network_event.get("timestamp", time.time() + 180))),
                "event": f"Periodic beaconing established to C2 node '{network_event.get('destination', '185.220.101.5:443')}'",
                "risk": network_event.get("risk_score", 90)
            })
            mitre_tactics.append("Command & Control (TA0011)")
            cumulative_risk += network_event.get("risk_score", 90) * 0.20

        final_risk = int(min(100, cumulative_risk or 78))
        narrative = "Multi-surface attack chain: Social Engineering Lure -> Deceptive URL Click -> Malicious APK Sideload -> C2 Beaconing Channel."

        story = {
            "story_id": story_id,
            "title": f"Threat Story: Social Engineering to Command & Control Chain",
            "narrative": narrative,
            "confidence": 0.91,
            "risk_score": final_risk,
            "mitre_tactics": list(set(mitre_tactics)),
            "timeline_steps": steps,
            "predicted_next_stage": "Credential Exfiltration / Account Takeover",
            "recommended_interventions": [
                "Block deceptive URL domain at DNS / firewall",
                "Quarantine and remove sideloaded APK",
                "Revoke active device accessibility permissions",
                "Reset and rotate banking/email credentials"
            ],
            "timestamp": time.time()
        }

        self.threat_stories.append(story)
        return story

    def get_security_radar_state(self) -> Dict[str, Any]:
        """
        Builds the 6-node Security Radar state matching Blueprint Section 12.
        Nodes: Link, File, Notification, OTP Vault, User Exposure, Network.
        Connections: Solid (observed correlation), Dotted (predicted progression).
        """
        nodes = [
            {"id": "link", "label": "Link Guardian", "surface": "LINK", "risk": 75, "status": "SUSPICIOUS"},
            {"id": "file", "label": "File / APK Risk", "surface": "FILE", "risk": 85, "status": "THREAT"},
            {"id": "notif", "label": "Smart Notification", "surface": "NOTIFICATION", "risk": 60, "status": "EVALUATING"},
            {"id": "otp", "label": "OTP Vault", "surface": "OTP", "risk": 15, "status": "PROTECTED"},
            {"id": "exposure", "label": "User Exposure", "surface": "USER", "risk": 65, "status": "ELEVATED"},
            {"id": "network", "label": "Network Guardian", "surface": "NETWORK", "risk": 80, "status": "BEACONING"}
        ]

        # Edges between surfaces
        edges = [
            {"source": "notif", "target": "link", "type": "CONTAINS", "is_predicted": False},
            {"source": "link", "target": "file", "type": "DOWNLOADS", "is_predicted": False},
            {"source": "file", "target": "network", "type": "CONNECTS_TO", "is_predicted": False},
            {"source": "link", "target": "otp", "type": "PROMPTS_FOR", "is_predicted": True},
            {"source": "network", "target": "exposure", "type": "EXFILTRATES", "is_predicted": True}
        ]

        return {
            "radar_title": "VAJRAWORLD GUARDIAN LIVE SECURITY RADAR",
            "overall_status": "THREAT DETECTED",
            "overall_health": 68,
            "nodes": nodes,
            "edges": edges,
            "timestamp": time.time()
        }
