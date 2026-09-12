# VajraWorld Guardian — Feature Status

This document provides a transparent, evidence-based status for every feature in VajraWorld Guardian.  
Every feature is strictly classified as one of:
- **REAL**: Fully functional implementation with authentic calculations and automated test verification.
- **PARTIAL**: Partially functional implementation or working with known gaps/limitations.
- **DEMO / SIMULATION**: Operates on synthetic or pre-canned data scenarios; clearly disclosed to the user.
- **NOT SUPPORTED YET**: Architectural stub or planned feature without operational implementation.

---

| Feature Area | Specific Feature | Status | Evidence & Code Location | Notes & Limitations |
|---|---|---|---|---|
| **URL & Phishing Analysis** | Structural Rule Engine (Layer 1) | **REAL** | `edge/guardian/link_engine.py` (line 47), `test_guardian.py` | Detects IP literals, Punycode, suspicious TLDs, dangerous extensions, and Shannon entropy. |
| | Brand Similarity (Layer 2) | **PARTIAL** | `edge/guardian/link_engine.py` (line 78) | Matches brand keywords; lacks normalized Levenshtein distance against official domains. |
| | Lightweight ML Classifier (Layer 3) | **NOT SUPPORTED YET** | Missing ONNX/TFLite export | Needs trained n-gram / logistic regression classifier exportable for mobile inference. |
| | Offline Android URL Rules | **PARTIAL** | `LinkScanViewModel.kt` (line 46) | Previously used primitive substring check; requires full Kotlin domain rule engine. |
| | Android Share Sheet URL Ingestion | **NOT SUPPORTED YET** | `AndroidManifest.xml` | `ACTION_SEND` intent filter not yet declared in `MainActivity`. |
| **File & APK Inspection** | Storage Access Framework (SAF) | **NOT SUPPORTED YET** | `FileScanScreen.kt` | Missing `rememberLauncherForActivityResult(OpenDocument)`. |
| | Streaming SHA-256 Hash | **REAL** | `edge/guardian/file_engine.py` (line 79) | Computes authentic SHA-256; Android fallback previously used empty string. |
| | Zip-Bomb Defense Guardrails | **REAL** | `edge/guardian/file_engine.py` (line 37), `test_guardian.py` | Enforces max files, uncompressed size limits, and max compression ratio. |
| | APK Permission Combination Analysis | **REAL** | `edge/guardian/file_engine.py` (line 14) | Detects toxic combinations: Accessibility + Overlay + SMS. |
| | APK Manifest Binary XML Parsing | **PARTIAL** | `edge/guardian/file_engine.py` (line 95) | Analyzes `mock_manifest` dictionary; needs binary XML parser for raw APK bytes. |
| **Smart Notification Defence** | In-flight Scam Pattern Matching | **REAL** | `edge/guardian/notification_engine.py` (line 14), `test_guardian.py` | Detects urgent banking, delivery, prize, and download lures. |
| | Zero Plaintext Notification Storage | **REAL** | `edge/guardian/notification_engine.py` (line 35) | Discards raw message body; retains only SHA-256 hash and metadata. |
| | Android Notification Listener | **NOT SUPPORTED YET** | `AndroidManifest.xml` | `NotificationListenerService` not declared in Android manifest. |
| **OTP Privacy Vault** | Forwarding Scam Detection | **REAL** | `edge/guardian/notification_engine.py` (line 58) | Flags messages requesting users to share or forward verification codes. |
| | Guaranteed Zero Plaintext OTP Retention | **REAL** | `edge/guardian/notification_engine.py` (line 79), `test_guardian.py` | Never stores OTP digits in memory, logs, database, or API responses (`value_stored: false`). |
| **Clipboard Guardian** | Foreground Secret Leak Detection | **REAL** | `edge/guardian/clipboard_engine.py` (line 14), `test_guardian.py` | Detects AWS keys, GitHub tokens, Slack tokens, private keys, credit cards, seed phrases. |
| | Privacy & Zero Value Retention | **REAL** | `edge/guardian/clipboard_engine.py` (line 54) | Never stores raw clipboard content; outputs only secret type and risk score. |
| | Auto-Clear Countdown Timer | **REAL** | `ClipboardGuardianScreen.kt` | Interactive countdown timer (15s/30s/60s) with memory wipe button. |
| **Network Security** | Packet Metadata Ingestion (PCAP) | **REAL** | `edge/collectors/pcap_collector.py`, `test_api.py` | Parses Ethernet/IP/TCP/UDP frames and computes flow statistics. |
| | NetFlow / Flow Batch Telemetry | **REAL** | `edge/api/routes.py` (line 78), `test_api.py` | Ingests NetFlow batches and dynamically updates the entity graph. |
| | Android VpnService Packet Capture | **PARTIAL / SKELETON** | `VajraVpnService.kt` | Opens TUN interface and parses IPv4 headers; lacks outgoing NAT/forwarding. |
| | Live Telemetry Stream | **SIMULATION ONLY** | `edge/live_stream.py` | Generates synthetic flow batches and risk fluctuations; clearly marked as simulation. |
| **Security Event Engine** | Canonical Event Normalization | **REAL** | `edge/guardian/guardian_events.py`, `test_guardian.py` | Normalizes all surface events with UUIDs, timestamps, and SHA-256 hashes. |
| | Event Ingestion API | **REAL** | `POST /v1/guardian/events`, `test_api.py` | Dynamically updates radar surface threat levels upon event arrival. |
| **Threat Story Engine** | Multi-Surface Causal Narrative | **PARTIAL** | `edge/guardian/threat_story_engine.py`, `test_guardian.py` | Generates narrative from events; seeds fallback template when events are absent. |
| | Dynamic Correlation Graph | **PARTIAL** | `edge/guardian/threat_story_engine.py` | Needs dynamic edge linking by shared IP, domain, package, and temporal ordering. |
| **Security State & World Model** | 40-Dimensional Security State Vector | **REAL** | `edge/feature_engine/state_builder.py`, `test_features.py` | Combines 24 flow/packet features and 16 graph topology features into $S_t \in \mathbb{R}^{40}$. |
| | PyTorch World Model Architecture | **REAL** | `edge/world_model/model.py`, `test_world_model.py` | Fully functional latent encoder, transition model, stage head, and uncertainty head. |
| | K-Step Stochastic Rollout Engine | **REAL** | `edge/world_model/model.py` (line 61), `test_world_model.py` | Computes multi-horizon risk paths and dominant future clusters. |
| | Model Status & Registry | **PARTIAL** | `GET /v1/model/status`, `db.py` | Database stores model version; needs dynamic status flags (`TRAINED`, `DEMO`, `UNTRAINED`). |
| **Forecasting & Branches** | Horizon Risk Projection ($T \to +120s$) | **REAL** | `edge/forecasting/rollout_engine.py`, `test_api.py` | Generates horizon risk curves and lead-time-to-compromise estimates. |
| | Probabilistic Future Branches | **REAL** | `POST /v1/forecast`, `test_api.py` | Clusters rollouts into dominant future paths with branch probabilities. |
| **Defence Simulator** | Counterfactual Latent Simulation | **REAL** | `edge/simulation/counterfactual.py`, `test_simulation.py` | Simulates actions (`ISOLATE_HOST`, `BLOCK_PORT`) and calculates risk reduction. |
| | Action Utility Scoring | **REAL** | `edge/policy/action_policy.py`, `test_simulation.py` | Evaluates benefit, disruption cost, confidence, and recommend status. |
| | Android Simulation Screen | **PARTIAL** | `SimulationViewModel.kt` | Successfully calls API, but previously pre-populated artificial result on startup. |
| **Explainability** | 5-Level Layered Explanation | **REAL** | `edge/explainability/layered_explainer.py`, `test_explainability.py` | Generates narrative, SHAP feature attributions, and temporal change points. |
| | Android Explainability Screen | **FAKE / MOCK** | `ExplainabilityViewModel.kt` | Displayed 100% hardcoded strings; requires repository and API connection. |
| **Model Health & Benchmarks** | Benchmark Evaluation Script | **FAKE / NOT MEASURED** | `training/evaluation/benchmark.py` | Hardcoded static metrics; needs real dataset test execution. |
| | Android Health Screen | **FAKE / MOCK** | `HealthViewModel.kt` | Static hardcoded numbers; button only mutated local UI string. |
| **Android Architecture & Quality** | Jetpack Compose Clean Architecture | **REAL** | `/android/app/src/main/` | 12 interactive screens, Material 3, clean ViewModels and Repository. |
| | Room Database Persistence | **PARTIAL** | `VajraDatabase.kt` | Tables for incidents and forecasts exist; missing events and scan results. |
| | Android Automated Tests | **NOT SUPPORTED YET** | Missing in `android/app/src/test` | Zero unit or integration tests currently in Android project. |
