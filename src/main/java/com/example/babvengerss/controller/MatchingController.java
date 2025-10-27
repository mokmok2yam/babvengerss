package com.example.babvengerss.controller;

import com.example.babvengerss.domain.MapCollection;
import com.example.babvengerss.domain.Matching;
import com.example.babvengerss.domain.User;
import com.example.babvengerss.dto.MatchingRequest;
import com.example.babvengerss.dto.MatchingResponse;
import com.example.babvengerss.dto.MatchingStatusUpdateRequest;
import com.example.babvengerss.repository.MapCollectionRepository;
import com.example.babvengerss.repository.MatchingRepository;
import com.example.babvengerss.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/matching")
public class MatchingController {

    private final MatchingRepository matchingRepository;
    private final UserRepository userRepository;
    private final MapCollectionRepository mapCollectionRepository;

    // 매칭 요청 생성
    @PostMapping
    public String requestMatching(@RequestBody MatchingRequest dto) {
        User sender = userRepository.findById(dto.getSenderId()).orElseThrow();
        User receiver = userRepository.findById(dto.getReceiverId()).orElseThrow();
        MapCollection map = mapCollectionRepository.findById(dto.getMapCollectionId()).orElseThrow();

        Matching matching = new Matching();
        matching.setSender(sender);
        matching.setReceiver(receiver);
        matching.setMapCollection(map);
        matching.setStatus("요청됨");

        matchingRepository.save(matching);
        return "매칭 요청 완료!";
    }

    // 받은 매칭 목록 조회
    @GetMapping("/received/{userId}")
    public List<MatchingResponse> getReceived(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        List<Matching> list = matchingRepository.findByReceiver(user);

        return list.stream().map(m -> {
            MatchingResponse dto = new MatchingResponse();
            dto.setId(m.getId());
            dto.setSenderName(m.getSender().getNickname());
            dto.setReceiverName(m.getReceiver().getNickname());
            dto.setMapName(m.getMapCollection().getName());
            dto.setStatus(m.getStatus());
            return dto;
        }).collect(Collectors.toList());
    }

    // 보낸 매칭 목록 조회 (sender 기준)
    @GetMapping("/sent/{userId}")
    public List<MatchingResponse> getSent(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        List<Matching> list = matchingRepository.findBySender(user);

        return list.stream().map(m -> {
            MatchingResponse dto = new MatchingResponse();
            dto.setId(m.getId());
            dto.setSenderName(m.getSender().getNickname());
            dto.setReceiverName(m.getReceiver().getNickname());
            dto.setMapName(m.getMapCollection().getName());
            dto.setStatus(m.getStatus());
            return dto;
        }).collect(Collectors.toList());
    }

    @PatchMapping("/update-status")
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
    // ✅ 수락된 최신 매칭 목록 조회
    @GetMapping("/accepted")
    @Transactional(readOnly = true)
    public List<MatchingResponse> getAcceptedMatches() {
        List<Matching> list = matchingRepository.findByStatusOrderByIdDesc("수락됨");

        return list.stream().map(m -> {
            MatchingResponse dto = new MatchingResponse();
            dto.setId(m.getId());
            dto.setSenderName(m.getSender().getNickname());
            dto.setReceiverName(m.getReceiver().getNickname());
            dto.setMapName(m.getMapCollection().getName());
            dto.setStatus(m.getStatus());
            return dto;
        }).collect(Collectors.toList());
    }
}


