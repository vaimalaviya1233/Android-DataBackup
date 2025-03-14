package com.xayah.databackup.util

import android.os.Build
import com.xayah.databackup.BuildConfig
import com.xayah.core.database.model.CmdEntity
import com.xayah.core.database.model.LogCmdType
import com.xayah.core.database.dao.LogDao
import com.xayah.core.database.model.LogEntity
import com.xayah.core.util.DateUtil
import com.xayah.core.rootservice.util.withIOContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

fun Array<String>.formatToString() = joinToString(separator = ", ")

@Singleton
class LogUtil @Inject constructor(private val logDao: LogDao) {
    init {
        CoroutineScope(Dispatchers.IO).launch {
            log("Version", BuildConfig.VERSION_NAME)
            log("Model", Build.MODEL)
            log("ABIs", Build.SUPPORTED_ABIS.formatToString())
            log("SDK", Build.VERSION.SDK_INT.toString())
        }
    }

    private val startTimestamp: Long = DateUtil.getTimestamp()

    suspend fun log(tag: String, msg: String): Long = withIOContext {
        if (msg.trim().isEmpty()) return@withIOContext -1
        val logEntity = LogEntity(startTimestamp = startTimestamp, tag = tag, msg = msg)
        logDao.upsert(logEntity)
    }

    suspend fun logCmd(logId: Long, type: LogCmdType, msg: String): Long = withIOContext {
        val cmdEntity = CmdEntity(logId = logId, type = type, msg = msg)
        logDao.upsert(cmdEntity)
    }
}
