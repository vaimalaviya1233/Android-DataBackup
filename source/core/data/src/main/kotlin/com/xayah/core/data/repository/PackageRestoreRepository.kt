package com.xayah.core.data.repository

import android.content.Context
import com.xayah.core.data.R
import com.xayah.core.database.dao.PackageRestoreEntireDao
import com.xayah.core.database.model.OperationMask
import com.xayah.core.database.model.PackageRestoreEntire
import com.xayah.core.datastore.readRestoreFilterFlagIndex
import com.xayah.core.datastore.readRestoreInstallationTypeIndex
import com.xayah.core.datastore.readRestoreSavePath
import com.xayah.core.datastore.readRestoreSortType
import com.xayah.core.datastore.readRestoreSortTypeIndex
import com.xayah.core.datastore.readRestoreUserId
import com.xayah.core.datastore.saveRestoreFilterFlagIndex
import com.xayah.core.datastore.saveRestoreInstallationTypeIndex
import com.xayah.core.datastore.saveRestoreSortType
import com.xayah.core.datastore.saveRestoreSortTypeIndex
import com.xayah.core.model.SortType
import com.xayah.core.service.util.PackagesBackupUtil
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.TopBarState
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.Tar
import com.xayah.core.util.filesDir
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.rootservice.util.withIOContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.text.Collator
import javax.inject.Inject

class PackageRestoreRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootService: RemoteRootService,
    private val packageRestoreDao: PackageRestoreEntireDao,
    private val pathUtil: PathUtil,
    private val packagesBackupUtil: PackagesBackupUtil,
) {
    fun observePackages(timestamp: Long) = packageRestoreDao.queryPackagesFlow(timestamp).distinctUntilChanged()
    val packages = packageRestoreDao.observeActivePackages().distinctUntilChanged()
    val packagesApkOnly = packages.map { packages -> packages.filter { it.operationCode == OperationMask.Apk } }.distinctUntilChanged()
    val packagesDataOnly = packages.map { packages -> packages.filter { it.operationCode == OperationMask.Data } }.distinctUntilChanged()
    val packagesBoth = packages.map { packages -> packages.filter { it.operationCode == OperationMask.Both } }.distinctUntilChanged()
    val selectedPackages = packageRestoreDao.observeSelectedPackages().distinctUntilChanged()
    val selectedAPKsCount = packageRestoreDao.countSelectedAPKs().distinctUntilChanged()
    val selectedDataCount = packageRestoreDao.countSelectedData().distinctUntilChanged()

    val restoreFilterFlagIndex = context.readRestoreFilterFlagIndex().distinctUntilChanged()
    val restoreSortTypeIndex = context.readRestoreSortTypeIndex().distinctUntilChanged()
    val restoreSortType = context.readRestoreSortType().distinctUntilChanged()
    val restoreInstallationTypeIndex = context.readRestoreInstallationTypeIndex().distinctUntilChanged()

    val restoreSavePath = context.readRestoreSavePath().distinctUntilChanged()
    private val configsDir = restoreSavePath.map { pathUtil.getConfigsDir(it) }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTimestamps() = restoreSavePath.flatMapLatest { packageRestoreDao.observeTimestamps(it).distinctUntilChanged() }.distinctUntilChanged()

    suspend fun saveRestoreSortType(value: SortType) = context.saveRestoreSortType(value = value)
    suspend fun saveRestoreSortTypeIndex(value: Int) = context.saveRestoreSortTypeIndex(value = value)
    suspend fun saveRestoreFilterFlagIndex(value: Int) = context.saveRestoreFilterFlagIndex(value = value)
    suspend fun saveRestoreInstallationTypeIndex(value: Int) = context.saveRestoreInstallationTypeIndex(value = value)

    suspend fun updatePackage(entity: PackageRestoreEntire) = packageRestoreDao.update(entity)
    suspend fun updateActive(active: Boolean) = packageRestoreDao.updateActive(active = active)
    suspend fun updateActive(active: Boolean, timestamp: Long, savePath: String) =
        packageRestoreDao.updateActive(active = active, timestamp = timestamp, savePath = savePath)

    suspend fun andOpCodeByMask(mask: Int, packageNames: List<String>) = packageRestoreDao.andOpCodeByMask(mask, packageNames)
    suspend fun orOpCodeByMask(mask: Int, packageNames: List<String>) = packageRestoreDao.orOpCodeByMask(mask, packageNames)

    fun getFlagPredicate(index: Int): (PackageRestoreEntire) -> Boolean = { packageRestore ->
        when (index) {
            1 -> packageRestore.isSystemApp.not()
            2 -> packageRestore.isSystemApp
            else -> true
        }
    }

    fun getInstallationPredicate(index: Int): (PackageRestoreEntire) -> Boolean = { packageRestore ->
        when (index) {
            1 -> packageRestore.installed
            2 -> packageRestore.installed.not()
            else -> true
        }
    }

    fun getKeyPredicate(key: String): (PackageRestoreEntire) -> Boolean = { packageRestore ->
        packageRestore.label.lowercase().contains(key.lowercase()) || packageRestore.packageName.lowercase().contains(key.lowercase())
    }

    fun getSortComparator(sortIndex: Int, sortType: SortType): Comparator<in PackageRestoreEntire> = when (sortIndex) {
        1 -> sortByDataSize(sortType)
        else -> sortByAlphabet(sortType)
    }

    private fun sortByAlphabet(type: SortType): Comparator<PackageRestoreEntire> = Comparator { entity1, entity2 ->
        if (entity1 != null && entity2 != null) {
            when (type) {
                SortType.ASCENDING -> {
                    Collator.getInstance().let { collator -> collator.getCollationKey(entity1.label).compareTo(collator.getCollationKey(entity2.label)) }
                }

                SortType.DESCENDING -> {
                    Collator.getInstance().let { collator -> collator.getCollationKey(entity2.label).compareTo(collator.getCollationKey(entity1.label)) }
                }
            }
        } else {
            0
        }
    }

    private fun sortByDataSize(type: SortType): Comparator<PackageRestoreEntire> = when (type) {
        SortType.ASCENDING -> {
            compareBy { entity -> entity.sizeBytes }
        }

        SortType.DESCENDING -> {
            compareByDescending { entity -> entity.sizeBytes }
        }
    }

    /**
     * Update sizeBytes, installed state.
     */
    suspend fun updatePackageState(entity: PackageRestoreEntire) = withIOContext {
        val timestampPath = "${pathUtil.getLocalRestoreArchivesPackagesDir()}/${entity.packageName}/${entity.timestamp}"
        val sizeBytes = rootService.calculateSize(timestampPath)
        val installed = rootService.queryInstalled(entity.packageName, context.readRestoreUserId().first())
        if (entity.sizeBytes != sizeBytes || entity.installed != installed) {
            updatePackage(entity.copy(sizeBytes = sizeBytes, installed = installed))
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun loadLocalConfig(topBarState: MutableStateFlow<TopBarState>) {
        val packageRestoreList: MutableList<PackageRestoreEntire> = mutableListOf()
        runCatching {
            val configPath = packagesBackupUtil.getConfigsDst(dstDir = configsDir.first())
            val bytes = rootService.readBytes(configPath)
            packageRestoreList.addAll(ProtoBuf.decodeFromByteArray<List<PackageRestoreEntire>>(bytes).toMutableList())
        }
        val packagesCount = (packageRestoreList.size - 1).coerceAtLeast(1)

        // Get 1/10 of total count.
        val epoch: Int = ((packagesCount + 1) / 10).coerceAtLeast(1)

        packageRestoreList.forEachIndexed { index, packageInfo ->
            val packageRestore =
                packageRestoreDao.queryPackage(packageName = packageInfo.packageName, timestamp = packageInfo.timestamp, savePath = packageInfo.savePath)
            val id = packageRestore?.id ?: 0
            val active = packageRestore?.active ?: false
            val operationCode = packageRestore?.operationCode ?: OperationMask.None
            packageRestoreDao.upsert(packageInfo.copy(id = id, active = active, operationCode = operationCode))

            if (index % epoch == 0)
                topBarState.emit(
                    TopBarState(
                        progress = index.toFloat() / packagesCount,
                        title = StringResourceToken.fromStringId(R.string.updating)
                    )
                )
        }
        topBarState.emit(TopBarState(progress = 1f, title = StringResourceToken.fromStringId(R.string.restore_list)))
    }

    suspend fun loadLocalIcon() {
        val archivePath = packagesBackupUtil.getIconsDst(dstDir = configsDir.first())
        Tar.decompress(src = archivePath, dst = context.filesDir(), extra = packagesBackupUtil.tarCompressionType.decompressPara)
    }
}
