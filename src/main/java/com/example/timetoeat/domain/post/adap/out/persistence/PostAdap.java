package com.example.timetoeat.domain.post.adap.out.persistence;

import com.example.timetoeat.domain.post.application.port.data.ParticipationData;
import com.example.timetoeat.domain.post.application.port.data.PostData;
import com.example.timetoeat.domain.post.application.port.out.LoadPost;
import com.example.timetoeat.domain.post.application.port.out.SavePost;
import com.example.timetoeat.domain.post.adap.out.mapper.PostMapper;
import com.example.timetoeat.domain.participation.adap.out.persistence.ParticipationEntity;
import com.example.timetoeat.domain.participation.adap.out.persistence.ParticipationRepository;
import com.example.timetoeat.domain.post.domain.Post;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import com.example.timetoeat.global.error.GlobalErrorCode;
import com.example.timetoeat.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostAdap implements LoadPost, SavePost {

    private final PostRepository postRepository;
    private final ParticipationRepository participationRepository;
    private final PostMapper postMapper;

    @Override
    public Post findById(PostId postId) {
        PostEntity postEntity = postRepository.findById(postId.getId())
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND, "공고를 찾을 수 없습니다"));
        return postMapper.toDomain(postEntity);
    }

    @Override
    public List<PostData> findAllPosts() {
        List<PostEntity> postEntities = postRepository.findAllWithMember();

        List<Long> postIds = postEntities.stream()
                .map(postEntity -> postEntity.getId())
                .collect(Collectors.toList());

        List<ParticipationEntity> participations = participationRepository.findMembersByPost(postIds);

        Map<Long, List<ParticipationData>> participantsMap = participations.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPost().getId(),
                        Collectors.mapping(
                                p -> new ParticipationData(
                                        p.getMember().getId(),
                                        p.getMember().getUsername(),
                                        p.getMember().getProfileImageUrl()
                                ),
                                Collectors.toList()
                        )
                ));
        return postEntities.stream()
                .map(postEntity -> new PostData(
                        postEntity.getId(),
                        postEntity.getCreatedAt(),
                        postEntity.getMember().getId(),
                        postEntity.getMember().getUsername(),
                        postEntity.getMember().getProfileImageUrl(),
                        postEntity.getMessage(),
                        postEntity.getMeetingAt(),
                        postEntity.getLocation(),
                        participantsMap.getOrDefault(postEntity.getId(), List.of()),
                        postEntity.getTargetCount()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public Post save(Post post) {
        if (post.getPostId() == null) {
            PostEntity postEntity = postMapper.toPostEntity(post);
            PostEntity savedPostEntity = postRepository.save(postEntity);
            return postMapper.toDomain(savedPostEntity);
        } else {
            PostEntity postEntity = postRepository.findById(post.getPostId().getId())
                    .orElseThrow(() -> new IllegalStateException("수정할 공고를 찾을 수 없습니다."));
            postMapper.updateEntityFromDomain(post, postEntity);
            return postMapper.toDomain(postEntity);
        }
    }
}
