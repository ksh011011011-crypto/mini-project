@echo off
REM UTF-8 콘솔 (한글 안내 문구)
chcp 65001 >nul
REM 지연 확장(!) 미사용 → 경로/Gradle 로그에 ! 가 있어도 깨지지 않음
setlocal

cd /d "%~dp0"

set "JAVA_HOME="
set "FULL="
for /f "delims=" %%J in ('where java 2^>nul') do (
  set "FULL=%%J"
  goto :got_java
)
:got_java
if not defined FULL (
  echo.
  echo [sehyun-cinema] PATH에 java.exe가 없습니다.
  echo   winget install Microsoft.OpenJDK.21
  echo   설치 후 PC 재시작 또는 새 터미널에서 다시 실행하세요.
  echo.
  pause
  exit /b 1
)

for %%F in ("%FULL%") do set "JDK_BIN=%%~dpF"
set "PATH=%JDK_BIN%;%PATH%"

"%JDK_BIN%java.exe" -version 2>nul
if errorlevel 1 (
  echo [sehyun-cinema] Java 실행 실패: %JDK_BIN%java.exe
  pause
  exit /b 1
)

set "SHOW_HOME=%FULL:\bin\java.exe=%"
set "SHOW_HOME=%SHOW_HOME:\bin\javaw.exe=%"
echo [sehyun-cinema] 사용 JDK: %SHOW_HOME%
echo [sehyun-cinema] 프로필: local (메모리 H2)
echo [sehyun-cinema] 접속: http://localhost:8080
echo.

set "JAVA_HOME="
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
call gradlew.bat bootRun --no-daemon -PuseLocal
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" pause
exit /b %RC%
