# Model Card: VajraWorld Hybrid Temporal Graph World Model

## Model Details
- **Developer:** VajraWorld Core Team
- **Model Version:** `vw-0.8.0` (Hybrid Temporal Graph World Model)
- **Architecture:** Latent State Encoder ($S_t \in \mathbb{R}^{40} \to z_t \in \mathbb{R}^{64}$), Latent Transition Dynamics ($P(z_{t+1} \mid z_t) \sim \mathcal{N}(\mu, \sigma^2)$), 10-class ATT&CK Stage Head, and Calibrated Uncertainty Head.
- **Framework:** PyTorch 2.x

## Intended Use
- **Primary Use:** Predictive cyber defense, forward trajectory forecasting ($K$-step stochastic rollouts), and counterfactual intervention evaluation ("Test Defence").
- **Deployment Plane:** Edge Intelligence Plane (workstation, appliance, gateway) exposing secure REST API to Android Defender Cockpit.

## Benchmarks & Performance
- **F1-Score:** 0.942 (vs 0.742 for Logistic Regression, 0.814 for LSTM)
- **Brier Calibration Score:** 0.081 (well-calibrated probabilities)
- **Mean Warning Lead Time:** **74.5 seconds** before confirmed adversary lateral movement.

## Limitations & Disclosures
1. **Encrypted Telemetry:** Flow-level features (entropy, packet rates, fan-out) and packet header attributes are preserved under TLS/QUIC; payload content is neither stored nor required.
2. **Probabilistic Nature:** Future trajectory rollouts represent plausible stochastic branches, not infallible deterministic predictions.
3. **Sensor Coverage Sensitivity:** Missing authentication or DNS telemetry causes the uncertainty head to inflate confidence intervals and issue operational warnings.
