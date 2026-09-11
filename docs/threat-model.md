# VajraWorld Threat Model & Self-Defence Architecture

A security platform becomes dangerous if its own control plane is vulnerable. VajraWorld enforces strict self-protection mechanisms across both planes.

---

## 1. Asset & Surface Inventory

| Component | Asset | Potential Threat | Mitigation |
|---|---|---|---|
| **Edge PCAP Ingestion** | Packet parsers | Malformed PCAP buffer overflows, RCE | Sandboxed Scapy parser, input validation, strict packet length limits, dropped malformed frame accounting |
| **World Model Storage** | Model weights / checkpoints | Weight tampering, backdooring | SHA-256 integrity checksums, signed model bundles, version registries |
| **REST API Gateway** | Control plane | Unauthorized simulation, telemetry replay, denial of service | Token-based auth, mTLS support, sliding rate limiters, replay timestamp windows |
| **Audit Ledger** | Forensic integrity | Record deletion, log tampering | Append-only SQLite/PostgreSQL audit table with actor attribution |
| **Android Cockpit** | Local cache, VPN TUN | Telemetry interception, credential theft | Android Keystore encryption, encrypted Room database, secure VpnService constraints |
| **Simulator Action Engine** | Production actuators | Accidental automated service disruption | **Read-only by default**. Actions cannot touch production networks without explicit multi-signature approval |

---

## 2. OT / ICS Safety Boundary

Under the `OT / ICS` and `Critical Infrastructure` profiles:
- Automated containment actions are strictly disabled.
- The policy engine downgrades recommendations to "Observe", "Verify", or "Request Human Authorization".
- The system prevents AI forecasting from issuing unverified disruptive control commands into operational technology environments.
