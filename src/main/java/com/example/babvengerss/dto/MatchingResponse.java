package com.example.babvengerss.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchingResponse {
    private Long id;
    private String senderName;
    private String receiverName;
    private String mapName;
    private String status;
}
