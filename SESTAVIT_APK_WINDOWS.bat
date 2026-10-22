@echo off
setlocal
powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0SESTAVIT_APK_WINDOWS.ps1"
if errorlevel 1 (
  echo.
  echo Sestaveni se nepovedlo. Zkontroluj chybu vyse a README.md.
  pause
  exit /b 1
)
echo.
echo Hotovo! APK je ve slozce app\build\outputs\apk\debug\
pause
