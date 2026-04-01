package com.sehyun.cinema.controller;

import com.sehyun.cinema.entity.Member;
import com.sehyun.cinema.entity.Notice;
import com.sehyun.cinema.service.CustomerService;
import com.sehyun.cinema.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final MemberService memberService;

    // FAQ 페이지
    @GetMapping("/faq")
    public String faq(@RequestParam(defaultValue = "영화관 이용") String category,
                      @RequestParam(defaultValue = "0") int page,
                      @RequestParam(required = false) String keyword,
                      Model model) {
        if (keyword != null && !keyword.isBlank()) {
            model.addAttribute("faqs", customerService.searchFAQs(keyword, page));
            model.addAttribute("keyword", keyword);
        } else {
            model.addAttribute("faqs", customerService.getFAQsByCategory(category, page));
        }
        model.addAttribute("category", category);
        model.addAttribute("currentPage", page);
        return "faq";
    }

    // 공지사항 목록
    @GetMapping("/notice")
    public String noticeList(@RequestParam(defaultValue = "전체") String category,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(required = false) String keyword,
                             Model model) {
        Page<Notice> notices;
        if (keyword != null && !keyword.isBlank()) {
            notices = customerService.searchNotices(keyword, page);
            model.addAttribute("keyword", keyword);
        } else {
            notices = customerService.getNotices(category, page);
        }
        model.addAttribute("notices", notices);
        model.addAttribute("category", category);
        model.addAttribute("currentPage", page);
        return "notice";
    }

    // 공지사항 상세
    @GetMapping("/notice/{id}")
    public String noticeDetail(@PathVariable Long id, Model model) {
        Notice notice = customerService.increaseViewCount(id);
        model.addAttribute("notice", notice);
        return "notice-detail";
    }

    // AI 상담톡 페이지
    @GetMapping("/chat")
    public String chatPage() {
        return "chat";
    }

    // 1:1 문의 페이지
    @GetMapping("/inquiry")
    public String inquiryPage(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            try {
                Member member = memberService.getByUsername(authentication.getName());
                model.addAttribute("member", member);
            } catch (Exception ignored) {}
        }
        return "inquiry";
    }

    // 1:1 문의 제출
    @PostMapping("/inquiry")
    public String submitInquiry(@RequestParam Map<String, String> form,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        Member member = null;
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            try { member = memberService.getByUsername(authentication.getName()); }
            catch (Exception ignored) {}
        }
        customerService.submitInquiry(member, form);
        redirectAttributes.addFlashAttribute("success", "문의가 접수되었습니다. 빠른 시일 내 답변 드리겠습니다.");
        return "redirect:/customer/inquiry";
    }

    // 내 문의 내역 (로그인 필요)
    @GetMapping("/inquiry/my")
    public String myInquiries(Authentication authentication, Model model) {
        Member member = memberService.getByUsername(authentication.getName());
        model.addAttribute("inquiries", customerService.getMyInquiries(member));
        return "inquiry-list";
    }

    // 단체관람/대관문의
    @GetMapping("/group")
    public String groupInquiry() {
        return "group-inquiry";
    }
}
