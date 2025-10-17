package com.example.babvengerss.controller;

import com.example.babvengerss.domain.MapCollection;
import com.example.babvengerss.domain.Review;
import com.example.babvengerss.domain.User;
import com.example.babvengerss.dto.ReviewRequest;
import com.example.babvengerss.dto.ReviewResponse;
import com.example.babvengerss.repository.MapCollectionRepository;
import com.example.babvengerss.repository.ReviewRepository;
import com.example.babvengerss.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/map-reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final MapCollectionRepository mapCollectionRepository;
    private final UserRepository userRepository;

    // 리뷰 등록
    @PostMapping
    @Transactional
    public ResponseEntity<String> createReview(@RequestBody ReviewRequest request) {
        User user = userRepository.findById(request.getUserId()).orElseThrow();
        MapCollection map = mapCollectionRepository.findById(request.getMapCollectionId()).orElseThrow();

        Review review = new Review();
        review.setUser(user);
        review.setMapCollection(map);
        review.setContent(request.getContent());
        review.setRating(request.getRating());

        reviewRepository.save(review);
        return ResponseEntity.ok("✅ 리뷰 등록 완료!");
    }

    // 리뷰 수정 (보안 강화: 본인 리뷰만 수정 가능하도록)
    @PutMapping("/{reviewId}")
    @Transactional
    public ResponseEntity<String> updateReview(@PathVariable Long reviewId, @RequestBody ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId).orElseThrow();
        User user = userRepository.findById(request.getUserId()).orElseThrow();

        if (!review.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("수정 권한이 없습니다.");
        }

        review.setContent(request.getContent());
        review.setRating(request.getRating());
        reviewRepository.save(review);
        return ResponseEntity.ok("✏️ 리뷰 수정 완료!");
    }

    // 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    @Transactional
    public ResponseEntity<String> deleteReview(@PathVariable Long reviewId) {
        reviewRepository.deleteById(reviewId);
        return ResponseEntity.ok("🗑️ 리뷰 삭제 완료!");
    }

    // 지도별 리뷰 조회
    @GetMapping("/map/{mapCollectionId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReviewResponse>> getReviewsByMap(@PathVariable Long mapCollectionId) {
        MapCollection map = mapCollectionRepository.findById(mapCollectionId).orElseThrow();
        List<Review> reviews = reviewRepository.findByMapCollection(map);
        List<ReviewResponse> response = reviews.stream().map(this::convertToDto).toList();
        return ResponseEntity.ok(response);
    }

    // 사용자별 리뷰 조회
    @GetMapping("/user/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReviewResponse>> getReviewsByUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        List<Review> reviews = reviewRepository.findByUser(user);
        List<ReviewResponse> response = reviews.stream().map(this::convertToDto).toList();
        return ResponseEntity.ok(response);
    }

    // DTO 변환을 위한 private 헬퍼 메서드
    private ReviewResponse convertToDto(Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getId())
                .authorId(review.getUser().getId())
                .nickname(review.getUser().getNickname())
                .content(review.getContent())
                .rating(review.getRating())
                .mapId(review.getMapCollection().getId())
                .mapName(review.getMapCollection().getName())
                .build();
    }
}