package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.SemesterForm;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.repository.SemesterRepository;
import com.group4.javagrader.service.SemesterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SemesterServiceImpl implements SemesterService {

    private final SemesterRepository semesterRepository;

    public SemesterServiceImpl(SemesterRepository semesterRepository) {
        this.semesterRepository = semesterRepository;
    }

    @Override
    @Transactional
    public Long create(SemesterForm form) {
        Semester semester = new Semester();
        semester.setName(form.getName());

        Semester savedSemester = semesterRepository.save(semester);
        return savedSemester.getId();
    }
}