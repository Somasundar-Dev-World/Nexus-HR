package com.example.expensestracker.repository;

import com.example.expensestracker.model.TrackerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrackerEntryRepository extends JpaRepository<TrackerEntry, Long> {
    List<TrackerEntry> findByTrackerId(Long trackerId);
    List<TrackerEntry> findByUserId(Long userId);
    List<TrackerEntry> findByTrackerIdOrderByDateAsc(Long trackerId);
    List<TrackerEntry> findByTrackerIdAndUserId(Long trackerId, Long userId);
}
