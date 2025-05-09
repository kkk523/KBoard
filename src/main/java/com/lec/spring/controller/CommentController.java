package com.lec.spring.controller;

import com.lec.spring.domain.Comment;
import com.lec.spring.domain.QryCommentList;
import com.lec.spring.domain.QryResult;
import com.lec.spring.domain.User;
import com.lec.spring.service.CommentService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController  // data 를 response 한다 (VIEW 를 리턴하는게 아니다)
@RequestMapping("/comment")
public class CommentController {

    private final CommentService commentService;


    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }


    @GetMapping("/test1")
    public QryCommentList test1(){
        QryCommentList list = new QryCommentList();

        list.setCount(1);
        list.setStatus("OK");

        Comment cmt = Comment.builder()
                .user(User.builder().username("우동").id(34L).regDate(LocalDateTime.now()).name("보름달").build())
                .content("정말 재미있나?")
                .regDate(LocalDateTime.now())
                .build();

        list.setList(List.of(cmt));

        return list;
    }

    @GetMapping("/list/{postId}")
    public QryCommentList list(@PathVariable Long postId){
        return commentService.list(postId);
    }

    @PostMapping("/write")
    public QryResult write(
            @RequestParam("post_id") Long postId,
            @RequestParam("user_id") Long userId,
            String content
    ){
        return commentService.write(postId, userId, content);
    }

    @PostMapping("/delete")
    public QryResult delete(Long id){
        return commentService.delete(id);
    }


}














