package com.hicct3.projectfinder.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "application_answers")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private ProjectApplication application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private ProjectQuestion question;

    @Column(nullable = false, length = 500)
    private String answerText;
}
