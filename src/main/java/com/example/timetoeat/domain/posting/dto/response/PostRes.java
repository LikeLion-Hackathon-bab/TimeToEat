package com.example.timetoeat.domain.posting.dto.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor // Jackson이 객체를 생성하기 위해 필요
@AllArgsConstructor
public class PostRes{
    private String authorName;
    private LocalDateTime createdAt;
    private String message;
    private LocalDateTime meetingAt;
    private String location;
    private List<String> participantNames;
}
