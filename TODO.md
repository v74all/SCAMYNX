# 📋 SCAMYNX - لیست کارهای باقی‌مانده

این فایل شامل تمام موارد ناقص یا گم‌شده در پروژه SCAMYNX است که باید تکمیل شوند.

---

## � مشکلات فوری Build

### 0. ✅ مشکل Kotlin Serialization و Room (حل شد)
- **مشکل:** `AbstractMethodError` در `FieldBundle$$serializer` - نسخه Kotlin Serialization با Room ناسازگار بود
- **فایل:** `gradle/libs.versions.toml`
- **راه‌حل:** ✅ Downgrade `kotlinSerialization` از `1.8.1` به `1.7.3` که با Room 2.8.x سازگار است

---

## ✅ اولویت بحرانی (Critical) - حل شده

### 1. ✅ تطابق Feature Extractor با مدل ML (حل شد)
- **وضعیت:** تمام ۲۵ ویژگی در `UrlFeatureExtractor.kt` پیاده‌سازی شده‌اند
- **فایل:** `ml/src/main/java/com/v7lthronyx/scamynx/ml/feature/UrlFeatureExtractor.kt`
- **ویژگی‌ها:** `length`, `digit_ratio`, `special_ratio`, `keyword_hits`, `entropy`, `path_depth`, `query_length`, `form_count`, `subdomain_count`, `domain_length`, `has_ip_address`, `tld_risk`, `hyphen_count`, `brand_impersonation`, `url_shortener`, `homograph_risk`, `subdomain_entropy`, `phishing_pattern`, `urgency_score`, `password_fields`, `hidden_fields`, `iframe_count`, `external_link_ratio`, `no_https`, `non_std_port`

### 2. ✅ فیلد `openAiApiKey` در ApiCredentials (حل شد)
- **وضعیت:** چک‌لیست نشان می‌دهد این مورد قبلاً حل شده

---

## 🟠 اولویت بالا (High)

### 3. ✅ سرویس QRCodeScanner پیاده‌سازی شده
- **وضعیت:** پیاده‌سازی کامل با CameraX و ML Kit انجام شد
- **فایل‌های جدید:**
  - `data/src/main/java/.../data/qrcode/QRCodeScannerServiceImpl.kt` - پیاده‌سازی اصلی
  - `data/src/main/java/.../data/qrcode/QRCodeParser.kt` - تحلیل محتوای QR
  - `data/src/main/java/.../data/qrcode/QRCodeThreatAnalyzer.kt` - تحلیل تهدیدات
  - `data/src/main/java/.../data/qrcode/QRCodeHistoryDao.kt` - ذخیره تاریخچه
  - `data/src/main/java/.../data/qrcode/QRCodePreferences.kt` - تنظیمات
  - `data/src/main/java/.../data/di/QRCodeScannerModule.kt` - Hilt DI
- **وابستگی‌های جدید:**
  - `com.google.mlkit:barcode-scanning:17.3.0`
  - CameraX (قبلاً موجود بود)

### 4. ✅ سرویس RealTimeProtection پیاده‌سازی شده
- **وضعیت:** پیاده‌سازی کامل با پشتیبانی از تمام قابلیت‌ها انجام شد
- **فایل‌های جدید:**
  - `data/src/main/java/.../data/realtimeprotection/RealTimeProtectionServiceImpl.kt` - پیاده‌سازی اصلی
  - `data/src/main/java/.../data/realtimeprotection/RealTimeProtectionPreferences.kt` - تنظیمات DataStore
  - `data/src/main/java/.../data/realtimeprotection/BlockedThreatsDao.kt` - ذخیره تهدیدات
  - `data/src/main/java/.../data/realtimeprotection/LinkThreatAnalyzer.kt` - تحلیل لینک‌ها
  - `data/src/main/java/.../data/di/RealTimeProtectionModule.kt` - Hilt DI
- **قابلیت‌ها:**
  - اسکن لینک‌ها با شناسایی تهدیدات
  - اسکن امنیتی برنامه‌ها
  - مانیتورینگ امنیت شبکه
  - اسکن نوتیفیکیشن‌ها
  - مانیتورینگ کلیپ‌بورد
  - مدیریت لیست سفید/سیاه

### 5. ✅ Hash و IP Lookup در ThreatIntel پیاده‌سازی شد
- **وضعیت:** متدهای `lookupHash` و `lookupIp` با استفاده از VirusTotal API پیاده‌سازی شدند
- **فایل‌های جدید/تغییر یافته:**
  - `data/src/main/java/.../threatintel/VirusTotalLookupDataSource.kt` - دیتاسورس جدید برای lookup
  - `data/src/main/java/.../network/api/VirusTotalApi.kt` - اضافه شدن endpoint های hash و IP
  - `data/src/main/java/.../network/model/VirusTotalDtos.kt` - DTOهای جدید برای پاسخ hash و IP
  - `app/src/main/java/.../ui/threatintel/ThreatIntelViewModel.kt` - پیاده‌سازی searchHash و searchIp
  - `domain/src/main/kotlin/.../repository/ThreatFeedRepository.kt` - اضافه شدن ThreatLookupResult

### 6. ✅ اسکن اینستاگرام فعال شد
- **وضعیت:** اسکن اینستاگرام کاملاً پیاده‌سازی شده بود، فقط UI نشان می‌داد "Coming Soon"
- **تغییرات:**
  - `app/src/main/res/values/strings.xml` - حذف "(Coming Soon)" از عنوان و به‌روزرسانی متن‌های مرتبط
  - `InstagramScamAnalyzer` قبلاً کاملاً پیاده‌سازی شده بود
  - `ScanRepositoryImpl` از قبل اسکن اینستاگرام را پشتیبانی می‌کرد
  - `HomeViewModel` تمام اکشن‌های فرم اینستاگرام را مدیریت می‌کند

---

## 🟡 اولویت متوسط (Medium)

### 7. ✅ سرویس NetworkMonitor پیاده‌سازی شد
- **وضعیت:** پیاده‌سازی کامل با تمام قابلیت‌های مانیتورینگ شبکه
- **فایل‌های جدید:**
  - `data/src/main/java/.../networkmonitor/NetworkMonitorServiceImpl.kt` - پیاده‌سازی اصلی
  - `data/src/main/java/.../di/NetworkMonitorModule.kt` - Hilt DI
- **قابلیت‌ها:**
  - مانیتورینگ اتصالات فعال (خواندن از /proc/net/tcp و udp)
  - ردیابی مصرف داده برنامه‌ها
  - مدیریت کوئری‌های DNS و آمار
  - سیستم هشدار برای اتصالات مشکوک
  - فایروال با قوانین سفارشی
  - مسدودسازی دامنه و برنامه
  - تحلیل ناهنجاری‌ها و نگرانی‌های حریم خصوصی
  - خروجی JSON از لاگ‌ها

### 8. ❌ سرویس AppPermissionAudit پیاده‌سازی نشده
- **مشکل:** اینترفیس `AppPermissionAuditService` بدون implementation
- **فایل:** `domain/src/main/kotlin/.../service/AppPermissionAuditService.kt`
- **اقدام:** پیاده‌سازی بررسی مجوزهای برنامه‌های نصب شده

### 9. ✅ سرویس Dark Web Monitoring پیاده‌سازی شد
- **وضعیت:** پیاده‌سازی کامل با تمام قابلیت‌های مانیتورینگ دارک وب
- **فایل‌های جدید:**
  - `domain/src/main/kotlin/.../service/DarkWebMonitoringService.kt` - اینترفیس سرویس
  - `domain/src/main/kotlin/.../repository/DarkWebMonitoringRepository.kt` - اینترفیس ریپازیتوری
  - `data/src/main/java/.../darkwebmonitoring/DarkWebMonitoringServiceImpl.kt` - پیاده‌سازی اصلی
  - `data/src/main/java/.../darkwebmonitoring/DarkWebMonitoringRepositoryImpl.kt` - پیاده‌سازی ریپازیتوری
  - `data/src/main/java/.../darkwebmonitoring/DarkWebMonitoringDao.kt` - Room DAO
  - `data/src/main/java/.../darkwebmonitoring/DarkWebMonitoringEntities.kt` - Entity‌های Room
  - `data/src/main/java/.../darkwebmonitoring/DarkWebMonitoringPreferences.kt` - تنظیمات DataStore
  - `data/src/main/java/.../darkwebmonitoring/DarkWebTypeConverters.kt` - TypeConverter‌های Room
  - `data/src/main/java/.../di/DarkWebMonitoringModule.kt` - Hilt DI
- **قابلیت‌ها:**
  - مدیریت دارایی‌های تحت نظارت (ایمیل، تلفن، نام کاربری و...)
  - اسکن خودکار و دستی برای نشت داده
  - شناسایی و ذخیره exposure‌ها
  - سیستم هشدار برای تهدیدات جدید
  - گزارش‌دهی جامع با امتیاز ریسک
  - ادغام با HIBP API برای بررسی نشت

### 10. ✅ Username Breach Check پیاده‌سازی شد
- **وضعیت:** پیاده‌سازی کامل با چند روش مختلف برای بررسی نشت نام‌کاربری
- **فایل‌های تغییر یافته:**
  - `data/src/main/java/.../breachmonitoring/BreachMonitoringServiceImpl.kt`
  - `data/src/main/java/.../darkwebmonitoring/DarkWebMonitoringServiceImpl.kt`
- **روش‌های بررسی:**
  - بررسی نام‌کاربری در لیست‌های رمز عبور با k-anonymity
  - بررسی ترکیب نام‌کاربری با دامنه‌های ایمیل رایج (gmail, yahoo, etc.)
  - پشتیبانی از HIBP API Key برای بررسی‌های پیشرفته‌تر
- **قابلیت‌ها:**
  - `checkUsername()` در BreachMonitoringService
  - `checkUsernameBreaches()` در DarkWebMonitoringService
  - `checkHibpBreachedAccount()` برای استفاده با API Key
  - `checkPasswordPwned()` برای بررسی k-anonymity

---

## 🟢 اولویت پایین (Low)

### 11. ✅ onClick در SecurityScoreScreen اضافه شد
- **وضعیت:** عملکرد اشتراک‌گذاری امتیاز امنیتی به دکمه Share اضافه شد
- **فایل:** `app/src/main/java/.../ui/securityscore/SecurityScoreScreen.kt`
- **تغییرات:**
  - اضافه شدن import های `Intent` و `LocalContext`
  - پیاده‌سازی `onShareScore` با استفاده از `Intent.ACTION_SEND`
  - استفاده از `shareableText` از `SecurityBadge` برای متن اشتراک‌گذاری
  - اتصال onClick به عملکرد اشتراک‌گذاری

### 12. ✅ Telemetry آماده Supabase شد
- **وضعیت:** Edge Function و راهنمای کامل برای Supabase ایجاد شد
- **فایل‌های جدید:**
  - `docs/supabase/functions/telemetry/index.ts` - Edge Function
  - `docs/supabase/TELEMETRY_SETUP.md` - راهنمای کامل تنظیم
- **اقدام باقی‌مانده:** 
  1. Deploy کردن Edge Function در Supabase
  2. ساخت جدول `telemetry_events` در دیتابیس
  3. تنظیم `SCAMYNX_TELEMETRY_ENDPOINT` در secrets.properties

---

## 📝 کارهای تکمیلی طبق api-ml-integration-checklist.md

| کار | وضعیت |
|-----|--------|
| تکمیل اینترفیس VirusTotal API با منطق retry | ⚠️ نیمه‌کاره |
| TelemetryRepository با صف آفلاین در Room | ⚠️ بدون persistence |
| WorkManager PeriodicWork برای Threat Feed Sync | ⚠️ فقط اسکلت |
| مکانیزم آپدیت مدل از CDN | ❌ پیاده‌سازی نشده |
| ذخیره نسخه مدل ML در DataStore | ❌ پیاده‌سازی نشده |

---

## 🔧 کلیدهای API مورد نیاز

در فایل `secrets.properties` این کلیدها را تنظیم کنید:

```properties
VIRUSTOTAL_API_KEY=your_key_here
GOOGLE_SAFE_BROWSING_API_KEY=your_key_here
URLSCAN_API_KEY=your_key_here
GROQ_API_KEY=your_groq_key_here       # برای AI Co-Pilot (Primary - رایگان و سریع)
OPENAI_API_KEY=your_openai_key_here   # برای AI Co-Pilot (Fallback)
SCAMYNX_TELEMETRY_ENDPOINT=https://your-project.supabase.co/functions/v1/telemetry
```

---

## 🤖 یکپارچه‌سازی AI Co-Pilot

### ✅ پیاده‌سازی شد! (Groq + OpenAI با Fallback)

| Provider | مدل | وضعیت | سرعت |
|----------|-----|-------|------|
| **Groq** | Llama 3.3 70B | Primary | ⚡ خیلی سریع |
| **OpenAI** | GPT-4o-mini | Fallback | 🐢 معمولی |

### فایل‌های اصلی:
- `data/src/main/java/.../ai/AiCoPilot.kt` - پیاده‌سازی یکپارچه
- `data/src/main/java/.../util/ApiCredentials.kt` - کلیدهای API

### جریان کار:
```
URL → Vendors + ML + Network → AI Co-Pilot → Final Score
                                    ↓
                              1. Try Groq
                              2. If fails → Try OpenAI
                              3. If both fail → Skip AI
```

### نحوه استفاده:
1. کلید Groq از https://console.groq.com بگیر
2. (اختیاری) کلید OpenAI از https://platform.openai.com
3. توی `secrets.properties` اضافه کن

### توجه:
- اگه هیچ کلیدی نباشه، AI skip میشه و بقیه pipeline کار میکنه

---

## ✅ چک‌لیست قبل از Release

- [x] تمام ۲۵ ویژگی در UrlFeatureExtractor پیاده‌سازی شود ✅
- [x] فیلد openAiApiKey به ApiCredentials اضافه شود ✅
- [x] Hash/IP lookup در ThreatIntel کار کند ✅
- [ ] QRCodeScannerService پیاده‌سازی شود
- [ ] RealTimeProtectionService پیاده‌سازی شود
- [ ] تست واحد برای تمام سرویس‌ها نوشته شود
- [ ] تست یکپارچگی build انجام شود

---

**آخرین بروزرسانی:** ۱۲ بهمن ۱۴۰۴ (February 1, 2026)
