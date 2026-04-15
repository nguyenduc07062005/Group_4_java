package com.group4.javagrader.dto;

import com.group4.javagrader.entity.Course;
import lombok.Value;

import java.util.List;

@Value
public class CourseBoardView {

    Course course;
    List<AssignmentGroupView> assignmentGroups;
    int assignmentCount;
}
