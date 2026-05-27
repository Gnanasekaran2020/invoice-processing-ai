package com.invoice.domain.entity;

import com.invoice.domain.enums.AuditAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Immutable audit trail — one row per significant system event.
 * Rows must NEVER be updated or deleted; corrections are recorded
 * as a new row referencing the original via correctionId.
 */
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_user_id",     columnList = "user_id"),
        @Index(name = "idx_audit_entity",      columnList = "entity, entity_id"),
        @Index(name = "idx_audit_action",      columnList = "action"),
        @Index(name = "idx_audit_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_audit_source_ip",   columnList = "source_ip"),
        @Index(name = "idx_audit_correction",  columnList = "correction_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    /**
     * User who performed the action.
     * Nullable — system/background jobs may fire events with no human actor.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",
                foreignKey = @ForeignKey(name = "fk_audit_user"))
    private User user;

    /** What happened — strongly typed via enum */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 60)
    private AuditAction action;

    /**
     * Name of the affected domain entity class.
     * e.g. "Invoice", "User", "Notification"
     */
    @Column(name = "entity", nullable = false, length = 100)
    private String entity;

    /**
     * Primary-key value of the affected entity row (stored as String
     * so it works for Integer, Long, UUID keys alike).
     */
    @Column(name = "entity_id", length = 100)
    private String entityId;

    /**
     * Arbitrary JSON blob: before/after values, request params,
     * AWS response snippets — whatever is relevant to the action.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta_data", columnDefinition = "jsonb")
    private String metaData;

    /** IPv4 / IPv6 address of the client that triggered the action */
    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    /**
     * Points to the auditId of the original erroneous row when this
     * row represents a correction / compensating entry.
     */
    @Column(name = "correction_id")
    private Long correctionId;

    /** Exact moment the event occurred (set by the application, not the DB) */
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    /** Ensure occurredAt is always set before persisting */
    @PrePersist
    private void prePersist() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}

