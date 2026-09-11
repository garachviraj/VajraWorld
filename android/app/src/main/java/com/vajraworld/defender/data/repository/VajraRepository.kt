package com.vajraworld.defender.data.repository

import com.vajraworld.defender.data.local.VajraDao
import com.vajraworld.defender.data.local.IncidentEntity
import com.vajraworld.defender.data.local.ForecastEntity
import com.vajraworld.defender.data.remote.ApiClient
import com.vajraworld.defender.data.remote.ForecastApiRequest
import com.vajraworld.defender.data.remote.SimulationApiRequest
import com.vajraworld.defender.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.gson.Gson

class VajraRepository(private val dao: VajraDao) {
    private val api = ApiClient.service
    private val gson = Gson()

    // Offline-first Incidents stream
    val incidentsFlow: Flow<List<Incident>> = dao.getAllIncidents().map { entities ->
        entities.map { e ->
            Incident(
                incidentId = e.incidentId,
                title = e.title,
                status = e.status,
                severity = e.severity,
                risk = e.risk,
                confidence = e.confidence,
                etaSeconds = e.etaSeconds,
                predictedStage = e.predictedStage,
                affectedAssets = emptyList(),
                evidence = emptyList(),
                recommendedAction = e.recommendedAction,
                acknowledged = e.acknowledged,
                createdAt = e.createdAt
            )
        }
    }

    suspend fun refreshIncidents(): Result<Unit> {
        return try {
            val resp = api.getIncidents()
            if (resp.isSuccessful && resp.body() != null) {
                val entities = resp.body()!!.map { item ->
                    IncidentEntity(
                        incidentId = item["incident_id"] as? String ?: "INC-0",
                        title = item["title"] as? String ?: "Alert",
                        status = item["status"] as? String ?: "OPEN",
                        severity = item["severity"] as? String ?: "HIGH",
                        risk = (item["risk"] as? Number)?.toFloat() ?: 0.5f,
                        confidence = (item["confidence"] as? Number)?.toFloat() ?: 0.5f,
                        etaSeconds = (item["eta_seconds"] as? Number)?.toInt() ?: 60,
                        predictedStage = item["predicted_stage"] as? String ?: "Benign",
                        affectedAssetsJson = "",
                        evidenceJson = "",
                        recommendedAction = item["recommended_action"] as? String ?: "Isolate",
                        acknowledged = item["acknowledged"] as? Boolean ?: false,
                        createdAt = item["created_at"] as? String ?: ""
                    )
                }
                dao.insertIncidents(entities)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch incidents: ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getForecast(): Result<Map<String, Any>> {
        return try {
            val resp = api.getForecast(ForecastApiRequest())
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception("Forecast error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun runSimulation(targetAsset: String, actionType: String): Result<SimulationResult> {
        return try {
            val resp = api.runSimulation(SimulationApiRequest(target_asset = targetAsset, action_type = actionType))
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                val result = SimulationResult(
                    simulationId = body["simulation_id"] as? String ?: "",
                    targetAsset = body["target_asset"] as? String ?: targetAsset,
                    actionType = body["action_type"] as? String ?: actionType,
                    baselineRisk = (body["baseline_risk"] as? Number)?.toFloat() ?: 0.7f,
                    postActionRisk = (body["post_action_risk"] as? Number)?.toFloat() ?: 0.2f,
                    residualRisk = (body["residual_risk"] as? Number)?.toFloat() ?: 0.2f,
                    riskReductionPct = (body["risk_reduction_pct"] as? Number)?.toInt() ?: 50,
                    newLikelyStage = body["new_likely_stage"] as? String ?: "Contained",
                    disruptionRating = body["disruption_rating"] as? String ?: "Medium",
                    utilityScore = (body["utility_score"] as? Number)?.toFloat() ?: 0.4f,
                    isRecommended = body["is_recommended"] as? Boolean ?: true
                )
                Result.success(result)
            } else {
                Result.failure(Exception("Simulation error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acknowledgeIncident(id: String) {
        dao.acknowledgeIncident(id)
        try {
            api.acknowledgeIncident(id)
        } catch (_: Exception) {}
    }

    suspend fun getLiveSummary(): Result<Map<String, Any>> {
        return try {
            val resp = api.getLiveSummary()
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception("Live summary error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSecurityRadar(): Result<SecurityRadarState> {
        return try {
            val resp = api.getSecurityRadar()
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                val rawNodes = (body["nodes"] as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()
                val rawEdges = (body["edges"] as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()

                val nodes = rawNodes.map { n ->
                    RadarNode(
                        id = n["id"] as? String ?: "",
                        label = n["label"] as? String ?: "",
                        surface = n["surface"] as? String ?: "",
                        risk = (n["risk"] as? Number)?.toInt() ?: 20,
                        status = n["status"] as? String ?: "NOMINAL",
                        x = (n["x"] as? Number)?.toFloat() ?: 300f,
                        y = (n["y"] as? Number)?.toFloat() ?: 200f
                    )
                }

                val edges = rawEdges.map { e ->
                    RadarEdge(
                        source = e["source"] as? String ?: "",
                        target = e["target"] as? String ?: "",
                        type = e["type"] as? String ?: "",
                        isPredicted = e["is_predicted"] as? Boolean ?: false
                    )
                }

                Result.success(
                    SecurityRadarState(
                        radarTitle = body["radar_title"] as? String ?: "VAJRAWORLD GUARDIAN LIVE SECURITY RADAR",
                        overallStatus = body["overall_status"] as? String ?: "NOMINAL",
                        overallHealth = (body["overall_health"] as? Number)?.toInt() ?: 80,
                        nodes = nodes,
                        edges = edges
                    )
                )
            } else {
                Result.failure(Exception("Radar fetch error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeLink(url: String, context: String?): Result<GuardianLinkAnalysis> {
        return try {
            val resp = api.analyzeLink(com.vajraworld.defender.data.remote.LinkAnalyzeRequest(url = url, message_context = context))
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                val result = GuardianLinkAnalysis(
                    url = body["url"] as? String ?: url,
                    riskScore = (body["risk_score"] as? Number)?.toInt() ?: 50,
                    confidence = (body["confidence"] as? Number)?.toFloat() ?: 0.9f,
                    whyPoints = (body["why_points"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    recommendedAction = body["recommended_action"] as? String ?: "Allow",
                    entropy = (body["entropy"] as? Number)?.toFloat() ?: 3.5f,
                    progressionTrajectory = (body["progression_trajectory"] as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList(),
                    hasUrgentContext = context?.isNotEmpty() == true
                )
                Result.success(result)
            } else {
                Result.failure(Exception("Link analyze failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeFile(filename: String, mockManifest: Map<String, Any>? = null): Result<GuardianFileAnalysis> {
        return try {
            val resp = api.analyzeFile(com.vajraworld.defender.data.remote.FileAnalyzeRequest(filename = filename, mock_manifest = mockManifest))
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                val result = GuardianFileAnalysis(
                    filename = body["filename"] as? String ?: filename,
                    sha256 = body["sha256"] as? String ?: "",
                    isApk = body["is_apk"] as? Boolean ?: true,
                    riskScore = (body["risk_score"] as? Number)?.toInt() ?: 50,
                    confidence = (body["confidence"] as? Number)?.toFloat() ?: 0.9f,
                    whyPoints = (body["why_points"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    recommendedAction = body["recommended_action"] as? String ?: "Allow",
                    permissionsAnalyzed = (body["permissions_analyzed"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    progressionTrajectory = (body["progression_trajectory"] as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList(),
                    archiveSafe = body["archive_safe"] as? Boolean ?: true
                )
                Result.success(result)
            } else {
                Result.failure(Exception("File analyze failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeClipboard(text: String): Result<Map<String, Any>> {
        return try {
            val resp = api.analyzeClipboard(com.vajraworld.defender.data.remote.ClipboardAnalyzeRequest(clipboard_text = text))
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception("Clipboard analyze failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

