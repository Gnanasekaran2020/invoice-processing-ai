package com.invoice.repository;

import com.invoice.domain.entity.Invoice;
import com.invoice.domain.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    Page<Invoice> findByUserId(Integer userId, Pageable pageable);

    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    boolean existsByInvoiceNumber(String invoiceNumber);

    boolean existsByInvoiceNumberAndInvoiceIdNot(String invoiceNumber, Integer invoiceId);

    @Query("""
        SELECT i FROM Invoice i
                                         WHERE ( LOWER(i.vendorName) LIKE LOWER(CONCAT('%', :vendorName, '%')) OR CAST(:vendorName AS string) IS NULL )
                                            AND ( i.status = :status OR CAST(:status AS string) IS NULL )
                                            AND ( i.invoiceDate >= :fromDate OR CAST(:fromDate AS localdatetime) IS NULL )
                                            AND ( i.invoiceDate <= :toDate OR CAST(:toDate AS localdatetime) IS NULL )
                                            AND ( i.user.id = CAST(:userId AS long) OR CAST(:userId AS long) IS NULL )
                                         ORDER BY i.createdAt DESC
    """)
    Page<Invoice> searchInvoices(
        @Param("vendorName") String vendorName,
        @Param("status")     InvoiceStatus status,
        @Param("fromDate")   LocalDate fromDate,
        @Param("toDate")     LocalDate toDate,
        @Param("userId")     Integer userId,
        Pageable pageable
    );

    @Query("SELECT i FROM Invoice i WHERE i.processingStatus IN ('UPLOADED','EXTRACTING') ORDER BY i.createdAt ASC")
    List<Invoice> findPendingProcessing();

    @Query("SELECT i.status, COUNT(i) FROM Invoice i GROUP BY i.status")
    List<Object[]> countByStatus();

    @Query("SELECT i.processingStatus, COUNT(i) FROM Invoice i GROUP BY i.processingStatus")
    List<Object[]> countByProcessingStatus();

    @Query("SELECT i.status, COUNT(i) FROM Invoice i WHERE i.user.id = :userId GROUP BY i.status")
    List<Object[]> countByStatusForUser(@Param("userId") Integer userId);

    @Query("SELECT i.processingStatus, COUNT(i) FROM Invoice i WHERE i.user.id = :userId GROUP BY i.processingStatus")
    List<Object[]> countByProcessingStatusForUser(@Param("userId") Integer userId);

    @Query(value = """
        SELECT TO_CHAR(i.invoice_date, 'Mon') AS month,
               EXTRACT(YEAR  FROM i.invoice_date) AS yr,
               EXTRACT(MONTH FROM i.invoice_date) AS mon,
               COALESCE(SUM(i.amount), 0)                                              AS total,
               COALESCE(SUM(CASE WHEN i.status = 'APPROVED' THEN i.amount ELSE 0 END), 0) AS approved,
               COALESCE(SUM(CASE WHEN i.status = 'PENDING'  THEN i.amount ELSE 0 END), 0) AS pending,
               COALESCE(SUM(CASE WHEN i.status = 'REJECTED' THEN i.amount ELSE 0 END), 0) AS rejected
        FROM invoices i
        WHERE i.invoice_date IS NOT NULL
          AND (:userId IS NULL OR i.user_id = :userId)
          AND i.invoice_date >= :fromDate
        GROUP BY TO_CHAR(i.invoice_date, 'Mon'),
                 EXTRACT(YEAR  FROM i.invoice_date),
                 EXTRACT(MONTH FROM i.invoice_date)
        ORDER BY yr ASC, mon ASC
    """, nativeQuery = true)
    List<Object[]> getMonthlyTrend(@Param("userId") Integer userId,
                                   @Param("fromDate") LocalDate fromDate);
}
