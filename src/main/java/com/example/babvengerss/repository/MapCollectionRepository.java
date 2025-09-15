package com.example.babvengerss.repository;

import com.example.babvengerss.domain.MapCollection;
import com.example.babvengerss.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MapCollectionRepository extends JpaRepository<MapCollection, Long> {
    List<MapCollection> findByUser(User user);
    List<MapCollection> findByNameContaining(String keyword);
}
