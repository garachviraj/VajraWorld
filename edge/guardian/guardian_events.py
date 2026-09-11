"""
Unified Security Event Schema & Normalizer for VajraWorld Guardian.
Matches Blueprint Section 24.
Normalizes cross-surface signals (Link, File, Notification, OTP, Clipboard, Network)
into a structured temporal state graph.
"""
from dataclasses import dataclass, field, asdict
from typing import List, Dict, Any, Optional
import hashlib
import time
import uuid

EVENT_TYPES = [
    "LINK_ANALYZED",
    "FILE_SCANNED",
    "NOTIFICATION_RISK",
    "OTP_EXPOSURE",
    "CLIPBOARD_CHECK",
    "NETWORK_ANOMALY",
    "FORECAST_CREATED",
    "ACTION_SIMULATED"
]

@dataclass
class SecurityEvent:
    id: str
    timestamp: float
    event_type: str
    source: str
    risk_score: int          # 0 to 100
    confidence: float        # 0.0 to 1.0
    privacy_level: str       # "METADATA_ONLY", "ANONYMIZED", "LOCAL_ONLY"
    correlation_id: str
    raw_data_hash: str       # SHA-256 hash of raw input (zero plaintext retention)
    explanation: str
    model_version: str = "vw-guardian-0.9.0"
    metadata: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)

class EventNormalizer:
    def __init__(self):
        self.event_log: List[SecurityEvent] = []

    def normalize(
        self,
        event_type: str,
        source: str,
        risk_score: int,
        confidence: float,
        explanation: str,
        raw_content: Optional[str] = None,
        correlation_id: Optional[str] = None,
        metadata: Optional[Dict[str, Any]] = None
    ) -> SecurityEvent:
        """
        Creates an anonymized SecurityEvent.
        Strict Privacy: Never stores raw_content. Stores only its SHA-256 digest.
        """
        raw_hash = ""
        if raw_content:
            raw_hash = hashlib.sha256(raw_content.encode("utf-8")).hexdigest()
        
        event = SecurityEvent(
            id=f"evt_{int(time.time())}_{str(uuid.uuid4())[:6]}",
            timestamp=time.time(),
            event_type=event_type,
            source=source,
            risk_score=max(0, min(100, risk_score)),
            confidence=round(confidence, 2),
            privacy_level="METADATA_ONLY",
            correlation_id=correlation_id or f"corr_{int(time.time() // 300)}",
            raw_data_hash=raw_hash,
            explanation=explanation,
            metadata=metadata or {}
        )
        self.event_log.append(event)
        if len(self.event_log) > 1000:
            self.event_log.pop(0)
        return event

    def get_events_for_correlation(self, correlation_id: str) -> List[SecurityEvent]:
        return [e for e in self.event_log if e.correlation_id == correlation_id]

    def get_recent_events(self, limit: int = 50) -> List[SecurityEvent]:
        return self.event_log[-limit:]
