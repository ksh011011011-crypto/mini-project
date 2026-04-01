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

@Service
@RequiredArgsConstructor
public class KakaoOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        Long kakaoId = (Long) attributes.get("id");

        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = kakaoAccount != null
                ? (Map<String, Object>) kakaoAccount.get("profile") : null;

        String nickname  = profile != null ? (String) profile.get("nickname") : "카카오사용자";
        String email     = kakaoAccount != null ? (String) kakaoAccount.get("email") : "";
        String username  = "kakao_" + kakaoId;

        Optional<Member> existing = memberRepository.findByUsername(username);
        Member member;
        if (existing.isEmpty()) {
            member = Member.builder()
                    .username(username).password("")
                    .name(nickname).email(email != null ? email : "")
                    .grade("일반").totalSpent(0).role("ROLE_USER")
                    .build();
            memberRepository.save(member);
        } else {
            member = existing.get();
            if (!nickname.equals(member.getName())) {
                member.setName(nickname);
                memberRepository.save(member);
            }
        }

        // loginUsername 키를 추가해 getName() = "kakao_XXX" 로 반환
        Map<String, Object> modifiedAttrs = new HashMap<>(attributes);
        modifiedAttrs.put("loginUsername", username);

        return new DefaultOAuth2User(
                List.of(new OAuth2UserAuthority(member.getRole(), modifiedAttrs)),
                modifiedAttrs,
                "loginUsername"   // authentication.getName() = "kakao_XXX"
        );
    }
}
