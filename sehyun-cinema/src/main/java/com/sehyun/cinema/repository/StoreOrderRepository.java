package com.sehyun.cinema.repository;

import com.sehyun.cinema.entity.Member;
import com.sehyun.cinema.entity.StoreOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreOrderRepository extends JpaRepository<StoreOrder, Long> {

    List<StoreOrder> findByMemberOrderByCreatedAtDesc(Member member);
}
