package com.lec.spring.repository;

import com.lec.spring.domain.User;

public interface UserRepository {

    // 특정 id (PK) 의 user 리턴
    User findById(Long id);

    // 특정 username 의 user 리턴
    User findByUsername(String username);

    // 새로운 User 등록
    int save(User user);

    // User 정보 수정
    int update(User user);

}













