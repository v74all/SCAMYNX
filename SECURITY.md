# 🔒 Security Policy

## Supported Versions

Currently supported versions with security updates:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.0-beta1 | :white_check_mark: |
| < 1.0   | :x:                |

## 🛡️ Security Best Practices

### For Users

1. **Never share your API keys**
   - Keep `secrets.properties` private
   - Don't commit API keys to version control
   - Regenerate keys if accidentally exposed

2. **Verify app authenticity**
   - Only download from official GitHub releases
   - Verify APK signatures before installation
   - Check SHA256 hashes match official releases

3. **Keep the app updated**
   - Install security updates promptly
   - Enable auto-update if available
   - Check release notes for security fixes

### For Developers

1. **API Key Management**
   ```properties
   # ✅ CORRECT: Use secrets.properties (gitignored)
   VIRUSTOTAL_API_KEY=your_key_here
   
   # ❌ WRONG: Never hardcode in source files
   const val API_KEY = "abc123..." // DON'T DO THIS!
   ```

2. **Secure Coding**
   - Always validate user input
   - Use HTTPS for all network requests
   - Implement certificate pinning for production
   - Sanitize data before storage/display

3. **Dependency Security**
   ```bash
   # Check for vulnerable dependencies
   ./gradlew dependencyCheckAnalyze
   ```

## 🚨 Reporting a Vulnerability

If you discover a security vulnerability, please follow these steps:

### DO:
1. **Email directly**: [Your contact email here]
2. **Include details**:
   - Type of vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)
3. **Wait for response** before public disclosure

### DON'T:
- ❌ Open a public GitHub issue
- ❌ Post on social media
- ❌ Share exploit code publicly

## ⏱️ Response Timeline

- **24 hours**: Initial acknowledgment
- **72 hours**: Preliminary assessment
- **7 days**: Fix development (for critical issues)
- **14 days**: Release with security patch

## 🏆 Security Hall of Fame

We appreciate responsible disclosure. Security researchers who help improve SCAMYNX will be acknowledged here (with permission):

*No reports yet - be the first!*

## 🔐 Encryption & Data Protection

### What We Protect
- ✅ API keys stored in encrypted preferences
- ✅ Local database encrypted at rest (optional)
- ✅ All network traffic uses TLS 1.2+
- ✅ No sensitive data transmitted without encryption

### What We Don't Collect
- ❌ Personal information
- ❌ Scan history (stored locally only)
- ❌ Usage analytics (optional, user-controlled)
- ❌ Location data

## 📋 Security Checklist

Before each release, we verify:

- [ ] All dependencies updated to latest secure versions
- [ ] No hardcoded secrets in codebase
- [ ] ProGuard/R8 obfuscation enabled for release builds
- [ ] Certificate pinning configured
- [ ] Input validation on all user inputs
- [ ] SQL injection prevention in database queries
- [ ] XSS prevention in WebView (if used)
- [ ] Secure random number generation
- [ ] Proper error handling (no sensitive info in logs)
- [ ] HTTPS-only communication

## 🔗 Security Resources

- [OWASP Mobile Security](https://owasp.org/www-project-mobile-security/)
- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)
- [Kotlin Security Guidelines](https://kotlinlang.org/docs/security.html)

## 📞 Contact

For security-related inquiries:
- **Security Email**: [Your security email]
- **GitHub**: [@V7LTHRONYX](https://github.com/V7LTHRONYX)
- **Response Time**: Within 24 hours

---

**Last Updated**: October 18, 2025  
**Policy Version**: 1.0
