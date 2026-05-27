package com.invoice.repository;

import com.invoice.domain.entity.Notification;
import com.invoice.domain.enums.NotificationChannel;
import com.invoice.domain.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** All notifications for a specific user, newest first */
    Page<Notification> findByUser_IdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    /** All notifications linked to a specific invoice */
    List<Notification> findByInvoice_InvoiceIdOrderByCreatedAtDesc(Integer invoiceId);

    /** Fetch pending/retrying rows for the retry-scheduler */
    List<Notification> findByStatusInAndRetryCountLessThan(
            List<NotificationStatus> statuses, int maxRetries);

    /** Count unread (PENDING) notifications for a user */
    long countByUser_IdAndStatus(Integer userId, NotificationStatus status);

    /** Find by SES message ID (for bounce/complaint webhooks) */
    List<Notification> findBySesMsgId(String sesMsgId);

    /** Notifications by channel and status — useful for monitoring dashboards */
    List<Notification> findByChannelAndStatus(NotificationChannel channel, NotificationStatus status);

    /** Notifications sent within a time window — for rate-limit checks */
    @Query("""
           SELECT n FROM Notification n
           WHERE n.user.id = :userId
             AND n.channel = :channel
             AND n.createdAt >= :since
           """)
    List<Notification> findRecentByUserAndChannel(
            @Param("userId")  Integer userId,
            @Param("channel") NotificationChannel channel,
            @Param("since")   LocalDateTime since);

    /** Bulk-mark notifications as SENT */
    @Modifying
    @Query("""
           UPDATE Notification n
           SET n.status = 'SENT', n.sentAt = :sentAt
           WHERE n.notifId IN :ids
           """)
    int markAsSent(@Param("ids") List<Long> ids, @Param("sentAt") LocalDateTime sentAt);
}

