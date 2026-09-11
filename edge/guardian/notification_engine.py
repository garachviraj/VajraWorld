"""
Smart Notification Defence & OTP Privacy Vault for VajraWorld Guardian.
Implements:
1. Social engineering & scam lure detection (fake delivery, fake banking, urgency)
2. URL extraction for link correlation
3. Strict Privacy Architecture: Never stores raw message bodies or plaintext OTP values
4. OTP Vault: Logs OTP_DETECTED metadata (value_stored=false) and flags OTP forwarding scams
"""
from typing import Dict, Any, List, Optional
import re
import hashlib
import time

SCAM_PATTERNS = [
    (r"(account\s+suspended|account\s+blocked|verify\s+immediately|card\s+blocked)", "Urgent Banking Threat Lure", 45),
    (r"(delivery\s+failed|package\s+delayed|reschedule\s+delivery|parcel\s+pending)", "Postal / Courier Delivery Scam", 35),
    (r"(won\s+lottery|claim\s+prize|reward\s+points\s+expire|cashback\s+credited)", "Financial Reward / Lottery Lure", 35),
    (r"(share\s+this\s+otp|send\s+the\s+code|verify\s+code\s+with\s+agent)", "OTP Forwarding / Social Engineering Scam", 65),
    (r"(install\s+update|download\s+security\s+patch|upgrade\s+now)", "Malicious Application Download Lure", 40)
]

OTP_REGEX = r"\b\d{4,8}\b"

class GuardianNotificationEngine:
    def analyze_notification(
        self,
        source_app: str,
        message_text: str,
        extract_links: bool = True
    ) -> Dict[str, Any]:
        """
        Analyzes notification text in-memory.
        Enforces Privacy Policy: Discards raw message_text. Stores only cryptographic hash.
        """
        raw_hash = hashlib.sha256(message_text.encode("utf-8")).hexdigest()
        why_points = []
        risk_score = 0

        # 1. Scam Pattern Matching
        for pattern, label, weight in SCAM_PATTERNS:
            if re.search(pattern, message_text, re.IGNORECASE):
                risk_score += weight
                why_points.append(label)

        # 2. URL Extraction
        urls = []
        if extract_links:
            found_urls = re.findall(r"https?://[^\s]+", message_text)
            urls = found_urls

        if urls:
            why_points.append(f"Notification contains {len(urls)} external URL link(s)")
            risk_score += 15

        # 3. OTP Detection (Privacy Vault)
        otp_detected = False
        is_otp_scam = False
        if re.search(r"\b(otp|one[- ]time[- ]password|verification\s+code)\b", message_text, re.IGNORECASE):
            otp_detected = True
            # Check for high-risk OTP forwarding lure
            if re.search(r"(share|send|forward|call|support)", message_text, re.IGNORECASE):
                is_otp_scam = True
                risk_score += 40
                why_points.append("CRITICAL: Request detected to share or forward one-time verification code!")

        total_risk = min(100, risk_score)
        if total_risk == 0:
            total_risk = 5

        action = "Safe Notification"
        if total_risk >= 70:
            action = "DO NOT OPEN / REPORT SCAM"
        elif total_risk >= 40:
            action = "Exercise Caution -- Avoid clicking links or sharing codes"

        # Privacy Vault Record
        otp_vault_record = {
            "otp_detected": otp_detected,
            "value_stored": False,           # STRICT RULE: Zero Plaintext OTP Storage
            "is_forwarding_lure": is_otp_scam,
            "hash": raw_hash[:16]
        }

        return {
            "source_app": source_app,
            "raw_content_hash": raw_hash,
            "risk_score": total_risk,
            "confidence": 0.89 if why_points else 0.70,
            "why_points": why_points or ["Routine application notification"],
            "extracted_urls": urls,
            "otp_vault": otp_vault_record,
            "recommended_action": action,
            "privacy_compliance": "Raw message body discarded (zero retention)"
        }
