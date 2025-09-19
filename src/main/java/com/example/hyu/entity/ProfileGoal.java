package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "profile_goals")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProfileGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String text;

    private Integer orderNo;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
