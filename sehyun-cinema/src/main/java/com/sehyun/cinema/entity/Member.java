package com.sehyun.cinema.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "members")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 50)
    private String name;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    // VIP 등급: 일반, VIP, VVIP, LVIP
    @Column(length = 20)
    @Builder.Default
    private String grade = "일반";

    // 누적 결제 금액 (원 단위)
    @Column(name = "total_spent")
    @Builder.Default
    private Integer totalSpent = 0;

    @Column(length = 20)
    @Builder.Default
    private String role = "ROLE_USER";

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // 등급 자동 업데이트
    public void updateGrade() {
        if (this.totalSpent >= 360000) {
            this.grade = "LVIP";
        } else if (this.totalSpent >= 300000) {
            this.grade = "VVIP";
        } else if (this.totalSpent >= 260000) {
            this.grade = "VIP";
        } else {
            this.grade = "일반";
        }
    }

    public void addSpent(int amount) {
        this.totalSpent += amount;
        updateGrade();
    }
}
