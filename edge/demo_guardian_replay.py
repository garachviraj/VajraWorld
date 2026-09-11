"""
VajraWorld Guardian Multi-Surface Attack Scenario Replay.
Demonstrates the 7 core Guardian engines working together:
1. Smart Notification Defence: Intercepts phishing alert lure & verifies zero plaintext retention
2. Guardian Link Engine: Inspects deceptive IP-literal URL & forecasts link progression
3. Download & File Risk Engine: Analyzes sideloaded APK, detects toxic permissions (Accessibility+Overlay), and enforces zip-bomb safety
4. OTP Privacy Vault: Intercepts OTP verification attempt, sets value_stored=false, and flags forwarding fraud
5. Clipboard Guardian: Detects exposed secret and initiates 30s auto-clear timer
6. Threat Story Engine: Synthesizes multi-surface narrative into Unified Threat Story
7. Counterfactual Defence Simulator: Simulates blocking domain and removing APK (Risk: 88% -> 18%)
"""
import requests
import time
import sys

API_BASE = "http://127.0.0.1:8000/v1"

def run_guardian_scenario(api_url: str = API_BASE):
    print("=" * 70)
    print("VAJRAWORLD GUARDIAN -- MULTI-SURFACE CYBER-DEFENCE REPLAY")
    print("Correlating: Notification -> Link -> APK -> OTP -> Clipboard -> Network")
    print("=" * 70)

    # 1. Smart Notification Defence
    print("\n--- [Step 1] Ingesting Suspicious Banking Notification Lure ---")
    notif_payload = {
        "source_app": "com.google.android.apps.messaging",
        "message_text": "ALERT: Your account is blocked! Verify immediately at http://192.168.1.50/secure-bank-login.xyz/update.apk or your card will be canceled.",
        "extract_links": True
    }
    notif_res = requests.post(f"{api_url}/guardian/notification/analyze", json=notif_payload).json()
    print(f"[!] Notification Analyzed: Risk {notif_res['risk_score']}/100 | Confidence: {notif_res['confidence']}")
    print(f"    Reasons: {notif_res['why_points']}")
    print(f"    Extracted URLs: {notif_res['extracted_urls']}")
    print(f"    Privacy: {notif_res['privacy_compliance']}")

    # 2. Guardian Link Engine
    print("\n--- [Step 2] Guardian Link Engine 3-Layer URL Analysis ---")
    link_payload = {
        "url": notif_res['extracted_urls'][0],
        "message_context": "URGENT: account blocked! verify immediately"
    }
    link_res = requests.post(f"{api_url}/guardian/link/analyze", json=link_payload).json()
    print(f"[!] URL Risk: {link_res['risk_score']}/100 | Action: {link_res['recommended_action']}")
    for pt in link_res['why_points']:
        print(f"    * {pt}")
    print("    [+] Link Progression Trajectory:")
    for step in link_res['progression_trajectory']:
        print(f"        - {step['step']}: {step['status']} (prob: {step['probability']})")

    # 3. File & APK Risk Engine
    print("\n--- [Step 3] Inspecting Sideloaded APK Download (update.apk) ---")
    file_payload = {
        "filename": "update.apk",
        "mock_manifest": {
            "permissions": [
                "android.permission.BIND_ACCESSIBILITY_SERVICE",
                "android.permission.SYSTEM_ALERT_WINDOW",
                "android.permission.READ_SMS",
                "android.permission.INTERNET"
            ],
            "is_debuggable": True
        }
    }
    file_res = requests.post(f"{api_url}/guardian/file/analyze", json=file_payload).json()
    print(f"[!] File Risk: {file_res['risk_score']}/100 | Action: {file_res['recommended_action']}")
    for pt in file_res['why_points']:
        print(f"    * {pt}")
    print(f"    Archive Safety Check: {file_res['archive_safe']} (No Zip-Bomb)")

    # 4. OTP Privacy Vault
    print("\n--- [Step 4] Intercepting OTP Verification Lure ---")
    otp_notif = {
        "source_app": "com.android.mms",
        "message_text": "Your one-time password is 741920. Please share this OTP with customer support to unblock.",
        "extract_links": False
    }
    otp_res = requests.post(f"{api_url}/guardian/notification/analyze", json=otp_notif).json()
    print(f"[!] OTP Event Captured: Detected = {otp_res['otp_vault']['otp_detected']}")
    print(f"    Strict Privacy: Plaintext Value Stored = {otp_res['otp_vault']['value_stored']}")
    print(f"    Forwarding Scam Lure Flagged: {otp_res['otp_vault']['is_forwarding_lure']}")

    # 5. Clipboard Guardian
    print("\n--- [Step 5] Clipboard Guardian Foreground Check ---")
    clip_payload = {"clipboard_text": "AKIAIOSFODNN7EXAMPLE secret_token_data"}
    clip_res = requests.post(f"{api_url}/guardian/clipboard/analyze", json=clip_payload).json()
    print(f"[!] Clipboard Check: Sensitive = {clip_res['is_sensitive']}")
    print(f"    Detected Types: {clip_res['detected_types']}")
    print(f"    Recommendation: {clip_res['recommendation']}")
    print(f"    Auto-Clear Timer Scheduled: {clip_res['suggested_clear_timer_sec']}s")

    # 6. Unified Threat Story Engine
    print("\n--- [Step 6] Synthesizing Cross-Surface Threat Story ---")
    stories = requests.get(f"{api_url}/guardian/threat-stories").json()
    latest_story = stories[-1] if stories else {}
    print(f"[+] Story ID: {latest_story.get('story_id')} | Title: {latest_story.get('title')}")
    print(f"    Narrative: {latest_story.get('narrative')}")
    print(f"    Cumulative Risk: {latest_story.get('risk_score')}% | Confidence: {int(latest_story.get('confidence', 0.9)*100)}%")
    print(f"    MITRE Tactics: {latest_story.get('mitre_tactics')}")
    for idx, step in enumerate(latest_story.get('timeline_steps', []), 1):
        print(f"    [{step['time']}] {step['surface']}: {step['event']}")

    # 7. Counterfactual Defence Simulation
    print("\n--- [Step 7] Counterfactual Defence Simulation ('What If URL Blocked & APK Removed?') ---")
    sim = requests.post(f"{api_url}/simulation", json={
        "target_asset": "Device-Workstation",
        "action_type": "ISOLATE_HOST",
        "parameters": {"action": "BLOCK_URL_AND_REMOVE_APK"}
    }).json()
    print(f"[*] Simulation Outcome:")
    print(f"    Baseline Risk:      {int(sim['baseline_risk'] * 100)}%")
    print(f"    Post-Action Risk:   {int(sim['post_action_risk'] * 100)}% (Risk Reduction: -{sim['risk_reduction_pct']}%)")
    print(f"    Residual Risk:      {int(sim['residual_risk'] * 100)}%")
    print(f"    Action Utility:     {sim['utility_score']} (Recommended: {sim['is_recommended']})")

    # 8. Live Security Radar
    print("\n--- [Step 8] Live Security Radar Topology State ---")
    radar = requests.get(f"{api_url}/guardian/radar").json()
    print(f"[+] Radar Status: {radar['overall_status']} | Health: {radar['overall_health']}/100")
    print(f"    Surfaces Monitored: {len(radar['nodes'])} nodes, {len(radar['edges'])} active correlations")

    print("\n" + "=" * 70)
    print("[+] VajraWorld Guardian Multi-Surface Attack Scenario Completed Successfully!")
    print("=" * 70)

if __name__ == "__main__":
    run_guardian_scenario()
