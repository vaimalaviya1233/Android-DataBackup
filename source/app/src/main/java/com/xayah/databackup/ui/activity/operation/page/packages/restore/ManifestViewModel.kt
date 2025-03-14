package com.xayah.databackup.ui.activity.operation.page.packages.restore

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.xayah.core.database.dao.PackageRestoreEntireDao
import com.xayah.core.database.model.PackageRestoreEntire
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

data class ManifestUiState(
    val packageRestoreEntireDao: PackageRestoreEntireDao,
) {
    val bothPackages: Flow<List<PackageRestoreEntire>> = packageRestoreEntireDao.queryActiveBothPackages().distinctUntilChanged()
    val apkOnlyPackages: Flow<List<PackageRestoreEntire>> = packageRestoreEntireDao.queryActiveAPKOnlyPackages().distinctUntilChanged()
    val dataOnlyPackages: Flow<List<PackageRestoreEntire>> = packageRestoreEntireDao.queryActiveDataOnlyPackages().distinctUntilChanged()
    val selectedBoth: Flow<Int> = packageRestoreEntireDao.countSelectedBoth().distinctUntilChanged()
    val selectedAPKs: Flow<Int> = packageRestoreEntireDao.countSelectedAPKs().distinctUntilChanged()
    val selectedData: Flow<Int> = packageRestoreEntireDao.countSelectedData().distinctUntilChanged()
}

@HiltViewModel
class ManifestViewModel @Inject constructor(packageRestoreEntireDao: PackageRestoreEntireDao) : ViewModel() {
    private val _uiState = mutableStateOf(ManifestUiState(packageRestoreEntireDao = packageRestoreEntireDao))
    val uiState: State<ManifestUiState>
        get() = _uiState
}
