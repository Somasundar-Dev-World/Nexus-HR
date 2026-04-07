package com.example.expensestracker.repository;

import com.example.expensestracker.model.AiInsightCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiInsightCacheRepository extends JpaRepository<AiInsightCache, Long> {
}
