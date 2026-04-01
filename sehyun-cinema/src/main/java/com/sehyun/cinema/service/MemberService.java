package com.sehyun.cinema.service;

import com.sehyun.cinema.dto.MemberJoinDto;
import com.sehyun.cinema.entity.Member;
import com.sehyun.cinema.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    public Member join(MemberJoinDto dto) {
        if (memberRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()
                && memberRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        Member member = Member.builder()
            .username(dto.getUsername())
            .password(passwordEncoder.encode(dto.getPassword()))
            .name(dto.getName())
            .email(dto.getEmail())
            .phone(dto.getPhone())
            .birthDate(dto.getBirthDate())
            .grade("일반")
            .totalSpent(0)
            .role("ROLE_USER")
            .build();

        return memberRepository.save(member);
    }

    // 사용자 조회
    @Transactional(readOnly = true)
    public Optional<Member> findByUsername(String username) {
        return memberRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Member getByUsername(String username) {
        return memberRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    // 회원 정보 수정
    public void updateMemberInfo(String username, String name, String email, String phone) {
        Member member = getByUsername(username);
        member.setName(name);
        member.setEmail(email);
        member.setPhone(phone);
    }

    // 비밀번호 변경
    public void changePassword(String username, String currentPw, String newPw) {
        Member member = getByUsername(username);
        if (!passwordEncoder.matches(currentPw, member.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        member.setPassword(passwordEncoder.encode(newPw));
    }

    // 아이디 중복 체크
    @Transactional(readOnly = true)
    public boolean isUsernameDuplicate(String username) {
        return memberRepository.existsByUsername(username);
    }
}
