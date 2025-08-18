package com.example.timetoeat.domain.article.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RestaurantSnapshot {

    private String placeId;
    private String placeName;
    private String addressName;
    private String roadAddressName;
    private String phoneNumber;
    private String placeUrl;

    private String categoryGroupCode;
    private String categoryGroupName;
    private String categoryName;

    private Double x;  // longitude (경도)
    private Double y;  // latitude (위도)
}
