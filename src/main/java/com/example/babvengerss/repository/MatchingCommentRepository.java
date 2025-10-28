package com.example.babvengerss.repository;

import com.example.babvengerss.domain.Matching;
import com.example.babvengerss.domain.MatchingComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional; // 추가

import java.util.List;

public interface MatchingCommentRepository extends JpaRepository<MatchingComment, Long> {
    List<MatchingComment> findByMatchingOrderByIdAsc(Matching matching);

    // 👇 추가: 특정 Matching(게시글)에 달린 모든 댓글 삭제
    @Transactional
    void deleteByMatching(Matching matching);
}