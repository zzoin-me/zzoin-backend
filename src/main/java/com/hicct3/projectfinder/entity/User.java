package com.hicct3.projectfinder.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    @Column(nullable = false, unique = true, length = 20)
    private String nickName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private String provider = "local";

    @Column
    private String providerId;

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

    @Column(length = 500)
    private String bio;

    @Column
    private String profileUrl;

    @Column
    private String socialProfileUrl;

    @Column
    private LocalDateTime nicknameChangedAt;

    @Column
    private LocalDateTime deletedAt;

    @Column
    private LocalDateTime deletionFinalizedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean localLoginEnabled = true;

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

    public void requestWithdrawal(LocalDateTime requestedAt) {
        this.deletedAt = requestedAt;
        this.deletionFinalizedAt = null;
    }

    public boolean isRecoverable(LocalDateTime now) {
        return this.deletedAt != null
                && this.deletionFinalizedAt == null
                && now.isBefore(this.deletedAt.plusDays(30));
    }

    public LocalDateTime getRecoverableUntil() {
        return this.deletedAt == null ? null : this.deletedAt.plusDays(30);
    }

    public void recover() {
        this.deletedAt = null;
        this.deletionFinalizedAt = null;
    }

    public void finalizeWithdrawal(LocalDateTime finalizedAt)
    {
        this.deletionFinalizedAt = finalizedAt;

        this.nickName = "DELETED" + Long.toUnsignedString(this.userId, 36).toUpperCase();
        this.email = "DELETED_EMAIL_" + this.userId + "@deleted.local";
        this.verifiedEmail = null;

        this.password = "";
        this.provider = "local";
        this.providerId = null;
        this.localLoginEnabled = false;
        this.verified = false;
        this.grade = null;
        this.major = null;
        this.field = null;
        this.bio = null;
        this.profileUrl = null;
        this.socialProfileUrl = null;
        this.schoolDomain = null;

        this.stacks.clear();
    }

    public Boolean isDeleted()
    {
        return this.deletedAt != null;
    }

    @Transient
    public List<String> getFields() {
        if (field == null || field.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(field.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public void setFields(List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            this.field = null;
        } else {
            this.field = String.join(",", fields.stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
        }
    }

    @Transient
    public boolean hasCustomProfileImage() {
        return profileUrl != null && !profileUrl.equals(socialProfileUrl);
    }
}
