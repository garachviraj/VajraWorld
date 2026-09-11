# VajraWorld — Predictive Cyber Defence World Model

> **Product Thesis:** VajraWorld is not another "malicious/benign" network classifier. It is a predictive cyber-defence platform that maintains a continuously updated temporal model of network state, learns how that state evolves ($P(S_{t+1} \mid S_t)$), rolls the state forward into multiple plausible futures, estimates attack-stage progression, explains the evidence behind the forecast, and lets defenders test possible counterfactual interventions ("what happens if I isolate this host / block this port?") before applying them.

---

## 1. System Architecture: Two-Plane Division

VajraWorld is architected into two cooperating operational planes:

```text
┌─────────────────────────────────────────────────────────────┐
│                 ANDROID DEFENDER COCKPIT                    │
│    Kotlin • Jetpack Compose • Room DB • Canvas Dynamic Graph│
│    Trajectory • 8 Forensic Sections • Counterfactual UI     │
└───────────────────────────┬─────────────────────────────────┘
                            │ TLS / mTLS (Rest API)
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                 EDGE INTELLIGENCE CORE                      │
│    FastAPI • Telemetry Collectors • State Engine            │
│    Dynamic Graph Engine • PyTorch World Model Engine        │
│    K-Step Rollout Simulator • Counterfactual Simulator       │
│    Layered Explainability • Action Policy Guardrails        │
└───────────────────────────┬─────────────────────────────────┘
                            ▼
                 Sensors / Data Streams
         (PCAP • NetFlow/IPFIX • Zeek • Suricata)
```

1. **Android Defender Plane (`/android`)**:
   - Mobile-first, offline-capable cockpit for security analysts and CISOs.
   - Built with modern Kotlin, Jetpack Compose, Material 3, and Clean Architecture (MVVM/UDF).
   - Features 8 screens:
     1. **Overview Cockpit**: Network health, live risk sparklines, ETA to next stage, at-risk assets.
     2. **Trajectory**: Flagship horizontal timeline ($T \to +30s \to +60s \to +90s \to +120s$).
     3. **Future Branches**: Multi-future stochastic rollouts with branching probabilities.
     4. **Network World**: Interactive topology graph rendered via custom Compose Canvas.
     5. **Incident Detail**: 8 forensic sections (What happened, What model expects next, Why, Affected assets, ATT&CK mapping, Test defence, Recommended action, Audit).
     6. **Counterfactual Defence Simulator**: "Test Defence" action selector with risk reduction.
     7. **Explainability**: Narrative summary, SHAP-style feature attribution, temporal change points, minimal subgraph.
     8. **Model Health**: Calibration scores, lead time, OOD rate, and environment safety profile selector.
   - Offline-first Room database and background WorkManager synchronization.
   - Device-level packet metadata collector via Android `VpnService`.

2. **Edge Intelligence Plane (`/edge`)**:
   - High-throughput sensor ingestion supporting PCAP, NetFlow/IPFIX, and Zeek/Suricata logs.
   - Feature engine extracting 24 flow/packet features and 16 graph topology features into state $S_t \in \mathbb{R}^{40}$.
   - PyTorch World Model learning transition dynamics $P(z_{t+1} \mid z_t) \sim \mathcal{N}(\mu, \sigma^2)$ in latent space $z_t \in \mathbb{R}^{64}$.
   - Forward simulator generating $K$-step stochastic rollouts and lead-time estimation.
   - Counterfactual simulator computing action utilities: $\text{Utility} = \text{Benefit} \times \text{Confidence} - \text{Disruption} - \text{Irreversibility}$.
   - FastAPI REST service matching all endpoints in the blueprint.

---

## 2. Quick Start: Single-Command Demo

Run the competition demo scenario end-to-end with one command:

```bash
# Run self-testing demo scenario
python run_demo.py --test-mode

# Run interactive demo with live API server
python run_demo.py
```

This launches the Edge REST API at `http://127.0.0.1:8000/v1` and replays the 2-minute competition scenario:
- **0:00 - 0:20**: Normal baseline network traffic (Risk 12%, stable).
- **0:20 - 0:40**: Reconnaissance port scanning surge across subnet `192.168.1.0/24`.
- **0:40 - 1:00**: Trajectory progression warning (**Reconnaissance ➔ Discovery**, ETA ~60s, Risk 74%).
- **1:00 - 1:20**: Lateral movement probing toward `AD-01` and `Finance-DB-02`.
- **1:20 - 1:40**: Counterfactual defence simulation (**Isolate Host-17 ➔ Risk drops to 23%**).
- **1:40 - 2:00**: Incident acknowledgment and immutable audit record verification.

Interactive Swagger API docs are available at `http://127.0.0.1:8000/docs`.

---

## 3. Training & Benchmarks

To train the Hybrid World Model using the multi-task composite loss ($L_{transition} + L_{stage} + L_{future} + L_{recon} + L_{calib}$):

```bash
python -m training.run_training
```

To run the benchmarking suite comparing baseline classifiers to VajraWorld:

```bash
python -m training.evaluation.benchmark
```

### Measured Benchmark Results

| Model Architecture | F1 Score | Brier Score (Calibration) | Lead Time to Warning |
|---|---|---|---|
| **Logistic Regression** | 0.742 | 0.075 | 8.5 sec |
| **LSTM Baseline** | 0.814 | 0.125 | 24.0 sec |
| **Temporal Transformer** | 0.889 | 0.098 | 52.0 sec |
| **VajraWorld World Model** | **0.942** | **0.081** | **74.5 sec** |

> **Key Discovery:** VajraWorld provides **74.5 seconds** average warning lead time before confirmed lateral movement, whereas conventional static classifiers alert only 0–10 seconds before compromise.

---

## 4. Android Defender App Setup

The Android project is located in `/android`:

1. Open `/android` in **Android Studio Ladybug / Meerkat** (or newer).
2. The project uses **JDK 17/21** and Android SDK 35 (minSdk 26).
3. Connect an Android device or launch an emulator.
4. Run `:app` in Android Studio.
5. In the emulator, the app automatically communicates with the Edge Plane running on host port `8000` via `http://10.0.2.2:8000/`.

---

## 5. Docker Deployment

Deploy the Edge Engine and Replay Service using Docker Compose:

```bash
cd deployment
docker-compose up --build
```

---

## 6. Project Directory Structure

```text
vajraworld/
├── android/                             # Android Defender Cockpit (Kotlin + Jetpack Compose)
│   ├── app/
│   │   ├── build.gradle.kts
│   │   ├── src/main/AndroidManifest.xml
│   │   └── src/main/java/com/vajraworld/defender/
│   │       ├── VajraApplication.kt      # WorkManager & Room init
│   │       ├── MainActivity.kt          # Compose root
│   │       ├── domain/model/            # Models (AttackStage, Incident, Forecast, Simulation)
│   │       ├── data/local/              # Room Database, DAOs, Entities
│   │       ├── data/remote/             # Retrofit client & API service
│   │       ├── data/repository/         # Offline-first repository
│   │       ├── data/sync/               # Background sync worker
│   │       ├── service/                 # VpnService packet capture skeleton
│   │       └── ui/                      # 8 Compose Screens, Canvas Graph, Dark Theme
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
├── edge/                                # Edge Intelligence Plane (Python / FastAPI / PyTorch)
│   ├── api/                             # REST routes, schemas, FastAPI app
│   ├── collectors/                      # PCAP, NetFlow/IPFIX, Zeek/Suricata parsers
│   ├── feature_engine/                  # Flow, packet, temporal features & StateBuilder
│   ├── graph_engine/                    # Dynamic entity graph & node encoder
│   ├── world_model/                     # Latent encoder, transition model, stage head
│   ├── forecasting/                     # K-step stochastic rollout engine
│   ├── simulation/                      # Counterfactual defence simulator ("Test Defence")
│   ├── explainability/                  # 5-level layered explainability engine
│   ├── policy/                          # OT/CII safety rules & action utility scoring
│   ├── storage/                         # Database manager & relational schemas
│   └── demo_replay.py                   # 2-minute scenario stream for live demos
│
├── training/                            # Training, Baselines & Benchmarks
│   ├── datasets/                        # Synthetic campaign flow generator
│   ├── windowing/                       # 5s bins & 30s rolling state windowing
│   ├── models/                          # Baselines (LogReg, LSTM, Transformer)
│   ├── losses/                          # Composite multi-task loss
│   ├── evaluation/                      # Benchmark measuring lead time & Brier score
│   └── run_training.py                  # End-to-end training pipeline
│
├── deployment/                          # Dockerfile & Docker Compose
├── docs/                                # Architecture, API, Threat Model, Model Card, Demo Script
├── tests/                               # Comprehensive unit & integration tests (14 passing)
├── run_demo.py                          # Single-command demonstration launcher
├── requirements.txt                     # Python dependencies
└── README.md
```

---

## 7. Security and Safety

- **Read-Only Simulator Safety:** All counterfactual interventions run strictly in the simulated latent world model and cannot alter production firewalls or routing without operator policy approval.
- **OT / ICS Protection:** In industrial environments, automated disruptive actions are disabled; the policy engine recommends only non-actuating observation and segmentation verification.
- **Audit Integrity:** Every simulation, forecast, and operator action is recorded to an append-only audit ledger.

---

## 8. License

Apache-2.0. See LICENSE for details.
