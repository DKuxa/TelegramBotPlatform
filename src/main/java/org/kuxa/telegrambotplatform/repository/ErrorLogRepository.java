package org.kuxa.telegrambotplatform.repository;

import org.kuxa.telegrambotplatform.domain.ErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {
    List<ErrorLog> findTop5ByOrderByCreatedAtDesc();
}
