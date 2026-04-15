package com.group4.javagrader.repository;

import com.group4.javagrader.entity.PlagiarismPair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlagiarismPairRepository extends JpaRepository<PlagiarismPair, Long> {

    List<PlagiarismPair> findByReportIdOrderByFinalScoreDescIdAsc(Long reportId);
}
