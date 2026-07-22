package com.hicct3.projectfinder.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="users")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String nickName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true)
    private String verifiedEmail;

    @Column(nullable = false)
    private Boolean verified;

    @Column
    private Integer grade;

    @Column
    private String major;

    @Column
    private String field;

    @Column
    private String bio;

    @Column
    private String profileUrl;

    @Column
    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private Boolean admin;

    @Column(nullable = false)
    @Builder.Default
    private Double ratingAvg = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Integer ratingCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_domain_id")
    private SchoolDomain schoolDomain;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "user_stacks",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "stack_id")
    )
    private List<Stack> stacks = new ArrayList<>();

    //계정 삭제 처리
    public void withDraw()
    {
        this.deletedAt = LocalDateTime.now();

        this.nickName = "DELETED_USER_" + this.userId;
        this.email = "DELETED_EMAIL_" + this.userId + "@deleted.local";
        this.verifiedEmail = null;

        this.password = "";
        this.verified = false;
        this.grade = null;
        this.major = null;
        this.field = null;
        this.bio = null;
        this.profileUrl = null;
        this.schoolDomain = null;

        this.stacks.clear();
    }

    public Boolean isDeleted()
    {
        return this.deletedAt != null;
    }
}
