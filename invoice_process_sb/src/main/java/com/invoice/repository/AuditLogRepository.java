package com.invoice.repository;

import com.invoice.domain.entity.AuditLog;
import com.invoice.domain.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** Full audit trail for a specific user, newest first */
    Page<AuditLog> findByUser_IdOrderByOccurredAtDesc(Integer userId, Pageable pageable);

    /** All events for a specific entity row e.g. ("Invoice", "1001") */
    List<AuditLog> findByEntityAndEntityIdOrderByOccurredAtDesc(String entity, String entityId);

    /** Filter by action type */
    Page<AuditLog> findByActionOrderByOccurredAtDesc(AuditAction action, Pageable pageable);

    /** Filter by source IP — for security investigation */
    List<AuditLog> findBySourceIpAndOccurredAtBetweenOrderByOccurredAtDesc(
            String sourceIp, LocalDateTime from, LocalDateTime to);

    /** All corrections that reference a given original audit row */
    List<AuditLog> findByCorrectionId(Long originalAuditId);

    /** Time-range query — used for compliance exports */
    @Query("""
           SELECT a FROM AuditLog a
           WHERE a.occurredAt BETWEEN :from AND :to
             AND (:action IS NULL OR a.action = :action)
             AND (:entity IS NULL OR a.entity = :entity)
           ORDER BY a.occurredAt DESC
           """)
    Page<AuditLog> findByFilters(
            @Param("from")   LocalDateTime from,
            @Param("to")     LocalDateTime to,
            @Param("action") AuditAction action,
            @Param("entity") String entity,
            Pageable pageable);

    /** Count actions per user in a window — for anomaly / brute-force detection */
    @Query("""
           SELECT COUNT(a) FROM AuditLog a
           WHERE a.user.id = :userId
             AND a.action  = :action
             AND a.occurredAt >= :since
           """)
    long countRecentActionsByUser(
            @Param("userId") Integer userId,
            @Param("action") AuditAction action,
            @Param("since")  LocalDateTime since);

    /** Events from a specific IP in the last N minutes — intrusion detection */
    @Query("""
           SELECT a FROM AuditLog a
           WHERE a.sourceIp   = :ip
             AND a.occurredAt >= :since
           ORDER BY a.occurredAt DESC
           """)
    List<AuditLog> findRecentBySourceIp(
            @Param("ip")    String ip,
            @Param("since") LocalDateTime since);
}

