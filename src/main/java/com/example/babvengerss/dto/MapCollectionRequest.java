package com.example.babvengerss.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class MapCollectionRequest {
    private String name;
    private Long userId;
    private List<RestaurantInfoRequest> restaurantInfos;
}
