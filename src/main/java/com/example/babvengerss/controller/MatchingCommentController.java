package com.example.babvengerss.controller;

import com.example.babvengerss.domain.Matching;
import com.example.babvengerss.domain.MatchingComment;
import com.example.babvengerss.domain.User;
import com.example.babvengerss.dto.CommentRequest;
import com.example.babvengerss.dto.CommentResponse;
import com.example.babvengerss.repository.MatchingCommentRepository;
import com.example.babvengerss.repository.MatchingRepository;
import com.example.babvengerss.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/matching-comments")
public class MatchingCommentController {

    private final MatchingCommentRepository commentRepository;
    private final MatchingRepository matchingRepository;
    private final UserRepository userRepository;

    /**
     * 특정 어셈블 게시글에 메시지(댓글)를 등록합니다.
     * 호스트 또는 해당 게시글에 대해 '수락됨' 상태의 신청을 한 사용자만 등록할 수 있습니다.
     * 게시글 상태가 '모집중' 또는 '모집마감'일 경우에만 등록 가능합니다.
     *
     * @param request 댓글 요청 DTO (userId, matchingId - 게시글 ID, content)
     * @return 등록 결과 메시지
     */
    @PostMapping
    @Transactional
    public ResponseEntity<String> createComment(@RequestBody CommentRequest request) {
        User user = userRepository.findById(request.getUserId()).orElseThrow(() -> new NoSuchElementException("User not found"));
        // matchingId는 댓글을 달 대상 *게시글*의 ID를 의미합니다.
        Matching post = matchingRepository.findById(request.getMatchingId()).orElseThrow(() -> new NoSuchElementException("Matching Post not found"));
        User host = post.getSender(); // 게시글 작성자는 호스트

        // 1. 요청된 matchingId가 원본 게시글인지 확인 (receiver가 null이어야 함)
        if (post.getReceiver() != null) {
            return ResponseEntity.badRequest().body("댓글은 신청 건이 아닌 원본 게시글에만 달 수 있습니다.");
        }

        // 2. [수정된 권한 확인 로직]
        // 현재 사용자가 게시글 작성자(호스트)인지 확인
        boolean isHost = user.getId().equals(host.getId());

        // 현재 사용자가 해당 게시글(제목 기준)에 대해 '수락됨' 상태의 신청 건을 가지고 있는지 확인
        // existsBy... 메서드는 신청자(Sender=user), 호스트(Receiver=host), 게시글 제목(Title=post.getTitle()), 상태("수락됨")를 기준으로 확인합니다.
        boolean isAcceptedApplicant = matchingRepository.existsBySenderAndReceiverAndTitleAndStatus(user, host, post.getTitle(), "수락됨");

        // 호스트도 아니고, 수락된 신청자도 아니면 댓글 작성 권한 없음
        if (!isHost && !isAcceptedApplicant) {
            return ResponseEntity.status(403).body("모임 참가자(호스트 또는 수락된 신청자)만 메시지를 남길 수 있습니다.");
        }

        // 3. [수정된 상태 검증 로직]
        // 게시글 상태가 "모집중" 또는 "모집마감"일 경우에만 메시지 작성 허용
        if (!"모집중".equals(post.getStatus()) && !"모집마감".equals(post.getStatus())) {
            return ResponseEntity.badRequest().body("모집중이거나 모집 마감된 모임 게시글에만 메시지를 남길 수 있습니다.");
        }


        // 댓글 생성 및 저장
        MatchingComment comment = new MatchingComment();
        comment.setUser(user);
        comment.setMatching(post); // 댓글은 원본 게시글(post)에 연결
        comment.setContent(request.getContent());

        commentRepository.save(comment);
        return ResponseEntity.ok("✅ 메시지 등록 완료!");
    }

    /**
     * 특정 어셈블 게시글에 달린 메시지 목록을 조회합니다.
     *
     * @param matchingId 조회할 게시글의 ID
     * @return 메시지 목록 DTO 리스트
     */
    @GetMapping("/matching/{matchingId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CommentResponse>> getCommentsByMatching(@PathVariable Long matchingId) {
        // matchingId는 게시글 ID여야 합니다.
        Matching matching = matchingRepository.findById(matchingId).orElseThrow(() -> new NoSuchElementException("Matching not found"));

        // 만약 matchingId가 신청 건(receiver != null)이면 잘못된 요청으로 간주
        if (matching.getReceiver() != null) {
            return ResponseEntity.badRequest().body(null); // 혹은 에러 메시지
        }

        // 해당 게시글(matching)에 연결된 모든 댓글을 시간순으로 조회
        List<MatchingComment> comments = commentRepository.findByMatchingOrderByIdAsc(matching);

        List<CommentResponse> response = comments.stream().map(this::convertToDto).toList();
        return ResponseEntity.ok(response);
    }

    /**
     * MatchingComment 엔티티를 CommentResponse DTO로 변환합니다.
     *
     * @param comment 변환할 MatchingComment 엔티티
     * @return 변환된 CommentResponse DTO
     */
    private CommentResponse convertToDto(MatchingComment comment) {
        return CommentResponse.builder()
                .commentId(comment.getId())
                .userId(comment.getUser().getId())
                .nickname(comment.getUser().getNickname()) // 댓글 작성자의 닉네임 포함
                .content(comment.getContent())
                .build();
    }
}