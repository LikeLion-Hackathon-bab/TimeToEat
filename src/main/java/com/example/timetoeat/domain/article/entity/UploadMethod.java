package com.example.timetoeat.domain.article.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

/** 게시물 사진을 올린 방법: 카메라로 직접 찍음 / 앨범에서 고름. */
public enum UploadMethod {
    CAMERA, ALBUM;

    @JsonCreator
    public static UploadMethod from(Object v) {
        if (v == null) return null;
        return UploadMethod.valueOf(v.toString().trim().toUpperCase(Locale.ROOT));
    }
}
