package com.xayah.databackup.ui.activity.main.page.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.databackup.DataBackupApplication
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.SettingsGridItem
import com.xayah.databackup.ui.component.SettingsModalDropdownMenu
import com.xayah.databackup.ui.component.SettingsSwitch
import com.xayah.databackup.ui.component.SettingsTitle
import com.xayah.databackup.ui.component.VerticalGrid
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.component.paddingVertical
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.core.model.CompressionType
import com.xayah.databackup.util.of
import com.xayah.databackup.util.readBackupItself
import com.xayah.databackup.util.readBackupUserId
import com.xayah.databackup.util.readCleanRestoring
import com.xayah.databackup.util.readCompatibleMode
import com.xayah.databackup.util.readCompressionTest
import com.xayah.databackup.util.readCompressionType
import com.xayah.databackup.util.readFollowSymlinks
import com.xayah.databackup.util.readKeepScreenOn
import com.xayah.databackup.util.readMonetEnabled
import com.xayah.databackup.util.readResetBackupList
import com.xayah.databackup.util.readResetRestoreList
import com.xayah.databackup.util.readRestoreUserId
import com.xayah.databackup.util.saveBackupItself
import com.xayah.databackup.util.saveBackupUserId
import com.xayah.databackup.util.saveCleanRestoring
import com.xayah.databackup.util.saveCompatibleMode
import com.xayah.databackup.util.saveCompressionTest
import com.xayah.databackup.util.saveCompressionType
import com.xayah.databackup.util.saveFollowSymlinks
import com.xayah.databackup.util.saveKeepScreenOn
import com.xayah.databackup.util.saveMonetEnabled
import com.xayah.databackup.util.saveResetBackupList
import com.xayah.databackup.util.saveResetRestoreList
import com.xayah.databackup.util.saveRestoreUserId
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.rootservice.util.ExceptionUtil.tryService
import com.xayah.core.rootservice.util.withIOContext
import com.xayah.core.rootservice.util.withMainContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Composable
private fun InfoCard() {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .paddingHorizontal(CommonTokens.PaddingMedium)
    ) {
        val context = LocalContext.current
        val viewModel = hiltViewModel<SettingsViewModel>()
        val uiState by viewModel.uiState
        val items = uiState.infoCardItems

        LaunchedEffect(null) {
            initialize(context, viewModel)
        }

        VerticalGrid(columns = 2, count = items.size) { index ->
            SettingsGridItem(
                modifier = Modifier
                    .padding(CommonTokens.PaddingMedium)
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { if (items[index].onClick != null) items[index].onClick!!.invoke() },
                config = items[index]
            )
        }
    }
}

@Composable
private fun ApplicationSettings() {
    val context = LocalContext.current
    SettingsSwitch(
        icon = ImageVector.vectorResource(R.drawable.ic_round_auto_awesome),
        title = stringResource(R.string.monet),
        defaultValue = remember { context.readMonetEnabled() },
        content = stringResource(R.string.monet_desc)
    ) {
        context.saveMonetEnabled(it)
        DataBackupApplication.monetEnabled.value = it
    }
    SettingsSwitch(
        icon = ImageVector.vectorResource(R.drawable.ic_rounded_brightness_high),
        title = stringResource(R.string.keep_screen_on),
        defaultValue = remember { context.readKeepScreenOn() },
        content = stringResource(R.string.keep_screen_on_desc)
    ) {
        context.saveKeepScreenOn(it)
    }

    val ctList = remember { listOf(CompressionType.TAR, CompressionType.ZSTD, CompressionType.LZ4).map { it.type } }
    val ctIndex = remember { ctList.indexOf(context.readCompressionType().type) }
    SettingsModalDropdownMenu(
        icon = ImageVector.vectorResource(R.drawable.ic_rounded_bolt),
        title = stringResource(R.string.compression_type),
        content = stringResource(R.string.compression_type_desc),
        defaultValue = ctIndex,
        list = ctList
    ) { _, selected ->
        context.saveCompressionType(CompressionType.of(selected))
    }
}

@Composable
private fun BackupUserSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var userIdList: List<Int> by remember { mutableStateOf(listOf(context.readBackupUserId())) }
    var userList: List<String> by remember { mutableStateOf(listOf()) }
    var userIdIndex: Int by remember { mutableIntStateOf(0) }
    SettingsModalDropdownMenu(
        icon = ImageVector.vectorResource(R.drawable.ic_rounded_person),
        title = stringResource(R.string.backup_user),
        content = stringResource(R.string.backup_user_desc),
        selectedIndex = userIdIndex,
        displayValue = userIdList[userIdIndex].toString(),
        onLoading = {
            val remoteRootService = RemoteRootService(context)
            tryService(onFailed = { msg ->
                scope.launch {
                    withMainContext {
                        Toast.makeText(context, "$msg\n${context.getString(R.string.remote_service_err_info)}", Toast.LENGTH_LONG).show()
                    }
                }
            }) {
                val oldUserId = userIdList[userIdIndex]
                val users = remoteRootService.getUsers()
                userList = users.map { "${it.id}: ${it.name}" }
                userIdList = users.map { it.id }
                userIdIndex = userIdList.indexOf(oldUserId)
                if (userIdIndex == -1) {
                    // UserId doesn't exist any more.
                    userIdIndex = 0
                    context.saveBackupUserId(userIdList[userIdIndex])
                }
                remoteRootService.destroyService()
            }
            userList
        }
    ) { index, _ ->
        userIdIndex = index
        context.saveBackupUserId(userIdList[userIdIndex])
    }
}

@Composable
private fun RestoreUserSettings() {
    val context = LocalContext.current
    var userIdList: List<Int> by remember { mutableStateOf(listOf(context.readRestoreUserId())) }
    var userList: List<String> by remember { mutableStateOf(listOf()) }
    var userIdIndex: Int by remember { mutableIntStateOf(0) }
    SettingsModalDropdownMenu(
        icon = ImageVector.vectorResource(R.drawable.ic_rounded_phone_android),
        title = stringResource(R.string.restore_user),
        content = stringResource(R.string.restore_user_desc),
        selectedIndex = userIdIndex,
        displayValue = userIdList[userIdIndex].toString(),
        onLoading = {
            val remoteRootService = RemoteRootService(context)
            tryService(onFailed = { msg ->
                Toast.makeText(context, "$msg\n${context.getString(R.string.remote_service_err_info)}", Toast.LENGTH_LONG).show()
            }) {
                val oldUserId = userIdList[userIdIndex]
                val users = remoteRootService.getUsers()
                userList = users.map { "${it.id}: ${it.name}" }
                userIdList = users.map { it.id }
                userIdIndex = userIdList.indexOf(oldUserId)
                if (userIdIndex == -1) {
                    // UserId doesn't exist any more.
                    userIdIndex = 0
                    context.saveRestoreUserId(userIdList[userIdIndex])
                }
                remoteRootService.destroyService()
            }
            userList
        }
    ) { index, _ ->
        userIdIndex = index
        context.saveRestoreUserId(userIdList[userIdIndex])
    }
}

@Composable
private fun UserSettings() {
    BackupUserSettings()
    RestoreUserSettings()
}

@Composable
private fun BackupSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    SettingsSwitch(
        icon = ImageVector.vectorResource(R.drawable.ic_rounded_join_inner),
        title = stringResource(R.string.backup_itself),
        defaultValue = remember { context.readBackupItself() },
        content = stringResource(R.string.backup_itself_desc)
    ) {
        context.saveBackupItself(it)
    }
    SettingsSwitch(
        icon = ImageVector.vectorResource(R.drawable.ic_rounded_layers),
        title = stringResource(R.string.compression_test),
        defaultValue = remember { context.readCompressionTest() },
        content = stringResource(R.string.compression_test_desc)
    ) {
        context.saveCompressionTest(it)
    }
    SettingsSwitch(
        icon = ImageVector.vectorResource(R.drawable.ic_rounded_restart_alt),
        title = stringResource(R.string.reset_backup_list),
        defaultValue = remember { context.readResetBackupList() },
        content = stringResource(R.string.reset_backup_list_desc)
    ) {
        context.saveResetBackupList(it)
    }
    SettingsSwitch(
        icon = ImageVector.vectorResource(R.drawable.ic_rounded_build),
        title = stringResource(R.string.compatible_mode),
        defaultValue = remember { context.readCompatibleMode() },
        content = stringResource(R.string.compatible_mode_desc)
    ) {
        context.saveCompatibleMode(it)
    }
    SettingsSwitch(
        icon = Icons.Rounded.Link,
        title = stringResource(R.string.follow_symlinks),
        defaultValue = remember { runBlocking { context.readFollowSymlinks().first() } },
        content = stringResource(R.string.follow_symlinks_desc)
    ) {
        scope.launch {
            withIOContext {
                context.saveFollowSymlinks(it)
            }
        }
    }
}

@Composable
private fun RestoreSettings() {
    val context = LocalContext.current
    SettingsSwitch(
        icon = ImageVector.vectorResource(R.drawable.ic_rounded_mop),
        title = stringResource(R.string.clean_restoring),
        defaultValue = remember { context.readCleanRestoring() },
        content = stringResource(R.string.clean_restoring_desc)
    ) {
        context.saveCleanRestoring(it)
    }
    SettingsSwitch(
        icon = ImageVector.vectorResource(R.drawable.ic_rounded_restart_alt),
        title = stringResource(R.string.reset_restore_list),
        defaultValue = remember { context.readResetRestoreList() },
        content = stringResource(R.string.reset_restore_list_desc)
    ) {
        context.saveResetRestoreList(it)
    }
}

@ExperimentalLayoutApi
@ExperimentalMaterial3Api
@Composable
fun PageSettings() {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.paddingVertical(CommonTokens.PaddingMedium))
        InfoCard()
        Divider(modifier = Modifier.padding(CommonTokens.PaddingMedium))
        ApplicationSettings()
        SettingsTitle(modifier = Modifier.paddingVertical(CommonTokens.PaddingMedium), title = stringResource(R.string.user))
        UserSettings()
        SettingsTitle(modifier = Modifier.paddingVertical(CommonTokens.PaddingMedium), title = stringResource(R.string.backup))
        BackupSettings()
        SettingsTitle(modifier = Modifier.paddingVertical(CommonTokens.PaddingMedium), title = stringResource(R.string.restore))
        RestoreSettings()
        Spacer(modifier = Modifier.paddingVertical(CommonTokens.PaddingMedium))
    }
}
