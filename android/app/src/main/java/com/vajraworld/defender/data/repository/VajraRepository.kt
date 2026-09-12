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

    // Real-time security events & scan results flows from Room
    val securityEventsFlow: Flow<List<com.vajraworld.defender.data.local.SecurityEventEntity>> = dao.getAllSecurityEvents()
    val scanResultsFlow: Flow<List<com.vajraworld.defender.data.local.ScanResultEntity>> = dao.getAllScanResults()

    suspend fun analyzeUrlLocally(url: String, context: String? = null): com.vajraworld.defender.domain.engine.LocalUrlAnalysisResult {
        val result = com.vajraworld.defender.domain.engine.UrlRuleEngine.analyze(url, context)
        val entity = com.vajraworld.defender.data.local.ScanResultEntity(
            id = java.util.UUID.randomUUID().toString(),
            target = url,
            scanType = "URL",
            riskScore = result.riskScore,
            confidence = result.confidence,
            signalsJson = gson.toJson(result.signals),
            sha256 = null,
            createdAt = System.currentTimeMillis()
        )
        dao.insertScanResult(entity)
        return result
    }

    suspend fun analyzeFileLocally(file: java.io.File): com.vajraworld.defender.domain.engine.LocalFileAnalysisResult {
        val result = com.vajraworld.defender.domain.engine.FileInspector.inspectFile(file)
        val entity = com.vajraworld.defender.data.local.ScanResultEntity(
            id = java.util.UUID.randomUUID().toString(),
            target = file.name,
            scanType = "FILE",
            riskScore = result.riskScore,
            confidence = result.confidence,
            signalsJson = gson.toJson(result.whyPoints),
            sha256 = result.sha256,
            createdAt = System.currentTimeMillis()
        )
        dao.insertScanResult(entity)
        return result
    }

    suspend fun analyzeStreamLocally(filename: String, inputStream: java.io.InputStream, fileSize: Long = 0L): com.vajraworld.defender.domain.engine.LocalFileAnalysisResult {
        val result = com.vajraworld.defender.domain.engine.FileInspector.inspectStream(filename, inputStream, fileSize)
        val entity = com.vajraworld.defender.data.local.ScanResultEntity(
            id = java.util.UUID.randomUUID().toString(),
            target = filename,
            scanType = "FILE",
            riskScore = result.riskScore,
            confidence = result.confidence,
            signalsJson = gson.toJson(result.whyPoints),
            sha256 = result.sha256,
            createdAt = System.currentTimeMillis()
        )
        dao.insertScanResult(entity)
        return result
    }

    fun analyzeClipboardLocally(text: String?): com.vajraworld.defender.domain.engine.LocalClipboardResult {
        return com.vajraworld.defender.domain.engine.ClipboardSecretEngine.analyze(text)
    }

    suspend fun getForecastExplanations(forecastId: String): Result<ExplainabilityData> {
        return try {
            val resp = api.getExplanations(forecastId)
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                val rawAttr = (body["level2_feature_attribution"] as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()
                val rawTemp = (body["level3_temporal_evidence"] as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()

                val attributions = rawAttr.map { a ->
                    AttributionItem(
                        feature = a["feature"] as? String ?: "",
                        impact = (a["impact"] as? Number)?.toFloat() ?: 0.1f,
                        direction = a["direction"] as? String ?: "up",
                        description = a["description"] as? String ?: ""
                    )
                }

                val temporalEvents = rawTemp.map { t ->
                    TemporalEventItem(
                        timeOffset = t["time_offset"] as? String ?: "T-0s",
                        signalLevel = t["signal_level"] as? String ?: "NORMAL",
                        description = t["description"] as? String ?: "",
                        risk = (t["risk"] as? Number)?.toFloat() ?: 0.5f
                    )
                }

                val graphEv = body["level4_graph_evidence"] as? Map<String, Any>
                val centerNode = graphEv?.get("center_node") as? String ?: "Host-17"
                val uncertaintyMap = body["level5_uncertainty"] as? Map<String, Any>
                val warning = uncertaintyMap?.get("warning") as? String ?: "Sensor coverage nominal across all monitored segments."

                val data = ExplainabilityData(
                    forecastId = body["forecast_id"] as? String ?: forecastId,
                    narrative = body["level1_narrative"] as? String ?: "No explanation available.",
                    attributions = attributions,
                    temporalEvents = temporalEvents,
                    centerNode = centerNode,
                    uncertaintyWarning = warning
                )
                Result.success(data)
            } else {
                Result.failure(Exception("Explanations failed: ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getModelStatus(): Result<ModelHealthData> {
        return try {
            val resp = api.getModelStatus()
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                val data = ModelHealthData(
                    modelVersion = body["model_version"] as? String ?: "vw-0.8.0",
                    activeModelId = body["active_model_id"] as? String ?: "m-vw-hybrid-0.8.0",
                    status = body["status"] as? String ?: "HEALTHY",
                    calibration = body["calibration"] as? String ?: "dynamic_composite",
                    accuracy = (body["accuracy"] as? Number)?.toFloat() ?: 0.0f,
                    brierScore = (body["brier_score"] as? Number)?.toFloat() ?: 0.0f,
                    leadTimeSec = (body["lead_time_sec"] as? Number)?.toFloat() ?: 0.0f,
                    sensorCoverage = (body["sensor_coverage"] as? Number)?.toFloat() ?: 0.96f,
                    telemetryFreshnessSec = (body["telemetry_freshness_sec"] as? Number)?.toFloat() ?: 1.2f,
                    oodRate = (body["ood_rate"] as? Number)?.toFloat() ?: 0.02f,
                    driftScore = (body["drift_score"] as? Number)?.toFloat() ?: 0.01f,
                    inferenceLatencyMs = (body["inference_latency_ms"] as? Number)?.toFloat() ?: 14.0f,
                    environmentProfile = body["environment_profile"] as? String ?: "Enterprise IT"
                )
                Result.success(data)
            } else {
                Result.failure(Exception("Model status failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentGraph(): Result<Pair<List<TopologyNode>, List<TopologyEdge>>> {
        return try {
            val resp = api.getCurrentGraph()
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                val rawNodes = (body["nodes"] as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()
                val rawEdges = (body["edges"] as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()

                val nodes = rawNodes.mapIndexed { idx, n ->
                    TopologyNode(
                        id = n["id"] as? String ?: "node_$idx",
                        label = n["label"] as? String ?: (n["id"] as? String ?: "node"),
                        type = n["type"] as? String ?: "Host",
                        criticality = n["criticality"] as? String ?: "Medium",
                        riskScore = (n["risk"] as? Number)?.toFloat() ?: 0.3f,
                        x = (n["x"] as? Number)?.toFloat() ?: (120f + (idx % 3) * 180f),
                        y = (n["y"] as? Number)?.toFloat() ?: (160f + (idx / 3) * 160f)
                    )
                }

                val edges = rawEdges.map { e ->
                    TopologyEdge(
                        source = e["source"] as? String ?: "",
                        target = e["target"] as? String ?: "",
                        type = e["type"] as? String ?: "CONNECTS_TO",
                        weight = (e["weight"] as? Number)?.toFloat() ?: 1.0f,
                        port = (e["port"] as? Number)?.toInt() ?: 443
                    )
                }
                Result.success(Pair(nodes, edges))
            } else {
                Result.failure(Exception("Graph failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


