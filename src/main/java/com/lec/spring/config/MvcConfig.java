package com.lec.spring.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    // PasswordEncoder 를 bean 으로 IoC 에 등록
    // IoC 에 등록된다, IoC 내에선 '어디서든' 가져다가 사용할수 있다.
    @Bean
    public PasswordEncoder encoder(){
        System.out.println("PasswordEncoder bean 생성");
        return new BCryptPasswordEncoder();
    }


    @Value("${app.upload.path}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        System.out.println("🎨MvcConfig.addResourceHandlers() 호출");

        //  /upload/** URL 로 request 가 들어오면
        // upload/ 경로의 static resource 가 응답되도록 함.
        // IntelliJ 의 경우 이 경로를 module 이 아닌 project 이하에 생성해야 한다.
        registry
                .addResourceHandler("/upload/**")
                .addResourceLocations("file:" + uploadDir + "/")
                ;
    }
}





















