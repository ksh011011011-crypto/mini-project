# Cursor/터미널에서 gradlew가 "JAVA_HOME invalid"로 죽을 때 사용합니다.
# run-server.bat과 동일: 잘못된 JAVA_HOME 무시 → PATH의 java.exe 사용
$ErrorActionPreference = 'Continue'
Set-Location $PSScriptRoot

# CMD와 비슷하게 UTF-8 출력 (한글/로그 깨짐 완화)
try {
    [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
    $OutputEncoding = [Console]::OutputEncoding
} catch { }

$env:JAVA_HOME = ''
$env:JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8'
$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    Write-Host '[sehyun-cinema] PATH에 java.exe가 없습니다. JDK 21 설치 후 다시 시도하세요.'
    exit 1
}
$bin = Split-Path $java.Source -Parent
$env:PATH = "$bin;$env:PATH"

Write-Host '[sehyun-cinema] JAVA_HOME 비움 → PATH의 java 사용 (잘못된 JAVA_HOME 오류 방지)'
Write-Host '[sehyun-cinema] 프로필: local (메모리 H2) → http://localhost:8080'
Write-Host ''

& .\gradlew.bat bootRun --no-daemon -PuseLocal
exit $LASTEXITCODE
