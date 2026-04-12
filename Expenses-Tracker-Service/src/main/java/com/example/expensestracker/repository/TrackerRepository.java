package com.example.expensestracker.repository;

import com.example.expensestracker.model.Tracker;
import com.example.expensestracker.model.TrackerType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrackerRepository extends JpaRepository<Tracker, Long> {
    List<Tracker> findByUserId(Long userId);
    List<Tracker> findByUserIdAndType(Long userId, TrackerType type);
    List<Tracker> findByUserIdAndAppId(Long userId, Long appId);
    List<Tracker> findByAppIdAndUserId(Long appId, Long userId);
}
