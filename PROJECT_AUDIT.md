# VajraWorld Guardian — Comprehensive Technical Audit

**Audit Date:** 2026-09-12  
**Auditors:** Senior Android Security Engineer, Cybersecurity Architect, AI/ML Engineer, Backend Engineer, Production Software Engineer  
**Audit Standard:** Zero Fake AI, Zero Fake Telemetry, Explicit Classification (REAL, SIMULATION, PARTIAL, NOT SUPPORTED YET).

---

## 1. Executive Summary

A comprehensive, line-by-line technical audit was performed across the entire VajraWorld Guardian codebase covering both operational planes:
1. **Android Defender Plane** (`/android` — Kotlin, Jetpack Compose, Room, Retrofit, Coroutines/Flow)
2. **Edge Intelligence Plane** (`/edge`, `/training`, `/tests` — Python, FastAPI, PyTorch, Scapy/Collectors, SQLite)

### Key Findings
- **Real Engineering Core**: The PyTorch World Model architecture (`VajraWorldModel`), Zip-bomb archive safety guardrail, notification privacy vault (zero plaintext OTP retention), clipboard regex detectors, and Android Compose UI framework are genuinely implemented with 23 passing backend unit tests and successful Kotlin compilation.
- **Identified "Fake" & Hardcoded Elements**:
  - `edge/live_stream.py`: Uses `random.uniform(-0.03, 0.03)` risk jitters and random IP pools without clearly labelling it as a synthetic simulation generator.
  - `training/evaluation/benchmark.py`: Contains hardcoded benchmark results (`F1: 0.942`, `Brier: 0.081`, `Lead Time: 74.5s`).
  - `edge/storage/db.py`: Seeds the same hardcoded benchmark numbers into the `model_versions` SQLite table.
  - `HealthViewModel.kt`: Displays hardcoded benchmark numbers without dynamic model health checks; the environment profile selector only mutates a local UI string.
  - `ExplainabilityViewModel.kt`: Uses 100% hardcoded mock data (`fc_demo_2041`, hardcoded narrative and attributions) without connecting to the backend explainer.
  - `FileScanViewModel.kt`: Has NO Android Storage Access Framework (SAF) integration; passes a hardcoded `mockManifest` and returns empty-string SHA-256 fallback.
  - `LinkScanViewModel.kt`: Offline fallback uses trivial substring check (`xyz`, `apk`) rather than local structural/brand inspection rules.
  - `NetworkGraphViewModel.kt`: Displays static hardcoded enterprise topology nodes without dynamic network discovery or API binding.
  - `SimulationViewModel.kt`: Pre-populates an artificial completed simulation result on startup before any action is executed.
  - `VajraVpnService.kt`: Reads from TUN interface but lacks outgoing socket forwarding/NAT, dropping traffic if enabled (skeleton implementation).
  - Android Tests: **Zero** Android unit or integration tests exist in `android/app/src/test`.

---

## 2. Complete Module Audit Table

| Feature / Component | Current Implementation | Classification | Problems Identified | Required Upgrade |
|---|---|---|---|---|
| **MainActivity** | `ComponentActivity` launching `VajraNavGraph` | **PARTIAL** | No Intent handling for `ACTION_SEND` (sharing URLs/files from browser/other apps); no deep link routing. | Add Intent filters for `ACTION_SEND` (text/plain, application/*) to directly ingest URLs and files into scanners. |
| **Navigation (`NavGraph.kt`, `Screen.kt`)** | Compose Navigation with 12 routes and bottom nav bar | **REAL** | Lacks dedicated routes for Onboarding, Settings, Splash, and deeper scanner drilldowns. | Add Onboarding, Settings, and Splash screens into the navigation hierarchy. |
| **OverviewScreen & ViewModel** | Polls `GET /v1/live/summary` every 2.5s; renders risk meter & horizon bars | **SIMULATION** | Telemetry source was `live_stream.py` generating random jitter; default state uses hardcoded metrics. | Disclose DEMO/SIMULATION vs REAL device telemetry; wire to real device events; show connection state. |
| **SecurityRadarScreen & ViewModel** | Custom Canvas rendering 6 surface nodes and dynamic correlation edges | **REAL** | Polling works, but initial state had static node positions and lacked real-time error states. | Add distinct loading/error/offline banners; support interactive node drilldown to individual scanner screens. |
| **LinkScanScreen & ViewModel** | UI with URL input, context input, preset chips, and verdict card | **PARTIAL** | Offline fallback only checks `contains("xyz")`; does not run local Shannon entropy or Levenshtein distance. | Implement real local Layer 1 structural rules & Layer 2 brand distance in Kotlin domain layer for true offline mode. |
| **FileScanScreen & ViewModel** | UI with file name selector, preset APK buttons, and permission cards | **FAKE / MOCK** | No Android file picker (SAF); hardcodes `mockManifest` and fallback empty SHA-256 hash. | Implement Android `ActivityResultContracts.OpenDocument()`, calculate real streaming SHA-256, parse real APK zip entries. |
| **ClipboardGuardianScreen** | Foreground inspection button, paste button, countdown timer (15s/30s/60s) | **REAL** | UI and timer work; backend regex works; lacks local offline regex analyzer in Android codebase. | Implement local Kotlin regex analyzer for offline secret detection; respect Android 10+ foreground clipboard rules. |
| **NetworkGraphScreen & ViewModel** | Canvas rendering host-to-host topology and selected node panel | **FAKE / STATIC** | Hardcodes 6 static nodes (`Host-17`, `AD-01`, etc.); no connection to Edge API or dynamic graph. | Connect to `GET /v1/graph/current` or display explicit "ENTERPRISE TOPOLOGY DEMO" badge. |
| **TrajectoryScreen & ViewModel** | Horizontal timeline ($T \to +120s$) and stochastic branching cards | **PARTIAL** | Branches parsed from API, but timeline nodes are hardcoded defaults in ViewModel. | Dynamically populate timeline nodes from forecast `horizon_risks` and stage transitions. |
| **SimulationScreen & ViewModel** | Action selector ("ISOLATE_HOST", etc.) and utility score cards | **PARTIAL** | `lastResult` is pre-populated with artificial success before user runs simulation; only works online. | Start with empty state; provide clear action rationale; add rule-based offline simulation fallback. |
| **ExplainabilityScreen & ViewModel** | 5-level forensic cards (Executive, Technical, Attribution, Temporal) | **FAKE / MOCK** | ViewModel is 100% hardcoded mock data; does not call `GET /v1/forecast/{id}/explanations`. | Connect ViewModel to repository and backend explainer; support dynamic feature attribution. |
| **HealthScreen & ViewModel** | Metrics grid (Accuracy, Brier, Lead Time) and profile picker | **FAKE / MOCK** | Hardcoded numbers from `benchmark.py`; environment profile button only changes local string. | Connect to `GET /v1/model/status`; clearly label model status (TRAINED, DEMO, UNTRAINED) with real measurements. |
| **IncidentsScreen & ViewModel** | Offline-first incident list from Room DB; acknowledge action | **REAL** | Room persistence works; API sync works; lacks granular filtering and pagination. | Add search/filter by severity; support batch incident resolution. |
| **VajraRepository** | Data access layer combining Room DAO and Retrofit API | **REAL** | Contains unchecked casts and lacks offline caching for scan results, threat stories, and events. | Create Room entities and DAOs for `SecurityEvent`, `ScanResult`, and `ThreatStory`. |
| **Room Database (`VajraDatabase.kt`)** | Room DB with `IncidentEntity` and `ForecastEntity` | **PARTIAL** | Lacks tables for events, file scan history, link scan history, and audit ledger. | Add migrations and tables for `security_events`, `scan_results`, and `audit_logs`. |
| **Retrofit Client & API Service** | `ApiClient` and `VajraApiService` with Gson converter | **REAL** | Lacks JWT auth header interceptor, error handling interceptor, and timeout configuration. | Add OkHttp interceptors for auth tokens, structured error parsing, and offline connectivity check. |
| **VajraVpnService** | Opens TUN interface and reads IPv4 packet headers | **PARTIAL / SKELETON** | Lacks outgoing forwarding socket / NAT; blackholes traffic if started; no connection anomaly detection. | Clearly document Android VPN limitations; implement safe flow-metadata-only logging with explicit user consent. |
| **Notification Engine (Android)** | None in Android manifest | **NOT SUPPORTED YET** | `NotificationListenerService` is neither declared in `AndroidManifest.xml` nor implemented in Kotlin. | Implement `VajraNotificationListenerService` with explicit user toggle, disclosure, and zero-retention vault. |
| **Backend API (`edge/api/routes.py`)** | FastAPI REST endpoints for all platform capabilities | **REAL** | File analyze route accepts `mock_manifest` dict rather than file upload; threat stories seed hardcoded fallback. | Add `UploadFile` endpoint for real file byte inspection; make threat story synthesis strictly dynamic. |
| **Live Telemetry Stream (`edge/live_stream.py`)** | Background daemon generating flow batches and risk fluctuations | **SIMULATION ONLY** | Generates random uniform jitter and random IP pools without marking as synthetic. | Add explicit `is_synthetic: true` metadata flag; implement deterministic scenario replay modes. |
| **Link Engine (`edge/guardian/link_engine.py`)** | 3-layer URL analyzer (rules, entropy, context) | **REAL** | Lacks normalized Levenshtein brand similarity and ML classifier. | Add Levenshtein distance brand matching against legitimate top-domain corpus and exportable ML features. |
| **File Engine (`edge/guardian/file_engine.py`)** | Zip-bomb defense, hash calculation, permission scoring | **PARTIAL** | Relies on `mock_manifest` for APK analysis instead of parsing real APK `AndroidManifest.xml` binary XML. | Add real APK zip inspection (extracting package name, permissions, and debug flag from APK bytes). |
| **Notification Engine (`edge/guardian/notification_engine.py`)** | In-flight scam lure matching & OTP privacy vault | **REAL** | Regex pattern matching works; strictly enforces `value_stored: false`. | Support local NLP classification or lightweight ONNX inference for offline Android use. |
| **Clipboard Engine (`edge/guardian/clipboard_engine.py`)** | Regex secret detector (AWS, GitHub, PEM, seed phrases) | **REAL** | Regex engine works; zero retention enforced. | Replicate regex patterns in Android Kotlin layer for offline instant foreground inspection. |
| **Threat Story Engine (`edge/guardian/threat_story_engine.py`)** | Synthesizes multi-surface events into causal stories | **PARTIAL** | Default narrative and step timestamps are hardcoded templates when called without full events. | Build dynamic correlation graph linking events by shared entity (IP, domain, package, timestamp). |
| **World Model (`edge/world_model/model.py`)** | PyTorch hybrid latent dynamics model & rollout engine | **REAL** | Neural network architecture is real and functional; requires pre-trained checkpoint to avoid random weights. | Save trained model checkpoint (`.pt`) and provide model versioning and status metadata. |
| **Counterfactual Simulator (`edge/simulation/counterfactual.py`)** | Latent intervention simulator computing risk reduction | **REAL** | Modifies state vector and runs PyTorch model; lacks rule-based fallback if model is uninitialized. | Add rule-based defence estimation when running offline or without PyTorch backend. |
| **Explainability Engine (`edge/explainability/layered_explainer.py`)** | 5-level layered explainer (narrative, SHAP attribution) | **REAL** | Backend generates dynamic explanations, but Android frontend was never wired to it. | Wire Android `ExplainabilityViewModel` to the backend endpoint. |
| **Training Pipeline (`training/run_training.py`)** | Trains model on synthetic campaign flow windows | **REAL** | Successfully trains model; needs export of trained weights to model registry. | Add checkpoint saving (`torch.save`) to persist trained model. |
| **Benchmark Script (`training/evaluation/benchmark.py`)** | Compares LogReg, LSTM, Transformer, World Model | **FAKE / NOT MEASURED** | LSTM, Transformer, and World Model metrics are hardcoded static numbers. | Replace hardcoded values with actual real-time evaluations across test campaign splits. |
| **Android Tests** | None | **NOT SUPPORTED YET** | 0 unit tests exist in Android project. | Create comprehensive unit tests for ViewModels, local URL rules, file hashing, and Room database. |
| **Backend Tests (`tests/`)** | 23 Pytest unit and integration tests | **REAL** | All 23 tests pass; needs security fuzzing tests (zip bombs, malformed APKs, SQL injection). | Add security test cases for malicious input edge cases. |
| **Docker Deployment (`deployment/`)** | Dockerfile and docker-compose | **REAL** | Standard Python 3.12-slim container running FastAPI on port 8000. | Ensure database volume persistence and healthcheck configurations. |

---

## 3. Disclosed Limitations & Android Constraints

1. **Storage Access Framework (SAF)**:
   - Modern Android (API 30+) strictly restricts broad storage access (`MANAGE_EXTERNAL_STORAGE`).
   - Legitimate implementation: Use `ActivityResultContracts.OpenDocument()` allowing users to explicitly select files or APKs without requesting dangerous storage permissions.
2. **Foreground Clipboard Restrictions**:
   - Android 10+ prohibits background clipboard access.
   - Legitimate implementation: Check clipboard exclusively when the user is in the foreground or taps "Check Clipboard" / "Paste & Inspect".
3. **Notification Access**:
   - Requires explicit user enablement via `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`.
   - Legitimate implementation: Provide clear privacy disclosures, metadata-only mode, and zero raw message retention.
4. **VPN Service**:
   - Capturing packet metadata on-device requires local TUN routing. Without an upstream socket loop, user traffic would stall.
   - Legitimate implementation: Clarify whether the VPN is in telemetry-only capture mode, or offer user-approved pcap ingestion.
