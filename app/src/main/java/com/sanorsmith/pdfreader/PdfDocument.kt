package com.sanorsmith.pdfreader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable

/** Wraps [PdfRenderer]; it only allows one open page at a time, so access is serialized. */
class PdfDocument private constructor(
    private val fd: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) : Closeable {

    val pageCount: Int = renderer.pageCount
    private val mutex = Mutex()
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 4).toInt().coerceAtMost(128 * 1024 * 1024)
    ) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /** Aspect ratio (height / width) of a page, used to reserve space before rendering. */
    suspend fun aspectRatio(index: Int): Float = mutex.withLock {
        withContext(Dispatchers.IO) {
            renderer.openPage(index).use { it.height.toFloat() / it.width }
        }
    }

    suspend fun render(index: Int, widthPx: Int): Bitmap {
        val key = "$index@$widthPx"
        cache.get(key)?.let { return it }
        return mutex.withLock {
            cache.get(key) ?: withContext(Dispatchers.IO) {
                renderer.openPage(index).use { page ->
                    val h = (widthPx * page.height.toFloat() / page.width).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    cache.put(key, bmp)
                    bmp
                }
            }
        }
    }

    override fun close() {
        runCatching { renderer.close() }
        runCatching { fd.close() }
        cache.evictAll()
    }

    companion object {
        suspend fun open(context: Context, uri: Uri): PdfDocument = withContext(Dispatchers.IO) {
            val fd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: error("Cannot open file")
            try {
                PdfDocument(fd, PdfRenderer(fd))
            } catch (e: Exception) {
                fd.close()
                throw e
            }
        }
    }
}
