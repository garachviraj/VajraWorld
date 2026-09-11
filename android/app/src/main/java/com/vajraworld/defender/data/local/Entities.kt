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
