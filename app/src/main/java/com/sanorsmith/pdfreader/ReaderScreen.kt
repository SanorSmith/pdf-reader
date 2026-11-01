package com.sanorsmith.pdfreader

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    uri: Uri,
    title: String,
    initialPage: Int,
    onPageChanged: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var doc by remember(uri) { mutableStateOf<PdfDocument?>(null) }
    var error by remember(uri) { mutableStateOf<String?>(null) }

    LaunchedEffect(uri) {
        try {
            doc = PdfDocument.open(context, uri)
        } catch (e: Exception) {
            error = "Could not open this PDF. It may be password-protected or damaged."
        }
    }
    DisposableEffect(uri) { onDispose { doc?.close() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Color(0xFF3A3A3A))) {
            val d = doc
            val err = error
            when {
                err != null -> Text(err, color = Color.White, modifier = Modifier.align(Alignment.Center).padding(24.dp))
                d == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> Pages(d, initialPage, onPageChanged)
            }
        }
    }
}

@Composable
private fun Pages(doc: PdfDocument, initialPage: Int, onPageChanged: (Int) -> Unit) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialPage.coerceIn(0, (doc.pageCount - 1).coerceAtLeast(0))
    )
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect(onPageChanged)
    }

    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        // Render at 2x so zooming in stays reasonably sharp.
        val renderWidth = (widthPx * 2).coerceAtMost(2400)

        fun clampOffset(o: Offset, s: Float): Offset {
            val maxX = widthPx * (s - 1f) / 2f
            val maxY = heightPx * (s - 1f) / 2f
            return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
        }

        LazyColumn(
            state = listState,
            userScrollEnabled = scale <= 1.01f,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        if (scale > 1.01f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    })
                }
                .pointerInput(Unit) {
                    // Pinch to zoom; pan only while zoomed so normal scrolling keeps working.
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val multi = event.changes.count { it.pressed } > 1
                            if (multi || scale > 1.01f) {
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = newScale
                                offset = if (newScale <= 1.01f) Offset.Zero else clampOffset(offset + pan, newScale)
                                event.changes.forEach { if (it.pressed) it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        ) {
            items(List(doc.pageCount) { it }, key = { it }) { index ->
                PageView(doc, index, renderWidth)
            }
        }

        Surface(
            color = Color(0xCC000000),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
        ) {
            Text(
                "${listState.firstVisibleItemIndex + 1} / ${doc.pageCount}",
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun PageView(doc: PdfDocument, index: Int, renderWidth: Int) {
    var ratio by remember(index) { mutableFloatStateOf(1.414f) }
    var image by remember(index, renderWidth) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(index, renderWidth) {
        ratio = doc.aspectRatio(index)
        image = doc.render(index, renderWidth).asImageBitmap()
    }

    Box(
        Modifier.fillMaxWidth().aspectRatio(1f / ratio).background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        val img = image
        if (img != null) {
            Image(
                img,
                contentDescription = "Page ${index + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CircularProgressIndicator()
        }
    }
}
