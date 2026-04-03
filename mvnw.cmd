@ECHO OFF
SETLOCAL
SET "__MVNW_SCRIPT_DIR__=%~dp0"
SET "__MVNW_SCRIPT__=%__MVNW_SCRIPT_DIR__%.mvn\wrapper\maven-wrapper.ps1"
powershell -NoProfile -ExecutionPolicy Bypass -File "%__MVNW_SCRIPT__%" %*
EXIT /B %ERRORLEVEL%
