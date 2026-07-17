package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Stack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StackRepository extends JpaRepository<Stack, Long> {
    boolean existsByName(String name);
}
