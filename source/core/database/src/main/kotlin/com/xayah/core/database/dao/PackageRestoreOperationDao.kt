package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.core.database.model.PackageBackupOperation
import com.xayah.core.database.model.PackageRestoreOperation
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageRestoreOperationDao {
    @Upsert(entity = PackageRestoreOperation::class)
    suspend fun upsert(item: PackageRestoreOperation): Long

    @Query("SELECT * FROM PackageRestoreOperation WHERE timestamp = :timestamp ORDER BY id DESC")
    fun queryOperationsFlow(timestamp: Long): Flow<List<PackageRestoreOperation>>

    @Query("SELECT timestamp FROM PackageRestoreOperation ORDER BY id DESC LIMIT 1")
    suspend fun queryLastOperationTime(): Long

    @Query("SELECT startTimestamp FROM PackageRestoreOperation WHERE timestamp = :timestamp ORDER BY id LIMIT 1")
    suspend fun queryFirstOperationStartTime(timestamp: Long): Long

    @Query("SELECT endTimestamp FROM PackageRestoreOperation WHERE timestamp = :timestamp ORDER BY id DESC LIMIT 1")
    suspend fun queryLastOperationEndTime(timestamp: Long): Long

    @Query("SELECT * FROM PackageRestoreOperation WHERE timestamp = :timestamp ORDER BY id DESC LIMIT 1")
    fun queryLastOperationPackage(timestamp: Long): Flow<PackageRestoreOperation>

    @Query("SELECT COUNT(*) FROM PackageRestoreOperation WHERE timestamp = :timestamp AND endTimestamp != 0")
    fun countByTimestamp(timestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM PackageRestoreOperation WHERE timestamp = :timestamp AND packageState = 'DONE'")
    suspend fun countSucceedByTimestamp(timestamp: Long): Int

    @Query("SELECT COUNT(*) FROM PackageRestoreOperation WHERE timestamp = :timestamp AND packageState = 'ERROR'")
    suspend fun countFailedByTimestamp(timestamp: Long): Int
}
