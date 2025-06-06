package com.lec.spring.repository;

// Repository layer(aka. Data layer)
// DataSource (DB) 등에 대한 직접적인 접근

import com.lec.spring.domain.Post;

import java.util.List;

public interface PostRepository {

    // 새글 작성(INSERT) <- Post (작성자, 제목, 내용)
    int save(Post post);

    // 특정 id 글 내용 읽기 (SELECT)
    // 만약 해당 id 의 글 없으면 null 리턴함
    Post findById(Long id);

    // 특정 id 글 조회수 +1 증가 (UPDATE)
    int incViewCnt(Long id);

    // 전체 글 목록 : 최신순 (SELECT) -> List<>
    List<Post> findAll();

    // 특정 id 글 수정 (제목, 내용) (UPDATE) <- Post(id, subject, content)
    int update(Post post);

    // 특정 id 글 삭제하기 (DELETE) <= Post(id)
    int delete(Post post);

    // 페이징
    // from 부터 rows 개 만큰 SELECT
    List<Post> selectFromRow(int from, int rows);

    // 전체 글의 개수
    int countAll();

}












