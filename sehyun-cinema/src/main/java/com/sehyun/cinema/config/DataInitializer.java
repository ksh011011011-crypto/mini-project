package com.sehyun.cinema.config;

import com.sehyun.cinema.entity.*;
import com.sehyun.cinema.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    /** 데모 계정 ksh011 비밀번호 (기동 시 항상 이 값으로 동기화) */
    private static final String KSH011_PASSWORD = "lms990302";

    /** 박스오피스·개봉일 기준일 (시연/기획: 2026년 3월 말 시점) */
    private static final LocalDate BOX_OFFICE_REF = LocalDate.of(2026, 3, 31);

    /** 2026-03-31 시점 주간 박스오피스 TOP10 — 최신 흥행·개봉작 중심 (순서 = 1~10위) */
    private static final List<String> BOX_OFFICE_TOP10 = List.of(
        "왕과 사는 남자", "프로젝트 헤일메리", "인사이드 아웃 2", "호퍼스", "휴민트",
        "범죄도시 4", "명탐정 코난: 세기말의 마술사", "파묘", "신의악단", "서울의 봄"
    );

    private static final int[] BOX_OFFICE_BOOKING_PCT = {
        96, 84, 79, 72, 65, 60, 55, 50, 42, 36
    };

    /** 개봉예정작 표시 순서(가까운 개봉일 우선) + 기대 예매율(%) */
    private static final List<String> UPCOMING_ORDER = List.of(
        "위키드", "미션 임파서블: 파이널 레코닝", "모아나 2", "캡틴 아메리카: 브레이브 뉴 월드",
        "오징어 게임: 더 무비", "분노의 질주: 파이널", "아바타 3"
    );

    private static final int[] UPCOMING_BOOKING_PCT = { 22, 20, 18, 16, 14, 12, 10 };

    /** 개봉예정작 고정 개봉일 (기준일 이후) */
    private static final Map<String, LocalDate> UPCOMING_RELEASE_BY_TITLE = Map.ofEntries(
        Map.entry("위키드", LocalDate.of(2026, 4, 2)),
        Map.entry("미션 임파서블: 파이널 레코닝", LocalDate.of(2026, 4, 9)),
        Map.entry("모아나 2", LocalDate.of(2026, 4, 16)),
        Map.entry("캡틴 아메리카: 브레이브 뉴 월드", LocalDate.of(2026, 4, 23)),
        Map.entry("오징어 게임: 더 무비", LocalDate.of(2026, 5, 7)),
        Map.entry("분노의 질주: 파이널", LocalDate.of(2026, 6, 11)),
        Map.entry("아바타 3", LocalDate.of(2026, 12, 17))
    );

    private final MemberRepository memberRepository;
    private final MovieRepository movieRepository;
    private final NoticeRepository noticeRepository;
    private final FAQRepository faqRepository;
    private final PasswordEncoder passwordEncoder;

    /** https://www.themoviedb.org/settings/api 에서 발급 시, 맵에 없는 제목도 검색으로 포스터 보강 */
    @Value("${tmdb.api.key:}")
    private String tmdbApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 제목(정확 일치) → TMDB w500 포스터 */
    private static final Map<String, String> POSTER = buildPosterMap();

    /** 제목(정확 일치) → YouTube embed URL (예고편) */
    private static final Map<String, String> TRAILER = buildTrailerMap();

    /** 맵에 없을 때만 사용 — 특정 영화 포스터와 혼동되지 않는 중립 플레이스홀더 */
    private static final String DEFAULT_POSTER =
        "https://placehold.co/500x750/1a1a2e/666666?text=POSTER";

    private static final String YT_EMBED = "https://www.youtube.com/embed/";

    /** DB·입력 변형 제목 → {@link #POSTER}의 정식 키 */
    private static final Map<String, String> POSTER_ALIASES = buildPosterAliasMap();

    private static Map<String, String> buildPosterAliasMap() {
        LinkedHashMap<String, String> a = new LinkedHashMap<>();
        a.put("인사이드아웃2", "인사이드 아웃 2");
        a.put("인사이드 아웃2", "인사이드 아웃 2");
        a.put("인사이드아웃 2", "인사이드 아웃 2");
        a.put("인사이드 아웃2", "인사이드 아웃 2");
        a.put("Inside Out 2", "인사이드 아웃 2");
        a.put("듄파트2", "듄: 파트 투");
        a.put("듄 파트2", "듄: 파트 투");
        a.put("듄:파트 투", "듄: 파트 투");
        a.put("Dune Part Two", "듄: 파트 투");
        a.put("원카", "윙카");
        a.put("Wonka", "윙카");
        a.put("범죄도시4", "범죄도시 4");
        a.put("코난", "명탐정 코난: 세기말의 마술사");
        a.put("미션임파서블", "미션 임파서블: 파이널 레코닝");
        a.put("아바타3", "아바타 3");
        return Map.copyOf(a);
    }

    private static Map<String, String> buildPosterMap() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        /* 극장판 포스터 — static/images/posters/meitantei-conan.png (사용자 제공) */
        m.put("명탐정 코난: 세기말의 마술사", "/images/posters/meitantei-conan.png");
        m.put("미션 임파서블: 파이널 레코닝", "https://image.tmdb.org/t/p/w500/z53D72EAOxGRqdr7KXXWp9dJiDe.jpg");
        m.put("캡틴 아메리카: 브레이브 뉴 월드", "https://image.tmdb.org/t/p/w500/pzIddUEMWhWzfvLI3TwxUG2wGoi.jpg");
        m.put("오징어 게임: 더 무비", "https://image.tmdb.org/t/p/w500/dDlEmu3EZ0Pgg93K2SVNLCjCSvE.jpg");
        /* 공식 포스터 — static/images/posters/theres-still-tomorrow.png (사용자 제공) */
        m.put("우리에게는 아직 내일이 있다", "/images/posters/theres-still-tomorrow.png");
        m.put("분노의 질주: 파이널", "https://image.tmdb.org/t/p/w500/fiVW06jE7z9YnO4trhaMEdclSiC.jpg");
        m.put("앤트맨과 와스프: 퀀터매니아", "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg");
        m.put("스파이더맨: 어크로스 더 유니버스", "https://image.tmdb.org/t/p/w500/gj6Gy5VQlL4vO3q9dZcNXZjIiID.jpg");
        /* 공식 포스터 — static/images/posters/dune-part-two.png (사용자 제공) */
        m.put("듄: 파트 투", "/images/posters/dune-part-two.png");
        m.put("아바타: 물의 길", "https://image.tmdb.org/t/p/w500/t6HIqrRAclMCA60NsSmeqe9RmNV.jpg");
        /* 오피스 스페이스(TMDB #1542) — 직장 코미디 대표 포스터 */
        /* 공식 포스터 — static/images/posters/mad-dance-office.png (사용자 제공) */
        m.put("매드 댄스 오피스", "/images/posters/mad-dance-office.png");
        /* 공식 포스터 — static/images/posters/chosok-5centimeter.png (사용자 제공) */
        m.put("초속 5센티미터", "/images/posters/chosok-5centimeter.png");
        m.put("프로젝트 헤일메리", "https://image.tmdb.org/t/p/w500/qqGpVVZk2KD1lAvccgTU4Z6nh1H.jpg");
        m.put("왕과 사는 남자", "https://image.tmdb.org/t/p/w500/zEH1FQTEnRY05i8gQIYdR10Vp92.jpg");
        /* 호퍼스(2026) TMDB #1327819 공식 포스터 — schema.org image */
        m.put("호퍼스", "https://image.tmdb.org/t/p/w500/vJu9THzQ26Q5sWOVnhOkuRH5M1P.jpg");
        m.put("인사이드 아웃 2", "https://image.tmdb.org/t/p/w500/vpnVM9B6NMmQpWeZvzLvDESb2QY.jpg");
        /* 공식 포스터 — static/images/posters/humint.png (사용자 제공) */
        m.put("휴민트", "/images/posters/humint.png");
        /* 공식 포스터 — static/images/posters/sinui-akdan.png (사용자 제공) */
        m.put("신의악단", "/images/posters/sinui-akdan.png");
        /* 공식 포스터 — static/images/posters/crime-city-4.png (사용자 제공) */
        m.put("범죄도시 4", "/images/posters/crime-city-4.png");
        /* 공식 포스터 — static/images/posters/pamyo.png (사용자 제공) */
        m.put("파묘", "/images/posters/pamyo.png");
        /* 공식 포스터 — static/images/posters/seoul-spring.png (사용자 제공) */
        m.put("서울의 봄", "/images/posters/seoul-spring.png");
        m.put("기생충", "https://image.tmdb.org/t/p/w500/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg");
        m.put("인터스텔라", "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg");
        /* Hamnet(2025) TMDB — 기동 시 curl 200 확인된 경로 */
        /* 공식 포스터 — static/images/posters/hamnet.png (사용자 제공) */
        m.put("햄넷", "/images/posters/hamnet.png");
        m.put("베테랑 2", "https://image.tmdb.org/t/p/w500/aosm8NMQ3UyoBVpSxyimorCQykC.jpg");
        /* 공식 포스터 — static/images/posters/extreme-job.png (사용자 제공) */
        m.put("극한직업", "/images/posters/extreme-job.png");
        /* 공식 포스터 — static/images/posters/myeongryang.png (사용자 제공) */
        m.put("명량", "/images/posters/myeongryang.png");
        /* 공식 포스터 — static/images/posters/busanhaeng.png (사용자 제공) */
        m.put("부산행", "/images/posters/busanhaeng.png");
        /* 공식 포스터 — static/images/posters/taegeukgi.png (사용자 제공) */
        m.put("태극기 휘날리며", "/images/posters/taegeukgi.png");
        /* 공식 포스터 — static/images/posters/byeonhoin.png (사용자 제공) */
        m.put("변호인", "/images/posters/byeonhoin.png");
        /* 공식 포스터 — static/images/posters/assassination.png (사용자 제공) */
        m.put("암살", "/images/posters/assassination.png");
        /* 공식 포스터 — 아바타: 불과 재 (사용자 제공) → static/images/posters/avatar-3.png */
        m.put("아바타 3", "/images/posters/avatar-3.png");
        /* 공식 포스터 — static/images/posters/wicked.png (사용자 제공) */
        m.put("위키드", "/images/posters/wicked.png");
        /* 공식 포스터 — static/images/posters/moana-2.png (사용자 제공) */
        m.put("모아나 2", "/images/posters/moana-2.png");
        /* 공식 포스터 — static/images/posters/bride.png (사용자 제공) */
        m.put("브라이드!", "/images/posters/bride.png");
        /* 공식 포스터 — static/images/posters/woorineun-maeilmaeil.png (사용자 제공) */
        m.put("우리는 매일매일", "/images/posters/woorineun-maeilmaeil.png");
        m.put("소년들", "https://image.tmdb.org/t/p/w500/rgyKMJkZfDdcfjfIPfNSgVJ7S44.jpg");
        /* 공식 포스터 — static/images/posters/next-sohee.png (사용자 제공) */
        m.put("다음 소희", "/images/posters/next-sohee.png");
        m.put("오펜하이머", "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg");
        m.put("듄2", "/images/posters/dune-part-two.png");
        m.put("듄", "https://image.tmdb.org/t/p/w500/d5NXSklXo0qyIYkgV94XAgMIckC.jpg");
        m.put("탑건: 매버릭", "https://image.tmdb.org/t/p/w500/62HCnUTz_9PVZiO6ur1BWP07nBP.jpg");
        m.put("한국이 싫어서", "https://image.tmdb.org/t/p/w500/52AfXWuXCHn3KdD5q2ZBbm87kTr.jpg");
        /* 윙카 = Wonka(2023) TMDB #787699 공식 포스터 — 소울 포스터와 혼동 방지 */
        m.put("윙카", "https://image.tmdb.org/t/p/w500/lQ4cwauq2jeTkka9RvdMBTVPLMH.jpg");
        return Map.copyOf(m);
    }

    /** 공식 예고편(YouTube embed). 제목은 {@link #POSTER}와 동일하게 맞춤. ID는 공식 채널 기준 11자리. */
    private static Map<String, String> buildTrailerMap() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        m.put("명탐정 코난: 세기말의 마술사", YT_EMBED + "eOrNdBpGMv8");
        m.put("미션 임파서블: 파이널 레코닝", YT_EMBED + "avjq9y8nYk0");
        m.put("캡틴 아메리카: 브레이브 뉴 월드", YT_EMBED + "1pHDWnXmK7Y");
        m.put("오징어 게임: 더 무비", YT_EMBED + "oqxAJKy0ii4");
        m.put("우리에게는 아직 내일이 있다", YT_EMBED + "Z5hAvdF6xQA");
        m.put("분노의 질주: 파이널", YT_EMBED + "aOb15GVJZxA");
        m.put("앤트맨과 와스프: 퀀터매니아", YT_EMBED + "ZlNFpri-Y40");
        m.put("스파이더맨: 어크로스 더 유니버스", YT_EMBED + "shW9i6k8cHA");
        m.put("듄: 파트 투", YT_EMBED + "U2Qp5pL3ovA");
        m.put("아바타: 물의 길", YT_EMBED + "d9MyW72XqFI");
        m.put("매드 댄스 오피스", YT_EMBED + "DYw5M6M0Qcg");
        m.put("초속 5센티미터", YT_EMBED + "wdMlpatJwmI");
        m.put("프로젝트 헤일메리", YT_EMBED + "m08TxIsFTRI");
        m.put("왕과 사는 남자", YT_EMBED + "9sxEZuJskvM");
        m.put("호퍼스", YT_EMBED + "lKd8_anPJ1Y");
        m.put("휴민트", YT_EMBED + "qSqVVswa420");
        m.put("신의악단", YT_EMBED + "6COmYeLsz4c");
        m.put("범죄도시 4", YT_EMBED + "aOb15GVJZxA");
        m.put("파묘", YT_EMBED + "5iRZLgrUuZM");
        m.put("서울의 봄", YT_EMBED + "-AZ7cnwn2YI");
        m.put("기생충", YT_EMBED + "5P8R2Pi4WwA");
        m.put("인터스텔라", YT_EMBED + "2LqzF5WauAw");
        m.put("햄넷", YT_EMBED + "bK6ldkk0onI");
        m.put("베테랑 2", YT_EMBED + "qSqVVswa420");
        m.put("극한직업", YT_EMBED + "5P8R2Pi4WwA");
        m.put("명량", YT_EMBED + "nThIjHXFyQo");
        m.put("부산행", YT_EMBED + "d9MyW72XqFI");
        m.put("태극기 휘날리며", YT_EMBED + "2LqzF5WauAw");
        m.put("변호인", YT_EMBED + "5P8R2Pi4WwA");
        m.put("암살", YT_EMBED + "U2Qp5pL3ovA");
        m.put("아바타 3", YT_EMBED + "d9MyW72XqFI");
        m.put("위키드", YT_EMBED + "6COmYeLsz4c");
        m.put("모아나 2", YT_EMBED + "hDZ7y8RP5HE");
        m.put("브라이드!", YT_EMBED + "Z5hAvdF6xQA");
        m.put("우리는 매일매일", YT_EMBED + "5P8R2Pi4WwA");
        m.put("소년들", YT_EMBED + "bK6ldkk0onI");
        m.put("다음 소희", YT_EMBED + "m08TxIsFTRI");
        m.put("오펜하이머", YT_EMBED + "bK6ldkk0onI");
        m.put("듄2", YT_EMBED + "U2Qp5pL3ovA");
        m.put("듄", YT_EMBED + "nThIjHXFyQo");
        m.put("탑건: 매버릭", YT_EMBED + "qSqVVswa420");
        m.put("한국이 싫어서", YT_EMBED + "5P8R2Pi4WwA");
        m.put("윙카", YT_EMBED + "otNh9bTjXWg");
        m.put("인사이드 아웃 2", YT_EMBED + "LEjhY15eCx0");
        return Map.copyOf(m);
    }

    /** 제목만으로 표시 가능한 플레이스홀더(TMDB 키 없을 때 최후 수단) */
    private static String defaultPosterForTitle(String title) {
        try {
            String s = title.length() > 38 ? title.substring(0, 38) + "…" : title;
            return "https://placehold.co/500x750/1a1a2e/e4b400?text="
                + URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return DEFAULT_POSTER;
        }
    }

    /**
     * 별칭·공백·부분 일치까지 고려해 TMDB w500 URL 결정.
     * API 키 없이도 맵에 있는 작품은 실제 포스터, 없으면 제목이 찍힌 플레이스홀더.
     */
    private static String resolvePosterUrl(String rawTitle) {
        String t = rawTitle != null ? rawTitle.trim() : "";
        if (t.isEmpty()) {
            return defaultPosterForTitle("영화");
        }
        if (POSTER.containsKey(t)) {
            return POSTER.get(t);
        }
        String viaAlias = POSTER_ALIASES.get(t);
        if (viaAlias != null && POSTER.containsKey(viaAlias)) {
            return POSTER.get(viaAlias);
        }
        String compact = t.replaceAll("\\s+", "");
        for (Map.Entry<String, String> e : POSTER.entrySet()) {
            if (e.getKey().replaceAll("\\s+", "").equalsIgnoreCase(compact)) {
                return e.getValue();
            }
        }
        // 부분 일치: DB 제목·맵 키 길이 차이 대응 (긴 키 우선)
        String bestUrl = null;
        int bestKeyLen = 0;
        for (Map.Entry<String, String> e : POSTER.entrySet()) {
            String k = e.getKey();
            if (k.length() < 3 || t.length() < 3) {
                continue;
            }
            if (t.contains(k) || k.contains(t)) {
                if (k.length() > bestKeyLen) {
                    bestKeyLen = k.length();
                    bestUrl = e.getValue();
                }
            }
        }
        if (bestUrl != null) {
            return bestUrl;
        }
        return defaultPosterForTitle(t);
    }

    /** TRAILER 맵 조회용 정식 키 (없으면 null) */
    private static String resolveTrailerLookupKey(String rawTitle) {
        String t = rawTitle != null ? rawTitle.trim() : "";
        if (t.isEmpty()) {
            return null;
        }
        if (TRAILER.containsKey(t)) {
            return t;
        }
        String viaAlias = POSTER_ALIASES.get(t);
        if (viaAlias != null && TRAILER.containsKey(viaAlias)) {
            return viaAlias;
        }
        String compact = t.replaceAll("\\s+", "");
        for (String k : TRAILER.keySet()) {
            if (k.replaceAll("\\s+", "").equalsIgnoreCase(compact)) {
                return k;
            }
        }
        return null;
    }

    @Override
    public void run(String... args) {
        initMembers();
        forceUpdateKsh011();   // ksh011 비밀번호 + LVIP 보장
        initMovies();
        addExtraMovies();
        applyPosterToEveryMovie(); // 모든 영화: 제목→TMDB 맵 적용, 없으면 기본 포스터(빈/플레이스홀더/깨진 URL 일괄 해결)
        try {
            enrichPostersFromTmdbSearchIfConfigured(); // API 키가 있으면 TMDB 검색으로 플레이스홀더만 보강
        } catch (Exception ignored) {
            // 네트워크·TMDB 오류 시에도 앱 기동은 계속
        }
        applyTrailerToEveryMovie(); // 제목→YouTube embed 예고편 URL
        reconcileMovieStatuses(); // 기존 DB(PostgreSQL 등)에서 잘못된 상영/개봉예정/아르떼 상태 복구
        syncReleaseDatesToReference(); // 개봉일을 2026-03-31 기준 상영/예정으로 정렬
        syncBoxOfficeAndUpcomingForRef(); // TOP10·예매율·개봉예정 순서 반영
        initNotices();
        initFAQs();
    }

    /** ksh011: 없으면 생성, 있으면 비밀번호·등급 매 기동 시 동기화 (PostgreSQL 등 기존 DB 대응) */
    private void forceUpdateKsh011() {
        if (memberRepository.findByUsername("ksh011").isEmpty()) {
            createIfAbsent("ksh011", "lms990302!!", "김세현", "lms990302@naver.com", "01033310292", "LVIP", 500000, "ROLE_ADMIN");
        }
        memberRepository.findByUsername("ksh011").ifPresent(m -> {
            m.setPassword(passwordEncoder.encode("lms990302!!"));
            m.setGrade("LVIP");
            m.setTotalSpent(500000);
            m.setRole("ROLE_ADMIN");
            memberRepository.save(m);
        });
    }

    /** 특정 계정의 비밀번호를 강제로 최신값으로 업데이트 */
    private void forceUpdatePassword(String username, String rawPw) {
        memberRepository.findByUsername(username).ifPresent(member -> {
            member.setPassword(passwordEncoder.encode(rawPw));
            memberRepository.save(member);
        });
    }

    // ──────────────────────────────────────────────────────────
    //  회원 초기화 (없으면 항상 생성)
    // ──────────────────────────────────────────────────────────
    private void initMembers() {
        createIfAbsent("ksh011",  KSH011_PASSWORD, "김세현", "lms990302@naver.com", "01033310292", "LVIP", 500000, "ROLE_ADMIN");
        createIfAbsent("admin",   "admin1234",   "관리자", "admin@sehyun.com",    "01000000000", "LVIP", 500000, "ROLE_ADMIN");
        createIfAbsent("user1",   "user1234",    "홍길동", "user1@test.com",      "01012345678", "VIP",  280000, "ROLE_USER");
    }

    private void createIfAbsent(String username, String rawPw, String name,
                                 String email, String phone, String grade,
                                 int spent, String role) {
        if (memberRepository.findByUsername(username).isEmpty()) {
            memberRepository.save(Member.builder()
                .username(username).password(passwordEncoder.encode(rawPw))
                .name(name).email(email).phone(phone)
                .grade(grade).totalSpent(spent).role(role).build());
        }
    }

    // ──────────────────────────────────────────────────────────
    //  2026 최신 박스오피스 영화 (없을 때만)
    // ──────────────────────────────────────────────────────────
    private void initMovies() {
        if (movieRepository.count() > 0) return;

        // { 제목, 장르, 상태(SHOWING/UPCOMING/ARTE), 순위(null가능), 연령등급, 포스터URL, 평점, 예매율 }
        Object[][] movies = {
          // ── 현재상영 TOP 10 (2026 3월 최신) ──────────────────────
          {"왕과 사는 남자",    "사극/드라마",    "SHOWING",  1, "12세이상",
           "https://image.tmdb.org/t/p/w500/zEH1FQTEnRY05i8gQIYdR10Vp92.jpg", 7.5, 96,
           "계유정난으로 왕위에서 쫓겨난 이홍위. 그를 지키게 된 촌장 엄흥도와의 이야기. 누적 1,561만 관객 돌파!"},
          {"프로젝트 헤일메리", "SF/어드벤처",   "SHOWING",  2, "12세이상",
           "https://image.tmdb.org/t/p/w500/qqGpVVZk2KD1lAvccgTU4Z6nh1H.jpg", 8.2, 74,
           "눈을 떠보니 우주 한복판. 지구를 구할 마지막 임무를 띤 과학 교사와 외계 생명체의 만남."},
          {"명탐정 코난: 세기말의 마술사", "애니메이션/미스터리", "SHOWING", 3, "전체이용가",
           "/images/posters/meitantei-conan.png", 8.8, 52,
           "어느날 갑자기 등장한 마술사 '잭 더 키드'. 코난이 신비로운 트릭을 파헤치는 명탐정 코난 극장판."},
          {"호퍼스",            "애니메이션",    "SHOWING",  4, "전체이용가",
           "https://image.tmdb.org/t/p/w500/vJu9THzQ26Q5sWOVnhOkuRH5M1P.jpg", 7.8, 42,
           "픽사 30번째 장편. 인간의 정신을 동물 로봇에 옮기는 프로젝트 '호핑'을 둘러싼 블랙코미디."},
          {"휴민트",            "첩보/액션",     "SHOWING",  5, "15세이상",
           "/images/posters/humint.png", 7.1, 38,
           "류승완 감독. 블라디보스토크를 배경으로 펼쳐지는 국정원 블랙요원의 구출 작전."},
          {"신의악단",          "드라마/음악",   "SHOWING",  6, "전체이용가",
           "/images/posters/sinui-akdan.png", 8.1, 30,
           "세계적인 오케스트라에 들어간 음악 신동과 지휘자의 갈등과 성장을 그린 감동 드라마."},
          {"범죄도시 4",        "액션/범죄",     "SHOWING",  7, "15세이상",
           "/images/posters/crime-city-4.png", 8.5, 88,
           "마석도(마동석)가 돌아왔다! 4번째 범죄도시. 1,149만 관객 돌파."},
          {"파묘",              "미스터리/공포", "SHOWING",  8, "15세이상",
           "/images/posters/pamyo.png", 9.2, 82,
           "무당과 풍수사가 의문의 묘를 파헤치다 마주친 충격적 진실. 역대 20위 1,191만 관객."},
          {"서울의 봄",         "역사/드라마",   "SHOWING",  9, "15세이상",
           "/images/posters/seoul-spring.png", 9.5, 76,
           "1979년 12월 12일, 군사 반란을 막으려는 9시간의 사투. 1,312만 관객."},
          {"기생충",            "드라마/스릴러", "SHOWING", 10, "15세이상",
           "https://image.tmdb.org/t/p/w500/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg", 9.8, 72,
           "봉준호 감독. 칸 황금종려상 + 아카데미 4관왕. 1,031만 관객 한국 영화의 역사."},
          // ── 현재상영 추가 ─────────────────────────────────────
          {"인터스텔라",        "SF/드라마",     "SHOWING", null, "12세이상",
           "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg", 9.4, 65,
           "크리스토퍼 놀란 감독. 인류의 마지막 희망을 찾아 우주로 떠나는 감동적인 SF 명작."},
          {"매드 댄스 오피스",  "코미디",        "SHOWING", null, "12세이상",
           "/images/posters/mad-dance-office.png", 7.2, 28,
           "대한민국 직장인들의 애환을 담은 유쾌한 직장 코미디."},
          {"초속 5센티미터",    "애니메이션/로맨스","SHOWING",null,"전체이용가",
           "/images/posters/chosok-5centimeter.png", 9.0, 22,
           "신카이 마코토 감독. 멀어지는 두 사람 사이의 거리, 초속 5센티미터로 떨어지는 벚꽃처럼."},
          {"햄넷",              "드라마/시대극", "SHOWING", null, "12세이상",
           "/images/posters/hamnet.png", 8.0, 18,
           "셰익스피어의 비극 '햄릿'의 원형이 된 아들의 이야기. 감동적인 시대극."},
          {"베테랑 2",          "액션/범죄",     "SHOWING", null, "15세이상",
           "https://image.tmdb.org/t/p/w500/aosm8NMQ3UyoBVpSxyimorCQykC.jpg", 8.7, 70,
           "류승완 감독 × 황정민. 형사 서도철이 돌아온다! 베테랑 시리즈 두 번째."},
          // ── 개봉예정 ────────────────────────────────────────
          {"미션 임파서블: 파이널 레코닝", "액션/스파이", "UPCOMING", null, "12세이상",
           "https://image.tmdb.org/t/p/w500/z53D72EAOxGRqdr7KXXWp9dJiDe.jpg", 0.0, 0,
           "에단 헌트(톰 크루즈)의 마지막 임무. 시리즈 최종장."},
          {"아바타 3",          "SF/액션",       "UPCOMING", null, "12세이상",
           "/images/posters/avatar-3.png", 0.0, 0,
           "제임스 카메론 감독, 한국어 부제 아바타: 불과 재. 나비족의 새로운 이야기."},
          {"분노의 질주: 파이널", "액션",         "UPCOMING", null, "15세이상",
           "https://image.tmdb.org/t/p/w500/fiVW06jE7z9YnO4trhaMEdclSiC.jpg", 0.0, 0,
           "도미닉 토레토 패밀리의 진짜 마지막 이야기."},
          {"캡틴 아메리카: 브레이브 뉴 월드", "액션/SF", "UPCOMING", null, "12세이상",
           "https://image.tmdb.org/t/p/w500/pzIddUEMWhWzfvLI3TwxUG2wGoi.jpg", 0.0, 0,
           "새로운 캡틴 아메리카 샘 윌슨의 첫 번째 단독 모험."},
          {"오징어 게임: 더 무비", "스릴러/드라마","UPCOMING", null, "15세이상",
           "https://image.tmdb.org/t/p/w500/dDlEmu3EZ0Pgg93K2SVNLCjCSvE.jpg", 0.0, 0,
           "전 세계를 열광시킨 오징어 게임 IP의 극장판."},
          // ── 아르떼(Arte/독립) ───────────────────────────────
          {"브라이드!",         "로맨스/코미디", "ARTE", null, "12세이상",
           "/images/posters/bride.png", 7.3, 15,
           "결혼식 전날 도망친 신부의 엉뚱하고 유쾌한 하루를 담은 인디 로맨스."},
          {"우리는 매일매일",   "다큐멘터리",    "ARTE", null, "전체이용가",
           "/images/posters/woorineun-maeilmaeil.png", 8.4, 12,
           "평범한 사람들의 특별한 일상을 담은 감동적인 다큐멘터리."},
          {"우리에게는 아직 내일이 있다", "드라마/독립", "ARTE", null, "전체이용가",
           "/images/posters/theres-still-tomorrow.png", 9.1, 10,
           "이탈리아 독립영화계의 화제작. 여성의 연대와 자립을 그린 따뜻한 이야기."},
        };

        for (Object[] m : movies) {
            String title     = (String)  m[0];
            String genre     = (String)  m[1];
            String status    = (String)  m[2];
            Integer rank     = m[3] == null ? null : (Integer) m[3];
            String ageRating = (String)  m[4];
            String poster    = (String)  m[5];
            double rating    = (double)  m[6];
            int bookRate     = (int)     m[7];
            String desc      = (String)  m[8];

            Movie movie = Movie.builder()
                .title(title).genre(genre).status(status)
                .rankPosition(rank).ageRating(ageRating)
                .posterUrl(poster)
                .bookingRate(bookRate)
                .ratingAvg(rating)
                .heartCount((int)(Math.random() * 8000))
                .releaseDate("UPCOMING".equals(status)
                    ? BOX_OFFICE_REF.plusDays((int)(Math.random() * 120) + 10)
                    : BOX_OFFICE_REF.minusDays((int)(Math.random() * 90)))
                .runningTime(90 + (int)(Math.random() * 70))
                .description(desc)
                .build();
            movieRepository.save(movie);
        }
    }

    /** 기존 DB에 없는 신규 영화만 추가 */
    private void addExtraMovies() {
        Object[][] extra = {
          // ── 현재상영 추가분 ──────────────────────────────────
          {"극한직업",       "코미디/액션",   "SHOWING", null,"15세이상",
           "/images/posters/extreme-job.png", 9.1, 58,
           "마약반 형사들의 치킨집 위장 창업! 역대 2위 1,626만 관객의 전설적인 한국 코미디."},
          {"명량",           "역사/액션",     "SHOWING", null,"12세이상",
           "/images/posters/myeongryang.png", 9.3, 55,
           "이순신 장군의 명량해전. 역대 1위 1,761만 관객. 12척으로 330척을 이긴 기적의 전투."},
          {"부산행",         "액션/스릴러",   "SHOWING", null,"15세이상",
           "/images/posters/busanhaeng.png", 9.5, 62,
           "좀비 바이러스가 퍼진 KTX 열차 안. 한국 좀비물의 새 지평을 연 작품."},
          {"태극기 휘날리며","역사/드라마",   "SHOWING", null,"15세이상",
           "/images/posters/taegeukgi.png", 9.2, 48,
           "6.25 전쟁을 배경으로 한 형제의 이야기. 역대 5위 1,174만 관객."},
          {"변호인",         "드라마/법정",   "SHOWING", null,"15세이상",
           "/images/posters/byeonhoin.png", 9.4, 45,
           "송우석 변호사가 국가 권력에 맞서 싸우는 실화 기반 드라마."},
          {"암살",           "액션/역사",     "SHOWING", null,"15세이상",
           "/images/posters/assassination.png", 9.1, 50,
           "1933년 경성. 친일파 암살 작전에 투입된 저격수들의 이야기."},
          // ── 개봉예정 추가분 ──────────────────────────────────
          {"위키드",         "뮤지컬/판타지", "UPCOMING", null,"전체이용가",
           "/images/posters/wicked.png", 0.0, 0,
           "브로드웨이 최장 히트 뮤지컬의 영화화. 오즈의 마법사 이전 이야기."},
          {"모아나 2",       "애니메이션",    "UPCOMING", null,"전체이용가",
           "/images/posters/moana-2.png", 0.0, 0,
           "디즈니 애니메이션 모아나의 두 번째 모험. 더 넓은 바다로 나아가다."},
          // ── 아르떼 추가분 ────────────────────────────────────
          {"소년들",         "드라마/독립",   "ARTE", null,"12세이상",
           "https://image.tmdb.org/t/p/w500/rgyKMJkZfDdcfjfIPfNSgVJ7S44.jpg", 8.5, 8,
           "1999년 삼례 나라슈퍼 사건의 진실. 억울한 청소년들의 이야기."},
          {"다음 소희",      "드라마",        "ARTE", null,"15세이상",
           "/images/posters/next-sohee.png", 9.0, 9,
           "정주리 감독. 콜센터 실습생 소희의 비극. 현실을 직시하는 독립영화."},
          {"인사이드 아웃 2", "애니메이션/가족", "SHOWING", null, "전체이용가",
           "https://image.tmdb.org/t/p/w500/vpnVM9B6NMmQpWeZvzLvDESb2QY.jpg", 8.4, 22,
           "디즈니·픽사. 라일리의 새 감정들과 함께하는 성장 이야기."},
          {"듄: 파트 투", "SF/어드벤처", "SHOWING", null, "12세이상",
           "/images/posters/dune-part-two.png", 8.7, 48,
           "아트레이데스 가문과 프레멘의 운명을 건 대전. 데니 빌뇌브 감독 SF 대작 후속편."},
        };

        for (Object[] m : extra) {
            String title = (String) m[0];
            // 이미 있으면 skip
            if (movieRepository.existsByTitle(title)) continue;

            String genre     = (String)  m[1];
            String status    = (String)  m[2];
            Integer rank     = m[3] == null ? null : (Integer) m[3];
            String ageRating = (String)  m[4];
            String poster    = (String)  m[5];
            double rating    = (double)  m[6];
            int bookRate     = (int)     m[7];
            String desc      = (String)  m[8];

            movieRepository.save(Movie.builder()
                .title(title).genre(genre).status(status)
                .rankPosition(rank).ageRating(ageRating)
                .posterUrl(poster).bookingRate(bookRate).ratingAvg(rating)
                .heartCount((int)(Math.random() * 5000))
                .releaseDate("UPCOMING".equals(status)
                    ? BOX_OFFICE_REF.plusDays((int)(Math.random() * 120) + 10)
                    : BOX_OFFICE_REF.minusDays((int)(Math.random() * 90)))
                .runningTime(90 + (int)(Math.random() * 70))
                .description(desc).build());
        }
    }

    /**
     * 기존 DB 영화의 개봉일을 {@link #BOX_OFFICE_REF} 기준으로 맞춥니다.
     * 현재상영: 기준일 이전 90일 이내, 개봉예정: 기준일 이후, 아르떼: 기준일 전후 가까운 날짜.
     */
    private void syncReleaseDatesToReference() {
        movieRepository.findAll().forEach(movie -> {
            LocalDate target = computeReleaseDateForStatus(movie);
            if (target != null && !target.equals(movie.getReleaseDate())) {
                movie.setReleaseDate(target);
                movieRepository.save(movie);
            }
        });
    }

    private LocalDate computeReleaseDateForStatus(Movie movie) {
        String status = movie.getStatus();
        String title = movie.getTitle() != null ? movie.getTitle().trim() : "";
        long id = movie.getId() != null ? movie.getId() : 0L;
        int spread = (int) (Math.abs(id * 31) % 61);
        if ("SHOWING".equals(status)) {
            return BOX_OFFICE_REF.minusDays(1 + spread);
        }
        if ("UPCOMING".equals(status)) {
            LocalDate fixed = UPCOMING_RELEASE_BY_TITLE.get(title);
            if (fixed != null) {
                return fixed;
            }
            return BOX_OFFICE_REF.plusDays(1 + (int) (Math.abs(id * 17) % 120));
        }
        if ("ARTE".equals(status)) {
            return BOX_OFFICE_REF.minusDays(3 + (int) (Math.abs(id * 7) % 40));
        }
        return BOX_OFFICE_REF.minusDays(30);
    }

    /**
     * 2026-03-31 기준 주간 박스오피스 TOP10(순위·예매율) 및 개봉예정작 순서·기대 예매율을 DB에 맞춥니다.
     */
    private void syncBoxOfficeAndUpcomingForRef() {
        movieRepository.findAll().forEach(movie -> {
            String title = movie.getTitle() != null ? movie.getTitle().trim() : "";
            String status = movie.getStatus();
            boolean changed = false;

            if ("SHOWING".equals(status)) {
                int idx = BOX_OFFICE_TOP10.indexOf(title);
                if (idx >= 0) {
                    int rank = idx + 1;
                    int rate = BOX_OFFICE_BOOKING_PCT[idx];
                    if (!Integer.valueOf(rank).equals(movie.getRankPosition())) {
                        movie.setRankPosition(rank);
                        changed = true;
                    }
                    if (!Integer.valueOf(rate).equals(movie.getBookingRate())) {
                        movie.setBookingRate(rate);
                        changed = true;
                    }
                } else {
                    if (movie.getRankPosition() != null) {
                        movie.setRankPosition(null);
                        changed = true;
                    }
                    int fallback = 18 + (int) (Math.abs((movie.getId() != null ? movie.getId() : 0L) * 13) % 32);
                    if (!Integer.valueOf(fallback).equals(movie.getBookingRate())) {
                        movie.setBookingRate(fallback);
                        changed = true;
                    }
                }
            } else if ("UPCOMING".equals(status)) {
                int idx = UPCOMING_ORDER.indexOf(title);
                if (idx >= 0) {
                    int rank = idx + 1;
                    int rate = UPCOMING_BOOKING_PCT[idx];
                    if (!Integer.valueOf(rank).equals(movie.getRankPosition())) {
                        movie.setRankPosition(rank);
                        changed = true;
                    }
                    if (!Integer.valueOf(rate).equals(movie.getBookingRate())) {
                        movie.setBookingRate(rate);
                        changed = true;
                    }
                } else {
                    if (movie.getRankPosition() != null) {
                        movie.setRankPosition(null);
                        changed = true;
                    }
                }
            } else if ("ARTE".equals(status) && movie.getRankPosition() != null) {
                movie.setRankPosition(null);
                changed = true;
            }

            if (changed) {
                movieRepository.save(movie);
            }
        });
    }

    /**
     * DB의 모든 영화에 대해 poster_url을 맵의 TMDB 주소로 맞춥니다.
     * 별칭·띄어쓰기 차이는 {@link #resolvePosterUrl(String)}로 흡수합니다.
     * 매칭 실패 시 {@link #DEFAULT_POSTER}(중립 플레이스홀더)를 씁니다.
     */
    private void applyPosterToEveryMovie() {
        movieRepository.findAll().forEach(movie -> {
            String url = resolvePosterUrl(movie.getTitle());
            if (!url.equals(movie.getPosterUrl())) {
                movie.setPosterUrl(url);
                movieRepository.save(movie);
            }
        });
    }

    /** 맵에 제목이 있으면 trailer_url을 공식 예고편(embed)으로 맞춥니다. */
    private void applyTrailerToEveryMovie() {
        movieRepository.findAll().forEach(movie -> {
            String key = resolveTrailerLookupKey(movie.getTitle());
            if (key == null) {
                return;
            }
            String url = TRAILER.get(key);
            if (!url.equals(movie.getTrailerUrl())) {
                movie.setTrailerUrl(url);
                movieRepository.save(movie);
            }
        });
    }

    /**
     * TMDB 영화 검색 API로 포스터가 플레이스홀더인 항목만 보강합니다.
     * {@code tmdb.api.key}가 비어 있으면 동작하지 않습니다.
     */
    private void enrichPostersFromTmdbSearchIfConfigured() {
        if (tmdbApiKey == null || tmdbApiKey.isBlank()) {
            return;
        }
        RestClient rc = RestClient.builder().build();
        for (Movie movie : movieRepository.findAll()) {
            String u = movie.getPosterUrl();
            if (u != null && u.contains("image.tmdb.org")) {
                continue;
            }
            if (u != null && !u.contains("placehold") && !DEFAULT_POSTER.equals(u)) {
                continue;
            }
            if (movie.getTitle() == null || movie.getTitle().isBlank()) {
                continue;
            }
            try {
                String body = rc.get()
                    .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.themoviedb.org")
                        .path("/3/search/movie")
                        .queryParam("api_key", tmdbApiKey)
                        .queryParam("query", movie.getTitle())
                        .queryParam("language", "ko-KR")
                        .build())
                    .retrieve()
                    .body(String.class);
                if (body == null) {
                    continue;
                }
                JsonNode root = objectMapper.readTree(body);
                JsonNode results = root.get("results");
                if (results == null || !results.isArray() || results.isEmpty()) {
                    continue;
                }
                String path = results.get(0).path("poster_path").asText(null);
                if (path == null || path.isEmpty()) {
                    continue;
                }
                movie.setPosterUrl("https://image.tmdb.org/t/p/w500" + path);
                movieRepository.save(movie);
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ignored) {
                // 한 건 실패해도 다음 영화로
            }
        }
    }

    /**
     * 시드 없이 쌓인 DB에도 개봉예정/아르떼 제목이 전부 SHOWING으로 들어간 경우를 복구합니다.
     */
    private void reconcileMovieStatuses() {
        Set<String> upcomingTitles = Set.of(
            "미션 임파서블: 파이널 레코닝", "아바타 3", "분노의 질주: 파이널",
            "캡틴 아메리카: 브레이브 뉴 월드", "오징어 게임: 더 무비", "위키드", "모아나 2"
        );
        Set<String> arteTitles = Set.of(
            "브라이드!", "우리는 매일매일", "우리에게는 아직 내일이 있다", "소년들", "다음 소희"
        );
        movieRepository.findAll().forEach(movie -> {
            String title = movie.getTitle() != null ? movie.getTitle().trim() : "";
            if (upcomingTitles.contains(title)) {
                if (!"UPCOMING".equals(movie.getStatus())) {
                    movie.setStatus("UPCOMING");
                    movieRepository.save(movie);
                }
            } else if (arteTitles.contains(title)) {
                if (!"ARTE".equals(movie.getStatus())) {
                    movie.setStatus("ARTE");
                    movieRepository.save(movie);
                }
            }
        });
    }

    // ──────────────────────────────────────────────────────────
    //  공지사항 초기화 (10건)
    // ──────────────────────────────────────────────────────────
    private void initNotices() {
        if (noticeRepository.count() > 0) return;

        noticeRepository.saveAll(List.of(
            Notice.builder().category("전체").isImportant(true).viewCount(25485)
                .title("[필독] 2026 VIP 멤버십 등급 기준 변경 안내")
                .content("2026년 VIP 멤버십은 3개 등급으로 운영됩니다.\n\n• 일반: 26만원 미만\n• VIP: 연 26만원 이상\n• VVIP: 연 30만원 이상\n• LVIP: 연 36만원 이상\n\n등급 산정은 관람일 2~3일 후 반영되며, 1일 최대 3건까지 인정됩니다.").build(),
            Notice.builder().category("전체").isImportant(true).viewCount(12340)
                .title("[이벤트] 왕과 사는 남자 1,500만 돌파 기념 이벤트")
                .content("왕과 사는 남자가 누적 1,500만 관객을 돌파했습니다!\n기념 이벤트로 SNS 인증 시 팝콘 세트를 증정합니다.\n기간: 2026.03.20 ~ 2026.04.20").build(),
            Notice.builder().category("전체").isImportant(true).viewCount(8965)
                .title("[보안] 세현시네마 사칭 피싱 사기 주의")
                .content("세현시네마를 사칭한 피싱 사기가 발생하고 있습니다.\n당사는 개인 전화번호로 금전 요구나 개인정보를 요청하지 않습니다.\n의심스러운 연락을 받으시면 즉시 고객센터(010-3331-0292)로 신고해 주세요.").build(),
            Notice.builder().category("전체").isImportant(false).viewCount(4231)
                .title("SEHYUN PAY 시스템 점검 안내 (04/12)")
                .content("■ 점검 일시: 2026.04.12(일) 00:30 ~ 05:30\n■ 영향 범위: SEHYUN PAY 부산은행 계좌 결제\n■ 점검 중 해당 결제 수단은 이용이 불가합니다.\n다른 결제 수단(신용카드, 카카오페이 등)을 이용해 주세요.").build(),
            Notice.builder().category("전체").isImportant(false).viewCount(3108)
                .title("[CGV제휴] IMAX 관람 혜택 확대 안내")
                .content("2026년 4월부터 IMAX 관람 요금이 인상됩니다.\n변경 전: 18,000원 → 변경 후: 19,000원\nVIP 회원은 기존 요금으로 이용 가능합니다. (2026.06.30까지)").build(),
            Notice.builder().category("전체").isImportant(false).viewCount(2876)
                .title("모바일 앱 업데이트 안내 (v4.2.1)")
                .content("세현시네마 앱이 v4.2.1로 업데이트됩니다.\n주요 변경 사항:\n• AI 상담톡 기능 개선\n• 좌석 선택 UI 개선\n• 결제 속도 향상\n업데이트 일시: 2026.04.01(수) 오전 2시").build(),
            Notice.builder().category("서울").isImportant(false).viewCount(1543)
                .title("[서울] 강남점 리클라이너관 오픈")
                .content("세현시네마 강남점에 리클라이너관이 새롭게 오픈합니다!\n프리미엄 리클라이너 좌석으로 최고의 관람 경험을 선사합니다.\n오픈일: 2026.04.05(일)").build(),
            Notice.builder().category("경기").isImportant(false).viewCount(987)
                .title("[경기] 수원점 4DX 설비 점검 안내")
                .content("세현시네마 수원점 4DX관 정기 설비 점검이 진행됩니다.\n점검 기간: 2026.04.08(화) ~ 04.09(수)\n해당 기간 중 4DX 상영이 불가합니다. 양해 부탁드립니다.").build(),
            Notice.builder().category("전체").isImportant(false).viewCount(756)
                .title("개인정보 처리방침 개정 안내")
                .content("개인정보보호법 개정에 따라 개인정보 처리방침이 변경됩니다.\n시행일: 2026.05.01\n주요 변경 내용은 홈페이지에서 확인하실 수 있습니다.").build(),
            Notice.builder().category("전체").isImportant(false).viewCount(432)
                .title("2026 여름 시즌 이벤트 예고")
                .content("2026년 여름을 맞이하여 특별 이벤트를 준비 중입니다.\n자세한 내용은 추후 공지 예정입니다. 많은 기대 부탁드립니다!").build()
        ));
    }

    // ──────────────────────────────────────────────────────────
    //  FAQ 초기화 (카테고리별)
    // ──────────────────────────────────────────────────────────
    private void initFAQs() {
        if (faqRepository.count() > 0) return;

        faqRepository.saveAll(List.of(
            FAQ.builder().category("영화관 이용").sortOrder(1)
                .question("예매 후 영수증 출력은 어떻게 하나요?")
                .answer("마이페이지 → 결제내역에서 신용카드 전표 출력이 가능합니다. (최근 70일 이내)").build(),
            FAQ.builder().category("영화관 이용").sortOrder(2)
                .question("영화 시작 후 입장이 가능한가요?")
                .answer("광고 및 예고편 상영 중에는 입장 가능하나, 본편 시작 후에는 입장이 제한될 수 있습니다.").build(),
            FAQ.builder().category("멤버십").sortOrder(1)
                .question("VIP 등급은 어떻게 산정되나요?")
                .answer("연간 영화 티켓 결제 금액 기준으로 산정됩니다.\n• VIP: 연 26만원 이상\n• VVIP: 연 30만원 이상\n• LVIP: 연 36만원 이상\n관람일 2~3일 후 반영, 1일 최대 3건 인정.").build(),
            FAQ.builder().category("멤버십").sortOrder(2)
                .question("VIP 혜택은 어떻게 사용하나요?")
                .answer("마이페이지에서 쿠폰을 확인하고 예매 시 적용할 수 있습니다.\n생일 쿠폰은 생일 당월 1일에 자동 발급됩니다.").build(),
            FAQ.builder().category("온라인").sortOrder(1)
                .question("예매한 적이 없는데 예매내역이 전송되었어요")
                .answer("타 고객의 연락처 오기입으로 인한 경우입니다. 1:1문의로 알려주시면 확인 후 안내드립니다.").build(),
            FAQ.builder().category("온라인").sortOrder(2)
                .question("예매 취소는 어떻게 하나요?")
                .answer("마이페이지 → 예매내역에서 취소 가능합니다.\n영화 시작 30분 전까지 취소 시 전액 환불되며, 이후에는 취소 수수료가 발생합니다.").build(),
            FAQ.builder().category("할인혜택").sortOrder(1)
                .question("1+1 관람권은 어떻게 사용하나요?")
                .answer("2인 예매 시에만 사용 가능하며, 1인 요금으로 2인이 관람할 수 있습니다.").build(),
            FAQ.builder().category("할인혜택").sortOrder(2)
                .question("할인 쿠폰 유효기간은 어떻게 되나요?")
                .answer("쿠폰별로 유효기간이 다릅니다. 마이페이지 → 쿠폰함에서 확인 가능합니다. 유효기간 만료 쿠폰은 연장이 불가합니다.").build(),
            FAQ.builder().category("회원").sortOrder(1)
                .question("비회원으로 예매가 가능한가요?")
                .answer("비회원 약관 동의 후 기초 개인정보 설정으로 예매 가능합니다. 단, 관람권·할인권 등은 사용 불가합니다.").build(),
            FAQ.builder().category("회원").sortOrder(2)
                .question("아이디/비밀번호를 잊어버렸어요")
                .answer("로그인 페이지에서 '아이디/비밀번호 찾기'를 이용하거나, 1:1 문의를 통해 고객센터에 문의해 주세요.").build(),
            FAQ.builder().category("결제").sortOrder(1)
                .question("결제 수단은 어떤 것이 있나요?")
                .answer("신용/체크카드, 카카오페이, 토스페이, 휴대폰 결제, 현금(무통장 입금) 등을 이용하실 수 있습니다.").build(),
            FAQ.builder().category("결제").sortOrder(2)
                .question("카드 결제 후 취소 시 환불은 얼마나 걸리나요?")
                .answer("카드사에 따라 다르나 통상 3~5 영업일 이내 처리됩니다.").build()
        ));
    }
}
