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

    // [1. 수정] Domain -> Entity 변환 규칙 추가
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "memberId", target = "member")
    PostEntity toPostEntity(Post post);

    // [2. 수정] Entity -> Domain 변환 규칙 추가
    @Mapping(source = "id", target = "postId")
    @Mapping(source = "member.id", target = "memberId")
    Post toDomain(PostEntity postEntity);

    // [3. 수정] Entity 수정 시 규칙 추가
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "member", ignore = true) // 공고 수정 시 작성자는 변경되지 않으므로 무시
    void updateEntityFromDomain(Post post, @MappingTarget PostEntity postEntity);

    PostRes toPostRes(PostData postData);
    /**
     * [4. 추가] MapStruct가 MemberId를 MemberEntity로 변환하는 방법을 알려주는 헬퍼 메서드
     */
    default MemberEntity memberIdToMemberEntity(MemberId memberId) {
        if (memberId == null) {
            return null;
        }
        // MemberEntity의 실제 구현에 따라 ID를 설정하는 방식이 달라질 수 있습니다.
        // 이 예제에서는 MemberEntity가 ID를 설정할 수 있는 방법(builder, setter 등)이 있다고 가정합니다.
        return MemberEntity.builder()
                .id(memberId.getId())
                .build();
    }

    // MapStruct가 Long을 PostId로 변환할 때 사용할 메서드
    default PostId longToPostId(Long id) {
        return id != null ? new PostId(id) : null;
    }

    // MapStruct가 Long을 MemberId로 변환할 때 사용할 메서드
    default MemberId longToMemberId(Long id) {
        return id != null ? new MemberId(id) : null;
    }
}
