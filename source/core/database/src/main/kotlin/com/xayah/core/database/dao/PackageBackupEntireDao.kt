package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Update
import androidx.room.Upsert
import com.xayah.core.database.model.PackageBackupActivate
import com.xayah.core.database.model.PackageBackupEntire
import com.xayah.core.database.model.PackageBackupManifest
import com.xayah.core.database.model.PackageBackupOp
import com.xayah.core.database.model.PackageBackupUpdate
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageBackupEntireDao {
    @Upsert(entity = PackageBackupEntire::class)
    suspend fun upsert(items: List<PackageBackupUpdate>)

    @Update(entity = PackageBackupEntire::class)
    suspend fun update(items: List<PackageBackupActivate>)

    @Upsert(entity = PackageBackupEntire::class)
    suspend fun upsertEntire(items: List<PackageBackupEntire>)

    @Update(entity = PackageBackupEntire::class)
    suspend fun update(item: PackageBackupEntire)

    @Update(entity = PackageBackupEntire::class)
    suspend fun update(item: PackageBackupOp)

    @Query("SELECT * FROM PackageBackupEntire WHERE active = 1")
    fun queryActivePackages(): Flow<List<PackageBackupEntire>>

    @Query("SELECT * FROM PackageBackupEntire WHERE active = 1 AND (operationCode = 1 OR operationCode = 2 OR operationCode = 3)")
    fun querySelectedPackagesFlow(): Flow<List<PackageBackupEntire>>

    @Query("SELECT * FROM PackageBackupEntire WHERE active = 1 AND (operationCode = 1 OR operationCode = 2 OR operationCode = 3)")
    suspend fun querySelectedPackages(): List<PackageBackupEntire>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM PackageBackupEntire WHERE packageName = :packageName LIMIT 1")
    suspend fun queryManifestPackage(packageName: String): PackageBackupManifest

    @Query("SELECT packageName, label, operationCode, versionName, versionCode, flags FROM PackageBackupEntire WHERE active = 1 AND (operationCode = 1 OR operationCode = 2 OR operationCode = 3)")
    suspend fun queryActiveTotalPackages(): List<PackageBackupOp>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM PackageBackupEntire WHERE active = 1 AND operationCode = 3")
    fun queryActiveBothPackages(): Flow<List<PackageBackupManifest>>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM PackageBackupEntire WHERE active = 1 AND operationCode = 2")
    fun queryActiveAPKOnlyPackages(): Flow<List<PackageBackupManifest>>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM PackageBackupEntire WHERE active = 1 AND operationCode = 1")
    fun queryActiveDataOnlyPackages(): Flow<List<PackageBackupManifest>>

    @Query("SELECT COUNT(*) FROM PackageBackupEntire WHERE active = 1")
    suspend fun countActivePackages(): Int

    @Query("SELECT COUNT(*) FROM PackageBackupEntire WHERE active = 1 AND (operationCode = 1 OR operationCode = 2 OR operationCode = 3)")
    fun countSelectedTotal(): Flow<Int>

    @Query("SELECT COUNT(*) FROM PackageBackupEntire WHERE active = 1 AND operationCode = 3")
    fun countSelectedBoth(): Flow<Int>

    @Query("SELECT COUNT(*) FROM PackageBackupEntire WHERE active = 1 AND (operationCode = 2 OR operationCode = 3)")
    fun countSelectedAPKs(): Flow<Int>

    @Query("SELECT COUNT(*) FROM PackageBackupEntire WHERE active = 1 AND (operationCode = 1 OR operationCode = 3)")
    fun countSelectedData(): Flow<Int>

    @Query("UPDATE PackageBackupEntire SET active = :active")
    suspend fun updateActive(active: Boolean)

    @Query("UPDATE PackageBackupEntire SET operationCode = (operationCode & :mask) WHERE packageName in (:packageNames)")
    suspend fun andOpCodeByMask(mask: Int, packageNames: List<String>)

    @Query("UPDATE PackageBackupEntire SET operationCode = (operationCode | :mask) WHERE packageName in (:packageNames)")
    suspend fun orOpCodeByMask(mask: Int, packageNames: List<String>)
}
