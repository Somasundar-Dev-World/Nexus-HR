package com.example.expensestracker.repository;

import com.example.expensestracker.model.DeepInsightCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeepInsightCacheRepository extends JpaRepository<DeepInsightCache, Long> {
}
