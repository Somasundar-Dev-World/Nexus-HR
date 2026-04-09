package com.example.expensestracker.repository;

import com.example.expensestracker.model.TrackerMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TrackerMappingRepository extends JpaRepository<TrackerMapping, Long> {
    Optional<TrackerMapping> findByTrackerIdAndCsvHeaders(Long trackerId, String csvHeaders);
}
