package com.example.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Centralized error handler for all Firebase Firestore operations in User and Admin apps.
 * Captures RESOURCE_EXHAUSTED exceptions and displays explicit Arabic warning messages.
 */
object FirestoreErrorHandler {

    private const val TAG = "FirestoreErrorHandler"
    const val QUOTA_EXHAUSTED_MESSAGE = "عفواً، تم استهلاك الحد الأقصى لقاعدة البيانات. يرجى التواصل مع الإدارة لتفعيل الباقة."

    /**
     * Checks if the throwable is a Firestore RESOURCE_EXHAUSTED or Quota error.
     */
    fun isQuotaExhausted(e: Throwable?): Boolean {
        if (e == null) return false
        if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
            return true
        }
        var current: Throwable? = e
        while (current != null) {
            if (current is FirebaseFirestoreException && current.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                return true
            }
            val msg = current.message ?: ""
            if (msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                msg.contains("Quota exceeded", ignoreCase = true) ||
                (msg.contains("quota", ignoreCase = true) && msg.contains("exceed", ignoreCase = true))
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    /**
     * Formats error into user-facing Arabic message.
     */
    fun getErrorMessage(e: Throwable, actionName: String = ""): String {
        return if (isQuotaExhausted(e)) {
            QUOTA_EXHAUSTED_MESSAGE
        } else {
            val detail = e.localizedMessage ?: e.message ?: "حدث خطأ غير متوقع"
            if (actionName.isNotBlank()) "فشل $actionName: $detail" else "خطأ في قاعدة البيانات: $detail"
        }
    }

    /**
     * Logs the error, returns the message, and displays a Long Toast on the UI thread.
     */
    fun handleAndToast(context: Context, e: Throwable, actionName: String = ""): String {
        val message = getErrorMessage(e, actionName)
        Log.e(TAG, "Firestore error [action=$actionName]: $message", e)
        try {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to display toast: ${ex.message}")
        }
        return message
    }
}
