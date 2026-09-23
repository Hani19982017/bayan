package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.util.VoiceSpeechHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTransactionBottomSheet(
    onDismiss: () -> Unit,
    currency: String = "USD",
    onTransactionExtracted: (amount: Double, type: String, description: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isListening by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>("لم يُستخرج أي نص. حاول التحدّث بوضوح.") }
    var statusText by remember { mutableStateOf("تعذّر إكمال التسجيل.") }
    var spokenText by remember { mutableStateOf("") }
    var extractedResult by remember { mutableStateOf<VoiceSpeechHelper.ExtractedTransaction?>(null) }
    var speechRecognizerInstance by remember { mutableStateOf<SpeechRecognizer?>(null) }

    // Pulse animation for mic while listening
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    fun startSpeech() {
        errorMessage = null
        statusText = "جاري الاستماع... تحدّث بالمعاملة الآن"
        isListening = true
        spokenText = ""
        extractedResult = null

        speechRecognizerInstance?.destroy()
        speechRecognizerInstance = VoiceSpeechHelper.createSpeechRecognizer(
            context = context,
            onReady = {
                statusText = "تحدّث الآن بوضوح (مثال: سجل لي 500 دولار دفعة نقدية)"
            },
            onResults = { text ->
                spokenText = text
                isListening = false
                val parsed = VoiceSpeechHelper.parseArabicTransaction(text)
                if (parsed != null) {
                    extractedResult = parsed
                    statusText = "تم استخراج المعاملة بنجاح ✓"
                    errorMessage = null
                } else {
                    statusText = "تعذّر استخراج المبلغ والبيان تلقائياً"
                    errorMessage = "تأكد من ذكر المبلغ بوضوح (مثال: سجل 250 لنا فاتورة مواد)"
                }
            },
            onError = { err ->
                isListening = false
                statusText = "تعذّر إكمال التسجيل."
                errorMessage = "لم يُستخرج أي نص. حاول التحدّث بوضوح."
            }
        )

        if (speechRecognizerInstance == null) {
            isListening = false
            statusText = "تعذّر تشغيل محرك الصوت."
            errorMessage = "محرك الصوت غير متوفر في جهازك، يمكنك كتابة الجملة أو اختيار عبارة تجريبية أدناه."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startSpeech()
        } else {
            errorMessage = "يلزم منح إذن الميكروفون للتسجيل الصوتي."
            statusText = "تعذّر إكمال التسجيل."
        }
    }

    fun handleMicClick() {
        if (isListening) {
            isListening = false
            speechRecognizerInstance?.stopListening()
            statusText = "تم إيقاف التسجيل."
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                startSpeech()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    fun testWithPhrase(phrase: String) {
        spokenText = phrase
        isListening = false
        val parsed = VoiceSpeechHelper.parseArabicTransaction(phrase)
        if (parsed != null) {
            extractedResult = parsed
            statusText = "تم استخراج المعاملة بنجاح ✓"
            errorMessage = null
        } else {
            statusText = "تعذّر استخراج البيانات."
            errorMessage = "لم يتم التعرف على المبلغ."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizerInstance?.destroy()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1E293B) else Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE2E8F0))
            )
        }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
            // Header: Title
            Text(
                text = "معاملة بالصوت",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle status
            Text(
                text = statusText,
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Red Warning Banner (Matching Image 2 exactly)
            if (errorMessage != null && extractedResult == null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF1F2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE4E6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFBE123C),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Center Microphone Button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .scale(if (isListening) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(if (isListening) Color(0xFFDCFCE7) else Color(0xFFF0FDF4))
                        .clickable { handleMicClick() }
                        .testTag("voice_record_mic_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "تسجيل صوتي",
                        tint = Color(0xFF00A86B),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isListening) "جاري الاستماع... اضغط للإيقاف" else "اضغط للتسجيل",
                    fontSize = 14.sp,
                    color = Color(0xFF334155),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick speech simulation chips
            Text(
                text = "أو اختر عبارة صوتية جاهزة للاختبار:",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.clickable { testWithPhrase("سجل لنا خمسين دولار فاتورة بضاعة") }
                ) {
                    Text(
                        text = "🗣️ سجل لنا 50$ فاتورة",
                        fontSize = 11.sp,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.clickable { testWithPhrase("استلمنا سداد دفعة نقدية 200 دولار") }
                ) {
                    Text(
                        text = "🗣️ سداد دفعة 200$",
                        fontSize = 11.sp,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.clickable { testWithPhrase("دين لنا 1000 ليرة كشف حساب") }
                ) {
                    Text(
                        text = "🗣️ دين لنا 1000",
                        fontSize = 11.sp,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Extracted Transaction Result Card
            extractedResult?.let { res ->
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (res.type == "LANA") Color(0xFFFEF2F2) else Color(0xFFF0FDF4)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (res.type == "LANA") Color(0xFFFCA5A5) else Color(0xFF86EFAC)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (res.type == "LANA") "معاملة دين (لنا)" else "سداد دفعة (له)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (res.type == "LANA") Color(0xFFDC2626) else Color(0xFF16A34A)
                            )
                            Text(
                                text = "${res.amount} $currency",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = if (res.type == "LANA") Color(0xFFDC2626) else Color(0xFF16A34A)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "البيان: ${res.description}",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                onTransactionExtracted(res.amount, res.type, res.description)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (res.type == "LANA") Color(0xFFDC2626) else Color(0xFF00A86B)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اعتماد وإدراج الفاتورة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Close Action (Matching Image 2: green 'إغلاق' on right in RTL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("voice_bottomsheet_close_btn")
                ) {
                    Text(
                        text = "إغلاق",
                        color = Color(0xFF00A86B),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
}
