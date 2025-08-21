package com.example.timetoeat.domain.article.exception;

import com.example.timetoeat.global.error.BaseError;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum ArticleErrorCode implements BaseError {

    ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    INVALID_PLACE(HttpStatus.BAD_REQUEST, "장소 정보가 올바르지 않습니다."),
    NOT_OWNED_IMAGE(HttpStatus.FORBIDDEN, "작성자의 이미지가 아닙니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    PARENT_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부모 댓글을 찾을 수 없습니다."),
    PARENT_COMMENT_NOT_IN_ARTICLE(HttpStatus.FORBIDDEN, "부모 댓글이 해당 게시글에 속하지 않습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {

        return name();
    }

    @Override
    public HttpStatus getHttpStatus() {

        return status;
    }

    @Override
    public String getMessage() {

        return message;
    }
}
