package com.example.timetoeat.domain.plans.meeting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import com.example.timetoeat.global.auth.model.CustomOauth2User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.example.timetoeat.global.common.ApiResponse;

@RestController
@RequestMapping("/api/v1/meetings")
class MeetingController {
    private final MeetingService service;
    private final String key;
    MeetingController(MeetingService service, @Value("${ROOM_RESULT_KEY:}") String key) {
        this.service = service;
        this.key = key;
    }
    record Selection(@NotBlank @Size(max=255) String restaurant) {}
    @PostMapping("/{id}/result")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void record(@PathVariable long id, @RequestHeader("X-Room-Key") String provided, @Valid @RequestBody Selection result) {
        if (key.isBlank() || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        service.record(id, result.restaurant());
    }
    @GetMapping
    ApiResponse<List<MeetingService.View>> list(@AuthenticationPrincipal CustomOauth2User viewer) {
        return ApiResponse.success(service.list(viewer == null ? null : viewer.getMemberId(), null), "밥약 목록");
    }
    @GetMapping("/rooms")
    ApiResponse<List<MeetingService.Room>> rooms(@AuthenticationPrincipal CustomOauth2User viewer) {
        return ApiResponse.success(service.rooms(viewer == null ? null : viewer.getMemberId()), "참여 중인 대기실");
    }
    @GetMapping("/{id}")
    ApiResponse<MeetingService.View> detail(@AuthenticationPrincipal CustomOauth2User viewer, @PathVariable long id) {
        return ApiResponse.success(service.list(viewer == null ? null : viewer.getMemberId(),id).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)), "밥약 상세");
    }
}
