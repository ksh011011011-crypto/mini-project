'use strict';

/* ===========================
   팝업 제어
   =========================== */
function initPopup() {
  const overlay  = document.getElementById('popup-overlay');
  const closeBtn = document.getElementById('popup-close-btn');
  const closeX   = document.getElementById('popup-close-x');
  const noToday  = document.getElementById('popup-no-today-check');

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
   헤더 스크롤 효과
   =========================== */
function initHeaderScroll() {
  const header = document.getElementById('main-header');
  if (!header) return;
  window.addEventListener('scroll', () => {
    header.classList.toggle('scrolled', window.scrollY > 10);
  }, { passive: true });
}

/* ===========================
   모바일 메뉴
   =========================== */
function initMobileMenu() {
  const btn = document.getElementById('mobile-menu-btn');
  const gnb = document.getElementById('gnb');
  if (!btn || !gnb) return;

  btn.addEventListener('click', () => {
    const isOpen = gnb.classList.toggle('open');
    btn.innerHTML = isOpen
      ? '<i class="fa-solid fa-xmark"></i>'
      : '<i class="fa-solid fa-bars"></i>';
  });

  document.querySelectorAll('.gnb-item.has-dropdown .gnb-link').forEach((link) => {
    link.addEventListener('click', (e) => {
      if (window.innerWidth > 768) return;
      const href = (link.getAttribute('href') || '').trim();
      // 실제 이동 URL이 있으면 링크 그대로 동작 (예매/영화/홈 등)
      if (href && href !== '#' && !href.startsWith('#')) return;
      e.preventDefault();
      link.closest('.gnb-item').classList.toggle('open');
    });
  });
}

/* ===========================
   히어로 스크롤 힌트
   =========================== */
function initHeroScroll() {
  const hint = document.querySelector('.hero-scroll-hint');
  if (!hint) return;
  hint.style.cursor = 'pointer';
  hint.addEventListener('click', () => {
    const moviesSection = document.getElementById('movies');
    moviesSection && moviesSection.scrollIntoView({ behavior: 'smooth' });
  });
}

/* ===========================
   더보기 (Thymeleaf AJAX)
   =========================== */
function initLoadMore() {
  const btn = document.getElementById('load-more-btn');
  if (!btn) return;

  btn.addEventListener('click', async () => {
    const page    = parseInt(btn.dataset.page) || 1;
    const status  = btn.dataset.status || 'SHOWING';
    const grid    = document.getElementById('movies-grid');
    const maxPage = 10; // 최대 100개 / 10개 = 10 페이지

    if (page >= maxPage) {
      btn.classList.add('hidden');
      document.getElementById('load-more-info').textContent = '전체 영화가 표시되었습니다.';
      return;
    }

    btn.innerHTML = '로딩 중... <i class="fa-solid fa-spinner fa-spin"></i>';

    try {
      const response = await fetch(`/movies/more?status=${status}&page=${page}`);
      const html = await response.text();
      const parser = new DOMParser();
      const doc = parser.parseFromString(html, 'text/html');
      const cards = doc.querySelectorAll('.movie-card');

      if (cards.length === 0) {
        btn.classList.add('hidden');
        document.getElementById('load-more-info').textContent = '더 이상 영화가 없습니다.';
        return;
      }

      cards.forEach(card => grid.appendChild(card));

      const newPage = page + 1;
      btn.dataset.page = newPage;
      btn.innerHTML = '더보기 <i class="fa-solid fa-chevron-down"></i>';

      const shown = (newPage) * 10;
      document.getElementById('load-more-info').textContent = `${shown} / 100개 표시 중`;

      if (newPage >= maxPage) {
        btn.classList.add('hidden');
        document.getElementById('load-more-info').textContent = `전체 ${shown}개 표시됨`;
      }
    } catch(e) {
      btn.innerHTML = '더보기 <i class="fa-solid fa-chevron-down"></i>';
    }
  });
}

/* ===========================
   영화 검색 (서버 사이드)
   =========================== */
function initSearch() {
  const input    = document.getElementById('movie-search');
  const searchBtn = document.getElementById('search-btn');
  const grid     = document.getElementById('movies-grid');
  const loadMore = document.getElementById('load-more-btn');
  const infoText = document.getElementById('load-more-info');

  if (!input) return;

  let debounceTimer;

  async function doSearch(keyword) {
    if (!keyword) {
      location.reload();
      return;
    }

    try {
      const resp = await fetch('/api/movies/search?keyword=' + encodeURIComponent(keyword));
      const movies = await resp.json();

      if (!grid) return;

      if (movies.length === 0) {
        grid.innerHTML = `
          <div class="no-results" style="grid-column:1/-1; text-align:center; padding:60px; color:var(--text-muted);">
            <i class="fa-solid fa-film" style="font-size:3rem; opacity:.2; display:block; margin-bottom:12px;"></i>
            '<strong>${keyword}</strong>' 검색 결과가 없습니다.
          </div>`;
        loadMore && loadMore.classList.add('hidden');
        return;
      }

      grid.innerHTML = movies.map((m, i) => `
        <div class="movie-card">
          <div class="movie-poster" style="background:hsl(${m.id * 37 % 360},35%,22%);">
            <div class="movie-poster-placeholder">
              <i class="fa-solid fa-clapperboard"></i>
              <span>${m.genre}</span>
            </div>
            ${m.rank > 0 ? `<div class="movie-rank-badge ${m.rank <= 3 ? 'top3' : ''}">${m.rank}</div>` : ''}
          </div>
          <div class="movie-info">
            <p class="movie-title">${m.title}</p>
            <div class="movie-meta">
              <span class="movie-rating"><i class="fa-solid fa-star"></i>${m.rating}</span>
              <span>${m.bookingRate}% 예매율</span>
            </div>
            <a href="/booking?movieId=${m.id}" class="btn-book">예매하기</a>
          </div>
        </div>
      `).join('');

      loadMore && loadMore.classList.add('hidden');
      infoText && (infoText.textContent = `'${keyword}' 검색 결과: ${movies.length}개`);
    } catch(e) {
      console.error('검색 오류', e);
    }
  }

  input.addEventListener('input', () => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => doSearch(input.value.trim()), 300);
  });

  searchBtn && searchBtn.addEventListener('click', () => doSearch(input.value.trim()));
  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') doSearch(input.value.trim());
  });
}

/* ===========================
   극장/결제 카드 선택 스타일 보조
   =========================== */
function initCardSelects() {
  document.querySelectorAll('.theater-card').forEach(card => {
    card.style.cssText += 'padding:12px; border-radius:8px; border:1px solid rgba(255,255,255,.1); cursor:pointer; text-align:center; transition:all .2s; background:rgba(255,255,255,.05);';
  });

  // radio 변경 시 선택 상태 시각화
  document.querySelectorAll('input[type="radio"]').forEach(radio => {
    const updateStyle = () => {
      const label = radio.closest('label');
      if (!label) return;
      const card = label.querySelector('.theater-card');
      if (!card) return;
      if (radio.checked) {
        card.style.background = 'rgba(228,180,0,.15)';
        card.style.borderColor = 'var(--gold)';
        card.style.color = 'var(--gold)';
      } else {
        card.style.background = 'rgba(255,255,255,.05)';
        card.style.borderColor = 'rgba(255,255,255,.1)';
        card.style.color = '';
      }
    };
    radio.addEventListener('change', () => {
      document.querySelectorAll(`input[name="${radio.name}"]`).forEach(r => {
        const lbl = r.closest('label');
        const card = lbl?.querySelector('.theater-card');
        if (card) { card.style.background='rgba(255,255,255,.05)'; card.style.borderColor='rgba(255,255,255,.1)'; card.style.color=''; }
      });
      updateStyle();
    });
    if (radio.checked) updateStyle();
  });
}

/* ===========================
   Flash 메시지 자동 숨기기
   =========================== */
function initAlerts() {
  document.querySelectorAll('.alert-box').forEach(el => {
    setTimeout(() => {
      el.style.opacity = '0';
      el.style.transition = 'opacity 0.5s';
      setTimeout(() => el.remove(), 500);
    }, 4000);
  });
}

/* ===========================
   초기화
   =========================== */
document.addEventListener('DOMContentLoaded', () => {
  initPopup();
  initHeaderScroll();
  initMobileMenu();
  initHeroScroll();
  initLoadMore();
  initSearch();
  initCardSelects();
  initAlerts();
});
