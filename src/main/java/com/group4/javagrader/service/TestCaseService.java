package com.group4.javagrader.service;

import com.group4.javagrader.dto.TestCaseForm;
import com.group4.javagrader.dto.TestCaseImportPreviewForm;
import com.group4.javagrader.entity.TestCase;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TestCaseService {

    Long create(TestCaseForm form);

    void update(Long id, TestCaseForm form);

    void delete(Long problemId, Long testCaseId);

    TestCaseImportPreviewForm buildImportPreview(Long problemId, MultipartFile file);

    long saveImportedTestCases(TestCaseImportPreviewForm previewForm);

    List<TestCase> findByProblemId(Long problemId);

    long countByProblemId(Long problemId);
}
