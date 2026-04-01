package com.sehyun.cinema.repository;

import com.sehyun.cinema.entity.Booking;
import com.sehyun.cinema.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT DISTINCT b FROM Booking b JOIN FETCH b.movie WHERE b.member = :member ORDER BY b.createdAt DESC")
    List<Booking> findByMemberOrderByCreatedAtDesc(@Param("member") Member member);

    // 최근 3개월 예매 내역
    @Query("SELECT b FROM Booking b WHERE b.member = :member ORDER BY b.createdAt DESC")
    List<Booking> findRecentByMember(@Param("member") Member member);

    // 확정된 예매 건수
    long countByMemberAndStatus(Member member, String status);
}
