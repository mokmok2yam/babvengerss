package com.example.babvengerss.repository;

import com.example.babvengerss.domain.MapCollection;
import com.example.babvengerss.domain.Matching;
import com.example.babvengerss.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchingRepository extends JpaRepository<Matching, Long> {
    List<Matching> findBySender(User sender);
    List<Matching> findByReceiver(User receiver);
    List<Matching> findByMapCollection(MapCollection mapCollection);
}
