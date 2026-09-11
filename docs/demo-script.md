# VajraWorld 2-Minute Demonstration Script

This demo script matches Section 38 of the VajraWorld Blueprint. Run via `python run_demo.py`.

---

## Timeline Walkthrough

### 1. [0:00 - 0:20] Baseline Stability
- **What Evaluators See:**
  - Android Cockpit shows **Network Health: 82/100 (Stable)**.
  - Forecast Risk is **12%**; 10-minute horizon sparkline is flat green.
  - Network graph shows routine workstation traffic to DMZ Gateway.
- **Presenter Narrative:**
  > *"Traditional IDS waits for a signature match. VajraWorld begins by continuously learning normal temporal dynamics and maintaining an encoded network world model."*

### 2. [0:20 - 0:40] Reconnaissance Surge
- **What Evaluators See:**
  - Host-17 initiates multi-port SYN probing across subnet `192.168.1.0/24`.
  - Feature engine flags elevated destination port entropy and SYN burstiness.
- **Presenter Narrative:**
  > *"Host-17 has begun scanning. Rather than issuing a low-priority port scan alert that will be ignored, VajraWorld feeds this evolving transition into the world model."*

### 3. [0:40 - 1:00] Trajectory Warning
- **What Evaluators See:**
  - Risk surges to **74%**; predicted stage shifts: **Reconnaissance ➔ Discovery / Credential Access**.
  - **ETA: ~60 seconds**; High-value asset `Finance-DB-02` flagged at risk.
  - Future Branches show **72% likelihood of internal pivot**.
- **Presenter Narrative:**
  > *"Notice that no data compromise has occurred yet. The world model forecasts an escalating trajectory 74 seconds before confirmed lateral compromise."*

### 4. [1:00 - 1:20] Topology & Layered Evidence
- **What Evaluators See:**
  - On the dynamic canvas, red suspicious directional edges link Host-17 to AD-01 and Finance-DB-02.
  - Explainability card displays narrative and SHAP attributions (+19% fan-out, +13% SMB edge).
- **Presenter Narrative:**
  > *"The defender inspects why the trajectory escalated: rapid internal fan-out, Kerberos probing, and an abnormal SMB edge."*

### 5. [1:20 - 1:40] Counterfactual Defence Simulation ("Test Defence")
- **What Evaluators See:**
  - Defender selects **Isolate Host-17** and taps **Run Simulation**.
  - Simulated risk immediately drops from **78% ➔ 23% (-55% reduction)**.
  - Utility Score confirms high benefit with medium disruption.
- **Presenter Narrative:**
  > *"Here is the key differentiator: the defender tests a counterfactual intervention in the world model before taking action on the production network."*

### 6. [1:40 - 2:00] Acknowledgment & Audit
- **What Evaluators See:**
  - Incident is acknowledged; status updates to INVESTIGATING.
  - Action is written to immutable audit trail.
- **Presenter Narrative:**
  > *"The system didn't wait for exfiltration. It learned the evolving state, forecast the future, explained why, and let the defender change the future."*

---

## Part II: VajraWorld Guardian Multi-Surface Attack Scenario Replay

Run via:
```bash
python run_demo.py --guardian
```

### 1. Phishing SMS & Banking Alert Interception
- **Observed Surface**: Notification (`com.google.android.apps.messaging`)
- **Action**: Extracts deceptive URL, evaluates urgency cues, immediately discards raw body with SHA-256 hash privacy guarantee.

### 2. Guardian Link 3-Layer URL Analysis & Progression
- **Observed Surface**: Link
- **Action**: Detects IP-literal hostname, brand deception keywords, and flags `.apk` executable download.
- **Progression Trajectory**: Models 5 stages: Discovered (1.0) -> Opened (1.0) -> Credential Harvest (0.85) -> Credential Exposure (0.70) -> Account Takeover (0.55).

### 3. Sideloaded APK Decompression & Manifest Risk
- **Observed Surface**: File/APK (`update.apk`)
- **Action**: Enforces zip-bomb guardrails, flags `android:debuggable=true`, and identifies toxic combination `BIND_ACCESSIBILITY_SERVICE` + `SYSTEM_ALERT_WINDOW` + `READ_SMS` (Banking trojan).

### 4. OTP Privacy Vault
- **Observed Surface**: OTP Notification
- **Action**: Flags forwarding scam lure; strictly enforces `value_stored: false` and zero plaintext retention.

### 5. Foreground Clipboard Secret Detection
- **Observed Surface**: Clipboard
- **Action**: Detects leaked API token (`AWS_ACCESS_KEY`) on user paste; triggers 30-second memory auto-clear countdown.

### 6. Threat Story Synthesis & Security Radar
- **Observed Surface**: Multi-Surface Correlation
- **Action**: Causal narrative synthesized: `Notification Lure -> URL Click -> APK Sideload -> C2 Beaconing Channel`.
- **Counterfactual Intervention**: Simulating `BLOCK_URL_AND_REMOVE_APK` drops cumulative attack risk from 82% to 15%.
- **Radar Canvas**: All 6 nodes render with live health (68/100) and correlated threat edges.

