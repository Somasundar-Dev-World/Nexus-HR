package com.example.expensestracker.repository;

import com.example.expensestracker.model.TrackerIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface TrackerIntegrationRepository extends JpaRepository<TrackerIntegration, Long> {
    List<TrackerIntegration> findByTrackerId(Long trackerId);
    Optional<TrackerIntegration> findByTrackerIdAndProvider(Long trackerId, String provider);
}
