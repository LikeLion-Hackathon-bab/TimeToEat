package com.example.timetoeat.domain.posting.core.application.preference;

import com.example.timetoeat.domain.article.repository.ArticlePhotoQueryRepository;
import com.example.timetoeat.domain.article.repository.projection.ArticlePhotoProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PreferenceService {

    private final ArticlePhotoQueryRepository photoRepo;
    private final Clock clock;

    public List<String> findAllPhotoUrls(Set<Long> participantIds) {
        LocalDateTime since = LocalDateTime.now(clock).minusDays(5);
        return participantIds.stream()
                .flatMap(memberId ->
                        photoRepo.findAllUserPhotosSince(memberId, since).stream()
                )
                .map(ArticlePhotoProjection::getImageUrl)
                .collect(Collectors.toList());
    }
}
