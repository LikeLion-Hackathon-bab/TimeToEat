package com.example.timetoeat.domain.posting.adapter.out.persistence.post;

import com.example.timetoeat.domain.posting.adapter.out.mapper.PostMapper;
import com.example.timetoeat.domain.posting.application.port.out.Query.GetPostQuery;
import com.example.timetoeat.domain.posting.application.port.out.save.SavePostPort;
import com.example.timetoeat.domain.posting.domain.model.Post;
import com.example.timetoeat.domain.posting.domain.vo.PostId;
import com.example.timetoeat.global.error.GlobalErrorCode;
import com.example.timetoeat.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostPersistenceAdapter implements GetPostQuery, SavePostPort {

    private final PostJpaRepository postJpaRepository;
    private final PostMapper postMapper;

    @Override
    public Post findById(PostId postId) {
        PostEntity postEntity = postJpaRepository.findById(postId.getId())
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND, "공고를 찾을 수 없습니다"));
        return postMapper.toDomain(postEntity);
    }

    @Override
    public void save(Post post) {
        if (post.getPostId() == null) {
            PostEntity postEntity = postMapper.toPostEntity(post);
            postJpaRepository.save(postEntity);
        } else {
            PostEntity postEntity = postJpaRepository.findById(post.getPostId().getId())
                    .orElseThrow(() -> new IllegalStateException("수정할 공고를 찾을 수 없습니다."));

            // Mapper를 사용해 도메인 객체의 변경 내용을 엔티티에 반영합니다.
            postMapper.updateEntityFromDomain(post, postEntity);
        }
    }
}
