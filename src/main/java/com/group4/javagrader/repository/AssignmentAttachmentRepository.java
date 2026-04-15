package com.group4.javagrader.repository;

import com.group4.javagrader.entity.AssignmentAttachment;
import com.group4.javagrader.entity.AssignmentAttachmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssignmentAttachmentRepository extends JpaRepository<AssignmentAttachment, Long> {

    Optional<AssignmentAttachment> findByAssignmentIdAndAttachmentType(Long assignmentId, AssignmentAttachmentType attachmentType);

    void deleteByAssignmentIdAndAttachmentType(Long assignmentId, AssignmentAttachmentType attachmentType);
}
