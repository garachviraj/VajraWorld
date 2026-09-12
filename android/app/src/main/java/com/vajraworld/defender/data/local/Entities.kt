package com.vajraworld.defender.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents_cache")
data class IncidentEntity(
    @PrimaryKey val incidentId: String,
    val title: String,
    val status: String,
    val severity: String,
    val risk: Float,
    val confidence: Float,
    val etaSeconds: Int,
    val predictedStage: String,
    val affectedAssetsJson: String,
    val evidenceJson: String,
    val recommendedAction: String,
    val acknowledged: Boolean,
    val createdAt: String
)

@Entity(tableName = "forecast_cache")
data class ForecastEntity(
    @PrimaryKey val forecastId: String,
    val currentRisk: Float,
    val horizonRisksJson: String,
    val predictedStage: String,
    val confidence: Float,
    val leadTimeSec: Int,
    val cachedAt: Long
)

@Entity(tableName = "topology_nodes_cache")
data class TopologyNodeEntity(
    @PrimaryKey val id: String,
    val label: String,
    val type: String,
    val criticality: String,
    val riskScore: Float
)

@Entity(tableName = "security_events")
data class SecurityEventEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val eventType: String,
    val source: String,
    val risk: Float,
    val confidence: Float,
    val explanation: String,
    val rawContentHash: String,
    val isSynthetic: Boolean = false
)

@Entity(tableName = "scan_results")
data class ScanResultEntity(
    @PrimaryKey val id: String,
    val target: String,
    val scanType: String, // "URL" or "FILE"
    val riskScore: Int,
    val confidence: Float,
    val signalsJson: String,
    val sha256: String?,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "clipboard_logs")
data class ClipboardLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val maskedPreview: String,
    val detectedType: String,
    val riskScore: Int,
    val isSensitive: Boolean,
    val recommendation: String
)


