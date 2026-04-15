package com.group4.javagrader.dto;

import lombok.Value;

@Value
public class HeaderSemesterSummary {

    Long id;
    String code;
    String name;
    int courseCount;
    int assignmentCount;
}
