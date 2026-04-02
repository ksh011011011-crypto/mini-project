package com.sehyun.cinema.repository;

import com.sehyun.cinema.entity.Member;
import com.sehyun.cinema.entity.PointLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointLedgerRepository extends JpaRepository<PointLedger, Long> {
    List<PointLedger> findByMemberOrderByCreatedAtDesc(Member member);
}
