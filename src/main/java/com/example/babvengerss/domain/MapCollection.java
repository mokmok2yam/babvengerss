package com.example.babvengerss.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class MapCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private double latitude;
    private double longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToMany
    @JoinTable(
            name = "map_collection_restaurants",
            joinColumns = @JoinColumn(name = "map_collection_id"),
            inverseJoinColumns = @JoinColumn(name = "restaurant_id")
    )
    private List<Restaurant> restaurants;

    @Formula("(SELECT AVG(r.rating) FROM review r WHERE r.map_collection_id = id)")
    private Double averageRating;
    @Formula("(SELECT count(*) FROM review r WHERE r.map_collection_id = id)")
    private int reviewCount;
}
