package com.example.expensestracker.repository;

import com.example.expensestracker.model.TrackerApp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrackerAppRepository extends JpaRepository<TrackerApp, Long> {
    List<TrackerApp> findByUserId(Long userId);
}
