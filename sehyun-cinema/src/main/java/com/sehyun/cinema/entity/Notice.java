package com.sehyun.cinema.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notices")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 카테고리: 전체, 영화관, 서울, 경기/인천 등
    @Column(length = 50)
    @Builder.Default
    private String category = "전체";

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "is_important")
    @Builder.Default
    private Boolean isImportant = false;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void increaseViewCount() {
        this.viewCount++;
    }
}
