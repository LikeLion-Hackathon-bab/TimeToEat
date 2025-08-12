package com.example.timetoeat.domain.posting.adapter.out.persistence.post;

import com.example.timetoeat.domain.posting.adapter.out.mapper.PostMapper;
import com.example.timetoeat.domain.posting.adapter.out.persistence.participation.ParticipationEntity;
import com.example.timetoeat.domain.posting.adapter.out.persistence.participation.ParticipationJpaRepository;
import com.example.timetoeat.domain.posting.application.port.out.Query.GetPostQuery;
import com.example.timetoeat.domain.posting.application.port.out.data.PostData;
import com.example.timetoeat.domain.posting.application.port.out.save.SavePostPort;
import com.example.timetoeat.domain.posting.domain.model.Post;
import com.example.timetoeat.domain.posting.domain.vo.PostId;
import com.example.timetoeat.global.error.GlobalErrorCode;
import com.example.timetoeat.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostPersistenceAdapter implements GetPostQuery, SavePostPort {

    private final PostJpaRepository postJpaRepository;
    private final ParticipationJpaRepository participationJpaRepository;
    private final PostMapper postMapper;

    @Override
    public Post findById(PostId postId) {
        PostEntity postEntity = postJpaRepository.findById(postId.getId())
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND, "공고를 찾을 수 없습니다"));
        return postMapper.toDomain(postEntity);
    }

    @Override
    public List<PostData> findAllPosts() {
        List<PostEntity> postEntities = postJpaRepository.findAllWithMember();

        List<Long> postIds = postEntities.stream()
                .map(postEntity -> postEntity.getId())
                .collect(Collectors.toList());

        List<ParticipationEntity> participations = participationJpaRepository.findMembersByPost(postIds);

        Map<Long, List<String>> participantsMap = participations.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPost().getId(),
                        Collectors.mapping(p -> p.getMember().getUsername(), Collectors.toList())
                ));

        // 조회된 Post 목록을 순회하며 최종 데이터 구조로 조립
        return postEntities.stream()
                .map(postEntity -> new PostData(
                        postEntity.getMember().getUsername(),
                        postEntity.getCreatedAt(),
                        postEntity.getMessage(),
                        postEntity.getMeetingAt(),
                        postEntity.getLocation(),
                        participantsMap.getOrDefault(postEntity.getId(), List.of()) // Map에서 참여자 목록을 찾아 결합
                ))
                .collect(Collectors.toList());
    }

    @Override
    public void save(Post post) {
        if (post.getPostId() == null) {
            PostEntity postEntity = postMapper.toPostEntity(post);
            postJpaRepository.save(postEntity);
        } else {
            PostEntity postEntity = postJpaRepository.findById(post.getPostId().getId())
                    .orElseThrow(() -> new IllegalStateException("수정할 공고를 찾을 수 없습니다."));
            postMapper.updateEntityFromDomain(post, postEntity);
        }
    }
}
