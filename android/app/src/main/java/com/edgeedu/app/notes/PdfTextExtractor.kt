package com.edgeedu.app.notes

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

/**
 * Offline text extraction from a text-based PDF (PRD §8.2 "Text-based PDF").
 * Runs fully on-device via PdfBox-Android — no network, no OCR. Scanned /
 * image-only PDFs yield no selectable text and are rejected (OCR is out of
 * scope, §8.2/§20).
 */
object PdfTextExtractor {
    @Volatile private var initialized = false

    fun extract(context: Context, bytes: ByteArray): String {
        if (!initialized) {
            // Loads PdfBox's bundled font/AFM resources from app assets.
            PDFBoxResourceLoader.init(context.applicationContext)
            initialized = true
        }
        PDDocument.load(bytes).use { doc ->
            if (doc.isEncrypted) {
                throw ImportException("This PDF is password-protected — can't read it.")
            }
            val text = PDFTextStripper().getText(doc)
            if (text.isBlank()) {
                throw ImportException(
                    "No selectable text found. Scanned/image PDFs need OCR, which isn't supported."
                )
            }
            return text
        }
    }
}
