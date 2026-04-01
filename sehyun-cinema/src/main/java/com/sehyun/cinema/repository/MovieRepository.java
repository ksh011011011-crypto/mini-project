package com.sehyun.cinema.repository;

import com.sehyun.cinema.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    // 상태별 영화 목록 (순위 정렬)
    List<Movie> findByStatusOrderByRankPositionAscBookingRateDesc(String status);

    // 제목 검색 (대소문자 무시)
    @Query("SELECT m FROM Movie m WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Movie> searchByTitle(@Param("keyword") String keyword);

    // 상태 + 제목 검색
    @Query("SELECT m FROM Movie m WHERE m.status = :status AND LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Movie> findByStatusAndTitleContaining(@Param("status") String status,
                                               @Param("keyword") String keyword,
                                               Pageable pageable);

    // TOP 10 (현재상영 + 순위 있는 것만)
    List<Movie> findByStatusAndRankPositionIsNotNullOrderByRankPositionAsc(String status);

    // 상태별 페이징
    Page<Movie> findByStatus(String status, Pageable pageable);

    /**
     * 개봉예정: rankPosition null은 뒤로 (Pageable의 Sort.NullHandling은 Criteria에서 미지원 → JPQL로 처리)
     */
    @Query("SELECT m FROM Movie m WHERE m.status = :status ORDER BY "
        + "CASE WHEN m.rankPosition IS NULL THEN 1 ELSE 0 END ASC, "
        + "m.rankPosition ASC, m.bookingRate DESC")
    Page<Movie> findByStatusUpcomingPaged(@Param("status") String status, Pageable pageable);

    // 장르별
    List<Movie> findByGenre(String genre);

    // 제목 존재 여부
    boolean existsByTitle(String title);

    // 상태별 + 예매율 정렬
    Page<Movie> findByStatusOrderByBookingRateDesc(String status, Pageable pageable);
}
