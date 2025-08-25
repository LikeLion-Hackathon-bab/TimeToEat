package com.example.timetoeat.domain.plans.service;

import com.example.timetoeat.domain.plans.dto.PlanDtos;
import com.example.timetoeat.domain.plans.entity.Plan;
import com.example.timetoeat.domain.plans.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PlanService {
    private final PlanRepository planRepository;

    @Transactional
    public PlanDtos.Response create(PlanDtos.CreateRequest req) {
        Plan plan = Plan.builder()
                .meetingDate(req.meetingDate())
                .meetingTime(req.meetingTime())
                .location(req.location())
                .authorName(req.author().name())
                .build();
        Plan saved = planRepository.save(plan);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PlanDtos.Response> findAll() {
        return planRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private PlanDtos.Response toResponse(Plan p) {
        return new PlanDtos.Response(
                p.getId(),
                p.getMeetingDate(),
                p.getMeetingTime(),
                p.getLocation(),
                p.getAuthorName()
        );
    }
}
