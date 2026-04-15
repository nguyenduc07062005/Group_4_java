package com.group4.javagrader.dto;

import com.group4.javagrader.entity.Assignment;
import lombok.Value;

import java.util.List;

@Value
public class AssignmentGroupView {

    String label;
    String helper;
    String icon;
    Integer weekNumber;
    List<Assignment> assignments;

    public boolean isWeekly() {
        return weekNumber != null;
    }
}
