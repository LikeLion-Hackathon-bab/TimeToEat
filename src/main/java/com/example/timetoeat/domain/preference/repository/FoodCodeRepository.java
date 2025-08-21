package com.example.timetoeat.domain.preference.repository;

import com.example.timetoeat.domain.preference.entity.FoodCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodCodeRepository extends JpaRepository<FoodCode, String> {
}
