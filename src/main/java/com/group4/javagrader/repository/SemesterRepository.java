package com.group4.javagrader.repository;

import com.group4.javagrader.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

    Optional<Semester> findFirstByArchivedFalseOrderByStartDateDescIdDesc();

    List<Semester> findByArchivedFalseOrderByStartDateDescIdDesc();

    Optional<Semester> findByIdAndArchivedFalse(Long id);

    @Query(value = "SELECT * FROM semesters WHERE id = :id AND archived = 0 FOR UPDATE", nativeQuery = true)
    Optional<Semester> findByIdAndArchivedFalseForUpdate(@Param("id") Long id);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
