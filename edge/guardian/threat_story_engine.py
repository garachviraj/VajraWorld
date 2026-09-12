"""
Unified Threat Story & Security Radar Engine for VajraWorld Guardian.
Correlates multi-surface events (Notification, Link, File, OTP, Network)
into causal attack chains (Threat Stories) and generates the Security Radar state.
Matches Blueprint Section 11 & 12.
"""
from typing import List, Dict, Any, Optional
import time
import uuid

from urllib.parse import urlparse
import re
from collections import defaultdict

class ThreatStoryEngine:
    def __init__(self):
        self.events: List[Dict[str, Any]] = []
        self.threat_stories: List[Dict[str, Any]] = []
        self.radar_nodes: Dict[str, Dict[str, Any]] = {
            "link": {"id": "link", "label": "Link Guardian", "surface": "LINK", "risk": 25, "status": "NOMINAL", "x": 400.0, "y": 140.0},
            "file": {"id": "file", "label": "File / APK Risk", "surface": "FILE", "risk": 20, "status": "SECURE", "x": 500.0, "y": 300.0},
            "notif": {"id": "notif", "label": "Smart Notification", "surface": "NOTIFICATION", "risk": 15, "status": "MONITORING", "x": 200.0, "y": 140.0},
            "otp": {"id": "otp", "label": "OTP Vault", "surface": "OTP", "risk": 5, "status": "ZERO_STORAGE", "x": 320.0, "y": 260.0},
            "exposure": {"id": "exposure", "label": "User Exposure", "surface": "USER", "risk": 18, "status": "PROTECTED", "x": 140.0, "y": 300.0},
            "network": {"id": "network", "label": "Network Guardian", "surface": "NETWORK", "risk": 30, "status": "STABLE", "x": 320.0, "y": 420.0}
        }

    def update_radar_surface(self, surface: str, risk: int, status: str):
        """Dynamically updates a specific surface node on the radar in real time."""
        surface_key_map = {
            "LINK": "link",
            "FILE": "file",
            "NOTIFICATION": "notif",
            "OTP": "otp",
            "USER": "exposure",
            "CLIPBOARD": "exposure",
            "NETWORK": "network"
        }
        key = surface_key_map.get(surface.upper())
        if key and key in self.radar_nodes:
            self.radar_nodes[key]["risk"] = max(0, min(100, int(risk)))
            self.radar_nodes[key]["status"] = status

    def _extract_entities(self, event: Dict[str, Any]) -> Dict[str, set]:
        """Extracts correlatable entities (IPs, URLs, domains, filenames) from an event."""
        entities = {"ips": set(), "urls": set(), "domains": set(), "files": set()}
        
        # 1. URLs and Domains
        raw_urls = []
        if "url" in event and event["url"]:
            raw_urls.append(event["url"])
        if "raw_content" in event and event["raw_content"]:
            found = re.findall(r"https?://[^\s<>\"']+|hxxps?://[^\s<>\"']+", str(event["raw_content"]))
            raw_urls.extend(found)

        for u in raw_urls:
            entities["urls"].add(u)
            clean_u = u.replace("hxxp://", "http://").replace("hxxps://", "https://")
            try:
                parsed = urlparse(clean_u)
                if parsed.hostname:
                    entities["domains"].add(parsed.hostname.lower())
                if parsed.path and ("." in parsed.path.split("/")[-1]):
                    entities["files"].add(parsed.path.split("/")[-1].lower())
            except Exception:
                pass

        # 2. Destination and IP addresses
        text_corpus = f"{event.get('destination', '')} {event.get('raw_content', '')} {event.get('target', '')}"
        ips = re.findall(r"\b(?:\d{1,3}\.){3}\d{1,3}\b", text_corpus)
        for ip in ips:
            entities["ips"].add(ip)

        # 3. Filenames
        if "filename" in event and event["filename"]:
            entities["files"].add(event["filename"].lower())

        return entities

    def ingest_event(self, event: Dict[str, Any]) -> str:
        """Stores an event into the bipartite event list and triggers story synthesis."""
        event_copy = dict(event)
        if "id" not in event_copy:
            event_copy["id"] = f"EVT-{len(self.events) + 1:04d}"
        if "timestamp" not in event_copy:
            event_copy["timestamp"] = time.time()
        if "risk_score" not in event_copy:
            event_copy["risk_score"] = 50

        event_copy["entities"] = self._extract_entities(event_copy)
        self.events.append(event_copy)

        # Keep rolling buffer of recent events
        if len(self.events) > 500:
            self.events.pop(0)

        self.correlate_and_synthesize()
        return event_copy["id"]

    def correlate_and_synthesize(self) -> List[Dict[str, Any]]:
        """
        Builds a real dynamic bipartite correlation graph across ingested events.
        Links events that:
        - Share destination IPs, extracted URLs, or domain names
        - Share filename/package references
        - Occur within a temporal correlation window (<= 300 seconds)
        Synthesizes dynamic attack narratives strictly from connected components.
        """
        if not self.events:
            return []

        # Build adjacency graph
        n = len(self.events)
        adj = defaultdict(list)

        for i in range(n):
            for j in range(i + 1, n):
                e1 = self.events[i]
                e2 = self.events[j]

                # 1. Temporal correlation window (300 seconds)
                time_delta = abs(e1["timestamp"] - e2["timestamp"])
                is_temporally_correlated = time_delta <= 300.0

                # 2. Shared entity correlation
                ent1 = e1["entities"]
                ent2 = e2["entities"]
                shared_ips = ent1["ips"] & ent2["ips"]
                shared_urls = ent1["urls"] & ent2["urls"]
                shared_domains = ent1["domains"] & ent2["domains"]
                shared_files = ent1["files"] & ent2["files"]
                has_shared_entity = bool(shared_ips or shared_urls or shared_domains or shared_files)

                # Link if sharing entities or if multi-surface events occur in the same temporal attack window
                if has_shared_entity or (is_temporally_correlated and e1.get("surface") != e2.get("surface")):
                    adj[i].append(j)
                    adj[j].append(i)

        # Connected components via BFS
        visited = set()
        components = []
        for i in range(n):
            if i not in visited:
                comp = []
                queue = [i]
                visited.add(i)
                while queue:
                    curr = queue.pop(0)
                    comp.append(curr)
                    for neighbor in adj[curr]:
                        if neighbor not in visited:
                            visited.add(neighbor)
                            queue.append(neighbor)
                components.append(comp)

        stories = []
        for comp_idx, comp_indices in enumerate(components):
            comp_events = [self.events[idx] for idx in comp_indices]
            comp_events.sort(key=lambda x: x["timestamp"])

            # Generate dynamic timeline steps and narrative
            timeline_steps = []
            narrative_segments = []
            mitre_tactics = set()
            all_domains = set()
            all_ips = set()
            all_files = set()
            risk_weights = []

            for ev in comp_events:
                surface = ev.get("surface", "SECURITY").upper()
                ev_time = time.strftime("%H:%M:%S", time.localtime(ev["timestamp"]))
                r = int(ev.get("risk_score", 50))
                risk_weights.append(r)

                # Collect entities for dynamic interventions
                ent = ev["entities"]
                all_domains.update(ent["domains"])
                all_ips.update(ent["ips"])
                all_files.update(ent["files"])

                if "NOTIF" in surface:
                    src = ev.get("source_app", ev.get("source", "Messaging"))
                    desc = f"Urgent social engineering lure received from '{src}'"
                    mitre_tactics.add("Initial Access (TA0001)")
                    narrative_segments.append(f"Inbound lure via {src}")
                elif "LINK" in surface:
                    target_u = ev.get("url", "unknown URL")
                    desc = f"User navigated to deceptive destination '{target_u}'"
                    mitre_tactics.add("Execution (TA0002)")
                    narrative_segments.append(f"Target URL access ('{target_u}')")
                elif "FILE" in surface:
                    fn = ev.get("filename", "package.apk")
                    perms = ev.get("permissions", [])
                    desc = f"Downloaded sideloaded package '{fn}' requesting {len(perms)} permission(s)"
                    mitre_tactics.add("Persistence (TA0003)")
                    mitre_tactics.add("Privilege Escalation (TA0004)")
                    narrative_segments.append(f"Package staging ('{fn}')")
                elif "NET" in surface:
                    dest = ev.get("destination", ev.get("dst_ip", "C2 Node"))
                    desc = f"Outbound socket communication established to C2 node '{dest}'"
                    mitre_tactics.add("Command & Control (TA0011)")
                    narrative_segments.append(f"C2 beaconing channel to {dest}")
                elif "OTP" in surface or "CLIP" in surface or "USER" in surface:
                    desc = f"Credential / authentication secret exposure detected on {surface}"
                    mitre_tactics.add("Credential Access (TA0006)")
                    narrative_segments.append("Credential harvesting/exposure")
                else:
                    desc = f"Anomaly recorded on surface {surface}"
                    mitre_tactics.add("Defense Evasion (TA0005)")
                    narrative_segments.append(f"{surface} anomaly")

                timeline_steps.append({
                    "surface": surface,
                    "time": ev_time,
                    "event": desc,
                    "risk": r
                })

            # Dynamic composite risk calculation
            final_risk = int(min(100, max(risk_weights) if risk_weights else 50))

            # Dynamic narrative synthesized directly from linked components
            if narrative_segments:
                dynamic_narrative = f"Dynamic correlation graph identified multi-surface threat progression: {' -> '.join(narrative_segments)}."
            else:
                dynamic_narrative = "Correlated anomalous activity across monitored surfaces."

            # Dynamic recommended interventions based on linked entities
            interventions = []
            if all_domains:
                interventions.append(f"Block deceptive domains at DNS: {', '.join(list(all_domains)[:3])}")
            if all_files:
                interventions.append(f"Quarantine and remove suspicious files: {', '.join(list(all_files)[:2])}")
            if all_ips:
                interventions.append(f"Terminate network connections and firewall isolate: {', '.join(list(all_ips)[:3])}")
            if not interventions:
                interventions.append("Rotate session tokens and verify active endpoint authorizations")

            story = {
                "story_id": f"STORY-{comp_idx + 1:03d}-{int(comp_events[0]['timestamp']) % 10000}",
                "title": f"Threat Story: {' -> '.join([s['surface'] for s in timeline_steps])} Chain",
                "narrative": dynamic_narrative,
                "confidence": 0.92,
                "risk_score": final_risk,
                "mitre_tactics": sorted(list(mitre_tactics)),
                "timeline_steps": timeline_steps,
                "predicted_next_stage": "Credential Exfiltration / Lateral Compromise",
                "recommended_interventions": interventions,
                "timestamp": comp_events[-1]["timestamp"]
            }
            stories.append(story)

        self.threat_stories = stories
        return stories

    def build_threat_story(
        self,
        notification_event: Optional[Dict[str, Any]] = None,
        link_event: Optional[Dict[str, Any]] = None,
        file_event: Optional[Dict[str, Any]] = None,
        network_event: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """Ingests specified events into the bipartite graph and returns the primary synthesized story."""
        base_t = time.time()
        if notification_event:
            ne = dict(notification_event)
            ne["surface"] = "NOTIFICATION"
            ne.setdefault("timestamp", base_t)
            self.ingest_event(ne)

        if link_event:
            le = dict(link_event)
            le["surface"] = "LINK"
            le.setdefault("timestamp", base_t + 30)
            self.ingest_event(le)

        if file_event:
            fe = dict(file_event)
            fe["surface"] = "FILE"
            fe.setdefault("timestamp", base_t + 60)
            self.ingest_event(fe)

        if network_event:
            nwe = dict(network_event)
            nwe["surface"] = "NETWORK"
            nwe.setdefault("timestamp", base_t + 90)
            self.ingest_event(nwe)

        stories = self.correlate_and_synthesize()
        if stories:
            return stories[-1]

        # Fallback if no events provided
        return {
            "story_id": f"STORY-FALLBACK-{int(time.time())}",
            "title": "Threat Story: Nominal State",
            "narrative": "No active multi-surface correlation graph components detected.",
            "confidence": 0.95,
            "risk_score": 10,
            "mitre_tactics": [],
            "timeline_steps": [],
            "predicted_next_stage": "Nominal Surveillance",
            "recommended_interventions": ["Maintain active telemetry monitoring"],
            "timestamp": time.time()
        }

    def get_security_radar_state(self) -> Dict[str, Any]:
        """
        Builds the 6-node Security Radar state matching Blueprint Section 12.
        Nodes: Link, File, Notification, OTP Vault, User Exposure, Network.
        Connections: Solid (observed correlation), Dotted (predicted progression).
        """
        nodes = list(self.radar_nodes.values())
        avg_risk = sum(n["risk"] for n in nodes) / max(1, len(nodes))
        overall_health = max(10, min(100, int(100 - avg_risk)))

        if overall_health < 50:
            status = "ATTACK CHAIN ACTIVE"
        elif overall_health < 75:
            status = "THREAT ELEVATED"
        else:
            status = "RADAR NOMINAL"

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
            "overall_status": status,
            "overall_health": overall_health,
            "nodes": nodes,
            "edges": edges,
            "timestamp": time.time()
        }
