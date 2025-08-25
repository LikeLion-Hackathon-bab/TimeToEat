package com.example.timetoeat.domain.post.adap.out.mapper;

import com.example.timetoeat.domain.post.application.port.data.PostData;
import com.example.timetoeat.domain.post.adap.out.persistence.PostEntity;
import com.example.timetoeat.domain.post.application.port.dto.AuthorInfo;
import com.example.timetoeat.domain.post.domain.Post;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import com.example.timetoeat.domain.post.application.port.dto.response.PostRes;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

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

    @Mapping(source = "postData", target = "author", qualifiedByName = "mapAuthor")
    PostRes toPostRes(PostData postData);

    @Named("mapAuthor")
    default AuthorInfo mapAuthor(PostData postData) {
        return new AuthorInfo(
                postData.authorName(),
                postData.authorProfileImageUrl()
        );
    }

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
