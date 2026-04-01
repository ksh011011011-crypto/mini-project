import { Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType } from "docx";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const lines = [
  ["h1", "세현시네마 PRD — 변경 반영본"],
  ["p", "작성일: 2026-03-31 · 대상: Spring Boot 예매 서비스(sehyun-cinema) 및 발표 자료"],
  ["p", "※ 채팅에서 처음 주신 원본 .docx 파일은 워크스페이스에 없어, 동일 파일명으로 덮어쓰려면 해당 문서를 mini-project 폴더에 넣은 뒤 목차만 맞춰 병합하시면 됩니다."],
  ["h2", "1. 개요"],
  ["p", "온라인 영화 예매·멤버십·결제·AI 상담 등을 포함한 세현시네마 웹 애플리케이션. 포스터는 TMDB 이미지 URL을 DB에 저장해 화면에 표시한다."],
  ["h2", "2. 백엔드 변경 (DataInitializer)"],
  ["p", "• POSTER 맵: 영화 제목(정확 일치) → TMDB w500 포스터 URL"],
  ["p", "• applyPosterMapExact(): 기동 시 DB의 각 영화 제목이 맵에 있으면 poster_url을 맵 값과 동기화(값이 같으면 저장 생략)."],
  ["p", "• repairBrokenPostersOnly(): URL이 비어 있거나 placehold.co 등 플레이스홀더일 때만 보정. 맵에 없으면 기본 포스터(기생충 포스터 URL) 사용."],
  ["p", "• 부분 매칭(제목 포함 검색)으로 포스터를 덮어쓰는 로직은 제거하여 오매칭을 방지함."],
  ["h2", "3. 프론트(Thymeleaf) 변경"],
  ["p", "• templates/fragments/movie-cards.html: 더보기·검색 결과 카드에 TMDB 포스터 <img> 표시(기존 플레이스홀더 아이콘만 있던 문제 수정)."],
  ["p", "• templates/index.html: 없는 로컬 /images/no-poster.png 대신 placehold.co 기본 이미지로 통일."],
  ["p", "• 예매(booking): /api/movies/search 로 movieMap 채운 뒤 선택 영화 포스터 미리보기 — DB posterUrl이 정확해야 함."],
  ["h2", "4. 발표 HTML (참고)"],
  ["p", "• sehyun-cinema-PPT.html: 표지·박스오피스 슬라이드에 TOP10 TMDB 포스터 반영, 긴 슬라이드는 스크롤 가능하도록 스타일 조정."],
  ["h2", "5. 실행 환경"],
  ["p", "• server.port=8080, PostgreSQL(Supabase) 연동. application.properties의 DB 비밀번호는 배포 시 본인 값으로 유지."],
  ["p", "• 기동: sehyun-cinema 디렉터리에서 gradlew bootRun"],
];

const children = [];
for (const [kind, text] of lines) {
  if (kind === "h1") {
    children.push(
      new Paragraph({
        heading: HeadingLevel.TITLE,
        spacing: { after: 200 },
        children: [new TextRun({ text, bold: true, size: 36 })],
      })
    );
  } else if (kind === "h2") {
    children.push(
      new Paragraph({
        heading: HeadingLevel.HEADING_1,
        spacing: { before: 240, after: 120 },
        children: [new TextRun({ text, bold: true, size: 28 })],
      })
    );
  } else {
    children.push(
      new Paragraph({
        spacing: { after: 120 },
        children: [new TextRun({ text, size: 22 })],
      })
    );
  }
}

const doc = new Document({
  sections: [{ children }],
});

const buf = await Packer.toBuffer(doc);
const out = path.join(__dirname, "..", "세현시네마-PRD-변경반영.docx");
fs.writeFileSync(out, buf);
console.log("Wrote:", out);
