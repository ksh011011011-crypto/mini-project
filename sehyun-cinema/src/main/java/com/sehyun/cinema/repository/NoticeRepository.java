package com.sehyun.cinema.repository;

import com.sehyun.cinema.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 중요 공지 (최신순)
    List<Notice> findByIsImportantTrueOrderByCreatedAtDesc();

    // 카테고리별 페이징
    Page<Notice> findByCategoryOrderByIsImportantDescCreatedAtDesc(String category, Pageable pageable);

    // 제목/내용 검색
    @Query("SELECT n FROM Notice n WHERE LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Notice> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 전체 카테고리 페이징
    Page<Notice> findAllByOrderByIsImportantDescCreatedAtDesc(Pageable pageable);
}
