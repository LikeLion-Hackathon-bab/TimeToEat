package com.example.timetoeat.domain.posting.core.application.port.in.usecase.usecase.post;

import com.example.timetoeat.domain.posting.core.domain.dto.response.PostRes;

import java.util.List;

public interface GetPostUseCase {
    List<PostRes> getPosts();
}
