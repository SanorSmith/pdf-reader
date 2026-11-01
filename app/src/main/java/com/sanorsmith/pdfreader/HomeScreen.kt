package com.sanorsmith.pdfreader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    recents: List<RecentFile>,
    onPick: () -> Unit,
    onOpen: (RecentFile) -> Unit,
    onRemove: (RecentFile) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("PDF Reader") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onPick) { Text("Open PDF") }
        },
    ) { padding ->
        if (recents.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("No documents yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap \"Open PDF\" to pick a file from your phone.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(Modifier.padding(padding)) {
                item {
                    Text(
                        "Recent",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(recents, key = { it.uri.toString() }) { file ->
                    ListItem(
                        modifier = Modifier.clickable { onOpen(file) },
                        headlineContent = { Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text("Page ${file.lastPage + 1}") },
                        trailingContent = { TextButton(onClick = { onRemove(file) }) { Text("Remove") } },
                    )
                    HorizontalDivider()
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }
}
