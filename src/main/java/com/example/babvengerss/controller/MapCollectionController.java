//매칭 시스템 구현
package com.example.babvengerss.controller;

import com.example.babvengerss.domain.MapCollection;
import com.example.babvengerss.domain.Restaurant;
import com.example.babvengerss.domain.User;
import com.example.babvengerss.dto.MapCollectionRequest;
import com.example.babvengerss.dto.MapCollectionResponse;
import com.example.babvengerss.dto.RestaurantInfoRequest;
import com.example.babvengerss.dto.RestaurantResponse;
import com.example.babvengerss.repository.MapCollectionRepository;
import com.example.babvengerss.repository.RestaurantRepository;
import com.example.babvengerss.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/map-collections")
public class MapCollectionController {

    private final MapCollectionRepository mapCollectionRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    @Value("${kakao.api.key}")
    private String KAKAO_API_KEY;
    // ✅ 1. 맛집 지도 등록
    @PostMapping
    public ResponseEntity<String> createMap(@RequestBody MapCollectionRequest request) {
        try {
            User user = userRepository.findById(request.getUserId()).orElseThrow();
            List<Restaurant> finalRestaurants = new ArrayList<>();

            for (RestaurantInfoRequest info : request.getRestaurantInfos()) {
                Restaurant existing = restaurantRepository.findByNameAndAddress(info.getName(), info.getAddress());
                if (existing != null) {
                    finalRestaurants.add(existing);
                    continue;
                }

                WebClient client = WebClient.builder()
                        .baseUrl("https://dapi.kakao.com")
                        .defaultHeader(HttpHeaders.AUTHORIZATION, KAKAO_API_KEY)
                        .build();

                String response = client.get()
                        .uri(uriBuilder -> uriBuilder.path("/v2/local/search/address.json")
                                .queryParam("query", info.getAddress())
                                .build())
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                JSONObject json = new JSONObject(response);
                JSONArray documents = json.getJSONArray("documents");

                if (documents.isEmpty()) continue;

                JSONObject location = documents.getJSONObject(0);
                double lat = location.getDouble("y");
                double lng = location.getDouble("x");

                Restaurant newRestaurant = new Restaurant();
                newRestaurant.setName(info.getName());
                newRestaurant.setAddress(info.getAddress());
                newRestaurant.setLatitude(lat);
                newRestaurant.setLongitude(lng);
                newRestaurant.setUser(user);

                restaurantRepository.save(newRestaurant);
                finalRestaurants.add(newRestaurant);
            }

            MapCollection map = new MapCollection();
            map.setName(request.getName());
            map.setUser(user);
            map.setRestaurants(finalRestaurants);

            if (!finalRestaurants.isEmpty()) {
                map.setLatitude(finalRestaurants.get(0).getLatitude());
                map.setLongitude(finalRestaurants.get(0).getLongitude());
            }

            mapCollectionRepository.save(map);
            return ResponseEntity.ok("✅ 맛집 지도 등록 완료!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("❌ 등록 실패: " + e.getMessage());
        }
    }

    // ✅ 2. 키워드로 맛집 지도 검색
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<MapCollectionResponse>> searchMapsByKeyword(@RequestParam String keyword) {
        List<MapCollection> results = mapCollectionRepository.findByNameContaining(keyword);

        List<MapCollectionResponse> response = results.stream().map(map -> {
            List<RestaurantResponse> restaurantDtos = map.getRestaurants().stream().map(r ->
                    RestaurantResponse.builder()
                            .name(r.getName())
                            .address(r.getAddress())
                            .latitude(r.getLatitude())
                            .longitude(r.getLongitude())
                            .build()
            ).toList();

            return MapCollectionResponse.builder()
                    .id(map.getId())
                    .name(map.getName())
                    .username(map.getUser().getUsername())
                    .restaurants(restaurantDtos)
                    .build();
        }).toList();

        return ResponseEntity.ok(response);
    }

    // ✅ 3. 사용자 ID로 내가 만든 지도 목록 조회
    @GetMapping("/user/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MapCollectionResponse>> getMapsByUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        List<MapCollection> maps = mapCollectionRepository.findByUser(user);

        List<MapCollectionResponse> response = maps.stream().map(map -> {
            List<RestaurantResponse> restaurantDtos = map.getRestaurants().stream().map(r ->
                    RestaurantResponse.builder()
                            .name(r.getName())
                            .address(r.getAddress())
                            .latitude(r.getLatitude())
                            .longitude(r.getLongitude())
                            .build()
            ).toList();

            return MapCollectionResponse.builder()
                    .id(map.getId())
                    .name(map.getName())
                    .username(map.getUser().getUsername())
                    .restaurants(restaurantDtos)
                    .build();
        }).toList();

        return ResponseEntity.ok(response);
    }
}
