#!/bin/bash

# 🚀 SCAMYNX - Quick Setup Script
# This script helps you set up the project quickly

echo "🛡️  Welcome to SCAMYNX Setup!"
echo "================================"
echo ""

# Check if git is initialized
if [ ! -d ".git" ]; then
    echo "📦 Initializing Git repository..."
    git init
    git add .
    git commit -m "Initial commit - SCAMYNX v1.0.0-beta1"
    echo "✅ Git repository initialized"
else
    echo "✅ Git repository already exists"
fi

# Create secrets.properties if it doesn't exist
if [ ! -f "secrets.properties" ]; then
    echo ""
    echo "🔑 Creating secrets.properties from template..."
    cp secrets.defaults.properties secrets.properties
    echo "✅ secrets.properties created"
    echo ""
    echo "⚠️  IMPORTANT: Edit secrets.properties and add your API keys!"
    echo "   Get your keys from:"
    echo "   - VirusTotal: https://www.virustotal.com/gui/user/[username]/apikey"
    echo "   - Google Safe Browsing: https://developers.google.com/safe-browsing/v4/get-started"
    echo "   - URLScan: https://urlscan.io/user/profile/"
else
    echo "✅ secrets.properties already exists"
fi

# Create local.properties if it doesn't exist
if [ ! -f "local.properties" ]; then
    echo ""
    echo "📱 Creating local.properties..."
    
    # Try to detect Android SDK
    if [ -n "$ANDROID_SDK_ROOT" ]; then
        echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties
        echo "✅ local.properties created with Android SDK from environment"
    elif [ -d "$HOME/Android/Sdk" ]; then
        echo "sdk.dir=$HOME/Android/Sdk" > local.properties
        echo "✅ local.properties created with detected Android SDK"
    elif [ -d "$HOME/Library/Android/sdk" ]; then
        echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
        echo "✅ local.properties created with detected Android SDK (macOS)"
    else
        echo "⚠️  Could not detect Android SDK location"
        echo "   Please create local.properties manually with:"
        echo "   sdk.dir=/path/to/your/android/sdk"
    fi
else
    echo "✅ local.properties already exists"
fi

# Check Java version
echo ""
echo "☕ Checking Java version..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge "21" ]; then
        echo "✅ Java $JAVA_VERSION detected (required: 21+)"
    else
        echo "⚠️  Java $JAVA_VERSION detected, but Java 21+ is required"
        echo "   Install Java 21: https://adoptium.net/"
    fi
else
    echo "❌ Java not found! Please install Java 21 or higher"
fi

# Make gradlew executable
echo ""
echo "🔧 Setting up Gradle wrapper..."
chmod +x gradlew
echo "✅ gradlew is now executable"

# Setup git hooks
if [ -d ".git" ]; then
    echo ""
    echo "🪝 Setting up Git hooks..."
    mkdir -p .git/hooks
    if [ -f ".git-hooks/pre-commit" ]; then
        cp .git-hooks/pre-commit .git/hooks/pre-commit
        chmod +x .git/hooks/pre-commit
        echo "✅ Pre-commit hook installed"
    fi
fi

echo ""
echo "================================"
echo "🎉 Setup Complete!"
echo ""
echo "Next steps:"
echo "1. Edit secrets.properties with your API keys"
echo "2. Open project in Android Studio"
echo "3. Run: ./gradlew build"
echo "4. Run: ./gradlew :app:installDebug"
echo ""
echo "For more information, see README.md"
echo "================================"
