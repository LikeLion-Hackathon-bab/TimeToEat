package com.example.timetoeat.domain.post.application.service;

import com.example.timetoeat.domain.post.application.port.dto.AuthorInfo;
import com.example.timetoeat.domain.post.application.port.dto.ParticipationInfo;
import com.example.timetoeat.domain.post.application.port.in.usecase.GetPostUseCase;
import com.example.timetoeat.domain.post.application.port.out.LoadPost;
import com.example.timetoeat.domain.post.application.port.data.PostData;
import com.example.timetoeat.domain.post.application.port.dto.response.PostRes;
import com.example.timetoeat.domain.post.domain.CloseReason;
import com.example.timetoeat.domain.post.domain.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Override
    public PostRes getPost(com.example.timetoeat.domain.post.domain.vo.post.PostId postId) {
        return toPostRes(loadPost.findPostData(postId));
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
                data.targetCount(),
                data.expiredAt(),
                effectiveStatus(data).name(),
                effectiveReason(data) == null ? null : effectiveReason(data).name()
        );
    }

    /** 만료 시각이 지난 OPEN(스케줄러 반영 전 최대 1분)은 이미 신청할 수 없으므로 CLOSED·EXPIRED로 보여 준다. */
    private static boolean lazilyExpired(PostData data) {
        return data.status() == Status.OPEN && data.expiredAt() != null && !data.expiredAt().isAfter(LocalDateTime.now());
    }

    private static Status effectiveStatus(PostData data) {
        return lazilyExpired(data) ? Status.CLOSED : data.status();
    }

    private static CloseReason effectiveReason(PostData data) {
        return lazilyExpired(data) ? CloseReason.EXPIRED : data.closeReason();
    }
}
