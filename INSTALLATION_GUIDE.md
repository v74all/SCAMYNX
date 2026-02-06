# 🔧 حل مشکل نصب APK و خطای Google Play Protect

## ⚠️ مشکل: Google Play Protect Warning

وقتی APK امضا شده با keystore شخصی رو نصب می‌کنید، Google Play Protect هشدار می‌ده:
```
"This app was built for an older version of Android"
یا
"App not verified by Google Play Protect"
```

این **کاملاً طبیعی** است و به دلیل اینکه:
- APK با keystore شخصی امضا شده (نه Google Play)
- هنوز در Google Play Store منتشر نشده
- Google این developer/keystore رو نمی‌شناسه

---

## ✅ راه‌حل‌ها

### راه‌حل 1: نادیده گرفتن هشدار (امن‌ترین برای شما)

چون خودتون developer هستید:

1. **روی دستگاه:**
   - Settings → Security → Play Protect
   - "Scan apps with Play Protect" رو خاموش کنید (موقت)

2. **حین نصب:**
   - وقتی هشدار اومد، روی "Install anyway" یا "More details" بزنید
   - سپس "Install anyway" را انتخاب کنید

3. **بعد از نصب:**
   - Play Protect رو دوباره فعال کنید
   - برنامه شما نصب شده و کار می‌کنه

### راه‌حل 2: غیرفعال کردن موقت Play Protect

```
Settings → Google → Security → Google Play Protect → 
[⚙️] → "Scan apps with Play Protect" → OFF
```

بعد از نصب، دوباره فعالش کنید.

### راه‌حل 3: استفاده از ADB (برای Developer)

```bash
# نصب مستقیم بدون هشدار
adb install -r SCAMYNX-v1.0.0-beta1-signed.apk

# اگر قبلاً نصب شده بود
adb uninstall com.v7lthronyx.scamynx
adb install SCAMYNX-v1.0.0-beta1-signed.apk
```

### راه‌حل 4: Build کردن Debug نسخه (برای تست)

```bash
cd /home/aiden/Desktop/SCAMYNX
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Debug نسخه با debug keystore امضا میشه که Android بهتر می‌شناسه.

---

## 🔐 برای انتشار عمومی (Production)

### گزینه A: Google Play Store (توصیه می‌شود)

1. **ثبت در Play Console:**
   - https://play.google.com/console/signup
   - هزینه یکبار: $25

2. **آپلود AAB:**
   - Play Console خودش APK امضا می‌کنه
   - Google Play signing رو فعال کنید
   - کاربران هیچ هشداری نمی‌بینن

3. **مزایا:**
   - ✅ بدون هشدار Play Protect
   - ✅ به‌روزرسانی خودکار
   - ✅ دسترسی به 2 میلیارد+ کاربر
   - ✅ آمار و تحلیل

### گزینه B: توزیع خارج از Play Store

اگر می‌خواید خودتون توزیع کنید:

1. **F-Droid:**
   - مخزن Open Source
   - برای اپ‌های FOSS
   - https://f-droid.org/

2. **GitHub Releases:**
   - توزیع رایگان
   - برای developer ها و beta tester ها
   - کاربران باید هشدار رو قبول کنن

3. **وب‌سایت شخصی:**
   - APK رو روی سرور خودتون
   - لینک مستقیم دانلود
   - کاربران باید "Unknown Sources" فعال کنن

---

## 📱 دستورالعمل برای کاربران Beta

وقتی APK رو توزیع می‌کنید، این راهنما رو به کاربران بدید:

### برای نصب SCAMYNX Beta:

**مرحله 1: فعال کردن نصب از منابع نامشخص**
```
Settings → Security → Install unknown apps → 
[Browser یا File Manager شما] → Allow from this source
```

**مرحله 2: دانلود APK**
- فایل `SCAMYNX-v1.0.0-beta1-signed.apk` را دانلود کنید

**مرحله 3: نصب**
- روی فایل APK کلیک کنید
- اگر Google Play Protect هشدار داد:
  - روی "More details" بزنید
  - سپس "Install anyway" انتخاب کنید

**مرحله 4: تأیید امنیت (اختیاری)**
- SHA256 Checksum را با مقدار زیر مقایسه کنید:
```
6bb9be847050ecbc204f7a2938598ebcfa02c48d93fa7f46d98a6ff30e40ea10
```

---

## 🔍 تأیید امضا APK

برای اطمینان از اینکه APK دستکاری نشده:

### روی کامپیوتر:

**Windows:**
```powershell
certutil -hashfile SCAMYNX-v1.0.0-beta1-signed.apk SHA256
```

**Linux/Mac:**
```bash
sha256sum SCAMYNX-v1.0.0-beta1-signed.apk
```

**مقدار صحیح:**
```
6bb9be847050ecbc204f7a2938598ebcfa02c48d93fa7f46d98a6ff30e40ea10
```

### بررسی امضا:
```bash
jarsigner -verify -verbose -certs SCAMYNX-v1.0.0-beta1-signed.apk
```

باید ببینید:
```
CN=Aiden, OU=V7LTHRONYX, O=V7LTHRONYX, L=Tehran, C=IR
jar verified.
```

---

## 🚀 برای انتشار Production

### مراحل توصیه‌شده:

**1. Alpha/Beta Testing داخلی (فعلی)**
- ✅ توزیع APK به دوستان و تستر ها
- ✅ بازخورد و bug fix
- ✅ بدون نیاز به Play Store

**2. Open Beta در Play Store**
- آپلود AAB به Play Console
- تنظیم "Closed Beta" یا "Open Beta"
- لینک دعوت برای تسترها
- Google Play signing فعال میشه

**3. Production Release**
- بعد از تست کافی
- انتشار عمومی در Play Store
- همه کاربران بدون هشدار دانلود می‌کنن

---

## 📋 Checklist قبل از Production

قبل از انتشار نهایی:

### امنیت و کیفیت:
- [ ] تست روی 5+ دستگاه مختلف
- [ ] تست اندروید 6 تا 14
- [ ] بررسی crash reports
- [ ] تست با API keys واقعی
- [ ] بررسی permissions
- [ ] تست upgrade از نسخه قبلی

### Play Store:
- [ ] Screenshots (8 تا) برای همه سایزها
- [ ] Icon 512x512
- [ ] Feature Graphic 1024x500
- [ ] توضیحات (EN + FA)
- [ ] Privacy Policy
- [ ] Content Rating پر شده
- [ ] Store Listing کامل

### مستندات:
- [ ] README به‌روز
- [ ] CHANGELOG کامل
- [ ] راهنمای استفاده
- [ ] FAQ

---

## 💡 نکات مهم

### برای Beta Testing:
✅ **خطای Play Protect طبیعی است** - نگران نباشید  
✅ **APK شما امضا شده و امن است**  
✅ **فقط از منابع معتبر دانلود کنید** (GitHub Release شما)  

### برای Production:
⚠️ **حتماً از Google Play استفاده کنید**  
⚠️ **توزیع خارج از Play محدودیت دارد**  
⚠️ **کاربران عادی هشدارها رو جدی می‌گیرن**  

---

## 🔗 منابع مفید

- [Google Play Console](https://play.google.com/console/)
- [App Signing by Google Play](https://support.google.com/googleplay/android-developer/answer/9842756)
- [Distribute Outside Play Store](https://developer.android.com/studio/publish#unknown-sources)
- [F-Droid Submission](https://f-droid.org/docs/Inclusion_Policy/)

---

## 📞 سوالات متداول

**Q: آیا این APK امن است؟**  
A: بله! خودتون ساختید و امضا کردید. فقط از Play Protect جدید نیست.

**Q: چرا Google هشدار می‌ده؟**  
A: چون با keystore شخصی امضا شده، نه Google Play signing.

**Q: آیا می‌تونم توی Play Store بذارم؟**  
A: بله! AAB رو آپلود کنید و Google خودش امضا می‌کنه.

**Q: کاربران چطور نصب کنن؟**  
A: باید "Install anyway" بزنن. یا منتظر انتشار Play Store بمونن.

**Q: میشه این هشدار رو حذف کرد؟**  
A: فقط با انتشار در Play Store یا استفاده از debug build.

---

**تاریخ:** February 6, 2026  
**نسخه:** v1.0.0-beta1  
**Developer:** Aiden (V7LTHRONYX)
