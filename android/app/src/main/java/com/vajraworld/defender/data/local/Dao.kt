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
}
