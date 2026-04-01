package com.sehyun.cinema.store;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 매점 메뉴 가격표 — 프론트 store.html PRODUCTS와 동일해야 결제 검증이 맞습니다.
 */
public final class StoreCatalog {

    private static final Map<String, Product> BY_ID;

    static {
        Map<String, Product> m = new HashMap<>();
        m.put("pop-s", new Product("pop-s", "팝콘 S", 4500));
        m.put("pop-m", new Product("pop-m", "팝콘 M", 5500));
        m.put("pop-l", new Product("pop-l", "팝콘 L", 6500));
        m.put("pop-caramel", new Product("pop-caramel", "카라멜 팝콘 M", 6000));
        m.put("combo-a", new Product("combo-a", "콤보 A (팝콘M+탄산2)", 11000));
        m.put("combo-b", new Product("combo-b", "콤보 B (팝콘L+탄산2+초콜릿)", 13500));
        m.put("combo-sweet", new Product("combo-sweet", "스위트 콤보", 15000));
        m.put("drink-soda", new Product("drink-soda", "탄산음료 M", 3500));
        m.put("drink-coffee", new Product("drink-coffee", "아메리카노", 4000));
        m.put("nacho", new Product("nacho", "나초 · 치즈", 5500));
        m.put("hotdog", new Product("hotdog", "핫도그", 5000));
        m.put("pretzel", new Product("pretzel", "프레첼", 4500));
        BY_ID = Collections.unmodifiableMap(m);
    }

    private StoreCatalog() {}

    public static Optional<Product> find(String productId) {
        if (productId == null || productId.isBlank()) {
            return Optional.empty();
        }
        String id = normalizeProductId(productId.trim());
        return Optional.ofNullable(BY_ID.get(id));
    }

    /**
     * 예전 장바구니 형식(pop-s-0 등) 호환
     */
    public static String normalizeProductId(String raw) {
        String s = raw;
        int dash = s.lastIndexOf('-');
        if (dash > 0) {
            String tail = s.substring(dash + 1);
            if (tail.matches("\\d+") && BY_ID.containsKey(s.substring(0, dash))) {
                return s.substring(0, dash);
            }
        }
        return s;
    }

    public record Product(String id, String name, int priceWon) {}
}
