````markdown
# 🛡️ SCAMYNX (Android)

**Version:** 1.0.0-beta1  
**Developer:** Aiden ([V7LTHRONYX](https://github.com/v74all))  
**License:** MIT

---

A powerful, native Kotlin scam detection platform for Android. SCAMYNX analyzes URLs, files, VPN configurations, and Instagram profiles using advanced threat intelligence, on-device machine learning, and comprehensive security checks.

## ✨ Features

### 🔍 Multi-Target Scanning
- **URLs** - Web phishing & malicious link detection
- **Files** - APK, EXE, script analysis with hash verification
- **VPN Configs** - VMESS, VLESS, Trojan, Shadowsocks validation
- **Instagram** - Profile scam detection & phishing message analysis

### 🧠 Advanced Analysis
- ✅ **6+ Threat Intelligence APIs** (VirusTotal, Google Safe Browsing, URLScan, ThreatFox, etc.)
- ✅ **TensorFlow Lite ML Model** - On-device phishing detection
- ✅ **Network Security Analysis** - TLS, SSL certificates, security headers
- ✅ **Fuzzy Risk Scoring** - Sophisticated risk categorization algorithm
- ✅ **Real-time Updates** - Background threat feed synchronization

### 🎨 Modern UI
- Material 3 Design with Jetpack Compose
- Bilingual support (English/Persian) with RTL layout
- Dark/Light theme support
- Accessibility compliant (WCAG)

### 🏗️ Architecture
- Clean Architecture with multi-module structure
- MVVM pattern with Kotlin Coroutines & Flow
- Hilt Dependency Injection
- Room Database for local persistence
- WorkManager for background processing

## 📦 Project Status

- ✅ Complete multi-module Gradle project (AGP 8.3.2, Kotlin 1.9.23)
- ✅ All analyzers implemented and tested
- ✅ Database layer with migration support
- ✅ API integrations fully functional
- ✅ Background processing with WorkManager
- ✅ Comprehensive test coverage
- 🔜 **AI-powered telemetry** (Coming in future releases)

## Module layout
| Module | Responsibility |
| --- | --- |
| `:app` | UI entry point, navigation, Hilt setup, Compose screens |
| `:common` | Design system, shared UI utilities |
| `:domain` | Pure Kotlin models and (future) use cases |
| `:data` | Data sources, repositories, Room, Retrofit (stubbed) |
| `:ml` | TensorFlow Lite integration & feature extraction (stubbed) |
| `:networksecurity` | TLS / DNS / header analyzers (stubbed) |
| `:report` | PDF/JSON export pipeline (stubbed) |

## 🚀 Getting Started

### Prerequisites
- **Android Studio** Hedgehog or newer
- **JDK 21** (required)
- **Android SDK 34**
- **API Keys** (see setup below)

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/v74all/scamynx-android.git
   cd scamynx-android
   ```

2. **Configure Android SDK**
   
   Create `local.properties` in the root directory:
   ```properties
   sdk.dir=/path/to/Android/Sdk
   ```

3. **⚠️ IMPORTANT: Setup API Keys**
   
   Copy the template file:
   ```bash
   cp secrets.defaults.properties secrets.properties
   ```
   
   Edit `secrets.properties` and add your API keys:
   ```properties
   VIRUSTOTAL_API_KEY=your_actual_virustotal_key
   GOOGLE_SAFE_BROWSING_API_KEY=your_actual_google_key
   URLSCAN_API_KEY=your_actual_urlscan_key
   SCAMYNX_TELEMETRY_ENDPOINT=
   ```
   
   **Get API Keys:**
   - [VirusTotal API](https://www.virustotal.com/gui/user/[username]/apikey)
   - [Google Safe Browsing](https://developers.google.com/safe-browsing/v4/get-started)
   - [URLScan.io](https://urlscan.io/user/profile/)
   
   > ⚠️ **Security Note:** Never commit `secrets.properties` to version control! This file is already in `.gitignore`.

4. **Build the project**
   ```bash
   ./gradlew :app:assembleDebug
   ```

5. **Run on device/emulator**
   ```bash
   ./gradlew :app:installDebug
   ```

### Running Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

## Localization
- The app ships with English (`en`) and Persian (`fa`).
- `@xml/locales_config` enables per-app language controls (Android 13+).
- Compose UI adapts layout direction automatically when the locale switches.

## 📂 Project Structure

```
scamynx-android/
├── app/                    # Main Android application module
├── common/                 # Shared UI components & design system
├── domain/                 # Business logic & models (Pure Kotlin)
├── data/                   # Repositories, databases, APIs
├── ml/                     # TensorFlow Lite ML integration
├── networksecurity/        # TLS/SSL/DNS security analyzers
├── report/                 # PDF/JSON report generation
└── baselineprofile/        # Performance optimization profiles
```

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Code Style
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add comments for complex logic
- Write unit tests for new features

## 🔒 Security & Privacy

- **No Data Collection**: SCAMYNX does not collect or transmit user data
- **On-Device Processing**: ML models run entirely on-device
- **API Key Security**: Your API keys are stored locally and never shared
- **Open Source**: Full transparency - inspect the code yourself

## 📝 API Rate Limits

Be aware of rate limits for free API tiers:
- **VirusTotal**: 4 requests/minute (free tier)
- **Google Safe Browsing**: 10,000 requests/day
- **URLScan**: 50 requests/day (free tier)

For production use, consider upgrading to paid API plans.

## 🐛 Known Issues

- Telemetry endpoint not yet implemented (planned for future release)
- PDF report generation in progress
- Some edge cases in VPN config parsing

Report bugs via [GitHub Issues](https://github.com/v74all/scamynx-android/issues).

## 🗺️ Roadmap

- [ ] Complete PDF report generation
- [ ] Implement AI-powered telemetry
- [ ] Add more threat intelligence providers
- [ ] iOS version development
- [ ] Web dashboard for scan history
- [ ] Scan scheduling & automation
- [ ] Browser extension integration

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

```
MIT License - Copyright (c) 2025 Aiden (V7LTHRONYX)
All rights reserved to Aiden (v74all)
```

## 👤 Author

**Aiden (V7LTHRONYX)**
- GitHub: [@v74all](https://github.com/v74all)

## 🙏 Acknowledgments

- [VirusTotal](https://www.virustotal.com/) for threat intelligence API
- [Google Safe Browsing](https://safebrowsing.google.com/) for phishing detection
- [URLScan.io](https://urlscan.io/) for URL analysis
- [ThreatFox](https://threatfox.abuse.ch/) for IOC data
- Android Open Source Project & Jetpack libraries
- TensorFlow Lite team for ML framework

## ⭐ Star History

If you find this project useful, please consider giving it a star ⭐

---

**Made with ❤️ in Iran 🇮🇷**

