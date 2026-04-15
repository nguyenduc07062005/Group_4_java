package com.group4.javagrader.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TestCaseImportPreviewForm {

    @NotNull(message = "Problem is required.")
    private Long problemId;

    @Valid
    private List<TestCaseImportRowForm> rows = new ArrayList<>();
}
