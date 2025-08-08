package com.example.timetoeat.global.auth.entity;

import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Member {

    @Getter private final MemberId memberId;

}
