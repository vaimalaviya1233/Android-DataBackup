package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.core.database.model.DirectoryEntity
import com.xayah.core.database.model.DirectoryUpsertEntity
import com.xayah.core.model.OpType
import com.xayah.core.model.StorageType
import kotlinx.coroutines.flow.Flow

@Dao
interface DirectoryDao {
    @Upsert(entity = DirectoryEntity::class)
    suspend fun upsert(item: DirectoryEntity)

    @Upsert(entity = DirectoryEntity::class)
    suspend fun upsert(items: List<DirectoryUpsertEntity>)

    @Query("SELECT * FROM DirectoryEntity WHERE active = 1")
    fun queryActiveDirectoriesFlow(): Flow<List<DirectoryEntity>>

    @Query("SELECT * FROM DirectoryEntity WHERE active = 1")
    suspend fun queryActiveDirectories(): List<DirectoryEntity>

    @Query("SELECT COUNT(*) FROM DirectoryEntity WHERE active = 1")
    suspend fun countActiveDirectories(): Int

    @Query("SELECT * FROM DirectoryEntity WHERE opType = :type AND selected = 1 LIMIT 1")
    suspend fun querySelectedByDirectoryType(type: OpType): DirectoryEntity?

    @Query("SELECT * FROM DirectoryEntity WHERE opType = :type AND selected = 1 LIMIT 1")
    fun querySelectedByDirectoryTypeFlow(type: OpType): Flow<DirectoryEntity?>

    @Query("SELECT id FROM DirectoryEntity WHERE parent = :parent AND child = :child AND opType = :type LIMIT 1")
    suspend fun queryId(parent: String, child: String, type: OpType): Long

    @Query("UPDATE DirectoryEntity SET active = :active")
    suspend fun updateActive(active: Boolean)

    @Query("UPDATE DirectoryEntity SET active = :active WHERE opType = :type AND storageType != :excludeType")
    suspend fun updateActive(type: OpType, excludeType: StorageType, active: Boolean)

    @Query("UPDATE DirectoryEntity SET selected = CASE WHEN id = :id THEN 1 ELSE 0 END WHERE opType = :type")
    suspend fun select(type: OpType, id: Long)

    @Delete(entity = DirectoryEntity::class)
    suspend fun delete(item: DirectoryEntity)
}
