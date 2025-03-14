package com.xayah.databackup.ui.component

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.xayah.databackup.R
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.rootservice.util.ExceptionUtil
import com.xayah.core.rootservice.util.withIOContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Creates a [DialogState] and acts as a slot with [DialogState.Insert].
 */
@Composable
fun rememberDialogState(): DialogState {
    val state = remember { DialogState() }
    state.Insert()
    return state
}

class DialogState {
    private var content: (@Composable () -> Unit)? by mutableStateOf(null)

    @Composable
    internal fun Insert() = content?.invoke()

    private fun dismiss() {
        content = null
    }

    /**
     * Return **Pair<Boolean, T>**.
     *
     * If user clicks **confirmButton**, then return **Pair(true, T)**,
     * otherwise return **Pair(false, T)**.
     */
    suspend fun <T> open(
        initialState: T,
        title: String,
        icon: ImageVector? = null,
        confirmText: String? = null,
        dismissText: String? = null,
        onLoading: (suspend () -> Unit)? = null,
        block: @Composable (T) -> Unit,
    ): Pair<Boolean, T> {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { dismiss() }
            content = {
                val uiState by remember { mutableStateOf(initialState) }
                AlertDialog(
                    onDismissRequest = {
                        dismiss()
                        continuation.resume(Pair(false, uiState))
                    },
                    confirmButton = {
                        TextButton(text = confirmText ?: stringResource(id = R.string.confirm), onClick = {
                            dismiss()
                            continuation.resume(Pair(true, uiState))
                        })
                    },
                    dismissButton = {
                        TextButton(text = dismissText ?: stringResource(id = R.string.cancel), onClick = {
                            dismiss()
                            continuation.resume(Pair(false, uiState))
                        })
                    },
                    title = { Text(text = title) },
                    icon = icon?.let { { Icon(imageVector = icon, contentDescription = null) } },
                    text = {
                        if (onLoading == null) block(uiState)
                        else Loader(modifier = Modifier.fillMaxWidth(), onLoading = onLoading, uiState = uiState, content = block)
                    },
                )
            }
        }
    }

    /**
     * Loading dialog
     */
    suspend fun openLoading(title: String, icon: ImageVector, onLoading: suspend () -> Unit) {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { dismiss() }
            content = {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {},
                    dismissButton = null,
                    title = { Text(text = title) },
                    icon = { Icon(imageVector = icon, contentDescription = null) },
                    text = {
                        Loader(
                            modifier = Modifier.fillMaxWidth(),
                            onLoading = {
                                onLoading()
                                dismiss()
                                continuation.resume(Unit)
                            },
                            content = {}
                        )
                    },
                    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
                )
            }
        }
    }
}

suspend fun DialogState.openFileOpDialog(context: Context, title: String, filePath: String, icon: ImageVector, text: String) {
    val remoteRootService = RemoteRootService(context)
    var msg: String? = null

    open(
        initialState = false,
        title = title,
        icon = icon,
        onLoading = {
            withIOContext {
                ExceptionUtil.tryService(onFailed = { msg = it }) {
                    remoteRootService.writeText(text, filePath, context)
                }
                remoteRootService.destroyService()
            }
        },
        block = { _ ->
            Text(
                text = if (msg == null) "${context.getString(R.string.succeed)}: $filePath"
                else "${context.getString(R.string.failed)}: $msg\n${context.getString(R.string.remote_service_err_info)}"
            )
        }
    )
}

suspend fun DialogState.openConfirmDialog(context: Context, text: String) = open(
    initialState = false,
    title = context.getString(R.string.prompt),
    icon = Icons.Outlined.Info,
    block = { _ -> Text(text = text) }
)
