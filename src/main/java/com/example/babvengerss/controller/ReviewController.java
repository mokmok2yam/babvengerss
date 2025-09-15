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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/map-reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final MapCollectionRepository mapCollectionRepository;
    private final UserRepository userRepository;

    // ✅ 리뷰 등록
    @PostMapping
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

    // ✅ 리뷰 수정
    @PutMapping("/{reviewId}")
    public ResponseEntity<String> updateReview(@PathVariable Long reviewId,
                                               @RequestBody ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId).orElseThrow();

        review.setContent(request.getContent());
        review.setRating(request.getRating());
        reviewRepository.save(review);

        return ResponseEntity.ok("✏️ 리뷰 수정 완료!");
    }

    // ✅ 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<String> deleteReview(@PathVariable Long reviewId) {
        reviewRepository.deleteById(reviewId);
        return ResponseEntity.ok("🗑️ 리뷰 삭제 완료!");
    }

    // ✅ 리뷰 조회
    @GetMapping("/map/{mapCollectionId}")
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable Long mapCollectionId) {
        MapCollection map = mapCollectionRepository.findById(mapCollectionId).orElseThrow();
        List<Review> reviews = reviewRepository.findByMapCollection(map);

        List<ReviewResponse> response = reviews.stream()
                .map(r -> ReviewResponse.builder()
                        .username(r.getUser().getUsername())
                        .content(r.getContent())
                        .rating(r.getRating())
                        .build())
                .toList();

        return ResponseEntity.ok(response);
    }
}
