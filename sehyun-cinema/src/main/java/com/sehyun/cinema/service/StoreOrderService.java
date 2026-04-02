package com.sehyun.cinema.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sehyun.cinema.dto.StoreCheckoutRequest;
import com.sehyun.cinema.entity.Member;
import com.sehyun.cinema.entity.StoreOrder;
import com.sehyun.cinema.repository.StoreOrderRepository;
import com.sehyun.cinema.store.StoreCatalog;
import com.sehyun.cinema.store.StoreCatalog.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreOrderService {

    private static final Set<String> PAYMENT_METHODS = Set.of(
            "CARD", "PHONE", "KAKAO", "NAVER", "TOSS"
    );

    private static final int MAX_QTY_PER_LINE = 30;

    private final StoreOrderRepository storeOrderRepository;
    private final ObjectMapper objectMapper;
    private final PointService pointService;

    /**
     * 온라인 매점 데모 결제 — 실제 PG 연동 없이 주문·금액만 서버 검증 후 저장.
     * VIP 누적(totalSpent)에는 반영하지 않음(예매와 동일 정책: 관람 금액만).
     */
    public StoreOrder checkout(Member member, StoreCheckoutRequest req) {
        if (req == null || req.items() == null || req.items().isEmpty()) {
            throw new IllegalArgumentException("주문할 상품을 담아 주세요.");
        }
        String method = req.paymentMethod() != null ? req.paymentMethod().trim().toUpperCase() : "";
        if (!PAYMENT_METHODS.contains(method)) {
            throw new IllegalArgumentException("결제 수단을 선택해 주세요.");
        }

        List<Map<String, Object>> lines = new ArrayList<>();
        int total = 0;
        for (StoreCheckoutRequest.LineItem line : req.items()) {
            if (line.qty() <= 0 || line.qty() > MAX_QTY_PER_LINE) {
                throw new IllegalArgumentException("각 메뉴는 1~" + MAX_QTY_PER_LINE + "개까지 주문할 수 있습니다.");
            }
            Product p = StoreCatalog.find(line.productId())
                    .orElseThrow(() -> new IllegalArgumentException("알 수 없는 메뉴가 포함되어 있습니다: " + line.productId()));
            int lineTotal = p.priceWon() * line.qty();
            total += lineTotal;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productId", p.id());
            row.put("name", p.name());
            row.put("unitPrice", p.priceWon());
            row.put("qty", line.qty());
            row.put("lineTotal", lineTotal);
            lines.add(row);
        }

        if (total <= 0) {
            throw new IllegalArgumentException("결제 금액이 올바르지 않습니다.");
        }

        String linesJson;
        try {
            linesJson = objectMapper.writeValueAsString(lines);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("주문 직렬화 실패");
        }

        String paymentRef = "DEMO-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        StoreOrder order = StoreOrder.builder()
                .member(member)
                .totalPrice(total)
                .paymentMethod(method)
                .status("PAID")
                .linesJson(linesJson)
                .paymentRef(paymentRef)
                .build();

        StoreOrder saved = storeOrderRepository.save(order);
        pointService.earnFromStore(member, saved.getId(), total);
        return saved;
    }

    public void cancelOrder(Member member, Long orderId) {
        StoreOrder o = storeOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
        if (!o.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("취소 권한이 없습니다.");
        }
        if (!"PAID".equals(o.getStatus())) {
            throw new IllegalArgumentException("이미 취소되었거나 취소할 수 없는 주문입니다.");
        }
        o.setStatus("CANCELLED");
        o.setCancelledAt(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<StoreOrder> getMyOrders(Member member) {
        return storeOrderRepository.findByMemberOrderByCreatedAtDesc(member);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> parseLinesJson(StoreOrder order) {
        try {
            return objectMapper.readValue(order.getLinesJson(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
