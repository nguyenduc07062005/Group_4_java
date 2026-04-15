package com.group4.javagrader.service;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.dto.AssignmentWorkspaceForm;
import com.group4.javagrader.entity.AssignmentAttachmentType;
import com.group4.javagrader.entity.Assignment;

import java.util.List;
import java.util.Optional;

public interface AssignmentService {

    Long create(AssignmentForm form);

    void update(Long id, AssignmentForm form);

    Long saveWorkspace(AssignmentWorkspaceForm form);

    AssignmentWorkspaceForm loadWorkspace(Long id);

    void delete(Long id);

    boolean canDelete(Long id);

    Optional<Assignment> findById(Long id);

    Optional<byte[]> findAttachmentData(Long assignmentId, AssignmentAttachmentType attachmentType);

    List<Assignment> findBySemesterId(Long semesterId);

    List<Assignment> findBySemesterIds(List<Long> semesterIds);

    List<Assignment> findByCourseId(Long courseId);
}
