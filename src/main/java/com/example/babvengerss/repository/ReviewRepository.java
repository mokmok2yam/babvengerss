package com.example.babvengerss.repository;

import com.example.babvengerss.domain.MapCollection;
import com.example.babvengerss.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByMapCollection(MapCollection mapCollection);
}
