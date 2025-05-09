package com.lec.spring.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/")
public class HomeController {

    @RequestMapping("/")
    public String home(Model model) {
        return "redirect:/home";
    }

    @RequestMapping("/home")
    public void home(){}

    //-------------------------------------------------------------
    // 암호화 객체 (PasswordEncoder) 동작 확인
    @Autowired
    private PasswordEncoder passwordEncoder;

    @RequestMapping("test1")
    @ResponseBody
    public String test1(@RequestParam(defaultValue = "1234") String password){
        return password + " => " + passwordEncoder.encode(password);
    }

    // --------------------------------------------
    // 현재 Authentication 보기 (디버깅 등 용도로 활용)
    @RequestMapping("/auth")
    @ResponseBody
    public Authentication auth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    // OAuth2 Client 를 사용하여 로그인 경우.
    // Principal 객체는 OAuth2User 타입으로 받아올수도 있다.
    // AuthenticatedPrincipal(I)
    //  └─ OAuth2AuthenticatedPrincipal(I)
    //       └─ OAuth2User (I)
    @RequestMapping("/oauth2")
    @ResponseBody
    public OAuth2User oauth2(Authentication authentication) {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        return oAuth2User;
    }

    @RequestMapping("/oauth2user")
    @ResponseBody
    public Map<String, Object> oauth2user(@AuthenticationPrincipal OAuth2User oAuth2User){
        return (oAuth2User != null) ? oAuth2User.getAttributes() : null;
    }

}












