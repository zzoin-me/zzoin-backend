package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.ChatRoom;
import com.hicct3.projectfinder.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByProject(Project project);
}
