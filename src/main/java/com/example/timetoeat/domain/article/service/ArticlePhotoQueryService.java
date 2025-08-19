package com.example.timetoeat.domain.article.service;

import com.example.timetoeat.domain.article.dto.response.ArticlePhotoResponse;
import com.example.timetoeat.domain.article.repository.ArticlePhotoQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticlePhotoQueryService {

    private final ArticlePhotoQueryRepository photoRepo;
    private final Clock clock;

    // 작성자가 올린 사진만 (최근 N일 필터 옵션)
    public Page<ArticlePhotoResponse> findAuthorPhotos(Long memberId, Integer days, int page, int size) {
        var sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        var pageable = PageRequest.of(page, size, sort);
        LocalDateTime since = (days == null) ? null : LocalDateTime.now(clock).minusDays(days);

        var pageProj = photoRepo.findAuthorPhotos(memberId, since, pageable);
        var list = pageProj.getContent().stream()
                .map(ArticlePhotoResponse::from)
                .collect(Collectors.toList());

        return new PageImpl<>(list, pageable, pageProj.getTotalElements());
    }

    // 해당 사용자가 태그된 사진들
    public Page<ArticlePhotoResponse> findTaggedPhotos(Long memberId, Integer days, int page, int size) {
        var sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        var pageable = PageRequest.of(page, size, sort);
        LocalDateTime since = (days == null) ? null : LocalDateTime.now(clock).minusDays(days);

        var pageProj = photoRepo.findTaggedPhotos(memberId, since, pageable);
        var list = pageProj.getContent().stream()
                .map(ArticlePhotoResponse::from)
                .collect(Collectors.toList());

        return new PageImpl<>(list, pageable, pageProj.getTotalElements());
    }
}
