package com.sehyun.cinema.service;

import com.sehyun.cinema.entity.Member;
import com.sehyun.cinema.entity.PointLedger;
import com.sehyun.cinema.repository.PointLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PointService {

    private final PointLedgerRepository pointLedgerRepository;

    /** 결제 금액의 1% 적립, 최소 1P (데모) */
    private static int pointsFromAmount(int amountWon) {
        if (amountWon <= 0) {
            return 0;
        }
        return Math.max(1, amountWon / 100);
    }

    public void earnFromBooking(Member member, Long bookingId, int amountWon, String movieTitle) {
        int p = pointsFromAmount(amountWon);
        if (p <= 0) {
            return;
        }
        pointLedgerRepository.save(PointLedger.builder()
                .member(member)
                .points(p)
                .category("BOOKING")
                .refId(bookingId)
                .description("영화 예매 · " + (movieTitle != null ? movieTitle : "예매"))
                .build());
    }

    public void earnFromStore(Member member, Long orderId, int amountWon) {
        int p = pointsFromAmount(amountWon);
        if (p <= 0) {
            return;
        }
        pointLedgerRepository.save(PointLedger.builder()
                .member(member)
                .points(p)
                .category("STORE")
                .refId(orderId)
                .description("매점 주문 · SN-" + orderId)
                .build());
    }

    @Transactional(readOnly = true)
    public List<PointLedger> getMyPoints(Member member) {
        return pointLedgerRepository.findByMemberOrderByCreatedAtDesc(member);
    }

    @Transactional(readOnly = true)
    public int getTotalPoints(Member member) {
        return pointLedgerRepository.findByMemberOrderByCreatedAtDesc(member).stream()
                .mapToInt(PointLedger::getPoints)
                .sum();
    }
}
