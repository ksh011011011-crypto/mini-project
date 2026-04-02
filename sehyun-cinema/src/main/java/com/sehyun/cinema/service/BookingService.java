package com.sehyun.cinema.service;

import com.sehyun.cinema.dto.BookingDto;
import com.sehyun.cinema.entity.Booking;
import com.sehyun.cinema.entity.Member;
import com.sehyun.cinema.entity.Movie;
import com.sehyun.cinema.repository.BookingRepository;
import com.sehyun.cinema.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final MovieRepository movieRepository;
    private final PointService pointService;

    // 관람 요금표 (원) — 예매 화면 value·상영시간표 hall.key와 동일해야 함
    private static final Map<String, Integer> THEATER_PRICES = new HashMap<>() {{
        put("일반관", 14000);
        put("IMAX", 18000);
        put("4DX", 19000);
        put("SCREENX", 17000);
        put("수퍼플렉스", 17000);
        put("돌비", 18500);
        put("샤롯데", 22000);
        put("리클라이너", 16000);
        put("커플석", 24000);
        put("스위트", 28000);
        put("씨네앤리빙룸", 21000);
    }};

    // 결제 및 예매 확정
    public Booking book(Member member, Movie movie, BookingDto dto) {
        int unitPrice = THEATER_PRICES.getOrDefault(dto.getTheaterType(), 14000);
        int adult = dto.getAdultCount() != null ? dto.getAdultCount() : 0;
        int teen = dto.getTeenCount() != null ? dto.getTeenCount() : 0;
        int teenUnit = (int) Math.floor(unitPrice * 0.8);
        int rawTotal = adult * unitPrice + teen * teenUnit;
        int discount = dto.getDiscountAmount() != null ? Math.max(0, dto.getDiscountAmount()) : 0;
        if (discount > rawTotal) {
            discount = rawTotal;
        }
        int finalPrice = Math.max(0, rawTotal - discount);

        Booking booking = Booking.builder()
            .member(member)
            .movie(movie)
            .theaterType(dto.getTheaterType())
            .seatInfo(dto.getSeatInfo())
            .adultCount(dto.getAdultCount())
            .teenCount(dto.getTeenCount())
            .paymentMethod(dto.getPaymentMethod())
            .totalPrice(finalPrice)
            .discountAmount(discount)
            .showtime(dto.getShowtime())
            .status("CONFIRMED")
            .build();

        Booking saved = bookingRepository.save(booking);

        // VIP 승급 금액 반영 (매점 제외, 관람 금액만)
        member.addSpent(finalPrice);
        pointService.earnFromBooking(member, saved.getId(), finalPrice, movie.getTitle());

        return saved;
    }

    // 예매 취소
    public void cancel(Long bookingId, String username) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("예매 내역을 찾을 수 없습니다."));
        if (!booking.getMember().getUsername().equals(username)) {
            throw new IllegalArgumentException("취소 권한이 없습니다.");
        }
        booking.setStatus("CANCELLED");

        // 취소 시 VIP 금액 차감
        booking.getMember().addSpent(-booking.getTotalPrice());
    }

    // 예매 내역 조회
    @Transactional(readOnly = true)
    public List<Booking> getMyBookings(Member member) {
        return bookingRepository.findByMemberOrderByCreatedAtDesc(member);
    }

    // 상영관 가격 조회
    @Transactional(readOnly = true)
    public Map<String, Integer> getTheaterPrices() {
        return THEATER_PRICES;
    }

    @Transactional(readOnly = true)
    public int getPrice(String theaterType) {
        return THEATER_PRICES.getOrDefault(theaterType, 14000);
    }
}
