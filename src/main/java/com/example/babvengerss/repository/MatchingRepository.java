package com.example.babvengerss.repository;

import com.example.babvengerss.domain.Matching;
import com.example.babvengerss.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchingRepository extends JpaRepository<Matching, Long> {
    // 호스트가 올린 게시글 목록 (receiver가 null인 경우)
    List<Matching> findBySenderAndReceiverIsNull(User sender);

    // 게시글에 달린 신청 목록 (게시글 ID로 찾기)
    List<Matching> findByStatusOrderByIdDesc(String status);

    // 받은 요청 (Receiver는 호스트, Sender는 신청자)
    List<Matching> findByReceiver(User receiver);

    // 보낸 요청 (Sender는 신청자)
    List<Matching> findBySender(User sender);

    // 전체 어셈블 게시판 (receiver가 null인 게시글만)
    List<Matching> findByReceiverIsNullOrderByIdDesc();

    // 👇 수정된 메서드: 중복 신청 확인 및 참가자 확인을 위한 메서드
    boolean existsBySenderAndReceiverAndTitleAndStatus(User sender, User receiver, String title, String status);
}
