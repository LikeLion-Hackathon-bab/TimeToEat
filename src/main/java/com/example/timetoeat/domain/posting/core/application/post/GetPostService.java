package com.example.timetoeat.domain.posting.core.application.post;

import com.example.timetoeat.domain.posting.core.application.port.in.usecase.usecase.post.GetPostUseCase;
import com.example.timetoeat.domain.posting.infrastructure.persistence.mapper.PostMapper;
import com.example.timetoeat.domain.posting.core.application.port.out.gateway.post.GetPostQuery;
import com.example.timetoeat.domain.posting.core.application.port.out.gateway.data.PostData;
import com.example.timetoeat.domain.posting.core.domain.dto.response.PostRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPostService implements GetPostUseCase {

    private final GetPostQuery getPostQuery;
    private final PostMapper postMapper;

    @Override
    public List<PostRes> getPosts() {
        List<PostData> postDataList = getPostQuery.findAllPosts();
        return postDataList.stream()
                .map(postData -> new PostRes(
                        postData.getAuthorName(),
                        postData.getCreatedAt(),
                        postData.getMessage(),
                        postData.getMeetingAt(),
                        postData.getLocation(),
                        postData.getParticipantNames()
                ))
                .toList();
    }
}
