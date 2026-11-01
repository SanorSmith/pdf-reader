package com.sanorsmith.pdfreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    private var incoming by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incoming = intent?.data
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                App(incoming, onConsumed = { incoming = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { incoming = it }
    }
}

@Composable
private fun App(incoming: Uri?, onConsumed: () -> Unit) {
    val context = LocalContext.current
    val recents = remember { RecentFiles(context) }
    var current by remember { mutableStateOf<Uri?>(null) }
    var list by remember { mutableStateOf(recents.load()) }

    fun open(uri: Uri) {
        RecentFiles.keepAccess(context, uri)
        recents.add(uri, RecentFiles.displayName(context, uri), recents.lastPage(uri))
        list = recents.load()
        current = uri
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(::open) }

    LaunchedEffect(incoming) {
        incoming?.let { open(it); onConsumed() }
    }

    val uri = current
    if (uri == null) {
        HomeScreen(
            recents = list,
            onPick = { picker.launch(arrayOf("application/pdf")) },
            onOpen = { open(it.uri) },
            onRemove = { recents.remove(it.uri); list = recents.load() },
        )
    } else {
        BackHandler { current = null; list = recents.load() }
        ReaderScreen(
            uri = uri,
            title = RecentFiles.displayName(context, uri),
            initialPage = recents.lastPage(uri),
            onPageChanged = { recents.savePage(uri, it) },
            onBack = { current = null; list = recents.load() },
        )
    }
}
