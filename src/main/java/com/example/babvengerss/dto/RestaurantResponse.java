package com.example.babvengerss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RestaurantResponse {
    private String name;
    private String address;
    private double latitude;
    private double longitude;
}
