# 📦 SCAMYNX v1.0.0-beta1 Release Files

این پوشه شامل فایل‌های release برای انتشار در GitHub است.

## 📁 محتویات

### فایل‌های اصلی:
- `SCAMYNX-v1.0.0-beta1-signed.apk` (49 MB) - فایل نصب اندروید
- `SCAMYNX-v1.0.0-beta1-signed.aab` (24 MB) - App Bundle برای Play Store
- `CHECKSUMS-SHA256.txt` - Checksum های SHA256
- `RELEASE_NOTES_v1.0.0-beta1.md` - یادداشت‌های انتشار

## ✅ تأیید امضا

هر دو فایل با keystore شخصی امضا شده‌اند:
- ✅ APK: Signed & Verified
- ✅ AAB: Signed & Verified

## 🔐 Checksums

```
APK SHA256:
6bb9be847050ecbc204f7a2938598ebcfa02c48d93fa7f46d98a6ff30e40ea10

AAB SHA256:
4eabcab4f5b520055e8624bb5dc476bfdb48261173bab48e57cfb672298270dd
```

## 📤 مراحل انتشار GitHub Release

### 1. ایجاد Tag
```bash
git tag -a v1.0.0-beta1 -m "Release v1.0.0-beta1 - First Beta"
git push origin v1.0.0-beta1
```

### 2. ایجاد Release در GitHub
1. به https://github.com/v74all/SCAMYNX/releases/new بروید
2. Tag را انتخاب کنید: `v1.0.0-beta1`
3. عنوان: `v1.0.0-beta1 - First Beta Release`
4. توضیحات: محتویات `RELEASE_NOTES_v1.0.0-beta1.md` را کپی کنید
5. فایل‌های زیر را آپلود کنید:
   - `SCAMYNX-v1.0.0-beta1-signed.apk`
   - `SCAMYNX-v1.0.0-beta1-signed.aab`
   - `CHECKSUMS-SHA256.txt`
6. "This is a pre-release" را تیک بزنید
7. Publish release

---

**تاریخ:** October 18, 2025  
**توسعه‌دهنده:** Aiden (V7LTHRONYX)
