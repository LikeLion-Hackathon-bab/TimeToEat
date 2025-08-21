package com.example.timetoeat.domain.member.entity;

import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class Member {

    @Getter private final MemberId memberId;

}
