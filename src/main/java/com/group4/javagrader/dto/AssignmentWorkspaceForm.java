package com.group4.javagrader.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AssignmentWorkspaceForm extends AssignmentForm {

    private Long id;

    @Valid
    private List<WorkspaceProblemForm> problems = new ArrayList<>();
}
