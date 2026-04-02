package com.sehyun.cinema.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 결제(예매·매점) 확정 시 적립 포인트 이력 (데모) */
@Entity
@Table(name = "point_ledgers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 적립 포인트 (양수) */
    @Column(nullable = false)
    private Integer points;

    /** BOOKING | STORE */
    @Column(length = 20, nullable = false)
    private String category;

    /** booking.id 또는 store_orders.id */
    @Column(name = "ref_id")
    private Long refId;

    @Column(length = 200)
    private String description;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
