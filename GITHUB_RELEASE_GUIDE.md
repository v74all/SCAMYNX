# 🚀 راهنمای انتشار در GitHub

## مراحل آماده‌سازی برای انتشار عمومی

### 1️⃣ اطمینان از امنیت API Keys ✅

**انجام شده:**
- ✅ API Keys واقعی از `secrets.defaults.properties` حذف شدند
- ✅ فایل `secrets.properties` به `.gitignore` اضافه شد
- ✅ PlaceHolder برای API Keys جایگزین شد
- ✅ هشدارهای امنیتی در فایل اضافه شد

### 2️⃣ به‌روزرسانی اطلاعات پروژه ✅

**انجام شده:**
- ✅ نسخه به `1.0.0-beta1` تغییر کرد
- ✅ اطلاعات شما (Aiden) در تمام فایل‌ها اضافه شد
- ✅ ذکر "AI Telemetry در آینده" در README
- ✅ License بر اساس نام شما به‌روز شد

### 3️⃣ مستندات کامل ✅

**فایل‌های ایجاد شده:**
- ✅ `README.md` - راهنمای کامل و حرفه‌ای
- ✅ `SECURITY.md` - سیاست‌های امنیتی
- ✅ `CONTRIBUTING.md` - راهنمای مشارکت
- ✅ `RELEASE_NOTES.md` - یادداشت‌های نسخه Beta 1
- ✅ `RELEASE_CHECKLIST.md` - چک‌لیست انتشار
- ✅ `.github/ISSUE_TEMPLATE/` - قالب‌های Issue
- ✅ `setup.sh` - اسکریپت نصب خودکار

---

## 📋 مراحل انتشار (گام به گام)

### مرحله 1: آماده‌سازی محلی

```bash
cd /home/aiden/Desktop/SCAMYNX

# اطمینان از پاکسازی build
./gradlew clean

# بررسی compile
./gradlew build --dry-run

# اجرای تست‌ها (اختیاری)
./gradlew test
```

### مرحله 2: راه‌اندازی Git Repository

```bash
# Initialize git
git init

# Add all files
git add .

# اولین commit
git commit -m "🎉 Initial Release - SCAMYNX v1.0.0-beta1

- Multi-target scam detection (URL, File, VPN, Instagram)
- Machine Learning powered analysis
- 6+ Threat Intelligence APIs
- Bilingual support (English/Persian)
- Clean Architecture with Jetpack Compose
- Developed by Aiden (V7LTHRONYX)
"
```

### مرحله 3: ایجاد Repository در GitHub

1. برو به: https://github.com/new
2. اطلاعات Repository:
   - **Name**: `scamynx-android`
   - **Description**: `🛡️ Advanced Scam Detection App for Android - ML-powered analysis of URLs, files, VPN configs & Instagram profiles`
   - **Visibility**: Public ✅
   - **Initialize**: ❌ (چون از قبل git داریم)

3. روی "Create repository" کلیک کن

### مرحله 4: Push به GitHub

```bash
# اضافه کردن remote
git remote add origin https://github.com/V7LTHRONYX/scamynx-android.git

# تغییر نام branch به main
git branch -M main

# Push اولیه
git push -u origin main
```

### مرحله 5: ایجاد Tag و Release

```bash
# ایجاد tag برای نسخه beta
git tag -a v1.0.0-beta1 -m "Release v1.0.0-beta1 - First Public Beta"

# Push tag به GitHub
git push origin v1.0.0-beta1
```

### مرحله 6: ایجاد GitHub Release

1. برو به: https://github.com/V7LTHRONYX/scamynx-android/releases/new
2. انتخاب tag: `v1.0.0-beta1`
3. عنوان: `🚀 SCAMYNX v1.0.0-beta1 - First Public Beta`
4. توضیحات را از `RELEASE_NOTES.md` کپی کن
5. تیک "This is a pre-release" را بزن ✅
6. اگر APK داری، آپلود کن
7. روی "Publish release" کلیک کن

### مرحله 7: تنظیمات Repository

#### Topics (برای جستجو بهتر):
```
android, kotlin, scam-detection, phishing-detection, 
security, machine-learning, threat-intelligence,
jetpack-compose, clean-architecture, vpn-scanner
```

#### About Section:
```
🛡️ Advanced Scam Detection for Android
```

#### Social Preview Image:
- اگر لوگو داری، آپلود کن در Settings > Social Preview

---

## 🔒 نکات امنیتی مهم

### ✅ چیزهایی که انجام شد:

1. **محافظت از API Keys:**
   ```
   secrets.defaults.properties → placeholder keys
   .gitignore → secrets.properties اضافه شد
   ```

2. **راهنماهای امنیتی:**
   - SECURITY.md برای گزارش آسیب‌پذیری
   - Pre-commit hook برای جلوگیری از commit کلیدها
   - هشدارها در README

3. **License:**
   - MIT License با نام شما
   - Copyright 2025 Aiden (V7LTHRONYX)

### ⚠️ چک‌لیست امنیتی نهایی:

```bash
# بررسی عدم وجود API Keys در تاریخچه
git log -p | grep -i "AIzaSy\|sk_\|pk_"

# بررسی فایل‌های ignore شده
cat .gitignore | grep secrets

# اطمینان از عدم وجود secrets.properties
git ls-files | grep secrets.properties
# باید خروجی خالی باشد!
```

---

## 🎯 بعد از انتشار

### 1. Repository Settings

**Settings > General:**
- ✅ Enable Issues
- ✅ Enable Discussions  
- ✅ Enable Projects (اختیاری)
- ✅ Enable Wiki (اختیاری)

**Settings > Branches:**
```
Protected Branch Rules for 'main':
- Require pull request reviews
- Require status checks to pass
```

### 2. README Badges

به README اضافه کن:

```markdown
![Version](https://img.shields.io/badge/version-1.0.0--beta1-blue)
![License](https://img.shields.io/badge/license-MIT-green)
![Platform](https://img.shields.io/badge/platform-Android-brightgreen)
![Min SDK](https://img.shields.io/badge/min%20sdk-23-orange)
![Target SDK](https://img.shields.io/badge/target%20sdk-34-orange)
```

### 3. اطلاع‌رسانی

اگر می‌خواهی اعلام کنی:
- Twitter/X
- LinkedIn
- Reddit (r/androiddev)
- Dev.to
- Telegram channels

نمونه پست:
```
🚀 Excited to announce SCAMYNX v1.0.0-beta1!

An open-source Android app for detecting scams:
✅ URL phishing detection
✅ File malware analysis
✅ VPN config validation
✅ Instagram scam profiles

Built with Kotlin, Jetpack Compose, and ML 🧠

Check it out: https://github.com/V7LTHRONYX/scamynx-android

#Android #Security #OpenSource
```

---

## 📊 نظارت بعد از انتشار

### اولویت‌های 48 ساعت اول:

1. **پاسخ به Issues:**
   - هر issue را ظرف 24 ساعت تایید کن
   - به سوالات پاسخ بده

2. **بررسی Stars:**
   - اگر کسی star زد، تشکر کن

3. **مانیتور کردن Discussions:**
   - به سوالات پاسخ بده
   - فیدبک جمع کن

4. **بررسی Security:**
   - چک کن کسی API key commit نکرده
   - نگاه کن Fork ها امن هستند

---

## 🆘 عیب‌یابی رایج

### مشکل: "Permission denied" هنگام push

```bash
# از SSH استفاده کن
git remote set-url origin git@github.com:V7LTHRONYX/scamynx-android.git

# یا Personal Access Token بساز
# Settings > Developer settings > Personal access tokens
```

### مشکل: فایل بزرگ نمی‌تواند push شود

```bash
# فایل‌های بزرگ را ignore کن
echo "*.apk" >> .gitignore
echo "*.aab" >> .gitignore
git add .gitignore
git commit -m "Ignore large files"
```

### مشکل: اشتباهاً secret را commit کردی

```bash
# 1. فایل را حذف کن
git rm --cached secrets.properties

# 2. Commit کن
git commit -m "Remove secrets file"

# 3. Force push (احتیاط!)
git push --force

# 4. API keys را بلافاصله تغییر بده!
```

---

## ✅ چک‌لیست نهایی قبل از انتشار

- [x] API Keys حذف شدند
- [x] secrets.properties در .gitignore است
- [x] نسخه به 1.0.0-beta1 تغییر کرد
- [x] README کامل است
- [x] SECURITY.md موجود است
- [x] CONTRIBUTING.md موجود است
- [x] LICENSE درست است
- [x] Issue templates آماده است
- [ ] Git repository initialize شده
- [ ] اولین commit ساخته شده
- [ ] Remote به GitHub اضافه شده
- [ ] Push به GitHub انجام شده
- [ ] Tag ساخته شده
- [ ] Release منتشر شده

---

## 🎉 آماده برای انتشار!

همه چیز آماده است! فقط مراحل بالا را دنبال کن.

**موفق باشی! 🚀**

---

**تهیه شده برای:** Aiden (V7LTHRONYX)  
**تاریخ:** 18 اکتبر 2025  
**نسخه:** 1.0.0-beta1
