package com.sehyun.cinema.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Thymeleaf에서 ${_csrf} 사용. Spring Security 6는 요청 속성에
 * {@link CsrfToken} 또는 {@code Supplier}/{@code DeferredCsrfToken} 형태로 둘 수 있어 둘 다 해석합니다.
 */
@ControllerAdvice
public class CsrfModelAdvice {

    @ModelAttribute
    public void addCsrf(HttpServletRequest request, Model model) {
        CsrfToken token = resolveToken(request);
        model.addAttribute("_csrf", token);
    }

    private static CsrfToken resolveToken(HttpServletRequest request) {
        try {
            Object attr = request.getAttribute(CsrfToken.class.getName());
            CsrfToken fromAttr = tryConvert(attr);
            if (fromAttr != null) {
                return fromAttr;
            }
            Object legacy = request.getAttribute("_csrf");
            return tryConvert(legacy);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static CsrfToken tryConvert(Object attr) {
        if (attr == null) {
            return null;
        }
        if (attr instanceof CsrfToken ct) {
            return ct;
        }
        if (attr instanceof Supplier<?> supplier) {
            Object v = supplier.get();
            if (v instanceof CsrfToken ct) {
                return ct;
            }
        }
        return null;
    }
}
