package com.example.timetoeat.domain.post.application.port.in.command;

import com.example.timetoeat.global.common.SelfValidating;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CreatePostCommand extends SelfValidating<CreatePostCommand> {
    @Min(value = 1, message = "모집 인원은 1명 이상이어야 합니다.")
    private final int targetCount;

    @NonNull
    private final LocalDateTime meetingAt;

    @NotBlank(message = "만날 장소를 입력해야 합니다.")
    private final String location;

    @NotBlank(message = "메시지를 입력해야 합니다.")
    @Size(max = 100, message = "메시지는 100자 이내여야 합니다.")
    private final String message;
}
