# 8080 사용 중이면 종료한 뒤 run-local.ps1로 Spring Boot 재기동합니다.
$ErrorActionPreference = 'Continue'
Set-Location $PSScriptRoot

$port = 8080
$pids = @(
  Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique
) | Where-Object { $_ -and $_ -gt 0 }

foreach ($procId in $pids) {
  try {
    Stop-Process -Id $procId -Force -ErrorAction Stop
    Write-Host "[sehyun-cinema] 포트 $port 프로세스 종료: PID $procId"
  } catch {
    Write-Host "[sehyun-cinema] PID $procId 종료 실패: $_"
  }
}

if ($pids.Count -eq 0) {
  Write-Host "[sehyun-cinema] 포트 $port 을(를) 쓰는 프로세스가 없습니다. 새로 기동합니다."
}

Write-Host ""
& .\run-local.ps1
exit $LASTEXITCODE
