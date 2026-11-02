package com.steadfast.app.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steadfast.app.core.security.BackupManager
import com.steadfast.app.core.security.BackupPayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class SettingsBackupStage {
    IDLE, EXPORT_PICKER, EXPORT_FORM, EXPORTING,
    IMPORT_PICKER, IMPORT_FORM, INSPECTING, PREVIEW, RESTORING, RESULT
}

internal data class SettingsBackupState(
    val stage: SettingsBackupStage = SettingsBackupStage.IDLE,
    val uri: Uri? = null,
    val passphrase: String = "",
    val confirmation: String = "",
    val payload: BackupPayload? = null,
    val error: String? = null,
    val resultMessage: String? = null
)

/** Holds private backup UI state and its operation above disposable Settings composition. */
class SettingsWorkflowModel : ViewModel() {
    private val mutableBackupState = MutableStateFlow(SettingsBackupState())
    internal val backupState: StateFlow<SettingsBackupState> = mutableBackupState.asStateFlow()

    fun beginExportPicker(): Boolean = beginPicker(SettingsBackupStage.EXPORT_PICKER)
    fun beginImportPicker(): Boolean = beginPicker(SettingsBackupStage.IMPORT_PICKER)

    private fun beginPicker(stage: SettingsBackupStage): Boolean {
        if (mutableBackupState.value.stage != SettingsBackupStage.IDLE) return false
        mutableBackupState.value = SettingsBackupState(stage = stage)
        return true
    }

    fun onExportPickerResult(uri: Uri?) {
        if (mutableBackupState.value.stage != SettingsBackupStage.EXPORT_PICKER) return
        mutableBackupState.value = if (uri == null) SettingsBackupState() else SettingsBackupState(
            stage = SettingsBackupStage.EXPORT_FORM,
            uri = uri
        )
    }

    fun onImportPickerResult(uri: Uri?) {
        if (mutableBackupState.value.stage != SettingsBackupStage.IMPORT_PICKER) return
        mutableBackupState.value = if (uri == null) SettingsBackupState() else SettingsBackupState(
            stage = SettingsBackupStage.IMPORT_FORM,
            uri = uri
        )
    }

    fun updateExportPassphrase(value: String) = updateDraft { it.copy(passphrase = value, error = null) }
    fun updateExportConfirmation(value: String) = updateDraft { it.copy(confirmation = value, error = null) }
    fun updateImportPassphrase(value: String) = updateDraft { it.copy(passphrase = value, error = null) }

    private fun updateDraft(update: (SettingsBackupState) -> SettingsBackupState) {
        val current = mutableBackupState.value
        if (current.stage == SettingsBackupStage.EXPORT_FORM || current.stage == SettingsBackupStage.IMPORT_FORM) {
            mutableBackupState.value = update(current)
        }
    }

    fun cancelBackupWorkflow() {
        if (mutableBackupState.value.stage !in setOf(SettingsBackupStage.EXPORTING, SettingsBackupStage.INSPECTING, SettingsBackupStage.RESTORING)) {
            mutableBackupState.value = SettingsBackupState()
        }
    }

    fun dismissResult() {
        if (mutableBackupState.value.stage == SettingsBackupStage.RESULT) mutableBackupState.value = SettingsBackupState()
    }

    @Synchronized
    internal fun takeResultMessage(): String? {
        val current = mutableBackupState.value
        if (current.stage != SettingsBackupStage.RESULT) return null
        mutableBackupState.value = SettingsBackupState()
        return current.resultMessage
    }

    fun exportBackup(manager: BackupManager, resolver: ContentResolver) {
        val current = mutableBackupState.value
        if (current.stage != SettingsBackupStage.EXPORT_FORM || current.uri == null ||
            current.passphrase.length < BackupManager.MIN_PASSPHRASE_LENGTH || current.passphrase != current.confirmation
        ) return
        val uri = current.uri
        val passphrase = current.passphrase
        mutableBackupState.value = current.copy(stage = SettingsBackupStage.EXPORTING, error = null)
        viewModelScope.launch {
            val result = runCatchingCancellable {
                withContext(Dispatchers.IO) { manager.exportBackup(resolver.openOutputStream(uri), passphrase) }
            }
            mutableBackupState.value = SettingsBackupState(
                stage = SettingsBackupStage.RESULT,
                resultMessage = if (result.isSuccess) "Backup exported successfully." else "Could not create the backup. Check available storage and try again."
            )
        }
    }

    fun inspectBackup(manager: BackupManager, resolver: ContentResolver) {
        val current = mutableBackupState.value
        if (current.stage != SettingsBackupStage.IMPORT_FORM || current.uri == null) return
        val uri = current.uri
        val passphrase = current.passphrase // Empty legacy passphrases remain inspectable.
        mutableBackupState.value = current.copy(stage = SettingsBackupStage.INSPECTING, error = null)
        viewModelScope.launch {
            val result = runCatchingCancellable {
                withContext(Dispatchers.IO) {
                    manager.previewBackup(resolver.openInputStream(uri), passphrase).getOrThrow()
                }
            }
            mutableBackupState.value = if (result.isSuccess) {
                SettingsBackupState(stage = SettingsBackupStage.PREVIEW, payload = result.getOrNull())
            } else {
                mutableBackupState.value.copy(stage = SettingsBackupStage.IMPORT_FORM, error = "Could not unlock the backup. Check the passphrase and file.")
            }
        }
    }

    fun restoreBackup(manager: BackupManager) {
        val current = mutableBackupState.value
        val payload = current.payload ?: return
        if (current.stage != SettingsBackupStage.PREVIEW) return
        mutableBackupState.value = current.copy(stage = SettingsBackupStage.RESTORING, error = null)
        viewModelScope.launch {
            val result = runCatchingCancellable { manager.restorePayloadWithRollback(payload).getOrThrow() }
            mutableBackupState.value = SettingsBackupState(
                stage = if (result.isSuccess) SettingsBackupStage.RESULT else SettingsBackupStage.PREVIEW,
                payload = if (result.isSuccess) null else payload,
                error = if (result.isSuccess) null else "Restore failed. Your current data is still protected; try again or cancel.",
                resultMessage = if (result.isSuccess) "All data was restored successfully." else null
            )
        }
    }
}

private suspend fun <T> runCatchingCancellable(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}
