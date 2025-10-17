package com.example.babvengerss.repository;

import com.example.babvengerss.domain.MapCollection;
import com.example.babvengerss.domain.Review;
import com.example.babvengerss.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByMapCollection(MapCollection mapCollection);
    List<Review> findByUser(User user);
}
