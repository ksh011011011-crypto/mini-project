package com.sehyun.cinema.service;

import com.sehyun.cinema.entity.FAQ;
import com.sehyun.cinema.entity.Inquiry;
import com.sehyun.cinema.entity.Member;
import com.sehyun.cinema.entity.Notice;
import com.sehyun.cinema.repository.FAQRepository;
import com.sehyun.cinema.repository.InquiryRepository;
import com.sehyun.cinema.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final FAQRepository faqRepository;
    private final NoticeRepository noticeRepository;
    private final InquiryRepository inquiryRepository;
    private final MovieService movieService;

    // ===== FAQ =====
    @Transactional(readOnly = true)
    public Page<FAQ> getFAQsByCategory(String category, int page) {
        return faqRepository.findByCategoryOrderBySortOrderAsc(category, PageRequest.of(page, 10));
    }

    @Transactional(readOnly = true)
    public Page<FAQ> searchFAQs(String keyword, int page) {
        return faqRepository.searchByKeyword(keyword, PageRequest.of(page, 10));
    }

    // ===== 공지사항 =====
    @Transactional(readOnly = true)
    public Page<Notice> getNotices(String category, int page) {
        if ("전체".equals(category)) {
            return noticeRepository.findAllByOrderByIsImportantDescCreatedAtDesc(PageRequest.of(page, 10));
        }
        return noticeRepository.findByCategoryOrderByIsImportantDescCreatedAtDesc(category, PageRequest.of(page, 10));
    }

    @Transactional(readOnly = true)
    public Optional<Notice> getNotice(Long id) {
        return noticeRepository.findById(id);
    }

    public Notice increaseViewCount(Long id) {
        Notice notice = noticeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다."));
        notice.increaseViewCount();
        return notice;
    }

    @Transactional(readOnly = true)
    public Page<Notice> searchNotices(String keyword, int page) {
        return noticeRepository.searchByKeyword(keyword, PageRequest.of(page, 10));
    }

    // ===== 1:1 문의 =====
    public Inquiry submitInquiry(Member member, Map<String, String> form) {
        Inquiry inquiry = Inquiry.builder()
            .member(member)
            .category(form.get("category"))
            .inquiryType(form.get("inquiryType"))
            .title(form.get("title"))
            .content(form.get("content"))
            .authorName(form.getOrDefault("authorName", member != null ? member.getName() : ""))
            .phone(form.getOrDefault("phone", member != null ? member.getPhone() : ""))
            .email(form.getOrDefault("email", member != null ? member.getEmail() : ""))
            .emailNotify("true".equals(form.get("emailNotify")))
            .smsNotify("true".equals(form.get("smsNotify")))
            .build();
        return inquiryRepository.save(inquiry);
    }

    @Transactional(readOnly = true)
    public List<Inquiry> getMyInquiries(Member member) {
        return inquiryRepository.findByMemberOrderByCreatedAtDesc(member);
    }

    // ===== AI 상담톡 (규칙 기반 강화) =====
    private static final List<String> PROFANITY_LIST = List.of(
        "씨발","개새","ㅅㅂ","ㅂㅅ","병신","죽어","꺼져","닥쳐","미친","존나","fuck","shit","bitch"
    );

    /** 시연·상담 테스트: 시간 제한 없이 항상 열림 */
    @Transactional(readOnly = true)
    public boolean isChatServiceOpen() {
        return true;
    }

    /**
     * 규칙(키워드)에 매칭되면 답변 문자열, 없으면 empty (LLM·폴백으로 넘김).
     * 공백 메시지는 호출 전에 처리할 것.
     */
    public Optional<String> keywordReply(String message) {
        String msg   = message.trim();
        String lower = msg.toLowerCase().replace(" ", "");

        for (String p : PROFANITY_LIST) {
            if (lower.contains(p)) {
                return Optional.of("정중한 표현으로 질문해 주시면 더 잘 도와드릴 수 있어요 😊\n"
                    + "예매, 취소·환불, 상영시간표, 멤버십 등 편하게 물어보세요.");
            }
        }

        if (isAutoBookingIntent(lower)) {
            return Optional.of(buildAutoBookingToken());
        }

        if (lower.contains("예매") && (lower.contains("방법") || lower.contains("어떻게") || lower.contains("하는법"))) {
            return Optional.of("🎬 예매 방법 안내\n\n① 상단 메뉴 [예매] 클릭\n② 영화 선택 → 상영관 선택 → 날짜/시간 선택\n③ 인원 및 좌석 선택\n④ 쿠폰 적용 후 결제 완료\n\n예매 페이지로 이동하시겠습니까? '네'라고 입력해 주세요.");
        }
        if (lower.contains("취소") || lower.contains("환불")) {
            return Optional.of("🔄 예매 취소/환불 안내\n\n• 취소 방법: 마이페이지 → 예매내역 → 취소\n• 상영 시작 전: 전액 환불\n• 상영 시작 후: 취소 불가\n• 환불 처리: 카드사에 따라 3~5 영업일 소요");
        }
        if (lower.contains("결제") || lower.contains("페이")) {
            return Optional.of("💳 결제 수단 안내\n\n• 신용/체크카드\n• 카카오페이\n• 토스페이\n• 휴대폰 결제\n• 현금(무통장 입금)\n\n모든 결제 수단은 예매 완료 화면에서 선택하실 수 있습니다.");
        }

        if (lower.contains("vip") || lower.contains("멤버십") || lower.contains("등급") || lower.contains("혜택")) {
            return Optional.of("⭐ VIP 멤버십 등급 안내\n\n• 일반: 26만원 미만\n• VIP: 연 26만원 이상\n• VVIP: 연 30만원 이상\n• LVIP: 연 36만원 이상\n\n📌 관람일 2~3일 후 자동 반영\n📌 1일 최대 3건 인정 (매점 제외)\n\n자세한 내용은 [멤버십] 페이지를 확인해 주세요.");
        }

        if (lower.contains("imax") || lower.contains("이맥스")) {
            return Optional.of("🎬 IMAX관 안내\n\n초대형 스크린(최대 26m)과 12채널 입체 음향이 특징입니다.\n• 요금: 18,000원 (일반관 대비 +4,000원)\n• VIP 회원: 기존 요금 유지 (2026.06.30까지)");
        }
        if (lower.contains("4dx")) {
            return Optional.of("🌪️ 4DX관 안내\n\n움직이는 좌석과 바람·물·향기 등 20가지 특수효과를 즐길 수 있습니다.\n• 요금: 19,000원 (일반관 대비 +5,000원)\n• 세현 강남·수원·부산점 운영 중");
        }
        if (lower.contains("수퍼플렉스") || lower.contains("샤롯데")) {
            return Optional.of("🏟️ 스페셜관 안내\n\n• 수퍼플렉스: 270도 초대형 스크린 (17,000원)\n• 샤롯데: 프리미엄 음향 + 럭셔리 인테리어 (22,000원)\n• 리클라이너관: 완전 젖혀지는 리클라이너 좌석 (16,000원)");
        }

        if (lower.contains("상영") && (lower.contains("시간") || lower.contains("시간표") || lower.contains("언제"))) {
            return Optional.of("🕐 상영시간표 안내\n\n상단 메뉴 [예매] → [상영시간표] 에서 날짜별·영화관별 상영 시간을 확인하실 수 있습니다.\n\n직접 이동하려면: /schedule");
        }

        if (lower.contains("왕과사는남자") || lower.contains("왕사남")) {
            return Optional.of("👑 왕과 사는 남자\n\n• 누적 관객: 1,561만 명 (8주 연속 1위)\n• 장르: 사극/드라마\n• 등급: 12세 이상\n• 상영 시간: 약 129분\n\n예매를 원하시면 '네'라고 입력해 주세요!");
        }
        if (lower.contains("헤일메리") || lower.contains("projecthailmary")) {
            return Optional.of("🚀 프로젝트 헤일메리\n\n• 누적 관객: 115만 명 (2위)\n• 장르: SF/어드벤처\n• 등급: 12세 이상\n• 상영 시간: 약 141분\n• 원작: 앤디 위어 베스트셀러 소설\n\n예매를 원하시면 '네'라고 입력해 주세요!");
        }

        if (lower.contains("운영시간") || lower.contains("영업시간") || lower.contains("몇시")) {
            return Optional.of("⏰ 영업 안내\n\n• 첫 상영: 오전 10:00\n• 마지막 상영: 자정 (23:30)\n• AI 상담톡: 시간 제한 없이 이용 가능\n• 1:1 문의 답변: 평일 09:00 ~ 18:00");
        }
        if (lower.contains("주차") || lower.contains("파킹")) {
            return Optional.of("🚗 주차 안내\n\n• 영화 관람 시 최초 1시간 무료 주차\n• VIP 회원: 최초 2시간 무료\n• 이후 10분당 500원 추가\n• 각 지점 주차장 규모가 다를 수 있습니다.");
        }
        if (lower.contains("분실물") || lower.contains("잃어버")) {
            return Optional.of("🔍 분실물 안내\n\n분실물은 상영관 내 직원에게 문의하거나\n고객센터(010-3331-0292)로 연락해 주세요.\n분실물 보관 기간: 30일\n온라인 문의: 1:1 문의 → 분실물 카테고리 선택");
        }
        if (lower.contains("안녕") || lower.contains("hi") || lower.contains("hello") || lower.contains("안뇽")) {
            return Optional.of("안녕하세요! 세현시네마 AI 상담톡입니다 😊\n\n아래 항목으로 빠르게 도움드릴 수 있어요:\n• 예매 방법\n• 취소/환불\n• 상영시간표\n• VIP 멤버십\n• 상영관 안내\n• 자동예매(키워드 입력 시 예매 화면 연결)\n\n궁금한 내용을 입력해 주세요!");
        }
        if (lower.contains("상담톡") || lower.contains("챗봇") || lower.contains("ai상담")) {
            return Optional.of("네, 지금 AI 상담톡으로 연결되어 있어요 🤖\n규칙 기반으로 예매·멤버십·시간표 등을 안내합니다.\n\n자동으로 예매 화면을 열려면 「자동예매」 또는 「바로예매」라고 입력해 주세요.");
        }
        if (lower.contains("고맙") || lower.contains("감사") || lower.contains("땡큐") || lower.contains("thank")) {
            return Optional.of("도움이 되어 기쁩니다! 😊\n추가 문의사항이 있으시면 언제든지 질문해 주세요.\n좋은 영화 관람 되세요! 🎬");
        }
        if (lower.contains("종료") || lower.contains("끝") || lower.contains("그만")) {
            return Optional.of("상담을 종료합니다. 세현시네마를 이용해 주셔서 감사합니다! 🎬\n즐거운 하루 되세요 😊");
        }
        if (lower.contains("도와") || lower.contains("help") || lower.contains("문의")) {
            return Optional.of("무엇을 도와드릴까요? 😊\n• 예매·환불·시간표·멤버십·IMAX/4DX 등 키워드로 질문해 주세요.\n• 「자동예매」 입력 시 현재 상영작 기준으로 예매 화면으로 안내합니다.");
        }
        if (lower.contains("예매")) {
            return Optional.of("🎫 예매 도우미\n\n• 단계: 영화 선택 → 상영관·일시 → 인원·좌석 → 결제\n• 상영시간표: 상단 메뉴 [상영시간표]\n• 「자동예매」라고 입력하면 상영 중인 작품 중 하나로 예매 페이지를 열어 드립니다.");
        }

        return Optional.empty();
    }

    public String unknownKeywordFallback() {
        return "그 부분은 아직 학습된 키워드가 없어요 😅\n\n이렇게 질문해 보세요:\n"
            + "• 예매 방법 / 환불 / 상영시간표\n"
            + "• IMAX 또는 4DX\n"
            + "• 「자동예매」(예매 화면 바로 연결)\n"
            + "• 멤버십·주차·분실물\n\n"
            + "FAQ·1:1 문의·고객센터(010-3331-0292)도 이용 가능합니다.";
    }

    /** 규칙만 (키워드 미매칭 시 기존 안내 문구). LLM 미설정 시와 동일한 동작. */
    public String chatResponse(String message) {
        if (message == null || message.isBlank()) {
            return "무엇이든 질문해 주세요! 😊";
        }
        return keywordReply(message).orElseGet(this::unknownKeywordFallback);
    }

    private static boolean isAutoBookingIntent(String lower) {
        return lower.contains("자동예매")
            || lower.contains("자동으로예매")
            || lower.contains("바로예매")
            || lower.contains("빠른예매")
            || lower.contains("랜덤예매")
            || lower.contains("풀오토")
            || lower.contains("ai예매")
            || lower.contains("예매시작")
            || (lower.contains("랜덤") && lower.contains("예매"));
    }

    /** 현재 상영작 중 무작위 1편으로 예매 연결 */
    private String buildAutoBookingToken() {
        var page = movieService.getMoviesByStatus("SHOWING", 0, 40);
        var list = page.getContent();
        if (list.isEmpty()) {
            return "지금 DB에 등록된 상영작이 없어 자동 예매를 연결할 수 없습니다.\n[예매] 메뉴에서 직접 선택해 주세요.";
        }
        long id = list.get(ThreadLocalRandom.current().nextInt(list.size())).getId();
        return "__AUTO_BOOKING__:" + id;
    }
}
