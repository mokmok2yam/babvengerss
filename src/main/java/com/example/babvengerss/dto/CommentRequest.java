// 파일: src/main/java/com/example/babvengerss/dto/CommentRequest.java
package com.example.babvengerss.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentRequest {
    private Long userId;
    private Long matchingId;
    private String content;
}