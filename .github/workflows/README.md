# GitHub Actions Workflows

This directory will contain CI/CD workflows for automated testing and building.

## Planned Workflows

### 1. CI - Continuous Integration
- Run on every push and PR
- Execute unit tests
- Run lint checks
- Generate test coverage reports
- Build debug APK

### 2. Release
- Triggered on version tags
- Build release APK
- Sign with release keystore
- Create GitHub release
- Upload APK as artifact

### 3. Security Scan
- Weekly dependency vulnerability check
- Code security analysis
- Secret scanning

## Setup Instructions

To enable GitHub Actions:

1. Go to repository Settings > Actions
2. Enable Actions for this repository
3. Add secrets for signing (if using release workflow):
   - `KEYSTORE_FILE` (base64 encoded)
   - `KEYSTORE_PASSWORD`
   - `KEY_ALIAS`
   - `KEY_PASSWORD`

## Status Badges

Add these to README.md once workflows are set up:

```markdown
![CI](https://github.com/V7LTHRONYX/scamynx-android/workflows/CI/badge.svg)
![Release](https://github.com/V7LTHRONYX/scamynx-android/workflows/Release/badge.svg)
```

## Example: Basic CI Workflow

Create `.github/workflows/ci.yml`:

```yaml
name: CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        distribution: 'temurin'
        java-version: '21'
        
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Run tests
      run: ./gradlew test
      
    - name: Run lint
      run: ./gradlew lint
```
