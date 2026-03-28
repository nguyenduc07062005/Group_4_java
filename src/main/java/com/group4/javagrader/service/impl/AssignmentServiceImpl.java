package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.SemesterRepository;
import com.group4.javagrader.service.AssignmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SemesterRepository semesterRepository;

    public AssignmentServiceImpl(AssignmentRepository assignmentRepository, SemesterRepository semesterRepository) {
        this.assignmentRepository = assignmentRepository;
        this.semesterRepository = semesterRepository;
    }

    @Override
    @Transactional
    public Long create(AssignmentForm form) {
        Semester semester = semesterRepository.findById(form.getSemesterId())
                .orElseThrow(() -> new IllegalArgumentException("Semester not found with ID: " + form.getSemesterId()));

        Assignment assignment = new Assignment();
        assignment.setAssignmentName(form.getAssignmentName());
        assignment.setSemester(semester);

        Assignment savedAssignment = assignmentRepository.save(assignment);
        return savedAssignment.getId();
    }
}