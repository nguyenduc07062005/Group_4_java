package com.group4.javagrader.dto;

import com.group4.javagrader.entity.Semester;
import lombok.Value;

import java.util.List;

@Value
public class SemesterBoardView {

    Semester semester;
    List<CourseBoardView> courses;
    int assignmentCount;
}
