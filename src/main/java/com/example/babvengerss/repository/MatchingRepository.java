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

    // 중복 신청 확인 및 참가자 확인
    boolean existsBySenderAndReceiverAndTitleAndStatus(User sender, User receiver, String title, String status);

    // 👇 추가: 특정 호스트(Receiver)의 특정 게시글 제목(Title)에 대한 모든 신청 건 조회 (게시글 삭제 시 관련 신청 삭제용)
    List<Matching> findByReceiverAndTitle(User receiver, String title);

    // 👇 추가: 참가자 확인용 (수락된 참가자인지 확인)
    List<Matching> findByReceiverAndTitleAndStatus(User receiver, String title, String status);

    // 👇 추가: ID와 사용자(Sender 또는 Receiver)로 신청/게시글 찾기 (삭제 권한 확인용)
    Optional<Matching> findByIdAndSender(Long id, User sender);
    Optional<Matching> findByIdAndReceiver(Long id, User receiver);
}