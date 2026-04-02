package com.sehyun.cinema.controller;

import com.sehyun.cinema.dto.MemberJoinDto;
import com.sehyun.cinema.entity.Booking;
import com.sehyun.cinema.entity.Member;
import com.sehyun.cinema.entity.Movie;
import com.sehyun.cinema.entity.StoreOrder;
import com.sehyun.cinema.service.BookingService;
import com.sehyun.cinema.service.MemberService;
import com.sehyun.cinema.service.MovieService;
import com.sehyun.cinema.service.StoreOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final BookingService bookingService;
    private final MovieService movieService;
    private final StoreOrderService storeOrderService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("loginError", "아이디 또는 비밀번호가 일치하지 않습니다.");
        }
        if (logout != null) {
            model.addAttribute("success", "로그아웃되었습니다.");
        }
        return "login";
    }

    @GetMapping("/join")
    public String joinPage(Model model) {
        model.addAttribute("joinDto", new MemberJoinDto());
        return "join";
    }

    @PostMapping("/join")
    public String join(@Valid @ModelAttribute("joinDto") MemberJoinDto dto,
                       BindingResult result,
                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return "join";
        try {
            memberService.join(dto);
            redirectAttributes.addFlashAttribute("success", "회원가입이 완료되었습니다. 로그인해 주세요.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            result.rejectValue("username", "duplicate", e.getMessage());
            return "join";
        }
    }

    @GetMapping("/mypage")
    public String myPage(Authentication authentication, Model model) {
        Member member = memberService.getByUsername(authentication.getName());
        model.addAttribute("member", member);
        int spent = member.getTotalSpent();
        int nextTarget = 0; String nextGrade = "";
        if      (spent < 260000) { nextTarget = 260000; nextGrade = "VIP"; }
        else if (spent < 300000) { nextTarget = 300000; nextGrade = "VVIP"; }
        else if (spent < 360000) { nextTarget = 360000; nextGrade = "LVIP"; }
        model.addAttribute("nextTarget", nextTarget);
        model.addAttribute("nextGrade", nextGrade);
        model.addAttribute("remaining", Math.max(0, nextTarget - spent));
        return "mypage";
    }

    @GetMapping("/mypage/bookings")
    public String myBookings(Authentication authentication, Model model) {
        Member member = memberService.getByUsername(authentication.getName());
        List<Booking> all = bookingService.getMyBookings(member);
        List<Booking> activeBookings = all.stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .collect(Collectors.toList());
        List<Booking> cancelledBookings = all.stream()
                .filter(b -> "CANCELLED".equals(b.getStatus()))
                .collect(Collectors.toList());
        model.addAttribute("member", member);
        model.addAttribute("bookings", all);
        model.addAttribute("activeBookings", activeBookings);
        model.addAttribute("cancelledBookings", cancelledBookings);
        return "mypage-bookings";
    }

    @GetMapping("/mypage/hearts")
    public String myHearts(Authentication authentication, Model model) {
        Member member = memberService.getByUsername(authentication.getName());
        List<Movie> hearts = movieService.getHeartedMovies(member);
        model.addAttribute("member", member);
        model.addAttribute("movies", hearts);
        return "mypage-hearts";
    }

    @GetMapping("/mypage/store-orders")
    public String myStoreOrders(Authentication authentication, Model model) {
        Member member = memberService.getByUsername(authentication.getName());
        List<StoreOrder> orders = storeOrderService.getMyOrders(member);
        Map<Long, List<Map<String, Object>>> orderLineMap = new LinkedHashMap<>();
        for (StoreOrder o : orders) {
            orderLineMap.put(o.getId(), storeOrderService.parseLinesJson(o));
        }
        model.addAttribute("member", member);
        model.addAttribute("storeOrders", orders);
        model.addAttribute("orderLineMap", orderLineMap);
        return "mypage-store-orders";
    }

    @GetMapping("/mypage/reviews")
    public String myReviews(Authentication authentication, Model model) {
        Member member = memberService.getByUsername(authentication.getName());
        model.addAttribute("member", member);
        return "mypage-reviews";
    }

    @PostMapping("/mypage/update")
    public String updateInfo(Authentication authentication,
                             @RequestParam String name,
                             @RequestParam String email,
                             @RequestParam String phone,
                             RedirectAttributes redirectAttributes) {
        memberService.updateMemberInfo(authentication.getName(), name, email, phone);
        redirectAttributes.addFlashAttribute("success", "정보가 수정되었습니다.");
        return "redirect:/mypage";
    }

    @GetMapping("/membership")
    public String membership() {
        return "membership";
    }
}
