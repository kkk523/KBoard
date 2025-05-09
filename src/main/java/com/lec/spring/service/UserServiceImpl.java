package com.lec.spring.service;

import com.lec.spring.domain.Authority;
import com.lec.spring.domain.User;
import com.lec.spring.repository.AuthorityRepository;
import com.lec.spring.repository.UserRepository;
import org.apache.ibatis.session.SqlSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;

    public UserServiceImpl(SqlSession sqlSession, PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = sqlSession.getMapper(UserRepository.class);
        this.authorityRepository = sqlSession.getMapper(AuthorityRepository.class);
    }

    // username(회원 아이디)의 User 정보 읽어오기.
    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username.toUpperCase());
    }

    // 특정 username 의 회원이 존재하는지 확인
    @Override
    public boolean isExist(String username) {
        User user = findByUsername(username.toUpperCase());
        return (user != null);
    }

    // 신규회원 등록
    @Override
    public int register(User user) {
        user.setUsername(user.getUsername().toUpperCase());   // DB 에는 username 을 대문자로 저장할거다!
        user.setPassword(passwordEncoder.encode(user.getPassword()));  // password 는 암호화 하여 저장.
        userRepository.save(user);   // 새로 회원(User) 저장.  id 값 받아옴.

        // 신규회원은 ROLE_MEMBER 권한 기본적으로 부여합니다.
        Authority auth = authorityRepository.findByName("ROLE_MEMBER");

        Long userId = user.getId();
        Long authId = auth.getId();
        authorityRepository.addAuthority(userId, authId);

        return 1;
    }

    // 특정 사용자(id) 의 authoritiy (들)
    @Override
    public List<Authority> selectAuthoritiesById(Long id) {
        User user = userRepository.findById(id);
        return authorityRepository.findByUser(user);
    }
}













