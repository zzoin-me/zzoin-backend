package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.DeviceToken;
import com.hicct3.projectfinder.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    List<DeviceToken> findAllByUser(User user);
    List<DeviceToken> findAllByUser_UserId(Long userId);
    java.util.Optional<DeviceToken> findByToken(String token);
    void deleteByToken(String token);
}
