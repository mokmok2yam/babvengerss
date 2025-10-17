package com.example.babvengerss.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewResponse {
    private Long reviewId;
    private Long authorId;
    private String nickname;
    private String content;
    private int rating;
    private Long mapId;
    private String mapName;

}
