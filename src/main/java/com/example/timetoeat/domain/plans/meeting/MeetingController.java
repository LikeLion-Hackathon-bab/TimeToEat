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
    private final RoomEntryService roomEntry;
    MeetingController(MeetingService service, RoomEntryService roomEntry, @Value("${ROOM_RESULT_KEY:}") String key) {
        this.service = service;
        this.roomEntry = roomEntry;
        this.key = key;
    }
    record Selection(@NotBlank @Size(max=255) String restaurant) {}
    /** 409 바디: 기존 형식 {code, message}에 data(저장된 결과)를 더한다. 기존 클라이언트는 data를 무시해도 된다. */
    @ExceptionHandler(MeetingService.AlreadyConfirmed.class)
    org.springframework.http.ResponseEntity<ApiResponse<Object>> alreadyConfirmed(MeetingService.AlreadyConfirmed e) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.builder().code("HTTP_409").message(e.message).data(e.stored).build());
    }
    private void requireRoomKey(String provided) {
        if (key.isBlank() || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    @PostMapping("/{id}/result")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void record(@PathVariable long id, @RequestHeader("X-Room-Key") String provided, @Valid @RequestBody Selection result) {
        requireRoomKey(provided);
        service.record(id, result.restaurant());
    }
    /** 계약: qa-cloud/contracts/invitation-result.md. meetingAt은 오프셋 포함 ISO 8601만 받는다. */
    record InvitationResult(Long invitationId, @NotNull java.time.OffsetDateTime meetingAt,
                            @NotBlank @Size(max=255) String location, @NotBlank @Size(max=255) String restaurant) {}
    @PostMapping("/invitations/{id}/result")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void recordInvitation(@PathVariable long id, @RequestHeader("X-Room-Key") String provided,
                          @Valid @RequestBody InvitationResult result) {
        requireRoomKey(provided);
        if (result.invitationId() != null && result.invitationId() != id)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "경로와 본문의 초대 번호가 다릅니다.");
        service.recordInvitation(id, result.meetingAt(), result.location(), result.restaurant());
    }
    @GetMapping("/invitations/{id}")
    ApiResponse<MeetingService.View> invitationDetail(@AuthenticationPrincipal CustomOauth2User viewer, @PathVariable long id) {
        return ApiResponse.success(service.invitationDetail(viewer == null ? null : viewer.getMemberId(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)), "밥약 상세");
    }
    @GetMapping
    ApiResponse<List<MeetingService.View>> list(@AuthenticationPrincipal CustomOauth2User viewer) {
        return ApiResponse.success(service.all(viewer == null ? null : viewer.getMemberId()), "밥약 목록");
    }
    @PostMapping("/rooms/{id}/enter")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void enter(@AuthenticationPrincipal CustomOauth2User viewer, @PathVariable long id) {
        roomEntry.enter(id, viewer == null ? null : viewer.getMemberId());
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
