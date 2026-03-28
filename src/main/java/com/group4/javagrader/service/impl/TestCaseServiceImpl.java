package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.TestCaseForm;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.entity.TestCase;
import com.group4.javagrader.repository.ProblemRepository;
import com.group4.javagrader.repository.TestCaseRepository;
import com.group4.javagrader.service.TestCaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestCaseServiceImpl implements TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final ProblemRepository problemRepository;

    public TestCaseServiceImpl(TestCaseRepository testCaseRepository, ProblemRepository problemRepository) {
        this.testCaseRepository = testCaseRepository;
        this.problemRepository = problemRepository;
    }

    @Override
    @Transactional
    public Long create(TestCaseForm form) {
        Problem problem = problemRepository.findById(form.getProblemId())
                .orElseThrow(() -> new IllegalArgumentException("Problem not found with ID: " + form.getProblemId()));

        TestCase testCase = new TestCase();
        testCase.setInput(form.getInput());
        testCase.setExpectedOutput(form.getExpectedOutput());
        testCase.setWeight(form.getWeight());
        testCase.setProblem(problem);

        TestCase savedTestCase = testCaseRepository.save(testCase);
        return savedTestCase.getId();
    }
}