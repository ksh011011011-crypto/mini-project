package com.sehyun.cinema.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "faqs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FAQ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 카테고리: 영화관 이용, 스페셜관, L.POINT, 회원, 멤버십, 온라인, 할인혜택, 관람권, 스토어
    @Column(length = 50, nullable = false)
    private String category;

    @Column(nullable = false, length = 300)
    private String question;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
}
