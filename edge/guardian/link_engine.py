"""
Guardian Link Engine for VajraWorld.
Inspects suspicious URLs across 3 layers:
- Layer A: Structural & rule-based inspection (IP-literal, punycode, TLDs, entropy)
- Layer B: Lexical scoring & brand impersonation heuristics
- Layer C: Context correlation (urgent credential lures, OTP verification phrases)
Also models multi-step Link Progression Trajectory.
"""
from urllib.parse import urlparse, parse_qs
from typing import Dict, Any, List, Optional
import math
import re
from collections import Counter

SUSPICIOUS_TLDS = {".xyz", ".top", ".buzz", ".cc", ".tk", ".fit", ".rest", ".online", ".live"}
DANGEROUS_EXTENSIONS = {".apk", ".exe", ".scr", ".bat", ".cmd", ".vbs", ".dex"}
BRAND_KEYWORDS = ["bank", "secure", "login", "verify", "account", "update", "paypal", "apple", "google", "support"]
URGENT_PHRASES = ["account blocked", "verify otp", "click now", "install update", "action required", "immediate action", "suspended"]

OFFICIAL_TOP_DOMAINS = {
    "paypal": "paypal.com",
    "google": "google.com",
    "apple": "apple.com",
    "microsoft": "microsoft.com",
    "amazon": "amazon.com",
    "bankofamerica": "bankofamerica.com",
    "chase": "chase.com",
    "wellsfargo": "wellsfargo.com"
}

def levenshtein_distance(s1: str, s2: str) -> int:
    """Computes exact edit distance between two strings using dynamic programming."""
    m, n = len(s1), len(s2)
    dp = [[0] * (n + 1) for _ in range(m + 1)]
    for i in range(m + 1):
        dp[i][0] = i
    for j in range(n + 1):
        dp[0][j] = j
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            if s1[i - 1] == s2[j - 1]:
                dp[i][j] = dp[i - 1][j - 1]
            else:
                dp[i][j] = 1 + min(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
    return dp[m][n]

def calculate_shannon_entropy(text: str) -> float:
    if not text:
        return 0.0
    counts = Counter(text)
    total = len(text)
    ent = 0.0
    for count in counts.values():
        p = count / total
        if p > 0:
            ent -= p * math.log2(p)
    return float(ent)

class GuardianLinkEngine:
    def analyze_url(self, url: str, message_context: Optional[str] = None) -> Dict[str, Any]:
        """Runs 3-layer inspection and returns risk breakdown, evidence, and progression stage."""
        url_clean = url.strip()
        if not url_clean.startswith(("http://", "https://")):
            url_clean = "http://" + url_clean

        parsed = urlparse(url_clean)
        hostname = parsed.hostname or ""
        path = parsed.path or ""
        query = parsed.query or ""

        why_points = []
        rule_risk = 0

        # Layer A: Rules & Structural Inspection
        # 1. IP-literal check
        if re.match(r"^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$", hostname):
            rule_risk += 40
            why_points.append("Direct IP-literal hostname (avoids standard DNS resolution)")

        # 2. Punycode / Homoglyph check
        if "xn--" in hostname or any(ord(c) > 127 for c in hostname):
            rule_risk += 35
            why_points.append("Unicode punycode / homoglyph characters detected in domain")

        # 3. Suspicious TLD
        for tld in SUSPICIOUS_TLDS:
            if hostname.endswith(tld):
                rule_risk += 25
                why_points.append(f"High-abuse top-level domain ({tld})")
                break

        # 4. Dangerous file extension
        for ext in DANGEROUS_EXTENSIONS:
            if path.lower().endswith(ext):
                rule_risk += 35
                why_points.append(f"Direct download link to executable/package ({ext})")
                break

        # 5. Hostname Entropy
        entropy = calculate_shannon_entropy(hostname)
        if entropy > 3.8 and len(hostname) > 15:
            rule_risk += 20
            why_points.append(f"Unusually high domain character entropy ({entropy:.2f})")

        # Layer B: Lexical & Brand Impersonation
        lexical_risk = 0
        subdomains = hostname.split(".")
        if len(subdomains) > 4:
            lexical_risk += 15
            why_points.append(f"Excessive subdomain nesting ({len(subdomains)} levels)")

        # Brand impersonation detection via normalized Levenshtein distance
        hostname_lower = hostname.lower()
        domain_tokens = [t for t in re.split(r"[.\-_]", hostname_lower) if t and len(t) >= 3]

        for brand, official_domain in OFFICIAL_TOP_DOMAINS.items():
            is_official = (hostname_lower == official_domain) or hostname_lower.endswith("." + official_domain)
            if is_official:
                continue

            max_sim = 0.0
            if brand in hostname_lower:
                max_sim = 1.0
            else:
                for token in domain_tokens:
                    dist = levenshtein_distance(token, brand)
                    norm_sim = 1.0 - (dist / max(len(token), len(brand)))
                    if norm_sim > max_sim:
                        max_sim = norm_sim

            if max_sim >= 0.75:
                lexical_risk += 45
                why_points.append(f"Brand impersonation detected: deceptive similarity to '{brand}' ({max_sim*100:.1f}%)")
                break

        # Keyword stuffing in subdomains / path
        matched_brands = [kw for kw in BRAND_KEYWORDS if kw in hostname.lower() or kw in path.lower()]
        if len(matched_brands) >= 2 and not any(hostname.endswith(f".{b}.com") for b in ["google", "apple", "paypal"]):
            lexical_risk += 30
            why_points.append(f"Brand deception keyword combination: {matched_brands}")

        # Layer C: Context Correlation
        context_risk = 0
        if message_context:
            ctx_lower = message_context.lower()
            matched_urgent = [phrase for phrase in URGENT_PHRASES if phrase in ctx_lower]
            if matched_urgent:
                context_risk += 35
                why_points.append(f"Urgent social-engineering language in message: {matched_urgent}")

        # Total Composite Risk
        total_risk = min(100, rule_risk + lexical_risk + context_risk)
        if total_risk == 0:
            total_risk = 5 # baseline benign risk

        action = "Allow"
        if total_risk >= 70:
            action = "DO NOT OPEN -- Phishing / Malware Threat"
        elif total_risk >= 40:
            action = "Exercise Caution -- Suspicious Structure"

        # Link Progression Trajectory
        progression_stages = [
            {"step": "Link Discovered", "probability": 1.0, "status": "OBSERVED"},
            {"step": "Link Opened", "probability": round(min(1.0, total_risk / 90.0), 2), "status": "PENDING"},
            {"step": "Credential Harvest Page", "probability": round(min(1.0, total_risk / 100.0 * 0.85), 2), "status": "PREDICTED"},
            {"step": "Credential Exposure", "probability": round(min(1.0, total_risk / 100.0 * 0.70), 2), "status": "PREDICTED"},
            {"step": "Account Takeover Risk", "probability": round(min(1.0, total_risk / 100.0 * 0.55), 2), "status": "PREDICTED"}
        ]

        return {
            "url": url,
            "risk_score": total_risk,
            "confidence": 0.88 if len(why_points) > 1 else 0.72,
            "why_points": why_points or ["Standard lexical structure, known clean indicators"],
            "recommended_action": action,
            "entropy": round(entropy, 2),
            "progression_trajectory": progression_stages,
            "has_urgent_context": bool(context_risk > 0)
        }
