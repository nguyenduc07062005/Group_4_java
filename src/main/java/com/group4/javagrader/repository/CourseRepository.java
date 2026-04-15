package com.group4.javagrader.repository;

import com.group4.javagrader.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    boolean existsBySemesterIdAndArchivedFalse(Long semesterId);

    List<Course> findBySemesterIdAndArchivedFalseOrderByCourseCodeAscIdAsc(Long semesterId);

    List<Course> findBySemesterIdInAndArchivedFalseOrderBySemesterIdAscCourseCodeAscIdAsc(List<Long> semesterIds);

    Optional<Course> findFirstBySemesterIdAndArchivedFalseOrderByIdAsc(Long semesterId);

    Optional<Course> findBySemesterIdAndCourseCodeIgnoreCase(Long semesterId, String courseCode);

    Optional<Course> findBySemesterIdAndCourseCodeIgnoreCaseAndArchivedFalse(Long semesterId, String courseCode);

    boolean existsBySemesterIdAndCourseCodeIgnoreCaseAndIdNot(Long semesterId, String courseCode, Long id);

    boolean existsBySemesterIdAndCourseCodeIgnoreCaseAndArchivedFalse(Long semesterId, String courseCode);

    boolean existsBySemesterIdAndCourseCodeIgnoreCaseAndIdNotAndArchivedFalse(Long semesterId, String courseCode, Long id);
}
