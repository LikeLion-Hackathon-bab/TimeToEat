package com.example.timetoeat.domain.post.domain.vo.post;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class PostId {
    private final Long id;

    public static PostId of(Long id) {
        return new PostId(id);
    }
}
