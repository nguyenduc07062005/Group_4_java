package com.group4.javagrader.service;

import com.group4.javagrader.dto.SemesterForm;
import com.group4.javagrader.entity.Semester;

import java.util.List;
import java.util.Optional;

public interface SemesterService {

    Long create(SemesterForm form);

    void update(Long id, SemesterForm form);

    void archive(Long id);

    List<Semester> findActiveSemesters();

    Optional<Semester> findById(Long id);

    Optional<Semester> findActiveById(Long id);
}
