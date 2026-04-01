package com.sehyun.cinema.repository;

import com.sehyun.cinema.entity.FAQ;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface FAQRepository extends JpaRepository<FAQ, Long> {

    List<FAQ> findByCategoryOrderBySortOrderAsc(String category);

    Page<FAQ> findByCategoryOrderBySortOrderAsc(String category, Pageable pageable);

    @Query("SELECT f FROM FAQ f WHERE LOWER(f.question) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<FAQ> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
