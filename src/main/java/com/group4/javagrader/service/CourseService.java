package com.group4.javagrader.service;

import com.group4.javagrader.dto.CourseForm;
import com.group4.javagrader.entity.Course;

import java.util.List;
import java.util.Optional;

public interface CourseService {

    Long create(CourseForm form);

    List<Course> findBySemesterId(Long semesterId);

    List<Course> findBySemesterIds(List<Long> semesterIds);

    Optional<Course> findById(Long id);

    Course ensureGeneralCourse(Long semesterId);

    void update(Long id, CourseForm form);

    void archive(Long id);
}
