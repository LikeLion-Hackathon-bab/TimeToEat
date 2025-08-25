package com.example.timetoeat.domain.plans.repository;

import com.example.timetoeat.domain.plans.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan,Long> {
}
