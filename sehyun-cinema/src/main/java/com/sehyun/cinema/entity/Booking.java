package com.sehyun.cinema.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    // 상영관 종류: 일반관, IMAX, 4DX, 수퍼플렉스, 샤롯데, 리클라이너
    @Column(name = "theater_type", length = 30)
    @Builder.Default
    private String theaterType = "일반관";

    // 좌석 정보 (예: A-12)
    @Column(name = "seat_info", length = 20)
    private String seatInfo;

    // 인원 수
    @Column(name = "adult_count")
    @Builder.Default
    private Integer adultCount = 1;

    @Column(name = "teen_count")
    @Builder.Default
    private Integer teenCount = 0;

    // 결제 수단: CARD, PHONE, KAKAO, TOSS, CASH
    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    // 총 결제 금액
    @Column(name = "total_price")
    private Integer totalPrice;

    // 쿠폰 할인 금액
    @Column(name = "discount_amount")
    @Builder.Default
    private Integer discountAmount = 0;

    // 상태: CONFIRMED, CANCELLED
    @Column(length = 15)
    @Builder.Default
    private String status = "CONFIRMED";

    @Column(name = "showtime")
    private LocalDateTime showtime;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
