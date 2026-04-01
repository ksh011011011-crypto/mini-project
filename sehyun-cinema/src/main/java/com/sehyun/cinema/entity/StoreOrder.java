package com.sehyun.cinema.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "store_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    /** PAID (데모 결제 완료) */
    @Column(length = 15)
    @Builder.Default
    private String status = "PAID";

    @Column(name = "lines_json", columnDefinition = "TEXT", nullable = false)
    private String linesJson;

    /** 데모용 거래 참조 번호 */
    @Column(name = "payment_ref", length = 64)
    private String paymentRef;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
