package com.example.babvengerss.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestaurantRequest {
    private String name;
    private String address;
    private Long userId;
}
