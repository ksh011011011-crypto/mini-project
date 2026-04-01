package com.sehyun.cinema.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 화면 공통: 소셜 로그인 노출 여부 등
 */
@ControllerAdvice
public class UiModelAdvice {

    @Value("${sehyun.oauth2.enabled:false}")
    private boolean oauth2Enabled;

    @ModelAttribute
    public void addUiFlags(Model model) {
        model.addAttribute("oauth2Enabled", oauth2Enabled);
    }
}
