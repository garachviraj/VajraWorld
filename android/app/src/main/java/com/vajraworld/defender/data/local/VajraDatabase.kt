package com.vajraworld.defender.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        IncidentEntity::class,
        ForecastEntity::class,
        TopologyNodeEntity::class,
        SecurityEventEntity::class,
        ScanResultEntity::class,
        ClipboardLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class VajraDatabase : RoomDatabase() {
    abstract fun dao(): VajraDao

    companion object {
        @Volatile
        private var INSTANCE: VajraDatabase? = null

        fun getInstance(context: Context): VajraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VajraDatabase::class.java,
                    "vajraworld_defender.db"
                ).fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
