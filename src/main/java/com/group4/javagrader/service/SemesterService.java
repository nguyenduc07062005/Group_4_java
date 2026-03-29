package com.group4.javagrader.service;

import com.group4.javagrader.dto.SemesterDetailDto;
import com.group4.javagrader.dto.SemesterForm;
import com.group4.javagrader.dto.SemesterSummaryDto;
import java.util.List;
import java.util.Optional;

public interface SemesterService {

    List<SemesterSummaryDto> findAllSummaries();

    Optional<SemesterDetailDto> findDetailById(Long id);

    Long create(SemesterForm form);

    boolean existsById(Long id);

    boolean existsByCode(String code);
}
