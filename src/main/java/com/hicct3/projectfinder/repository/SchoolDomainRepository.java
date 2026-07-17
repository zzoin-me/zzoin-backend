package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.SchoolDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolDomainRepository extends JpaRepository<SchoolDomain, Long> {
    boolean existsByDomain(String domain);
}
