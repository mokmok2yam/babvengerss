// 파일: src/main/java/com/example/babvengerss/dto/CommentResponse.java
package com.example.babvengerss.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponse {
    private Long commentId;
    private Long userId;
    private String nickname;
    private String content;
}