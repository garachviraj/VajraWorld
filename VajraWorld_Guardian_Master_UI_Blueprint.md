# VAJRAWORLD GUARDIAN — MASTER-LEVEL ANDROID UI/UX BLUEPRINT

## Production UI Upgrade Specification

### Purpose

This document is the **UI-only master specification** for upgrading the existing VajraWorld Guardian Android application into a premium, highly animated, visually distinctive cybersecurity cockpit **without breaking the existing domain logic, ViewModels, repositories, backend contracts, scanner implementations, or real-data work**.

The project already contains screens and ViewModels for:

- Overview
- Security Radar
- Network Graph
- File Scan
- Link Scan
- Explainability
- Model Health
- Incidents / Threat Stories
- Simulation
- Trajectory / Forecast
- Clipboard Guardian

The current implementation should be treated as the **functional foundation**. This document governs how the presentation layer is upgraded.

---

# 1. NON-NEGOTIABLE UI CONTRACT

## The most important rule

> **Upgrade presentation, never fake or replace functionality.**

The UI layer must consume the existing real state exposed by the ViewModels.

Do not move business logic into composables.

Do not generate fake risk values in composables.

Do not create random graph nodes just to make the screen look alive.

Do not replace a real API result with a hardcoded visual.

Do not replace a real scanner result with demo text.

Do not silently change API models.

Do not rename domain fields simply for UI convenience unless an explicit UI mapper is created.

Do not alter repository behavior unless required to expose already-existing real state.

Do not rewrite backend contracts for aesthetic reasons.

---

# 2. CORE DESIGN PRINCIPLE

The app should visually answer four questions at all times:

```text
WHAT IS HAPPENING?
        ↓
WHAT IS CONNECTED?
        ↓
WHAT MAY HAPPEN NEXT?
        ↓
WHAT CAN I DO ABOUT IT?
```

The experience should feel like a **living security system**, not a collection of static screens.

The UI should therefore emphasize:

- state transitions
- temporal movement
- relationships
- uncertainty
- evidence
- forecast
- user action

---

# 3. VISUAL PRODUCT IDENTITY

## Brand

**VAJRAWORLD GUARDIAN**

Tagline:

**See the threat. Predict the future. Change the outcome.**

## Visual personality

The application should feel like a combination of:

- modern aerospace control room
- premium SOC interface
- scientific visualization
- intelligent mobile security product
- calm enterprise software

Avoid:

- gamer-style neon overload
- excessive cyberpunk decoration
- dozens of glowing borders
- fake hologram effects
- random particle backgrounds
- excessive gradients
- flashing danger effects
- ornamental graphs without data

The visual complexity should come from **real data movement and relationships**, not decoration.

---

# 4. COLOR SYSTEM

Use a deep dark theme as the primary product identity.

### Base palette

```text
Background 0: #070A0F
Background 1: #0B1018
Surface 0:    #101722
Surface 1:    #151D29
Surface 2:    #1A2432
Border:       #263244
Text Primary: #F3F7FB
Text Secondary:#98A7BA
Text Muted:   #617084
```

### Semantic colors

```text
Healthy:  #32D583
Info:     #4DA3FF
Warning:  #F5B942
High:     #FF7A45
Critical: #FF4D5F
```

Do not use semantic colors as permanent background fills. They should appear primarily as:

- indicators
- graph nodes
- chart series
- status pills
- glows
- progress states
- focused controls

This prevents the UI from becoming visually noisy.

---

# 5. TYPOGRAPHY

Use a modern technical typography hierarchy.

Recommended:

- App title: bold / semi-bold
- Screen title: 24–28sp
- Section title: 16–18sp
- Body: 14–16sp
- Metadata: 11–13sp
- Technical values: 12–14sp medium/monospace where appropriate

Numbers such as:

```text
82%
14 events
+30m
3.2 MB
```

should be visually strong, but never larger than necessary.

Use monospace only for:

- IP addresses
- ports
- hashes
- package names
- model versions
- timestamps where useful
- technical identifiers

---

# 6. MOTION LANGUAGE

The app must have a single coherent animation language.

## Animation principles

### 1. Data should move toward meaning

When a new event appears, show it entering the timeline or graph instead of simply changing a number.

### 2. Risk should pulse subtly

Critical risk can use a soft pulse.

Never use continuous aggressive flashing.

### 3. Forecasts should grow into view

Future nodes should fade and expand from the current state.

### 4. User actions should have physical feedback

Buttons should:

- compress slightly on press
- release smoothly
- provide a small visual confirmation

### 5. Transitions must be deterministic

Use actual state changes to drive animations.

Never run animations merely to make the screen appear active.

---

# 7. MOTION TIMINGS

Use a consistent timing scale:

```text
Micro interaction: 120–180 ms
Small transition: 180–260 ms
Normal transition: 260–360 ms
Large scene transition: 400–650 ms
Chart reveal: 500–900 ms
Radar expansion: 650–1000 ms
```

Use spring physics for:

- cards
- node selection
- bottom sheets
- radar expansion

Avoid excessively slow animations.

---

# 8. UI ARCHITECTURE RULE

Create a dedicated design system layer.

Suggested package:

```text
ui/
  theme/
  components/
  motion/
  charts/
  graph/
  radar/
  cards/
  indicators/
  sheets/
```

Recommended reusable components:

```text
VajraScaffold
VajraTopBar
SecurityStatusOrb
RiskScoreRing
ForecastMiniChart
LiveEventTicker
ThreatStoryCard
EventTimeline
SecurityNode
NodeConnection
SecurityRadarCanvas
NetworkGraphCanvas
EvidenceBar
ConfidenceBadge
UncertaintyBadge
ModelStatusBadge
LiveModeBadge
DemoModeBadge
MetricCard
ExpandableEvidenceCard
ActionButton
RiskChip
TechnicalMetadataRow
BottomActionSheet
```

The screens should be compositions of these reusable components rather than one-off drawings.

---

# 9. HOME / OVERVIEW — MASTER DASHBOARD

This is the most important screen.

It must feel alive, but every moving element must correspond to real application state.

## Layout

```text
┌─────────────────────────────────────┐
│ VAJRAWORLD                 ● LIVE   │
│ Guardian                    08:42   │
├─────────────────────────────────────┤
│                                     │
│          SECURITY STATE             │
│                                     │
│             ◉ 82                    │
│          PROTECTED                  │
│                                     │
│     ╭──────────────────────╮        │
│     │     FORECAST         │        │
│     │  Now ─────── +30m    │        │
│     │  32%        61%      │        │
│     ╰──────────────────────╯        │
│                                     │
├─────────────────────────────────────┤
│ LIVE SECURITY ACTIVITY              │
│ ───────╮                            │
│        ╰─●──●────●────────●         │
│                                     │
├─────────────────────────────────────┤
│ THREAT STORY                        │
│  Suspicious campaign detected       │
│  3 correlated events                │
├─────────────────────────────────────┤
│ [SCAN] [RADAR] [TIMELINE] [ACT]    │
└─────────────────────────────────────┘
```

## Header

Show:

- VajraWorld logo
- monitoring status
- current time
- connection status when relevant

The word **LIVE** must only appear when the screen is actually receiving live device/backend updates.

Use:

```text
LIVE DEVICE TELEMETRY
```

or

```text
SYNTHETIC DEMO
```

as an explicit, highly visible mode pill.

Never hide the data source.

---

# 10. SECURITY STATE ORB

Build a premium animated security state visualization.

### Center

Large score:

```text
82
```

Below:

```text
PROTECTED
```

### Ring layers

Layer 1:

current risk.

Layer 2:

forecast risk.

Layer 3:

uncertainty halo.

The uncertainty halo should become wider when model uncertainty increases.

This is extremely important visually:

**uncertainty must not be represented as another risk number only; it should be visible geometrically.**

---

# 11. LIVE RISK GRAPH

The dashboard needs a real time-series graph driven by ViewModel data.

Show:

- current risk
- previous observations
- short forecast
- confidence band

### Graph

```text
Risk
100 ┤                         ╭── forecast
 80 ┤               ╭─────────╯
 60 ┤         ╭──────╯
 40 ┤─────────╯
 20 ┤
  0 ┼────────────────────────────────
      -30   -20   -10    NOW    +30m
```

Use:

- solid line = observed
- dotted line = forecast
- translucent region = uncertainty

Do not invent history. When historical points are unavailable, render the graph with an honest empty/insufficient-data state.

---

# 12. LIVE EVENT STREAM

Add a horizontally compact but visually rich event stream below the graph.

Each event:

```text
● 08:42
  Suspicious URL analyzed
  Risk +14
```

New events should slide in from the top and settle into position.

Older events should compress slightly.

Tapping an event opens its detail sheet.

---

# 13. SECURITY RADAR — MASTER EXPERIENCE

The existing SecurityRadarScreen must be upgraded into the application's signature visualization.

It should not be a decorative circular graph.

It should behave like an interactive security map.

## Radar structure

Center node:

```text
DEVICE
```

Primary rings:

```text
Ring 1 — User / local state
Ring 2 — Apps / files / links
Ring 3 — Network destinations
Ring 4 — Threat intelligence / forecast
```

Nodes are positioned according to actual graph data where available.

If server data contains explicit node coordinates, respect them.

If coordinates are absent, create a deterministic layout based on stable node identity, never `Random()` on every recomposition.

---

# 14. RADAR NODE TYPES

Node categories:

```text
DEVICE
APPLICATION
FILE
LINK
DOMAIN
IP
NOTIFICATION
OTP
CLIPBOARD
THREAT STORY
FORECAST
```

Each category has a consistent icon shape.

Do not rely on color alone.

---

# 15. RADAR NODE STATES

Every node can be:

```text
NORMAL
OBSERVED
SUSPICIOUS
HIGH_RISK
CRITICAL
FORECAST
SELECTED
DISCONNECTED
```

### Observed node

Small steady glow.

### Suspicious

Slow breathing glow.

### Critical

Faster but subtle pulse.

### Forecast

Dashed outline + low-opacity halo.

### Selected

Expanded ring + focused details.

---

# 16. RADAR CONNECTIONS

Connections must represent actual relationships.

Examples:

```text
Notification ──contains──> URL
URL ──downloads──> APK
APK ──contacts──> Domain
Domain ──resolves──> IP
Event ──contributes──> Threat Story
Current State ──predicts──> Future State
```

Connection animation should communicate direction.

Use a moving dot along an active connection only when an actual event/transition exists.

Do not continuously animate all edges.

---

# 17. RADAR TOUCH INTERACTION

When the user taps a node:

1. Node moves into focus.
2. Camera subtly zooms toward node.
3. Other nodes reduce opacity.
4. Connected nodes remain visible.
5. Connection labels appear.
6. Bottom sheet opens.

The bottom sheet should show:

```text
DOMAIN
example.com

RISK
72 / 100

STATE
SUSPICIOUS

FIRST OBSERVED
08:31:22

LAST OBSERVED
08:41:08

CONNECTED EVENTS
4

WHY
• Destination is new
• Unusual connection frequency
• Linked to suspicious notification

PREDICTION
Potential C2 progression

[VIEW STORY]
[EXPLAIN]
[SIMULATE DEFENCE]
```

---

# 18. RADAR LONG PRESS

Long press a node to expose advanced controls:

```text
Inspect
Trace connections
Open timeline
View evidence
Forecast from here
Simulate block
```

Do not make destructive actions happen immediately.

Use confirmation for disruptive actions.

---

# 19. RADAR ZOOM / PAN

Support:

- pinch zoom
- drag/pan
- double tap to focus
- reset camera

At extreme zoom-out:

collapse individual nodes into clusters.

At zoom-in:

show detailed nodes.

Example:

```text
5 Network Destinations
```

expands into:

```text
api.example
cdn.example
203.0.113.x
```

This prevents graph overload.

---

# 20. NETWORK GRAPH — PROFESSIONAL SOC VIEW

NetworkGraphScreen should be a different visualization from the radar.

### Radar

High-level security state.

### Network Graph

Technical relationship map.

The network screen should feel closer to a SOC investigation tool.

---

# 21. NETWORK GRAPH LAYOUT

Center:

```text
DEVICE
```

Branches:

```text
APP
  ↓
DOMAIN
  ↓
IP
  ↓
PORT
```

Display:

- node labels
- destination type
- connection count
- bytes
- first seen
- last seen
- risk

---

# 22. NETWORK GRAPH LIVE FLOW

When real telemetry arrives:

animate an event from source node to destination node.

Example:

```text
DEVICE ●──────────────● api.example
              →
```

The moving indicator represents the actual event.

Do not animate traffic when there is no new event.

---

# 23. NETWORK GRAPH DETAILS

When selected:

```text
api.example.com

Connections: 42
First seen: 08:21
Last seen: 08:42

Outbound: 1.2 MB
Inbound: 420 KB

Periodicity: 0.81
Novelty: HIGH
Risk: 67

Possible behaviour
Periodic beaconing
```

Use real ViewModel values.

If unavailable:

```text
Not available from current telemetry
```

Never invent data.

---

# 24. FORECAST / TRAJECTORY SCREEN

This screen must feel like a predictive analysis console.

Use a horizontal time axis:

```text
PAST                NOW                    FUTURE
────●────●────●────●────────────●──────────●────
                    │            │          │
                   +5m         +15m       +30m
```

Observed nodes:

solid.

Predicted nodes:

dashed.

Uncertainty:

soft cloud / band.

---

# 25. MULTI-FUTURE BRANCH VISUALIZATION

Where the backend provides multiple future branches:

```text
                    ┌─ Benign 52%
CURRENT ────────────┤
                    ├─ Credential Access 31%
                    │
                    └─ C2 17%
```

Use animated path splitting.

The branch with the highest probability should not automatically appear "true".

Make probability visually proportional but maintain readability.

---

# 26. EXPLAINABILITY SCREEN

This screen should visually explain the model rather than merely list features.

## Header

```text
WHY IS RISK INCREASING?
```

Then show a contribution waterfall.

```text
BASE RISK          22
+ New destination  +12
+ URL anomaly      +18
+ Beacon pattern   +15
- Known trusted    -4
----------------------
CURRENT RISK       63
```

Use actual backend feature attribution values.

---

# 27. TEMPORAL CHANGE POINT VISUAL

Show the moment when behaviour changed.

Example:

```text
Normal traffic
───────●────●────●────
                   ↑
              CHANGE POINT
                   │
                   ●──●──● suspicious
```

Tapping the change point opens the evidence from that interval.

---

# 28. MODEL HEALTH SCREEN

The model health page should feel like a scientific instrument panel.

Show:

```text
MODEL
VajraWorld Temporal Transformer

STATUS
TRAINED

VERSION
1.2.0

F1
0.91

PR-AUC
0.88

Brier
0.14

CALIBRATION
GOOD

LATENCY
182 ms
```

All values must be real API data.

Use special states:

```text
TRAINED
DEMO
UNTRAINED
ERROR
UNKNOWN
```

The UI color and wording must change accordingly.

---

# 29. FILE SCANNER — PREMIUM UI

The scan process should feel like an actual inspection.

## Step sequence

```text
Selecting file
       ↓
Reading metadata
       ↓
Computing SHA-256
       ↓
Inspecting structure
       ↓
Analyzing permissions
       ↓
Calculating risk
```

Show actual step completion.

Do not display a fake progress bar that advances on a timer independently of work.

---

# 30. FILE SCAN RESULT

Hero card:

```text
LOW RISK

invoice.pdf

SHA-256
7a83...9f21

Structural checks
✓ Passed

Archive checks
✓ Passed

Type consistency
✓ Passed
```

For APK:

Show a dedicated permission/risk matrix.

---

# 31. LINK GUARDIAN — PREMIUM UI

Input card:

```text
Paste or share a link

[ URL input                       ]

[ ANALYZE ]
```

Result animation:

1. URL enters the scanner.
2. Structural signals appear.
3. Domain analysis appears.
4. Final risk ring settles.

This creates the feeling of an actual reasoning pipeline without fabricating analysis.

---

# 32. THREAT STORY SCREEN

This is the narrative investigation view.

Use a vertical story line:

```text
08:30
● Notification
│
│ contains
│
08:31
● Suspicious URL
│
│ leads to
│
08:33
● APK
│
│ contacts
│
08:35
● New domain
│
│ correlated with
│
08:36
● Threat Story
```

Each node expands into evidence.

At the bottom:

```text
CURRENT STAGE
Initial Access

FORECAST
Credential Access

CONFIDENCE
78%

[EXPLAIN]
[SIMULATE DEFENCE]
```

---

# 33. INCIDENT DETAIL

Use a large collapsible evidence surface.

Sections:

```text
Overview
Evidence
Timeline
Related Nodes
ATT&CK Mapping
Forecast
Recommended Actions
Model Information
```

Collapsed by default except Overview.

This prevents information overload.

---

# 34. DEFENCE SIMULATOR UI

Make this a signature interaction.

Start screen:

```text
CURRENT FUTURE

Risk: 84%

WHAT WOULD YOU LIKE TO CHANGE?

○ Block domain
○ Quarantine file
○ Disconnect network
○ Clear exposure
```

Then animate the future trajectory being recalculated.

---

# 35. SIMULATION REPLAY ANIMATION

Visual sequence:

```text
CURRENT STATE
      ↓
INTERVENTION
      ↓
MODEL REPLAY
      ↓
NEW TRAJECTORY
```

During replay:

- future nodes move
- high-risk path fades
- alternative path becomes visible
- risk graph updates

At completion:

```text
BEFORE
84%

AFTER
29%

RISK REDUCTION
55 points
```

Label:

```text
MODEL SIMULATION
```

or

```text
RULE-BASED ESTIMATE
```

according to the actual engine.

---

# 36. SHARE-TO-VAJRAWORLD EXPERIENCE

When a URL or suspicious content is shared into the app:

Do not open a generic home screen.

Deep-link directly into:

```text
Link Guardian
```

with the content prefilled.

Animation:

```text
Shared content
      ↓
Security lens opens
      ↓
Analysis begins
```

The user must still see the actual source and analysis state.

---

# 37. PERMISSION UI

Permissions should have dedicated visual explanations.

Example:

```text
NETWORK MONITORING

VajraWorld can monitor network
metadata through Android's VPN
security interface.

What is collected?
✓ connection metadata
✓ timing
✓ destination information

What is not collected by default?
✕ message payloads
✕ passwords
✕ OTP values

[ENABLE MONITORING]
```

This should feel trustworthy rather than frightening.

---

# 38. EMPTY STATES

Never use blank white/dark space.

Create intelligent empty states.

Example:

```text
NO LIVE NETWORK DATA

Network Guardian is currently off.

Enable monitoring to build your
security world model.

[ENABLE]
```

Or:

```text
NO FORECAST YET

The model needs enough recent
observations before forecasting.
```

---

# 39. OFFLINE STATE

When offline:

Top status:

```text
OFFLINE
```

But do not make the whole application appear broken.

Show:

```text
Local protection remains active
```

for features that genuinely remain available.

Disable only dependent actions.

---

# 40. DEMO STATE

Synthetic mode must be unmistakable.

Use a persistent but elegant badge:

```text
SYNTHETIC DEMO
```

Do not label synthetic results as live.

Demo mode can still look beautiful.

The difference is the data source label, not a lower-quality UI.

---

# 41. LIVE DEVICE TELEMETRY STATE

When actual device telemetry is connected:

Show:

```text
● LIVE DEVICE TELEMETRY
```

Add a subtle connection pulse.

The pulse stops if live data stops arriving.

This is a much better indicator than fake perpetual animation.

---

# 42. LIVE DATA ANIMATION RULE

Every dynamic visualization must have a state source.

Create a mapping:

```text
ViewModel Flow
       ↓
UI State
       ↓
Animation State
       ↓
Compose Animation
```

Do not have the UI generate fake events.

For example:

Bad:

```kotlin
LaunchedEffect(Unit) {
    while(true) {
        addRandomNode()
    }
}
```

Good:

```text
NetworkGraphViewModel.graphState
        ↓
collectAsStateWithLifecycle()
        ↓
animate node positions based on actual state
```

---

# 43. GRAPH POSITION STABILITY

Very important.

Graph nodes must not jump around whenever Compose recomposes.

Use deterministic positioning based on:

```text
node ID
node type
relationship
layout bounds
```

Cache positions in a presentation-layer state holder.

When the data set changes:

- existing nodes preserve positions
- new nodes animate into position
- removed nodes fade out

This creates a professional graph experience.

---

# 44. GRAPH LAYOUT ANIMATION

When a new node arrives:

```text
opacity 0 → 1
scale 0.7 → 1
position old anchor → target
```

When removed:

```text
scale 1 → 0.75
opacity 1 → 0
```

When selected:

```text
scale 1 → 1.12
halo expands
connections emphasize
```

---

# 45. REAL-TIME CHARTS

Use a chart abstraction so the UI does not depend on a particular chart library.

Example:

```text
RiskChart
ForecastChart
TrafficChart
EventVelocityChart
ConfidenceChart
```

The chart composables receive already-prepared presentation models.

They must not call APIs directly.

---

# 46. MICRO-INTERACTIONS

Add subtle interactions everywhere:

### Cards

Lift 2–4dp on press.

### Buttons

Scale 0.98 on press.

### Risk ring

Animate only when score changes.

### Timeline

New event expands once.

### Node selection

Focus + detail sheet.

### Tabs

Smooth indicator movement.

### Bottom navigation

Icon and label transition together.

---

# 47. HAPTIC FEEDBACK

Use carefully.

Low intensity:

- successful scan
- selection
- simulation completed

Medium:

- warning state

Strong:

- critical security result only when user explicitly enters or views a critical alert

Do not constantly vibrate during live updates.

---

# 48. SOUND

Default:

OFF.

If implemented later:

- tiny confirmation sound for completed user actions
- optional warning sound for critical events

Never add background cyber sound loops.

---

# 49. BOTTOM NAVIGATION

Recommended primary navigation:

```text
HOME
RADAR
TIMELINE
SCANS
MORE
```

The More screen contains:

- Network
- Explainability
- Model Health
- Simulation
- Settings

This avoids a navigation bar containing 10 tiny icons.

---

# 50. GLOBAL SECURITY HEADER

Use a compact status element on major screens:

```text
● PROTECTED
```

or

```text
▲ ELEVATED
```

or

```text
! HIGH RISK
```

Tapping it opens current security context.

This creates a persistent mental model for the user.

---

# 51. BOTTOM SHEETS

Use bottom sheets for contextual detail.

Examples:

- radar node
- timeline event
- network destination
- explanation
- simulation result

Do not navigate away for every small detail.

This preserves investigation flow.

---

# 52. DETAIL EXPANSION PATTERN

Every major object should have three levels:

### Level 1

Human-readable summary.

### Level 2

Evidence.

### Level 3

Technical details.

Example:

```text
Suspicious Domain
      ↓
Why?
      ↓
Technical telemetry
```

This keeps the app approachable without sacrificing depth.

---

# 53. ACCESSIBILITY

Animations must not make the application unusable.

Support reduced-motion preference.

If reduced motion is enabled:

- remove camera zoom animations
- shorten graph movement
- replace large motion with fades
- preserve information hierarchy

Support:

- content descriptions
- minimum touch targets
- sufficient contrast
- large font scaling

---

# 54. PERFORMANCE RULES

Master-level visual quality is useless if the phone becomes slow.

Do not:

- redraw the entire graph unnecessarily
- restart infinite animations per recomposition
- allocate large objects in Canvas draw loops
- parse JSON inside composables
- perform file hashing on the main thread
- perform network requests in composables

Use:

```text
remember
derivedStateOf
snapshotFlow
LaunchedEffect only where justified
rememberCoroutineScope
Flow
StateFlow
collectAsStateWithLifecycle
```

For graph rendering, optimize Canvas and avoid excessive composable node trees when the graph becomes large.

---

# 55. SCREEN STATE MODEL

Every screen should have explicit states:

```text
Loading
Ready
Empty
Error
Offline
PermissionRequired
Unavailable
```

Example:

```kotlin
sealed interface NetworkUiState {
    data object Loading : NetworkUiState
    data class Ready(...) : NetworkUiState
    data object Empty : NetworkUiState
    data class Error(...) : NetworkUiState
    data object PermissionRequired : NetworkUiState
}
```

Do not infer state from arbitrary nullable fields.

---

# 56. VIEWMODEL CONTRACT

ViewModels remain the source of truth.

UI should consume:

```text
StateFlow<UiState>
StateFlow<List<...>>
SharedFlow<UiEvent>
```

The ViewModel handles:

- repository collection
- API calls
- scanner results
- state transformations
- error mapping

The composable handles:

- rendering
- animation
- interaction events
- local visual state only

---

# 57. DO NOT BREAK STEP-1 LOGIC

The previously planned real-data work remains authoritative:

### FileScan

Keep:

- SAF
- OpenDocument
- real FileInspector
- real SHA-256
- real archive safety
- real APK permission extraction

UI changes only.

### LinkScan

Keep:

- UrlRuleEngine
- entropy
- Levenshtein
- Share Sheet
- real scan state

UI changes only.

### Explainability

Keep:

`GET /v1/forecast/{id}/explanations`

UI only visualizes returned attribution and temporal change points.

### Health

Keep:

`GET /v1/model/status`

UI visualizes actual metadata and metrics.

### Simulation

Keep clean initial state.

UI does not generate fake initial results.

### Network graph

Keep:

`GET /v1/graph/current`

UI renders actual graph state.

### Overview

Keep:

clear:

`SYNTHETIC DEMO`

vs

`LIVE DEVICE TELEMETRY`

---

# 58. INTERACTION CONTRACT

Define a clear UI event interface.

Example:

```kotlin
sealed interface OverviewAction {
    data object Refresh : OverviewAction
    data object OpenRadar : OverviewAction
    data object OpenTimeline : OverviewAction
    data class OpenThreatStory(val id: String) : OverviewAction
}
```

Composables emit actions.

ViewModels process actions.

Never call repositories directly from UI components.

---

# 59. LOADING ANIMATIONS

Use skeletons instead of generic spinners where practical.

Example:

```text
SECURITY STATE
██████████
█████

FORECAST
████████████
```

For actual scanning:

use stage-based progress.

Do not fake percentage completion.

If exact percentage is unknown, show:

```text
ANALYZING...
```

with stage indicator.

---

# 60. ERROR EXPERIENCE

Errors should be explainable.

Example:

```text
NETWORK GRAPH UNAVAILABLE

The backend did not return graph data.

Your existing local security information
is still available.

[RETRY]
```

Avoid:

```text
Something went wrong
```

as the only message.

---

# 61. OFFLINE-FIRST UX

When the device loses network access:

Do not navigate the user to a dead-end.

Instead:

```text
LOCAL PROTECTION ACTIVE

Cloud intelligence unavailable.

Local URL rules, file inspection and
stored security history remain available.
```

Only disable actions that genuinely require the backend.

---

# 62. ANIMATION SAFETY

Animations should never obscure critical information.

Critical information must remain visible even if:

- animation is disabled
- reduced-motion is enabled
- device is slow
- frame rate drops

All animated visualization must have a static semantic representation underneath.

---

# 63. LARGE GRAPH FALLBACK

If graph nodes exceed a manageable threshold:

1. cluster nodes
2. show cluster counts
3. allow expansion
4. preserve search/filter

Example:

```text
NETWORK DESTINATIONS
[ 27 ]
```

Tap:

```text
Critical: 1
High: 3
Normal: 23
```

---

# 64. FILTER SYSTEM

For Timeline/Radar/Network screens provide filters:

```text
ALL
HIGH RISK
NETWORK
FILES
LINKS
NOTIFICATIONS
FORECAST
```

Filters should operate on already-loaded presentation data.

Do not make each filter execute a new backend request unless the existing ViewModel/API contract requires it.

---

# 65. SEARCH

Allow searching graph/timeline entities:

```text
example.com
192.0.2.10
invoice.apk
Threat Story #12
```

Search should highlight matching nodes/events rather than simply replacing the whole screen.

---

# 66. DARK/LIGHT MODE

Primary product identity remains dark mode.

Support light mode only if the existing product requirements allow it.

Do not create a completely separate visual language.

The same semantic colors, layouts, and motion language should remain consistent.

---

# 67. DEVICE SIZE SUPPORT

Support:

- small phones
- normal phones
- large phones
- tablets where useful

Responsive layout rules:

```text
Compact phone
1-column

Large phone
1-column + larger visualization

Tablet
2-pane investigation layout
```

On tablet:

left = graph/radar
right = details

This is especially valuable for the Network and Threat Story screens.

---

# 68. VISUAL INVESTIGATION MODE

When the user opens a threat story:

Enter an immersive investigation mode.

Dim unrelated UI.

Expand relevant graph nodes.

Show timeline on one side.

Show threat evidence on the other.

On tablet:

```text
┌───────────────┬──────────────────────┐
│ THREAT GRAPH  │ EVIDENCE             │
│               │                      │
│ nodes         │ timeline             │
│               │ explanation          │
│               │ forecast             │
└───────────────┴──────────────────────┘
```

This should feel like a miniature SOC investigation workspace.

---

# 69. PREMIUM VISUAL EFFECTS

Allowed:

- subtle blur
- glass-like elevated surfaces
- soft shadow
- radial glow behind key nodes
- soft animated noise only if performance allows
- depth through layering

Avoid:

- excessive glassmorphism
- huge glows
- perpetual particle fields
- 3D effects that reduce readability

The core visualization must remain data-driven.

---

# 70. LOGO ANIMATION

The Vajra logo can have a short startup animation.

Concept:

```text
Fragments
   ↓
assemble
   ↓
Vajra symbol
   ↓
security ring
   ↓
Guardian
```

Maximum duration:

~900 ms.

Allow skipping after first launch.

---

# 71. HOME SCREEN TRANSITION

When opening Home:

1. Header fades in.
2. Security state orb scales in.
3. Risk graph draws itself.
4. Threat story card appears.
5. Quick actions settle in.

Do not animate every card independently with random delays.

Use a small coordinated sequence.

---

# 72. RADAR ENTRY ANIMATION

When entering Radar:

1. Camera begins at full-system view.
2. rings appear.
3. current device node appears.
4. actual nodes materialize.
5. connections draw.
6. labels appear last.

If real graph data loads later:

nodes appear naturally as data arrives.

---

# 73. NETWORK ENTRY ANIMATION

On entry:

```text
DEVICE
  ↓
core nodes
  ↓
destinations
  ↓
connections
```

This animation should represent graph topology construction, not fake traffic.

---

# 74. FORECAST ENTRY ANIMATION

Observed history slides into view.

Then:

```text
NOW
 │
 └── forecast branches grow forward
```

The forecast line should appear as a projection of current model state.

---

# 75. CRITICAL ALERT TRANSITION

For critical results:

Do not suddenly flash the entire application red.

Instead:

- focus the affected object
- increase local glow
- show a red risk marker
- expand actionable message
- provide clear action buttons

Example:

```text
CRITICAL
Potential credential theft progression

[VIEW EVIDENCE]
[SIMULATE DEFENCE]
```

---

# 76. UI DATA TRACEABILITY

Every visible security number should be traceable.

For example:

```text
Risk 72
```

tapping the number should reveal:

```text
Risk source
Model version
Latest event
Feature contributors
Timestamp
Confidence
```

This is one of the key differences between a trustworthy security UI and a decorative dashboard.

---

# 77. TECHNICAL DETAIL DRAWER

Provide a hidden-but-accessible technical drawer on complex screens.

Example:

```text
TECHNICAL DETAILS

modelVersion: 1.2.0
stateWindow: 30
forecastHorizon: 6
telemetryAge: 2.4s
inputQuality: 0.93
oodScore: 0.08
```

This is useful for engineers, judges, researchers and SOC professionals without cluttering the main view.

---

# 78. DEMO / PRESENTATION MODE

Create a presentation mode that uses the same production UI but allows deterministic synthetic scenarios.

Example:

```text
Scenario:
Credential Phishing Progression
```

Replay:

```text
Notification
→ URL
→ APK
→ Network anomaly
→ Threat Story
→ Forecast
→ Defence Simulation
```

Every event should have a timestamp from the scenario.

No random generation.

This can be used for the 2-minute demo video.

---

# 79. DEMO MODE CONTROLS

Optional controls:

```text
PLAY
PAUSE
STEP
RESET
SPEED 1x / 2x / 4x
```

This is especially useful for showing judges how the world model sees progression.

Production mode should never show these controls unless demo mode is explicitly enabled.

---

# 80. DO NOT TOUCH LOGIC LIST

During UI implementation, do not modify these unless a compile/API contract issue absolutely requires it:

```text
Domain models
Repository interfaces
Repository business logic
FileInspector algorithms
UrlRuleEngine algorithms
Backend threat logic
World model implementation
Forecast mathematics
Threat correlation mathematics
Database schema
Authentication logic
VPN packet handling
```

If the UI cannot render something because the existing model lacks a required field:

1. inspect the existing model
2. create a UI mapper
3. show an honest unavailable state
4. only request a domain change if absolutely necessary

Do not invent values.

---

# 81. UI IMPLEMENTATION ORDER

Implement in this order:

## Phase A — Design System

1. Theme
2. Typography
3. Dimensions
4. Components
5. Motion system
6. Status indicators

## Phase B — Dashboard

1. Overview shell
2. Security state orb
3. live risk graph
4. event stream
5. threat card

## Phase C — Graph Experiences

1. Radar
2. Network graph
3. node selection
4. bottom sheets
5. zoom/pan
6. clustering

## Phase D — Investigation

1. Timeline
2. Threat Story
3. Incident detail
4. Explainability
5. Forecast

## Phase E — Scanning

1. Link scanner
2. File scanner
3. APK result

## Phase F — Model / Simulation

1. Health
2. Trajectory
3. Defence simulator

## Phase G — Polish

1. transitions
2. haptics
3. accessibility
4. tablet layout
5. reduced motion
6. performance

---

# 82. IMPLEMENTATION ACCEPTANCE TEST

The UI upgrade is complete only when all are true:

### Visual

[ ] Premium cohesive dark security design

[ ] No inconsistent cards

[ ] No arbitrary colors

[ ] Smooth transitions

[ ] Real graphs

[ ] Radar feels interactive

[ ] Network graph is investigable

[ ] Forecast is visually distinct from history

[ ] Explainability is understandable

[ ] Demo/live status is obvious

### Functional

[ ] Existing ViewModels remain connected

[ ] Real data appears in UI

[ ] No hardcoded production security scores

[ ] No random nodes

[ ] No random graphs

[ ] No fake live telemetry

[ ] API errors render correctly

[ ] Offline state renders correctly

[ ] Empty state renders correctly

[ ] Permission state renders correctly

### Performance

[ ] No unnecessary recomposition loops

[ ] No memory leak from animations

[ ] Large graphs remain responsive

[ ] Canvas drawing is optimized

[ ] Scanning never blocks UI thread

[ ] Navigation remains smooth

### Accessibility

[ ] Reduced motion

[ ] Content descriptions

[ ] Scalable typography

[ ] Touch targets

[ ] Color is not the only indicator

---

# 83. FINAL VISUAL TARGET

The final application should feel like this conceptually:

```text
                 VAJRAWORLD
                ─────────────
                  GUARDIAN

                    ◉ 82
                 PROTECTED

        ┌──────────────────────────┐
        │   OBSERVED → FORECAST    │
        │        ╭──────╮          │
        │   ─────╯      ╰────      │
        └──────────────────────────┘

              LIVE SECURITY RADAR

                  ● DEVICE
               ╱    │    ╲
             ●      ●      ●
           LINK    APP    DOMAIN
             ╲      │      ╱
                ● THREAT
                    │
                ○ FORECAST

─────────────────────────────────────

THREAT STORY
Notification → URL → APK → Network

NEXT 30 MINUTES
Risk ↑ 32% → 61%

WHY?
URL anomaly +18
Destination novelty +12
Beacon pattern +15

[EXPLAIN] [SIMULATE] [ACT]
```

The important part is not the ASCII layout.

The important part is that **every visual object represents a real state, event, relationship, forecast or user action**.

---

# 84. FINAL INSTRUCTION TO THE CODING AGENT

You are upgrading a real cybersecurity application, not designing a Dribbble mockup.

The implementation must therefore follow this priority:

```text
REAL FUNCTIONALITY
        ↓
CORRECT STATE
        ↓
CORRECT DATA
        ↓
CORRECT SECURITY SEMANTICS
        ↓
VISUALIZATION
        ↓
ANIMATION
        ↓
POLISH
```

Never reverse this order.

The UI may be extremely beautiful, animated and visually advanced, but it must never hide the truth of the underlying system.

### Final principle

**Make VajraWorld look alive because the security state is alive — not because the UI is pretending to be alive.**

That is the visual standard for the production version.
