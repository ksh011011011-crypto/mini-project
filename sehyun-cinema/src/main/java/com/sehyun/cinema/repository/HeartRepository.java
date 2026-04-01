package com.sehyun.cinema.repository;

import com.sehyun.cinema.entity.Heart;
import com.sehyun.cinema.entity.Member;
import com.sehyun.cinema.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface HeartRepository extends JpaRepository<Heart, Long> {
    Optional<Heart> findByMemberAndMovie(Member member, Movie movie);
    boolean existsByMemberAndMovie(Member member, Movie movie);
    List<Heart> findByMember(Member member);
}
