# Minimal OOXML Word document (.docx = zip)
$ErrorActionPreference = "Stop"
$root = Join-Path $PSScriptRoot "docx_build"
if (Test-Path $root) { Remove-Item $root -Recurse -Force }
New-Item -ItemType Directory -Path (Join-Path $root "_rels") -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $root "word\_rels") -Force | Out-Null

$ct = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>
'@
$rels = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>
'@
$docrels = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>
'@

function Xml-Escape([string]$s) {
  if ($null -eq $s) { return "" }
  return ($s -replace '&', '&amp;' -replace '<', '&lt;' -replace '>', '&gt;' -replace '"', '&quot;')
}

$paras = @(
  "세현시네마 PRD — 변경 반영본",
  "작성일: 2026-03-31 · Spring Boot 예매 서비스(sehyun-cinema) 및 발표 자료 반영",
  "※ 처음 채팅으로 주신 원본 .docx는 워크스페이스에 없습니다. 동일 양식으로 맞추려면 해당 파일을 mini-project 폴더에 두고 이 본문을 복사해 넣으시면 됩니다.",
  "",
  "1. 개요",
  "온라인 영화 예매·멤버십·결제·AI 상담 등. 포스터는 TMDB 이미지 URL을 DB에 저장해 화면에 표시합니다.",
  "",
  "2. 백엔드 (DataInitializer)",
  "• POSTER 맵: 영화 제목(정확 일치) → TMDB w500 포스터 URL",
  "• applyPosterMapExact(): 기동 시 제목이 맵에 있으면 poster_url 동기화(값 동일 시 저장 생략)",
  "• repairBrokenPostersOnly(): 빈 URL·플레이스홀더만 보정. 맵에 없으면 기본 포스터(기생충 포스터 URL)",
  "• 부분 매칭으로 포스터를 덮어쓰는 로직 제거(오매칭 방지)",
  "",
  "3. 프론트",
  "• fragments/movie-cards.html: 더보기·검색 카드에 포스터 img 표시",
  "• index.html: 없는 /images/no-poster.png → placehold.co 기본 이미지",
  "• 예매: /api/movies/search 로 포스터 미리보기 — DB posterUrl 필요",
  "",
  "4. 발표 HTML",
  "• sehyun-cinema-PPT.html: TOP10 포스터·슬라이드 레이아웃 조정(참고)",
  "",
  "5. 실행",
  "• server.port=8080, PostgreSQL(Supabase). sehyun-cinema 에서 gradlew bootRun"
)

$sb = New-Object System.Text.StringBuilder
[void]$sb.Append('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>')
[void]$sb.Append('<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">')
[void]$sb.Append('<w:body>')
foreach ($line in $paras) {
  $t = Xml-Escape $line
  [void]$sb.Append("<w:p><w:r><w:t xml:space=`"preserve`">$t</w:t></w:r></w:p>")
}
[void]$sb.Append('<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/></w:sectPr>')
[void]$sb.Append('</w:body></w:document>')
$documentXml = $sb.ToString()

[System.IO.File]::WriteAllText((Join-Path $root "[Content_Types].xml"), $ct, [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllText((Join-Path $root "_rels\.rels"), $rels, [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllText((Join-Path $root "word\document.xml"), $documentXml, [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllText((Join-Path $root "word\_rels\document.xml.rels"), $docrels, [System.Text.UTF8Encoding]::new($false))

$outZip = Join-Path $PSScriptRoot "temp-prd.zip"
$outDocx = Join-Path (Split-Path $PSScriptRoot -Parent) "세현시네마-PRD-변경반영.docx"
if (Test-Path $outZip) { Remove-Item $outZip -Force }
if (Test-Path $outDocx) { Remove-Item $outDocx -Force }
Compress-Archive -Path (Join-Path $root "*") -DestinationPath $outZip -Force
# Compress-Archive puts top-level items; need [Content_Types] at root — use Path $root\* 
Remove-Item $outZip -Force -ErrorAction SilentlyContinue
Compress-Archive -LiteralPath @(
  (Join-Path $root "[Content_Types].xml"),
  (Join-Path $root "_rels"),
  (Join-Path $root "word")
) -DestinationPath $outZip -Force
Move-Item -LiteralPath $outZip -Destination $outDocx -Force
Remove-Item $root -Recurse -Force
Write-Host "OK:" $outDocx
