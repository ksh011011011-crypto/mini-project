package com.sehyun.cinema.controller;

import com.sehyun.cinema.entity.Member;
import com.sehyun.cinema.entity.Movie;
import com.sehyun.cinema.entity.Review;
import com.sehyun.cinema.service.MemberService;
import com.sehyun.cinema.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final MemberService memberService;

    @GetMapping
    public String movieList(@RequestParam(defaultValue = "SHOWING") String status,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        Page<Movie> movies = movieService.getMoviesByStatus(status, page, 20);
        model.addAttribute("movies", movies);
        model.addAttribute("status", status);
        model.addAttribute("currentPage", page);
        return "movie-list";
    }

    @GetMapping("/{id}")
    public String movieDetail(@PathVariable Long id,
                              Authentication authentication,
                              Model model) {
        Movie movie = movieService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("영화를 찾을 수 없습니다."));
        List<Review> reviews = movieService.getReviews(movie);
        model.addAttribute("movie", movie);
        model.addAttribute("reviews", reviews);

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            try {
                Member member = memberService.getByUsername(authentication.getName());
                model.addAttribute("isHearted", movieService.isHearted(member, movie));
                model.addAttribute("currentMember", member);
                boolean alreadyReviewed = reviews.stream()
                        .anyMatch(r -> r.getMember().getUsername().equals(authentication.getName()));
                model.addAttribute("alreadyReviewed", alreadyReviewed);
            } catch (Exception ignored) {}
        }
        return "movie-detail";
    }

    @PostMapping("/{id}/reviews")
    public String writeReview(@PathVariable Long id,
                              @RequestParam int rating,
                              @RequestParam String content,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            Member member = memberService.getByUsername(authentication.getName());
            Movie movie = movieService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("영화를 찾을 수 없습니다."));
            movieService.writeReview(member, movie, rating, content);
            redirectAttributes.addFlashAttribute("success", "리뷰가 등록되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/movies/" + id;
    }

    @PostMapping("/reviews/{reviewId}/delete")
    public String deleteReview(@PathVariable Long reviewId,
                               @RequestParam Long movieId,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            movieService.deleteReview(reviewId, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "리뷰가 삭제되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/movies/" + movieId;
    }
}
