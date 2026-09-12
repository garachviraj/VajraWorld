import pytest
import os
import zipfile
import io
from fastapi.testclient import TestClient
from edge.api.server import app
from edge.guardian.link_engine import GuardianLinkEngine
from edge.guardian.file_engine import GuardianFileEngine
from edge.guardian.notification_engine import GuardianNotificationEngine
from edge.guardian.clipboard_engine import ClipboardGuardianEngine
from edge.guardian.threat_story_engine import ThreatStoryEngine
from edge.guardian.guardian_events import EventNormalizer

client = TestClient(app)

def test_link_engine_deceptive_url():
    engine = GuardianLinkEngine()
    
    # Suspicious link with urgent context
    res = engine.analyze_url(
        url="http://192.168.1.50/secure-bank-login.xyz/update.apk",
        message_context="URGENT: account blocked! verify OTP now"
    )
    assert res["risk_score"] >= 70
    assert "DO NOT OPEN" in res["recommended_action"]
    assert res["has_urgent_context"] is True
    assert len(res["why_points"]) >= 2
    assert len(res["progression_trajectory"]) == 5

    # Benign link
    clean_res = engine.analyze_url("https://www.google.com/search")
    assert clean_res["risk_score"] < 30
    assert "Allow" in clean_res["recommended_action"]

def test_file_engine_zip_bomb_guardrail(tmp_path):
    engine = GuardianFileEngine(max_uncompressed_bytes=1024 * 1024) # 1MB limit
    
    # Create synthetic zip bomb (large uncompressed data)
    zip_file = tmp_path / "bomb.zip"
    with zipfile.ZipFile(zip_file, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("huge_file.txt", "0" * (2 * 1024 * 1024)) # 2MB

    is_safe, msg = engine.inspect_archive_safety(str(zip_file))
    assert is_safe is False
    assert "Zip-bomb guardrail" in msg

def test_file_engine_apk_analysis():
    engine = GuardianFileEngine()
    mock_manifest = {
        "permissions": [
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.INTERNET",
            "android.permission.READ_SMS"
        ],
        "is_debuggable": True
    }
    res = engine.inspect_file("banking_trojan.apk", mock_manifest=mock_manifest)
    assert res["risk_score"] >= 75
    assert res["is_apk"] is True
    assert any("Accessibility" in pt for pt in res["why_points"])
    assert len(res["progression_trajectory"]) == 5

def test_notification_privacy_and_otp():
    engine = GuardianNotificationEngine()
    text = "Your bank verification code is 849201. Please share this OTP with customer support agent immediately."
    res = engine.analyze_notification("SMS", text)

    assert res["risk_score"] >= 70
    assert res["otp_vault"]["otp_detected"] is True
    assert res["otp_vault"]["value_stored"] is False # STRICT PRIVACY CHECK
    assert res["otp_vault"]["is_forwarding_lure"] is True
    assert "849201" not in str(res) # Proves OTP plaintext digits are NOT stored

def test_clipboard_guardian_secret_detection():
    engine = ClipboardGuardianEngine()
    clip_text = "Here is the key: AKIAIOSFODNN7EXAMPLE and secret token"
    res = engine.check_clipboard_text(clip_text)

    assert res["is_sensitive"] is True
    assert "AWS_ACCESS_KEY" in res["detected_types"]
    assert res["risk_score"] >= 80
    assert res["value_stored"] is False # STRICT PRIVACY CHECK
    assert clip_text not in str(res)    # Plaintext secret discarded

def test_threat_story_synthesis():
    engine = ThreatStoryEngine()
    story = engine.build_threat_story(
        notification_event={"source_app": "SMS", "risk_score": 60},
        link_event={"url": "hxxp://bank-update.xyz", "risk_score": 80},
        file_event={"filename": "trojan.apk", "risk_score": 85},
        network_event={"destination": "198.51.100.1:443", "risk_score": 90}
    )

    assert story["risk_score"] >= 70
    assert len(story["timeline_steps"]) == 4
    assert len(story["mitre_tactics"]) >= 3
    assert len(story["recommended_interventions"]) >= 2

def test_security_radar_state():
    engine = ThreatStoryEngine()
    radar = engine.get_security_radar_state()
    assert len(radar["nodes"]) == 6
    assert len(radar["edges"]) == 5
    surfaces = {n["surface"] for n in radar["nodes"]}
    assert {"LINK", "FILE", "NOTIFICATION", "OTP", "USER", "NETWORK"} == surfaces

def test_guardian_rest_api_endpoints():
    # 1. Link analyze endpoint
    link_resp = client.post("/v1/guardian/link/analyze", json={
        "url": "http://192.168.1.1/malware.apk",
        "message_context": "urgent action required"
    })
    assert link_resp.status_code == 200
    assert link_resp.json()["risk_score"] >= 60

    # 2. File analyze endpoint
    file_resp = client.post("/v1/guardian/file/analyze", json={
        "filename": "sideload.apk",
        "mock_manifest": {"permissions": ["android.permission.BIND_DEVICE_ADMIN"]}
    })
    assert file_resp.status_code == 200
    assert file_resp.json()["is_apk"] is True

    # 3. Notification endpoint
    notif_resp = client.post("/v1/guardian/notification/analyze", json={
        "source_app": "WhatsApp",
        "message_text": "Delivery failed! Reschedule at https://track-parcel.top/claim"
    })
    assert notif_resp.status_code == 200
    assert len(notif_resp.json()["extracted_urls"]) == 1

    # 4. Clipboard endpoint
    clip_resp = client.post("/v1/guardian/clipboard/analyze", json={
        "clipboard_text": "-----BEGIN RSA PRIVATE KEY-----"
    })
    assert clip_resp.status_code == 200
    assert clip_resp.json()["is_sensitive"] is True

    # 5. Radar endpoint
    radar_resp = client.get("/v1/guardian/radar")
    assert radar_resp.status_code == 200
    assert len(radar_resp.json()["nodes"]) == 6

    # 6. File upload endpoint with real binary APK
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w") as zf:
        manifest_data = b"package=com.trojan.banker\x00android.permission.BIND_ACCESSIBILITY_SERVICE\x00android.permission.SYSTEM_ALERT_WINDOW\x00"
        zf.writestr("AndroidManifest.xml", manifest_data)
        zf.writestr("classes.dex", b"dex\n035\x00")
    apk_payload = buf.getvalue()

    upload_resp = client.post(
        "/v1/guardian/file/upload",
        files={"file": ("malicious_payload.apk", apk_payload, "application/vnd.android.package-archive")}
    )
    assert upload_resp.status_code == 200
    assert upload_resp.json()["is_apk"] is True
    assert upload_resp.json()["risk_score"] >= 70

    # 7. Threat stories endpoint
    story_resp = client.get("/v1/guardian/threat-stories")
    assert story_resp.status_code == 200
    stories = story_resp.json()
    assert isinstance(stories, list)
    assert len(stories) >= 1
    assert "narrative" in stories[0]

def test_link_engine_brand_impersonation_levenshtein():
    engine = GuardianLinkEngine()

    # Deceptive typo-squatted brand
    res_fake = engine.analyze_url("https://paypa1-security.com/account/login")
    assert res_fake["risk_score"] >= 45
    assert any("impersonation" in pt.lower() for pt in res_fake["why_points"])
    assert any("paypal" in pt.lower() for pt in res_fake["why_points"])

    # Token containing brand
    res_token = engine.analyze_url("http://apple-support-update.xyz/verify")
    assert res_token["risk_score"] >= 45
    assert any("impersonation" in pt.lower() for pt in res_token["why_points"])

    # Official domain should NOT be flagged as impersonation
    res_official = engine.analyze_url("https://www.paypal.com/signin")
    assert not any("impersonation" in pt.lower() for pt in res_official["why_points"])

def test_file_engine_real_apk_zip_inspection():
    engine = GuardianFileEngine()

    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w") as zf:
        zf.writestr(
            "AndroidManifest.xml",
            b"test\x00android.permission.BIND_ACCESSIBILITY_SERVICE\x00android.permission.SYSTEM_ALERT_WINDOW\x00android.permission.READ_SMS\x00"
        )
        zf.writestr("classes.dex", b"dex\n035\x00code")
        zf.writestr("lib/arm64-v8a/libnative.so", b"\x7fELFfake")
    apk_content = buf.getvalue()

    result = engine.inspect_file("bank_stealer.apk", file_bytes=apk_content)
    assert result["is_apk"] is True
    assert result["risk_score"] >= 80
    assert "android.permission.BIND_ACCESSIBILITY_SERVICE" in result["permissions_analyzed"]
    assert "android.permission.SYSTEM_ALERT_WINDOW" in result["permissions_analyzed"]
    assert any("classes.dex" in pt for pt in result["why_points"])
    assert result["archive_safe"] is True

