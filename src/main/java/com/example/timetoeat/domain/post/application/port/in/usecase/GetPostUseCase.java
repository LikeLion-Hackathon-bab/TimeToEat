package com.example.timetoeat.domain.post.application.port.in.usecase;

import com.example.timetoeat.domain.post.application.port.dto.response.PostRes;

import java.util.List;

public interface GetPostUseCase {
    List<PostRes> getPosts();
}
