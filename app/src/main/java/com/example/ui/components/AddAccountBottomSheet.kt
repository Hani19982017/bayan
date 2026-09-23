package com.example.ui.components

import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.TextStyle
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LahoGreen
import com.example.data.model.AccountEntity
import com.example.ui.theme.LanaRed
import com.example.ui.theme.ReportTealBg
import com.example.ui.theme.TawthiqPrimary

data class CurrencyOption(val code: String, val name: String, val symbol: String)

val AvailableCurrencies = listOf(
    // العملات الأكثر تداولاً
    CurrencyOption("USD", "دولار أمريكي", "$"),
    CurrencyOption("SAR", "ريال سعودي", "ر.س"),
    CurrencyOption("EGP", "جنيه مصري", "ج.م"),
    CurrencyOption("AED", "درهم إماراتي", "د.إ"),
    CurrencyOption("KWD", "دينار كويتي", "د.ك"),
    CurrencyOption("EUR", "يورو", "€"),
    // كافة العملات العربية
    CurrencyOption("YER", "ريال يمني", "ر.ي"),
    CurrencyOption("QAR", "ريال قطري", "ر.ق"),
    CurrencyOption("OMR", "ريال عماني", "ر.ع"),
    CurrencyOption("BHD", "دينار بحريني", "د.ب"),
    CurrencyOption("JOD", "دينار أردني", "د.أ"),
    CurrencyOption("IQD", "دينار عراقي", "د.ع"),
    CurrencyOption("LYD", "دينار ليبي", "د.ل"),
    CurrencyOption("TND", "دينار تونسي", "د.ت"),
    CurrencyOption("DZD", "دينار جزائري", "د.ج"),
    CurrencyOption("MAD", "درهم مغربي", "د.م"),
    CurrencyOption("SDG", "جنيه سوداني", "ج.س"),
    CurrencyOption("SYP", "ليرة سورية", "ل.س"),
    CurrencyOption("LBP", "ليرة لبنانية", "ل.ل"),
    CurrencyOption("MRU", "أوقية موريتانية", "أ.م"),
    CurrencyOption("DJF", "فرنك جيبوتي", "ف.ج"),
    CurrencyOption("SOS", "شلن صومالي", "ش.ص"),
    CurrencyOption("KMF", "فرنك قمري", "ف.ق"),
    // عملات إقليمية وعالمية بارزة
    CurrencyOption("TRY", "ليرة تركية", "₺"),
    CurrencyOption("GBP", "جنيه إسترليني", "£"),
    CurrencyOption("CAD", "دولار كندي", "C$"),
    CurrencyOption("AUD", "دولار أسترالي", "A$"),
    CurrencyOption("CHF", "فرنك سويسري", "CHF"),
    CurrencyOption("CNY", "يوان صيني", "¥"),
    CurrencyOption("JPY", "ين ياباني", "¥"),
    CurrencyOption("INR", "روبية هندية", "₹"),
    CurrencyOption("PKR", "روبية باكستانية", "₨"),
    CurrencyOption("MYR", "رينغيت ماليزي", "RM"),
    CurrencyOption("BRL", "ريال برازيلي", "R$"),
    CurrencyOption("RUB", "روبل روسي", "₽"),
    CurrencyOption("SEK", "كرونة سويدية", "kr"),
    CurrencyOption("NOK", "كرونة نرويجية", "kr"),
    CurrencyOption("DKK", "كرونة دنماركية", "kr"),
    CurrencyOption("NZD", "دولار نيوزيلندي", "NZ$"),
    CurrencyOption("SGD", "دولار سنغافوري", "S$")
)

/**
 * Modal Bottom Sheet exactly matching Screenshot 5 ("إضافة حساب جديد")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountBottomSheet(
    categories: List<String>,
    defaultCurrency: String = "USD",
    accountToEdit: AccountEntity? = null,
    existingAccounts: List<AccountEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, category: String, currency: String, initialBalance: Double, initialType: String, notes: String, avatarUri: String, creditLimit: Double, displayIndex: Int, autoSendWhatsApp: Boolean) -> Unit,
    onUpdate: ((AccountEntity) -> Unit)? = null,
    onAddNewCategory: (String) -> Unit = {}
) {
    val isEditMode = accountToEdit != null
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(accountToEdit) { mutableStateOf(accountToEdit?.name ?: "") }
    val initialCurrencyCode = if (accountToEdit != null && accountToEdit.currency.isNotBlank()) {
        accountToEdit.currency
    } else {
        defaultCurrency.ifBlank { "USD" }
    }
    var selectedCurrency by remember(accountToEdit, defaultCurrency) {
        mutableStateOf(
            AvailableCurrencies.find { it.code.equals(initialCurrencyCode, ignoreCase = true) }
                ?: CurrencyOption(initialCurrencyCode, initialCurrencyCode, initialCurrencyCode)
        )
    }
    var showCurrencyDropdown by remember { mutableStateOf(false) }
    var currencySearchQuery by remember { mutableStateOf("") }
    var phone by remember(accountToEdit) { mutableStateOf(accountToEdit?.phone ?: "") }
    var autoSendWhatsApp by remember(accountToEdit) { mutableStateOf(accountToEdit?.autoSendWhatsApp ?: false) }
    var notes by remember(accountToEdit) { mutableStateOf(accountToEdit?.notes ?: "") }
    var selectedCategory by remember(accountToEdit) { mutableStateOf(accountToEdit?.category ?: categories.firstOrNull { it != "الكل" } ?: "الحسابات") }
    var initialBalance by remember { mutableStateOf("") }
    var initialType by remember { mutableStateOf("LANA") }
    var creditLimitText by remember(accountToEdit) {
        mutableStateOf(if (accountToEdit != null && accountToEdit.creditLimit > 0) accountToEdit.creditLimit.toString() else "")
    }
    var displayIndexText by remember(accountToEdit) {
        mutableStateOf(if (accountToEdit != null && accountToEdit.displayIndex > 0) accountToEdit.displayIndex.toString() else "")
    }
    var hasError by remember { mutableStateOf(false) }

    val duplicatePhoneAccount = remember(phone, existingAccounts, accountToEdit) {
        val trimmed = phone.trim()
        if (trimmed.isNotBlank() && trimmed.length >= 7) {
            existingAccounts.firstOrNull {
                it.id != accountToEdit?.id && it.phone.trim() == trimmed
            }
        } else null
    }

    val duplicateIndexAccount = remember(displayIndexText, existingAccounts, accountToEdit) {
        val idx = displayIndexText.toIntOrNull()
        if (idx != null && idx > 0) {
            existingAccounts.firstOrNull {
                it.id != accountToEdit?.id && it.displayIndex == idx
            }
        } else null
    }

    var selectedAvatarUri by remember(accountToEdit) {
        mutableStateOf<Uri?>(if (accountToEdit != null && accountToEdit.avatarUri.isNotBlank()) Uri.parse(accountToEdit.avatarUri) else null)
    }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAvatarUri = uri
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        if (contactUri != null) {
            try {
                context.contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            val cName = cursor.getString(nameIndex)
                            if (name.isBlank() && !cName.isNullOrBlank()) {
                                name = cName
                            }
                        }
                        val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                        val hasPhone = if (hasPhoneIndex >= 0) cursor.getInt(hasPhoneIndex) else 0
                        val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                        if (hasPhone > 0 && idIndex >= 0) {
                            val contactId = cursor.getString(idIndex)
                            context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                arrayOf(contactId),
                                null
                            )?.use { pCursor ->
                                if (pCursor.moveToFirst()) {
                                    val numberIdx = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (numberIdx >= 0) {
                                        phone = pCursor.getString(numberIdx)?.replace(" ", "") ?: ""
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "تعذر قراءة جهة الاتصال", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val categoryList = remember(categories) {
        val base = mutableListOf<String>()
        categories.filter { it != "الكل" }.forEach { base.add(it) }
        if (base.isEmpty()) base.add("الحسابات")
        base.distinct()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1E293B) else Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()
        val inputBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
        val inputBorder = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
        val inputTextColor = if (isDark) Color.White else Color(0xFF0F172A)
        val inputPlaceholderColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Header Title
            Text(
                text = if (isEditMode) "تعديل إعدادات الحساب" else "إضافة حساب جديد",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isEditMode) 4.dp else 16.dp),
                textAlign = TextAlign.Center
            )
            if (isEditMode) {
                Text(
                    text = "تعديل رقم الهاتف، سقف الدين والمبلغ المستحق، والتفاصيل",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Circular Avatar Placeholder matching Screenshot 5
            Box(
                modifier = Modifier
                    .size(105.dp)
                    .clip(CircleShape)
                    .background(ReportTealBg)
                    .border(2.dp, TawthiqPrimary, CircleShape)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("add_photo_avatar"),
                contentAlignment = Alignment.Center
            ) {
                if (selectedAvatarUri != null) {
                    AsyncImage(
                        model = selectedAvatarUri,
                        contentDescription = "صورة الحساب",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "تغيير",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "إضافة صورة",
                            tint = TawthiqPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "إضافة صورة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TawthiqPrimary
                        )
                    }
                }
            }

            if (selectedAvatarUri != null) {
                TextButton(
                    onClick = { selectedAvatarUri = null },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إزالة الصورة",
                        tint = LanaRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "إزالة الصورة", color = LanaRed, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Account Name Field (with red asterisk)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "*", color = LanaRed, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "اسم الحساب",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(inputBg)
                        .border(
                            1.dp,
                            if (hasError) LanaRed else inputBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotBlank()) hasError = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_account_name_input"),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = inputTextColor,
                            textAlign = TextAlign.Start
                        ),
                        decorationBox = { innerTextField ->
                            if (name.isEmpty()) {
                                Text(
                                    text = "أدخل اسم الحساب",
                                    fontSize = 14.sp,
                                    color = inputPlaceholderColor
                                )
                            }
                            innerTextField()
                        }
                    )
                }
                if (hasError) {
                    Text(
                        text = "يرجى كتابة اسم الحساب أولاً",
                        color = LanaRed,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Currency Selector Field (with red asterisk)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "*", color = LanaRed, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "العملة",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(inputBg)
                        .border(1.dp, inputBorder, RoundedCornerShape(12.dp))
                        .clickable { showCurrencyDropdown = true }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Right side in RTL (currency badge + name)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ReportTealBg)
                                    .border(1.dp, TawthiqPrimary, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = TawthiqPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "(${selectedCurrency.symbol} - ${selectedCurrency.code}) ${selectedCurrency.name}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "عملة موحدة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TawthiqPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "تغيير العملة",
                                tint = TawthiqPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showCurrencyDropdown,
                        onDismissRequest = { showCurrencyDropdown = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        AvailableCurrencies.take(16).forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "(${option.symbol} - ${option.code}) ${option.name}",
                                            fontWeight = if (selectedCurrency.code == option.code) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedCurrency.code == option.code) TawthiqPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (option.code.equals(defaultCurrency, ignoreCase = true)) {
                                            Text(
                                                text = "الرئيسية",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TawthiqPrimary
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedCurrency = option
                                    showCurrencyDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. WhatsApp Number Field (Optional)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "رقم الوتساب (اختياري)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(inputBg)
                        .border(1.dp, inputBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left contact icon in RTL
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = "جهات الاتصال",
                            tint = TawthiqPrimary,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    try {
                                        contactPickerLauncher.launch(null)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "سجل الهاتف غير متوفر", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        BasicTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("whatsapp_number_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = inputTextColor,
                                textAlign = TextAlign.Start
                            ),
                            decorationBox = { innerTextField ->
                                if (phone.isEmpty()) {
                                    Text(
                                        text = "لإرسال التذكيرات",
                                        fontSize = 14.sp,
                                        color = inputPlaceholderColor
                                    )
                                }
                                innerTextField()
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Green Phone/WhatsApp Icon
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "واتساب",
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (duplicatePhoneAccount != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "⚠️ تنبيه: هذا الرقم مسجل مسبقاً لحساب: ${duplicatePhoneAccount.name}",
                                fontSize = 11.5.sp,
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // WhatsApp Auto-Send Toggle
                val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                val isToggleActive = autoSendWhatsApp && phone.isNotBlank()
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = phone.isNotBlank()) {
                            autoSendWhatsApp = !autoSendWhatsApp
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isToggleActive) {
                        Color(0xFF25D366).copy(alpha = if (isDark) 0.15f else 0.08f)
                    } else {
                        if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isToggleActive) Color(0xFF25D366) else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isToggleActive) Color(0xFF25D366) else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = if (isToggleActive) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "إرسال الفواتير تلقائياً عبر واتساب",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isToggleActive) Color(0xFF25D366).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = if (isToggleActive) "مفعل" else "متوقف",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isToggleActive) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (phone.isBlank()) "يرجى كتابة رقم الهاتف أولاً لتفعيل الإرسال التلقائي" else "فتح واتساب تلقائياً لإرسال الفاتورة بعد كل عملية مالية",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.material3.Switch(
                            checked = isToggleActive,
                            onCheckedChange = { isChecked ->
                                if (phone.isNotBlank()) {
                                    autoSendWhatsApp = isChecked
                                } else {
                                    Toast.makeText(context, "أدخل رقم الهاتف أولاً لتفعيل الإرسال التلقائي عبر واتساب", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = phone.isNotBlank(),
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF25D366)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Description Notes Field (Optional)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "وصف الحساب (اختياري)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(inputBg)
                        .border(1.dp, inputBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("account_notes_input"),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = inputTextColor,
                                textAlign = TextAlign.Start
                            ),
                            decorationBox = { innerTextField ->
                                if (notes.isEmpty()) {
                                    Text(
                                        text = "شروط، تنبيهات، تفاصيل...",
                                        fontSize = 14.sp,
                                        color = inputPlaceholderColor
                                    )
                                }
                                innerTextField()
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = TawthiqPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. Category Chips Row matching Screenshot 5
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "التصنيف",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Circular '+' Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ReportTealBg)
                            .clickable { showAddCategoryDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "إضافة تصنيف",
                            tint = TawthiqPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Categories chips
                    categoryList.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) ReportTealBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, TawthiqPrimary) else null,
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TawthiqPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6. Credit Limit & Fixed Account Number Fields (Requirement 1 & 8)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fixed Account Number / Index (Requirement 1: 1: احمد, 2: محمد)
                OutlinedTextField(
                    value = displayIndexText,
                    onValueChange = { displayIndexText = it.filter { char -> char.isDigit() } },
                    label = { Text("رقم الحساب (ترتيب ثابت 🔢)", fontSize = 12.sp) },
                    placeholder = { Text("تلقائي", fontSize = 12.sp) },
                    singleLine = true,
                    colors = tawthiqTextFieldColors(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                // Credit Limit / Maximum Allowed Debt (Requirement 8)
                OutlinedTextField(
                    value = creditLimitText,
                    onValueChange = { creditLimitText = it },
                    label = { Text("السقف الائتماني (الحد الأقصى 🚨)", fontSize = 12.sp) },
                    placeholder = { Text("0.0", fontSize = 12.sp) },
                    singleLine = true,
                    colors = tawthiqTextFieldColors(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (duplicateIndexAccount != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⚠️ تنبيه: الرقم #${displayIndexText} مستخدم لحساب (${duplicateIndexAccount.name})، سيتم تعيين رقم غير مكرر تلقائياً.",
                            fontSize = 11.5.sp,
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save Button matching Screenshot 5
            Button(
                onClick = {
                    if (name.isBlank()) {
                        hasError = true
                    } else {
                        if (isEditMode && onUpdate != null) {
                            val updated = accountToEdit!!.copy(
                                name = name.trim(),
                                phone = phone.trim(),
                                category = selectedCategory,
                                currency = selectedCurrency.code,
                                notes = notes.trim(),
                                avatarUri = selectedAvatarUri?.toString() ?: accountToEdit.avatarUri,
                                creditLimit = creditLimitText.toDoubleOrNull() ?: 0.0,
                                displayIndex = displayIndexText.toIntOrNull() ?: accountToEdit.displayIndex,
                                autoSendWhatsApp = autoSendWhatsApp && phone.isNotBlank(),
                                updatedAt = System.currentTimeMillis()
                            )
                            onUpdate(updated)
                        } else {
                            onSave(
                                name.trim(),
                                phone.trim(),
                                selectedCategory,
                                selectedCurrency.code,
                                initialBalance.toDoubleOrNull() ?: 0.0,
                                initialType,
                                notes.trim(),
                                selectedAvatarUri?.toString() ?: "",
                                creditLimitText.toDoubleOrNull() ?: 0.0,
                                displayIndexText.toIntOrNull() ?: 0,
                                autoSendWhatsApp && phone.isNotBlank()
                            )
                        }
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_account_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TawthiqPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isEditMode) "حفظ التعديلات" else "حفظ الحساب",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    }

    // Dialog to add custom category
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("إضافة تصنيف جديد", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newCategoryText,
                    onValueChange = { newCategoryText = it },
                    label = { Text("اسم التصنيف (مثال: موظف، مقاول)") },
                    singleLine = true,
                    colors = tawthiqTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryText.isNotBlank()) {
                            val trimmed = newCategoryText.trim()
                            onAddNewCategory(trimmed)
                            selectedCategory = trimmed
                            newCategoryText = ""
                        }
                        showAddCategoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary)
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
