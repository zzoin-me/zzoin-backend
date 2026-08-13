package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByNickName(String nickName);
    boolean existsByVerifiedEmail(String verifiedEmail);
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    List<User> findAllByDeletedAtBeforeAndDeletionFinalizedAtIsNull(LocalDateTime cutoff);

    @Query("""
    SELECT u
    FROM User u
    WHERE u.email = :email
       OR u.verifiedEmail = :email
""")
    Optional<User> findByAnyEmail(@Param("email") String email);
}
