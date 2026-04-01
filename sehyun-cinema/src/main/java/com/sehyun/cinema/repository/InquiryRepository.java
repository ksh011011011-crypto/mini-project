package com.sehyun.cinema.repository;

import com.sehyun.cinema.entity.Inquiry;
import com.sehyun.cinema.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findByMemberOrderByCreatedAtDesc(Member member);
}
