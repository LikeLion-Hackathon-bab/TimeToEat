package com.example.timetoeat.domain.plans.controller;

import com.example.timetoeat.domain.plans.dto.PlanDtos;
import com.example.timetoeat.domain.plans.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
@Tag(name = "plans-controller")
public class PlansController {
    private final PlanService planService;

    @PostMapping
    @Operation(summary = "약속 생성", description = "웹소켓 쪽에서 호출해 약속을 DB에 저장합니다.")
    public ResponseEntity<PlanDtos.Response> create(@Valid @RequestBody PlanDtos.CreateRequest req) {
        return ResponseEntity.ok(planService.create(req));
    }

    @GetMapping
    @Operation(summary = "약속 전체 조회", description = "등록된 모든 약속을 반환합니다.")
    public ResponseEntity<List<PlanDtos.Response>> findAll() {
        return ResponseEntity.ok(planService.findAll());
    }
}
