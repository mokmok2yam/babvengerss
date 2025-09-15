package com.example.babvengerss.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchingRequest {
    private Long senderId;           // 매칭 요청을 보낸 사용자 ID
    private Long receiverId;         // 매칭 요청을 받을 사용자 ID
    private Long mapCollectionId;    // 매칭 대상이 되는 맛집지도 ID
}
