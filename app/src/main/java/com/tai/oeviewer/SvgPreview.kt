package com.tai.oeviewer

import android.graphics.Bitmap
import android.graphics.Canvas
import com.caverock.androidsvg.SVG

internal object SvgPreview {
    init { SVG.setInternalEntitiesEnabled(false) }

    fun render(bytes: ByteArray, edge: Int): Bitmap {
        require(bytes.size <= 8 * 1024 * 1024) { "SVG 文件过大，无法安全预览" }
        // No external resolver: SVG cannot fetch remote images/fonts or execute scripts.
        val svg = bytes.inputStream().use { SVG.getFromInputStream(it) }
        val box = svg.documentViewBox
        val width = svg.documentWidth.takeIf { it > 0 } ?: box?.width() ?: 512f
        val height = svg.documentHeight.takeIf { it > 0 } ?: box?.height() ?: 512f
        val size = LibraryLogic.svgRenderSize(width, height, edge)
        svg.setDocumentWidth("100%")
        svg.setDocumentHeight("100%")
        return Bitmap.createBitmap(size[0], size[1], Bitmap.Config.ARGB_8888).also {
            svg.renderToCanvas(Canvas(it))
        }
    }
}
