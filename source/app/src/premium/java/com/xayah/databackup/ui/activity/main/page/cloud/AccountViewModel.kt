package com.xayah.databackup.ui.activity.main.page.cloud

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.gson.reflect.TypeToken
import com.xayah.core.database.model.AccountMap
import com.xayah.core.database.model.CloudAccountEntity
import com.xayah.core.database.dao.CloudDao
import com.xayah.core.util.GsonUtil
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.command.CloudUtil
import com.xayah.core.rootservice.util.ExceptionUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class AccountUiState(
    val isLoading: Boolean,
    val mutex: Mutex,
    val logUtil: LogUtil,
    val gsonUtil: GsonUtil,
    val cloudDao: CloudDao,
) {
    val cloudEntities: Flow<List<CloudAccountEntity>> = cloudDao.queryAccountFlow().distinctUntilChanged()
}

@HiltViewModel
class AccountViewModel @Inject constructor(logUtil: LogUtil, gsonUtil: GsonUtil, cloudDao: CloudDao) : ViewModel() {
    private val _uiState = mutableStateOf(
        AccountUiState(
            isLoading = true,
            mutex = Mutex(),
            logUtil = logUtil,
            gsonUtil = gsonUtil,
            cloudDao = cloudDao,
        )
    )
    val uiState: State<AccountUiState>
        get() = _uiState

    private suspend fun inactivateEntities() = uiState.value.cloudDao.updateActive(false)

    suspend fun initialize() {
        val uiState by uiState
        if (uiState.mutex.isLocked.not()) {
            uiState.mutex.withLock {
                val (_, json) = CloudUtil.Config.dump()
                val type = object : TypeToken<AccountMap>() {}.type
                val map = ExceptionUtil.tryOn(block = { uiState.gsonUtil.fromJson(json, type) }, onException = { AccountMap() })

                // Inactivate all cloud entities.
                inactivateEntities()
                uiState.cloudDao.upsertAccount(map.map { (key, value) ->
                    CloudAccountEntity(
                        name = key,
                        account = value,
                        active = true,
                    )
                })

                _uiState.value = uiState.copy(isLoading = false)
            }
        }
    }

    suspend fun delete(entity: CloudAccountEntity) {
        val uiState by uiState
        CloudUtil.Config.delete(logUtil = uiState.logUtil, name = entity.name)
        uiState.cloudDao.deleteAccount(entity)
    }
}
