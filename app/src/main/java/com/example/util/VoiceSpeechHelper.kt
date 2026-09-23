package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

object VoiceSpeechHelper {

    data class ExtractedTransaction(
        val amount: Double,
        val type: String, // "LANA" (لنا) or "LAHO" (له)
        val description: String,
        val rawText: String
    )

    /**
     * Parses Arabic speech or text to extract:
     * - Amount (e.g. 500, خمسين, ألف, 1200.50)
     * - Transaction type (LANA / LAHO)
     * - Description
     */
    fun parseArabicTransaction(text: String): ExtractedTransaction? {
        if (text.isBlank()) return null
        val cleanText = text.trim()

        // 1. Extract numeric digits or written Arabic words
        val digitMatch = Regex("""(\d+(\.\d+)?)""").find(cleanText)
        var amount: Double? = digitMatch?.value?.toDoubleOrNull()

        if (amount == null) {
            // Check common Arabic number words
            amount = when {
                cleanText.contains("مائة ألف") || cleanText.contains("مية الف") -> 100000.0
                cleanText.contains("خمسين ألف") || cleanText.contains("خمسين الف") -> 50000.0
                cleanText.contains("عشرة آلاف") || cleanText.contains("عشرة الاف") || cleanText.contains("عشر الاف") -> 10000.0
                cleanText.contains("خمسة آلاف") || cleanText.contains("خمسة الاف") || cleanText.contains("خمس الاف") -> 5000.0
                cleanText.contains("ألفين") || cleanText.contains("الفين") -> 2000.0
                cleanText.contains("ألف") || cleanText.contains("الف") -> 1000.0
                cleanText.contains("تسعمائة") || cleanText.contains("تسعمية") -> 900.0
                cleanText.contains("ثمانمائة") || cleanText.contains("تمنمية") -> 800.0
                cleanText.contains("سبعمائة") || cleanText.contains("سبعمية") -> 700.0
                cleanText.contains("ستمائة") || cleanText.contains("ستمية") -> 600.0
                cleanText.contains("خمسمائة") || cleanText.contains("خمسمية") -> 500.0
                cleanText.contains("أربعمائة") || cleanText.contains("اربعمية") -> 400.0
                cleanText.contains("ثلاثمائة") || cleanText.contains("تلاتمية") -> 300.0
                cleanText.contains("مائتين") || cleanText.contains("ميتين") -> 200.0
                cleanText.contains("مائة") || cleanText.contains("مية") -> 100.0
                cleanText.contains("تسعين") -> 90.0
                cleanText.contains("ثمانين") || cleanText.contains("تمانين") -> 80.0
                cleanText.contains("سبعين") -> 70.0
                cleanText.contains("ستين") -> 60.0
                cleanText.contains("خمسين") -> 50.0
                cleanText.contains("أربعين") || cleanText.contains("اربعين") -> 40.0
                cleanText.contains("ثلاثين") || cleanText.contains("تلاتين") -> 30.0
                cleanText.contains("عشرين") -> 20.0
                cleanText.contains("عشرة") || cleanText.contains("عشر") -> 10.0
                cleanText.contains("خمسة") || cleanText.contains("خمس") -> 5.0
                else -> null
            }
        }

        if (amount == null || amount <= 0.0) {
            return null
        }

        // 2. Determine type (لنا / له)
        // Keywords for LAHO (سداد / دفعة / استلمنا / قبضنا / له / دفع)
        val isLaho = cleanText.contains("سداد") ||
                cleanText.contains("دفعة") ||
                cleanText.contains("استلمنا") ||
                cleanText.contains("قبضنا") ||
                cleanText.contains("دفع") ||
                cleanText.contains("سدد") ||
                cleanText.contains("له") ||
                cleanText.contains("واصل") ||
                cleanText.contains("قبض")

        val type = if (isLaho) "LAHO" else "LANA"

        // 3. Extract description
        var desc = cleanText
        // Remove amount and type keywords to leave meaningful description
        val keywordsToRemove = listOf(
            "سجل", "معاملة", "لنا", "له", "دين", "دفعة", "سداد", "مبلغ", "قيمة", "دولار", "ليرة", "ريال", "جنيه",
            amount.toInt().toString(), amount.toString()
        )
        for (kw in keywordsToRemove) {
            desc = desc.replace(kw, "", ignoreCase = true)
        }
        desc = desc.replace(Regex("""\s+"""), " ").trim()
        if (desc.isBlank()) {
            desc = if (type == "LANA") "معاملة دين (لنا)" else "سداد دفعة (له)"
        }

        return ExtractedTransaction(
            amount = amount,
            type = type,
            description = desc,
            rawText = cleanText
        )
    }

    /**
     * Helper to create SpeechRecognizer intent configured for Arabic
     */
    fun createSpeechRecognizer(
        context: Context,
        onReady: () -> Unit,
        onResults: (String) -> Unit,
        onError: (String) -> Unit
    ): SpeechRecognizer? {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return null
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ar")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { onReady() }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                onError("تعذّر إكمال التسجيل.")
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull() ?: ""
                if (spokenText.isNotBlank()) {
                    onResults(spokenText)
                } else {
                    onError("لم يُستخرج أي نص. حاول التحدّث بوضوح.")
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { if (it.isNotBlank()) onResults(it) }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            recognizer.startListening(intent)
        } catch (e: Exception) {
            onError("تعذّر بدء المايكروفون: ${e.message}")
        }

        return recognizer
    }
}
