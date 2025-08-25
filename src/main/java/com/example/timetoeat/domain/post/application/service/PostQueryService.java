package com.example.timetoeat.domain.post.application.service;

import com.example.timetoeat.domain.post.application.port.dto.AuthorInfo;
import com.example.timetoeat.domain.post.application.port.dto.ParticipationInfo;
import com.example.timetoeat.domain.post.application.port.in.usecase.GetPostUseCase;
import com.example.timetoeat.domain.post.application.port.out.LoadPost;
import com.example.timetoeat.domain.post.application.port.data.PostData;
import com.example.timetoeat.domain.post.application.port.dto.response.PostRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService implements GetPostUseCase {

    private final LoadPost loadPost;


    @Override
    public List<PostRes> getPosts() {
        List<PostData> postDataList = loadPost.findAllPosts();

        return postDataList.stream()
                .map(this::toPostRes)
                .collect(Collectors.toList());
    }

    private PostRes toPostRes(PostData data) {
        AuthorInfo authorInfo = new AuthorInfo(
                data.authorId(),
                data.authorName(),
                data.authorProfileImageUrl()
        );

        List<ParticipationInfo> participantInfoList = data.participants().stream()
                .map(p -> new ParticipationInfo(p.authorId(),p.name(), p.profileImageUrl()))
                .collect(Collectors.toList());

        return new PostRes(
                data.postId(),
                data.createdAt(),
                authorInfo,
                data.message(),
                data.meetingAt(),
                data.location(),
                participantInfoList,
                data.targetCount()
        );
    }
}
