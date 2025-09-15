package com.example.babvengerss.controller;

import com.example.babvengerss.domain.Restaurant;
import com.example.babvengerss.domain.User;
import com.example.babvengerss.dto.RestaurantRequest;
import com.example.babvengerss.repository.RestaurantRepository;
import com.example.babvengerss.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/restaurants")
public class RestaurantController {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    @Value("${kakao.api.key}")
    private String KAKAO_API_KEY;
    @PostMapping
    public ResponseEntity<String> registerRestaurant(@RequestBody RestaurantRequest request) {
        try {
            // 1. 사용자 조회
            User user = userRepository.findById(request.getUserId()).orElseThrow();

            // 2. 사용자 기준 중복 맛집 검사
            if (restaurantRepository.existsByUserAndName(user, request.getName())) {
                return ResponseEntity.status(409).body("⚠️ 이미 등록된 맛집입니다.");
            }

            // 3. Kakao 주소 검색 API 호출 (자동 인코딩)
            WebClient client = WebClient.builder()
                    .baseUrl("https://dapi.kakao.com")
                    .defaultHeader(HttpHeaders.AUTHORIZATION, KAKAO_API_KEY)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8")
                    .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                    .build();

            String response = client.get()
                    .uri(uriBuilder -> uriBuilder.path("/v2/local/search/address.json")
                            .queryParam("query", request.getAddress()) // ❗ URLEncoder 제거됨
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JSONObject json = new JSONObject(response);
            JSONArray documents = json.getJSONArray("documents");

            if (documents.isEmpty()) {
                return ResponseEntity.badRequest().body("📭 주소를 찾을 수 없습니다. 더 정확하게 입력해 주세요!");
            }

            JSONObject location = documents.getJSONObject(0);
            double latitude = location.getDouble("y");
            double longitude = location.getDouble("x");

            // 4. Restaurant 객체 생성
            Restaurant restaurant = new Restaurant();
            restaurant.setName(request.getName());
            restaurant.setAddress(request.getAddress());
            restaurant.setLatitude(latitude);
            restaurant.setLongitude(longitude);
            restaurant.setUser(user);

            // 5. 저장
            restaurantRepository.save(restaurant);

            return ResponseEntity.ok("✅ 맛집 등록 성공!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("❌ 등록 실패: " + e.getMessage());
        }
    }

    // ✅ 사용자별 맛집 조회
    @GetMapping("/user/{userId}")
    public List<Restaurant> getRestaurantsByUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return restaurantRepository.findByUser(user);
    }
}
