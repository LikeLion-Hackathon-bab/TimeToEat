package com.example.timetoeat.domain.plans.meeting;

import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.timetoeat.domain.participation.adap.out.persistence.ParticipationRepository;
import com.example.timetoeat.domain.member.entity.MemberEntity;

@Service
@RequiredArgsConstructor
class MeetingService {
    private final MeetingRepository meetings;
    private final ParticipationRepository participants;
    private final com.example.timetoeat.domain.post.adap.out.persistence.PostRepository posts;
    private final com.example.timetoeat.domain.invitation.adap.out.persistence.InvitationRepository invitations;
    private final ConfirmedInvitationRepository invitationResults;
    record Room(long id, String location) {}
    @Transactional(readOnly=true)
    public List<Room> rooms(Long viewer) {
        if (viewer == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return posts.findWaitingRooms(viewer).stream().map(p -> new Room(p.getId(), p.getLocation())).toList();
    }
    record Person(long userId, String name) {
        Person(MemberEntity member) { this(member.getId(), member.getUsername()); }
    }
    record View(long id, List<Person> participants, String location, String time, String restaurant,
                boolean isCompleted, String restaurantType, String type, String detailPath) {}
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("M월 d일 HH:mm");
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    record InvitationResult(LocalDateTime meetingAt, String location, String restaurant) {}

    /** 이미 다른 값으로 확정된 경우. 저장된 값을 담아 Socket이 그 값으로 진행할 수 있게 한다. */
    static class AlreadyConfirmed extends RuntimeException {
        final String message;
        final Object stored;
        AlreadyConfirmed(String message, Object stored) { super(message); this.message = message; this.stored = stored; }
    }
    record StoredRestaurant(String restaurant) {}
    record StoredInvitationResult(String meetingAt, String location, String restaurant) {}

    @Transactional
    public void record(long id, String restaurant) {
        // 공고 행을 잠근 뒤 상태를 본다. 대기실 나가기(RoomLeaveService)가 마지막 참여자를 빼고 공고를 다시 OPEN으로
        // 돌린 직후에 늦은 결과가 오면, 모집 중인 공고에 확정 결과가 붙는다. 그래서 OPEN이면 409(본문 data 없음)를 준다.
        var post = posts.findForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공고를 찾을 수 없습니다."));
        if (post.getStatus() == com.example.timetoeat.domain.post.domain.Status.OPEN)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "대기실이 없는 공고입니다.");
        meetings.record(id, restaurant);
        var stored = meetings.findById(id).orElseThrow().getRestaurant();
        if (!stored.equals(restaurant))
            throw new AlreadyConfirmed("이미 다른 식당으로 확정된 공고입니다.", new StoredRestaurant(stored));
    }
    /** 개별 초대 결과 저장. 같은 값 재전송은 성공, 다른 값은 409, 수락 안 된 초대는 409, 없는 초대는 404. */
    @Transactional
    public void recordInvitation(long id, java.time.OffsetDateTime meetingAt, String location, String restaurant) {
        var invitation = invitations.findForUpdate(id) // 취소(RoomLeaveService)와 직렬화하고 커밋된 상태를 읽는다
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "초대를 찾을 수 없습니다."));
        if (invitation.getStatus() != com.example.timetoeat.domain.invitation.domain.InvitationStatus.APPROVED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "수락된 초대가 아닙니다.");
        // 같은 순간을 한국 벽시계로 저장(post.meeting_at과 같은 규칙). DATETIME(6) 정밀도에 맞춘다.
        var local = meetingAt.atZoneSameInstant(SEOUL).toLocalDateTime().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        invitationResults.record(id, local, location, restaurant);
        var stored = invitationResults.findById(id).orElseThrow();
        if (!stored.getMeetingAt().equals(local) || !stored.getLocation().equals(location)
                || !stored.getRestaurant().equals(restaurant))
            throw new AlreadyConfirmed("이미 다른 결과로 확정된 초대입니다.", new StoredInvitationResult(
                    stored.getMeetingAt().atZone(SEOUL).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")), stored.getLocation(), stored.getRestaurant()));
    }

    /** 내 밥약 목록: 공고 밥약 + 초대 밥약, 만남 시각 내림차순. */
    @Transactional(readOnly=true)
    public List<View> all(Long viewer) {
        if (viewer == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var merged = new ArrayList<Map.Entry<LocalDateTime, View>>();
        merged.addAll(announcementRows(viewer, null));
        merged.addAll(invitationRows(viewer, null));
        merged.sort(Comparator.<Map.Entry<LocalDateTime, View>, LocalDateTime>comparing(Map.Entry::getKey).reversed()
                .thenComparing(e -> e.getValue().type())
                .thenComparing(e -> e.getValue().id(), Comparator.reverseOrder()));
        return merged.stream().map(Map.Entry::getValue).toList();
    }

    @Transactional(readOnly=true)
    public Optional<View> invitationDetail(Long viewer, long id) {
        if (viewer == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return invitationRows(viewer, id).stream().map(Map.Entry::getValue).findFirst();
    }

    private List<Map.Entry<LocalDateTime, View>> invitationRows(long viewer, Long id) {
        return invitationResults.visible(viewer, id).stream().map(row -> {
            var i = row.getInvitation();
            var people = List.of(new Person(i.getInviter()), new Person(i.getInvitee()));
            return Map.entry(row.getMeetingAt(), new View(row.getInvitationId(), people, row.getLocation(),
                    row.getMeetingAt().format(TIME), row.getRestaurant(),
                    row.getMeetingAt().isBefore(LocalDateTime.now(SEOUL)), "",
                    "INVITATION", "/meetings/invitations/" + row.getInvitationId()));
        }).toList();
    }

    /** 공고 밥약. id가 있으면 그 공고만(상세). */
    @Transactional(readOnly=true)
    public List<View> list(Long viewer, Long id) {
        if (viewer == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return announcementRows(viewer, id).stream().map(Map.Entry::getValue).toList();
    }

    private List<Map.Entry<LocalDateTime, View>> announcementRows(long viewer, Long id) {
        var rows = meetings.visible(viewer, id);
        if (rows.isEmpty()) return List.of();
        var people = new HashMap<Long, LinkedHashSet<Person>>();
        for (var row : rows) {
            var group = new LinkedHashSet<Person>();
            group.add(new Person(row.getPost().getMember()));
            people.put(row.getPostId(), group);
        }
        for (var a : participants.findMembersByPost(rows.stream().map(ConfirmedMeeting::getPostId).toList()))
            people.get(a.getPost().getId()).add(new Person(a.getMember()));
        return rows.stream().map(row -> {
            var post = row.getPost();
            return Map.entry(post.getMeetingAt(), new View(row.getPostId(), List.copyOf(people.get(row.getPostId())),
                post.getLocation(), post.getMeetingAt().format(TIME), row.getRestaurant(),
                post.getMeetingAt().isBefore(LocalDateTime.now(SEOUL)), "",
                "ANNOUNCEMENT", "/meetings/" + row.getPostId()));
        }).toList();
    }
}
