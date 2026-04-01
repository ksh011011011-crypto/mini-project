package com.sehyun.cinema.dto;

import java.util.List;

public record StoreCheckoutRequest(
        List<LineItem> items,
        String paymentMethod
) {
    public record LineItem(String productId, int qty) {}
}
