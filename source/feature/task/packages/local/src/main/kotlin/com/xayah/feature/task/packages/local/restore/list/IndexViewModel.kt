package com.xayah.feature.task.packages.local.restore.list

import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.UiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.data.repository.PackageRestoreRepository
import com.xayah.core.database.model.OperationMask
import com.xayah.core.database.model.PackageRestoreEntire
import com.xayah.core.model.SortType
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.TopBarState
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.util.DateUtil
import com.xayah.feature.task.packages.common.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class IndexUiState(
    val activating: Boolean = true,
    val shimmerCount: Int = 9,
    val emphasizedState: Boolean = false,
    val batchSelection: List<String> = listOf(),
) : UiState

sealed class IndexUiIntent : UiIntent {
    object Initialize : IndexUiIntent()
    object Update : IndexUiIntent()
    data class UpdatePackage(val entity: PackageRestoreEntire) : IndexUiIntent()
    data class FilterByKey(val key: String) : IndexUiIntent()
    data class Sort(val index: Int, val type: SortType) : IndexUiIntent()
    data class FilterByFlag(val index: Int) : IndexUiIntent()
    data class FilterByInstallation(val index: Int) : IndexUiIntent()
    object Emphasize : IndexUiIntent()
    object BatchingSelectAll : IndexUiIntent()
    data class BatchingSelect(val packageName: String) : IndexUiIntent()
    data class BatchAndOp(val mask: Int, val packageNames: List<String>) : IndexUiIntent()
    data class BatchOrOp(val mask: Int, val packageNames: List<String>) : IndexUiIntent()
    data class SelectTimestamp(val index: Int) : IndexUiIntent()
    data class UpdatePackageState(val entity: PackageRestoreEntire) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val packageRestoreRepository: PackageRestoreRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, UiEffect>(IndexUiState()) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Initialize -> {
                packageRestoreRepository.loadLocalIcon()
                emitState(uiState.value.copy(activating = false))
                packageRestoreRepository.loadLocalConfig(topBarState = _topBarState)
                emitIntentSuspend(IndexUiIntent.Update)
            }

            is IndexUiIntent.Update -> {
                // Inactivate all packages then activate displayed ones.
                packageRestoreRepository.updateActive(active = false)
                packageRestoreRepository.updateActive(
                    active = true,
                    timestamp = timestampState.value,
                    savePath = packageRestoreRepository.restoreSavePath.first()
                )
            }

            is IndexUiIntent.UpdatePackage -> {
                packageRestoreRepository.updatePackage(intent.entity)
            }


            is IndexUiIntent.FilterByKey -> {
                _keyState.value = intent.key
            }

            is IndexUiIntent.Sort -> {
                var type = intent.type
                val index = intent.index
                if (_sortIndexState.first() == index) {
                    type = if (type == SortType.ASCENDING) SortType.DESCENDING else SortType.ASCENDING
                    packageRestoreRepository.saveRestoreSortType(type)
                }
                packageRestoreRepository.saveRestoreSortTypeIndex(index)
            }

            is IndexUiIntent.FilterByFlag -> {
                packageRestoreRepository.saveRestoreFilterFlagIndex(intent.index)
            }

            is IndexUiIntent.FilterByInstallation -> {
                packageRestoreRepository.saveRestoreInstallationTypeIndex(intent.index)
            }

            is IndexUiIntent.Emphasize -> {
                emitState(state.copy(emphasizedState = state.emphasizedState.not()))
            }

            is IndexUiIntent.BatchingSelectAll -> {
                var batchSelection: List<String> = listOf()
                if (state.batchSelection.isEmpty()) {
                    batchSelection = packagesState.first().filter { it.isExists }.map { it.packageName }
                }
                emitState(state.copy(batchSelection = batchSelection))
            }

            is IndexUiIntent.BatchingSelect -> {
                val batchSelection = state.batchSelection.toMutableList()
                if (intent.packageName in batchSelection)
                    batchSelection.remove(intent.packageName)
                else
                    batchSelection.add(intent.packageName)
                emitState(state.copy(batchSelection = batchSelection))
            }

            is IndexUiIntent.BatchAndOp -> {
                packageRestoreRepository.andOpCodeByMask(mask = intent.mask, packageNames = intent.packageNames)
            }

            is IndexUiIntent.BatchOrOp -> {
                packageRestoreRepository.orOpCodeByMask(mask = intent.mask, packageNames = intent.packageNames)
            }

            is IndexUiIntent.SelectTimestamp -> {
                _timestampState.value = _timestampListState.first().getOrNull(intent.index) ?: 0
                emitIntentSuspend(IndexUiIntent.Update)
            }

            is IndexUiIntent.UpdatePackageState -> {
                packageRestoreRepository.updatePackageState(entity = intent.entity)
            }
        }
    }

    private var _timestampState: MutableStateFlow<Long> = MutableStateFlow(0)
    private val timestampState: StateFlow<Long> = _timestampState.asStateFlow()
    private var _timestampListState: Flow<List<Long>> =
        packageRestoreRepository.observeTimestamps().onEach {
            if (timestampState.value == 0L) {
                _timestampState.value = it.lastOrNull() ?: 0
                emitIntentSuspend(IndexUiIntent.Update)
            }
        }.flowOnIO()
    val timestampIndexState: StateFlow<Int> =
        combine(_timestampState, _timestampListState) { timestamp, timestampList ->
            timestampList.indexOf(timestamp)
        }.flowOnIO().stateInScope(-1)
    val timestampListState: StateFlow<List<String>> =
        _timestampListState.map { it.map { timestamp -> DateUtil.formatTimestamp(timestamp) } }.flowOnIO().stateInScope(listOf())

    @OptIn(ExperimentalCoroutinesApi::class)
    private var _packages: Flow<List<PackageRestoreEntire>> = _timestampState.flatMapLatest {
        packageRestoreRepository.observePackages(it)
    }.flowOnIO()
    private val _keyState: MutableStateFlow<String> = MutableStateFlow("")
    private var _flagIndexState: Flow<Int> = packageRestoreRepository.restoreFilterFlagIndex.flowOnIO()
    private var _installationTypeIndex: Flow<Int> = packageRestoreRepository.restoreInstallationTypeIndex.flowOnIO()
    private var _sortIndexState: Flow<Int> = packageRestoreRepository.restoreSortTypeIndex.flowOnIO()
    private var _sortTypeState: Flow<SortType> = packageRestoreRepository.restoreSortType.flowOnIO()
    private val _packagesState: Flow<List<PackageRestoreEntire>> =
        combine(_packages, _keyState, _flagIndexState, _sortIndexState, _sortTypeState) { packages, key, flagIndex, sortIndex, sortType ->
            packages
                .sortedWith(packageRestoreRepository.getSortComparator(sortIndex = sortIndex, sortType = sortType))
                .filter(packageRestoreRepository.getKeyPredicate(key = key))
                .filter(packageRestoreRepository.getFlagPredicate(index = flagIndex))
        }.combine(_installationTypeIndex) { packages, installationTypeIndex ->
            packages.filter(packageRestoreRepository.getInstallationPredicate(index = installationTypeIndex))
        }.flowOnIO()
    val packagesState: StateFlow<List<PackageRestoreEntire>> = _packagesState.stateInScope(listOf())
    val packagesSelectedState: StateFlow<List<PackageRestoreEntire>> = _packagesState.map { packages ->
        packages.filter { it.operationCode != OperationMask.None }
    }.flowOnIO().stateInScope(listOf())
    val packagesNotSelectedState: StateFlow<List<PackageRestoreEntire>> = _packagesState.map { packages ->
        packages.filter { it.operationCode == OperationMask.None }
    }.flowOnIO().stateInScope(listOf())

    private val _topBarState: MutableStateFlow<TopBarState> = MutableStateFlow(TopBarState(title = StringResourceToken.fromStringId(R.string.restore_list)))
    val topBarState: StateFlow<TopBarState> = _topBarState.asStateFlow()
    val shimmeringState: StateFlow<Boolean> = _packages.map { it.isEmpty() }.flowOnIO().stateInScope(initialValue = true)
    val selectedAPKsCountState: StateFlow<Int> = packageRestoreRepository.selectedAPKsCount.flowOnIO().stateInScope(0)
    val selectedDataCountState: StateFlow<Int> = packageRestoreRepository.selectedDataCount.flowOnIO().stateInScope(0)
}
