package com.tai.oeviewer

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.os.ParcelFileDescriptor
import java.io.File
import java.nio.ByteBuffer

internal data class UploadPreview(val bitmap: Bitmap? = null, val width: Int = 0, val height: Int = 0, val duration: Double = 0.0) {
    companion object {
        fun read(file: File, extension: String): UploadPreview {
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            return try {
                when (extension.lowercase()) {
                    "svg" -> {
                        require(file.length() <= 8 * 1024 * 1024)
                        val bytes = file.readBytes()
                        val bitmap = SvgPreview.render(bytes, 600)
                        val svg = bytes.inputStream().use { com.caverock.androidsvg.SVG.getFromInputStream(it) }
                        val width = svg.documentWidth.takeIf { it > 0 } ?: svg.documentViewBox?.width() ?: bitmap.width.toFloat()
                        val height = svg.documentHeight.takeIf { it > 0 } ?: svg.documentViewBox?.height() ?: bitmap.height.toFloat()
                        UploadPreview(bitmap, width.toInt(), height.toInt())
                    }
                    "pdf" -> ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                        PdfRenderer(descriptor).use { renderer -> renderer.openPage(0).use { page ->
                            val size = LibraryLogic.svgRenderSize(page.width.toFloat(), page.height.toFloat(), 600)
                            val bitmap = Bitmap.createBitmap(size[0], size[1], Bitmap.Config.ARGB_8888)
                            try { page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
                            catch (error: Exception) { bitmap.recycle(); throw error }
                            UploadPreview(bitmap, page.width, page.height)
                        } }
                    }
                    "mp4", "mov", "mkv", "webm", "avi", "m4v", "3gp", "mp3", "m4a", "aac", "wav", "flac", "ogg", "opus" -> {
                        val media = MediaMetadataRetriever()
                        try {
                            media.setDataSource(file.path)
                            val width = media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                            val height = media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                            val duration = (media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toDoubleOrNull() ?: 0.0) / 1000
                            val bitmap = if (width > 0 && height > 0) {
                                val size = LibraryLogic.svgRenderSize(width.toFloat(), height.toFloat(), 600)
                                media.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, size[0], size[1])
                            } else media.embeddedPicture?.let { decode(ImageDecoder.createSource(ByteBuffer.wrap(it))).bitmap }
                            UploadPreview(bitmap, width, height, duration)
                        } finally { media.release() }
                    }
                    else -> decode(ImageDecoder.createSource(file))
                }
            } catch (error: Exception) {
                if (error is InterruptedException || Thread.currentThread().isInterrupted) throw InterruptedException()
                // A preview is optional; unsupported files are still uploaded intact.
                UploadPreview()
            }
        }

        private fun decode(source: ImageDecoder.Source): UploadPreview {
            var width = 0
            var height = 0
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                width = info.size.width; height = info.size.height
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetSampleSize(((maxOf(width, height) + 599) / 600).coerceAtLeast(1))
            }
            return UploadPreview(bitmap, width, height)
        }
    }
}
