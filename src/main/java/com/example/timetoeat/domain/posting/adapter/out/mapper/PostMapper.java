package com.example.timetoeat.domain.posting.adapter.out.mapper;

import com.example.timetoeat.domain.posting.adapter.out.persistence.post.PostEntity;
import com.example.timetoeat.domain.posting.application.port.out.data.PostData;
import com.example.timetoeat.domain.posting.domain.model.Post;
import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.vo.PostId;
import com.example.timetoeat.domain.posting.dto.response.PostRes;
import com.example.timetoeat.global.auth.entity.MemberEntity;
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
