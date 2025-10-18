# 📱 SCAMYNX Release Build Summary

**تاریخ Build:** October 18, 2025  
**توسعه‌دهنده:** Aiden (V7LTHRONYX)  
**Version:** 1.0.0-beta1 (versionCode: 1)

---

## ✅ فایل‌های ساخته شده

### 1. APK امضا شده (Release)
```
📦 File: app-release.apk
📍 Path: app/build/outputs/apk/release/app-release.apk
💾 Size: 49 MB
🔐 Signed: ✅ با keystore شخصی
📱 Use: نصب مستقیم روی دستگاه اندروید
```

### 2. AAB امضا شده (Release)
```
📦 File: app-release.aab
📍 Path: app/build/outputs/bundle/release/app-release.aab
💾 Size: 24 MB
🔐 Signed: ✅ با keystore شخصی
📱 Use: آپلود به Google Play Store
```

### 3. APK دیباگ (برای تست)
```
📦 File: app-debug.apk
📍 Path: app/build/outputs/apk/debug/app-debug.apk
💾 Size: 68 MB
🔐 Signed: ✅ با debug keystore
📱 Use: فقط برای تست
```

---

## 🔐 اطلاعات Keystore

### فایل‌های امنیتی ایجاد شده:
- ✅ `scamynx-release-key.jks` - Keystore اصلی
- ✅ `keystore.properties` - تنظیمات (در .gitignore)
- ✅ `keystore-certificate-info.txt` - اطلاعات گواهی

### اطلاعات گواهی:

**Owner/Issuer:**
```
CN (Common Name):         Aiden
OU (Organizational Unit): V7LTHRONYX
O (Organization):         V7LTHRONYX
L (Locality):            Tehran
ST (State):              Tehran
C (Country):             IR
```

**تنظیمات امنیتی:**
```
Alias:              scamynx
Algorithm:          RSA
Key Size:           2048 bits
Signature:          SHA384withRSA
Serial Number:      8ff98bd45b4798f4
```

**اعتبار:**
```
Created:   October 18, 2025 (14:37:54 IRST)
Expires:   March 5, 2053 (14:37:54 IRST)
Validity:  10,000 days (~27 سال)
```

### Certificate Fingerprints:

**SHA1:**
```
87:15:1B:73:1E:A4:AC:0B:D0:D5:41:E3:00:7C:FF:87:DB:F4:A4:9E
```

**SHA256:**
```
5F:06:58:3C:5B:D0:89:60:B1:13:5E:62:2F:C2:FE:49:AA:E6:F9:7F:32:97:82:02:90:19:8D:BA:10:E5:BB:AE
```

> 💡 **نکته:** این fingerprint ها برای Firebase، Google Maps API و سرویس‌های دیگر نیاز خواهد بود.

---

## 🚀 نحوه استفاده

### نصب APK روی دستگاه

**روش 1: از طریق ADB**
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

**روش 2: کپی فایل**
1. فایل APK را به گوشی کپی کنید
2. از File Manager باز کنید
3. "Install unknown apps" را مجاز کنید
4. نصب کنید

### آپلود به Google Play

**مراحل:**
1. وارد [Google Play Console](https://play.google.com/console) شوید
2. Create app → اطلاعات برنامه را وارد کنید
3. Release → Production → Create new release
4. فایل `app-release.aab` را آپلود کنید
5. Release notes اضافه کنید
6. Review and rollout

### تست قبل از انتشار

```bash
# نصب روی دستگاه متصل
adb install -r app/build/outputs/apk/release/app-release.apk

# مشاهده لاگ‌ها
adb logcat | grep "SCAMYNX"

# حذف برنامه
adb uninstall com.v7lthronyx.scamynx
```

---

## 📊 Build Configuration

### ProGuard/R8
- ✅ Enabled (Minification)
- ✅ Resource Shrinking
- 📁 Mapping file: `app/build/outputs/mapping/release/mapping.txt`

### تفاوت حجم:
```
Debug:   68 MB (بدون optimization)
Release: 49 MB (با ProGuard/R8)
AAB:     24 MB (فشرده‌تر برای Play Store)

کاهش حجم: ~28% (19 MB کمتر)
```

---

## ⚠️ نکات امنیتی مهم

### 🔒 فایل‌های محرمانه (هرگز commit نکنید!)
- ❌ `scamynx-release-key.jks`
- ❌ `keystore.properties`
- ❌ `secrets.properties`
- ❌ `local.properties`
- ❌ `*.apk` و `*.aab`

### ✅ همه در `.gitignore` قرار دارند

### 💾 پشتیبان‌گیری ضروری
```
⚠️ بدون keystore نمی‌توانید برنامه را در Play Store به‌روزرسانی کنید!

پیشنهاد:
1. کپی روی USB Drive رمزگذاری شده
2. ذخیره در Password Manager
3. Backup در Cloud Storage امن
4. چاپ اطلاعات در محل امن
```

---

## 🔄 به‌روزرسانی نسخه‌های بعدی

قبل از build نسخه جدید:

**1. افزایش Version در `app/build.gradle.kts`:**
```kotlin
versionCode = 2          // +1 کنید
versionName = "1.0.1"    // تغییر دهید
```

**2. Clean و Build:**
```bash
./gradlew clean
./gradlew :app:bundleRelease
```

**3. Test کامل:**
- تست روی دستگاه‌های مختلف
- بررسی upgrade از نسخه قبلی
- تست migration دیتابیس

---

## 📝 Release Notes برای Google Play

```markdown
## نسخه 1.0.0-beta1 (اولین انتشار)

### ویژگی‌ها:
✅ تشخیص فیشینگ و لینک‌های مخرب
✅ اسکن فایل‌های APK و اجرایی
✅ تحلیل کانفیگ VPN
✅ بررسی پروفایل اینستاگرام
✅ 6+ API هوش تهدید
✅ یادگیری ماشین روی دستگاه
✅ پشتیبانی کامل فارسی/انگلیسی
✅ تم تیره و روشن

### امنیت و حریم خصوصی:
🔒 بدون جمع‌آوری داده
🔒 پردازش کامل محلی
🔒 بدون تبلیغات

### پلتفرم:
📱 Android 6.0+ (API 23+)
```

---

## 🔗 لینک‌ها

- **GitHub:** https://github.com/v74all/SCAMYNX
- **Website:** [به زودی]
- **Support:** [به زودی]

---

## ✅ Checklist انتشار

قبل از Release:
- [x] Build موفق (Release)
- [x] Keystore ایجاد و امضا شده
- [x] APK تست شده روی دستگاه
- [x] ProGuard rules بررسی شده
- [ ] Test روی اندروید نسخه‌های مختلف
- [ ] بررسی Permissions
- [ ] تست با API keys واقعی
- [ ] Screenshots برای Play Store
- [ ] Privacy Policy نوشته شده
- [ ] Store Listing تکمیل شده

---

**📅 Build Date:** October 18, 2025  
**👤 Developer:** Aiden (V7LTHRONYX)  
**🔐 Signed:** ✅ با keystore شخصی  
**✨ Status:** Ready for testing

---

## 🎉 خلاصه

✅ **Keystore ایجاد شد** - اعتبار 27 سال  
✅ **APK امضا شد** - 49 MB  
✅ **AAB ساخته شد** - 24 MB  
✅ **فایل‌های محرمانه محافظت شدند** - در .gitignore  
✅ **مستندات کامل** - KEYSTORE_INFO.md  

**برنامه آماده نصب و تست است! 🚀**
