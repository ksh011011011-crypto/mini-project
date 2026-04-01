package com.sehyun.cinema.config;

import com.sehyun.cinema.service.CustomUserDetailsService;
import com.sehyun.cinema.service.SocialOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final SocialOAuth2UserService socialOAuth2UserService;

    /** false면 폼 로그인만 사용 (카카오/구글은 나중에 sehyun.oauth2.enabled=true) */
    @Value("${sehyun.oauth2.enabled:false}")
    private boolean oauth2Enabled;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
"/", "/movies/**", "/membership", "/store", "/special-hall", "/customer/**",
                                "/events", "/events/**", "/schedule",
                                "/login", "/join", "/api/**",
                                "/css/**", "/js/**", "/images/**",
                                "/webjars/**", "/error",
                                "/oauth2/**", "/login/oauth2/**",
                                "/prd-claude.html", "/presentation.html"
                ).permitAll()
                // 예매 화면 조회는 비회원도 가능 (결제 확정·마이페이지만 로그인)
                .requestMatchers(HttpMethod.GET, "/booking").permitAll()
                .requestMatchers("/booking/**", "/mypage/**", "/reviews/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(socialOAuth2UserService)
                )
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // CSRF: API 경로만 비활성화, 나머지는 기본(세션) 방식 사용
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
            )
            .userDetailsService(userDetailsService);

        if (oauth2Enabled) {
            http.oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(socialOAuth2UserService)
                )
            );
        }

        return http.build();
    }
}
