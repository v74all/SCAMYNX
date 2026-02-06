# 🚀 SCAMYNX Beta Launch Playbook

> **Purpose:** Comprehensive guide for finishing the current refactor, validating it, and shipping a polished beta release.

---

## 📋 Table of Contents

1. [Immediate Pre-Debug Tasks](#1-immediate-pre-debug-tasks)
2. [Debug & Quality Assurance](#2-debug--quality-assurance)
3. [Pre-Beta Polish](#3-pre-beta-polish)
4. [Beta Release Checklist](#4-beta-release-checklist)
5. [Post-Release Monitoring](#5-post-release-monitoring)
6. [Troubleshooting](#6-troubleshooting)
7. [Future Enhancements](#7-future-enhancements)

---

## 1. Immediate Pre-Debug Tasks

### ✅ Completed

- [x] **Restore missing DTO usages** ✅  
  - ✅ `VirusTotalSubmitRequestDto` properly imported and used in `ScanRepositoryImpl.kt` (line 24, 426)
  - ✅ `UrlScanResultDto` properly imported and used in `ScanRepositoryImpl.kt` (line 23, 573)
  - ✅ `pow()` properly imported as `kotlin.math.pow` and used correctly (line 70, 578)

- [x] **Re-run Gradle sync + type check** ✅  
  All DTO wiring is fixed. Ready for compilation check.

### 🔄 Pending

- [ ] **Verify new UI strings**  
  - [ ] Review strings in `app/src/main/res/values/strings.xml` (English)
  - [ ] Review strings in `app/src/main/res/values-fa/strings.xml` (Persian/Farsi)
  - [ ] Test RTL layout rendering for Persian strings
  - [ ] Verify all string resources are properly referenced (no missing keys)
  - [ ] Check string length for UI overflow (especially in buttons/cards)

---

## 2. Debug & Quality Assurance

### 2.1 Build Verification

#### Kotlin Compilation
```bash
# Clean build
./gradlew clean

# Compile all modules
./gradlew :app:compileDebugKotlin
./gradlew :data:compileDebugKotlin
./gradlew :domain:compileDebugKotlin
./gradlew :common:compileDebugKotlin
./gradlew :ml:compileDebugKotlin
./gradlew :networksecurity:compileDebugKotlin

# Full build check
./gradlew :app:assembleDebug
```

**Expected:** All modules compile without errors or warnings.

#### Static Analysis & Linting
```bash
# Run lint checks
./gradlew :app:lintDebug

# Generate lint report
./gradlew :app:lintDebug --html-output=lint-report.html
```

**Check for:**
- Accessibility issues (missing content descriptions, low contrast)
- UI styling regressions
- Unused resources
- Performance warnings
- Security vulnerabilities

**Fix priority:**
- 🔴 Critical: Security issues, crashes
- 🟡 High: Accessibility violations, performance regressions
- 🟢 Low: Code style, unused resources

### 2.2 Testing Suite

#### Unit Tests
```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run specific module tests
./gradlew :data:testDebugUnitTest
./gradlew :domain:testDebugUnitTest
./gradlew :ml:testDebugUnitTest
```

**Coverage targets:**
- Repository layer: >80%
- Domain use cases: >75%
- Risk scoring logic: >90%

#### Instrumentation Tests
```bash
# Run on connected device/emulator
./gradlew connectedDebugAndroidTest

# Run specific test class
./gradlew connectedDebugAndroidTest --tests "*.AccessibilityAuditTest"
```

**Critical test scenarios:**
- [ ] URL scan flow (VirusTotal + UrlScan integration)
- [ ] File scan flow (hash verification, keyword detection)
- [ ] VPN config parsing (VMESS, VLESS, Trojan, Shadowsocks)
- [ ] Instagram profile analysis
- [ ] Risk score calculation and display
- [ ] History navigation and persistence
- [ ] Language switching (EN ↔ FA)
- [ ] Theme switching (Light ↔ Dark)

### 2.3 Manual Testing Matrix

#### Device Testing
Test on at least **2 devices** with different configurations:

| Device | Android Version | Screen Size | Language | Theme | Status |
|--------|----------------|-------------|-----------|-------|--------|
| Device 1 | 8.0+ (API 26) | Small/Medium | English | Light | ⬜ |
| Device 2 | 12+ (API 31) | Large | Persian | Dark | ⬜ |

#### UI Validation Checklist

**Home Screen:**
- [ ] Hero gradient renders correctly (light & dark themes)
- [ ] Scan mode cards display properly
- [ ] Feature highlights section is visible
- [ ] Quick actions card is functional
- [ ] Password security card works
- [ ] Social engineering card works
- [ ] Navigation to Settings/History/About works

**Scan Results Screen:**
- [ ] Risk meter/linear progress renders for each tier:
  - [ ] Safe (< 0.3) - Green
  - [ ] Low Risk (0.3-0.5) - Yellow
  - [ ] Medium Risk (0.5-0.7) - Orange
  - [ ] High Risk (0.7-0.9) - Red
  - [ ] Critical (≥ 0.9) - Dark Red
- [ ] Results cards display threat intelligence data
- [ ] ML analysis results are shown (if applicable)
- [ ] Network security findings are displayed
- [ ] Share/Export functionality works

**Navigation & Flow:**
- [ ] Deep linking works (if implemented)
- [ ] Back navigation preserves state
- [ ] History screen shows past scans
- [ ] Settings screen is accessible
- [ ] About screen displays correct version info

**Accessibility:**
- [ ] TalkBack navigation works
- [ ] All interactive elements have content descriptions
- [ ] Color contrast meets WCAG AA standards
- [ ] Text scaling works (system font size changes)

> 💡 **Pro Tip:** Keep Android Studio Layout Inspector open to check runtime colors & typography against the design palette defined in `docs/THEME_IMPROVEMENTS.md`.

### 2.4 API Integration Testing

**Test each scanner with real inputs:**

- [ ] **URL Scanner:**
  - [ ] Test with known phishing URL
  - [ ] Test with safe URL (e.g., `https://google.com`)
  - [ ] Test with invalid URL format
  - [ ] Verify VirusTotal API response handling
  - [ ] Verify UrlScan API response handling
  - [ ] Test network timeout scenarios

- [ ] **File Scanner:**
  - [ ] Test with APK file
  - [ ] Test with suspicious file (if available)
  - [ ] Test with large file (>10MB)
  - [ ] Verify SHA256 hash calculation
  - [ ] Test file permission handling

- [ ] **VPN Config Scanner:**
  - [ ] Test VMESS config parsing
  - [ ] Test VLESS config parsing
  - [ ] Test Trojan config parsing
  - [ ] Test Shadowsocks config parsing
  - [ ] Test invalid config handling

- [ ] **Instagram Scanner:**
  - [ ] Test with real profile handle
  - [ ] Test with invalid handle
  - [ ] Verify API rate limiting handling

---

## 3. Pre-Beta Polish

### 3.1 Documentation Updates

- [ ] **Release Notes:**
  - [ ] Update `RELEASE_NOTES.md` (English)
  - [ ] Create/update Persian release notes
  - [ ] Include: hero redesign, results risk meter, feature highlights
  - [ ] List known issues (if any)
  - [ ] Add migration notes (if upgrading from previous version)

- [ ] **Theme Documentation:**
  - [ ] Review `docs/THEME_IMPROVEMENTS.md` for accuracy
  - [ ] Update `docs/THEME_QUICK_REFERENCE.md` with gradient system
  - [ ] Document new spacing tokens and shape definitions

- [ ] **README Updates:**
  - [ ] Update version number in `README.md` and `README_FA.md`
  - [ ] Add screenshots of new UI (if available)
  - [ ] Update feature list if new features were added

### 3.2 Visual Assets

- [ ] **Screenshots:**
  - [ ] Home screen (light theme)
  - [ ] Home screen (dark theme)
  - [ ] Scan results screen (light theme)
  - [ ] Scan results screen (dark theme)
  - [ ] Settings screen
  - [ ] History screen
  - [ ] Screenshots should showcase:
    - Hero gradient
    - Risk meter visualization
    - Feature cards
    - Material 3 design elements

- [ ] **Play Store Assets:**
  - [ ] Feature graphic (1024x500)
  - [ ] Icon (512x512)
  - [ ] Promotional screenshots (if applicable)

### 3.3 Code Quality

- [ ] **Code Review:**
  - [ ] Remove debug logs
  - [ ] Remove commented-out code
  - [ ] Verify ProGuard rules are correct (`app/proguard-rules.pro`)
  - [ ] Check for hardcoded strings (should use string resources)
  - [ ] Verify API keys are properly secured (not in code)

- [ ] **Performance:**
  - [ ] Run Android Profiler for memory leaks
  - [ ] Check for unnecessary recompositions
  - [ ] Verify image loading is optimized
  - [ ] Test app startup time

---

## 4. Beta Release Checklist

### 4.1 Version Management

- [ ] **Update Version Information:**
  ```kotlin
  // In app/build.gradle.kts
  versionCode = X  // Increment by 1
  versionName = "1.0.0-betaX"  // Update version string
  ```

- [ ] **Git Tagging:**
  ```bash
  git tag -a beta/v1.0.0-betaX -m "Beta release v1.0.0-betaX"
  git push origin beta/v1.0.0-betaX
  ```

- [ ] **Create Release Branch (if needed):**
  ```bash
  git checkout -b release/beta-v1.0.0-betaX
  git push origin release/beta-v1.0.0-betaX
  ```

### 4.2 Build Artifacts

#### Generate Release Bundle
```bash
# Clean previous builds
./gradlew clean

# Generate signed AAB
./gradlew :app:bundleRelease

# Output location: app/build/outputs/bundle/release/app-release.aab
```

**Before building:**
- [ ] Verify `keystore.properties` exists and is correct
- [ ] Confirm keystore file (`scamynx-release-key.jks`) is accessible
- [ ] Test keystore password (don't wait until build fails)

#### Generate Release APK (for direct distribution)
```bash
# Generate signed APK
./gradlew :app:assembleRelease

# Output location: app/build/outputs/apk/release/app-release.apk
```

**Verify artifacts:**
- [ ] AAB file size is reasonable (<100MB recommended)
- [ ] APK file size is reasonable (<50MB recommended)
- [ ] Both files are signed (check with `jarsigner -verify`)

### 4.3 Final Validation

- [ ] **Install Release Build Locally:**
  ```bash
  adb install -r app/build/outputs/apk/release/app-release.apk
  ```

- [ ] **Smoke Test on Release Build:**
  - [ ] App launches without crashes
  - [ ] URL scan completes successfully
  - [ ] File scan works
  - [ ] History navigation works
  - [ ] Settings are accessible
  - [ ] Language switching works
  - [ ] Theme switching works

- [ ] **Verify ProGuard/R8:**
  - [ ] App doesn't crash due to obfuscation
  - [ ] API calls still work
  - [ ] Database operations function correctly

### 4.4 Play Console Deployment

- [ ] **Prepare Play Console:**
  - [ ] Log into Google Play Console
  - [ ] Navigate to your app → Testing → Internal/Closed testing track

- [ ] **Upload AAB:**
  - [ ] Upload `app-release.aab`
  - [ ] Wait for Play Console processing (usually 10-30 minutes)
  - [ ] Resolve any warnings/errors from Play Console

- [ ] **Release Notes:**
  - [ ] Copy content from `RELEASE_NOTES.md`
  - [ ] Add Play Console-specific notes if needed
  - [ ] Include both English and Persian notes (if supported)

- [ ] **Beta Track Configuration:**
  - [ ] Select beta track (Internal/Closed/Open)
  - [ ] Configure tester groups (if Closed testing)
  - [ ] Set rollout percentage (start with 10% for Open testing)

- [ ] **Start Rollout:**
  - [ ] Review all settings one final time
  - [ ] Click "Start rollout to beta testers"
  - [ ] Monitor initial deployment for errors

### 4.5 Release Communication

- [ ] **Announcement (if applicable):**
  - [ ] GitHub release page (if using GitHub Releases)
  - [ ] Community channels (Discord, Telegram, etc.)
  - [ ] Social media (if applicable)

- [ ] **Release Information:**
  - [ ] Version number
  - [ ] Key features/changes
  - [ ] Known issues
  - [ ] Feedback channels

---

## 5. Post-Release Monitoring

### 5.1 Crash Monitoring

**Firebase Crashlytics (if configured):**
- [ ] Monitor crash-free rate (target: >99%)
- [ ] Review crash reports daily for first week
- [ ] Prioritize crashes by:
  - Frequency
  - User impact
  - Affected Android versions

**Play Console Vitals:**
- [ ] Check ANR (App Not Responding) rate
- [ ] Monitor crash rate
- [ ] Review startup time metrics
- [ ] Check battery usage reports

### 5.2 User Feedback

- [ ] **Play Console Reviews:**
  - [ ] Monitor beta tester reviews
  - [ ] Respond to critical issues
  - [ ] Track common complaints/requests

- [ ] **Direct Feedback:**
  - [ ] Check GitHub Issues (if public)
  - [ ] Monitor community channels
  - [ ] Collect feedback on:
    - UI/UX improvements
    - Feature requests
    - Bug reports
    - Performance issues

### 5.3 Analytics (if configured)

- [ ] **Key Metrics to Track:**
  - Daily active users (DAU)
  - Scan completion rate
  - Most used scan types
  - Error rates per feature
  - User retention

- [ ] **Performance Metrics:**
  - Average scan time
  - API response times
  - App startup time
  - Memory usage

### 5.4 Hotfix Process

If critical issues are found:

1. **Assess Severity:**
   - 🔴 Critical: App crashes, data loss → Hotfix immediately
   - 🟡 High: Major feature broken → Fix within 24-48 hours
   - 🟢 Low: Minor UI issues → Include in next release

2. **Hotfix Workflow:**
   ```bash
   # Create hotfix branch
   git checkout -b hotfix/v1.0.0-betaX.1
   
   # Make fixes
   # ... code changes ...
   
   # Bump patch version
   versionCode = X+1
   versionName = "1.0.0-betaX.1"
   
   # Build and deploy
   ./gradlew :app:bundleRelease
   ```

3. **Deploy Hotfix:**
   - Upload new AAB to Play Console
   - Add hotfix notes explaining the fix
   - Notify beta testers if critical

---

## 6. Troubleshooting

### Common Build Issues

**Problem:** Gradle sync fails
```bash
# Solution:
./gradlew clean
./gradlew --refresh-dependencies
# Then sync in Android Studio
```

**Problem:** Keystore not found
- Verify `keystore.properties` path is correct
- Check `scamynx-release-key.jks` exists in project root
- Ensure keystore passwords are correct

**Problem:** ProGuard/R8 errors
- Check `app/proguard-rules.pro` for missing rules
- Verify all reflection-based code has keep rules
- Test release build thoroughly

### Common Runtime Issues

**Problem:** API calls failing
- Check API keys in `secrets.properties`
- Verify network security config allows API domains
- Test with network logging enabled

**Problem:** Database migration errors
- Verify Room migration paths are correct
- Check database version in `ScanDatabase.kt`
- Test migration on fresh install

**Problem:** UI rendering issues
- Check theme colors in `Color.kt`
- Verify string resources exist for all languages
- Test on different screen sizes/densities

### Getting Help

- Review `docs/THEME_IMPROVEMENTS.md` for theme-related issues
- Check `RELEASE_CHECKLIST.md` for release process questions
- Consult `CONTRIBUTING.md` for development guidelines
- Review GitHub Issues for known problems

---

## 7. Future Enhancements

### High Priority

- [ ] **Integration Tests:**
  - Mock VirusTotal/UrlScan responses for deterministic scoring
  - Test risk calculation with known inputs/outputs
  - Validate ML model integration

- [ ] **Feature Flags:**
  - Implement feature flag system for rapid rollback
  - Allow toggling new hero layout without app update
  - A/B testing support for UI changes

- [ ] **Documentation:**
  - Document gradient system in `docs/THEME_QUICK_REFERENCE.md`
  - Create API integration guide
  - Add architecture decision records (ADRs)

### Medium Priority

- [ ] **Performance:**
  - Implement baseline profiles for faster startup
  - Optimize image loading and caching
  - Add performance monitoring

- [ ] **Testing:**
  - Increase unit test coverage to >85%
  - Add UI tests for critical flows
  - Implement screenshot testing

- [ ] **Accessibility:**
  - Complete TalkBack support
  - Add haptic feedback
  - Improve keyboard navigation

### Low Priority

- [ ] **Developer Experience:**
  - Add pre-commit hooks for code quality
  - Automate screenshot generation
  - Create release automation scripts

- [ ] **Analytics:**
  - Implement comprehensive analytics
  - Add user behavior tracking
  - Create dashboard for metrics

---

## 📝 Quick Reference

### Essential Commands

```bash
# Full clean build
./gradlew clean build

# Run tests
./gradlew testDebugUnitTest connectedDebugAndroidTest

# Generate release bundle
./gradlew :app:bundleRelease

# Install release APK
adb install -r app/build/outputs/apk/release/app-release.apk

# Check lint
./gradlew :app:lintDebug
```

### Key Files

- Version: `app/build.gradle.kts` (lines 39-40)
- Strings: `app/src/main/res/values*/strings.xml`
- Theme: `common/src/main/java/.../designsystem/`
- Release Notes: `RELEASE_NOTES.md`
- Keystore Config: `keystore.properties`

### Version History

| Version | Date | Notes |
|---------|------|-------|
| 1.0.0-beta2 | Current | See `app/build.gradle.kts` |
| 1.0.0-beta1 | Oct 18, 2025 | Initial beta release |

---

**Happy shipping! 🚀**

> **Last Updated:** Check git history for latest changes  
> **Maintainer:** Aiden (V7LTHRONYX)
