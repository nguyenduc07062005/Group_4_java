package com.group4.javagrader.dto;

import com.group4.javagrader.entity.GradingMode;
import lombok.Value;

import java.util.List;

@Value
public class DashboardView {

    List<StatCard> stats;
    List<SemesterSection> semesters;
    Long spotlightSemesterId;
    List<AttentionItem> attentionItems;
    List<RunningBatchItem> runningBatches;
    List<ActivityItem> recentActivities;
    List<TaskRow> taskRows;
    List<CourseStatusBar> courseStatusBars;
    List<WeekLoadBar> weekLoadBars;

    public boolean hasSemesters() {
        return !semesters.isEmpty();
    }

    public boolean hasAttentionItems() {
        return !attentionItems.isEmpty();
    }

    public boolean hasRunningBatches() {
        return !runningBatches.isEmpty();
    }

    public boolean hasRecentActivities() {
        return !recentActivities.isEmpty();
    }

    public boolean hasTaskRows() {
        return !taskRows.isEmpty();
    }

    public boolean hasCourseStatusBars() {
        return !courseStatusBars.isEmpty();
    }

    public boolean hasWeekLoadBars() {
        return !weekLoadBars.isEmpty();
    }

    @Value
    public static class StatCard {
        String label;
        String value;
        String helper;
        String icon;
    }

    @Value
    public static class SemesterSection {
        Long id;
        String code;
        String name;
        String schedule;
        int courseCount;
        int assignmentCount;
        String openUrl;
        String createCourseUrl;
        String createAssignmentUrl;
        List<CourseSection> courses;
    }

    @Value
    public static class CourseSection {
        Long id;
        String code;
        String name;
        int weekCount;
        int assignmentCount;
        String createAssignmentUrl;
        List<AssignmentLane> lanes;
    }

    @Value
    public static class AssignmentLane {
        String label;
        String helper;
        String icon;
        List<AssignmentCard> assignments;
    }

    @Value
    public static class AssignmentCard {
        Long id;
        String name;
        GradingMode gradingMode;
        String stageKey;
        String stageLabel;
        String statusLabel;
        String statusHelper;
        int questionCount;
        long testcaseCount;
        int submissionCount;
        int validatedCount;
        String primaryActionLabel;
        String primaryActionUrl;
    }

    @Value
    public static class AttentionItem {
        String icon;
        String title;
        String detail;
        String courseLabel;
        String actionLabel;
        String actionUrl;
    }

    @Value
    public static class RunningBatchItem {
        String assignmentName;
        String courseLabel;
        String status;
        String progressLabel;
        int progressPercent;
        String actionLabel;
        String actionUrl;
    }

    @Value
    public static class ActivityItem {
        String icon;
        String title;
        String detail;
        String timeLabel;
        String actionLabel;
        String actionUrl;
    }

    @Value
    public static class TaskRow {
        String assignmentName;
        String courseLabel;
        String weekLabel;
        String stageLabel;
        String issue;
        String submissionsLabel;
        String updatedLabel;
        String actionLabel;
        String actionUrl;
    }

    @Value
    public static class CourseStatusBar {
        String courseLabel;
        int setupCount;
        int intakeCount;
        int plagiarismCount;
        int batchCount;
        int resultsCount;
        int totalCount;

        public int setupPercent() {
            return width(setupCount);
        }

        public int intakePercent() {
            return width(intakeCount);
        }

        public int plagiarismPercent() {
            return width(plagiarismCount);
        }

        public int batchPercent() {
            return width(batchCount);
        }

        public int resultsPercent() {
            return width(resultsCount);
        }

        private int width(int value) {
            if (totalCount <= 0) {
                return 0;
            }
            return (int) Math.round((value * 100.0) / totalCount);
        }
    }

    @Value
    public static class WeekLoadBar {
        String label;
        int submissionCount;
        int widthPercent;
    }
}
