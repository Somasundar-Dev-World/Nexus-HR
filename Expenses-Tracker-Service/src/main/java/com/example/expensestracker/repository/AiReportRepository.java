package com.example.expensestracker.repository;

import com.example.expensestracker.model.AiReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AiReportRepository extends JpaRepository<AiReport, Long> {
    List<AiReport> findByAppId(Long appId);
    List<AiReport> findByUserId(Long userId);
    List<AiReport> findByAppIdAndUserId(Long appId, Long userId);
}
