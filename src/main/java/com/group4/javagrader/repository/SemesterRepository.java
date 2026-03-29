package com.group4.javagrader.repository;

import com.group4.javagrader.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

    Optional<Semester> findFirstByArchivedFalseOrderByStartDateDesc();

    boolean existsByCode(String code);
}
