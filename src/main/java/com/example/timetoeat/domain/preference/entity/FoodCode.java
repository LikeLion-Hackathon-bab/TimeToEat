package com.example.timetoeat.domain.preference.entity;

import com.example.timetoeat.global.util.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "food_code")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FoodCode extends BaseTimeEntity {

    @Id
    @Column(length = 32)
    private String code;  // ex) "03021005"

    @Column(nullable = false, length = 100)
    private String label;  // ex) "불고기"

    public FoodCode(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
