package com.steadfast.app.ui.journey

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steadfast.app.data.model.JournalEntryEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Configuration-only memory. Sensitive drafts and searches deliberately disappear with the process. */
class JourneyDraftModel : ViewModel() {
    var initialized = false
    var query by mutableStateOf("")
    var filter by mutableStateOf("ALL")
    var showRestart by mutableStateOf(false)
    var showNew by mutableStateOf(false)
    var editing by mutableStateOf<JournalEntryEntity?>(null)
    var deleting by mutableStateOf<JournalEntryEntity?>(null)
    var body by mutableStateOf("")
    var context by mutableStateOf<String?>("GENERAL")
    var saving by mutableStateOf(false)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set
    var restartMillis by mutableLongStateOf(System.currentTimeMillis())
    var restartNote by mutableStateOf("")
    var restartError by mutableStateOf<String?>(null)
    var restarting by mutableStateOf(false)
    val operationScope get() = viewModelScope

    fun openNew(context: String? = "GENERAL") { body = ""; this.context = context; saveError = null; showNew = true }
    fun openEdit(entry: JournalEntryEntity) { body = entry.body; context = entry.context; saveError = null; editing = entry }
    fun clearEditor() { body = ""; context = "GENERAL"; saveError = null; showNew = false; editing = null }
    fun clearRestart() { showRestart = false; restartNote = ""; restartError = null }

    fun saveJournalEntry(entryId: Long?, save: suspend (Long?, String, String?) -> Result<Unit>) {
        if (saving || body.isBlank()) return
        val submittedBody = body
        val submittedContext = context
        saving = true
        saveError = null
        viewModelScope.launch {
            try {
                val result = try {
                    save(entryId, submittedBody, submittedContext)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    Result.failure(IllegalStateException("Could not save this journal entry."))
                }
                if (result.isSuccess) clearEditor()
                else saveError = "Could not save this journal entry. Your draft is still here; please try again."
            } finally {
                saving = false
            }
        }
    }
}
