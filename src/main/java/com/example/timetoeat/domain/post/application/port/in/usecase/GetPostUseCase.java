package com.example.timetoeat.domain.post.application.port.in.usecase;

import com.example.timetoeat.domain.post.application.port.dto.response.PostRes;

import java.util.List;

public interface GetPostUseCase {
    List<PostRes> getPosts();
    /** 공고 하나(마감·만료 포함). 오래된 화면이 409/410을 받은 뒤 사유를 다시 확인할 때 쓴다. */
    PostRes getPost(com.example.timetoeat.domain.post.domain.vo.post.PostId postId);
}
