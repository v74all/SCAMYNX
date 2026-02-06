# 📋 GitHub Release Checklist

Use this checklist before publishing SCAMYNX to GitHub.

## ✅ Pre-Release Checklist

### 🔒 Security

- [x] API keys removed from `secrets.defaults.properties`
- [x] `secrets.properties` added to `.gitignore`
- [x] No hardcoded credentials in source code
- [x] No sensitive data in commit history
- [x] SECURITY.md file created
- [ ] Security scan completed (run `./gradlew dependencyCheckAnalyze`)

### 📝 Documentation

- [x] README.md updated with accurate information
- [x] LICENSE file present (MIT)
- [x] CONTRIBUTING.md guide created
- [x] RELEASE_NOTES.md for beta 1
- [x] Code comments for complex logic
- [ ] API documentation (if applicable)

### 🏗️ Code Quality

- [x] All modules compile successfully
- [x] No compiler warnings
- [ ] Lint checks pass (`./gradlew lint`)
- [ ] Unit tests pass (`./gradlew test`)
- [ ] ProGuard rules tested for release build
- [x] Code follows Kotlin conventions

### 📦 Build Configuration

- [x] Version updated to 1.0.0-beta1
- [x] Application ID correct: `com.v7lthronyx.scamynx`
- [x] Minimum SDK: 23 (Android 6.0)
- [x] Target SDK: 34 (Android 14)
- [x] Build types configured (debug/release)
- [x] ProGuard/R8 enabled for release

### 🎨 UI/UX

- [x] English strings complete
- [x] Persian/Farsi strings complete
- [x] RTL layout tested
- [x] Dark/Light themes working
- [x] Navigation flows tested
- [ ] Accessibility compliance verified

### 🧪 Testing

- [ ] Manual testing on physical device
- [ ] Tested on emulator (API 23, 30, 34)
- [ ] All scan types tested (URL, File, VPN, Instagram)
- [ ] Background processing tested
- [ ] Database migrations tested
- [ ] Network error handling tested

### 📱 APK Generation

- [ ] Debug APK builds successfully
- [ ] Release APK builds successfully
- [ ] APK size reasonable (<50MB)
- [ ] No missing resources
- [ ] Signing configured (for release)

### 🌐 GitHub Setup

- [x] Repository created on GitHub
- [x] .gitignore properly configured
- [ ] Branch protection rules set
- [x] Issue templates created
- [x] README badges added (if using CI)
- [ ] GitHub Actions workflows (optional)

## 🚀 Release Steps

### 1. Final Code Cleanup

```bash
# Remove any debug code
git grep -n "TODO\|FIXME\|XXX\|HACK"

# Check for console logs
git grep -n "println\|Log.d\|Log.v"

# Verify no secrets
git grep -n "API_KEY\|PASSWORD\|SECRET"
```

### 2. Version Bump

```bash
# Update version in app/build.gradle.kts
versionCode = 1
versionName = "1.0.0-beta1"
```

### 3. Build Release APK

```bash
# Clean build
./gradlew clean

# Build release (unsigned for testing)
./gradlew :app:assembleRelease

# APK location: app/build/outputs/apk/release/
```

### 4. Test Release Build

- [ ] Install on test device
- [ ] Verify all features work
- [ ] Check app size
- [ ] Test ProGuard obfuscation

### 5. Create Git Tag

```bash
# Create annotated tag
git tag -a v1.0.0-beta1 -m "Release version 1.0.0-beta1"

# Push tag to GitHub
git push origin v1.0.0-beta1
```

### 6. Create GitHub Release

1. Go to: https://github.com/V7LTHRONYX/scamynx-android/releases/new
2. Select tag: `v1.0.0-beta1`
3. Title: `🚀 SCAMYNX v1.0.0-beta1 (Beta Release)`
4. Copy content from `RELEASE_NOTES.md`
5. Mark as "Pre-release" ✓
6. Upload APK file
7. Publish release

### 7. Post-Release

- [ ] Announcement on social media (optional)
- [ ] Update project website (if any)
- [ ] Monitor GitHub Issues
- [ ] Respond to feedback
- [ ] Plan next release

## 🔍 Final Verification

Before clicking "Publish release":

```bash
# Verify everything is committed
git status

# Verify no secrets in recent commits
git log -p -5 | grep -i "api_key\|password\|secret"

# Verify build passes
./gradlew clean build

# Verify tests pass
./gradlew test

# Verify APK can be installed
adb install app/build/outputs/apk/release/app-release.apk
```

## ⚠️ Important Reminders

1. **Never commit** `secrets.properties` or `local.properties`
2. **Always test** the release APK before publishing
3. **Keep secrets** in secure location (password manager)
4. **Backup** signing keys securely
5. **Tag releases** in Git for version tracking
6. **Monitor** first 48 hours after release for critical bugs

## 📊 Post-Release Metrics

Track these after release:

- [ ] GitHub stars
- [ ] Downloads/installations
- [ ] Bug reports
- [ ] Feature requests
- [ ] User feedback
- [ ] Performance issues

## 🆘 Emergency Procedures

If you accidentally commit secrets:

```bash
# 1. Remove the file
git rm --cached secrets.properties

# 2. Add to .gitignore
echo "secrets.properties" >> .gitignore

# 3. Commit the change
git commit -m "Remove secrets file"

# 4. Rewrite history (use with caution!)
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch secrets.properties" \
  --prune-empty --tag-name-filter cat -- --all

# 5. Force push (WARNING: This rewrites history)
git push origin --force --all

# 6. IMMEDIATELY rotate all API keys!
```

## ✅ Checklist Complete?

Once all items are checked:

- [ ] All security checks passed
- [ ] All documentation updated
- [ ] All tests passing
- [ ] APK tested and working
- [ ] GitHub release created
- [ ] Post-release monitoring active

**Ready to release! 🎉**

---

**Last Updated:** October 18, 2025  
**Release Version:** 1.0.0-beta1
