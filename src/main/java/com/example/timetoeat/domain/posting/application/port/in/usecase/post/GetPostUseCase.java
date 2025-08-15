package com.example.timetoeat.domain.posting.application.port.in.usecase.post;

import com.example.timetoeat.domain.posting.dto.response.PostRes;

import java.util.List;

public interface GetPostUseCase {
    List<PostRes> getPosts();
}
