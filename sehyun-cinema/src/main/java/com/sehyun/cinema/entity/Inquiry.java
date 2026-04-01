package com.sehyun.cinema.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inquiries")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 분류: 영화관, 멤버십/회원정보, 예매/결제, 이벤트
    @Column(length = 50)
    private String category;

    // 문의 종류: 문의, 칭찬, 불만, 건의
    @Column(name = "inquiry_type", length = 20)
    private String inquiryType;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_name", length = 50)
    private String authorName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    // 답변 상태: PENDING, ANSWERED
    @Column(length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "answer_content", columnDefinition = "TEXT")
    private String answerContent;

    @Column(name = "email_notify")
    @Builder.Default
    private Boolean emailNotify = false;

    @Column(name = "sms_notify")
    @Builder.Default
    private Boolean smsNotify = false;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;
}
