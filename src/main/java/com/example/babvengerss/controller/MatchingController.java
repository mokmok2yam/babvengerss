package com.example.babvengerss.controller;

import com.example.babvengerss.domain.Restaurant;
import com.example.babvengerss.domain.Matching;
import com.example.babvengerss.domain.User;
import com.example.babvengerss.dto.MatchingRequest;
import com.example.babvengerss.dto.MatchingResponse;
import com.example.babvengerss.dto.MatchingStatusUpdateRequest;
import com.example.babvengerss.repository.RestaurantRepository;
import com.example.babvengerss.repository.MatchingRepository;
import com.example.babvengerss.repository.MatchingCommentRepository; // CommentRepository 주입
import com.example.babvengerss.repository.UserRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus; // HttpStatus 추가
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
    private final MatchingCommentRepository commentRepository; // CommentRepository 주입

    @Value("${kakao.api.key}")
    private String KAKAO_API_KEY;

    // [New Logic: 1. Assemble 게시글 등록 (Host가 작성)]
    @PostMapping
    @Transactional
    public ResponseEntity<String> createAssemblePost(@RequestBody MatchingRequest dto) {
        try {
            User host = userRepository.findById(dto.getSenderId()).orElseThrow(() -> new NoSuchElementException("Host not found"));
            Restaurant restaurant = restaurantRepository.findByNameAndAddress(dto.getName(), dto.getAddress());
            if (restaurant == null) {
                restaurant = createNewRestaurant(dto.getName(), dto.getAddress(), host);
            }
            Matching post = new Matching();
            post.setSender(host);
            post.setRestaurant(restaurant);
            post.setTitle(dto.getTitle());
            post.setMeetingTime(dto.getMeetingTime());
            post.setStatus("모집중");
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
        List<Matching> list = matchingRepository.findByReceiverIsNullOrderByIdDesc();
        return list.stream().map(this::convertToPostResponseDto).collect(Collectors.toList());
    }

    // [New Logic: 3. Assemble 게시글에 신청 (Applicant가 요청)]
    @PostMapping("/{postId}/apply/{applicantId}")
    @Transactional
    public ResponseEntity<String> applyToPost(@PathVariable Long postId, @PathVariable Long applicantId) {
        Matching post = matchingRepository.findById(postId).orElseThrow(() -> new NoSuchElementException("Assemble Post not found"));
        User applicant = userRepository.findById(applicantId).orElseThrow(() -> new NoSuchElementException("Applicant not found"));
        User host = post.getSender();
        if (host.getId().equals(applicantId)) {
            return ResponseEntity.badRequest().body("자신이 작성한 게시글에는 신청할 수 없습니다.");
        }
        boolean alreadyRequested = matchingRepository.existsBySenderAndReceiverAndTitleAndStatus(applicant, host, post.getTitle(), "요청됨");
        boolean alreadyAccepted = matchingRepository.existsBySenderAndReceiverAndTitleAndStatus(applicant, host, post.getTitle(), "수락됨");
        if (alreadyRequested || alreadyAccepted) {
            return ResponseEntity.status(409).body("이미 신청했거나 수락된 모임입니다.");
        }
        Matching application = new Matching();
        application.setSender(applicant);
        application.setReceiver(host);
        application.setRestaurant(post.getRestaurant());
        application.setTitle(post.getTitle());
        application.setMeetingTime(post.getMeetingTime());
        application.setStatus("요청됨");
        matchingRepository.save(application);
        return ResponseEntity.ok("Assemble 신청 완료!");
    }

    // [New Logic: 8. 어셈블 게시글 삭제 (호스트만 가능)]
    @DeleteMapping("/{postId}/{userId}")
    @Transactional
    public ResponseEntity<String> deleteAssemblePost(@PathVariable Long postId, @PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
        Matching post = matchingRepository.findById(postId).orElseThrow(() -> new NoSuchElementException("Assemble Post not found"));

        if (post.getReceiver() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("신청 내역이 아닌 게시글만 삭제할 수 있습니다.");
        }
        if (!post.getSender().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("게시글 삭제 권한이 없습니다.");
        }
        try {
            commentRepository.deleteByMatching(post); // 관련 댓글 삭제
            List<Matching> relatedApplications = matchingRepository.findByReceiverAndTitle(post.getSender(), post.getTitle()); // 관련 신청 내역 조회
            matchingRepository.deleteAll(relatedApplications); // 관련 신청 내역 삭제
            matchingRepository.delete(post); // 게시글 삭제
            return ResponseEntity.ok("게시글 및 관련 데이터가 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("게시글 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // [New Logic: 9. 어셈블 신청 내역 삭제 (호스트 또는 신청자, 수락됨 제외)]
    @DeleteMapping("/request/{requestId}/{userId}")
    @Transactional
    public ResponseEntity<String> deleteAssembleRequest(@PathVariable Long requestId, @PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
        Matching request = matchingRepository.findById(requestId).orElseThrow(() -> new NoSuchElementException("Assemble Request not found"));

        if (request.getReceiver() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("게시글이 아닌 신청 내역만 삭제할 수 있습니다.");
        }
        boolean isSender = request.getSender().getId().equals(userId);
        boolean isReceiver = request.getReceiver().getId().equals(userId);
        if (!isSender && !isReceiver) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("신청 내역 삭제 권한이 없습니다.");
        }
        if ("수락됨".equals(request.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("수락된 신청은 삭제할 수 없습니다.");
        }
        try {
            matchingRepository.delete(request);
            return ResponseEntity.ok("신청 내역이 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("신청 내역 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // [New Logic: 4. Host가 받은 신청 목록 조회]
    @GetMapping("/requests/received/{userId}")
    @Transactional(readOnly = true)
    public List<MatchingResponse> getReceivedRequests(@PathVariable Long userId) {
        User host = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
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
                .filter(m -> m.getReceiver() == null)
                .map(this::convertToPostResponseDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // [Modified: 6. 상태 업데이트 (Host가 신청 수락/거절/모집마감)]
    @PatchMapping("/update-status")
    @Transactional
    public ResponseEntity<String> updateMatchingStatus(@RequestBody MatchingStatusUpdateRequest request) {
        try {
            Matching matching = matchingRepository.findById(request.getMatchingId())
                    .orElseThrow(() -> new RuntimeException("매칭 정보가 없습니다."));
            matching.setStatus(request.getStatus());
            matchingRepository.save(matching);
            return ResponseEntity.ok("매칭 상태가 업데이트되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("상태 업데이트 실패: " + e.getMessage());
        }
    }

    // DTO 변환 헬퍼 메서드 (게시글용)
    private MatchingResponse convertToPostResponseDto(Matching post) {
        MatchingResponse dto = new MatchingResponse();
        dto.setId(post.getId());
        dto.setSenderName(post.getSender().getNickname());
        dto.setTitle(post.getTitle());
        dto.setMeetingTime(post.getMeetingTime());
        dto.setStatus(post.getStatus());
        if (post.getRestaurant() != null) {
            dto.setRestaurantName(post.getRestaurant().getName());
            dto.setRestaurantId(post.getRestaurant().getId());
            dto.setLatitude(post.getRestaurant().getLatitude());
            dto.setLongitude(post.getRestaurant().getLongitude());
        }
        return dto;
    }

    // DTO 변환 헬퍼 메서드 (신청/요청용)
    private MatchingResponse convertToRequestResponseDto(Matching request) {
        MatchingResponse dto = new MatchingResponse();
        dto.setId(request.getId());
        dto.setSenderName(request.getSender().getNickname());
        dto.setReceiverName(request.getReceiver().getNickname());
        dto.setTitle(request.getTitle());
        dto.setMeetingTime(request.getMeetingTime());
        dto.setStatus(request.getStatus());
        if (request.getRestaurant() != null) {
            dto.setRestaurantName(request.getRestaurant().getName());
            dto.setRestaurantId(request.getRestaurant().getId());
            dto.setLatitude(request.getRestaurant().getLatitude());
            dto.setLongitude(request.getRestaurant().getLongitude());
        }
        return dto;
    }

    // 맛집 생성 로직
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