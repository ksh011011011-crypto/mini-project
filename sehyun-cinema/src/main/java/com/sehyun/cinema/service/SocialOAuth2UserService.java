package com.sehyun.cinema.service;

import com.sehyun.cinema.entity.Member;
import com.sehyun.cinema.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 카카오 + 구글 소셜 로그인 통합 서비스
 */
@Service
@RequiredArgsConstructor
public class SocialOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "kakao" or "google"

        String username;
        String name;
        String email;

        if ("kakao".equals(registrationId)) {
            // ── 카카오 ──────────────────────────────────────────
            Map<String, Object> attrs = oAuth2User.getAttributes();
            Long kakaoId = (Long) attrs.get("id");

            Map<String, Object> kakaoAccount = (Map<String, Object>) attrs.get("kakao_account");
            Map<String, Object> profile = kakaoAccount != null
                    ? (Map<String, Object>) kakaoAccount.get("profile") : null;

            name     = profile != null ? (String) profile.get("nickname") : "카카오사용자";
            email    = kakaoAccount != null ? (String) kakaoAccount.get("email") : "";
            username = "kakao_" + kakaoId;

        } else if ("google".equals(registrationId)) {
            // ── 구글 ────────────────────────────────────────────
            Map<String, Object> attrs = oAuth2User.getAttributes();
            String googleId = (String) attrs.get("sub");

            name     = (String) attrs.getOrDefault("name",  "구글사용자");
            email    = (String) attrs.getOrDefault("email", "");
            username = "google_" + googleId;

        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인: " + registrationId);
        }

        // DB 저장 또는 업데이트
        Optional<Member> existing = memberRepository.findByUsername(username);
        Member member;
        if (existing.isEmpty()) {
            member = Member.builder()
                    .username(username).password("")
                    .name(name).email(email != null ? email : "")
                    .grade("일반").totalSpent(0).role("ROLE_USER")
                    .build();
            memberRepository.save(member);
        } else {
            member = existing.get();
            if (!name.equals(member.getName())) {
                member.setName(name);
                memberRepository.save(member);
            }
        }

        // loginUsername 키를 추가해 getName() = "kakao_XXX" 또는 "google_XXX"
        Map<String, Object> modifiedAttrs = new HashMap<>(oAuth2User.getAttributes());
        modifiedAttrs.put("loginUsername", username);

        return new DefaultOAuth2User(
                List.of(new OAuth2UserAuthority(member.getRole(), modifiedAttrs)),
                modifiedAttrs,
                "loginUsername"
        );
    }
}
