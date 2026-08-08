package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.DeviceToken;
import com.hicct3.projectfinder.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    List<DeviceToken> findAllByUser(User user);
    void deleteByToken(String token);
}
