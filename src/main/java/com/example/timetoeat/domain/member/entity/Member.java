package com.example.timetoeat.domain.member.entity;

import com.example.timetoeat.domain.posting.core.domain.vo.member.MemberId;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class Member {
    @Getter private final MemberId memberId;

}
