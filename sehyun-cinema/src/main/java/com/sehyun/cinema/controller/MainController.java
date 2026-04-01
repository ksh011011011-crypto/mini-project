package com.sehyun.cinema.controller;

import com.sehyun.cinema.entity.Movie;
import com.sehyun.cinema.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final MovieService movieService;

    @GetMapping("/")
    public String index(Model model) {
        List<Movie> top10 = movieService.getTop10();
        Page<Movie> showing = movieService.getMoviesByStatus("SHOWING", 0, 10);
        Page<Movie> upcoming = movieService.getMoviesByStatus("UPCOMING", 0, 5);
        Page<Movie> arte = movieService.getMoviesByStatus("ARTE", 0, 5);

        model.addAttribute("top10", top10);
        model.addAttribute("showing", showing.getContent());
        model.addAttribute("upcoming", upcoming.getContent());
        model.addAttribute("arte", arte.getContent());
        return "index";
    }

    // 더보기 AJAX (10개씩, 최대 100개)
    @GetMapping("/movies/more")
    public String loadMoreMovies(
            @RequestParam(defaultValue = "SHOWING") String status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Page<Movie> movies = movieService.getMoviesByStatus(status, page, 10);
        model.addAttribute("movies", movies.getContent());
        model.addAttribute("hasNext", movies.hasNext());
        model.addAttribute("currentPage", page);
        return "fragments/movie-cards :: movieCards";
    }

    // 영화 검색
    @GetMapping("/movies/search")
    public String searchMovies(@RequestParam String keyword, Model model) {
        List<Movie> results = movieService.searchMovies(keyword);
        model.addAttribute("movies", results);
        model.addAttribute("keyword", keyword);
        return "fragments/movie-cards :: movieCards";
    }

    // 이벤트 페이지
    @GetMapping("/events")
    public String events() {
        return "events";
    }

    // 상영시간표
    @GetMapping("/schedule")
    public String schedule() {
        return "schedule";
    }

    /** 매점·스낵 메뉴(안내용). 실제 결제는 현장 매표소·키오스크 */
    @GetMapping("/store")
    public String store() {
        return "store";
    }

    /** IMAX·4DX 등 스페셜관 시설 안내 (멤버십·할인과 별도 페이지) */
    @GetMapping("/special-hall")
    public String specialHall() {
        return "special-hall";
    }
}
