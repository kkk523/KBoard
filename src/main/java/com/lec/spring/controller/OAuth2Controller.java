package com.lec.spring.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lec.spring.domain.User;
import com.lec.spring.domain.oauth.KakaoOAuthToken;
import com.lec.spring.domain.oauth.KakaoProfile;
import com.lec.spring.service.UserService;
import com.lec.spring.util.U;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/oauth2")
public class OAuth2Controller {

    // kakao 로그인
    @Value("${app.oauth2.kakao.client-id}")
    private String kakaoClientId;
    @Value("${app.oauth2.kakao.redirect-uri}")
    private String kakaoRedirectUri;
    @Value("${app.oauth2.kakao.token-uri}")
    private String kakaoTokenUri;
    @Value("${app.oauth2.kakao.user-info-uri}")
    private String kakaoUserInfoUri;
    @Value("${app.oauth2.password}")
    private String oauth2Password;

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public OAuth2Controller(UserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }


    @GetMapping("/kakao/callback")
    public String kakaoCallBack(String code){  // Kakao 가 보내준 code 값 받아오기
        //------------------------------------------------------------------
        // ■ code 값 확인
        //   code 값을 받았다는 것은 인증 완료 되었다는 뜻..
                System.out.println("\n🐱<<카카오 인증 완료>>\ncode: " + code);


        //----------------------------------------------------------------------
        // ■ Access token 받아오기 <= code 값 사용
        // 이 Access token 을 사용하여  Kakao resource server 에 있는 사용자 정보를 받아오기 위함.
        KakaoOAuthToken token = kakaoAccessToken(code);

        //------------------------------------------------------------------------
        // ■ 사용자 정보 요청 <= Access Token 사용
        KakaoProfile profile = kakaoUserInfo(token.getAccess_token());

        //------------------------------------------------------------------------
        // ■ 회원 가입 시키기 <= KakaoProfile (사용자 정보) 사용
        User kakaoUser = registerKakaoUser(profile);

        //------------------------------------------------------------------------
        // ■ 로그인 처리
        loginKakaoUser(kakaoUser);

        //-------------------------------------------------
        return "redirect:/";
    }

    //----------------------------------------------------------------------------
    // 로그인 시키기
    private void loginKakaoUser(User kakaoUser) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                kakaoUser.getUsername(),
                oauth2Password
        );

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        SecurityContext sc  = SecurityContextHolder.getContext();
        sc.setAuthentication(authentication);


        U.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
        System.out.println("🏀Kakao 인증 로그인 처리 완료");
    }

    //-----------------------------------------------------------------------------
    // 회원가입 시키기  (username, password, name 필요)
    // Kakao 로그인 한 회원을 User 에 등록하기
    private User registerKakaoUser(KakaoProfile profile) {

        // 새로이 가입시킬 username 을 생성 (unique 해야 한다!)
        String provider = "KAKAO";
        String providerId = "" + profile.getId();
        String username = provider + "_" + providerId;
        String name = profile.getKakaoAccount().getProfile().getNickname();
        String password = oauth2Password;

        System.out.println("""
                🏈[카카오 인증 회원 정보]
                    username: %s
                    name: %s
                    password: %s
                    provider: %s
                    providerId: %s
                """.formatted(username, name, password, provider, providerId));

        // 회원 가입 진행하기 전에
        // 이미 가입한 회원인지, 혹은 비가입자인지 체크하여야 한다
        User user = userService.findByUsername(username);
        if(user == null){  // 미가입자인 경우에만 회원 가입 진행
            User newUser = User.builder()
                    .username(username)
                    .name(name)
                    .password(password)
                    .provider(provider)
                    .providerId(providerId)
                    .build();

            int cnt = userService.register(newUser);  // 회원 가입! INSERT
            if(cnt > 0){
                System.out.println("🏈[Kakao 인증 회원 가입 성공]");
                user = userService.findByUsername(username);  // 다시 읽어온다.  regDate 등의 정보.
            } else {
                System.out.println("🏈[Kakao 인증 회원 가입 실패]");
            }

        } else {
            System.out.println("🏈[Kakao 인증. 이미 가입된 회원입니다]");
        }

        return user;
    }

    //-----------------------------------------------------------------------------
    // Kakao 사용자 정보 요청하기
    private KakaoProfile kakaoUserInfo(String accessToken) {
        // POST 방식으로 데이터 요청
        RestTemplate rt = new RestTemplate();

        // header 준비
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

        // body 는 필요없다.
        HttpEntity<MultiValueMap<String, String>> kakaoProfileRequest =
                new HttpEntity<>(headers);   // header 만 넘겨준다.

        // 요청
        ResponseEntity<String> response = rt.exchange(
                                    kakaoUserInfoUri,
                                    HttpMethod.POST,
                                    kakaoProfileRequest,
                                    String.class);

        System.out.println("👩‍🌾카카오 사용자 profile 요청 응답: " + response);
        System.out.println("👩‍🌾카카오 사용자 profile 응답 body: " + response.getBody());

        //------------------------------------------------------
        // 사용자정보(JSON) -> Java 로 받아내기
        ObjectMapper mapper = new ObjectMapper();
        KakaoProfile profile = null;

        try {
            profile = mapper.readValue(response.getBody(), KakaoProfile.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // 확인
        System.out.println("""
                👩‍🌾[카카오 회원정보]
                id: %s
                nickname: %s
                """.formatted(profile.getId(), profile.getKakaoAccount().getProfile().getNickname()));

        return profile;
    }

    //-----------------------------------------------------------------------------
    // Kakao Access Token 받아오기
    public KakaoOAuthToken kakaoAccessToken(String code){
        // POST 방식으로 key-value 형식으로 데이터를 요청 (카카오 서버쪽으로)
        RestTemplate rt = new RestTemplate();

        // header 준비 (HttpHeader)
        HttpHeaders headers = new HttpHeaders();   // org.springframework.http.HttpHeaders
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        // body 데이터 준비 (HttpBody)
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", code);   // ★인증 직후 받은 code 값 사용!

        // 위 Header 와 Body 를 담은 HttpEntity 생성
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(params, headers);

        // 요청!
        ResponseEntity<String> response = rt.exchange(
                kakaoTokenUri,
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class
                );

        System.out.println("🐷카카오 AccessToken 요청 응답: " + response);
        // body 만 확인해보기
        System.out.println("🤡카카오 AccessToken 응답 body: " + response.getBody());

        // Json -> Java Object
        ObjectMapper mapper = new ObjectMapper();
        KakaoOAuthToken token = null;

        try {
            token = mapper.readValue(response.getBody(), KakaoOAuthToken.class);
            // AccessToken 확인
            System.out.println("🌐카카오 AceessToken: " + token.getAccess_token());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return token;

    } // end kakaoAccessToken()


}


























