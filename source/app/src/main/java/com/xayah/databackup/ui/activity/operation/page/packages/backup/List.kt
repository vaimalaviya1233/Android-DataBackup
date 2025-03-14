package com.xayah.databackup.ui.activity.operation.page.packages.backup

import android.content.pm.PackageInfo
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.core.database.model.OperationMask
import com.xayah.core.database.model.PackageBackupActivate
import com.xayah.core.database.model.PackageBackupEntire
import com.xayah.core.database.model.PackageBackupUpdate
import com.xayah.core.database.model.StorageStats
import com.xayah.core.util.DateUtil
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.operation.router.OperationRoutes
import com.xayah.databackup.ui.component.ApkChip
import com.xayah.databackup.ui.component.ChipDropdownMenu
import com.xayah.databackup.ui.component.DataChip
import com.xayah.databackup.ui.component.ListItemPackageBackup
import com.xayah.databackup.ui.component.ListSelectionModeTopBar
import com.xayah.databackup.ui.component.ListTopBar
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.SearchBar
import com.xayah.databackup.ui.component.SortState
import com.xayah.databackup.ui.component.SortStateChipDropdownMenu
import com.xayah.databackup.ui.component.TopSpacer
import com.xayah.databackup.ui.component.emphasizedOffset
import com.xayah.databackup.ui.component.ignorePaddingHorizontal
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.token.AnimationTokens
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.command.EnvUtil
import com.xayah.databackup.util.readBackupFilterTypeIndex
import com.xayah.databackup.util.readBackupFlagTypeIndex
import com.xayah.databackup.util.readBackupSortState
import com.xayah.databackup.util.readBackupSortTypeIndex
import com.xayah.databackup.util.readBackupUserId
import com.xayah.databackup.util.readIconSaveTime
import com.xayah.databackup.util.saveBackupFilterTypeIndex
import com.xayah.databackup.util.saveBackupFlagTypeIndex
import com.xayah.databackup.util.saveBackupSortState
import com.xayah.databackup.util.saveBackupSortTypeIndex
import com.xayah.databackup.util.saveIconSaveTime
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.rootservice.util.ExceptionUtil.tryService
import com.xayah.core.rootservice.util.withIOContext
import kotlinx.coroutines.launch
import java.text.Collator

enum class ListState {
    Idle,
    Update,
    Error,
    Done;

    fun setState(state: ListState): ListState = if (this == Error) Error else state
}

private fun sortByAlphabet(state: SortState): Comparator<PackageBackupEntire> = Comparator { entity1, entity2 ->
    if (entity1 == null && entity2 == null) {
        0
    } else if (entity1 == null) {
        -1
    } else if (entity2 == null) {
        1
    } else {
        when (state) {
            SortState.ASCENDING -> {
                Collator.getInstance().let { collator ->
                    collator.getCollationKey(entity1.label)
                        .compareTo(collator.getCollationKey(entity2.label))
                }
            }

            SortState.DESCENDING -> {
                Collator.getInstance().let { collator ->
                    collator.getCollationKey(entity2.label)
                        .compareTo(collator.getCollationKey(entity1.label))
                }
            }
        }
    }
}

private fun sortByInstallTime(state: SortState): Comparator<PackageBackupEntire> = when (state) {
    SortState.ASCENDING -> {
        compareBy { entity -> entity.firstInstallTime }
    }

    SortState.DESCENDING -> {
        compareByDescending { entity -> entity.firstInstallTime }
    }
}

private fun sortByDataSize(state: SortState): Comparator<PackageBackupEntire> = when (state) {
    SortState.ASCENDING -> {
        compareBy { entity -> entity.sizeBytes }
    }

    SortState.DESCENDING -> {
        compareByDescending { entity -> entity.sizeBytes }
    }
}

private fun sort(index: Int, state: SortState): Comparator<PackageBackupEntire> = when (index) {
    1 -> sortByInstallTime(state)
    2 -> sortByDataSize(state)
    else -> sortByAlphabet(state)
}

private fun filter(index: Int, isFlagType: Boolean): (PackageBackupEntire) -> Boolean = { packageBackupEntire ->
    if (isFlagType.not()) {
        when (index) {
            1 -> packageBackupEntire.operationCode != OperationMask.None
            2 -> packageBackupEntire.operationCode == OperationMask.None
            else -> true
        }
    } else {
        when (index) {
            1 -> packageBackupEntire.isSystemApp.not()
            2 -> packageBackupEntire.isSystemApp
            else -> true
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PackageBackupList() {
    val context = LocalContext.current
    val viewModel = hiltViewModel<ListViewModel>()
    val navController = LocalSlotScope.current!!.navController
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val uiState by viewModel.uiState
    val packagesState by uiState.packages.collectAsState(initial = listOf())
    var packageSearchPredicate: (PackageBackupEntire) -> Boolean by remember { mutableStateOf({ true }) }
    var packageSelectionPredicate: (PackageBackupEntire) -> Boolean by remember { mutableStateOf(filter(context.readBackupFilterTypeIndex(), false)) }
    var packageFlagTypePredicate: (PackageBackupEntire) -> Boolean by remember { mutableStateOf(filter(context.readBackupFlagTypeIndex(), true)) }
    var packageSortComparator: Comparator<in PackageBackupEntire> by remember {
        mutableStateOf(
            sort(
                index = context.readBackupSortTypeIndex(),
                state = context.readBackupSortState()
            )
        )
    }
    val packages = remember(packagesState, packageSearchPredicate, packageSelectionPredicate, packageFlagTypePredicate, packageSortComparator) {
        packagesState.filter(packageSearchPredicate).filter(packageSelectionPredicate).filter(packageFlagTypePredicate).sortedWith(packageSortComparator)
    }
    val selectedAPKs by uiState.selectedAPKs.collectAsState(initial = 0)
    val selectedData by uiState.selectedData.collectAsState(initial = 0)
    val selected = remember(selectedAPKs, selectedData) {
        selectedAPKs != 0 || selectedData != 0
    }
    val packageManager = remember { context.packageManager }
    var progress by remember { mutableFloatStateOf(1f) }
    var state by remember { mutableStateOf(ListState.Idle) }
    val fadeState by remember(state) {
        mutableStateOf(state == ListState.Idle)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var emphasizedState by remember { mutableStateOf(false) }
    val emphasizedOffset by emphasizedOffset(targetState = emphasizedState)
    var updatingText: String? by remember { mutableStateOf(null) }
    var selectedCount by remember { mutableIntStateOf(0) }
    val selectionMode by remember(selectedCount) { mutableStateOf(selectedCount != 0) }
    val selectAll = {
        packages.forEach { it.selected.value = true }
        selectedCount = packages.size
    }
    val deselectAll = {
        packages.forEach { it.selected.value = false }
        selectedCount = 0
    }

    LaunchedEffect(null) {
        withIOContext {
            val remoteRootService = RemoteRootService(context)
            val userId = context.readBackupUserId()

            // Inactivate all packages and activate installed only.
            viewModel.inactivatePackages()
            var installedPackages = listOf<PackageInfo>()
            tryService(onFailed = { msg ->
                scope.launch {
                    state = state.setState(ListState.Error)
                    snackbarHostState.showSnackbar(
                        message = "$msg\n${context.getString(R.string.remote_service_err_info)}",
                        duration = SnackbarDuration.Indefinite
                    )
                }
            }) {
                installedPackages = remoteRootService.getInstalledPackagesAsUser(0, userId).filter {
                    // Filter itself
                    it.packageName != context.packageName
                }
            }
            val activePackages = mutableListOf<PackageBackupActivate>()
            installedPackages.forEach { packageInfo ->
                activePackages.add(PackageBackupActivate(packageName = packageInfo.packageName, active = true))
            }
            viewModel.activatePackages(activePackages)
            val activatePackagesEndIndex = activePackages.size - 1
            state = state.setState(ListState.Update)

            val newPackages = mutableListOf<PackageBackupUpdate>()
            EnvUtil.createIconDirectory(context)
            // Update packages' info.
            val iconSaveTime = context.readIconSaveTime()
            val now = DateUtil.getTimestamp()
            val hasPassedOneDay = DateUtil.getNumberOfDaysPassed(iconSaveTime, now) >= 1
            if (hasPassedOneDay) context.saveIconSaveTime(now)
            installedPackages.forEachIndexed { index, packageInfo ->
                val iconExists = remoteRootService.exists(PathUtil.getIconPath(context, packageInfo.packageName))
                if (iconExists.not() || (iconExists && hasPassedOneDay)) {
                    val icon = packageInfo.applicationInfo.loadIcon(packageManager)
                    EnvUtil.saveIcon(context, packageInfo.packageName, icon)
                }
                val storageStats = StorageStats()
                tryService(onFailed = { msg ->
                    scope.launch {
                        state = state.setState(ListState.Error)
                        snackbarHostState.showSnackbar(
                            message = "$msg\n${context.getString(R.string.remote_service_err_info)}",
                            duration = SnackbarDuration.Indefinite
                        )
                    }
                }) {
                    remoteRootService.queryStatsForPackage(packageInfo, remoteRootService.getUserHandle(userId)).also { stats ->
                        if (stats != null) {
                            storageStats.appBytes = stats.appBytes
                            storageStats.cacheBytes = stats.cacheBytes
                            storageStats.dataBytes = stats.dataBytes
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) storageStats.externalCacheBytes = stats.externalCacheBytes
                        }
                    }
                }

                newPackages.add(
                    PackageBackupUpdate(
                        packageName = packageInfo.packageName,
                        label = packageInfo.applicationInfo.loadLabel(packageManager).toString(),
                        versionName = packageInfo.versionName ?: "",
                        versionCode = packageInfo.longVersionCode,
                        storageStats = storageStats,
                        flags = packageInfo.applicationInfo.flags,
                        firstInstallTime = packageInfo.firstInstallTime,
                        active = true
                    )
                )
                progress = index.toFloat() / activatePackagesEndIndex
                updatingText = "${context.getString(R.string.updating)} (${index + 1}/${activatePackagesEndIndex + 1})"
            }
            viewModel.updatePackages(newPackages)
            state = state.setState(ListState.Done)
            updatingText = null
            remoteRootService.destroyService()
        }
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                if (selectionMode.not()) {
                    ListTopBar(scrollBehavior = scrollBehavior, title = stringResource(id = R.string.backup_list))
                } else {
                    var allSelected by remember { mutableStateOf(false) }
                    var allApkSelected by remember { mutableStateOf(false) }
                    var allDataSelected by remember { mutableStateOf(false) }
                    ListSelectionModeTopBar(
                        scrollBehavior = scrollBehavior,
                        title = "${stringResource(id = R.string.selected)}: $selectedCount",
                        onArrowBackPressed = {
                            scope.launch {
                                withIOContext {
                                    deselectAll()
                                }
                            }
                        },
                        onCheckListPressed = {
                            scope.launch {
                                withIOContext {
                                    if (allSelected.not()) {
                                        selectAll()
                                    } else {
                                        deselectAll()
                                    }
                                    allSelected = allSelected.not()
                                }
                            }
                        },
                        chipContent = {
                            ApkChip(selected = allApkSelected, onClick = {
                                scope.launch {
                                    withIOContext {
                                        if (allApkSelected.not()) {
                                            packages.forEach { packageInfo ->
                                                if (packageInfo.selected.value) {
                                                    packageInfo.operationCode = packageInfo.operationCode or OperationMask.Apk
                                                }
                                            }
                                            viewModel.updateEntirePackages(packages)
                                        } else {
                                            packages.forEach { packageInfo ->
                                                if (packageInfo.selected.value) {
                                                    packageInfo.operationCode = packageInfo.operationCode and OperationMask.Apk.inv()
                                                }
                                            }
                                            viewModel.updateEntirePackages(packages)
                                        }
                                        allApkSelected = allApkSelected.not()
                                    }
                                }
                            })
                            DataChip(selected = allDataSelected, onClick = {
                                scope.launch {
                                    withIOContext {
                                        if (allDataSelected.not()) {
                                            packages.forEach { packageInfo ->
                                                if (packageInfo.selected.value) {
                                                    packageInfo.operationCode = packageInfo.operationCode or OperationMask.Data
                                                }
                                            }
                                            viewModel.updateEntirePackages(packages)
                                        } else {
                                            packages.forEach { packageInfo ->
                                                if (packageInfo.selected.value) {
                                                    packageInfo.operationCode = packageInfo.operationCode and OperationMask.Data.inv()
                                                }
                                            }
                                            viewModel.updateEntirePackages(packages)
                                        }
                                        allDataSelected = allDataSelected.not()
                                    }
                                }
                            })
                        }
                    )
                }
                if (progress != 1F) LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = progress
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(visible = state != ListState.Idle, enter = scaleIn(), exit = scaleOut()) {
                ExtendedFloatingActionButton(
                    modifier = Modifier
                        .padding(CommonTokens.PaddingMedium)
                        .offset(x = emphasizedOffset),
                    onClick = {
                        if (selected.not() || state == ListState.Error) emphasizedState = !emphasizedState
                        else navController.navigate(OperationRoutes.PackageBackupManifest.route)
                    },
                    expanded = selected || state != ListState.Done,
                    icon = {
                        Icon(
                            imageVector = if (state != ListState.Done) Icons.Rounded.Refresh else if (selected) Icons.Rounded.ArrowForward else Icons.Rounded.Close,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            text = if (updatingText != null) updatingText!!
                            else "$selectedAPKs ${stringResource(id = R.string.apk)}, $selectedData ${stringResource(id = R.string.data)}"
                        )
                    },
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        Column {
            TopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f)) {
                Crossfade(targetState = fadeState, label = AnimationTokens.CrossFadeLabel) { fade ->
                    if (fade.not())
                        LazyColumn(
                            modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium),
                            verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(CommonTokens.PaddingMedium))
                                SearchBar(onTextChange = { text ->
                                    deselectAll()
                                    packageSearchPredicate = { packageBackupEntire ->
                                        packageBackupEntire.label.lowercase().contains(text.lowercase())
                                                || packageBackupEntire.packageName.lowercase().contains(text.lowercase())
                                    }
                                })
                            }

                            item {
                                Row(
                                    modifier = Modifier
                                        .ignorePaddingHorizontal(CommonTokens.PaddingMedium)
                                        .horizontalScroll(rememberScrollState())
                                        .paddingHorizontal(CommonTokens.PaddingMedium),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
                                ) {
                                    SortStateChipDropdownMenu(
                                        icon = ImageVector.vectorResource(R.drawable.ic_rounded_sort),
                                        defaultSelectedIndex = remember { context.readBackupSortTypeIndex() },
                                        defaultSortState = remember { context.readBackupSortState() },
                                        list = stringArrayResource(id = R.array.backup_sort_type_items).toList(),
                                        onSelected = { index, _, state ->
                                            context.saveBackupSortTypeIndex(index)
                                            context.saveBackupSortState(state)
                                            packageSortComparator = sort(index = index, state = state)
                                        },
                                        onClick = deselectAll
                                    )

                                    ChipDropdownMenu(
                                        leadingIcon = ImageVector.vectorResource(R.drawable.ic_rounded_filter_list),
                                        defaultSelectedIndex = remember { context.readBackupFilterTypeIndex() },
                                        list = stringArrayResource(id = R.array.filter_type_items).toList(),
                                        onSelected = { index, _ ->
                                            context.saveBackupFilterTypeIndex(index)
                                            packageSelectionPredicate = filter(index, false)
                                        },
                                        onClick = deselectAll
                                    )

                                    ChipDropdownMenu(
                                        leadingIcon = ImageVector.vectorResource(R.drawable.ic_rounded_deployed_code),
                                        defaultSelectedIndex = remember { context.readBackupFlagTypeIndex() },
                                        list = stringArrayResource(id = R.array.flag_type_items).toList(),
                                        onSelected = { index, _ ->
                                            context.saveBackupFlagTypeIndex(index)
                                            packageFlagTypePredicate = filter(index, true)
                                        },
                                        onClick = deselectAll
                                    )
                                }
                            }

                            items(items = packages, key = { it.packageName }) { packageInfo ->
                                ListItemPackageBackup(packageInfo = packageInfo, selectionMode = selectionMode) {
                                    if (state == ListState.Done) {
                                        packageInfo.selected.value = packageInfo.selected.value.not()
                                        selectedCount += 1 * if (packageInfo.selected.value) 1 else -1
                                    }
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(CommonTokens.None))
                            }
                        }
                }
            }
        }
    }
}
