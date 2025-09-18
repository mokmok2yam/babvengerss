package com.example.babvengerss.controller;

import com.example.babvengerss.domain.MapCollection;
import com.example.babvengerss.domain.Restaurant;
import com.example.babvengerss.domain.User;
import com.example.babvengerss.dto.*;
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
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/map-collections")
public class MapCollectionController {

    private final MapCollectionRepository mapCollectionRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    @Value("${kakao.api.key}")
    private String KAKAO_API_KEY;

    // 맛집 지도 등록
    @PostMapping
    @Transactional
    public ResponseEntity<String> createMap(@RequestBody MapCollectionRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + request.getUserId()));

        List<Restaurant> finalRestaurants = new ArrayList<>();
        for (RestaurantInfoRequest info : request.getRestaurantInfos()) {
            Restaurant restaurant = restaurantRepository.findByNameAndAddress(info.getName(), info.getAddress());
            if (restaurant == null) {
                restaurant = createNewRestaurant(info.getName(), info.getAddress(), user);
            }
            finalRestaurants.add(restaurant);
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
        return ResponseEntity.ok("맛집 지도 등록 완료!");
    }

    // 키워드로 맛집 지도 검색
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<MapCollectionResponse>> searchMapsByKeyword(@RequestParam String keyword) {
        List<MapCollection> results = mapCollectionRepository.findByNameContaining(keyword);
        List<MapCollectionResponse> response = results.stream().map(this::convertToResponseDto).toList();
        return ResponseEntity.ok(response);
    }

    // 사용자 ID로 생성한 지도 목록 조회
    @GetMapping("/user/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MapCollectionResponse>> getMapsByUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
        List<MapCollection> maps = mapCollectionRepository.findByUser(user);
        List<MapCollectionResponse> response = maps.stream().map(this::convertToResponseDto).toList();
        return ResponseEntity.ok(response);
    }

    // ID로 특정 맛집 지도 상세 조회
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<MapCollectionResponse> getMapById(@PathVariable Long id) {
        return mapCollectionRepository.findById(id)
                .map(this::convertToResponseDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ID로 특정 맛집 지도 수정 (이름 변경)
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<MapCollection> updateMapName(@PathVariable Long id, @RequestBody MapUpdateRequest request) {
        MapCollection map = mapCollectionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Map not found with id: " + id));
        map.setName(request.getName());
        MapCollection savedMap = mapCollectionRepository.save(map);
        return ResponseEntity.ok(savedMap);
    }

    // ID로 특정 맛집 지도 삭제
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteMap(@PathVariable Long id) {
        mapCollectionRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // 특정 지도에 맛집 추가
    @PostMapping("/{mapId}/restaurants")
    @Transactional
    public ResponseEntity<Restaurant> addRestaurantToMap(@PathVariable Long mapId, @RequestBody RestaurantInfoRequest request) {
        MapCollection map = mapCollectionRepository.findById(mapId)
                .orElseThrow(() -> new NoSuchElementException("Map not found with id: " + mapId));

        Restaurant restaurant = restaurantRepository.findByNameAndAddress(request.getName(), request.getAddress());
        if (restaurant == null) {
            restaurant = createNewRestaurant(request.getName(), request.getAddress(), map.getUser());
        }

        if (!map.getRestaurants().contains(restaurant)) {
            map.getRestaurants().add(restaurant);
            mapCollectionRepository.save(map);
        }

        return ResponseEntity.ok(restaurant);
    }

    // 특정 지도에서 맛집 삭제
    @DeleteMapping("/{mapId}/restaurants/{restaurantId}")
    @Transactional
    public ResponseEntity<Void> removeRestaurantFromMap(@PathVariable Long mapId, @PathVariable Long restaurantId) {
        MapCollection map = mapCollectionRepository.findById(mapId)
                .orElseThrow(() -> new NoSuchElementException("Map not found with id: " + mapId));

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new NoSuchElementException("Restaurant not found with id: " + restaurantId));

        map.getRestaurants().remove(restaurant);
        mapCollectionRepository.save(map);

        return ResponseEntity.ok().build();
    }

    // ========== Private Helper Methods ==========

    // Restaurant 엔티티를 MapCollectionResponse DTO로 변환
    private MapCollectionResponse convertToResponseDto(MapCollection map) {
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
                .nickname(map.getUser().getNickname())
                .restaurants(restaurantDtos)
                .build();
    }

    // 새로운 Restaurant 엔티티 생성 및 저장
    private Restaurant createNewRestaurant(String name, String address, User user) {
        WebClient client = WebClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + KAKAO_API_KEY)
                .build();

        String response = client.get()
                .uri(uriBuilder -> uriBuilder.path("/v2/local/search/address.json")
                        .queryParam("query", address)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JSONObject json = new JSONObject(response);
        JSONArray documents = json.getJSONArray("documents");

        if (documents.isEmpty()) {
            throw new NoSuchElementException("Address not found: " + address);
        }

        JSONObject location = documents.getJSONObject(0);
        double lat = location.getDouble("y");
        double lng = location.getDouble("x");

        Restaurant newRestaurant = new Restaurant();
        newRestaurant.setName(name);
        newRestaurant.setAddress(address);
        newRestaurant.setLatitude(lat);
        newRestaurant.setLongitude(lng);
        newRestaurant.setUser(user);

        return restaurantRepository.save(newRestaurant);
    }
}