package com.group4.javagrader.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class SubmissionUploadForm {

    @NotNull(message = "Assignment is required.")
    private Long assignmentId;

    private MultipartFile archiveFile;
}
