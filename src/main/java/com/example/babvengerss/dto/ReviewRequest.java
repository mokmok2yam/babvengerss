package com.example.babvengerss.dto;

import lombok.Getter;

@Getter
public class ReviewRequest {
    private Long userId;
    private Long mapCollectionId;
    private String content;
    private int rating;
}
