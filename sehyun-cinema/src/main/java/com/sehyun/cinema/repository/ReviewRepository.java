package com.sehyun.cinema.repository;

import com.sehyun.cinema.entity.Member;
import com.sehyun.cinema.entity.Movie;
import com.sehyun.cinema.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByMovieOrderByCreatedAtDesc(Movie movie);

    Optional<Review> findByMemberAndMovie(Member member, Movie movie);

    boolean existsByMemberAndMovie(Member member, Movie movie);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.movie = :movie")
    Double calculateAvgRating(@Param("movie") Movie movie);

    List<Review> findByMemberOrderByCreatedAtDesc(Member member);
}
