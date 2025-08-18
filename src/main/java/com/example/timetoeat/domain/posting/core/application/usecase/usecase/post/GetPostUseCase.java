package com.example.timetoeat.domain.posting.core.application.usecase.usecase.post;

import com.example.timetoeat.domain.posting.dto.response.PostRes;

import java.util.List;

public interface GetPostUseCase {
    List<PostRes> getPosts();
}
