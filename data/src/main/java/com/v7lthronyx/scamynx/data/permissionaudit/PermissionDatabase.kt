package com.v7lthronyx.scamynx.data.permissionaudit

import android.Manifest
import com.v7lthronyx.scamynx.domain.model.PermissionDetail
import com.v7lthronyx.scamynx.domain.model.PermissionGroup
import com.v7lthronyx.scamynx.domain.model.PermissionProtectionLevel
import com.v7lthronyx.scamynx.domain.model.PermissionRiskLevel

/**
 * Database of permission details including risk levels, descriptions, and privacy implications.
 */
object PermissionDatabase {

    /**
     * Map of permission name to its details.
     */
    val permissions: Map<String, PermissionDetail> by lazy { buildPermissionMap() }

    /**
     * Get permission detail, with fallback for unknown permissions.
     */
    fun getPermissionDetail(permission: String): PermissionDetail {
        return permissions[permission] ?: createUnknownPermission(permission)
    }

    /**
     * Get all dangerous permissions.
     */
    fun getDangerousPermissions(): List<PermissionDetail> {
        return permissions.values.filter {
            it.protectionLevel == PermissionProtectionLevel.DANGEROUS
        }
    }

    /**
     * Get permissions by group.
     */
    fun getPermissionsByGroup(group: PermissionGroup): List<PermissionDetail> {
        return permissions.values.filter { it.group == group }
    }

    /**
     * Search permissions by name or description.
     */
    fun searchPermissions(query: String): List<PermissionDetail> {
        val lowerQuery = query.lowercase()
        return permissions.values.filter {
            it.name.lowercase().contains(lowerQuery) ||
                it.label.lowercase().contains(lowerQuery) ||
                it.description.lowercase().contains(lowerQuery)
        }
    }

    private fun createUnknownPermission(permission: String): PermissionDetail {
        val simpleName = permission.substringAfterLast(".")
        return PermissionDetail(
            name = permission,
            label = simpleName.replace("_", " ").lowercase()
                .replaceFirstChar { it.uppercase() },
            description = "اجازه سیستمی: $simpleName",
            group = PermissionGroup.OTHER,
            riskLevel = PermissionRiskLevel.LOW,
            protectionLevel = PermissionProtectionLevel.NORMAL,
            isRuntime = false,
            privacyImplications = listOf("تاثیر حریم خصوصی نامشخص"),
            commonAbuses = emptyList(),
        )
    }

    @Suppress("LongMethod")
    private fun buildPermissionMap(): Map<String, PermissionDetail> = mapOf(
        // ============== LOCATION ==============
        Manifest.permission.ACCESS_FINE_LOCATION to PermissionDetail(
            name = Manifest.permission.ACCESS_FINE_LOCATION,
            label = "موقعیت دقیق",
            description = "دسترسی به موقعیت GPS دقیق دستگاه",
            group = PermissionGroup.LOCATION,
            riskLevel = PermissionRiskLevel.DANGEROUS,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "ردیابی مکان دقیق شما",
                "ایجاد الگوی حرکتی",
                "شناسایی آدرس منزل و محل کار",
            ),
            commonAbuses = listOf(
                "فروش داده‌های موقعیت به تبلیغ‌کنندگان",
                "ردیابی بدون اطلاع کاربر",
            ),
        ),

        Manifest.permission.ACCESS_COARSE_LOCATION to PermissionDetail(
            name = Manifest.permission.ACCESS_COARSE_LOCATION,
            label = "موقعیت تقریبی",
            description = "دسترسی به موقعیت تقریبی (شبکه)",
            group = PermissionGroup.LOCATION,
            riskLevel = PermissionRiskLevel.HIGH,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "ردیابی موقعیت تقریبی",
                "شناسایی شهر و منطقه",
            ),
            commonAbuses = listOf(
                "ردیابی غیرضروری",
            ),
        ),

        Manifest.permission.ACCESS_BACKGROUND_LOCATION to PermissionDetail(
            name = Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            label = "موقعیت در پس‌زمینه",
            description = "دسترسی به موقعیت حتی زمانی که برنامه بسته است",
            group = PermissionGroup.LOCATION,
            riskLevel = PermissionRiskLevel.DANGEROUS,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "ردیابی دائمی موقعیت",
                "ایجاد نمایه حرکتی کامل",
                "مصرف باتری بالا",
            ),
            commonAbuses = listOf(
                "ردیابی ۲۴/۷ بدون نیاز",
                "جمع‌آوری داده پس‌زمینه",
            ),
        ),

        // ============== CAMERA ==============
        Manifest.permission.CAMERA to PermissionDetail(
            name = Manifest.permission.CAMERA,
            label = "دوربین",
            description = "دسترسی به دوربین دستگاه برای عکسبرداری و فیلمبرداری",
            group = PermissionGroup.CAMERA,
            riskLevel = PermissionRiskLevel.DANGEROUS,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "گرفتن عکس و فیلم بدون اطلاع",
                "دسترسی به محیط اطراف",
            ),
            commonAbuses = listOf(
                "جاسوسی تصویری",
                "ضبط بدون رضایت",
            ),
        ),

        // ============== MICROPHONE ==============
        Manifest.permission.RECORD_AUDIO to PermissionDetail(
            name = Manifest.permission.RECORD_AUDIO,
            label = "میکروفون",
            description = "دسترسی به میکروفون برای ضبط صدا",
            group = PermissionGroup.MICROPHONE,
            riskLevel = PermissionRiskLevel.DANGEROUS,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "ضبط مکالمات",
                "شنود محیط اطراف",
            ),
            commonAbuses = listOf(
                "ضبط صدا در پس‌زمینه",
                "جمع‌آوری داده‌های صوتی",
            ),
        ),

        // ============== CONTACTS ==============
        Manifest.permission.READ_CONTACTS to PermissionDetail(
            name = Manifest.permission.READ_CONTACTS,
            label = "خواندن مخاطبین",
            description = "دسترسی به لیست مخاطبین شما",
            group = PermissionGroup.CONTACTS,
            riskLevel = PermissionRiskLevel.HIGH,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "دسترسی به شماره تلفن‌ها",
                "دسترسی به ایمیل‌ها",
                "شناسایی شبکه ارتباطی",
            ),
            commonAbuses = listOf(
                "ارسال اسپم به مخاطبین",
                "فروش لیست مخاطبین",
            ),
        ),

        Manifest.permission.WRITE_CONTACTS to PermissionDetail(
            name = Manifest.permission.WRITE_CONTACTS,
            label = "نوشتن مخاطبین",
            description = "تغییر و حذف مخاطبین",
            group = PermissionGroup.CONTACTS,
            riskLevel = PermissionRiskLevel.HIGH,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "تغییر اطلاعات مخاطبین",
                "اضافه کردن مخاطب جعلی",
            ),
            commonAbuses = listOf(
                "اضافه کردن شماره‌های اسپم",
            ),
        ),

        Manifest.permission.GET_ACCOUNTS to PermissionDetail(
            name = Manifest.permission.GET_ACCOUNTS,
            label = "دسترسی به حساب‌ها",
            description = "دسترسی به لیست حساب‌های کاربری روی دستگاه",
            group = PermissionGroup.CONTACTS,
            riskLevel = PermissionRiskLevel.MEDIUM,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "شناسایی ایمیل‌های شما",
                "شناسایی سرویس‌های استفاده‌شده",
            ),
            commonAbuses = listOf(
                "جمع‌آوری ایمیل برای اسپم",
            ),
        ),

        // ============== PHONE ==============
        Manifest.permission.READ_PHONE_STATE to PermissionDetail(
            name = Manifest.permission.READ_PHONE_STATE,
            label = "وضعیت تلفن",
            description = "دسترسی به اطلاعات تلفن شامل IMEI",
            group = PermissionGroup.PHONE,
            riskLevel = PermissionRiskLevel.HIGH,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "شناسایی یکتای دستگاه",
                "دسترسی به شماره تلفن",
                "ردیابی بین برنامه‌ای",
            ),
            commonAbuses = listOf(
                "ردیابی کاربر با IMEI",
                "fingerprinting دستگاه",
            ),
        ),

        Manifest.permission.CALL_PHONE to PermissionDetail(
            name = Manifest.permission.CALL_PHONE,
            label = "برقراری تماس",
            description = "برقراری تماس تلفنی مستقیم",
            group = PermissionGroup.PHONE,
            riskLevel = PermissionRiskLevel.DANGEROUS,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "تماس بدون اطلاع کاربر",
                "ایجاد هزینه تماس",
            ),
            commonAbuses = listOf(
                "تماس با شماره‌های پولی",
                "کلاهبرداری تلفنی",
            ),
        ),

        Manifest.permission.READ_PHONE_NUMBERS to PermissionDetail(
            name = Manifest.permission.READ_PHONE_NUMBERS,
            label = "خواندن شماره تلفن",
            description = "دسترسی به شماره تلفن دستگاه",
            group = PermissionGroup.PHONE,
            riskLevel = PermissionRiskLevel.HIGH,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "شناسایی شماره شما",
            ),
            commonAbuses = listOf(
                "اسپم SMS",
            ),
        ),

        Manifest.permission.ANSWER_PHONE_CALLS to PermissionDetail(
            name = Manifest.permission.ANSWER_PHONE_CALLS,
            label = "پاسخ به تماس",
            description = "پاسخ خودکار به تماس‌ها",
            group = PermissionGroup.PHONE,
            riskLevel = PermissionRiskLevel.MEDIUM,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "کنترل تماس‌های ورودی",
            ),
            commonAbuses = emptyList(),
        ),

        // ============== SMS ==============
        Manifest.permission.SEND_SMS to PermissionDetail(
            name = Manifest.permission.SEND_SMS,
            label = "ارسال پیامک",
            description = "ارسال پیامک از طرف شما",
            group = PermissionGroup.SMS,
            riskLevel = PermissionRiskLevel.DANGEROUS,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "ارسال پیامک بدون اطلاع",
                "ایجاد هزینه پیامک",
            ),
            commonAbuses = listOf(
                "ارسال به شماره‌های پولی",
                "انتشار بدافزار",
            ),
        ),

        Manifest.permission.READ_SMS to PermissionDetail(
            name = Manifest.permission.READ_SMS,
            label = "خواندن پیامک",
            description = "دسترسی به پیامک‌های شما",
            group = PermissionGroup.SMS,
            riskLevel = PermissionRiskLevel.DANGEROUS,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "دسترسی به کدهای احراز هویت",
                "خواندن پیامک‌های خصوصی",
            ),
            commonAbuses = listOf(
                "سرقت کد OTP",
                "جاسوسی پیامکی",
            ),
        ),

        Manifest.permission.RECEIVE_SMS to PermissionDetail(
            name = Manifest.permission.RECEIVE_SMS,
            label = "دریافت پیامک",
            description = "دریافت و پردازش پیامک‌های ورودی",
            group = PermissionGroup.SMS,
            riskLevel = PermissionRiskLevel.DANGEROUS,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "رهگیری کدهای OTP",
                "پردازش پیامک قبل از شما",
            ),
            commonAbuses = listOf(
                "سرقت کد بانکی",
            ),
        ),

        // ============== CALENDAR ==============
        Manifest.permission.READ_CALENDAR to PermissionDetail(
            name = Manifest.permission.READ_CALENDAR,
            label = "خواندن تقویم",
            description = "دسترسی به رویدادهای تقویم",
            group = PermissionGroup.CALENDAR,
            riskLevel = PermissionRiskLevel.MEDIUM,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "دسترسی به برنامه روزانه",
                "شناسایی جلسات و قرارها",
            ),
            commonAbuses = listOf(
                "جمع‌آوری اطلاعات رفتاری",
            ),
        ),

        Manifest.permission.WRITE_CALENDAR to PermissionDetail(
            name = Manifest.permission.WRITE_CALENDAR,
            label = "نوشتن تقویم",
            description = "اضافه یا حذف رویدادهای تقویم",
            group = PermissionGroup.CALENDAR,
            riskLevel = PermissionRiskLevel.MEDIUM,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "تغییر رویدادهای شما",
            ),
            commonAbuses = listOf(
                "اضافه کردن رویداد اسپم",
            ),
        ),

        // ============== CALL LOG ==============
        Manifest.permission.READ_CALL_LOG to PermissionDetail(
            name = Manifest.permission.READ_CALL_LOG,
            label = "خواندن تاریخچه تماس",
            description = "دسترسی به لیست تماس‌های شما",
            group = PermissionGroup.CALL_LOG,
            riskLevel = PermissionRiskLevel.DANGEROUS,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "شناسایی الگوی تماس",
                "دسترسی به شماره‌های تماس",
            ),
            commonAbuses = listOf(
                "جمع‌آوری شماره‌ها",
            ),
        ),

        Manifest.permission.WRITE_CALL_LOG to PermissionDetail(
            name = Manifest.permission.WRITE_CALL_LOG,
            label = "نوشتن تاریخچه تماس",
            description = "تغییر تاریخچه تماس‌ها",
            group = PermissionGroup.CALL_LOG,
            riskLevel = PermissionRiskLevel.HIGH,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "حذف شواهد تماس",
            ),
            commonAbuses = emptyList(),
        ),

        // ============== SENSORS ==============
        Manifest.permission.BODY_SENSORS to PermissionDetail(
            name = Manifest.permission.BODY_SENSORS,
            label = "سنسورهای بدنی",
            description = "دسترسی به سنسورهای سلامت (ضربان قلب و...)",
            group = PermissionGroup.SENSORS,
            riskLevel = PermissionRiskLevel.HIGH,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "دسترسی به داده‌های سلامتی",
                "شناسایی وضعیت فیزیکی",
            ),
            commonAbuses = listOf(
                "فروش داده‌های سلامتی",
            ),
        ),

        Manifest.permission.ACTIVITY_RECOGNITION to PermissionDetail(
            name = Manifest.permission.ACTIVITY_RECOGNITION,
            label = "تشخیص فعالیت",
            description = "تشخیص حرکت (پیاده‌روی، دویدن، رانندگی)",
            group = PermissionGroup.ACTIVITY_RECOGNITION,
            riskLevel = PermissionRiskLevel.MEDIUM,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "ردیابی فعالیت روزانه",
            ),
            commonAbuses = emptyList(),
        ),

        // ============== STORAGE ==============
        Manifest.permission.READ_EXTERNAL_STORAGE to PermissionDetail(
            name = Manifest.permission.READ_EXTERNAL_STORAGE,
            label = "خواندن حافظه",
            description = "دسترسی به فایل‌های ذخیره‌شده",
            group = PermissionGroup.STORAGE,
            riskLevel = PermissionRiskLevel.HIGH,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "دسترسی به عکس‌ها و اسناد",
                "دسترسی به فایل‌های شخصی",
            ),
            commonAbuses = listOf(
                "سرقت تصاویر",
                "جمع‌آوری اسناد",
            ),
        ),

        Manifest.permission.WRITE_EXTERNAL_STORAGE to PermissionDetail(
            name = Manifest.permission.WRITE_EXTERNAL_STORAGE,
            label = "نوشتن حافظه",
            description = "نوشتن و تغییر فایل‌ها",
            group = PermissionGroup.STORAGE,
            riskLevel = PermissionRiskLevel.HIGH,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "تغییر فایل‌ها",
                "ذخیره داده مخفی",
            ),
            commonAbuses = listOf(
                "نصب بدافزار",
            ),
        ),

        Manifest.permission.READ_MEDIA_IMAGES to PermissionDetail(
            name = Manifest.permission.READ_MEDIA_IMAGES,
            label = "خواندن تصاویر",
            description = "دسترسی به تصاویر و عکس‌ها",
            group = PermissionGroup.MEDIA,
            riskLevel = PermissionRiskLevel.MEDIUM,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "دسترسی به آلبوم عکس",
            ),
            commonAbuses = listOf(
                "سرقت تصاویر خصوصی",
            ),
        ),

        Manifest.permission.READ_MEDIA_VIDEO to PermissionDetail(
            name = Manifest.permission.READ_MEDIA_VIDEO,
            label = "خواندن ویدئو",
            description = "دسترسی به فایل‌های ویدئویی",
            group = PermissionGroup.MEDIA,
            riskLevel = PermissionRiskLevel.MEDIUM,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "دسترسی به ویدئوهای شخصی",
            ),
            commonAbuses = emptyList(),
        ),

        Manifest.permission.READ_MEDIA_AUDIO to PermissionDetail(
            name = Manifest.permission.READ_MEDIA_AUDIO,
            label = "خواندن صوت",
            description = "دسترسی به فایل‌های صوتی",
            group = PermissionGroup.MEDIA,
            riskLevel = PermissionRiskLevel.LOW,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "دسترسی به موسیقی و ضبط‌ها",
            ),
            commonAbuses = emptyList(),
        ),

        // ============== NEARBY DEVICES ==============
        Manifest.permission.BLUETOOTH_CONNECT to PermissionDetail(
            name = Manifest.permission.BLUETOOTH_CONNECT,
            label = "اتصال بلوتوث",
            description = "اتصال به دستگاه‌های بلوتوث",
            group = PermissionGroup.NEARBY_DEVICES,
            riskLevel = PermissionRiskLevel.MEDIUM,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "دسترسی به دستگاه‌های نزدیک",
            ),
            commonAbuses = emptyList(),
        ),

        Manifest.permission.BLUETOOTH_SCAN to PermissionDetail(
            name = Manifest.permission.BLUETOOTH_SCAN,
            label = "اسکن بلوتوث",
            description = "جستجوی دستگاه‌های بلوتوث نزدیک",
            group = PermissionGroup.NEARBY_DEVICES,
            riskLevel = PermissionRiskLevel.MEDIUM,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "شناسایی دستگاه‌های نزدیک",
                "ردیابی موقعیت غیرمستقیم",
            ),
            commonAbuses = emptyList(),
        ),

        Manifest.permission.NEARBY_WIFI_DEVICES to PermissionDetail(
            name = Manifest.permission.NEARBY_WIFI_DEVICES,
            label = "دستگاه‌های WiFi نزدیک",
            description = "دسترسی به دستگاه‌های WiFi اطراف",
            group = PermissionGroup.NEARBY_DEVICES,
            riskLevel = PermissionRiskLevel.MEDIUM,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "شناسایی شبکه‌های نزدیک",
            ),
            commonAbuses = emptyList(),
        ),

        // ============== NOTIFICATIONS ==============
        Manifest.permission.POST_NOTIFICATIONS to PermissionDetail(
            name = Manifest.permission.POST_NOTIFICATIONS,
            label = "ارسال نوتیفیکیشن",
            description = "نمایش اعلان‌ها",
            group = PermissionGroup.NOTIFICATIONS,
            riskLevel = PermissionRiskLevel.LOW,
            protectionLevel = PermissionProtectionLevel.DANGEROUS,
            isRuntime = true,
            privacyImplications = listOf(
                "ارسال اعلان‌های ناخواسته",
            ),
            commonAbuses = listOf(
                "تبلیغات مزاحم",
            ),
        ),
    )
}
