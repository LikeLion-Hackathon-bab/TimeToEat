package com.example.timetoeat.domain.push.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushTokenRequest(@NotBlank @Size(max = 512) String token, @Size(max = 32) String platform) {
}
