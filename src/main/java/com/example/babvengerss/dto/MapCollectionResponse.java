package com.example.babvengerss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class MapCollectionResponse {
    private Long id;
    private String name;
    private String nickname;
    private List<RestaurantResponse> restaurants;
    private Double averageRating;
    private int reviewCount;
}

