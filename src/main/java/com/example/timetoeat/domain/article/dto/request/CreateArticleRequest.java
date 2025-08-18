package com.example.timetoeat.domain.article.dto.request;

import com.example.timetoeat.domain.article.entity.Article;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreateArticleRequest {

    public enum Method { CAMERA, ALBUM }  // <카메라로 직접 찍어서 업로드 or 앨범에서 업로드>

    @NotBlank
    @Size(max = 1024)
    private String imageUrl;

    // 선택값: CAMERA면 null 허용(서버에서 now로 대체) or ALBUM이면 반드시 값 필요
    private LocalDate mealDate;
    private LocalTime mealTime;

    @Valid
    private RestaurantRequest restaurant;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<@NotNull Long> taggedMemberIds = new ArrayList<>();

    @NotNull
    private Method method;

    @Builder
    private CreateArticleRequest(String imageUrl, LocalDate mealDate, LocalTime mealTime,
                                 RestaurantRequest restaurant, List<Long> taggedMemberIds, Method method) {

        this.imageUrl = imageUrl;
        this.mealDate = mealDate;
        this.mealTime = mealTime;
        this.restaurant = restaurant;
        this.taggedMemberIds = (taggedMemberIds != null) ? taggedMemberIds : new ArrayList<>();
        this.method = method;
    }

    @AssertTrue(message = "앨범에서 업로드할 경우, 날짜(mealDate) 와 시간(mealTime)은 반드시 입력해야 합니다.")
    public boolean isMealDateTimeValid() {
        if (method == null) {
            return false;
        }

        if (method == Method.ALBUM) {
            return (mealDate != null) && (mealTime != null);
        }

        return true;  // CAMERA면 null 허용
    }

    public Article toEntity(MemberEntity author, LocalDate resolvedDate, LocalTime resolvedTime) {
        return Article.builder()
                .author(author)
                .imageUrl(imageUrl)
                .mealDate(resolvedDate)
                .mealTime(resolvedTime)
                .restaurant(restaurant != null ? restaurant.toSnapshot() : null)
                .build();
    }

    public boolean isCamera() { return method == Method.CAMERA; }
    public boolean isAlbum() { return method == Method.ALBUM; }
}
