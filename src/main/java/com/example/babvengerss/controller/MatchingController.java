package com.example.babvengerss.controller;

import com.example.babvengerss.domain.Restaurant;
import com.example.babvengerss.domain.Matching;
import com.example.babvengerss.domain.User;
import com.example.babvengerss.dto.MatchingRequest;
import com.example.babvengerss.dto.MatchingResponse;
import com.example.babvengerss.dto.MatchingStatusUpdateRequest;
import com.example.babvengerss.repository.RestaurantRepository;
import com.example.babvengerss.repository.MatchingRepository;
import com.example.babvengerss.repository.UserRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/matching")
public class MatchingController {

    private final MatchingRepository matchingRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;

    @Value("${kakao.api.key}")
    private String KAKAO_API_KEY;

    // [New Logic: 1. Assemble 게시글 등록 (Host가 작성)]
    @PostMapping
    @Transactional
    public ResponseEntity<String> createAssemblePost(@RequestBody MatchingRequest dto) {
        try {
            User host = userRepository.findById(dto.getSenderId()).orElseThrow(() -> new NoSuchElementException("Host not found"));

            // 1. 맛집 찾기 또는 새로 등록
            Restaurant restaurant = restaurantRepository.findByNameAndAddress(dto.getName(), dto.getAddress());
            if (restaurant == null) {
                restaurant = createNewRestaurant(dto.getName(), dto.getAddress(), host);
            }

            // 2. 게시글 생성
            Matching post = new Matching();
            post.setSender(host); // Sender는 Host (게시글 작성자)
            post.setRestaurant(restaurant);
            post.setTitle(dto.getTitle());
            post.setMeetingTime(dto.getMeetingTime());
            post.setStatus("모집중"); // 게시글 상태 (receiver는 null)

            matchingRepository.save(post);
            return ResponseEntity.ok("Assemble 게시글 등록 완료!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Assemble 게시글 등록 실패: " + e.getMessage());
        }
    }

    // [New Logic: 2. Assemble 게시판 전체 조회]
    @GetMapping("/board")
    @Transactional(readOnly = true)
    public List<MatchingResponse> getAssembleBoard() {
        // receiver가 null인, 즉 신청자가 없는 게시글(오리지널 게시물)만 조회
        List<Matching> list = matchingRepository.findByReceiverIsNullOrderByIdDesc();

        return list.stream().map(this::convertToPostResponseDto).collect(Collectors.toList());
    }

    // [New Logic: 3. Assemble 게시글에 신청 (Applicant가 요청)]
    @PostMapping("/{postId}/apply/{applicantId}")
    @Transactional
    public ResponseEntity<String> applyToPost(@PathVariable Long postId, @PathVariable Long applicantId) {
        Matching post = matchingRepository.findById(postId).orElseThrow(() -> new NoSuchElementException("Assemble Post not found"));
        User applicant = userRepository.findById(applicantId).orElseThrow(() -> new NoSuchElementException("Applicant not found"));

        if (post.getSender().getId().equals(applicantId)) {
            return ResponseEntity.badRequest().body("자신이 작성한 게시글에는 신청할 수 없습니다.");
        }

        // 신청 정보를 저장할 새로운 Matching 엔티티 생성 (1:1 매칭 요청)
        Matching application = new Matching();
        application.setSender(applicant); // Sender는 신청자
        application.setReceiver(post.getSender()); // Receiver는 호스트
        application.setRestaurant(post.getRestaurant()); // 게시글의 맛집 정보 복사
        application.setTitle(post.getTitle()); // 게시글 제목 복사
        application.setMeetingTime(post.getMeetingTime()); // 모임 시간 복사
        application.setStatus("요청됨");

        matchingRepository.save(application);
        return ResponseEntity.ok("Assemble 신청 완료!");
    }

    // [New Logic: 4. Host가 받은 신청 목록 조회]
    @GetMapping("/requests/received/{userId}")
    @Transactional(readOnly = true)
    public List<MatchingResponse> getReceivedRequests(@PathVariable Long userId) {
        User host = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));

        // Host에게 온 신청들 (Receiver가 Host이고 Sender가 Applicant인 경우)
        // receiver가 null이 아닌(신청) 요청들만 필터링합니다.
        List<Matching> list = matchingRepository.findByReceiver(host)
                .stream()
                .filter(m -> m.getRestaurant() != null)
                .collect(Collectors.toList());

        return list.stream().map(this::convertToRequestResponseDto).collect(Collectors.toList());
    }

    // [New Logic: 5. Applicant가 보낸 신청 목록 조회]
    @GetMapping("/requests/sent/{userId}")
    @Transactional(readOnly = true)
    public List<MatchingResponse> getSentRequests(@PathVariable Long userId) {
        User applicant = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));

        // Applicant가 보낸 신청들 (Sender가 Applicant이고 Receiver가 Host인 경우)
        List<Matching> list = matchingRepository.findBySender(applicant)
                .stream()
                .filter(m -> m.getReceiver() != null && m.getRestaurant() != null)
                .collect(Collectors.toList());

        return list.stream().map(this::convertToRequestResponseDto).collect(Collectors.toList());
    }

    // [New Logic: 7. ID로 단일 Assemble 게시글 조회]
    @GetMapping("/{postId}")
    @Transactional(readOnly = true)
    public ResponseEntity<MatchingResponse> getAssemblePostById(@PathVariable Long postId) {
        return matchingRepository.findById(postId)
                .filter(m -> m.getReceiver() == null) // 신청이 아닌 원본 게시글만 조회
                .map(this::convertToPostResponseDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // [Modified: 6. 상태 업데이트 (Host가 신청 수락/거절)]
    @PatchMapping("/update-status")
    @Transactional
    public ResponseEntity<String> updateMatchingStatus(@RequestBody MatchingStatusUpdateRequest request) {
        try {
            Matching matching = matchingRepository.findById(request.getMatchingId())
                    .orElseThrow(() -> new RuntimeException("매칭 신청 정보가 없습니다."));

            matching.setStatus(request.getStatus());
            matchingRepository.save(matching);

            return ResponseEntity.ok("매칭 상태가 업데이트되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("상태 업데이트 실패: " + e.getMessage());
        }
    }

    // DTO 변환 헬퍼 메서드 (게시글용) - Lat/Lng 포함
    private MatchingResponse convertToPostResponseDto(Matching post) {
        MatchingResponse dto = new MatchingResponse();
        dto.setId(post.getId());
        dto.setSenderName(post.getSender().getNickname()); // Host Nickname
        dto.setTitle(post.getTitle());
        dto.setMeetingTime(post.getMeetingTime());
        dto.setStatus(post.getStatus());
        if (post.getRestaurant() != null) {
            dto.setRestaurantName(post.getRestaurant().getName());
            dto.setRestaurantId(post.getRestaurant().getId());
            // Lat/Lng 추가
            dto.setLatitude(post.getRestaurant().getLatitude());
            dto.setLongitude(post.getRestaurant().getLongitude());
        }
        // receiverName (신청자)은 게시글이므로 null
        return dto;
    }

    // DTO 변환 헬퍼 메서드 (신청/요청용) - Lat/Lng 포함
    private MatchingResponse convertToRequestResponseDto(Matching request) {
        MatchingResponse dto = new MatchingResponse();
        dto.setId(request.getId());
        dto.setSenderName(request.getSender().getNickname()); // Applicant Nickname
        dto.setReceiverName(request.getReceiver().getNickname()); // Host Nickname
        dto.setTitle(request.getTitle());
        dto.setMeetingTime(request.getMeetingTime());
        dto.setStatus(request.getStatus());
        if (request.getRestaurant() != null) {
            dto.setRestaurantName(request.getRestaurant().getName());
            dto.setRestaurantId(request.getRestaurant().getId());
            // Lat/Lng 추가
            dto.setLatitude(request.getRestaurant().getLatitude());
            dto.setLongitude(request.getRestaurant().getLongitude());
        }
        return dto;
    }

    // 맛집 생성 로직 (MapCollectionController에서 가져옴)
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
            // 주소를 못 찾으면, 그냥 이름/주소만 저장하고 위도/경도는 0으로 설정
            Restaurant fallbackRestaurant = new Restaurant();
            fallbackRestaurant.setName(name);
            fallbackRestaurant.setAddress(address);
            fallbackRestaurant.setLatitude(0.0);
            fallbackRestaurant.setLongitude(0.0);
            fallbackRestaurant.setUser(user);
            return restaurantRepository.save(fallbackRestaurant);
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