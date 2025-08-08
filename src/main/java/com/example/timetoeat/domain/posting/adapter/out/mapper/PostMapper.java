package com.example.timetoeat.domain.posting.adapter.out.mapper;

import com.example.timetoeat.domain.posting.adapter.out.persistence.post.PostEntity;
import com.example.timetoeat.domain.posting.domain.model.Post;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PostMapper {
    PostEntity toPostEntity(Post post);

    Post toDomain(PostEntity postEntity);
    /**
     * post(도메인) 객체의 내용으로 postEntity(엔티티) 객체의 내용을 갱신(update)합니다.
     * @MappingTarget 어노테이션이 붙은 파라미터가 수정 대상이 됩니다.
     */
    void updateEntityFromDomain(Post post, @MappingTarget PostEntity postEntity);
}
