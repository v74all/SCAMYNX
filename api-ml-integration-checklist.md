# چک‌لیست نیازمندی‌های API و یادگیری ماشین برای SCAMYNX

این سند نقشهٔ راهی برای آماده‌سازی زیرساخت‌های بیرونی (APIها) و مؤلفه‌های لازم جهت افزودن قابلیت‌های یادگیری ماشین در اپلیکیشن SCAMYNX است. موارد براساس معماری فعلی (ماژول‌های `:data`, `:domain`, `:ml`, `:networksecurity`, `:report`) دسته‌بندی شده‌اند.

## ۱. سرویس‌های تهدیدشناسی و امنیت شبکه

### ۱.۱ VirusTotal
- **Endpoint پایه**: `https://www.virustotal.com/api/v3/`
- **کلید**: `VIRUSTOTAL_API_KEY`
- **Endpoints پیشنهادی**:
  - `urls/{id}` برای دریافت جزئیات اسکن URL.
  - `urls` (POST) برای ارسال اسکن جدید.
- **اقدامات لازم**:
  - تکمیل اینترفیس `VirusTotalApi` با مسیرهای فوق.
  - پیاده‌سازی مپِر DTO → Domain در `:data`.
  - افزودن منطق زمان‌بندی مجدد اسکن در صورت دریافت پاسخ `queued`.

### ۱.۲ Google Safe Browsing
- **Endpoint پایه**: `https://safebrowsing.googleapis.com/v4/`
- **کلید**: `GOOGLE_SAFE_BROWSING_API_KEY`
- **Endpoints پیشنهادی**:
  - `threatMatches:find` برای بررسی URL.
- **اقدامات لازم**:
  - تکمیل مدل درخواست/پاسخ در `GoogleSafeBrowsingDtos`.
  - افزودن rate limiting سطح سرویس (Safe Browsing محدودیت بالایی دارد).

### ۱.۳ URLScan.io
- **Endpoint پایه**: `https://urlscan.io/api/v1/`
- **کلید**: `URLSCAN_API_KEY`
- **Endpoints پیشنهادی**:
  - `scan` (POST) جهت ارسال URL.
  - `result/{uuid}` برای بازیابی نتیجه.
- **اقدامات لازم**:
  - پشتیبانی از webhooks یا polling جهت دریافت نتیجه نهایی.
  - نگهداری تاریخچهٔ اسکن‌ها در Room (جدول `threat_feed` یا جدول مستقل).

### ۱.۴ Threat Feed اختصاصی
- **Endpoint پایه**: به‌صورت قابل تنظیم (مثلاً `https://intel.yourdomain.com/`)
- **به‌روزرسانی**: از طریق `ThreatFeedApi.fetchLatest()`
- **اقدامات لازم**:
  - تعریف قرارداد JSON (فیلدهای threatId، riskScore، indicators، ttl).
  - ایجاد مپِر DTO → Entity → Domain.
  - افزودن WorkManager PeriodicWork برای Sync (در حال حاضر اسکلت وجود دارد).

### ۱.۵ سرویس تله‌متری (Telemetry)
- **Endpoint پایه**: مقداردهی از `SCAMYNX_TELEMETRY_ENDPOINT` در Secrets.
- **Endpoint نمونه**: `v1/telemetry/events`
- **اقدامات لازم**:
  - تعریف مدل رویداد (مثلاً `sessionId`, `timestamp`, `eventType`, `payload`).
  - افزودن صف داخلی (BufferedChannel یا Room) برای ارسال آفلاین.

## ۲. لایهٔ داده و ذخیره‌سازی
- **Room Migration**: افزایش نسخه دیتابیس برای اضافه کردن جداول threat feed و queue تله‌متری.
- **Repository Contracts**:
  - `ThreatFeedRepository` برای ترکیب دادهٔ محلی و شبکه.
  - `TelemetryRepository` جهت ارسال رویدادهای اپلیکیشن.
- **Caching Strategy**:
  - TTL مبتنی بر پیکربندی سرور (مثلاً ۶ ساعت).
  - سیاست پاکسازی داده‌های منقضی.

## ۳. نیازمندی‌های یادگیری ماشین

### ۳.۱ مدل اصلی (TensorFlow Lite)
- **وظیفه**: محاسبهٔ امتیاز ریسک URL.
- **فرمت**: فایل `.tflite` به همراه `metadata.json` برای توضیح ورودی/خروجی.
- **اقدامات لازم**:
  - قرار دادن مدل در ماژول `:ml` (پوشهٔ `src/main/ml/`).
  - ایجاد `Interpreter` با delegate مناسب (NNAPI / GPU در صورت نیاز).
  - هات‌پَث: تابعی در `MlRiskScorer` برای دریافت ویژگی‌ها و برگرداندن امتیاز.

### ۳.۲ استخراج ویژگی (Feature Engineering)
- **ورودی‌ها**:
  - ویژگی‌های URL (طول، تعداد کاراکترهای خاص، entropy).
  - سیگنال‌های محتوا (HTTP headers، DOM snapshot) - به کمک ماژول `:networksecurity`.
  - داده‌های Threat Feed (match score).
- **اقدامات لازم**:
  - تعریف data class `UrlFeatures` در `:domain`.
  - پیاده‌سازی `FeatureExtractor` در `:ml`.

### ۳.۳ ارزیابی مدل
- **ابزارها**: ML Kit Test Lab یا اجرای محلی با JUnit.
- **اقدامات لازم**:
  - افزودن تست واحد برای اعتبارسنجی خروجی مدل با دادهٔ نمونه.
  - ثبت متریک‌ها (AUC, Precision@K) در CI یا گزارش داخلی.

### ۳.۴ بروزرسانی مدل
- **استراتژی**:
  - دانلود مدل از CDN (Firebase App Distribution یا GCS)
  - تأیید امضای دیجیتال قبل از جایگزینی.
- **اقدامات لازم**:
  - افزودن کار WorkManager برای چک‌کردن نسخهٔ مدل.
  - ذخیرهٔ نسخهٔ فعلی در `DataStore`.

## ۴. هماهنگی با UI و گزارش‌دهی
- نمایش وضعیت اسکن (در حال جمع‌آوری داده، در حال تحلیل ML، دریافت نتیجه از API).
- ارائهٔ توضیح (Explainability) برای امتیاز ML:
  - نمایش سهم هر فاکتور (threat feed, URL heuristics, ML score).
- به‌روزرسانی `ReportRepositoryImpl` برای ضمیمه‌کردن جزئیات ML و نتایج API در خروجی PDF/JSON.

## ۵. الزامات عملیاتی و امنیتی
- **Rate Limiting**: پیاده‌سازی لایه نرم‌افزاری روی OkHttp برای جلوگیری از ban شدن کلیدها.
- **Observability**: ارسال رویدادهای خطا به تله‌متری و Crashlytics (در صورت نیاز).
- **Secrets Management**: استفاده از EncryptedSharedPreferences یا Android Keystore برای کلیدهای حساس (در صورت ذخیرهٔ محلی).
- **حریم خصوصی**: کنترل Opt-in/Opt-out تله‌متری و مستندسازی در Settings.

## ۶. جمع‌بندی اکشن‌های کوتاه‌مدت
1. تکمیل قراردادهای Retrofit برای هر سه سرویس تهدیدشناسی و Threat Feed اختصاصی.
2. پیاده‌سازی Repository/DAO مربوط به Threat Feed همراه با Migration.
3. افزودن TelemetryRepository و صف ارسال آفلاین.
4. جایگذاری مدل اولیهٔ TFLite و پیاده‌سازی FeatureExtractor.
5. افزودن تست‌های واحد برای مسیرهای ML و API.

با اجرای مراحل فوق، اپلیکیشن آمادهٔ تکمیل چرخهٔ اسکن، امتیازدهی ML، و گزارش‌دهی سرتاسری خواهد شد.
