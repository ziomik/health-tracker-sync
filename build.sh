#!/bin/bash
# Quick build script for Health Tracker Sync Android App

set -e

echo "🔨 Building Health Tracker Sync Android App..."
echo ""

# Check gradlew exists
if [ ! -f ./gradlew ]; then
    echo "❌ gradlew not found! Run this from android-companion directory"
    exit 1
fi

# Make gradlew executable
chmod +x ./gradlew

# Build type (default: debug)
BUILD_TYPE=${1:-debug}

if [ "$BUILD_TYPE" == "release" ]; then
    echo "📦 Building RELEASE APK..."
    ./gradlew assembleRelease
    
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
    
elif [ "$BUILD_TYPE" == "debug" ]; then
    echo "🐛 Building DEBUG APK..."
    ./gradlew assembleDebug
    
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
else
    echo "❌ Invalid build type: $BUILD_TYPE"
    echo "Usage: ./build.sh [debug|release]"
    exit 1
fi

# Check if APK was created
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo ""
    echo "✅ Build SUCCESS!"
    echo "📱 APK: $APK_PATH"
    echo "📊 Size: $APK_SIZE"
    echo ""
    echo "📲 Install with:"
    echo "   adb install $APK_PATH"
else
    echo "❌ Build failed! APK not found"
    exit 1
fi
