package com.example.data.model

/**
 * Granular application permissions definitions for Al-Bayan (البيان)
 */
object AppPermissions {
    // 1. Accounts & Customers Permissions
    const val VIEW_ACCOUNTS = "عرض الحسابات والعملاء"
    const val ADD_ACCOUNT = "إضافة حساب أو عميل جديد"
    const val EDIT_ACCOUNT = "تعديل بيانات الحسابات"
    const val DELETE_ACCOUNT = "حذف الحسابات والعملاء"

    // 2. Transactions & Ledger Permissions
    const val VIEW_TRANSACTIONS = "عرض المعاملات والقيود"
    const val ADD_TRANSACTION = "إضافة معاملة جديدة (قيد لنا/له)"
    const val EDIT_TRANSACTION = "تعديل القيود والمعاملات"
    const val DELETE_TRANSACTION = "حذف القيود والمعاملات"

    // 3. Reports & Dues Permissions
    const val VIEW_REPORTS = "عرض التقارير والأرصدة العامة"
    const val EXPORT_REPORTS = "تصدير واستخراج التقارير (Excel/PDF)"
    const val VIEW_DUES = "عرض تقرير المستحقات والديون"
    const val SETTLE_DUES = "تسوية الديون وتحصيل المستحقات"

    // 4. System & Administration Permissions
    const val ACCESS_SETTINGS = "الوصول لشاشة الإعدادات"
    const val MANAGE_STAFF = "إدارة الموظفين والمستخدمين"
    const val ACCESS_ADMIN = "لوحة تحكم الإدارة والاشتراكات"

    /**
     * Grouping structure for UI display with categories and icon hints
     */
    data class PermissionGroup(
        val categoryName: String,
        val permissions: List<PermissionItem>
    )

    data class PermissionItem(
        val key: String,
        val title: String,
        val description: String
    )

    val ALL_GROUPS = listOf(
        PermissionGroup(
            categoryName = "الحسابات والعملاء",
            permissions = listOf(
                PermissionItem(VIEW_ACCOUNTS, "عرض الحسابات والعملاء", "إمكانية استعراض قائمة العملاء والتجار والأرصدة"),
                PermissionItem(ADD_ACCOUNT, "إضافة حساب جديد", "السماح بإنشاء حساب عميل أو تاجر جديد"),
                PermissionItem(EDIT_ACCOUNT, "تعديل بيانات الحسابات", "تغيير الاسم، الهاتف، الفئة أو العملة"),
                PermissionItem(DELETE_ACCOUNT, "حذف الحسابات والعملاء", "إمكانية حذف حساب عميل نهائياً")
            )
        ),
        PermissionGroup(
            categoryName = "المعاملات والقيود المالية",
            permissions = listOf(
                PermissionItem(VIEW_TRANSACTIONS, "عرض القيود والمعاملات", "رؤية تفاصيل الحركات السابقة والحالية"),
                PermissionItem(ADD_TRANSACTION, "إضافة معاملة جديدة", "تسجيل قيد (لنا / له / دفعات / ديون)"),
                PermissionItem(EDIT_TRANSACTION, "تعديل القيود والمعاملات", "تعديل المبالغ والتفاصيل للمعاملات السابقة"),
                PermissionItem(DELETE_TRANSACTION, "حذف القيود والمعاملات", "حذف معاملة مالية مسجلة")
            )
        ),
        PermissionGroup(
            categoryName = "التقارير والمستحقات",
            permissions = listOf(
                PermissionItem(VIEW_REPORTS, "عرض التقارير والأرصدة", "استعراض تقرير أرصدة العملات والملخص"),
                PermissionItem(EXPORT_REPORTS, "تصدير وطباعة التقارير", "مشاركة أو تحميل كشوفات الحساب وملفات Excel"),
                PermissionItem(VIEW_DUES, "عرض تقرير المستحقات", "متابعة المواعيد المستحقة للتحصيل والسداد"),
                PermissionItem(SETTLE_DUES, "تسوية الديون وتأكيد السداد", "تغيير حالة الدين إلى تم التسوية")
            )
        ),
        PermissionGroup(
            categoryName = "الإدارة والنظام",
            permissions = listOf(
                PermissionItem(ACCESS_SETTINGS, "الوصول للإعدادات", "تغيير العملة الافتراضية والمظهر والبيانات"),
                PermissionItem(MANAGE_STAFF, "إدارة الموظفين والمستخدمين", "إضافة وتعديل صلاحيات المستخدمين الآخرين"),
                PermissionItem(ACCESS_ADMIN, "لوحة تحكم الإدارة", "إدارة الاشتراكات والتراخيص العامة")
            )
        )
    )

    val ALL_PERMISSIONS = ALL_GROUPS.flatMap { group -> group.permissions.map { it.key } }

    val DEFAULT_STAFF_PERMISSIONS = listOf(
        VIEW_ACCOUNTS,
        ADD_ACCOUNT,
        VIEW_TRANSACTIONS,
        ADD_TRANSACTION,
        VIEW_REPORTS,
        VIEW_DUES
    )

    val READ_ONLY_PERMISSIONS = listOf(
        VIEW_ACCOUNTS,
        VIEW_TRANSACTIONS,
        VIEW_REPORTS,
        VIEW_DUES
    )
}
