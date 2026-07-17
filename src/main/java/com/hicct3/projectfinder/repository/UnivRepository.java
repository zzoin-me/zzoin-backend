package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.SchoolDomain;
import com.hicct3.projectfinder.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UnivRepository extends JpaRepository<SchoolDomain, Long> {

}
