package com.example.babvengerss.repository;

import com.example.babvengerss.domain.Restaurant;
import com.example.babvengerss.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    boolean existsByUserAndName(User user, String name);
    List<Restaurant> findByUser(User user);  // 사용자별 맛집 리스트
    boolean existsByUserAndNameAndAddress(User user, String name, String address);
    Restaurant findByNameAndAddress(String name, String address);
}
