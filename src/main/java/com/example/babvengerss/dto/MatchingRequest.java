package com.example.babvengerss.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchingRequest {
    private Long senderId;           // Post 작성자 (Host) ID

    // 맛집 정보 (ID 대신 이름과 주소를 받아서 백엔드에서 처리)
    private String name;             // 맛집 이름
    private String address;          // 맛집 주소

    private String meetingTime;      // 모임 시간
    private String title;            // 게시물 제목
}