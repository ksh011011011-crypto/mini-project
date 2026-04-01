package com.sehyun.cinema.controller;

import com.sehyun.cinema.dto.BookingDto;
import com.sehyun.cinema.entity.Booking;
import com.sehyun.cinema.entity.Member;
import com.sehyun.cinema.entity.Movie;
import com.sehyun.cinema.service.BookingService;
import com.sehyun.cinema.service.MemberService;
import com.sehyun.cinema.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/booking")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final MovieService movieService;
    private final MemberService memberService;

    @GetMapping
    public String bookingPage(@RequestParam(required = false) Long movieId,
                              @RequestParam(required = false) String theaterType,
                              @RequestParam(required = false) String showtime,
                              Authentication authentication,
                              Model model) {
        model.addAttribute("theaterPrices", bookingService.getTheaterPrices());
        model.addAttribute("movies", movieService.getMoviesByStatus("SHOWING", 0, 100).getContent());
        if (movieId != null) {
            movieService.findById(movieId).ifPresent(m -> model.addAttribute("selectedMovie", m));
        }
        if (theaterType != null && !theaterType.isBlank()) {
            model.addAttribute("preselectedTheaterType", theaterType.trim());
        }
        if (showtime != null && !showtime.isBlank()) {
            model.addAttribute("preselectedShowtime", showtime.trim());
        }
        if (authentication != null) {
            try {
                Member member = memberService.getByUsername(authentication.getName());
                model.addAttribute("currentMember", member);
            } catch (Exception ignored) {}
        }
        return "booking";
    }

    @PostMapping("/confirm")
    public String confirmBooking(@ModelAttribute BookingDto dto,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            Member member = memberService.getByUsername(authentication.getName());
            Movie movie = movieService.findById(dto.getMovieId())
                    .orElseThrow(() -> new IllegalArgumentException("영화를 찾을 수 없습니다."));
            Booking booking = bookingService.book(member, movie, dto);
            redirectAttributes.addFlashAttribute("success", "예매가 완료되었습니다! 마이페이지에서 예매 내역을 확인해 주세요.");
            redirectAttributes.addFlashAttribute("flashBookingId", booking.getId());
            return "redirect:/mypage";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/booking";
        }
    }

    @GetMapping("/complete/{id}")
    public String bookingComplete(@PathVariable Long id,
                                  Authentication authentication,
                                  Model model) {
        Booking booking = bookingService.getMyBookings(
                memberService.getByUsername(authentication.getName())
        ).stream().filter(b -> b.getId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("예매 정보를 찾을 수 없습니다."));
        model.addAttribute("booking", booking);
        return "booking-complete";
    }

    @PostMapping("/cancel/{id}")
    public String cancelBooking(@PathVariable Long id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancel(id, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "예매가 취소되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mypage/bookings";
    }
}
