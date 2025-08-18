package com.example.timetoeat.domain.article.dto.request;

import com.example.timetoeat.domain.article.entity.RestaurantSnapshot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestaurantRequest {

    @NotBlank private String placeId;
    @NotBlank private String placeName;
    private String addressName;
    private String roadAddressName;
    private String phoneNumber;
    @Size(max = 1024) private String placeUrl;
    private String categoryGroupCode;
    private String categoryGroupName;
    private String categoryName;
    private Double x;
    private Double y;

    @Builder
    private RestaurantRequest(String placeId, String placeName, String addressName, String roadAddressName,
                              String phoneNumber, String placeUrl, String categoryGroupCode, String categoryGroupName,
                              String categoryName, Double x, Double y) {
        this.placeId = placeId;
        this.placeName = placeName;
        this.addressName = addressName;
        this.roadAddressName = roadAddressName;
        this.phoneNumber = phoneNumber;
        this.placeUrl = placeUrl;
        this.categoryGroupCode = categoryGroupCode;
        this.categoryGroupName = categoryGroupName;
        this.categoryName = categoryName;
        this.x = x;
        this.y = y;
    }

    public RestaurantSnapshot toSnapshot() {
        return RestaurantSnapshot.builder()
                .placeId(placeId)
                .placeName(placeName)
                .addressName(addressName)
                .roadAddressName(roadAddressName)
                .phoneNumber(phoneNumber)
                .placeUrl(placeUrl)
                .categoryGroupCode(categoryGroupCode)
                .categoryGroupName(categoryGroupName)
                .categoryName(categoryName)
                .x(x)
                .y(y)
                .build();
    }
}