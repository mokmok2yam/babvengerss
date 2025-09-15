package com.example.babvengerss.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewResponse {
    private String username;
    private String content;
    private int rating;
}
