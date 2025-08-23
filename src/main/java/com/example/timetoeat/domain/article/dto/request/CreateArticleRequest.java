package com.example.timetoeat.domain.article.dto.request;

import com.example.timetoeat.domain.article.entity.Article;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.Valid;
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
import java.util.Locale;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreateArticleRequest {

    // <카메라로 직접 찍어서 업로드 or 앨범에서 업로드>
    public enum Method {
        CAMERA, ALBUM;

        @JsonCreator
        public static Method from(Object v) {
            if (v == null) return null;
            String s = v.toString().trim().toUpperCase(Locale.ROOT);
            return Method.valueOf(s);
        }
    }

    @NotBlank
    @Size(max = 1024)
    private String imageUrl;

    // mealDate는 이제 클라이언트 입력을 사용X -> 업로드 시각(now KST)의 LocalDate로 강제 설정
    private LocalDate mealDate;
    // 프론트에서 선택한 시간(예: 아침 -> "09:00:00")을 HH:mm:ss로 전달
    @NotNull(message = "시간(mealTime)은 반드시 입력해야 합니다.")
    @JsonFormat(pattern = "HH:mm:ss")
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
        this.mealDate = mealDate;  // 요청값이 와도 서비스에서 무시됨
        this.mealTime = mealTime;  // 필수
        this.restaurant = restaurant;
        this.taggedMemberIds = (taggedMemberIds != null) ? taggedMemberIds : new ArrayList<>();
        this.method = method;
    }

    public Article toEntity(MemberEntity author, LocalDate resolvedDate, LocalTime resolvedTime) {
        return Article.builder()
                .author(author)
                .imageUrl(imageUrl)
                .mealDate(resolvedDate)  // 서비스에서 now KST로 결정
                .mealTime(resolvedTime)  // 프론트 전달 시간
                .restaurant(restaurant != null ? restaurant.toSnapshot() : null)
                .build();
    }

    public boolean isCamera() { return method == Method.CAMERA; }
    public boolean isAlbum() { return method == Method.ALBUM; }
}
