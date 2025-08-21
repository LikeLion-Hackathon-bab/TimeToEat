package com.example.timetoeat.domain.post.application.service;

import com.example.timetoeat.domain.participation.application.port.out.LoadParticipation;
import com.example.timetoeat.domain.post.application.port.out.LoadPreferenceFood;
import com.example.timetoeat.domain.participation.domain.Participation;
import com.example.timetoeat.domain.post.domain.PostEvent;
import com.example.timetoeat.domain.post.domain.PostEventType;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchingService implements PostEventHandler {

    private final PreferenceService preferenceService;
    private final LoadPreferenceFood loadPreferenceFood;
    private final LoadParticipation loadParticipation;

    @Override
    public boolean supports(PostEventType eventType) {
        return List.of(PostEventType.POST_CLOSED,
                       PostEventType.POST_EXPIRED,
                       PostEventType.POST_FILLED).contains(eventType);
    }

    @Override
    public void handle(PostEvent event) {

        PostId postId = new PostId(event.postId().getId());
        try {
            Participation participation = loadParticipation.getParticipationByPostId(postId);
            Set<Long> memberIds = participation.getMemberIds().stream()
                    .map(memberId -> memberId.getId())
                    .collect(Collectors.toSet());
            List<String> allPhotoUrls = preferenceService.findAllPhotoUrls(memberIds);
            List<String> foodNames = loadPreferenceFood.loadFoodPreferences(allPhotoUrls);
            System.out.println(foodNames);
        }catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }
}
