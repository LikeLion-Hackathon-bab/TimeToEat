package com.example.timetoeat.domain.posting.application.port.out.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostData{
    private String authorName;
    private LocalDateTime createdAt;
    private String message;
    private LocalDateTime meetingAt;
    private String location;
    private List<String> participantNames;
}
