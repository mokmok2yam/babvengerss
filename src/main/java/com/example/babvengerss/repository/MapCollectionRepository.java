package com.example.babvengerss.repository;

import com.example.babvengerss.domain.MapCollection;
import com.example.babvengerss.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MapCollectionRepository extends JpaRepository<MapCollection, Long> {
    List<MapCollection> findByUser(User user);

    List<MapCollection> findByNameContaining(String keyword);

    // 👇 JOIN 구문을 명시적으로 추가하여 쿼리 수정
    @Query("SELECT m FROM MapCollection m JOIN m.user u WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<MapCollection> findByNameOrUserNickname(@Param("keyword") String keyword);
}