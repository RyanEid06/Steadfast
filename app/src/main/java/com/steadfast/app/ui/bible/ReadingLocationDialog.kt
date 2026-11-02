package com.steadfast.app.ui.bible

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.steadfast.app.data.datastore.ReadingLocation
import com.steadfast.app.data.model.BibleBook

@Composable
fun ReadingLocationDialog(title: String, books: List<BibleBook>, current: ReadingLocation?,
    onSave: (Int, Int) -> Unit, onDismiss: () -> Unit, onClear: (() -> Unit)? = null) {
    var bookId by rememberSaveable { mutableStateOf(current?.bookId ?: books.firstOrNull()?.id) }
    var chapter by rememberSaveable { mutableIntStateOf(current?.chapter ?: 1) }
    var expanded by remember { mutableStateOf(false) }
    val book = books.find { it.id == bookId }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box {
                OutlinedButton(onClick = { expanded = true }, enabled = books.isNotEmpty()) { Text(book?.name ?: "Choose a book") }
                DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                    books.forEach { candidate ->
                        DropdownMenuItem(text = { Text(candidate.name) }, onClick = {
                            bookId = candidate.id; chapter = chapter.coerceIn(1, candidate.chapters); expanded = false
                        })
                    }
                }
            }
            book?.let { selected ->
                Text("Chapter $chapter of ${selected.chapters}")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { chapter-- }, enabled = chapter > 1, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Text("−") }
                    OutlinedButton(onClick = { chapter++ }, enabled = chapter < selected.chapters, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Text("+") }
                }
            }
            if (current != null && onClear != null) TextButton(onClick = onClear) { Text("Clear bookmark") }
        }
    }, confirmButton = {
        TextButton(onClick = { book?.let { onSave(it.id, chapter) } }, enabled = book != null && chapter in 1..book.chapters) { Text("Save bookmark") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
