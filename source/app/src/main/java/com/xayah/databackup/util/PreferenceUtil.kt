package com.xayah.databackup.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xayah.core.database.model.MediaBackupOperationEntity
import com.xayah.core.database.model.MediaRestoreOperationEntity
import com.xayah.core.database.model.PackageBackupOperation
import com.xayah.core.database.model.PackageRestoreOperation
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataType
import com.xayah.core.model.LZ4_SUFFIX
import com.xayah.core.model.OperationState
import com.xayah.core.model.TAR_SUFFIX
import com.xayah.core.model.ZSTD_SUFFIX
import com.xayah.databackup.BuildConfig
import com.xayah.databackup.ui.activity.main.page.restore.PageRestore
import com.xayah.databackup.ui.component.SortState
import com.xayah.databackup.util.command.EnvUtil.getCurrentAppVersionName
import com.xayah.core.rootservice.util.ExceptionUtil.tryOn
import kotlinx.coroutines.flow.map

const val PreferenceName = "settings"

fun CompressionType.Companion.of(name: String?): CompressionType = tryOn(
    block = {
        CompressionType.valueOf(name!!.uppercase())
    },
    onException = {
        CompressionType.ZSTD
    })

fun CompressionType.Companion.suffixOf(suffix: String): CompressionType? = when (suffix) {
    TAR_SUFFIX -> CompressionType.TAR
    ZSTD_SUFFIX -> CompressionType.ZSTD
    LZ4_SUFFIX -> CompressionType.LZ4
    else -> null
}

fun DataType.Companion.of(name: String): DataType {
    return tryOn(
        block = {
            DataType.valueOf(name.uppercase())
        },
        onException = {
            DataType.PACKAGE_USER
        })
}

fun DataType.origin(userId: Int): String = when (this) {
    DataType.PACKAGE_USER -> PathUtil.getPackageUserPath(userId)
    DataType.PACKAGE_USER_DE -> PathUtil.getPackageUserDePath(userId)
    DataType.PACKAGE_DATA -> PathUtil.getPackageDataPath(userId)
    DataType.PACKAGE_OBB -> PathUtil.getPackageObbPath(userId)
    DataType.PACKAGE_MEDIA -> PathUtil.getPackageMediaPath(userId)
    else -> ""
}

fun DataType.setEntityLog(entity: PackageBackupOperation, msg: String) {
    when (this) {
        DataType.PACKAGE_APK -> entity.apkOp.log = msg
        DataType.PACKAGE_USER -> entity.userOp.log = msg
        DataType.PACKAGE_USER_DE -> entity.userDeOp.log = msg
        DataType.PACKAGE_DATA -> entity.dataOp.log = msg
        DataType.PACKAGE_OBB -> entity.obbOp.log = msg
        DataType.PACKAGE_MEDIA -> entity.mediaOp.log = msg
        else -> {}
    }
}

fun DataType.setEntityLog(entity: PackageRestoreOperation, msg: String) {
    when (this) {
        DataType.PACKAGE_APK -> entity.apkOp.log = msg
        DataType.PACKAGE_USER -> entity.userOp.log = msg
        DataType.PACKAGE_USER_DE -> entity.userDeOp.log = msg
        DataType.PACKAGE_DATA -> entity.dataOp.log = msg
        DataType.PACKAGE_OBB -> entity.obbOp.log = msg
        DataType.PACKAGE_MEDIA -> entity.mediaOp.log = msg
        else -> {}
    }
}

fun DataType.setEntityLog(entity: MediaBackupOperationEntity, msg: String) {
    when (this) {
        DataType.MEDIA_MEDIA -> entity.dataOp.log = msg
        else -> {}
    }
}

fun DataType.setEntityLog(entity: MediaRestoreOperationEntity, msg: String) {
    when (this) {
        DataType.MEDIA_MEDIA -> entity.dataOp.log = msg
        else -> {}
    }
}

fun DataType.setEntityState(entity: PackageBackupOperation, state: OperationState) {
    when (this) {
        DataType.PACKAGE_APK -> entity.apkOp.state = state
        DataType.PACKAGE_USER -> entity.userOp.state = state
        DataType.PACKAGE_USER_DE -> entity.userDeOp.state = state
        DataType.PACKAGE_DATA -> entity.dataOp.state = state
        DataType.PACKAGE_OBB -> entity.obbOp.state = state
        DataType.PACKAGE_MEDIA -> entity.mediaOp.state = state
        else -> {}
    }
}

fun DataType.setEntityState(entity: PackageRestoreOperation, state: OperationState) {
    when (this) {
        DataType.PACKAGE_APK -> entity.apkOp.state = state
        DataType.PACKAGE_USER -> entity.userOp.state = state
        DataType.PACKAGE_USER_DE -> entity.userDeOp.state = state
        DataType.PACKAGE_DATA -> entity.dataOp.state = state
        DataType.PACKAGE_OBB -> entity.obbOp.state = state
        DataType.PACKAGE_MEDIA -> entity.mediaOp.state = state
        else -> {}
    }
}

fun DataType.setEntityState(entity: MediaBackupOperationEntity, state: OperationState) {
    when (this) {
        DataType.MEDIA_MEDIA -> entity.dataOp.state = state
        else -> {}
    }
}

fun DataType.setEntityState(entity: MediaRestoreOperationEntity, state: OperationState) {
    when (this) {
        DataType.MEDIA_MEDIA -> entity.dataOp.state = state
        else -> {}
    }
}

private fun Context.savePreferences(key: String, value: String) {
    getSharedPreferences(PreferenceName, Context.MODE_PRIVATE).edit().apply {
        putString(key, value)
        apply()
    }
}

private fun Context.savePreferences(key: String, value: Boolean) {
    getSharedPreferences(PreferenceName, Context.MODE_PRIVATE).edit().apply {
        putBoolean(key, value)
        apply()
    }
}

private fun Context.savePreferences(key: String, value: Int) {
    getSharedPreferences(PreferenceName, Context.MODE_PRIVATE).edit().apply {
        putInt(key, value)
        apply()
    }
}

private fun Context.savePreferences(key: String, value: Long) {
    getSharedPreferences(PreferenceName, Context.MODE_PRIVATE).edit().apply {
        putLong(key, value)
        apply()
    }
}

private fun Context.readPreferencesString(key: String, defValue: String = ""): String? {
    getSharedPreferences(PreferenceName, Context.MODE_PRIVATE).apply {
        return getString(key, defValue)
    }
}

private fun Context.readPreferencesBoolean(key: String, defValue: Boolean): Boolean {
    getSharedPreferences(PreferenceName, Context.MODE_PRIVATE).apply {
        return getBoolean(key, defValue)
    }
}

private fun Context.readPreferencesInt(key: String, defValue: Int): Int {
    getSharedPreferences(PreferenceName, Context.MODE_PRIVATE).apply {
        return getInt(key, defValue)
    }
}

private fun Context.readPreferencesLong(key: String, defValue: Long): Long {
    getSharedPreferences(PreferenceName, Context.MODE_PRIVATE).apply {
        return getLong(key, defValue)
    }
}

fun Context.saveAppVersionName() {
    savePreferences("app_version_name", getCurrentAppVersionName())
}

fun Context.readAppVersionName(): String {
    return readPreferencesString("app_version_name") ?: ""
}

fun Context.saveMonetEnabled(value: Boolean) {
    savePreferences("monet_enabled", value)
}

fun Context.readMonetEnabled(): Boolean {
    return readPreferencesBoolean("monet_enabled", true)
}

fun Context.saveKeepScreenOn(value: Boolean) {
    savePreferences("keep_screen_on", value)
}

fun Context.readKeepScreenOn(): Boolean {
    return readPreferencesBoolean("keep_screen_on", true)
}

fun Context.saveCompressionType(ct: CompressionType) {
    savePreferences("compression_type", ct.type)
}

fun Context.readCompressionType(): CompressionType {
    return CompressionType.of(readPreferencesString("compression_type"))
}

fun Context.saveBackupItself(value: Boolean) {
    savePreferences("backup_itself", value)
}

fun Context.readBackupItself(): Boolean {
    return readPreferencesBoolean("backup_itself", true)
}

fun Context.saveCompressionTest(value: Boolean) {
    savePreferences("compression_test", value)
}

fun Context.readCompressionTest(): Boolean {
    return readPreferencesBoolean("compression_test", true)
}

fun Context.saveResetBackupList(value: Boolean) {
    savePreferences("reset_backup_list", value)
}

fun Context.readResetBackupList(): Boolean {
    return readPreferencesBoolean("reset_backup_list", false)
}

fun Context.saveResetRestoreList(value: Boolean) {
    savePreferences("reset_restore_list", value)
}

fun Context.readResetRestoreList(): Boolean {
    return readPreferencesBoolean("reset_restore_list", true)
}

fun Context.saveCompatibleMode(value: Boolean) {
    savePreferences("compatible_mode", value)
}

fun Context.readCompatibleMode(): Boolean {
    return readPreferencesBoolean("compatible_mode", false)
}

fun Context.saveCleanRestoring(value: Boolean) {
    savePreferences("clean_restoring", value)
}

fun Context.readCleanRestoring(): Boolean {
    return readPreferencesBoolean("clean_restoring", false)
}

fun Context.saveLastBackupTime(time: Long) {
    savePreferences("last_backup_time", time)
}

fun Context.readLastBackupTime(): Long {
    return readPreferencesLong("last_backup_time", 0)
}

fun Context.saveLastRestoringTime(time: Long) {
    savePreferences("last_restoring_time", time)
}

fun Context.readLastRestoringTime(): Long {
    return readPreferencesLong("last_restoring_time", 0)
}

/**
 * The source user while backing up.
 */
fun Context.saveBackupUserId(userId: Int) {
    savePreferences("backup_user_id", userId)
}

/**
 * @see [saveBackupUserId]
 */
fun Context.readBackupUserId(): Int {
    return readPreferencesInt("backup_user_id", ConstantUtil.DefaultBackupUserId)
}

/**
 * The target user while restoring.
 */
fun Context.saveRestoreUserId(userId: Int) {
    savePreferences("restore_user_id", userId)
}

/**
 * @see [saveRestoreUserId]
 */
fun Context.readRestoreUserId(): Int {
    return readPreferencesInt("restore_user_id", ConstantUtil.DefaultRestoreUserId)
}

fun Context.saveIconSaveTime(timestamp: Long) {
    savePreferences("icon_save_time", timestamp)
}

fun Context.readIconSaveTime(): Long {
    return readPreferencesLong("icon_save_time", 0)
}

fun Context.saveBackupSortTypeIndex(index: Int) {
    savePreferences("backup_sort_type_index", index)
}

fun Context.readBackupSortTypeIndex(): Int {
    return readPreferencesInt("backup_sort_type_index", 0)
}

fun Context.saveBackupSortState(state: SortState) {
    savePreferences("backup_sort_state", state.toString())
}

fun Context.readBackupSortState(): SortState {
    return SortState.of(readPreferencesString("backup_sort_state", SortState.ASCENDING.toString()))
}

fun Context.saveBackupFilterTypeIndex(index: Int) {
    savePreferences("backup_filter_type_index", index)
}

fun Context.readBackupFilterTypeIndex(): Int {
    return readPreferencesInt("backup_filter_type_index", 0)
}

fun Context.saveBackupFlagTypeIndex(index: Int) {
    savePreferences("backup_flag_type_index", index)
}

fun Context.readBackupFlagTypeIndex(): Int {
    return readPreferencesInt("backup_flag_type_index", 1)
}

fun Context.saveRestoreSortTypeIndex(index: Int) {
    savePreferences("restore_sort_type_index", index)
}

fun Context.readRestoreSortTypeIndex(): Int {
    return readPreferencesInt("restore_sort_type_index", 0)
}

fun Context.saveRestoreSortState(state: SortState) {
    savePreferences("restore_sort_state", state.toString())
}

fun Context.readRestoreSortState(): SortState {
    return SortState.of(readPreferencesString("restore_sort_state", SortState.ASCENDING.toString()))
}

fun Context.saveRestoreFilterTypeIndex(index: Int) {
    savePreferences("restore_filter_type_index", index)
}

fun Context.readRestoreFilterTypeIndex(): Int {
    return readPreferencesInt("restore_filter_type_index", 0)
}

fun Context.saveRestoreInstallationTypeIndex(index: Int) {
    savePreferences("restore_installation_type_index", index)
}

fun Context.readRestoreInstallationTypeIndex(): Int {
    return readPreferencesInt("restore_installation_type_index", 0)
}

fun Context.saveRestoreFlagTypeIndex(index: Int) {
    savePreferences("restore_flag_type_index", index)
}

fun Context.readRestoreFlagTypeIndex(): Int {
    return readPreferencesInt("restore_flag_type_index", 1)
}

fun Context.saveUpdateCheckTime(timestamp: Long) {
    savePreferences("update_check_time", timestamp)
}

fun Context.readUpdateCheckTime(): Long {
    return readPreferencesLong("update_check_time", 0)
}

fun Context.saveLatestVersionName(value: String) {
    savePreferences("latest_version_name", value)
}

fun Context.readLatestVersionName(): String {
    return readPreferencesString("latest_version_name", BuildConfig.VERSION_NAME) ?: BuildConfig.VERSION_NAME
}

fun Context.saveLatestVersionLink(value: String) {
    savePreferences("latest_version_link", value)
}

fun Context.readLatestVersionLink(): String {
    return readPreferencesString("latest_version_link", ServerUtil.LinkReleases) ?: ServerUtil.LinkReleases
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PreferenceName)
fun Context.readStoreString(key: Preferences.Key<String>, defValue: String) = dataStore.data.map { preferences -> preferences[key] ?: defValue }
fun Context.readStoreBoolean(key: Preferences.Key<Boolean>, defValue: Boolean) = dataStore.data.map { preferences -> preferences[key] ?: defValue }
fun Context.readStoreInt(key: Preferences.Key<Int>, defValue: Int) = dataStore.data.map { preferences -> preferences[key] ?: defValue }
fun Context.readStoreLong(key: Preferences.Key<Long>, defValue: Long) = dataStore.data.map { preferences -> preferences[key] ?: defValue }
suspend fun Context.saveStoreString(key: Preferences.Key<String>, value: String) = dataStore.edit { settings -> settings[key] = value }
suspend fun Context.saveStoreBoolean(key: Preferences.Key<Boolean>, value: Boolean) = dataStore.edit { settings -> settings[key] = value }
suspend fun Context.saveStoreInt(key: Preferences.Key<Int>, value: Int) = dataStore.edit { settings -> settings[key] = value }
suspend fun Context.saveStoreLong(key: Preferences.Key<Long>, value: Long) = dataStore.edit { settings -> settings[key] = value }

// -----------------------------------------Keys-----------------------------------------
val KeyBackupSavePath = stringPreferencesKey("backup_save_path")
val KeyRestoreSavePath = stringPreferencesKey("restore_save_path")
val KeyCloudActiveName = stringPreferencesKey("cloud_active_name")
val KeyFollowSymlinks = booleanPreferencesKey("follow_symlinks")
// --------------------------------------------------------------------------------------


// -----------------------------------------Read-----------------------------------------
/**
 * The final path for saving the backup.
 */
fun Context.readBackupSavePath() = readStoreString(key = KeyBackupSavePath, defValue = ConstantUtil.DefaultPath)

/**
 * It defines restore source path, user can set it at [PageRestore].
 * Databases, icons, archives and other stuffs need to be reloaded
 * each time since the path is changed.
 */
fun Context.readRestoreSavePath() = readStoreString(key = KeyRestoreSavePath, defValue = ConstantUtil.DefaultPath)

fun Context.readCloudActiveName() = readStoreString(key = KeyCloudActiveName, defValue = "")
fun Context.readFollowSymlinks() = readStoreBoolean(key = KeyFollowSymlinks, defValue = false)
// --------------------------------------------------------------------------------------


// -----------------------------------------Write-----------------------------------------
/**
 * @see [readBackupSavePath]
 */
suspend fun Context.saveBackupSavePath(value: String) = saveStoreString(key = KeyBackupSavePath, value = value.trim())

/**
 * @see [readRestoreSavePath]
 */
suspend fun Context.saveRestoreSavePath(value: String) = saveStoreString(key = KeyRestoreSavePath, value = value.trim())

suspend fun Context.saveCloudActiveName(value: String) = saveStoreString(key = KeyCloudActiveName, value = value.trim())
suspend fun Context.saveFollowSymlinks(value: Boolean) = saveStoreBoolean(key = KeyFollowSymlinks, value = value)
// ---------------------------------------------------------------------------------------
