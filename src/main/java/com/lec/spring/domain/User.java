package com.lec.spring.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private Long id;

    private String username;
    @JsonIgnore
    private String password;

    @ToString.Exclude
    @JsonIgnore
    private String re_password;  // 비밀번호 확인 입력.

    private String name;
    private String email;

    @JsonIgnore
    private LocalDateTime regDate;

    // OAuth2 Client
    private String provider;
    private String providerId;

}












