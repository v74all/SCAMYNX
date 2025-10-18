# 🤝 Contributing to SCAMYNX

Thank you for your interest in contributing to SCAMYNX! This document provides guidelines and instructions for contributing.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Submitting Changes](#submitting-changes)

## 🤝 Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inclusive environment for everyone.

### Expected Behavior

- ✅ Be respectful and considerate
- ✅ Use welcoming and inclusive language
- ✅ Accept constructive criticism gracefully
- ✅ Focus on what's best for the community
- ✅ Show empathy towards others

### Unacceptable Behavior

- ❌ Harassment or discriminatory language
- ❌ Personal attacks or insults
- ❌ Trolling or inflammatory comments
- ❌ Publishing others' private information

## 🚀 Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally
3. **Create a branch** for your changes
4. **Make your changes** and commit
5. **Push to your fork** and submit a PR

## 💻 Development Setup

### Prerequisites

```bash
# Install Java 21
sudo apt install openjdk-21-jdk  # Linux
brew install openjdk@21           # macOS

# Install Android Studio
# Download from: https://developer.android.com/studio
```

### Clone and Setup

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/scamynx-android.git
cd scamynx-android

# Add upstream remote
git remote add upstream https://github.com/V7LTHRONYX/scamynx-android.git

# Setup API keys
cp secrets.defaults.properties secrets.properties
# Edit secrets.properties with your API keys

# Build the project
./gradlew build
```

## 🔧 Making Changes

### Branch Naming

Use descriptive branch names:

```
feature/add-new-analyzer
bugfix/fix-network-crash
docs/update-readme
refactor/improve-risk-scorer
```

### Commit Messages

Follow conventional commits:

```
feat: Add Twitter/X profile analyzer
fix: Resolve crash when scanning large files
docs: Update API setup instructions
refactor: Simplify VPN config parser
test: Add unit tests for InstagramAnalyzer
perf: Optimize database queries
chore: Update dependencies
```

## 📝 Coding Standards

### Kotlin Style Guide

Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html):

```kotlin
// ✅ Good
class ScanAnalyzer @Inject constructor(
    private val repository: ScanRepository,
) {
    suspend fun analyze(url: String): Result<ScanReport> {
        return repository.scan(url)
    }
}

// ❌ Bad
class scanAnalyzer{
    fun Analyze(URL:String):Result<ScanReport>{
        return repo.scan(URL)
    }
}
```

### Code Organization

```kotlin
// 1. Package declaration
package com.v7lthronyx.scamynx.data.analyzer

// 2. Imports (Android, then third-party, then internal)
import android.content.Context
import javax.inject.Inject
import com.v7lthronyx.scamynx.domain.model.ScanResult

// 3. Constants
private const val MAX_RETRIES = 3

// 4. Class definition
@Singleton
class MyAnalyzer @Inject constructor() {
    // Implementation
}
```

### Naming Conventions

```kotlin
// Classes: PascalCase
class NetworkSecurityAnalyzer

// Functions: camelCase
fun analyzeTlsVersion()

// Constants: SCREAMING_SNAKE_CASE
const val DEFAULT_TIMEOUT = 30_000L

// Variables: camelCase
val scanResult: ScanResult
```

## 🧪 Testing

### Writing Tests

```kotlin
class ScanRepositoryTest {
    
    @Test
    fun `analyze URL returns success when valid`() = runTest {
        // Given
        val repository = createTestRepository()
        val url = "https://example.com"
        
        // When
        val result = repository.analyze(url).first()
        
        // Then
        assertThat(result).isInstanceOf<ScanState.Success>()
    }
}
```

### Running Tests

```bash
# Unit tests
./gradlew test

# Specific module
./gradlew :data:test

# With coverage
./gradlew test jacocoTestReport

# Instrumented tests
./gradlew connectedAndroidTest
```

## 📤 Submitting Changes

### Pull Request Process

1. **Update your fork**
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

2. **Run checks**
   ```bash
   ./gradlew check
   ./gradlew test
   ./gradlew lint
   ```

3. **Push to your fork**
   ```bash
   git push origin feature/your-feature
   ```

4. **Create Pull Request**
   - Go to GitHub and create a PR
   - Fill out the PR template
   - Link any related issues

### PR Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Manual testing completed
- [ ] All tests passing

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex code
- [ ] Documentation updated
- [ ] No new warnings generated
```

## 🎯 Areas for Contribution

### High Priority

- 🔴 PDF report generation
- 🔴 Additional threat intelligence providers
- 🔴 Performance optimizations

### Medium Priority

- 🟡 UI/UX improvements
- 🟡 More comprehensive tests
- 🟡 Documentation enhancements

### Good First Issues

- 🟢 Fix typos in documentation
- 🟢 Add code comments
- 🟢 Improve error messages
- 🟢 Add unit tests

## 📚 Resources

- [Android Development Guide](https://developer.android.com/guide)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose Guide](https://developer.android.com/jetpack/compose)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)

## 💬 Questions?

- Open a [GitHub Discussion](https://github.com/V7LTHRONYX/scamynx-android/discussions)
- Check existing [Issues](https://github.com/V7LTHRONYX/scamynx-android/issues)
- Read the [Documentation](https://github.com/V7LTHRONYX/scamynx-android/wiki)

## 🙏 Thank You!

Your contributions make SCAMYNX better for everyone. We appreciate your time and effort!

---

**Happy Coding! 🚀**
