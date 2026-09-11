# VajraWorld — Predictive Cyber Defence World Model

## Production-grade Android + Edge AI project blueprint

> **Product thesis:** VajraWorld is not another "malicious/benign" network classifier. It is a predictive cyber-defence system that maintains a continuously updated model of network state, learns how that state evolves, rolls the state forward into multiple futures, estimates attack-stage progression, explains the evidence behind the forecast, and lets defenders test possible interventions before applying them.

---

## 1. Executive summary

The strongest version of this project should be built as a **two-plane system**:

1. **Android Defender Plane** — a polished, mobile-first security console for monitoring, investigation, forecasting, explanation, incident playbooks, and offline operation. On a personal/small-network deployment it can also observe the device/network through Android `VpnService` and metadata extraction.
2. **Edge Intelligence Plane** — the production sensor and world-model engine running on a workstation, server, gateway, or small edge appliance. It ingests flow records and PCAP-derived telemetry, builds temporal network states/graphs, performs multi-step forecasting, maps predicted activity to ATT&CK, and exposes a secure local API to the Android application.

This division is important. Android can observe packets for an Android device through a local VPN/TUN interface, but an enterprise/Critical Information Infrastructure (CII) deployment needs a network-level sensor or mirrored telemetry source rather than pretending that a phone alone can see an entire enterprise network. Android therefore becomes the **defender cockpit**, while the edge service becomes the **eyes + predictive brain**.

The central innovation should be a **World Model + Counterfactual Defence Loop**:

`Observe → Encode State → Learn Dynamics → Forecast K Futures → Estimate Attack Progression → Explain → Simulate Intervention → Recommend Action → Observe Outcome`

The market already has serious capabilities around behavioral NDR, attack-path analysis, attack-progression detection, autonomous investigations, and AI-assisted response. ExtraHop emphasizes behavioral machine learning and NDR; Vectra emphasizes attack progression and ATT&CK-aligned behavioral correlation; Darktrace emphasizes autonomous investigation/response; CrowdStrike markets predictive attack-path analysis and exploitability prediction; Corelight combines network evidence, ML, explainability, and governed AI workflows.[1–6] The opportunity is therefore **not** to claim that "nobody predicts attacks." The opportunity is to make the predictive mechanism explicitly a **learned temporal state-transition model**, expose its future rollouts to defenders, attach calibrated uncertainty and evidence, and add **counterfactual "what happens if I block/isolate/segment this asset?" simulation** inside a mobile-first, offline-capable experience.

---

## 2. What the research says about the current landscape

### 2.1 Existing commercial systems are already moving beyond static IDS

Modern NDR/XDR vendors already correlate activity across time instead of treating every flow independently. ExtraHop describes continuous behavioral machine learning, prioritization, investigation, and response across network activity.[1,2] Vectra explicitly describes correlating ATT&CK-aligned behaviors over time and visualizing attack progression, including lateral movement and command-and-control.[3,4] Darktrace's Cyber AI Analyst automates investigations and can initiate them from security alerts and third-party triggers.[5] Corelight positions its platform around network evidence, ML/behavioral/signature detection, explainability, AI-assisted investigations, and guided response.[6]

CrowdStrike has gone further in a related but different direction: Falcon Exposure Management offers predictive attack path analysis, and ExPRT.AI is presented as a predictive exploitability/risk prioritization mechanism based on adversary context and telemetry.[7–10] In September 2026, CrowdStrike also announced SafeMind, an agentic defender system with an offensive model that finds attack paths and a defensive model that closes them.[11]

**Implication:** the project must not use "AI-based predictive defence" as its differentiator by itself. That claim is already crowded.

### 2.2 The research direction supports dynamic state modelling

Dynamic graph learning is a natural fit because a network is not only a list of packets: it is a changing graph of entities and interactions. Temporal Graph Networks were introduced specifically to model evolving graphs represented as time-stamped events.[12] Temporal Fusion Transformers were designed for multi-horizon forecasting while exposing interpretable temporal relationships through feature selection and attention.[13] SHAP provides a general framework for feature attribution of complex model predictions.[14]

The supplied challenge's use of a transition distribution `P(S[t+1] | S[t])` is therefore well aligned with established machine-learning ideas, but the project should implement the idea as an **operational world model** rather than as a renamed classification model.

### 2.3 Digital-twin work points to a bigger product direction

A January 2026 open-access position paper on cybersecurity digital twins argues that security operations need models capable of predicting how attacks evolve in the real environment and under hypothetical configurations, including attack propagation, hunting, and response. It highlights the value of continuously synchronized operational models and counterfactual analysis across domains.[15]

This is a very useful strategic clue: the strongest long-term product is closer to a **Cybersecurity World Model / Security Digital Twin** than to a conventional IDS dashboard.

---

## 3. Market whitespace: what to add instead of copying existing products

The following should be treated as **research-backed product hypotheses**, not as absolute claims that no competitor has ever implemented any one feature privately. Public product documentation reviewed for this blueprint shows strong capabilities in detection, behavioral analytics, attack-path analysis, investigations, and response, but it does not clearly document one mainstream product experience that combines all of the following in one operator workflow:

| Proposed VajraWorld capability | Why it matters | Evidence / market interpretation |
|---|---|---|
| **Explicit learned world model** | Models state transitions and forecasts future states instead of only scoring current detections | Public vendors describe behavioral ML and attack-path prediction, but the product proposition here is to expose the learned temporal transition dynamics themselves as a first-class artifact. [1–10] |
| **K-step stochastic rollout** | Shows several plausible attack futures instead of one risk score | This changes the defender question from "what is happening?" to "where is the trajectory heading?" |
| **Counterfactual defence simulation** | Test `block host`, `isolate segment`, `disable credential`, `rate-limit`, etc. before touching production | Cybersecurity digital-twin research explicitly motivates prediction under hypothetical configurations.[15] |
| **Probability + uncertainty together** | A forecast without confidence bounds is dangerous in security | Make "unknown / insufficient evidence" a first-class state, not a low-confidence alert hidden behind a score. |
| **Temporal causal-ish evidence chain** | Shows *why the trajectory changed*: e.g. scan → credential probing → east-west connection fan-out | Stronger than a generic SHAP bar chart because evidence is attached to time windows and graph transitions. |
| **Mobile-first offline command cockpit** | Lets a defender inspect the situation without depending on a cloud SOC UI | Android's official architecture guidance explicitly supports offline-first systems, with local data as the source of truth and synchronization when connectivity returns.[16] |
| **CII / OT profile layer** | Different assets have different consequences and allowed actions | MITRE maintains separate Enterprise and ICS tactics, so the product should understand environment-specific attack semantics.[17,18] |
| **Privacy-preserving fleet learning** | Learn from multiple sites without centralizing raw PCAP | A 2026 paper already demonstrates federated learning with digital twins for DDoS defence, supporting this as a plausible advanced research direction rather than science fiction.[19] |
| **Outcome memory** | Store what happened after a recommendation and use it to calibrate future recommendations | Turns the product from a static model into a continually evaluated decision-support system. |

### The differentiated positioning

> **"Most security platforms tell you what was detected or which attack path exists. VajraWorld shows the most probable future network states, explains the trajectory, and lets the defender simulate how the future changes under a proposed intervention."**

That is a much stronger innovation statement than "we use a Transformer for intrusion detection."

---

## 4. Product name and concept

### Working product name

**VajraWorld — World Model for Predictive Cyber Defence**

Alternative names for later branding:

- VajraForecast
- VajraTwin
- AegisWorld
- SentinelFlow
- CyberTrajectory

### Core product promise

**See the attack before the compromise completes.**

### Three questions the UI must answer immediately

1. **What is happening now?**
2. **What is most likely to happen next?**
3. **What action changes the predicted future with the least operational risk?**

---

## 5. Scope: two deployment modes

### Mode A — Mobile / standalone mode

Designed for demonstration, labs, small environments, and a single Android device.

Data sources:

- Android `VpnService` packet metadata
- DNS observations where available
- Local connection metadata
- User-imported PCAP/CSV
- Wi-Fi/router exports
- Sample datasets

Use cases:

- Scan imported data offline.
- Monitor the Android device's network flows.
- Run a small quantized forecasting model locally.
- View attack trajectory and explanations.

Android's `VpnService` provides a virtual IP interface from which the application can read outgoing packets and write incoming packets; it is therefore a viable foundation for a device-level traffic collector, subject to Android's VPN constraints and careful handling of privacy/security.[20,21]

### Mode B — Enterprise / CII mode

Designed for actual organizations.

Data sources:

- NetFlow / IPFIX
- Zeek logs
- Suricata EVE JSON
- PCAP / SPAN / TAP sensor
- Firewall logs
- DNS telemetry
- Authentication logs
- Endpoint events
- Optional vulnerability/asset inventory
- Optional OT/ICS telemetry

Topology:

```text
                Enterprise / CII Network
                         |
              SPAN / TAP / NetFlow / Logs
                         |
                  [Vajra Sensor]
                         |
             [Feature + State Engine]
                         |
               [World Model Engine]
                         |
          +--------------+--------------+
          |                             |
 [Forecast / ATT&CK]          [Counterfactual Simulator]
          |                             |
          +--------------+--------------+
                         |
                  Secure Local API
                         |
                ==================
                Android Defender
                    Cockpit
                ==================
```

The Android application should **never** become the sole architectural dependency for enterprise sensing.

---

## 6. High-level architecture

```text
┌─────────────────────────────────────────────────────────────┐
│                     DATA / SENSOR LAYER                     │
├─────────────────────────────────────────────────────────────┤
│ NetFlow/IPFIX │ Zeek │ Suricata │ PCAP │ Auth │ DNS │ EDR │
└───────────────────────────┬─────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                 TELEMETRY NORMALIZATION                     │
│ timestamp alignment • schema normalization • deduplication │
│ flow reconstruction • packet aggregation • privacy policy  │
└───────────────────────────┬─────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    FEATURE / STATE ENGINE                   │
│ flow vectors • packet statistics • temporal features       │
│ host embeddings • service embeddings • graph construction  │
└───────────────────────────┬─────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    WORLD MODEL CORE                         │
│ temporal encoder → latent network state z_t                 │
│ dynamic graph encoder → entity/edge context                 │
│ transition model → P(z_t+k | z_t, context)                  │
│ stage head → ATT&CK stage probabilities                     │
│ calibration head → confidence / uncertainty                  │
└───────────────────────────┬─────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  FUTURE SIMULATION ENGINE                   │
│ K-step rollouts • multiple plausible futures • risk paths   │
│ intervention / counterfactual evaluation                    │
└───────────────────────────┬─────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  EXPLAINABILITY ENGINE                      │
│ attention • temporal attribution • SHAP • graph evidence    │
│ "why now" • "why next" • "what changed"                    │
└───────────────────────────┬─────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                     DECISION ENGINE                         │
│ severity • urgency • confidence • intervention cost        │
│ recommended action • reversible action preference           │
└───────────────────────────┬─────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                     ANDROID UX                               │
│ trajectory • network graph • futures • explainability       │
│ incident timeline • playbooks • simulation • audit trail    │
└─────────────────────────────────────────────────────────────┘
```

---

## 7. Recommended technology stack

### Android

**Language:** Kotlin

**UI:** Jetpack Compose + Material 3

**Architecture:** Clean Architecture + MVVM/UDF

**Persistence:** Room + DataStore

**Background:** WorkManager

**Networking:** Kotlin coroutines + Ktor/OkHttp

**Charts:** Compose-native charts or a well-maintained Compose chart library

**Graph rendering:** custom Compose Canvas for the main topology view; WebView/D3 only where necessary

**Local inference:** ONNX Runtime Mobile for the first production prototype, with an evaluation path toward other Android runtimes if model/operator constraints justify it. ONNX Runtime Mobile supports Android inference and offers model/runtime minimization options.[22,23]

**Security:** Android Keystore, certificate pinning where justified, encrypted local storage, signed model bundles, authenticated local API.

Android should be Compose-first for a new application; the Android documentation describes Compose as the modern declarative UI toolkit and the recommended direction for new UI work.[24]

### Edge / backend

**Language:** Python 3.12+ (pin exact minor version in the release environment)

**API:** FastAPI

**ML:** PyTorch

**Data processing:** Polars or pandas + NumPy

**PCAP:** Scapy / PyShark / Zeek-derived logs

**Model export:** ONNX

**Explainability:** SHAP + native attention/temporal attribution

**Graph:** PyTorch Geometric or DGL

**Storage:** PostgreSQL for production; SQLite for demo/offline mode

**Time series / events:** Parquet + object storage locally; optional ClickHouse/Timescale later for high volume

**Message bus:** NATS or Kafka in large deployments; start without a broker for the hackathon prototype

**Packaging:** Docker + Docker Compose

**Observability:** OpenTelemetry + Prometheus + Grafana

**Deployment:** local workstation → edge appliance → Kubernetes only when scale actually requires it

---

## 8. Data model: represent the network as a changing world

The world model must not consume an arbitrary CSV row and call that "state".

### 8.1 Entity graph

Represent the environment as a dynamic graph:

```text
Node types
──────────
Host
Server
User
Service
Subnet
Domain
External-IP
Device
OT-Asset

Edge types
──────────
CONNECTS_TO
AUTHENTICATES_TO
RESOLVES_TO
SCANS
ACCESSES
TRANSFERS_TO
MEMBER_OF
DEPENDS_ON
```

Each node has a feature vector; each edge is a time-stamped event with flow/packet attributes.

### 8.2 Flow-level features

Required baseline fields:

- source/destination IP
- source/destination port
- protocol
- TCP flags
- bytes forward/backward
- packets forward/backward
- duration
- packets/sec
- bytes/sec
- bidirectional ratio
- inter-arrival mean/variance/max
- SYN/ACK/FIN/RST/PSH/URG counts
- connection fan-out
- destination port entropy
- source port entropy
- failed-connection ratio

### 8.3 Packet-derived features

At minimum:

- TTL mean/variance
- TCP window size statistics
- fragment flags
- payload length histogram
- retransmissions
- SYN burstiness
- packet IAT distribution
- directional asymmetry
- TCP handshake completeness
- sequential vs randomized port access score

### 8.4 Higher-order temporal features

These are where the product becomes more interesting:

- new destination count per window
- unique ports touched per host
- east-west fan-out
- failed-to-successful connection ratio
- host-to-host transition frequency
- periodic beacon score
- burst entropy
- inter-event regularity
- new service discovery rate
- unusual protocol transition
- authentication failure → successful login gap
- rare communication pair score
- graph centrality drift
- asset criticality weighted connectivity

### 8.5 State vector

A practical state is:

```text
S_t = {
  global_features_t,
  node_embeddings_t,
  edge_embeddings_t,
  temporal_features_t,
  asset_context_t,
  security_control_state_t
}
```

The final learned representation is a compact latent state:

```text
z_t = Encoder(S_t)
```

The world model learns:

```text
p(z[t+1] | z[t], c[t])
```

and rolls this forward:

```text
z[t+1] → z[t+2] → ... → z[t+K]
```

---

## 9. World-model architecture: recommended implementation

### Version 1 production prototype

Use a **Hybrid Temporal Graph World Model**.

```text
Events / features
       |
       v
Dynamic Graph Builder
       |
       v
Temporal Graph Encoder
       |
       +---- node states
       +---- edge states
       |
       v
Temporal Transformer
       |
       v
Latent World State z_t
       |
       +------------------+
       |                  |
       v                  v
Transition Head      Stage Head
       |                  |
       v                  v
z(t+k) distribution   ATT&CK probabilities
       |
       v
Uncertainty Head
       |
       v
Forecast + confidence
```

### Why this architecture

A pure LSTM is easier to explain in a classroom demo, but the network itself is a graph. A pure Transformer can forecast well but risks becoming expensive and difficult to deploy. A dynamic graph encoder preserves relationships while a temporal sequence model preserves the attack's unfolding over time.

Temporal Graph Networks provide a principled starting point for time-stamped dynamic graph events.[12] A temporal attention block provides a natural route to multi-step, multi-horizon forecasting and interpretable temporal relationships.[13]

### Suggested model components

**1. Node/edge feature encoders**

- MLP for continuous fields
- embeddings for protocol / flag combinations / service categories
- bucketized ports or learned service identities

**2. Dynamic graph encoder**

Start with GraphSAGE/GAT or a temporal message-passing layer.

**3. Temporal encoder**

Two viable tracks:

- Track A: Temporal Transformer
- Track B: GRU/LSTM baseline

The baseline must remain because the challenge asks for comparison and it provides an interpretable engineering trade-off.

**4. Latent transition model**

Output a parameterized distribution over the next latent state rather than only a class label.

A practical first version can use:

```text
μ_t+1 = fμ(z_t)
σ_t+1 = fσ(z_t)
```

then sample/roll out multiple future paths.

**5. Stage head**

Predict the probability distribution over a curated stage set:

```text
Benign / Reconnaissance / Initial Access / Discovery /
Credential Access / Lateral Movement / C2 / Collection /
Exfiltration / Impact / Unknown
```

MITRE ATT&CK's Enterprise matrix currently contains 15 tactics, including Reconnaissance, Initial Access, Discovery, Lateral Movement, Command and Control, Exfiltration, and Impact.[25,26]

**6. Uncertainty head**

Model at least:

- epistemic uncertainty proxy
- aleatoric uncertainty proxy
- prediction interval / entropy
- OOD score

Do not display a raw confidence percentage as if it were truth. Use calibrated probabilities.

---

## 10. Training objective

Do not train only on `y = malicious`.

Use a composite objective:

```text
L_total =
  λ1 * L_transition
+ λ2 * L_stage
+ λ3 * L_future_attack
+ λ4 * L_reconstruction
+ λ5 * L_calibration
+ λ6 * L_contrastive
```

### 10.1 Transition loss

Learn the next-state distribution:

```text
L_transition = NLL(S[t+1] | S[t])
```

### 10.2 Stage loss

Cross entropy or focal loss over ATT&CK-aligned stages.

### 10.3 Future attack loss

For each horizon `k`:

```text
P(attack in t+k)
```

Use a multi-horizon BCE/focal objective.

### 10.4 Reconstruction auxiliary loss

Force the latent state to preserve enough information to reconstruct important observable state statistics.

### 10.5 Calibration loss

Use Brier score / expected calibration error (ECE) as monitoring metrics and a post-hoc calibrator such as temperature scaling on the validation set.

### 10.6 Contrastive temporal loss

Encourage temporally adjacent states to be more similar than unrelated states while preserving meaningful attack transitions.

---

## 11. Dataset strategy

### Primary datasets

**CSE-CIC-IDS2018**

The University of New Brunswick describes the dataset as containing seven attack scenarios, 50 attacking machines, 420 victim machines, 30 servers, raw PCAP/network traffic, system logs, and more than 80 extracted traffic features.[27]

**CTU-13**

The Stratosphere Laboratory describes CTU-13 as 13 botnet captures containing botnet, normal, and background traffic, with labeled bidirectional flows and available PCAP subsets.[28,29]

### Secondary datasets to improve generalization

Add at least two additional telemetry families during development, such as:

- IoT-23
- CTU-SME-11
- Android Mischief
- selected Zeek/Suricata public datasets

Stratosphere publishes CTU-13 plus IoT-23, CTU-SME-11, Android Mischief, and other real labelled traffic datasets.[30]

### Critical dataset warning

Do **not** randomly split individual flows from the same attack campaign across train and test. That can make temporal prediction look artificially good.

Use:

- scenario-level split
- day-level split
- campaign-level split when available
- leave-one-attack-family-out evaluation
- cross-dataset evaluation

The challenge's strongest evidence will be **generalization to unseen attack patterns**, not a high F1 score on a random row split.

---

## 12. Building attack timelines from datasets

The training pipeline should transform labelled traffic into windows.

Example:

```text
Raw events
  ↓
5-second event bins
  ↓
30-second rolling state windows
  ↓
state_001, state_002, state_003...
  ↓
attack annotations aligned to each state
```

For every state window:

```json
{
  "timestamp": "...",
  "nodes": 128,
  "edges": 492,
  "features": [...],
  "label_now": "reconnaissance",
  "label_future_1": "initial_access",
  "label_future_2": "lateral_movement",
  "time_to_next_stage_sec": 83.0
}
```

This creates a training target that genuinely represents **progression**.

---

## 13. Forward simulation

The forward simulator is the feature that should dominate the demo.

Input:

```text
current network state S_t
horizon K
N stochastic rollouts
```

Example output:

```text
Horizon         Likelihood of harmful progression
+30 sec         0.18
+60 sec         0.31
+90 sec         0.57
+120 sec        0.74
+150 sec        0.81
```

The app should simultaneously show:

```text
Most likely stage at +120s: Lateral Movement
Confidence: calibrated / medium-high
Critical asset at risk: Finance-DB-02
```

### Multi-future view

Instead of only one path, show:

```text
                ┌─ benign stabilization 28%
Current ────────┼─ reconnaissance loop 14%
                ├─ credential abuse 19%
                └─ lateral movement → critical asset 39%
```

This makes the world-model concept visible to evaluators.

---

## 14. Counterfactual Defence Simulator — key innovation

Add a dedicated **"Test Defence"** action.

Suppose the forecast is:

```text
Host-A → Host-B → AD-01 → Finance-DB
```

The user can test:

- isolate Host-A
- block destination port 445
- disable a compromised account
- segment the subnet
- block an external C2 domain
- rate-limit suspicious traffic
- quarantine an endpoint

Then run the world model again with the modified environment:

```text
Future risk before: 78%
Future risk after isolation: 24%
Residual risk: 24%
New likely path: C2 only
```

This is much more valuable than a simple "Recommended action" card because it asks the model to estimate the effect of the action.

### Safety principle

The simulator is **read-only by default**. It cannot modify production controls unless the user explicitly enables an integrated response connector.

---

## 15. Explainability design

A single SHAP bar chart is not enough for a production-quality UX.

Use a layered explanation system.

### Level 1 — Human summary

> "The forecast increased because Host-17 contacted 31 new internal destinations in 45 seconds, the RST/SYN ratio changed sharply, and a previously rare SMB connection appeared between two high-value segments."

### Level 2 — Feature attribution

Top contributors:

```text
+0.19  internal destination fan-out
+0.13  new SMB edge
+0.11  SYN burstiness
+0.09  failed authentication ratio
-0.04  known management pattern
```

SHAP is an established general approach for assigning feature importance to individual predictions.[14]

### Level 3 — Temporal evidence

Highlight exactly where the trajectory changed:

```text
T-120s   normal
T-90s    low anomaly
T-60s    recon signal ↑
T-30s    credential signal ↑↑
T-00s    lateral-movement probability ↑↑↑
T+30s    predicted pivot
```

### Level 4 — Graph evidence

Highlight the minimal subgraph contributing to the forecast.

### Level 5 — Model uncertainty

Display:

- forecast entropy
- data quality score
- OOD warning
- missing telemetry warning

Example:

> **Forecast confidence reduced:** authentication logs are unavailable for 42% of the affected hosts.

This is important for CII environments where missing visibility is itself operationally meaningful.

---

## 16. CII / OT extension

The product should have an **environment profile** rather than pretending that one universal risk model fits every environment.

### Profiles

- Enterprise IT
- Data Center
- Cloud / Hybrid
- IoT
- OT / ICS
- Critical Infrastructure
- Campus / SME

MITRE maintains separate Enterprise and ICS ATT&CK knowledge, including ICS-specific tactics such as Inhibit Response Function and Impair Process Control.[17,18]

### OT safety guardrail

For OT mode, the world model should primarily recommend:

- observe
- verify
- segment
- block only with policy approval
- avoid direct automated actuation

The product must not turn an uncertain AI forecast into autonomous industrial control.

---

## 17. Android application UX blueprint

The Android UI should look like a **professional security cockpit**, not a generic antivirus app.

Use a dark technical interface with restrained accent colors. Avoid excessive neon/glow. The visual language should emphasize:

- network topology
- time progression
- future trajectories
- confidence/uncertainty
- high-value asset relationships

The visual references researched show strong current mobile-security patterns around dark dashboards, incident timelines, AI suggested actions, network activity, and security scores; these are useful aesthetic references but not templates to copy.[31–33]

### Navigation

Bottom navigation:

```text
[Overview] [Trajectory] [Network] [Incidents] [More]
```

### Screen 1 — Overview

Top:

```text
VAJRAWORLD                           ● LIVE

Network Health                  Forecast Risk
82/100                          68%
Stable                          ↑ +14%
```

Center:

```text
NEXT 10 MINUTES

     ┌─────────── forecast ───────────┐
  0% │ ▁▂▃▄▅▆█████████              │ 100%
     └─────────────────────────────────┘

Predicted stage: Lateral Movement
ETA: 2m 14s
```

Then:

- Critical asset at risk
- Top 3 driving signals
- Current active incidents
- Data quality

### Screen 2 — Trajectory

This is the flagship screen.

A horizontal time axis:

```text
NOW ───── +30s ───── +60s ───── +90s ───── +120s
  │          │           │           │            │
  ●──────────●───────────●───────────●────────────●
  recon      discovery   creds       lateral      C2
```

Allow pinch/zoom.

### Screen 3 — Future branches

Display several simulated futures.

```text
FUTURE BRANCHES

72%  → internal pivot → Finance-DB
18%  → benign stabilization
10%  → external C2 only
```

Tap a branch to inspect its evidence.

### Screen 4 — Network world

Interactive graph:

- hosts as circles
- services as small nodes
- edges animated by activity
- suspicious path highlighted
- critical assets visually marked

Do not render hundreds/thousands of nodes directly on the phone. Use level-of-detail clustering.

### Screen 5 — Incident detail

Header:

```text
INC-2041
Predicted intrusion progression
Risk 81%
Confidence: 74%
```

Sections:

1. What is happening
2. What the model expects next
3. Why the model thinks so
4. Affected assets
5. ATT&CK mapping
6. Test a defence
7. Recommended next step
8. Audit history

### Screen 6 — Test Defence

A large simulator card:

```text
SIMULATE DEFENCE

Action
[ Isolate Host-17 ▼ ]

Predicted risk
78% → 23%

Potential trade-off
Business interruption: medium

[ RUN SIMULATION ]
```

### Screen 7 — Evidence

Show a ranked timeline of traffic events and telemetry.

### Screen 8 — Model health

For production deployments:

- model version
- last calibration
- telemetry freshness
- OOD rate
- sensor coverage
- drift score
- inference latency
- CPU/memory
- failed predictions

This makes the product look like a real platform rather than a hackathon model wrapped in an app.

---

## 18. Android offline-first architecture

Use the official offline-first pattern: local data is the source of truth, network synchronization is secondary, and persistent work can be scheduled with WorkManager.[16]

```text
Compose UI
   ↓
ViewModel / StateFlow
   ↓
Use Cases
   ↓
Repositories
   ↓
Room ←── sync queue ──→ Secure API
   ↓
Local forecast cache
```

### Offline behavior

The app should remain useful when disconnected:

- view recent incidents
- view last known topology
- inspect cached forecasts
- run local small-model inference where supported
- review evidence
- create response drafts
- queue approved actions for later transmission

### Sync states

```text
SYNCED
STALE
OFFLINE
CONFLICT
PARTIAL
UNTRUSTED
```

Never silently show stale predictions as current.

---

## 19. Backend API blueprint

### Authentication

Use short-lived access tokens with refresh tokens or mutual TLS in enterprise mode.

### Core endpoints

```http
POST /v1/telemetry/flow
POST /v1/telemetry/batch
POST /v1/telemetry/pcap/analyze

GET  /v1/state/current
GET  /v1/state/{timestamp}
GET  /v1/graph/current

POST /v1/forecast
GET  /v1/forecast/{id}
GET  /v1/forecast/{id}/explanations

POST /v1/simulation
GET  /v1/simulation/{id}

GET  /v1/incidents
GET  /v1/incidents/{id}
POST /v1/incidents/{id}/ack

GET  /v1/model/status
GET  /v1/model/versions

GET  /v1/assets
GET  /v1/assets/{id}

GET  /v1/audit
```

### Forecast request

```json
{
  "state_id": "state_2026_09_11_231005",
  "horizon_steps": 12,
  "rollouts": 64,
  "environment_profile": "enterprise"
}
```

### Forecast response

```json
{
  "risk": {
    "current": 0.68,
    "horizon": [0.21, 0.34, 0.47, 0.59, 0.68, 0.73],
    "calibration": "temperature_v3",
    "uncertainty": 0.18
  },
  "predicted_stage": {
    "name": "Lateral Movement",
    "probability": 0.74
  },
  "critical_assets": ["asset-finance-db-02"],
  "drivers": [
    {"feature": "east_west_fanout", "impact": 0.19},
    {"feature": "smb_edge_novelty", "impact": 0.13}
  ],
  "model_version": "vw-0.8.0"
}
```

---

## 20. Storage schema

### PostgreSQL tables

```text
assets
flows
packets_summary
telemetry_events
state_windows
state_nodes
state_edges
forecasts
forecast_paths
forecast_explanations
incidents
simulations
simulation_actions
model_versions
model_calibration
audit_events
users
roles
policies
```

### Key principle

Separate:

1. **raw evidence**
2. **derived features**
3. **learned state**
4. **predictions**
5. **recommended actions**
6. **executed actions**

This preserves forensic integrity and enables post-incident model auditing.

---

## 21. Explainability record schema

```json
{
  "forecast_id": "f_1039",
  "temporal_drivers": [
    {
      "window": "t-60s",
      "feature": "destination_port_entropy",
      "direction": "up",
      "impact": 0.12
    }
  ],
  "graph_drivers": [
    {
      "source": "host-17",
      "destination": "ad-01",
      "relation": "CONNECTS_TO",
      "impact": 0.21
    }
  ],
  "evidence_ids": ["evt-1021", "evt-1034", "evt-1049"],
  "missing_data": ["auth_logs:42%_missing"]
}
```

This allows the Android UI to move from a score to an evidence-backed story.

---

## 22. Production model-serving strategy

### Edge inference

The production path should keep raw traffic local whenever possible.

```text
raw telemetry
   ↓
local feature extraction
   ↓
world model inference
   ↓
small summaries + alerts
   ↓
Android
```

Only send raw PCAP upstream when explicitly requested.

### Android inference

Use a **small distilled model** for mobile:

- current risk estimate
- short-horizon stage prediction
- local anomaly scoring

Keep the full hybrid world model on the edge.

ONNX Runtime Mobile supports Android inference and can be built with a reduced operator/runtime footprint when needed.[22,23]

---

## 23. Model lifecycle

Do not overwrite the running model silently.

Every model bundle should have:

```text
model_id
version
training_data_manifest
feature_schema_version
normalization_version
ATT&CK mapping version
calibration version
checksum
signature
training commit
validation metrics
```

### Deployment gate

A new model must pass:

- accuracy regression
- calibration regression
- latency regression
- memory regression
- false-positive regression
- unseen-attack evaluation
- explainability sanity tests
- adversarial robustness checks

Only then should it move from `candidate` to `production`.

---

## 24. Benchmarking plan

### Baselines

Required:

1. Logistic Regression
2. Random Forest
3. XGBoost or LightGBM
4. LSTM
5. Temporal Transformer
6. Hybrid Temporal Graph World Model

### Metrics

#### Detection

- precision
- recall
- F1
- AUROC
- AUPRC
- false-positive rate

#### Forecasting

- horizon-wise precision/recall
- Brier score
- calibration error
- MAE/RMSE for selected state variables
- time-to-warning

#### Progression

- stage accuracy
- macro-F1
- next-stage top-k accuracy
- time-to-next-stage error

#### Operational

- inference latency
- CPU/memory
- energy consumption on Android for mobile mode
- data reduction ratio

### The most important benchmark

**Lead time before the attack stage becomes observable**.

Example:

```text
baseline classifier:
  warning = 0–10 sec before confirmed stage

VajraWorld:
  warning = 75 sec before confirmed stage
```

Do not invent a result. Measure it.

---

## 25. Generalization experiments

The evaluation should include:

### Experiment A — Standard split

Train/validation/test by scenario.

### Experiment B — Unseen attack family

Hold out an attack family from training.

### Experiment C — Cross-dataset

Train on CIC-IDS2018, test on CTU-13 or another dataset after compatible feature mapping.

### Experiment D — Sensor degradation

Remove:

- 20% telemetry
- 40% telemetry
- authentication logs
- DNS logs
- packet-derived features

Measure forecast degradation.

### Experiment E — Adversarially slow attack

Inject longer inter-arrival times and lower-intensity scans.

### Experiment F — Concept drift

Train on one period and evaluate on another.

This directly supports the claim that VajraWorld learns dynamics instead of memorizing signatures.

---

## 26. Calibration and uncertainty

This deserves a dedicated dashboard.

### Risk states

```text
HIGH RISK / HIGH CONFIDENCE
HIGH RISK / LOW CONFIDENCE
LOW RISK / HIGH CONFIDENCE
LOW RISK / LOW CONFIDENCE
```

The last state is important: **"we do not know"** should be visible.

### Missing telemetry badge

Example:

> **Coverage degraded** — 36% of affected host traffic unavailable. Forecast may under-estimate lateral movement.

This is a useful product philosophy for safety-critical environments.

---

## 27. Attack-stage mapping design

Do not simply map every anomaly to an ATT&CK stage.

Use a two-layer approach:

```text
traffic evidence
      ↓
behavior primitives
      ↓
technique hypotheses
      ↓
tactic / stage probabilities
      ↓
forecasted progression
```

Example:

```text
Repeated destination probing
        ↓
Active Scanning hypothesis
        ↓
Reconnaissance probability ↑
        ↓
Possible transition to Discovery / Initial Access
```

MITRE describes Reconnaissance as information gathering for later operations and Lateral Movement as entering/controlling remote systems to progress toward objectives.[25,26] The system should preserve these semantic meanings rather than using ATT&CK as a cosmetic label layer.

---

## 28. Alert design: reduce alert fatigue

Avoid:

```text
ALERT: Suspicious traffic detected
```

Prefer:

```text
PREDICTED PROGRESSION

Current: Reconnaissance
Likely next: Discovery
Likely in: 65–110 sec
Affected path: Host-17 → AD-01 → Finance-DB-02
Probability: 74%

Why:
• 31 new internal destinations / 45s
• unusual SMB edge appeared
• failed authentication burst

Suggested first action:
Isolate Host-17 (estimated risk reduction 54%)
```

This is much closer to a decision-support product.

---

## 29. AI recommendation safety

The AI should recommend actions in this order:

1. **Observe**
2. **Confirm**
3. **Contain with reversible action**
4. **Escalate**
5. **Destructive action only with explicit policy approval**

The action engine should score:

```text
security benefit
operational disruption
confidence
reversibility
asset criticality
policy permission
```

Then:

```text
Action Utility =
security_benefit × confidence
- disruption_cost
- irreversibility_cost
```

This is a product design principle, not a claim that the formula is objectively correct; tune it with domain experts and evaluate it empirically.

---

## 30. Privacy architecture

### Default

- local processing
- no raw PCAP upload
- pseudonymize IP addresses when exporting research data
- separate identity mapping from ML features
- immutable audit log for response actions

### Enterprise privacy tiers

```text
Tier 0: local only
Tier 1: metadata only
Tier 2: selected evidence upload
Tier 3: managed SOC integration
```

The operator chooses the tier.

---

## 31. Security of VajraWorld itself

A security product becomes dangerous if its own control plane is weak.

Implement:

- signed model packages
- secure boot expectations for appliance deployments
- TLS everywhere outside an explicitly isolated local process
- mTLS for enterprise sensor-to-engine links
- RBAC
- least privilege
- secrets in Android Keystore / server secret manager
- immutable audit records
- rate limits
- replay protection for telemetry
- input validation for PCAP and log parsers
- sandboxed packet parsing where practical
- update signature verification
- rollback support

Never let imported PCAP data execute arbitrary code.

---

## 32. Repository structure

```text
vajraworld/
│
├── android/
│   ├── app/
│   ├── core-ui/
│   ├── core-model/
│   ├── core-network/
│   ├── core-database/
│   ├── feature-overview/
│   ├── feature-trajectory/
│   ├── feature-network/
│   ├── feature-incidents/
│   ├── feature-simulation/
│   ├── feature-settings/
│   └── benchmark/
│
├── edge/
│   ├── api/
│   ├── collectors/
│   │   ├── netflow/
│   │   ├── zeek/
│   │   ├── suricata/
│   │   └── pcap/
│   ├── feature_engine/
│   ├── graph_engine/
│   ├── world_model/
│   ├── forecasting/
│   ├── explainability/
│   ├── calibration/
│   ├── simulation/
│   ├── attack_mapping/
│   └── storage/
│
├── training/
│   ├── datasets/
│   ├── preprocessing/
│   ├── windowing/
│   ├── models/
│   ├── losses/
│   ├── evaluation/
│   ├── explainability/
│   └── configs/
│
├── datasets/
│   ├── manifests/
│   └── schemas/
│
├── deployment/
│   ├── docker/
│   ├── k8s/
│   └── edge-appliance/
│
├── docs/
│   ├── architecture.md
│   ├── api.md
│   ├── threat-model.md
│   ├── model-card.md
│   └── demo-script.md
│
└── README.md
```

---

## 33. Core Android modules

### `feature-overview`

Owns:

- risk summary
- forecast chart
- top incidents
- sensor health

### `feature-trajectory`

Owns:

- timeline
- future branches
- stage transitions
- time-to-event

### `feature-network`

Owns:

- graph
- asset detail
- path inspection

### `feature-incidents`

Owns:

- alerts
- evidence
- acknowledgement
- action history

### `feature-simulation`

Owns:

- counterfactual actions
- simulation results
- trade-off display

---

## 34. Android state model

Use a single immutable screen state per feature.

Example:

```kotlin
data class TrajectoryUiState(
    val currentRisk: Float = 0f,
    val horizons: List<ForecastPoint> = emptyList(),
    val branches: List<FutureBranch> = emptyList(),
    val predictedStage: AttackStage? = null,
    val confidence: Float = 0f,
    val coverage: CoverageState = CoverageState.Unknown,
    val loading: Boolean = false,
    val error: String? = null
)
```

The UI should render from state rather than embedding inference/business logic inside composables.

---

## 35. PCAP ingestion pipeline

```text
PCAP
 ↓
parser
 ↓
packet events
 ↓
flow reconstruction
 ↓
window aggregation
 ↓
feature validation
 ↓
normalization
 ↓
graph update
 ↓
world-model inference
 ↓
forecast
```

### Required validation

- malformed packets
- timestamp order
- duplicate frames
- missing fields
- IP fragmentation edge cases
- unsupported protocols
- packet count mismatch

Provide a report:

```text
Parsed packets: 4,201,115
Reconstructed flows: 183,402
Dropped malformed: 281
Window count: 7,822
Telemetry coverage: 94%
```

---

## 36. Feature engineering contract

The feature schema must be versioned.

```yaml
schema_version: 1.4
features:
  - name: syn_rate
    type: float
    source: flow
    normalization: zscore
  - name: dst_port_entropy
    type: float
    source: flow
    normalization: robust
  - name: ttl_variance
    type: float
    source: packet
    normalization: robust
```

A model must refuse to run when the feature schema is incompatible rather than silently using the wrong columns.

---

## 37. Innovation backlog beyond the challenge

### Innovation 1 — Trajectory fingerprint

Instead of a signature for a packet, store a low-dimensional representation of an attack's **trajectory shape**.

Example:

```text
slow recon → bursty discovery → lateral expansion → C2
```

This may generalize better than fixed signatures.

### Innovation 2 — Future disagreement detector

Run two independent predictors:

- graph model
- sequence model

If they disagree strongly, surface:

> **Model disagreement: investigate manually.**

This turns ensemble disagreement into a useful risk signal.

### Innovation 3 — Defender action effectiveness memory

Record:

```text
predicted effect
actual effect
```

Over time the platform learns which interventions are reliable in that environment.

### Innovation 4 — Exposure-aware forecasting

Inject environment context:

- asset criticality
- reachability
- exposed ports
- segmentation
- known vulnerabilities
- identity privilege

Then predict not only "attack likely" but "attack likely to reach this critical asset".

### Innovation 5 — Missingness-aware world model

Do not impute missing telemetry as if it were normal. Make missingness itself an input.

Example:

```text
30% DNS data missing
→ forecast uncertainty increases
```

### Innovation 6 — Human-in-the-loop forecast correction

Allow analyst feedback:

```text
[Agree] [Wrong stage] [Benign] [Missing context]
```

Store the correction and use it for evaluation/retraining.

### Innovation 7 — Federated world-model learning

For multiple enterprises:

```text
Site A ─┐
Site B ─┼─ federated updates ─→ global model
Site C ─┘
```

No raw PCAP leaves the site.

A 2026 Scientific Reports paper, FL-TWIN, demonstrates a related combination of federated learning, digital twins, poisoning defense, and model-version rollback for DDoS detection.[19]

### Innovation 8 — Attack weather forecast

A new executive view:

```text
Cyber Weather
─────────────
Recon pressure      HIGH
Credential pressure MEDIUM
Lateral pressure    RISING
Exfil pressure      LOW

Next 15 min outlook
████████░░ 78%
```

This gives leadership a useful abstraction while analysts can drill down to evidence.

---

## 38. Demo flow for the competition

### Minute 0:00–0:20

Show a normal network.

```text
Risk 12%
Forecast stable
```

### Minute 0:20–0:40

Replay reconnaissance traffic.

The model notices a trajectory change before the final stage.

### Minute 0:40–1:00

Show:

```text
Now: Reconnaissance
Next predicted: Discovery
ETA: ~60 sec
```

### Minute 1:00–1:20

Open network graph and explain:

- new destinations
- unusual port pattern
- temporal burst

### Minute 1:20–1:40

Press **Test Defence**.

Simulate isolating the compromised host.

Show risk:

```text
74% → 21%
```

### Minute 1:40–2:00

Close with:

> "The system did not wait for the final malicious event. It learned the evolving network state, forecast what could happen next, showed why, and simulated how a defender could change the future."

Do not present fabricated performance numbers.

---

## 39. Five-slide technical presentation

### Slide 1 — Problem

**Traditional IDS sees events. VajraWorld models trajectories.**

Diagram:

```text
Static classifier
flow → malicious?

VajraWorld
state_t → state_t+1 → state_t+2 → attack progression
```

### Slide 2 — Architecture

Show:

```text
Telemetry → State/Graph → World Model → Forecast → Explain → Simulate
```

### Slide 3 — World model

Show:

- dynamic graph
- temporal encoder
- latent transition model
- K-step rollout

### Slide 4 — Android experience

Three phone mockups:

1. forecast dashboard
2. trajectory map
3. counterfactual defence

### Slide 5 — Evidence

Show measured results:

- F1
- false-positive rate
- calibration
- mean warning lead time
- unseen attack performance

Only show real measured numbers.

---

## 40. Two-page architecture document structure

### Page 1

- Problem
- Product thesis
- System architecture
- Data flow
- World-model design
- Android + edge deployment

### Page 2

- Forecast outputs
- ATT&CK mapping
- Explainability
- Counterfactual simulation
- Evaluation plan
- CII safety guardrails

---

## 41. README structure

```text
# VajraWorld

## What it does
## Why it is different
## Architecture
## Quick start
## Dataset preparation
## Train the baseline
## Train the world model
## Run inference
## Run the Android app
## Load a PCAP
## Run a forecast
## Run a defence simulation
## Benchmark
## Model card
## Security
## Offline mode
## License
```

Include a single-command demo path:

```bash
make demo
```

The demo should start:

- sample telemetry generator
- local inference API
- Android-compatible endpoint
- sample attack replay

---

## 42. Recommended development roadmap

### Phase 0 — Research skeleton

Deliver:

- dataset manifests
- feature schema
- timeline builder
- baseline logistic regression
- baseline LSTM

### Phase 1 — Challenge MVP

Deliver:

- CIC-IDS2018 ingestion
- CTU-13 ingestion
- temporal windows
- Transformer/LSTM forecasting
- ATT&CK stage head
- SHAP/attention explanation
- Streamlit reference UI

### Phase 2 — Android product

Deliver:

- Kotlin + Compose application
- local database
- forecast dashboard
- trajectory view
- graph view
- incident details
- secure API

### Phase 3 — World-model differentiator

Deliver:

- dynamic graph encoder
- latent transition model
- multi-rollout simulation
- calibrated uncertainty
- counterfactual defence simulator

### Phase 4 — Production hardening

Deliver:

- signed model artifacts
- RBAC
- audit log
- mTLS
- model registry
- OpenTelemetry
- drift detection
- deployment automation

### Phase 5 — CII / enterprise

Deliver:

- OT profile
- asset criticality
- vulnerability context
- Zeek + Suricata connectors
- policy engine
- human approval workflows

### Phase 6 — Research innovation

Deliver:

- federated learning
- outcome memory
- model disagreement detector
- cross-site adaptation
- future-branch quality metrics

---

## 43. Production readiness checklist

### Data

- [ ] schema versioning
- [ ] timestamp synchronization
- [ ] raw evidence retention policy
- [ ] sensor health
- [ ] privacy controls

### ML

- [ ] no row leakage across attack scenarios
- [ ] calibrated risk
- [ ] OOD detection
- [ ] unseen-family evaluation
- [ ] drift monitoring
- [ ] model signing

### Android

- [ ] offline-first operation
- [ ] encrypted local storage
- [ ] secure API client
- [ ] accessible UI
- [ ] adaptive layout
- [ ] low-power mode
- [ ] crash reporting without sensitive telemetry leakage

### Backend

- [ ] auth/RBAC
- [ ] audit logging
- [ ] rate limiting
- [ ] parser sandboxing
- [ ] health checks
- [ ] versioned APIs
- [ ] database backup

### CII

- [ ] environment profile
- [ ] read-only default
- [ ] human approval
- [ ] safety policy
- [ ] degraded-telemetry warning

---

## 44. Critical technical decisions

### Decision 1 — Do not put the full model in Android

Use the Android app as a control and visualization surface. Keep the full dynamic-graph world model at the edge. Distill only a compact model for optional offline mobile inference.

### Decision 2 — Do not start with a huge Transformer

Start with a credible baseline hierarchy:

```text
LogReg → LSTM → Temporal Transformer → Temporal Graph World Model
```

Then prove that each additional layer gives measurable value.

### Decision 3 — Make prediction measurable as lead time

The central KPI should be:

> **How much earlier does the system warn about meaningful progression while keeping false positives acceptable?**

### Decision 4 — Build simulation early

Even if the first simulation is simplified, expose the concept early because it is a key differentiator.

### Decision 5 — Treat uncertainty as a product feature

Security teams need a system that can say:

> "I need more evidence."

---

## 45. The single strongest product narrative

A good final story is:

```text
Traditional IDS
      ↓
What happened?

NDR / XDR
      ↓
What is happening?

VajraWorld
      ↓
What is likely to happen next?
      ↓
Why?
      ↓
What if we intervene now?
      ↓
Which action changes the predicted future most safely?
```

This does not claim to replace NDR/XDR. It positions VajraWorld as a **predictive decision layer** that can sit on top of network/security telemetry and, eventually, integrate with existing security controls.

---

## 46. Research novelty statement for paper/hackathon

A defensible novelty statement is:

> **VajraWorld treats enterprise network security as a partially observed temporal environment and learns a dynamic latent world model over evolving network graphs. The model forecasts multiple future network states, maps predicted trajectories to adversarial stages, quantifies uncertainty, explains trajectory-driving evidence, and evaluates counterfactual defensive interventions before execution.**

Do not claim that the general ideas of temporal GNNs, attack-path analysis, XAI, or digital twins are new. They are established. The novelty lies in the **integrated operational workflow, explicit learned state-transition forecasting, intervention simulation, and mobile-first decision interface**.

---

## 47. Research limitations to disclose

A production-quality proposal should openly acknowledge:

1. Public benchmark datasets do not perfectly represent modern enterprise/CII environments.
2. ATT&CK labels are semantic abstractions and may not be directly observable from network telemetry alone.
3. PCAP-derived features can be missing under encryption, sensor placement limitations, sampling, or packet loss.
4. A forecast is probabilistic; it is not an oracle.
5. Counterfactual simulation is only as good as the learned environment model and intervention assumptions.
6. Cross-dataset feature compatibility is a non-trivial engineering problem.
7. Enterprise deployment requires strong privacy, access-control, logging, and model-governance practices.

These limitations actually improve credibility in an evaluation.

---

## 48. Suggested initial package structure for the first public release

```text
VajraWorld-v1/

01_Android_App/
02_Edge_Service/
03_World_Model_Training/
04_Datasets_and_Manifests/
05_Demo_Replay/
06_Benchmarks/
07_Docs/
08_Docker/
09_Model_Cards/
10_Security/
```

### Public demo package

Include a synthetic/sample replay so evaluators can run the complete pipeline without downloading huge PCAP files.

---

## 49. Minimum viable feature set for a strong competition submission

If development time becomes limited, prioritize in this order:

### Must-have

- CIC-IDS2018 ingestion
- temporal state windows
- LSTM/Transformer transition model
- K-step forecast
- attack-stage mapping
- explainability
- Android trajectory dashboard
- baseline benchmark

### Strong differentiators

- dynamic graph
- uncertainty
- counterfactual defence simulation
- critical asset impact

### Advanced

- federated learning
- OT/CII profile
- model disagreement
- outcome memory
- automated integrations

---

## 50. Final recommended architecture

```text
                      ┌─────────────────────┐
                      │     ANDROID APP     │
                      │  Compose / Kotlin   │
                      │                     │
                      │ Overview            │
                      │ Trajectory          │
                      │ Network Graph       │
                      │ Incidents           │
                      │ Explainability      │
                      │ Test Defence        │
                      │ Offline Cache       │
                      └──────────┬──────────┘
                                 │ TLS / mTLS
                                 ▼
                   ┌──────────────────────────┐
                   │   VAJRAWORLD EDGE CORE   │
                   ├──────────────────────────┤
                   │ API Gateway              │
                   │ Telemetry Ingestion      │
                   │ Feature Engine           │
                   │ Graph Builder            │
                   │ State Store              │
                   │ World Model              │
                   │ Forecast Engine          │
                   │ ATT&CK Mapper            │
                   │ Explainability           │
                   │ Counterfactual Simulator │
                   │ Policy Engine             │
                   │ Audit / Model Registry   │
                   └────────────┬─────────────┘
                                │
              ┌─────────────────┼─────────────────┐
              ▼                 ▼                 ▼
          NetFlow/IPFIX       Zeek            Suricata
              │                 │                 │
              └──────────────┬──┴─────────────────┘
                             ▼
                           PCAP
```

---

# Sources

1. ExtraHop, **Network Detection and Response: How RevealX Detects Threats** — behavioral machine learning, threat detection, and post-compromise detection. https://www.extrahop.com/resources/papers/how-revealx-detects-threats-whitepaper
2. ExtraHop, **Threat Detection and Response** — NDR, behavioral analytics, prioritization, and response. https://www.extrahop.com/use-cases/threat-detection-and-response
3. Vectra AI, **Threat Detection, Investigation & Response** — behavioral detection across the attack chain and attack progression. https://www.vectra.ai/platform/threat-detection-investigation-response
4. Vectra AI, **How security teams use the MITRE ATT&CK framework** — ATT&CK-aligned behavior correlation over time and attack progression. https://www.vectra.ai/topics/mitre-attack
5. Darktrace, **Cyber AI Analyst** — AI-assisted investigations and autonomous response. https://www.darktrace.com/cyber-ai-analyst
6. Corelight, **Open NDR Platform** — network evidence, ML/behavioral detection, explainability, governed AI investigations, and response. https://corelight.com/platform/
7. CrowdStrike, **Predictive Path Analysis** — predictive attack-path analysis and dynamic visualization. https://www.crowdstrike.com/en-us/platform/exposure-management/attack-path-analysis/
8. CrowdStrike, **Falcon Exposure Management** — attack-path analysis and predictive risk/exploitability capabilities. https://www.crowdstrike.com/en-us/platform/exposure-management/continuous-threat-exposure-management-ctem/
9. CrowdStrike, **AI-driven risk prioritization** — ExPRT.AI, adversary context, and attack-path analysis. https://www.crowdstrike.com/en-us/blog/falcon-exposure-management-ai-driven-risk-prioritization-shows-what-to-fix-first/
10. CrowdStrike, **Platform innovations** — Attack Path Analysis and adversary behavior prediction. https://www.crowdstrike.com/en-us/press-releases/crowdstrike-falcon-platform-innovations/
11. CrowdStrike, **SafeMind / Frontier Models for Cybersecurity**, September 1, 2026. https://www.crowdstrike.com/en-us/press-releases/crowdstrike-launches-frontier-models-for-cybersecurity-with-nvidia/
12. Rossi et al., **Temporal Graph Networks for Deep Learning on Dynamic Graphs**, arXiv:2006.10637. https://arxiv.org/abs/2006.10637
13. Lim et al., **Temporal Fusion Transformers for Interpretable Multi-horizon Time Series Forecasting**, arXiv:1912.09363 / International Journal of Forecasting. https://arxiv.org/abs/1912.09363
14. Lundberg & Lee, **A Unified Approach to Interpreting Model Predictions**, NeurIPS 2017 (SHAP). https://arxiv.org/abs/1705.07874
15. **Cybersecurity Digital Twins: Concept, blueprint, and challenges for multi-ownership digital service chains**, Journal of Information Security and Applications, Volume 96, January 2026. https://doi.org/10.1016/j.jisa.2025.104299
16. Android Developers, **Build an offline-first app**. https://developer.android.com/topic/architecture/data-layer/offline-first
17. MITRE ATT&CK, **Enterprise tactics**. https://attack.mitre.org/tactics/
18. MITRE ATT&CK, **ICS tactics**. https://attack.mitre.org/tactics/ics/
19. Hamwi & Mittal, **FL-TWIN: a unified federated learning system for intrusion detection with digital twins modelling**, Scientific Reports, June 20, 2026. https://www.nature.com/articles/s41598-026-58750-1
20. Android Developers, **VpnService API reference**. https://developer.android.com/reference/android/net/VpnService
21. Android Developers, **VPN / Connectivity guide**. https://developer.android.com/develop/connectivity/vpn
22. ONNX Runtime, **Mobile**. https://onnxruntime.ai/docs/get-started/with-mobile.html
23. ONNX Runtime, **Deploy on mobile**. https://onnxruntime.ai/docs/tutorials/mobile/
24. Android Developers, **Android is Compose-first**. https://developer.android.com/develop/ui/compose/first
25. MITRE ATT&CK, **Enterprise Matrix**. https://attack.mitre.org/matrices/enterprise/
26. MITRE ATT&CK, **Reconnaissance and Lateral Movement tactics**. https://attack.mitre.org/tactics/TA0043/ and https://attack.mitre.org/tactics/TA0008/
27. University of New Brunswick, **CSE-CIC-IDS2018 dataset**. https://www.unb.ca/cic/datasets/ids-2018.html
28. Stratosphere Laboratory, **Datasets Overview / CTU-13**. https://www.stratosphereips.org/datasets-overview/
29. Stratosphere Laboratory, **CTU-13 Dataset**. https://stratosphere-ips.squarespace.com/datasets-ctu13
30. Stratosphere Laboratory, **Real attack and benign network traffic datasets**. https://stratosphere-ips.squarespace.com/datasets
31. Dribbble, **Cyber Security Mobile App Design** — visual reference for incident timelines and AI suggested actions. https://dribbble.com/shots/27293872-Cyber-Security-Mobile-App-Design
32. Dribbble, **Smart Cybersecurity Mobile App Design** — visual reference for mobile security dashboards. https://dribbble.com/shots/26942010-Smart-Cybersecurity-Mobile-App-Design
33. Dribbble, **CyberAI: AI Cybersecurity & Threat Monitoring** — visual reference for AI monitoring and security metrics on mobile. https://dribbble.com/shots/27005875-CyberAI-AI-Cybersecurity-Threat-Monitoring

---

## Final product definition

**VajraWorld should be built as a predictive cyber-defence platform, not an Android-only IDS.** The Android app is the defender cockpit; the edge engine is the network sensor and world-model brain. The core research contribution is a learned dynamic model of evolving network state with K-step future rollouts. The product differentiator is the combination of **future trajectory + uncertainty + evidence + counterfactual defence simulation** in one workflow. The enterprise differentiator is **privacy-first edge processing, CII-aware policy controls, and an audit-ready decision loop**.

The correct long-term direction is:

```text
IDS
 ↓
NDR
 ↓
Predictive NDR
 ↓
World Model
 ↓
Counterfactual Cyber Defence
 ↓
Cybersecurity Digital Twin
```

That is the architecture that can start as a competition prototype and realistically grow into a serious research/product platform.
