@echo off
chcp 65001 >nul
REM 한 번 실행: 사용자 환경 변수 JAVA_HOME을 PATH의 java 기준으로 고칩니다(Program.Files 오타 해결).
setlocal

set "FULL="
for /f "delims=" %%J in ('where java 2^>nul') do (
  set "FULL=%%J"
  goto :ok
)
:ok
if not defined FULL (
  echo PATH에 java가 없습니다. OpenJDK 설치 후 다시 실행하세요.
  pause
  exit /b 1
)
set "NEW_HOME=%FULL:\bin\java.exe=%"
set "NEW_HOME=%NEW_HOME:\bin\javaw.exe=%"
echo 이 PC에 저장할 JAVA_HOME:
echo   %NEW_HOME%
echo.
setx JAVA_HOME "%NEW_HOME%"
if errorlevel 1 (
  echo setx 실패. 관리자 권한이 필요할 수 있습니다.
  pause
  exit /b 1
)
echo.
echo 완료. 새 터미널/IDE를 연 뒤부터 적용됩니다.
pause
