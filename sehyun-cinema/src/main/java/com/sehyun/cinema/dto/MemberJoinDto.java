package com.sehyun.cinema.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class MemberJoinDto {

    @NotBlank(message = "아이디를 입력해 주세요.")
    @Size(min = 4, max = 20, message = "아이디는 4~20자리여야 합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "아이디는 영문, 숫자, 언더스코어만 사용 가능합니다.")
    private String username;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(min = 8, message = "비밀번호는 8자리 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "이름을 입력해 주세요.")
    private String name;

    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @Pattern(regexp = "^(01[0-9]{8,9})?$", message = "올바른 휴대폰 번호를 입력해 주세요. (예: 01012345678)")
    private String phone;

    private LocalDate birthDate;
}
