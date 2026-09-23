package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppPermissions
import com.example.data.model.StaffUser
import com.example.ui.theme.LanaRed
import com.example.ui.theme.TawthiqPrimary
import com.example.ui.components.tawthiqTextFieldColors
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.ui.viewmodel.TawthiqViewModel
import com.example.util.QrGeneratorHelper
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    viewModel: TawthiqViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val staffUsers by viewModel.staffUsers.collectAsStateWithLifecycle()
    val merchantEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val storeName by viewModel.storeName.collectAsStateWithLifecycle()

    var showAddUserDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<StaffUser?>(null) }
    var userToDelete by remember { mutableStateOf<StaffUser?>(null) }
    var userForQrDialog by remember { mutableStateOf<StaffUser?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "إدارة المستخدمين والموظفين",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "رجوع",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddUserDialog = true },
                    containerColor = TawthiqPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag("add_user_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة مستخدم", modifier = Modifier.size(28.dp))
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (staffUsers.isEmpty()) {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "لا يوجد مستخدمون مضافون حالياً",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "قم بإنشاء حسابات للموظفين مع تحديد الصلاحيات المسموحة لكل منهم بدقة عبر رمز QR للوصول المخصص.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { showAddUserDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "+ إضافة مستخدم جديد",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // List of existing users/staff
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(staffUsers, key = { it.id }) { user ->
                            StaffUserCard(
                                user = user,
                                onShowQr = { userForQrDialog = user },
                                onEdit = { userToEdit = user },
                                onDelete = { userToDelete = user }
                            )
                        }
                    }
                }
            }
        }
    }

    // QR Code Login Dialog
    if (userForQrDialog != null) {
        val user = userForQrDialog!!
        StaffLoginQrDialog(
            user = user,
            merchantEmail = merchantEmail,
            storeName = storeName,
            onDismiss = { userForQrDialog = null }
        )
    }

    // Add or Edit User Dialog with Granular Permissions Checkboxes
    if (showAddUserDialog || userToEdit != null) {
        val editingUser = userToEdit
        StaffUserPermissionsDialog(
            userToEdit = editingUser,
            onDismiss = {
                showAddUserDialog = false
                userToEdit = null
            },
            onSave = { savedUser ->
                if (editingUser != null) {
                    viewModel.updateStaffUser(savedUser)
                    Toast.makeText(context, "تم تعديل صلاحيات المستخدم بنجاح ✓", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addStaffUser(savedUser)
                    Toast.makeText(context, "تم إضافة المستخدم بنجاح ✓", Toast.LENGTH_SHORT).show()
                }
                showAddUserDialog = false
                userToEdit = null
                userForQrDialog = savedUser
            }
        )
    }

    // Delete Confirmation Dialog
    if (userToDelete != null) {
        val user = userToDelete!!
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { userToDelete = null },
                title = { Text("حذف المستخدم", fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد من رغبتك في حذف حساب ${user.name}؟ لن يتمكن من تسجيل الدخول بعد الآن.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.removeStaffUser(user.id)
                            userToDelete = null
                            Toast.makeText(context, "تم حذف المستخدم", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LanaRed)
                    ) {
                        Text("حذف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { userToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

/**
 * User Card Component
 */
@Composable
private fun StaffUserCard(
    user: StaffUser,
    onShowQr: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val permCount = user.permissions.size
    val totalPermCount = AppPermissions.ALL_PERMISSIONS.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("staff_user_card_${user.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Right Side: Avatar + Name + Permissions Pill Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TawthiqPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.avatarUri.isNotBlank()) {
                        AsyncImage(
                            model = user.avatarUri,
                            contentDescription = user.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PersonOutline,
                            contentDescription = null,
                            tint = TawthiqPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = user.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "(${user.role})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (user.phone.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = user.phone,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val isFull = user.permissionType == "كامل الصلاحيات" || permCount == totalPermCount
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isFull) TawthiqPrimary else Color(0xFF0284C7),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isFull) "كامل الصلاحيات" else "$permCount من $totalPermCount صلاحية",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }

            // Left Side: Action Icons (Edit + Share QR + Delete)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Edit Permissions Button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("staff_edit_button_${user.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل الصلاحيات",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Share / QR Login Code Icon
                IconButton(
                    onClick = onShowQr,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("staff_qr_button_${user.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "مشاركة رمز الدخول",
                        tint = TawthiqPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Trash Delete Icon
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("staff_delete_button_${user.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "حذف المستخدم",
                        tint = LanaRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Granular Staff User Creation and Permissions Assignment Dialog
 */
@Composable
private fun StaffUserPermissionsDialog(
    userToEdit: StaffUser?,
    onDismiss: () -> Unit,
    onSave: (StaffUser) -> Unit
) {
    val context = LocalContext.current
    val isEditing = userToEdit != null
    var name by remember(userToEdit) { mutableStateOf(userToEdit?.name ?: "") }
    var phone by remember(userToEdit) { mutableStateOf(userToEdit?.phone ?: "") }
    var role by remember(userToEdit) { mutableStateOf(userToEdit?.role ?: "محاسب") }
    var avatarUri by remember(userToEdit) { mutableStateOf(userToEdit?.avatarUri ?: "") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            avatarUri = uri.toString()
            Toast.makeText(context, "تم تحديد صورة الموظف", Toast.LENGTH_SHORT).show()
        }
    }

    // Selected permissions list state
    var selectedPermissions by remember(userToEdit) {
        mutableStateOf(
            userToEdit?.permissions?.toSet()
                ?: AppPermissions.DEFAULT_STAFF_PERMISSIONS.toSet()
        )
    }

    val availableRoles = listOf("محاسب", "كاشير", "موظف مبيعات", "مندوب توصيل", "مشرف فرع")

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(vertical = 16.dp)
                    .shadow(16.dp, shape = RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(TawthiqPrimary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = TawthiqPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isEditing) "تعديل المستخدم والصلاحيات" else "إضافة مستخدم وتحديد الصلاحيات",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "حدد الصلاحيات المتاحة لهذا المستخدم بدقة",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Dialog Body - Scrollable
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // User Avatar Photo Selection Box
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .align(Alignment.CenterHorizontally)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .border(2.dp, TawthiqPrimary, CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarUri.isNotBlank()) {
                                AsyncImage(
                                    model = avatarUri,
                                    contentDescription = "صورة الموظف",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "إضافة صورة",
                                        tint = TawthiqPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("صورة الموظف", fontSize = 10.sp, color = TawthiqPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Basic Details Inputs
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("اسم الموظف / المستخدم *") },
                            placeholder = { Text("مثال: محمد أحمد") },
                            singleLine = true,
                            colors = tawthiqTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("رقم الهاتف (اختياري)") },
                                placeholder = { Text("+9665...") },
                                singleLine = true,
                                colors = tawthiqTextFieldColors(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Job Role Selection
                        Text(
                            text = "المسمى الوظيفي:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableRoles.forEach { r ->
                                val selected = role == r
                                Surface(
                                    onClick = { role = r },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) TawthiqPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selected) TawthiqPrimary else Color.Transparent
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = r,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) TawthiqPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Quick Presets Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الصلاحيات المسموحة (${selectedPermissions.size} من ${AppPermissions.ALL_PERMISSIONS.size}):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Quick Select Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Select All
                            Surface(
                                onClick = {
                                    selectedPermissions = AppPermissions.ALL_PERMISSIONS.toSet()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = TawthiqPrimary.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TawthiqPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "تحديد الكل ✓",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TawthiqPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }

                            // Read Only Preset
                            Surface(
                                onClick = {
                                    selectedPermissions = AppPermissions.READ_ONLY_PERMISSIONS.toSet()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "قراءة فقط",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }

                            // Sales Accountant Preset
                            Surface(
                                onClick = {
                                    selectedPermissions = AppPermissions.DEFAULT_STAFF_PERMISSIONS.toSet()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "محاسب مبيعات",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }

                            // Clear All
                            Surface(
                                onClick = {
                                    selectedPermissions = emptySet()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = LanaRed.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LanaRed.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "إلغاء الكل ✗",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LanaRed,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }

                        // Categorized Granular Permissions List
                        AppPermissions.ALL_GROUPS.forEach { group ->
                            val groupKeys = group.permissions.map { it.key }
                            val allGroupSelected = groupKeys.all { it in selectedPermissions }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    // Group Header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedPermissions = if (allGroupSelected) {
                                                    selectedPermissions - groupKeys.toSet()
                                                } else {
                                                    selectedPermissions + groupKeys.toSet()
                                                }
                                            },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = group.categoryName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TawthiqPrimary
                                        )

                                        Text(
                                            text = if (allGroupSelected) "إلغاء المجموع" else "تحديد المجموع",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (allGroupSelected) LanaRed else Color(0xFF0284C7)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Items in Group
                                    group.permissions.forEach { item ->
                                        val isChecked = item.key in selectedPermissions

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedPermissions = if (isChecked) {
                                                        selectedPermissions - item.key
                                                    } else {
                                                        selectedPermissions + item.key
                                                    }
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    selectedPermissions = if (checked == true) {
                                                        selectedPermissions + item.key
                                                    } else {
                                                        selectedPermissions - item.key
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = TawthiqPrimary),
                                                modifier = Modifier.size(28.dp)
                                            )

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isChecked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = item.description,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dialog Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    Toast.makeText(context, "يرجى كتابة اسم المستخدم", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (selectedPermissions.isEmpty()) {
                                    Toast.makeText(context, "يرجى اختيار صلاحية واحدة على الأقل", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val permType = when {
                                    selectedPermissions.size == AppPermissions.ALL_PERMISSIONS.size -> "كامل الصلاحيات"
                                    selectedPermissions == AppPermissions.READ_ONLY_PERMISSIONS.toSet() -> "قراءة فقط"
                                    else -> "مخصص (${selectedPermissions.size} صلاحيات)"
                                }

                                val finalUser = if (isEditing) {
                                    userToEdit!!.copy(
                                        name = name.trim(),
                                        phone = phone.trim(),
                                        role = role,
                                        avatarUri = avatarUri,
                                        permissionType = permType,
                                        permissions = selectedPermissions.toList()
                                    )
                                } else {
                                    StaffUser(
                                        name = name.trim(),
                                        phone = phone.trim(),
                                        role = role,
                                        avatarUri = avatarUri,
                                        permissionType = permType,
                                        permissions = selectedPermissions.toList()
                                    )
                                }
                                onSave(finalUser)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isEditing) "حفظ التعديلات" else "إضافة وحفظ الصلاحيات",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(0.4f)
                        ) {
                            Text("إلغاء", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/**
 * QR Code Login Dialog
 */
@Composable
private fun StaffLoginQrDialog(
    user: StaffUser,
    merchantEmail: String,
    storeName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // Efficiently encode permissions & payload to prevent QR payload overflow
    val permsEncoded = when {
        user.permissionType == "كامل الصلاحيات" || user.permissionType.startsWith("كامل") || user.permissions.size >= AppPermissions.ALL_PERMISSIONS.size -> "ALL"
        user.permissionType == "قراءة فقط" || user.permissions == AppPermissions.READ_ONLY_PERMISSIONS -> "READ_ONLY"
        else -> java.net.URLEncoder.encode(user.permissions.joinToString(","), "UTF-8")
    }
    val avatarEncoded = if (user.avatarUri.isNotBlank() && user.avatarUri.length < 120) {
        java.net.URLEncoder.encode(user.avatarUri, "UTF-8")
    } else ""

    val qrPayload = "tawthiq://staff_login?email=${merchantEmail.trim()}&userId=${user.id}&name=${java.net.URLEncoder.encode(user.name, "UTF-8")}&role=${java.net.URLEncoder.encode(user.role, "UTF-8")}&permission=${java.net.URLEncoder.encode(user.permissionType, "UTF-8")}&perms=$permsEncoded&store=${java.net.URLEncoder.encode(storeName, "UTF-8")}&avatar=$avatarEncoded"
    val qrBitmap = remember(qrPayload) {
        QrGeneratorHelper.generateQrImageBitmap(qrPayload, size = 600, darkColor = android.graphics.Color.BLACK)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 24.dp)
                    .shadow(12.dp, shape = RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "رمز دخول ${user.name}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "شارك هذا الرمز مع المستخدم ليقوم بمسحه من شاشة الدخول. سيمنح هذا الرمز فقط الصلاحيات الـ ${user.permissions.size} المحددة له.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 19.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(TawthiqPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (user.avatarUri.isNotBlank()) {
                                AsyncImage(
                                    model = user.avatarUri,
                                    contentDescription = user.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PersonOutline,
                                    contentDescription = null,
                                    tint = TawthiqPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // QR Code Frame (always white background for camera scan contrast)
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = "رمز QR لدخول ${user.name}",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("جارٍ إنشاء رمز QR...", color = Color.Gray, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Share Via WhatsApp / System Button
                    Button(
                        onClick = {
                            try {
                                val bitmapToShare = qrBitmap?.asAndroidBitmap()
                                    ?: QrGeneratorHelper.generateQrBitmap(qrPayload, 600, 600, android.graphics.Color.BLACK, android.graphics.Color.WHITE)

                                if (bitmapToShare != null) {
                                    val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
                                    val imageFile = File(exportDir, "qr_login_${user.id}_${System.currentTimeMillis()}.png")
                                    FileOutputStream(imageFile).use { out ->
                                        bitmapToShare.compress(Bitmap.CompressFormat.PNG, 100, out)
                                    }

                                    val contentUri: Uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        imageFile
                                    )

                                    val cleanPhone = user.phone.replace("+", "").replace(" ", "").replace("-", "").replace("(", "").replace(")", "").trim()
                                    val caption = "مرحباً ${user.name} 👋\nإليك رمز QR الخاص بتسجيل الدخول كـ (${user.role}) في متجر $storeName عبر تطبيق البيان."

                                    val imageIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, contentUri)
                                        putExtra(Intent.EXTRA_TEXT, caption)
                                        putExtra(Intent.EXTRA_SUBJECT, "رمز دخول تطبيق البيان - ${user.name}")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }

                                    var success = false
                                    if (cleanPhone.isNotBlank()) {
                                        try {
                                            val waIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "image/png"
                                                putExtra(Intent.EXTRA_STREAM, contentUri)
                                                putExtra(Intent.EXTRA_TEXT, caption)
                                                setPackage("com.whatsapp")
                                                putExtra("jid", "$cleanPhone@s.whatsapp.net")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(waIntent)
                                            success = true
                                        } catch (_: Exception) {
                                            try {
                                                val wabIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "image/png"
                                                    putExtra(Intent.EXTRA_STREAM, contentUri)
                                                    putExtra(Intent.EXTRA_TEXT, caption)
                                                    setPackage("com.whatsapp.w4b")
                                                    putExtra("jid", "$cleanPhone@s.whatsapp.net")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(wabIntent)
                                                success = true
                                            } catch (_: Exception) {}
                                        }
                                    }

                                    if (!success) {
                                        try {
                                            val waGeneralIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "image/png"
                                                putExtra(Intent.EXTRA_STREAM, contentUri)
                                                putExtra(Intent.EXTRA_TEXT, caption)
                                                setPackage("com.whatsapp")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(waGeneralIntent)
                                        } catch (_: Exception) {
                                            val chooser = Intent.createChooser(imageIntent, "مشاركة رمز الدخول مع ${user.name}")
                                            context.startActivity(chooser)
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "عذراً، تعذر إعداد صورة QR للمشاركة", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "خطأ أثناء المشاركة: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TawthiqPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مشاركة الرابط والرمز مع ${user.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Close Button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "إغلاق",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
