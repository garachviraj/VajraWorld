# VajraWorld Guardian — Implementation Roadmap

This roadmap outlines the systematic, phased engineering execution plan to eliminate all fake, mock, and hardcoded implementations, replacing them with authentic, production-grade cybersecurity and machine learning capabilities.

---

## Phase 1: Build Verification & Clean Compilation (Completed)
- [x] Verify Python backend test suite (23 passing pytest tests).
- [x] Verify Android Kotlin compilation with Kotlin 2.1 / Compose BOM / Gradle 8.13.
- [x] Assemble Android standalone debug APK (`app-debug.apk`).
- [x] Complete technical audit and produce `PROJECT_AUDIT.md`, `FEATURE_STATUS.md`, and `IMPLEMENTATION_ROADMAP.md`.

---

## Phase 2: Eliminate Fake, Mock & Hardcoded Implementations
- [x] **Audit & Identify**: Fully mapped all hardcoded and mock points across ViewModels, engines, and benchmarks.
- [ ] **Fix `HealthViewModel` & Health Screen**:
  - Connect `HealthViewModel` to repository and `GET /v1/model/status`.
  - Replace static numbers with live model metadata (version, training date, status: `TRAINED`, `DEMO`, or `UNTRAINED`, measured calibration).
  - Make the environment safety profile selector actually send preferences to the backend policy engine.
- [ ] **Fix `ExplainabilityViewModel` & Screen**:
  - Connect `ExplainabilityViewModel` to `VajraRepository` and `GET /v1/forecast/{id}/explanations`.
  - Remove hardcoded mock strings; display dynamic feature attribution and temporal change points.
- [ ] **Fix `SimulationViewModel`**:
  - Remove pre-populated artificial `lastResult`; start in an empty state prompting user action.
  - Show explicit "SIMULATED INTERVENTION (LATENT MODEL)" or "RULE-BASED ESTIMATE" status banner.
- [ ] **Fix `NetworkGraphViewModel`**:
  - Connect to `GET /v1/graph/current` to render dynamic discovered nodes and edges.
  - Clearly label static enterprise topology as "REFERENCE TOPOLOGY DEMO".
- [ ] **Fix Benchmark Script (`benchmark.py`)**:
  - Remove hardcoded F1, Brier, and Lead Time numbers.
  - Implement actual model inference evaluation against a test partition of campaign flows, reporting only true measured metrics.
  - Remove hardcoded seed metrics from `edge/storage/db.py`.
- [ ] **Clarify Telemetry Generator (`live_stream.py`)**:
  - Explicitly mark synthetic flows with `is_synthetic: true` and add UI indicator "SYNTHETIC DEMO TELEMETRY".

---

## Phase 3: Real Local & Remote Scanners
- [ ] **Real Android File & APK Inspection**:
  - Implement Android Storage Access Framework (`ActivityResultContracts.OpenDocument()`) in `FileScanScreen.kt`.
  - Implement genuine streaming SHA-256 calculation over `ContentResolver.openInputStream(uri)`.
  - Implement real APK metadata inspection: parse package name, version, certificates, and permissions directly from the APK archive entries (`zipfile` / `ZipInputStream`).
  - Connect `FileScanViewModel` to upload or stream file bytes to backend `POST /v1/guardian/file/analyze`.
- [ ] **Real Local URL & Phishing Engine**:
  - Port Layer 1 structural rules (Punycode, homoglyphs, IP literals, TLD risk, Shannon entropy) directly into a Kotlin domain class `UrlRuleEngine` for 100% offline capability.
  - Implement normalized Levenshtein distance brand similarity comparison against top authentic domains (e.g. `paypal.com`, `apple.com`, `google.com`, `bankofamerica.com`).
  - Add Android Share Sheet intent filter (`ACTION_SEND`) to `MainActivity` so users can share suspicious links directly from Chrome or messaging apps into `LinkScanScreen`.
- [ ] **Real Local Clipboard Guardian**:
  - Implement offline secret detection in Kotlin (`ClipboardSecretEngine`) using the compiled regex patterns (AWS, GitHub, PEM keys, tokens).
  - Strictly observe Android 10+ foreground clipboard rules; ensure zero persistence of raw secret tokens.

---

## Phase 4: Real Multi-Surface Threat Correlation
- [ ] **Dynamic Threat Story Correlation Graph**:
  - Upgrade `edge/guardian/threat_story_engine.py` to maintain a dynamic bipartite graph of observed security events.
  - Correlate events based on shared identifiers: destination IP, URL domain, extracted package name, and temporal sequence.
  - Eliminate hardcoded fallback narratives; generate dynamic attack chain narratives based strictly on linked graph components.
- [ ] **Canonical Event Ledger & Room Persistence**:
  - Create Room entities for `SecurityEventEntity` and `ScanResultEntity` in Android database.
  - Store normalized security events locally so the Security Timeline screen reflects genuine user actions and scans.

---

## Phase 5: World Model & Forecasting Hardening
- [ ] **Model Checkpoint Management**:
  - Implement model weights persistence (`torch.save` / `torch.load`) in `edge/world_model/` so the model does not run on randomly initialized weights upon server restart.
  - Provide a pre-trained checkpoint trained on the synthetic multi-stage campaign dataset.
  - Expose model status (`TRAINED`, `TRAINING`, `UNTRAINED`, `DEMO`) through `GET /v1/model/status`.
- [ ] **Dynamic Timeline Projection**:
  - Wire `TrajectoryViewModel` to dynamically populate timeline nodes from forecast `horizon_risks` and stage probabilities rather than hardcoded default nodes.

---

## Phase 6: Smart Notification & Android Security Services
- [ ] **Android Notification Service**:
  - Declare `VajraNotificationListenerService` in `AndroidManifest.xml`.
  - Provide explicit permission enablement flow with clear privacy disclosures:
    - Mode 1: Metadata analysis only.
    - Mode 2: Local content analysis (zero raw text storage, SHA-256 hashed evidence only).
  - Guarantee zero plaintext OTP storage across all layers.
- [ ] **Android VpnService Disclosures & Safety**:
  - Add explicit user disclosure dialog explaining VPN packet metadata capture and Android network limitations.
  - Provide a safe toggle with foreground service notification and stop button.

---

## Phase 7: Production Hardening, Auth & Privacy
- [ ] **API Security**:
  - Implement JWT authentication and secure token handling.
  - Add input validation and rate limiting on FastAPI endpoints.
  - Remove all hardcoded secret keys; use environment variable abstractions.
- [ ] **Data Minimization & User Control**:
  - Add a "Purge All Data" action in Settings to clear Room DB and reset state.
  - Ensure zero plaintext storage of credentials, secrets, or OTPs is strictly enforced and audited.

---

## Phase 8: Testing & Verification
- [ ] **Android Unit Tests**:
  - Create unit tests for `UrlRuleEngine` (Punycode, entropy, brand similarity).
  - Create unit tests for `ClipboardSecretEngine` (token detection, zero retention).
  - Create unit tests for `FileHashCalculator` (SHA-256).
  - Create unit tests for ViewModels (`LinkScanViewModel`, `FileScanViewModel`, `OverviewViewModel`).
  - Create Room DAO integration tests.
- [ ] **Backend Security & Fuzzing Tests**:
  - Add zip-bomb and malformed archive tests.
  - Add benchmark evaluation test verifying that benchmark metrics are calculated dynamically without hardcoded constants.
  - Run full test suite (`python -m pytest -v`) and Android compile.
