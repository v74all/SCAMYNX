# 🔐 SCAMYNX Release Signing Key

## ⚠️ اطلاعات محرمانه - این فایل را به اشتراک نگذارید!

### اطلاعات Keystore

**Keystore File:** `scamynx-release-key.jks`
**Key Alias:** `scamynx`
**Store Password:** `V7LTHRONYX2025`
**Key Password:** `V7LTHRONYX2025`

### اطلاعات گواهی (Certificate)

```
Distinguished Name (DN):
  CN (Common Name): Aiden
  OU (Organizational Unit): V7LTHRONYX
  O (Organization): V7LTHRONYX
  L (Locality): Tehran
  ST (State): Tehran
  C (Country): IR

Algorithm: RSA
Key Size: 2048 bits
Signature Algorithm: SHA384withRSA
Validity: 10,000 days (حدود 27 سال)
Created: October 18, 2025
Expires: March 5, 2053
```

### محل فایل‌ها

```
SCAMYNX/
├── scamynx-release-key.jks          # Keystore اصلی
├── keystore.properties              # تنظیمات keystore
└── app/build/outputs/
    ├── apk/release/
    │   └── app-release.apk          # APK امضا شده (49 MB)
    └── bundle/release/
        └── app-release.aab          # App Bundle امضا شده (24 MB)
```

## 📱 فایل‌های Release

### APK (Android Package)
- **مسیر:** `app/build/outputs/apk/release/app-release.apk`
- **حجم:** ~49 MB
- **استفاده:** نصب مستقیم روی دستگاه
- **امضا:** ✅ با keystore امضا شده

### AAB (Android App Bundle)
- **مسیر:** `app/build/outputs/bundle/release/app-release.aab`
- **حجم:** ~24 MB
- **استفاده:** آپلود به Google Play Store
- **امضا:** ✅ با keystore امضا شده

## 🔧 دستورات Build

### ساخت APK امضا شده
```bash
./gradlew :app:assembleRelease
```

### ساخت AAB امضا شده
```bash
./gradlew :app:bundleRelease
```

### پاک کردن build های قبلی
```bash
./gradlew clean
```

## ✅ تأیید امضا

### بررسی امضای APK
```bash
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
```

### مشاهده اطلاعات گواهی
```bash
keytool -list -v -keystore scamynx-release-key.jks -alias scamynx
```
Password: `V7LTHRONYX2025`

### نمایش fingerprint
```bash
keytool -list -v -keystore scamynx-release-key.jks -alias scamynx | grep -A 5 "Certificate fingerprints"
```

## 🚀 انتشار در Google Play

### مراحل آپلود

1. **ورود به Google Play Console**
   - https://play.google.com/console

2. **ایجاد Application جدید**
   - نام: SCAMYNX
   - Package: com.v7lthronyx.scamynx

3. **آپلود AAB**
   - فایل: `app-release.aab`
   - به بخش Release → Production بروید
   - Create new release

4. **تکمیل اطلاعات**
   - Store Listing
   - Screenshots
   - Privacy Policy
   - Content Rating

## 📝 یادداشت‌های مهم

### ⚠️ امنیت Keystore

1. **هرگز keystore را commit نکنید!**
   - فایل در `.gitignore` قرار دارد
   - `keystore.properties` نیز ignore شده

2. **پشتیبان‌گیری**
   - از keystore در محل امن نسخه پشتیبان بگیرید
   - بدون keystore نمی‌توانید اپلیکیشن را به‌روزرسانی کنید!

3. **رمزهای عبور**
   - رمزها را در جای امن نگهداری کنید
   - از password manager استفاده کنید

### 📋 Checklist قبل از Release

- [ ] تست کامل روی دستگاه‌های مختلف
- [ ] بررسی ProGuard/R8 rules
- [ ] تست ProGuard با build release
- [ ] بررسی size و performance
- [ ] تست permissions و Runtime permissions
- [ ] بررسی crash reports
- [ ] تست deep links و share targets
- [ ] بررسی localization (EN/FA)
- [ ] تست Dark/Light theme
- [ ] تأیید API keys در production

### 🔄 به‌روزرسانی Version

قبل از هر release جدید در `app/build.gradle.kts`:

```kotlin
versionCode = 2        // افزایش دهید
versionName = "1.0.1"  // تغییر دهید
```

### 📊 نسخه‌بندی Semantic

```
Major.Minor.Patch[-Suffix]

1.0.0-beta1  → Beta اول
1.0.0        → Release اول
1.0.1        → Bug fixes
1.1.0        → Features جدید
2.0.0        → Breaking changes
```

## 🛠️ Build Types

### Debug
```bash
./gradlew :app:assembleDebug
```
- Package: `com.v7lthronyx.scamynx.debug`
- Debuggable: Yes
- Minification: No

### Release
```bash
./gradlew :app:assembleRelease
```
- Package: `com.v7lthronyx.scamynx`
- Debuggable: No
- Minification: Yes (ProGuard/R8)
- Signed: Yes

## 📞 در صورت مشکل

اگر keystore را گم کردید:
- ⚠️ **نمی‌توانید اپلیکیشن موجود را به‌روزرسانی کنید**
- باید package name عوض شود
- یا اپلیکیشن جدیدی در Play Store ثبت شود

اگر رمز عبور را فراموش کردید:
- ⚠️ **هیچ راهی برای بازیابی وجود ندارد**
- باید keystore جدید ساخته شود

## 🔒 محل امن برای نگهداری

پیشنهادات:
- Password Manager (1Password, LastPass, Bitwarden)
- Encrypted USB Drive
- Cloud Storage رمزگذاری شده (Cryptomator + Dropbox)
- Hardware Security Module (HSM) برای production

---

**تاریخ ایجاد:** October 18, 2025  
**سازنده:** Aiden (V7LTHRONYX)  
**GitHub:** https://github.com/v74all/SCAMYNX

⚠️ **این فایل حاوی اطلاعات حساس است - به اشتراک نگذارید!**
