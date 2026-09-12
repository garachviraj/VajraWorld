package com.vajraworld.defender.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VajraDao {
    @Query("SELECT * FROM incidents_cache ORDER BY createdAt DESC")
    fun getAllIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents_cache WHERE incidentId = :id")
    suspend fun getIncidentById(id: String): IncidentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncidents(incidents: List<IncidentEntity>)

    @Query("UPDATE incidents_cache SET acknowledged = 1, status = 'INVESTIGATING' WHERE incidentId = :id")
    suspend fun acknowledgeIncident(id: String)

    @Query("SELECT * FROM forecast_cache ORDER BY cachedAt DESC LIMIT 1")
    fun getLatestForecast(): Flow<ForecastEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForecast(forecast: ForecastEntity)

    @Query("SELECT * FROM topology_nodes_cache")
    fun getTopologyNodes(): Flow<List<TopologyNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopologyNodes(nodes: List<TopologyNodeEntity>)

    // Security Events
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecurityEvent(event: SecurityEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecurityEvents(events: List<SecurityEventEntity>)

    @Query("SELECT * FROM security_events ORDER BY timestamp DESC")
    fun getAllSecurityEvents(): Flow<List<SecurityEventEntity>>

    @Query("SELECT * FROM security_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSecurityEvents(limit: Int): Flow<List<SecurityEventEntity>>

    @Query("SELECT * FROM security_events WHERE source = :source ORDER BY timestamp DESC")
    fun getSecurityEventsBySource(source: String): Flow<List<SecurityEventEntity>>

    // Scan Results
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanResult(result: ScanResultEntity)

    @Query("SELECT * FROM scan_results ORDER BY createdAt DESC")
    fun getAllScanResults(): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentScanResults(limit: Int): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results WHERE target = :target ORDER BY createdAt DESC LIMIT 1")
    suspend fun getScanResultByTarget(target: String): ScanResultEntity?

    @Query("SELECT * FROM scan_results WHERE scanType = 'URL' ORDER BY createdAt DESC")
    fun getUrlScanHistory(): Flow<List<ScanResultEntity>>

    @Query("DELETE FROM scan_results WHERE scanType = 'URL'")
    suspend fun clearUrlScanHistory()

    @Query("DELETE FROM scan_results")
    suspend fun clearAllScanResults()

    @Query("DELETE FROM security_events")
    suspend fun clearSecurityEvents()

    // Clipboard Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClipboardLog(log: ClipboardLogEntity)

    @Query("SELECT * FROM clipboard_logs ORDER BY timestamp DESC")
    fun getAllClipboardLogs(): Flow<List<ClipboardLogEntity>>

    @Query("SELECT * FROM clipboard_logs WHERE timestamp >= :startOfDayMs ORDER BY timestamp DESC")
    fun getTodayClipboardLogs(startOfDayMs: Long): Flow<List<ClipboardLogEntity>>

    @Query("DELETE FROM clipboard_logs")
    suspend fun clearClipboardLogs()
}

