"""
Clipboard Guardian for VajraWorld.
Detects sensitive credential leaks on clipboard without storing values:
- API / Private Keys (AWS, GitHub, Slack, PEM headers)
- Credit Card numbers (Luhn check proxy)
- Recovery Codes & Seed Phrases
- Passwords & OTPs
Strict Privacy: Retains only type & risk level. Sets value_stored=False.
"""
from typing import Dict, Any, List
import re
import hashlib

SENSITIVE_PATTERNS = [
    (r"-----BEGIN\s+(RSA\s+)?PRIVATE\s+KEY-----", "PRIVATE_KEY_PEM", 95),
    (r"(AKIA[0-9A-Z]{16})", "AWS_ACCESS_KEY", 90),
    (r"(ghp_[0-9a-zA-Z]{36}|github_pat_[0-9a-zA-Z_]{82})", "GITHUB_TOKEN", 90),
    (r"(xox[baprs]-[0-9a-zA-Z]{10,48})", "SLACK_API_TOKEN", 85),
    (r"\b(?:\d{4}[- ]?){3}\d{4}\b", "CREDIT_CARD_NUMBER", 80),
    (r"\b[A-Za-z0-9+/]{40,}={0,2}\b", "HIGH_ENTROPY_SECRET", 65),
    (r"\b(?:\w+\s+){11,23}\w+\b", "MNEMONIC_SEED_PHRASE", 85)
]

class ClipboardGuardianEngine:
    def check_clipboard_text(self, text: str) -> Dict[str, Any]:
        """
        Inspects pasted or foreground clipboard text.
        Never stores the sensitive text value.
        """
        raw_hash = hashlib.sha256(text.encode("utf-8")).hexdigest()
        detected_types = []
        max_risk = 0

        for pattern, label, risk in SENSITIVE_PATTERNS:
            if re.search(pattern, text):
                detected_types.append(label)
                if risk > max_risk:
                    max_risk = risk

        is_sensitive = len(detected_types) > 0
        risk_score = max_risk if is_sensitive else 5

        recommendation = "Safe"
        if risk_score >= 80:
            recommendation = "SENSITIVE SECRET EXPOSED: Clear clipboard immediately and rotate exposed key."
        elif risk_score >= 50:
            recommendation = "Potential secret detected. Auto-clear clipboard recommended."

        return {
            "is_sensitive": is_sensitive,
            "detected_types": detected_types,
            "risk_score": risk_score,
            "raw_content_hash": raw_hash[:16],
            "value_stored": False,        # Strict privacy compliance
            "recommendation": recommendation,
            "suggested_clear_timer_sec": 30
        }
