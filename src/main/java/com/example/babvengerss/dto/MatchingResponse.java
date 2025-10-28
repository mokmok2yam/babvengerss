package com.example.babvengerss.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchingResponse {
    private Long id;
    private String senderName;
    private String receiverName;
    private String restaurantName; // mapName -> restaurantName
    private String status;
    private Long restaurantId; // 추가
    private String meetingTime; // 추가
    private String title; // 추가
    private double latitude;   // 👈 추가: 맛집 위도
    private double longitude;  // 👈 추가: 맛집 경도
}