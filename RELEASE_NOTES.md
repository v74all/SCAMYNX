# 🎉 SCAMYNX v1.0.0-beta1 Release Notes

**Release Date**: February 6, 2026  
**Developer:** Aiden (V7LTHRONYX)  
**Status:** Beta Release

---

## 🚀 What's New in Beta 1

This is the first public beta release of SCAMYNX Android! After months of development, we're excited to share this powerful scam detection tool with the community.

### ✨ Core Features

#### 🔍 Multi-Target Scanning
- **URL Scanner** - Detect phishing websites and malicious links
  - Integration with 6+ threat intelligence providers
  - Real-time URL analysis
  - Historical threat data lookup

- **File Scanner** - Analyze suspicious files
  - Support for APK, EXE, scripts, and more
  - SHA256 hash verification
  - Suspicious keyword detection
  - Obfuscation analysis

- **VPN Config Scanner** - Validate VPN configurations
  - VMESS, VLESS, Trojan, Shadowsocks support
  - TLS security verification
  - Private endpoint detection
  - JSON and link format parsing

- **Instagram Scanner** - Detect scam profiles
  - Handle keyword analysis
  - Phishing message detection
  - Follower count verification
  - Bio and link examination

#### 🧠 Advanced Analysis Engine
- **Machine Learning** - TensorFlow Lite-powered phishing detection
- **Network Security** - TLS/SSL certificate verification
- **Risk Scoring** - Sophisticated fuzzy logic algorithm
- **Threat Intelligence** - Real-time updates from multiple sources

#### 🎨 Modern User Interface
- Material Design 3 with Jetpack Compose
- Smooth animations and transitions
- Intuitive navigation
- Beautiful result visualization

#### 🌍 Bilingual Support
- Full English (LTR) support
- Full Persian/Farsi (RTL) support
- Auto-detection of system language
- Easy language switching

### 🏗️ Technical Highlights

#### Architecture
- **Clean Architecture** with clear separation of concerns
- **Multi-module** project structure for scalability
- **MVVM pattern** for maintainable UI layer
- **Dependency Injection** with Hilt

#### Performance
- On-device ML processing (no internet required for ML)
- Efficient background processing with WorkManager
- Room database for fast local storage
- Baseline profiles for optimized startup

#### Security
- API keys protected with Gradle Secrets Plugin
- No personal data collection
- All processing done on-device
- Encrypted local storage

---

## 📦 What's Included

### Modules
- `:app` - Main application
- `:domain` - Business logic
- `:data` - Data layer with repositories
- `:ml` - Machine learning integration
- `:networksecurity` - Network analysis
- `:report` - Report generation
- `:common` - Shared components

### Dependencies
- Kotlin 2.0.21
- Jetpack Compose BOM
- Hilt for DI
- Room for database
- Retrofit + OkHttp for networking
- TensorFlow Lite for ML
- Coroutines + Flow for async operations

---

## 🔧 Installation

### Requirements
- Android 6.0 (API 23) or higher
- ~50MB storage space
- Internet connection for API-based scans

### Download
1. Download the APK from [Releases](https://github.com/V7LTHRONYX/scamynx-android/releases/tag/v1.0.0-beta1)
2. Enable "Install from unknown sources" in Android settings
3. Install the APK
4. Grant necessary permissions

### Building from Source
```bash
git clone https://github.com/V7LTHRONYX/scamynx-android.git
cd scamynx-android
cp secrets.defaults.properties secrets.properties
# Add your API keys to secrets.properties
./gradlew :app:assembleDebug
```

---

## 🐛 Known Issues

### Limitations
- ⚠️ Telemetry endpoint placeholder (AI features coming soon)
- ⚠️ Some VPN config edge cases may not parse correctly
- ⚠️ Rate limits apply to free API tiers

### Planned Fixes
These will be addressed in upcoming releases:
- Improve VPN config parser robustness
- Add more detailed error messages
- Enhance ML model accuracy
- Optimize database queries

---

## 🔮 Coming Soon

### v1.0.0 (Stable Release)
- [ ] Complete PDF report generation
- [ ] Enhanced error handling
- [ ] Performance optimizations
- [ ] Bug fixes based on beta feedback

### v1.1.0 (Future)
- [ ] AI-powered telemetry integration
- [ ] Scan history cloud backup
- [ ] Scheduled scans
- [ ] Browser extension integration
- [ ] Additional threat intelligence sources

---

## 📊 API Rate Limits

Be aware of these limits with free API tiers:

| Provider | Free Tier Limit |
|----------|----------------|
| VirusTotal | 4 requests/minute |
| Google Safe Browsing | 10,000 requests/day |
| URLScan | 50 requests/day |
| ThreatFox | No official limit |
| URLHaus | No official limit |
| PhishStats | No official limit |

For production use or high volume scanning, consider:
- Upgrading to paid API plans
- Implementing request caching
- Using batch scanning features

---

## 🙏 Acknowledgments

Special thanks to:
- **Open Source Community** for incredible tools and libraries
- **Threat Intelligence Providers** for free API access
- **Beta Testers** (you!) for helping improve SCAMYNX
- **Android Developer Community** for extensive documentation

---

## 📢 Feedback & Support

We need your feedback to make SCAMYNX better!

### Report Issues
- [GitHub Issues](https://github.com/V7LTHRONYX/scamynx-android/issues)
- Be specific and include reproduction steps
- Attach logs if possible (Settings > Export Logs)

### Feature Requests
- [GitHub Discussions](https://github.com/V7LTHRONYX/scamynx-android/discussions)
- Describe your use case
- Explain why the feature would be useful

### General Questions
- Check the [README](https://github.com/V7LTHRONYX/scamynx-android#readme)
- Browse [existing issues](https://github.com/V7LTHRONYX/scamynx-android/issues)
- Ask in [Discussions](https://github.com/V7LTHRONYX/scamynx-android/discussions)

---

## 📄 License

SCAMYNX is open source software licensed under the MIT License.

```
MIT License - Copyright (c) 2025 Aiden (V7LTHRONYX)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction...
```

See [LICENSE](LICENSE) for full text.

---

## 🔐 Security

Found a security vulnerability? Please report it responsibly:
- Email: [Your security email]
- See [SECURITY.md](SECURITY.md) for details
- Do NOT open public issues for security bugs

---

## 🎯 Beta Testing Goals

Help us by testing these scenarios:

1. **URL Scanning**
   - [ ] Scan known phishing sites
   - [ ] Scan legitimate websites
   - [ ] Test with various URL formats

2. **File Scanning**
   - [ ] Scan APK files
   - [ ] Scan script files (JS, Python)
   - [ ] Test with large files (>10MB)

3. **VPN Config**
   - [ ] Test VMESS links
   - [ ] Test VLESS links
   - [ ] Test JSON configs

4. **Instagram**
   - [ ] Scan suspicious profiles
   - [ ] Scan verified accounts
   - [ ] Test with various handle formats

5. **UI/UX**
   - [ ] Switch between English/Persian
   - [ ] Test dark/light themes
   - [ ] Check accessibility features

**Share your findings** in GitHub Issues!

---

## 📈 Version History

| Version | Release Date | Notes |
|---------|-------------|-------|
| 1.0.0-beta1 | Feb 6, 2026 | First public beta |

---

## 💝 Support the Project

If you find SCAMYNX useful:
- ⭐ Star the repository
- 🐛 Report bugs
- 💡 Suggest features
- 🔀 Submit pull requests
- 📢 Share with others

---

**Made with ❤️ in Iran 🇮🇷**

Thank you for being part of the SCAMYNX beta! 🚀
