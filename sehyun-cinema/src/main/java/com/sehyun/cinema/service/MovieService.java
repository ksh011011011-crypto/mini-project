package com.sehyun.cinema.service;

import com.sehyun.cinema.entity.Heart;
import com.sehyun.cinema.entity.Member;
import com.sehyun.cinema.entity.Movie;
import com.sehyun.cinema.entity.Review;
import com.sehyun.cinema.repository.HeartRepository;
import com.sehyun.cinema.repository.MovieRepository;
import com.sehyun.cinema.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MovieService {

    private final MovieRepository movieRepository;
    private final ReviewRepository reviewRepository;
    private final HeartRepository heartRepository;

    // 현재 상영작 TOP 10 (DB 순위 기반, 개봉예정작 순위와 분리)
    @Transactional(readOnly = true)
    public List<Movie> getTop10() {
        return movieRepository.findByStatusAndRankPositionIsNotNullOrderByRankPositionAsc("SHOWING")
            .stream().limit(10).toList();
    }

    // 모든 영화 (순위 포함, 최대 100개)
    @Transactional(readOnly = true)
    public Page<Movie> getMoviesByStatus(String status, int page, int size) {
        if ("UPCOMING".equals(status)) {
            Pageable pageable = PageRequest.of(page, size);
            return movieRepository.findByStatusUpcomingPaged(status, pageable);
        }
        Sort sort = Sort.by(Sort.Direction.ASC, "rankPosition")
            .and(Sort.by(Sort.Direction.DESC, "bookingRate"));
        Pageable pageable = PageRequest.of(page, size, sort);
        return movieRepository.findByStatus(status, pageable);
    }

    // 영화 검색
    @Transactional(readOnly = true)
    public List<Movie> searchMovies(String keyword) {
        return movieRepository.searchByTitle(keyword);
    }

    // 영화 상세 조회
    @Transactional(readOnly = true)
    public Optional<Movie> findById(Long id) {
        return movieRepository.findById(id);
    }

    // 리뷰 목록
    @Transactional(readOnly = true)
    public List<Review> getReviews(Movie movie) {
        return reviewRepository.findByMovieOrderByCreatedAtDesc(movie);
    }

    // 리뷰 작성
    public Review writeReview(Member member, Movie movie, int rating, String content) {
        if (reviewRepository.existsByMemberAndMovie(member, movie)) {
            throw new IllegalArgumentException("이미 리뷰를 작성하셨습니다.");
        }
        Review review = Review.builder()
            .member(member).movie(movie)
            .rating(rating).content(content)
            .build();
        Review saved = reviewRepository.save(review);

        // 평점 평균 업데이트
        Double avg = reviewRepository.calculateAvgRating(movie);
        if (avg != null) movie.updateRating(avg);
        movieRepository.save(movie);

        return saved;
    }

    // 리뷰 수정
    public Review updateReview(Long reviewId, String username, int rating, String content) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
        if (!review.getMember().getUsername().equals(username)) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }
        review.setRating(rating);
        review.setContent(content);
        Review saved = reviewRepository.save(review);

        Double avg = reviewRepository.calculateAvgRating(review.getMovie());
        if (avg != null) review.getMovie().updateRating(avg);

        return saved;
    }

    // 리뷰 삭제
    public void deleteReview(Long reviewId, String username) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
        if (!review.getMember().getUsername().equals(username)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }
        Movie movie = review.getMovie();
        reviewRepository.delete(review);

        Double avg = reviewRepository.calculateAvgRating(movie);
        movie.updateRating(avg != null ? avg : 0.0);
    }

    // 하트 토글 (좋아요/취소)
    public boolean toggleHeart(Member member, Movie movie) {
        Optional<Heart> existing = heartRepository.findByMemberAndMovie(member, movie);
        if (existing.isPresent()) {
            heartRepository.delete(existing.get());
            movie.decrementHeart();
            movieRepository.save(movie);
            return false; // 취소됨
        } else {
            heartRepository.save(Heart.builder().member(member).movie(movie).build());
            movie.incrementHeart();
            movieRepository.save(movie);
            return true; // 추가됨
        }
    }

    // 내가 하트한 영화인지 확인
    @Transactional(readOnly = true)
    public boolean isHearted(Member member, Movie movie) {
        return heartRepository.existsByMemberAndMovie(member, movie);
    }

    // 내가 하트한 영화 목록
    @Transactional(readOnly = true)
    public List<Movie> getHeartedMovies(Member member) {
        return heartRepository.findByMember(member)
            .stream().map(Heart::getMovie).toList();
    }
}
