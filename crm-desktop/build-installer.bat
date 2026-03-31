@echo off
REM ═══════════════════════════════════════════════════
REM   CRM Desktop Application — Windows Installer Builder
REM   Requires: JDK 17+, Maven, JavaFX SDK
REM ═══════════════════════════════════════════════════

echo [1/3] Building JAR with Maven...
call mvn clean package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo BUILD FAILED
    exit /b 1
)

echo [2/3] Creating Windows installer with jpackage...
jpackage ^
  --type exe ^
  --name "CRM Desktop" ^
  --app-version 1.0.0 ^
  --vendor "CRM TempO" ^
  --description "CRM Desktop Application" ^
  --input target ^
  --main-jar crm-desktop-1.0.0.jar ^
  --main-class com.crm.desktop.CrmDesktopApp ^
  --dest target/installer ^
  --win-dir-chooser ^
  --win-shortcut ^
  --win-menu ^
  --win-menu-group "CRM TempO" ^
  --java-options "-Dcrm.api.url=http://localhost:8080"

if %ERRORLEVEL% neq 0 (
    echo PACKAGING FAILED
    exit /b 1
)

echo [3/3] Done! Installer created in target/installer/
echo.
pause
