package com.group4.javagrader.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class TestCaseImportForm {

    private Long problemId;

    private MultipartFile importFile;
}
