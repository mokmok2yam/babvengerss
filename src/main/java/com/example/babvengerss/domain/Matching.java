//진짜채팅구현 시자
package com.example.babvengerss.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Matching {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id") // MapCollection 대신 Restaurant 연결
    private Restaurant restaurant;

    private String status;

    // 추가: Assemble Post 제목
    private String title;

    // 추가: 모임 시간
    private String meetingTime;
}