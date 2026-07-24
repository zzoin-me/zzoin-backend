package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.SchoolDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SchoolDomainRepository extends JpaRepository<SchoolDomain, Long> {
    @Query("""
    SELECT CASE
    WHEN COUNT(s) > 0
    THEN true ELSE false
    END FROM SchoolDomain s
    WHERE :emailDomain = s.domain OR :emailDomain
    LIKE CONCAT('%.', s.domain)
    """)
    boolean existsByMatchingDomain(@Param("emailDomain") String emailDomain);

    @Query("""
    SELECT s FROM SchoolDomain s
    WHERE :emailDomain = s.domain OR :emailDomain
    LIKE CONCAT('%.', s.domain)
    """)
    Optional<SchoolDomain> findByMatchingDomain(@Param("emailDomain") String emailDomain);
}
