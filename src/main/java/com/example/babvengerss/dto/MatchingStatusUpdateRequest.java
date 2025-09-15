package com.example.babvengerss.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchingStatusUpdateRequest {
    private Long matchingId;
    private String status;
}
