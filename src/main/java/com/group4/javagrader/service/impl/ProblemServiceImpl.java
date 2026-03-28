package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.ProblemForm;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.ProblemRepository;
import com.group4.javagrader.service.ProblemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProblemServiceImpl implements ProblemService {

    private final ProblemRepository problemRepository;
    private final AssignmentRepository assignmentRepository;

    public ProblemServiceImpl(ProblemRepository problemRepository, AssignmentRepository assignmentRepository) {
        this.problemRepository = problemRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    @Transactional
    public Long create(ProblemForm form) {
        Assignment assignment = assignmentRepository.findById(form.getAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found with ID: " + form.getAssignmentId()));

        Problem problem = new Problem();
        problem.setProblemName(form.getProblemName());
        problem.setAssignment(assignment);

        Problem savedProblem = problemRepository.save(problem);
        return savedProblem.getId();
    }
}