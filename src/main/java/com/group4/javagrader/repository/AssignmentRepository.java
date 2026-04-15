package com.group4.javagrader.repository;

import com.group4.javagrader.entity.Assignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    boolean existsBySemesterId(Long semesterId);

    boolean existsByCourseId(Long courseId);

    @Query("""
            select assignment
            from Assignment assignment
            join fetch assignment.semester
            join fetch assignment.course
            where assignment.id = :id
            """)
    Optional<Assignment> findByIdWithSemesterAndCourse(@Param("id") Long id);

    @EntityGraph(attributePaths = {"semester", "course"})
    List<Assignment> findBySemesterIdOrderByCourseIdAscDisplayOrderAscIdAsc(Long semesterId);

    @EntityGraph(attributePaths = {"semester", "course"})
    List<Assignment> findBySemesterIdInOrderBySemesterIdAscCourseIdAscDisplayOrderAscIdAsc(List<Long> semesterIds);

    @EntityGraph(attributePaths = {"semester", "course"})
    List<Assignment> findByCourseIdOrderByDisplayOrderAscIdAsc(Long courseId);

    @Query(value = "SELECT * FROM assignments WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Assignment> findByIdForUpdate(@Param("id") Long id);
}
