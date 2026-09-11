package com.vajraworld.defender.domain.model

enum class AttackStage(val displayName: String) {
    BENIGN("Benign"),
    RECONNAISSANCE("Reconnaissance"),
    INITIAL_ACCESS("Initial Access"),
    DISCOVERY("Discovery"),
    CREDENTIAL_ACCESS("Credential Access"),
    LATERAL_MOVEMENT("Lateral Movement"),
    COMMAND_AND_CONTROL("Command & Control"),
    COLLECTION("Collection"),
    EXFILTRATION("Exfiltration"),
    IMPACT("Impact"),
    UNKNOWN("Unknown")
}

enum class CoverageState {
    NOMINAL,
    DEGRADED,
    STALE,
    OFFLINE
}

data class ForecastPoint(
    val stepIndex: Int,
    val timeOffset: String,
    val risk: Float
)

data class FutureBranch(
    val name: String,
    val probability: Float,
    val trajectoryTrend: String,
    val terminalStage: String,
    val meanFinalRisk: Float
)

data class DriverSignal(
    val feature: String,
    val impact: Float,
    val direction: String
)

data class TopologyNode(
    val id: String,
    val label: String,
    val type: String,
    val criticality: String,
    val riskScore: Float,
    var x: Float = 0f,
    var y: Float = 0f
)

data class TopologyEdge(
    val source: String,
    val target: String,
    val type: String,
    val weight: Float,
    val port: Int
)

data class Incident(
    val incidentId: String,
    val title: String,
    val status: String,
    val severity: String,
    val risk: Float,
    val confidence: Float,
    val etaSeconds: Int,
    val predictedStage: String,
    val affectedAssets: List<String>,
    val evidence: List<Map<String, Any>>,
    val recommendedAction: String,
    val acknowledged: Boolean,
    val createdAt: String
)

data class SimulationResult(
    val simulationId: String,
    val targetAsset: String,
    val actionType: String,
    val baselineRisk: Float,
    val postActionRisk: Float,
    val residualRisk: Float,
    val riskReductionPct: Int,
    val newLikelyStage: String,
    val disruptionRating: String,
    val utilityScore: Float,
    val isRecommended: Boolean
)

data class AttributionItem(
    val feature: String,
    val impact: Float,
    val direction: String,
    val description: String
)

data class TemporalEventItem(
    val timeOffset: String,
    val signalLevel: String,
    val description: String,
    val risk: Float
)

data class ExplainabilityData(
    val forecastId: String,
    val narrative: String,
    val attributions: List<AttributionItem>,
    val temporalEvents: List<TemporalEventItem>,
    val centerNode: String,
    val uncertaintyWarning: String
)

data class ModelHealthData(
    val modelVersion: String,
    val activeModelId: String,
    val status: String,
    val calibration: String,
    val accuracy: Float,
    val brierScore: Float,
    val leadTimeSec: Float,
    val sensorCoverage: Float,
    val telemetryFreshnessSec: Float,
    val oodRate: Float,
    val driftScore: Float,
    val inferenceLatencyMs: Float,
    val environmentProfile: String
)

// ----------------- GUARDIAN DOMAIN MODELS -----------------

data class GuardianLinkAnalysis(
    val url: String,
    val riskScore: Int,
    val confidence: Float,
    val whyPoints: List<String>,
    val recommendedAction: String,
    val entropy: Float,
    val progressionTrajectory: List<Map<String, Any>>,
    val hasUrgentContext: Boolean
)

data class GuardianFileAnalysis(
    val filename: String,
    val sha256: String,
    val isApk: Boolean,
    val riskScore: Int,
    val confidence: Float,
    val whyPoints: List<String>,
    val recommendedAction: String,
    val permissionsAnalyzed: List<String>,
    val progressionTrajectory: List<Map<String, Any>>,
    val archiveSafe: Boolean
)

data class ThreatStoryStep(
    val surface: String,
    val time: String,
    val event: String,
    val risk: Int
)

data class ThreatStory(
    val storyId: String,
    val title: String,
    val narrative: String,
    val confidence: Float,
    val riskScore: Int,
    val mitreTactics: List<String>,
    val timelineSteps: List<ThreatStoryStep>,
    val predictedNextStage: String,
    val recommendedInterventions: List<String>
)

data class RadarNode(
    val id: String,
    val label: String,
    val surface: String,
    val risk: Int,
    val status: String,
    var x: Float = 0f,
    var y: Float = 0f
)

data class RadarEdge(
    val source: String,
    val target: String,
    val type: String,
    val isPredicted: Boolean
)

data class SecurityRadarState(
    val radarTitle: String,
    val overallStatus: String,
    val overallHealth: Int,
    val nodes: List<RadarNode>,
    val edges: List<RadarEdge>
)

