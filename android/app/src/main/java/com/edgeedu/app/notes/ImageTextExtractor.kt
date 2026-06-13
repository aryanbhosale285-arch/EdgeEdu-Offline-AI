package com.edgeedu.app.notes

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * On-device OCR for imported photos (PRD §8.2 "Printed-page photos"). Uses
 * ML Kit Text Recognition with the bundled Latin model — fully offline, no
 * network. Reliable for printed text; handwriting and maths-image OCR remain
 * out of scope (§8.2/§20). Devanagari (Hindi/Marathi) photos would need the
 * separate Devanagari model — a future addition.
 */
object ImageTextExtractor {

    fun extract(context: Context, uri: Uri): String {
        // fromFilePath reads the image and applies EXIF rotation.
        val image = InputImage.fromFilePath(context.applicationContext, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val text = try {
            Tasks.await(recognizer.process(image)).text
        } catch (e: ImportException) {
            throw e
        } catch (e: Exception) {
            throw ImportException("Couldn't read text from the image.")
        } finally {
            recognizer.close()
        }
        if (text.isBlank()) {
            throw ImportException(
                "No readable text found in the photo. Use a clear photo of printed text."
            )
        }
        return text
    }
}
