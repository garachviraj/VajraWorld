"""
20-File Comprehensive Detection Engine Benchmark
Validates zero false positives on benign files (FPR = 0.0%)
and 100% detection recall on genuine threats.
"""
import io
import zipfile
import pytest
from edge.guardian.file_engine import GuardianFileEngine

@pytest.fixture
def file_engine():
    return GuardianFileEngine()

# ==============================================================================
# BENIGN FILES SUITE (Expected Verdict: Clean, risk < 35, 0 false positives)
# ==============================================================================

def test_01_camera_jpeg_with_exif_thumbnail(file_engine):
    """Camera JPEG with EXIF header and thumbnail should never be flagged as stego."""
    # Simulate valid JPEG with APP1 EXIF segment (which contains an embedded thumbnail JPEG FF D8 .. FF D9)
    # followed by the real SOS image scan FF DA and final EOI FF D9.
    app1_payload = b"Exif\x00\x00\xff\xd8\xff\xe0\x00\x10JFIF\x00\xff\xd9" # Thumbnail JPEG inside EXIF
    app1_len = len(app1_payload) + 2
    
    jpeg_bytes = (
        b"\xff\xd8" # SOI
        b"\xff\xe1" + app1_len.to_bytes(2, "big") + app1_payload + # APP1
        b"\xff\xdb\x00\x05\x00\x01\x02\x03" # DQT
        b"\xff\xda\x00\x08\x01\x01\x00\x00\x3f\x00" # SOS
        b"\x12\x34\x56\x78\x9a\xbc\xde\xf0" # Image scan data
        b"\xff\xd9" # True final EOI
    )
    res = file_engine.inspect_file("IMG_20260913_153022.jpg", file_bytes=jpeg_bytes)
    assert res["risk_score"] < 20, f"False positive on camera JPEG: risk={res['risk_score']}"
    assert res["recommended_action"] == "Safe to Keep"

def test_02_python_developer_script(file_engine):
    """Python scripts in storage are standard developer files, not Android threats."""
    script_bytes = b"import os\nprint('Clean maintenance script')\n"
    res = file_engine.inspect_file("backup_utility.py", file_bytes=script_bytes)
    assert res["risk_score"] <= 15, f"False positive on Python script: risk={res['risk_score']}"
    assert res["recommended_action"] == "Safe to Keep"

def test_03_native_shared_library(file_engine):
    """.so native libraries in app storage or downloads are normal assets."""
    elf_bytes = b"\x7fELF\x02\x01\x01\x00" + b"\x00" * 64
    res = file_engine.inspect_file("libtensorflowlite.so", file_bytes=elf_bytes)
    assert res["risk_score"] <= 15, f"False positive on .so library: risk={res['risk_score']}"
    assert res["recommended_action"] == "Safe to Keep"

def test_04_benign_apk_with_sms_otp(file_engine):
    """Standard chat or banking app with SMS and Internet for OTP auto-read."""
    mock_manifest = {
        "permissions": [
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.INTERNET"
        ],
        "is_debuggable": False
    }
    res = file_engine.inspect_file("signal_messenger.apk", mock_manifest=mock_manifest)
    assert res["risk_score"] <= 30, f"False positive on SMS OTP app: risk={res['risk_score']}"
    assert res["recommended_action"] == "Safe to Keep"

def test_05_benign_apk_with_accessibility_service(file_engine):
    """Legitimate accessibility screen reader / assistant."""
    mock_manifest = {
        "permissions": [
            "android.permission.BIND_ACCESSIBILITY_SERVICE"
        ],
        "is_debuggable": False
    }
    res = file_engine.inspect_file("talkback_service.apk", mock_manifest=mock_manifest)
    assert res["risk_score"] <= 30, f"False positive on Accessibility tool: risk={res['risk_score']}"
    assert res["recommended_action"] == "Safe to Keep"

def test_06_benign_apk_with_overlay(file_engine):
    """Screen filter or chat bubbles app with overlay permission."""
    mock_manifest = {
        "permissions": [
            "android.permission.SYSTEM_ALERT_WINDOW"
        ],
        "is_debuggable": False
    }
    res = file_engine.inspect_file("twilight_screen_dimmer.apk", mock_manifest=mock_manifest)
    assert res["risk_score"] <= 30, f"False positive on Overlay tool: risk={res['risk_score']}"
    assert res["recommended_action"] == "Safe to Keep"

def test_07_normal_png_image(file_engine):
    """Normal PNG graphic with IEND chunk."""
    png_bytes = b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\x00IEND\xaeB`\x82"
    res = file_engine.inspect_file("screenshot.png", file_bytes=png_bytes)
    assert res["risk_score"] <= 10, f"False positive on PNG: risk={res['risk_score']}"
    assert res["recommended_action"] == "Safe to Keep"

def test_08_pdf_document(file_engine):
    """Standard user document."""
    pdf_bytes = b"%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF"
    res = file_engine.inspect_file("quarterly_report.pdf", file_bytes=pdf_bytes)
    assert res["risk_score"] <= 10, f"False positive on PDF: risk={res['risk_score']}"
    assert res["recommended_action"] == "Safe to Keep"

def test_09_windows_pe_executable_on_android(file_engine):
    """Windows executable stored in downloads cannot execute on Android."""
    pe_bytes = b"MZ\x90\x00\x03\x00\x00\x00\x04\x00\x00\x00\xff\xff\x00\x00"
    res = file_engine.inspect_file("zoom_installer.exe", file_bytes=pe_bytes)
    assert res["risk_score"] <= 15, f"False positive on Windows PE: risk={res['risk_score']}"
    assert res["recommended_action"] == "Safe to Keep"

def test_10_mp4_video_file(file_engine):
    """Standard user video asset."""
    mp4_bytes = b"\x00\x00\x00 ftypisom\x00\x00\x02\x00isomiso2avc1mp41"
    res = file_engine.inspect_file("family_vacation.mp4", file_bytes=mp4_bytes)
    assert res["risk_score"] <= 10, f"False positive on MP4 video: risk={res['risk_score']}"
    assert res["recommended_action"] == "Safe to Keep"

def test_11_allowlisted_clean_hash(file_engine):
    """Verified allowlisted hash gives 0 risk and 100% confidence."""
    empty_bytes = b""
    res = file_engine.inspect_file("clean_stub.apk", file_bytes=empty_bytes)
    assert res["risk_score"] == 0
    assert res["confidence"] == 1.0
    assert "allowlist" in res["why_points"][0].lower()


# ==============================================================================
# MALICIOUS FILES SUITE (Expected Verdict: Threat, risk >= 85, 100% Recall)
# ==============================================================================

def test_12_stego_jpeg_with_appended_apk(file_engine):
    """JPEG image with an entire ZIP/APK appended after EOI marker."""
    jpeg_header = b"\xff\xd8\xff\xe0\x00\x10JFIF\x00\x01\x01\x01\x00`\x00`\x00\x00\xff\xda\x00\x08\x01\x01\x00\x00\x3f\x00\x11\x22\xff\xd9"
    appended_zip = b"PK\x03\x04\x14\x00\x00\x00" + b"\x00" * 40 # ZIP header
    stego_bytes = jpeg_header + appended_zip

    res = file_engine.inspect_file("innocent_cat.jpg", file_bytes=stego_bytes)
    assert res["risk_score"] >= 90
    assert any("Steganography" in pt for pt in res["why_points"])

def test_13_stego_jpeg_with_appended_dex(file_engine):
    """JPEG image with Dalvik bytecode appended after EOI marker."""
    jpeg_header = b"\xff\xd8\xff\xe0\x00\x10JFIF\x00\x01\x01\x01\x00`\x00`\x00\x00\xff\xda\x00\x08\x01\x01\x00\x00\x3f\x00\x11\x22\xff\xd9"
    appended_dex = b"dex\n035\x00" + b"\x00" * 40
    stego_bytes = jpeg_header + appended_dex

    res = file_engine.inspect_file("profile_pic.jpg", file_bytes=stego_bytes)
    assert res["risk_score"] >= 90
    assert any("Steganography" in pt for pt in res["why_points"])

def test_14_disguised_dex_as_jpg(file_engine):
    """Dalvik DEX bytecode file disguised with a .jpg file extension."""
    dex_bytes = b"dex\n035\x00" + b"\x00" * 128
    res = file_engine.inspect_file("avatar.jpg", file_bytes=dex_bytes)
    assert res["risk_score"] >= 95
    assert any("Executable Spoofing" in pt for pt in res["why_points"])

def test_15_disguised_elf_as_png(file_engine):
    """Linux ELF executable binary disguised with a .png file extension."""
    elf_bytes = b"\x7fELF\x01\x01\x01\x00" + b"\x00" * 64
    res = file_engine.inspect_file("nature_wallpaper.png", file_bytes=elf_bytes)
    assert res["risk_score"] >= 95
    assert any("Executable Spoofing" in pt for pt in res["why_points"])

def test_16_banking_trojan_apk(file_engine):
    """Classic toxic banking trojan pattern (Accessibility + Overlay + SMS + Internet + Debuggable)."""
    mock_manifest = {
        "permissions": [
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.READ_SMS",
            "android.permission.INTERNET"
        ],
        "is_debuggable": True
    }
    res = file_engine.inspect_file("bank_security_update.apk", mock_manifest=mock_manifest)
    assert res["risk_score"] >= 85
    assert any("banking trojan" in pt.lower() for pt in res["why_points"])

def test_17_ransomware_file(file_engine):
    """Known ransomware encrypted artifact."""
    res = file_engine.inspect_file("corporate_financials.locked")
    assert res["risk_score"] >= 90
    assert any("ransomware" in pt.lower() for pt in res["why_points"])

def test_18_double_extension_spoof(file_engine):
    """Deceptive double extension designed to trick user into executing APK."""
    res = file_engine.inspect_file("salary_increment_2026.pdf.apk")
    assert res["risk_score"] >= 90
    assert any("double extension" in pt.lower() for pt in res["why_points"])

def test_19_zip_bomb_defense(file_engine):
    """Zip bomb archive exceeding uncompressed size limits."""
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("huge_sparse_block.bin", b"0" * (105 * 1024 * 1024))
    bomb_bytes = buf.getvalue()

    res = file_engine.inspect_file("decompression_bomb.apk", file_bytes=bomb_bytes)
    assert res["risk_score"] == 100
    assert res["archive_safe"] is False
    assert any("Archive safety violation" in pt for pt in res["why_points"])

def test_20_privilege_escalation_dropper_apk(file_engine):
    """Privilege escalation dropper combining Device Admin, Accessibility, and Screen Overlay."""
    mock_manifest = {
        "permissions": [
            "android.permission.BIND_DEVICE_ADMIN",
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "android.permission.SYSTEM_ALERT_WINDOW"
        ],
        "is_debuggable": False
    }
    res = file_engine.inspect_file("system_patcher.apk", mock_manifest=mock_manifest)
    assert res["risk_score"] >= 85
    assert any("Accessibility" in pt for pt in res["why_points"])
