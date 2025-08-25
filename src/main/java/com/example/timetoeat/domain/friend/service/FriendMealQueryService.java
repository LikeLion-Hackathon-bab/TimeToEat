package com.example.timetoeat.domain.friend.service;

import com.example.timetoeat.domain.friend.dto.response.FriendMealItemResponse;
import com.example.timetoeat.domain.meal.entity.MemberMealStatus;
import com.example.timetoeat.domain.meal.repository.MemberMealStatusRepository;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendMealQueryService {

    @Qualifier("kstClock")
    private final Clock clock;

    private final MemberJpaRepository memberRepo;
    private final MemberMealStatusRepository mealRepo;

    // 공개 API: 기존 컨트롤러가 호출
    public List<FriendMealItemResponse> getDemoList(Long meId, String filter) {
        // 1) 실제 가입자 30명 (me 제외)
        List<MemberEntity> members = memberRepo.findTop30ByIdNotOrderByCreatedAtDesc(meId);

        // 2) 벌크로 식사상태 조회
        Map<Long, MemberMealStatus> statusMap = mealRepo.findByMember_IdIn(
                members.stream().map(MemberEntity::getId).toList()
        ).stream().collect(Collectors.toMap(s -> s.getMember().getId(), s -> s));

        // 3) 실제 사용자 아이템 생성
        List<FriendMealItemResponse> items = new ArrayList<>();
        for (MemberEntity m : members) {
            items.add(toItem(m, statusMap.get(m.getId())));
        }

        // 4) 30명 미만이면 데모로 채우기
        if (items.size() < 30) {
            items.addAll(fakeItems(30 - items.size(), 1000L + items.size()));
        }

        // 5) 필터 적용
        return items.stream().filter(it ->
                switch (filter) {
                    case "HUNGRY"     -> it.isHungry();
                    case "NOT_HUNGRY" -> !it.isHungry();
                    default           -> true;
                }
        ).toList();
    }

    // 실제 사용자 → 응답 아이템 변환 (4시간 윈도우 계산)
    private FriendMealItemResponse toItem(MemberEntity m, MemberMealStatus s) {
        String img = (m.getProfileImageUrl() == null || m.getProfileImageUrl().isBlank())
                ? "https://picsum.photos/seed/avatar" + m.getId() + "/80/80"
                : m.getProfileImageUrl();

        LocalDateTime now = LocalDateTime.now(clock);
        Duration window = Duration.ofHours(4);

        LocalDateTime last = (s == null) ? null : s.getLastMealAt();
        LocalDateTime manual = (s == null) ? null : s.getManualFastingSince();

        // 수동 OFF가 더 최근이면 그 시각부터 공복
        LocalDateTime base = last;
        if (manual != null && (base == null || manual.isAfter(base))) {
            long hours = Duration.between(manual, now).toHours();
            return FriendMealItemResponse.of(m.getId(), m.getUsername(), img, true, hours + "시간 공복이에요");
        }

        if (last == null) {
            return FriendMealItemResponse.of(m.getId(), m.getUsername(), img, true, "공복이에요");
        }

        LocalDateTime offAt = last.plus(window);
        if (now.isBefore(offAt)) {
            return FriendMealItemResponse.of(m.getId(), m.getUsername(), img, false, "방금 먹었어요");
        } else {
            long hours = Duration.between(last, now).toHours();
            return FriendMealItemResponse.of(m.getId(), m.getUsername(), img, true, hours + "시간 공복이에요");
        }
    }

    // 데모 아이템 생성(부족분 채우기)
    private List<FriendMealItemResponse> fakeItems(int count, long startId) {
        List<String> names = List.of(
                "박진아","권현욱","고지완","박진홍","임새연","이정훈","박지현","정민우","윤서현","한지우",
                "전민경","박민재","김윤아","최서준","이다은","오지호","문지안","노시윤","하다온","백도윤",
                "임지후","이하준","장수빈","신태윤","권서아","허도영","남유진","배승우","전해린","조시후"
        );
        Random r = new Random(Objects.hash(LocalDate.now(clock)));
        List<FriendMealItemResponse> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            boolean hungry = r.nextBoolean();
            int hours = hungry ? (1 + r.nextInt(10)) : 0;
            String label = hungry ? (hours + "시간 공복이에요") : "방금 먹었어요";
            String img = "https://picsum.photos/seed/friend" + (i+1) + "/80/80";
            list.add(FriendMealItemResponse.of(startId + i, names.get(i % names.size()), img, hungry, label));
        }
        return list;
    }
}
