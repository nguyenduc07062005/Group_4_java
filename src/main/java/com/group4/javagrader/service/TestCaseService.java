package com.group4.javagrader.service;

import com.group4.javagrader.dto.TestCaseForm;

public interface TestCaseService {
    Long create(TestCaseForm form);
}