package com.hicct3.projectfinder.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name="refresh_tokens")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true)
    Long userId;

    @Column(nullable = false, unique = true)
    String token;


    public void update(String token) {
        this.token = token;
    }
}
