package com.invoice.domain.entity;

import com.invoice.domain.enums.NotificationChannel;
import com.invoice.domain.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Tracks every outbound notification (email, SMS, push, webhook)
 * sent to a user in relation to an invoice event.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_user_id",    columnList = "user_id"),
        @Index(name = "idx_notif_invoice_id", columnList = "invoice_id"),
        @Index(name = "idx_notif_status",     columnList = "status"),
        @Index(name = "idx_notif_sent_at",    columnList = "sent_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notif_id")
    private Long notifId;

    /** Recipient user */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_notif_user"))
    private User user;

    /** Invoice that triggered this notification (nullable for system-level notifs) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id",
                foreignKey = @ForeignKey(name = "fk_notif_invoice"))
    private Invoice invoice;

    /** Delivery channel — defaults to EMAIL */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    @Builder.Default
    private NotificationChannel channel = NotificationChannel.EMAIL;

    /** Current delivery status */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    /**
     * Template name / identifier used to render the message
     * e.g. "invoice_approved", "invoice_rejected", "welcome"
     */
    @Column(name = "template", length = 100)
    private String template;

    /**
     * AWS SES Message-ID returned after successful send.
     * Null if channel != EMAIL or send not yet attempted.
     */
    @Column(name = "ses_msg_id", length = 255)
    private String sesMsgId;

    /** Timestamp when the message was successfully handed off to the provider */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * Free-form JSON payload stored alongside the notification for auditing /
     * re-send purposes (recipient address, subject, template variables …).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    /** Username / system principal that triggered this notification */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /** Row creation timestamp — immutable */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Incremented by the retry mechanism */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /** Last error message if status = FAILED */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}

