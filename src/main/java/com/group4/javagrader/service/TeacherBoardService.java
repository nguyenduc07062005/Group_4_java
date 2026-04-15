package com.group4.javagrader.service;

import com.group4.javagrader.dto.SemesterBoardView;

import java.util.List;
import java.util.Optional;

public interface TeacherBoardService {

    List<SemesterBoardView> buildActiveSemesterBoard();

    Optional<SemesterBoardView> buildSemesterBoard(Long semesterId);
}
