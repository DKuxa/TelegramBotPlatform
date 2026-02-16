package org.kuxa.telegrambotplatform.repository;

import org.kuxa.telegrambotplatform.domain.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    /**
     * Finds the last N events for a specific source.
     */
    List<WebhookEvent> findTop10BySourceOrderByCreatedAtDesc(String source);

    /**
     * Finds all failed events in the last 24 hours.
     */
    @Query("SELECT w FROM WebhookEvent w WHERE w.processedSuccessfully = false AND w.createdAt >= :since")
    List<WebhookEvent> findFailedEventsSince(LocalDateTime since);

    /**
     * Counts total events from a source for today.
     */
    @Query(value = "SELECT COUNT(*) FROM webhook_event WHERE source = :source AND DATE(created_at) = CURRENT_DATE", nativeQuery = true)
    long countTodayEventsBySource(String source);
}
