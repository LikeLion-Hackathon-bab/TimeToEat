package com.example.timetoeat.domain.post.application.service;

import com.example.timetoeat.domain.post.application.port.in.usecase.GetPostUseCase;
import com.example.timetoeat.domain.post.adap.out.mapper.PostMapper;
import com.example.timetoeat.domain.post.application.port.out.LoadPost;
import com.example.timetoeat.domain.post.application.port.data.PostData;
import com.example.timetoeat.domain.post.domain.dto.response.PostRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPostService implements GetPostUseCase {

    private final LoadPost loadPost;
    private final PostMapper postMapper;

    @Override
    public List<PostRes> getPosts() {
        List<PostData> postDataList = loadPost.findAllPosts();
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
