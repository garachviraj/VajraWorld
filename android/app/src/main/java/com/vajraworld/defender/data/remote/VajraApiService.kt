package com.vajraworld.defender.data.remote

import retrofit2.Response
import retrofit2.http.*

data class ForecastApiRequest(
    val horizon_steps: Int = 6,
    val rollouts: Int = 32,
    val environment_profile: String = "enterprise"
)

data class SimulationApiRequest(
    val target_asset: String,
    val action_type: String,
    val parameters: Map<String, Any>? = null
)

data class LinkAnalyzeRequest(
    val url: String,
    val message_context: String? = null
)

data class FileAnalyzeRequest(
    val filename: String,
    val mock_manifest: Map<String, Any>? = null
)

data class NotificationAnalyzeRequest(
    val source_app: String,
    val message_text: String,
    val extract_links: Boolean = true
)

data class ClipboardAnalyzeRequest(
    val clipboard_text: String
)

interface VajraApiService {
    @GET("v1/live/summary")
    suspend fun getLiveSummary(): Response<Map<String, Any>>

    @GET("v1/state/current")
    suspend fun getCurrentState(): Response<Map<String, Any>>

    @GET("v1/graph/current")
    suspend fun getCurrentGraph(): Response<Map<String, Any>>

    @POST("v1/forecast")
    suspend fun getForecast(@Body request: ForecastApiRequest): Response<Map<String, Any>>

    @GET("v1/forecast/{id}/explanations")
    suspend fun getExplanations(@Path("id") forecastId: String): Response<Map<String, Any>>

    @POST("v1/simulation")
    suspend fun runSimulation(@Body request: SimulationApiRequest): Response<Map<String, Any>>

    @GET("v1/incidents")
    suspend fun getIncidents(): Response<List<Map<String, Any>>>

    @POST("v1/incidents/{id}/ack")
    suspend fun acknowledgeIncident(@Path("id") incidentId: String): Response<Map<String, Any>>

    @GET("v1/model/status")
    suspend fun getModelStatus(): Response<Map<String, Any>>

    // Guardian Multi-Surface Endpoints
    @GET("v1/guardian/radar")
    suspend fun getSecurityRadar(): Response<Map<String, Any>>

    @POST("v1/guardian/link/analyze")
    suspend fun analyzeLink(@Body request: LinkAnalyzeRequest): Response<Map<String, Any>>

    @POST("v1/guardian/file/analyze")
    suspend fun analyzeFile(@Body request: FileAnalyzeRequest): Response<Map<String, Any>>

    @POST("v1/guardian/notification/analyze")
    suspend fun analyzeNotification(@Body request: NotificationAnalyzeRequest): Response<Map<String, Any>>

    @POST("v1/guardian/clipboard/analyze")
    suspend fun analyzeClipboard(@Body request: ClipboardAnalyzeRequest): Response<Map<String, Any>>

    @GET("v1/guardian/threat-stories")
    suspend fun getThreatStories(): Response<List<Map<String, Any>>>
}
