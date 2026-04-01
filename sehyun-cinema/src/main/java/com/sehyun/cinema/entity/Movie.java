package com.sehyun.cinema.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "movies")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Column(name = "trailer_url", length = 500)
    private String trailerUrl;

    // 장르: 액션, 드라마, SF, 코미디, 스릴러, 애니, 로맨스, 공포
    @Column(length = 30)
    private String genre;

    // 관람 등급: 전체, 12세, 15세, 청소년관람불가
    @Column(length = 20)
    @Builder.Default
    private String ageRating = "전체";

    // 상영 상태: SHOWING(현재상영), UPCOMING(개봉예정), ARTE(아르떼)
    @Column(length = 20)
    @Builder.Default
    private String status = "SHOWING";

    @Column(name = "release_date")
    private LocalDate releaseDate;

    // 상영 시간 (분)
    @Column(name = "running_time")
    private Integer runningTime;

    @Column(name = "rating_avg")
    @Builder.Default
    private Double ratingAvg = 0.0;

    @Column(name = "heart_count")
    @Builder.Default
    private Integer heartCount = 0;

    // 예매율 (%)
    @Column(name = "booking_rate")
    @Builder.Default
    private Integer bookingRate = 0;

    // TOP 순위 (1~100, null이면 미노출)
    @Column(name = "rank_position")
    private Integer rankPosition;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void incrementHeart() {
        this.heartCount++;
    }

    public void decrementHeart() {
        if (this.heartCount > 0) this.heartCount--;
    }

    public void updateRating(double newAvg) {
        this.ratingAvg = Math.round(newAvg * 10.0) / 10.0;
    }

    /** 카드/목록용 TMDB 포스터 — w500 → w780로 선명도 개선 */
    public String getPosterUrlForDisplay() {
        if (posterUrl == null || posterUrl.isBlank()) return null;
        String u = posterUrl.trim();
        if (u.contains("image.tmdb.org/t/p/w500")) {
            return u.replace("/w500/", "/w780/");
        }
        return u;
    }

    /** 상세·히어로용 (가능 시 w1280) */
    public String getPosterUrlLarge() {
        if (posterUrl == null || posterUrl.isBlank()) return null;
        String u = posterUrl.trim();
        if (u.contains("image.tmdb.org/t/p/w500")) {
            return u.replace("/w500/", "/w1280/");
        }
        if (u.contains("image.tmdb.org/t/p/w780")) {
            return u.replace("/w780/", "/w1280/");
        }
        return u;
    }
}
