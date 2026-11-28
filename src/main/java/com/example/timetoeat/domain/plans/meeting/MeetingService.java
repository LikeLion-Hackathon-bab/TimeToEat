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
                boolean isCompleted, String restaurantType) {}

    @Transactional
    public void record(long id, String restaurant) {
        if (!posts.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "공고를 찾을 수 없습니다.");
        meetings.record(id, restaurant);
        if (!meetings.findById(id).orElseThrow().getRestaurant().equals(restaurant))
            throw new ResponseStatusException(HttpStatus.CONFLICT);
    }
    @Transactional(readOnly=true)
    public List<View> list(Long viewer, Long id) {
        if (viewer == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
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
            return new View(row.getPostId(), List.copyOf(people.get(row.getPostId())), post.getLocation(),
                post.getMeetingAt().format(DateTimeFormatter.ofPattern("M월 d일 HH:mm")), row.getRestaurant(),
                post.getMeetingAt().isBefore(LocalDateTime.now(ZoneId.of("Asia/Seoul"))), "");
        }).toList();
    }
}
