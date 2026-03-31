'use strict';

/* ===========================
   영화 더미 데이터 (100개)
   =========================== */
const ALL_MOVIES = (() => {
  const titles = [
    '어벤져스: 엔드게임', '인터스텔라', '기생충', '오펜하이머', '탑건: 매버릭',
    '바비', '미션 임파서블: 데드 레코닝', '가디언즈 오브 갤럭시 3', '엘리멘탈', '스파이더맨: 어크로스 더 스파이더버스',
    '듄: 파트 2', '아쿠아맨과 로스트 킹덤', '더 마블스', '인디아나 존스 5', '트랜스포머: 비스트의 반란',
    '스즈메의 문단속', '슬램덩크', '더 플래시', '샹치와 텐 링즈의 전설', '이터널스',
    '닥터 스트레인지: 대혼돈의 멀티버스', '토르: 러브 앤 선더', '블랙 팬서: 와칸다 포에버', '앤트맨과 와스프: 퀀텀매니아', '가디언즈 오브 갤럭시 홀리데이 스페셜',
    '아바타: 물의 길', '탑건', '조커', '1917', '포드 v 페라리',
    '조조 래빗', '아이리시맨', '원스 어폰 어 타임 인 할리우드', '나이브스 아웃', '주디',
    '해리엇', '리처드 쥬얼', '작은 아씨들', '1917', '다크 워터스',
    '버즈 오브 프레이', '온워드', '콜', '소울', '미나리',
    '노매드랜드', '프라미싱 영 우먼', '사운드 오브 메탈', '맹크', '더 파더',
    '모리타니인', '주다스와 블랙 메시아', '미나리', '보이 버트 어 걸', '밀크 스트리트',
    '닥터 노', '골든핑거', '스카이폴', '스펙터', '노 타임 투 다이',
    '매트릭스: 리저렉션', '이터널 선샤인', '레미제라블', '레이디 버드', '세 얼간이',
    '설국열차', '부산행', '반도', '범죄도시', '범죄도시 2',
    '극한직업', '택시운전사', '1987', '밀정', '암살',
    '국제시장', '베테랑', '변호인', '광해', '도둑들',
    '타짜', '괴물', '살인의 추억', '올드보이', '봄여름가을겨울 그리고 봄',
    '클래식', '건축학개론', '응답하라 1988 극장판', '써니', '과속스캔들',
    '늑대소년', '7번방의 선물', '수상한 그녀', '검사외전', '내부자들',
    '아가씨', '곡성', '터널', '부산행: 반도', '다만 악에서 구하소서',
    '낙원의 밤', '모가디슈', '킹메이커', '브로커', '헤어질 결심',
    '범죄도시 3', '콘크리트 유토피아', '잠', '파묘', '웡카',
  ];

  const genres = ['액션', '드라마', 'SF', '코미디', '스릴러', '애니', '로맨스', '공포'];

  return titles.map((title, i) => ({
    id: i + 1,
    rank: i + 1,
    title,
    genre: genres[i % genres.length],
    rating: (Math.random() * 2 + 7.5).toFixed(1),      // 7.5 ~ 9.5
    bookingRate: Math.floor(Math.random() * 40 + 5),    // 5 ~ 44%
    color: `hsl(${(i * 37) % 360}, 35%, 22%)`,
  }));
})();

/* ===========================
   상태
   =========================== */
let visibleCount = 10;
let filteredMovies = [...ALL_MOVIES];
const PAGE_SIZE = 10;
const MAX_COUNT = 100;

/* ===========================
   유틸
   =========================== */
const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => document.querySelectorAll(selector);

/* ===========================
   팝업 제어
   =========================== */
function initPopup() {
  const overlay  = $('#popup-overlay');
  const closeBtn = $('#popup-close-btn');
  const closeX   = $('#popup-close-x');
  const noToday  = $('#popup-no-today-check');

  if (!overlay) return;

  const todayKey = 'popup_hidden_' + new Date().toDateString();
  if (localStorage.getItem(todayKey) === 'true') {
    overlay.classList.add('hidden');
    return;
  }

  function closePopup() {
    if (noToday && noToday.checked) {
      localStorage.setItem(todayKey, 'true');
    }
    overlay.style.animation = 'fadeIn 0.2s ease reverse forwards';
    setTimeout(() => overlay.classList.add('hidden'), 200);
  }

  closeBtn && closeBtn.addEventListener('click', closePopup);
  closeX   && closeX.addEventListener('click', closePopup);

  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) closePopup();
  });
}

/* ===========================
   영화 카드 렌더
   =========================== */
function renderMovieCard(movie) {
  const isTop3 = movie.rank <= 3;
  return `
    <div class="movie-card" data-title="${movie.title.toLowerCase()}" data-genre="${movie.genre}">
      <div class="movie-poster" style="background: ${movie.color};">
        <div class="movie-poster-placeholder">
          <i class="fa-solid fa-clapperboard"></i>
          <span>${movie.genre}</span>
        </div>
        <div class="movie-rank-badge ${isTop3 ? 'top3' : ''}">${movie.rank}</div>
      </div>
      <div class="movie-info">
        <p class="movie-title" title="${movie.title}">${movie.title}</p>
        <div class="movie-meta">
          <span class="movie-rating"><i class="fa-solid fa-star"></i>${movie.rating}</span>
          <span>${movie.bookingRate}% 예매율</span>
        </div>
        <a href="#" class="btn-book" onclick="return false;">예매하기</a>
      </div>
    </div>
  `;
}

function renderMovies() {
  const grid     = $('#movies-grid');
  const loadBtn  = $('#load-more-btn');
  const infoText = $('#load-more-info');
  if (!grid) return;

  const toShow = filteredMovies.slice(0, visibleCount);

  if (toShow.length === 0) {
    grid.innerHTML = `
      <div class="no-results">
        <i class="fa-solid fa-film"></i>
        <p>검색 결과가 없습니다.</p>
      </div>
    `;
    loadBtn && loadBtn.classList.add('hidden');
    infoText && (infoText.textContent = '');
    return;
  }

  grid.innerHTML = toShow.map(renderMovieCard).join('');

  const total      = filteredMovies.length;
  const capped     = Math.min(total, MAX_COUNT);
  const hasMore    = visibleCount < capped;

  if (loadBtn) loadBtn.classList.toggle('hidden', !hasMore);
  if (infoText) {
    infoText.textContent = hasMore
      ? `${visibleCount} / ${capped}개 표시 중`
      : `전체 ${toShow.length}개 표시됨`;
  }
}

/* ===========================
   더보기 버튼
   =========================== */
function initLoadMore() {
  const btn = $('#load-more-btn');
  if (!btn) return;
  btn.addEventListener('click', () => {
    visibleCount = Math.min(visibleCount + PAGE_SIZE, MAX_COUNT);
    renderMovies();
    btn.innerHTML = `더보기 <i class="fa-solid fa-chevron-down"></i>`;
  });
}

/* ===========================
   영화 검색 필터
   =========================== */
function initSearch() {
  const input = $('#movie-search');
  if (!input) return;

  let debounceTimer;

  input.addEventListener('input', () => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      const query = input.value.trim().toLowerCase();
      filteredMovies = query
        ? ALL_MOVIES.filter(m => m.title.toLowerCase().includes(query))
        : [...ALL_MOVIES];
      visibleCount = PAGE_SIZE;
      renderMovies();
    }, 220);
  });

  const searchBtn = $('.search-btn');
  searchBtn && searchBtn.addEventListener('click', () => {
    const query = input.value.trim().toLowerCase();
    filteredMovies = query
      ? ALL_MOVIES.filter(m => m.title.toLowerCase().includes(query))
      : [...ALL_MOVIES];
    visibleCount = PAGE_SIZE;
    renderMovies();
    input.focus();
  });

  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') searchBtn && searchBtn.click();
  });
}

/* ===========================
   헤더 스크롤 효과
   =========================== */
function initHeaderScroll() {
  const header = $('#main-header');
  if (!header) return;
  window.addEventListener('scroll', () => {
    header.classList.toggle('scrolled', window.scrollY > 10);
  }, { passive: true });
}

/* ===========================
   모바일 메뉴
   =========================== */
function initMobileMenu() {
  const btn = $('#mobile-menu-btn');
  const gnb = $('#gnb');
  if (!btn || !gnb) return;

  btn.addEventListener('click', () => {
    const isOpen = gnb.classList.toggle('open');
    btn.innerHTML = isOpen
      ? '<i class="fa-solid fa-xmark"></i>'
      : '<i class="fa-solid fa-bars"></i>';
  });

  // 모바일 드롭다운: 클릭으로 열기
  $$('.gnb-item.has-dropdown .gnb-link').forEach((link) => {
    link.addEventListener('click', (e) => {
      if (window.innerWidth <= 768) {
        e.preventDefault();
        const item = link.closest('.gnb-item');
        item.classList.toggle('open');
      }
    });
  });
}

/* ===========================
   히어로 스크롤 힌트 클릭
   =========================== */
function initHeroScroll() {
  const hint = $('.hero-scroll-hint');
  if (!hint) return;
  hint.addEventListener('click', () => {
    const moviesSection = $('#movies');
    moviesSection && moviesSection.scrollIntoView({ behavior: 'smooth' });
  });
  hint.style.cursor = 'pointer';
}

/* ===========================
   초기화
   =========================== */
document.addEventListener('DOMContentLoaded', () => {
  initPopup();
  renderMovies();
  initLoadMore();
  initSearch();
  initHeaderScroll();
  initMobileMenu();
  initHeroScroll();
});
