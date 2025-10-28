// 파일: src/main/java/com/example/babvengerss/repository/MatchingCommentRepository.java
package com.example.babvengerss.repository;

import com.example.babvengerss.domain.Matching;
import com.example.babvengerss.domain.MatchingComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchingCommentRepository extends JpaRepository<MatchingComment, Long> {
    List<MatchingComment> findByMatchingOrderByIdAsc(Matching matching);
}