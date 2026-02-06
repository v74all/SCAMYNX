# 🛠️ راهنمای کامل افزودن تنظیمات خصوصی SCAMYNX

این راهنما تمام فایل‌ها و مقادیری را که پس از کلون کردن مخزن باید **به‌صورت محلی** اضافه کنید پوشش می‌دهد تا قابلیت‌های Supabase، APIها و امضای ریلیز طبق پیاده‌سازی `SupabaseModule.kt` فعال شوند. تمام مراحل به‌ترتیب انجام شوند.

---

## 1. پیش‌نیازهای نرم‌افزاری
- Android Studio Hedgehog (یا جدیدتر)
- JDK 21 و Android SDK 34
- Supabase CLI (برای استقرار فانکشن) –‌ نصب از https://supabase.com/docs/guides/cli
- Git و Gradle Wrapper موجود در پروژه

---

## 2. پیکربندی `local.properties`
مسیر SDK دستگاه خود را مشخص کنید تا Gradle بتواند ابزارهای اندروید را پیدا کند:

```properties
# file: local.properties
sdk.dir=/path/to/Android/Sdk
```

> در لینوکس/مک معمولاً `~/Android/Sdk` و در ویندوز `C:\\Users\\USERNAME\\AppData\\Local\\Android\\Sdk` است.

---

## 3. تکمیل `secrets.properties`
1. فایل نمونه را کپی کنید:
   ```bash
   cp secrets.defaults.properties secrets.properties
   ```
2. کلیدها را مطابق جدول تکمیل کنید:

| کلید | کاربرد در برنامه | روش دریافت |
| --- | --- | --- |
| `VIRUSTOTAL_API_KEY` | اسکن URL/فایل در `ScanRepository` | [VirusTotal](https://www.virustotal.com/gui/join-us) |
| `GOOGLE_SAFE_BROWSING_API_KEY` | بررسی Safety API گوگل | [Google Safe Browsing](https://developers.google.com/safe-browsing/v4/get-started) |
| `URLSCAN_API_KEY` | آنالیز صفحه در URLScan | [URLScan.io](https://urlscan.io/user/signup) |
| `SCAMYNX_TELEMETRY_ENDPOINT` | رزرو شده برای تله‌متری | اگر سرویسی ندارید خالی بگذارید |
| `SUPABASE_URL` | مبنای `SupabaseModule.kt` برای Rest/Functions | از داشبورد Supabase > Settings > API |
| `SUPABASE_ANON_KEY` | کلید عمومی کلاینت | همان صفحه API |
| `SUPABASE_FUNCTION_JWT` | (اختیاری) JWT امضا شده برای صدا کردن Edge Function | اگر فانکشن با `verify_jwt()` محافظت شده باشد از تب API دریافت/تولید کنید |
| `OPENAI_API_KEY` | فقط برای تست محلی؛ در نسخهٔ ریلیز خالی باشد | از [OpenAI](https://platform.openai.com/api-keys) |

نمونهٔ تکمیل‌شده:
```properties
VIRUSTOTAL_API_KEY=vt_xxxxxxxxxxxxxxxxx
GOOGLE_SAFE_BROWSING_API_KEY=AIzaSy...
URLSCAN_API_KEY=key_xxxxx
SCAMYNX_TELEMETRY_ENDPOINT=
SUPABASE_URL=https://abcde.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
SUPABASE_FUNCTION_JWT= # در صورت عدم نیاز خالی بماند
OPENAI_API_KEY=
```

> فایل `secrets.properties` در `.gitignore` قرار دارد؛ هرگز آن را commit نکنید.

---

## 4. راه‌اندازی بک‌اند Supabase
به‌محض تکمیل مقادیر بالا، ماژول `SupabaseModule` می‌تواند کلاینت را بسازد. برای آماده‌سازی دیتابیس:

1. **ساخت جدول اشتراک تهدید** (SQL Editor):
   ```sql
   create table if not exists public.threat_indicators (
     indicator_id text primary key,
     url text not null,
     risk_score double precision not null,
     tags text[] default '{}',
     last_seen timestamptz,
     source text not null,
     fetched_at timestamptz default now()
   );
   ```
2. **فعال‌سازی RLS و سیاست خواندن عمومی:**
   ```sql
   alter table public.threat_indicators enable row level security;
   create policy "Allow read for anon"
     on public.threat_indicators
     for select using (true);
   ```
3. **Edge Function برای هوش تهدید (مثال `threat-intel-ai`):**
   ```bash
   supabase functions new threat-intel-ai
   supabase secrets set OPENAI_API_KEY=sk-xxxxx
   supabase functions deploy threat-intel-ai --project-ref <PROJECT_ID>
   ```
   - داخل فانکشن کلید OpenAI را با `Deno.env.get("OPENAI_API_KEY")` بخوانید.
   - اگر فانکشن با JWT محافظت شده است، مقدار تولید‌شده را در `SUPABASE_FUNCTION_JWT` قرار دهید؛ در غیر این صورت خالی بماند تا همان `anon` استفاده شود.
4. **توصیهٔ امنیتی:** از نقش **Service** برای عملیات Insert/Upsert استفاده کنید و آن را فقط در سمت سرور نگه دارید؛ کلاینت صرفاً خواندن/فراخوانی فانکشن را انجام می‌دهد.

---

## 5. مدیریت کلید‌های AI داخل اپ
- کلاس `SupabaseThreatFeedService` تمام فراخوانی‌های AI را به Edge Function می‌فرستد؛ بنابراین در بیلد ریلیز مقدار `OPENAI_API_KEY` را خالی بگذارید.
- تنها برای عیب‌یابی محلی می‌توانید موقتاً کلید OpenAI را در `secrets.properties` قرار دهید؛ قبل از انتشار حذف شود.

---

## 6. امضای ریلیز (`scamynx-release-key.jks`)
1. فایل keystore را در ریشهٔ مخزن کنار `gradlew` قرار دهید (`scamynx-release-key.jks`).
2. محتویات `keystore.properties` باید مطابق زیر باشد:
   ```properties
   storeFile=../scamynx-release-key.jks
   storePassword=V7LTHRONYX2025
   keyAlias=scamynx
   keyPassword=V7LTHRONYX2025
   ```
3. برای ساخت keystore جدید (در صورت نیاز):
   ```bash
   keytool -genkeypair \
     -v -keystore scamynx-release-key.jks \
     -alias scamynx \
     -keyalg RSA -keysize 2048 \
     -validity 10000
   ```
   سپس مسیر و رمزهای جدید را در `keystore.properties` ثبت کنید.
4. هیچ‌یک از فایل‌های بالا (`.jks` و `keystore.properties`) نباید در مخزن عمومی قرار بگیرند؛ نسخهٔ امن نگه دارید.

---

## 7. اعتبارسنجی تنظیمات
1. **Build دیباگ (تست سریع):**
   ```bash
   ./gradlew :app:assembleDebug
   ```
2. **Build ریلیز امضا شده:**
   ```bash
   ./gradlew :app:assembleRelease
   ```
3. **بررسی صحت امضا:**
   ```bash
   jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
   ```
4. **اطمینان از وصل بودن Supabase:** اجرای یکی از سناریوهای اسکن تهدید؛ لاگ `SupabaseThreatFeed` در Logcat باید بدون خطای 401/403 باشد. در صورت خطا، مقادیر `SUPABASE_URL` و کلید JWT را بازبینی کنید.

---

## 8. نکات امنیتی
- فقط `secrets.defaults.properties` در ریپو باقی بماند؛ فایل سفارشی در کنترل‌ورژن نباشد.
- از Password Manager برای نگهداری رمز keystore و کلیدهای API استفاده کنید.
- کلیدهای Supabase و OpenAI را دوره‌ای چرخش دهید و دسترسی فانکشن را با سیاست‌های دقیق محدود کنید.

---

## 9. رفع خطای Android Emulator (`emulator -list-avds`)
اگر دستور بالا پیغام «Error fetching your Android emulators» برگرداند، مراحل زیر را دنبال کنید:

1. **بررسی `local.properties`:** مطمئن شوید `sdk.dir` دقیقاً به پوشهٔ SDK اشاره می‌کند (`/Users/<نام‌کاربری>/Library/Android/sdk` در macOS و `~/Android/Sdk` در لینوکس). مسیر اشتباه باعث می‌شود Gradle و Android Studio نتوانند ابزار `emulator` را پیدا کنند.
2. **تنظیم متغیرهای محیطی:** در `~/.zshrc` یا شل دلخواه خود مقدارهای زیر را اضافه و سپس `source ~/.zshrc` را اجرا کنید تا مسیر ابزارها در `PATH` قرار گیرد:
   ```zsh
   export ANDROID_SDK_ROOT=$HOME/Library/Android/sdk   # یا ~/Android/Sdk در لینوکس
   export ANDROID_HOME=$ANDROID_SDK_ROOT
   export PATH=$ANDROID_SDK_ROOT/emulator:$ANDROID_SDK_ROOT/platform-tools:$PATH
   ```
3. **نصب ابزار Emulator:** از Android Studio > SDK Manager > تب SDK Tools گزینهٔ **Android Emulator** و از تب SDK Platforms یک سیستم‌عامل حداقل API 34 را نصب/به‌روزرسانی کنید.
4. **ساخت AVD:** در Device Manager حداقل یک Virtual Device بسازید؛ در غیر این صورت `emulator -list-avds` خروجی خالی دارد.
5. **تست مجدد:** پس از انجام مراحل بالا، در ترمینال تازه دستور `emulator -list-avds` را اجرا کنید. اگر باز هم خطا گرفتید، مقدار `ANDROID_SDK_ROOT` و وجود فایل‌های باینری `emulator` در پوشهٔ SDK را بررسی کنید.

با انجام مراحل بالا، تمام بخش‌هایی که باید «اضافه» شوند (SDK path، secrets، Supabase backend و keystore) تکمیل شده و پروژه آمادهٔ ساخت و انتشار است.
