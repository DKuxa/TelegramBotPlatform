package org.kuxa.telegrambotplatform.repository;

import org.kuxa.telegrambotplatform.domain.ActionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionLogRepository extends JpaRepository<ActionLog, Long> {

    @Query(value = "SELECT COUNT(DISTINCT chat_id) FROM action_log WHERE DATE(created_at) = CURRENT_DATE", nativeQuery = true)
    long countUniqueUsersToday();
}