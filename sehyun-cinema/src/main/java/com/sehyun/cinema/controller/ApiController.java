package com.sehyun.cinema.controller;

import com.sehyun.cinema.dto.StoreCheckoutRequest;
import com.sehyun.cinema.entity.Member;
import com.sehyun.cinema.entity.Movie;
import com.sehyun.cinema.entity.StoreOrder;
import com.sehyun.cinema.service.AiLlmChatService;
import com.sehyun.cinema.service.CustomerService;
import com.sehyun.cinema.service.MemberService;
import com.sehyun.cinema.service.MovieService;
import com.sehyun.cinema.service.StoreOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final MovieService movieService;
    private final MemberService memberService;
    private final CustomerService customerService;
    private final AiLlmChatService aiLlmChatService;
    private final StoreOrderService storeOrderService;

    // 하트 토글
    @PostMapping("/movies/{id}/heart")
    public ResponseEntity<Map<String, Object>> toggleHeart(
            @PathVariable Long id,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        Member member = memberService.getByUsername(authentication.getName());
        Movie movie = movieService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("영화를 찾을 수 없습니다."));
        boolean isHearted = movieService.toggleHeart(member, movie);
        return ResponseEntity.ok(Map.of("hearted", isHearted, "heartCount", movie.getHeartCount()));
    }

    // 영화 검색 (JSON)
    @GetMapping("/movies/search")
    public ResponseEntity<List<Map<String, Object>>> searchMovies(@RequestParam String keyword) {
        List<Movie> movies = movieService.searchMovies(keyword);
        List<Map<String, Object>> result = movies.stream().map(m -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id",          m.getId());
            map.put("title",       m.getTitle());
            map.put("genre",       m.getGenre()       != null ? m.getGenre()       : "");
            String poster = m.getPosterUrlForDisplay();
            if (poster == null || poster.isEmpty()) poster = m.getPosterUrl() != null ? m.getPosterUrl() : "";
            map.put("posterUrl", poster);
            map.put("status",      m.getStatus()      != null ? m.getStatus()      : "");
            map.put("ageRating",   m.getAgeRating()   != null ? m.getAgeRating()   : "전체이용가");
            map.put("runningTime", m.getRunningTime()!= null  ? m.getRunningTime() : 110);
            map.put("rating",      m.getRatingAvg());
            map.put("bookingRate", m.getBookingRate());
            map.put("rank",        m.getRankPosition() != null ? m.getRankPosition() : 0);
            return map;
        }).toList();
        return ResponseEntity.ok(result);
    }

    // AI 상담톡
    @GetMapping("/chat/status")
    public ResponseEntity<Map<String, Object>> chatStatus() {
        boolean open = customerService.isChatServiceOpen();
        return ResponseEntity.ok(Map.of(
            "open", open,
            "message", open
                ? "AI 상담을 이용하실 수 있습니다."
                : "잠시 후 다시 시도해 주세요."
        ));
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> req) {
        String message = req.getOrDefault("message", "");
        boolean open = customerService.isChatServiceOpen();
        AiLlmChatService.ChatAnswer ans = aiLlmChatService.answer(message);
        return ResponseEntity.ok(Map.of(
            "response", ans.text(),
            "provider", ans.provider(),
            "chatOpen", open
        ));
    }

    // 아이디 중복 체크
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        boolean available = !memberService.isUsernameDuplicate(username);
        return ResponseEntity.ok(Map.of("available", available));
    }

    /** 매점 온라인 결제(데모) — 금액·메뉴는 서버에서 재검증 */
    @PostMapping("/store/checkout")
    public ResponseEntity<Map<String, Object>> storeCheckout(
            @RequestBody StoreCheckoutRequest body,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            Member member = memberService.getByUsername(authentication.getName());
            StoreOrder order = storeOrderService.checkout(member, body);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "orderId", order.getId(),
                    "paymentRef", order.getPaymentRef() != null ? order.getPaymentRef() : "",
                    "totalPrice", order.getTotalPrice(),
                    "message", "결제가 완료되었습니다."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "message", e.getMessage()
            ));
        }
    }

    /** 매점 주문 취소(데모) */
    @PostMapping("/store/orders/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelStoreOrder(
            @PathVariable Long id,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            Member member = memberService.getByUsername(authentication.getName());
            storeOrderService.cancelOrder(member, id);
            return ResponseEntity.ok(Map.of("ok", true, "message", "매점 주문이 취소되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        }
    }
}
