# VajraWorld System Architecture

## 1. Architectural Philosophy: The Two-Plane Division

VajraWorld is engineered as a **two-plane predictive cyber-defence system**:

```text
┌─────────────────────────────────────────────────────────────┐
│                 ANDROID DEFENDER COCKPIT                    │
│      Jetpack Compose • Room DB • Canvas Dynamic Graph       │
│      Trajectory • Forensics • Test Defence Simulator        │
└───────────────────────────┬─────────────────────────────────┘
                            │ TLS / mTLS (Rest API)
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                 EDGE INTELLIGENCE CORE                      │
│      FastAPI • Telemetry Collectors • State Engine          │
│      Dynamic Graph Engine • PyTorch World Model Engine      │
│      K-Step Rollout Simulator • Counterfactual Simulator     │
│      Layered Explainability • Action Policy Guardrails      │
└───────────────────────────┬─────────────────────────────────┘
                            ▼
                Enterprise Sensor Telemetry
         (PCAP • NetFlow/IPFIX • Zeek • Suricata)
```

1. **Android Defender Plane**:
   - Mobile-first, offline-capable cockpit for threat investigators and CISOs.
   - Houses 8 purpose-built views: Overview, Trajectory Timeline, Dynamic Graph Canvas, Incident Forensics, Counterfactual Defence Simulator, Layered Explainability, and Model Health.
   - Uses Room + WorkManager for offline data preservation.
   - Implements `VpnService` for standalone edge packet metadata sniffing.

2. **Edge Intelligence Plane**:
   - Sensor normalization for PCAP, NetFlow/IPFIX, and Zeek/Suricata EVE JSON.
   - Computes rolling feature state vectors $S_t$ (24 flow/packet features + 16 graph topology features).
   - Encodes state into latent representation $z_t \in \mathbb{R}^{64}$.
   - Learns transition dynamics $P(z_{t+1} \mid z_t) \sim \mathcal{N}(\mu, \sigma^2)$.
   - Performs $K$-step stochastic rollouts into multiple hypothetical attack futures.
   - Evaluates counterfactual interventions ("Test Defence") before touching production.

## 2. World Model Mathematical Foundation

- **Observable State**: $S_t = [\text{FlowStats}_t, \text{PacketStats}_t, \text{TemporalFeatures}_t, \text{GraphEmbedding}_t] \in \mathbb{R}^{40}$
- **Latent State Encoder**: $z_t = \text{Encoder}(S_t) \in \mathbb{R}^{64}$
- **Transition Dynamics**: $\mu_{t+1}, \log \sigma^2_{t+1} = f_\theta(z_t)$
- **Forward Rollout**: $z_{t+k} \sim \mathcal{N}(\mu_{t+k}, \sigma_{t+k}^2)$
- **Progression Stage Head**: $P(\text{Stage}_{t+k}) = \text{Softmax}(W_s z_{t+k} + b_s)$
- **Risk Score**: $R_{t+k} = \sigma(W_r z_{t+k} + b_r) \in [0, 1]$
- **Calibrated Uncertainty**: $U_t = \text{Clamp}(0.6 \cdot \text{Var}(z_t) + 0.4 \cdot \text{OOD}(z_t) + \text{Penalty}_{\text{missing}}, 0.0, 0.95)$

## 3. Counterfactual Defence Utility Formula

$$\text{Action Utility} = (\text{Security Benefit} \times \text{Confidence}) - \text{Disruption Cost} - \text{Irreversibility Cost}$$

Enforces safe decision support:
1. Observe
2. Confirm
3. Contain with reversible action (e.g. host isolation, domain block, APK quarantine)
4. Escalate
5. Destructive action requires explicit operator approval (especially under OT/ICS profiles).

---

## 4. VajraWorld Guardian Multi-Surface Security Architecture

The Guardian upgrade extends the Edge World Model and Android Cockpit across 6 attack surfaces:

```text
┌────────────────────────────────────────────────────────────────────────┐
│                   VAJRAWORLD GUARDIAN RADAR                            │
│  Notification ◄──► Link ◄──► File/APK ◄──► OTP ◄──► Clipboard ◄──► Net  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   GUARDIAN CORRELATION & STORY ENGINE                  │
│  • Link Engine (Rules + Lexical + Message Context + Progression)       │
│  • File & APK Engine (Permissions, Debuggable, Zip-Bomb Guardrails)     │
│  • Notification Defence (Scam lure detection + SHA-256 Hashed Normal) │
│  • OTP Privacy Vault (Zero Plaintext Storage + Forwarding Flags)       │
│  • Clipboard Guardian (Foreground Secret Scanner + Auto-Clear Timers)  │
│  • Threat Story Engine (Multi-Surface Narrative Synthesis)             │
│  • Counterfactual Defence Simulator (What-if intervention modeling)    │
└────────────────────────────────────────────────────────────────────────┘
```

### 1. 3-Layer Guardian Link Engine
- **Layer 1 (Rules)**: IP-literal hostnames, punycode/IDN spoofing, suspicious TLDs (`.xyz`, `.top`, `.tk`), high-entropy paths, direct executable downloads (`.apk`, `.dex`, `.exe`).
- **Layer 2 (Lexical & Domain)**: Subdomain depth (>3), brand deception keywords (`bank`, `secure`, `verify`, `login`), hyphen count.
- **Layer 3 (Context Correlation)**: Urgency cues (`blocked`, `suspended`, `24 hours`) correlated with message and notification context.
- **Trajectory Progression**: Models link lifecycle ($P(\text{Discovered} \to \text{Opened} \to \text{Credential Harvest} \to \text{Exposure} \to \text{Takeover})$).

### 2. Download & File Risk Engine
- **Zip-Bomb & Decompression Guardrails**: Max uncompressed size (250MB), max file count (1,000), max recursion depth (3), compression ratio limit (15x).
- **APK Static Inspector**: Manifest risk parser identifying toxic permission combinations:
  - `BIND_ACCESSIBILITY_SERVICE` + `SYSTEM_ALERT_WINDOW` (Banking overlay trojans)
  - `READ_SMS` / `RECEIVE_SMS` + `INTERNET` (OTP exfiltration trojans)
  - `is_debuggable=true` vulnerability flags.

### 3. Smart Notification Defence & OTP Privacy Vault
- **Zero Plaintext Storage**: Intercepted OTPs and notification bodies are hashed via SHA-256 and immediately purged from memory. Only anonymized metadata and `value_stored: false` flags persist.
- **Scam & Forwarding Interception**: Detects social engineering lures instructing users to share or forward verification codes.

### 4. Foreground Clipboard Guardian
- **Zero Background Spying**: Scans only on foreground user intent.
- **Sensitive Token Detection**: AWS access keys, GitHub personal access tokens, PEM private keys, and crypto seed phrases.
- **Auto-Clear Timer**: Configurable 15s / 30s / 60s memory purging.

### 5. Unified Threat Story Engine
- Correlates isolated events into causal narratives with MITRE ATT&CK Mobile/Enterprise tactics and assigns cumulative multi-surface risk scores.

---

## 5. Strict Privacy & Platform Compliance Guarantees

1. **Zero Plaintext OTP Retention**: Plaintext numeric/alphanumeric authentication codes are strictly never stored to disk, database, or API responses.
2. **Zero Raw Notification Logging**: Notification text is evaluated in-flight; only categorized indicators and links are tracked.
3. **No Accessibility Abuse**: Foreground and explicit permission models ensure compliance with Google Play Developer Policies.
4. **Offline First**: All local threat telemetry persists in encrypted Room databases on device.
