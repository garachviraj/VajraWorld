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
3. Contain with reversible action (e.g. host isolation)
4. Escalate
5. Destructive action requires explicit operator approval (especially under OT/ICS profiles).
