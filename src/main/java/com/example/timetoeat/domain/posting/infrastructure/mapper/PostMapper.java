package com.example.timetoeat.domain.posting.infrastructure.mapper;

import com.example.timetoeat.domain.posting.core.application.gateway.data.PostData;
import com.example.timetoeat.domain.posting.infrastructure.persistence.post.PostEntity;
import com.example.timetoeat.domain.posting.core.domain.model.post.Post;
import com.example.timetoeat.domain.posting.core.domain.vo.member.MemberId;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;
import com.example.timetoeat.domain.posting.dto.response.PostRes;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "memberId", target = "member")
    PostEntity toPostEntity(Post post);

    @Mapping(source = "id", target = "postId")
    @Mapping(source = "member.id", target = "memberId")
    Post toDomain(PostEntity postEntity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "member", ignore = true) // 공고 수정 시 작성자는 변경되지 않으므로 무시
    void updateEntityFromDomain(Post post, @MappingTarget PostEntity postEntity);

    PostRes toPostRes(PostData postData);

    default MemberEntity memberIdToMemberEntity(MemberId memberId) {
        if (memberId == null) {
            return null;
        }
        return MemberEntity.builder()
                .id(memberId.getId())
                .build();
    }

    default PostId longToPostId(Long id) {
        return id != null ? new PostId(id) : null;
    }

    default MemberId longToMemberId(Long id) {
        return id != null ? new MemberId(id) : null;
    }
}
