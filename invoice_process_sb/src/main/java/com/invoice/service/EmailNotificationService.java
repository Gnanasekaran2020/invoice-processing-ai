package com.invoice.service;

import com.invoice.domain.entity.Invoice;
import com.invoice.domain.entity.User;
import com.invoice.domain.enums.InvoiceStatus;
import com.invoice.domain.enums.UserRole;
import com.invoice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final SesClient sesClient;
    private final UserRepository userRepository;

    @Value("${app.aws.ses.from-email:noreply@u2acts.com}")
    private String fromEmail;

    @Value("${app.aws.ses.reply-to-email:support@u2acts.com}")
    private String replyToEmail;

    @Value("${app.notifications.enabled:true}")
    private boolean notificationsEnabled;

    // ── Invoice lifecycle ─────────────────────────────────────────────────────

    @Async
    public void sendInvoiceUploaded(Invoice invoice) {
        if (!notificationsEnabled || invoice.getUser() == null) return;
        String ref = ref(invoice);
        // Notify uploader
        send(invoice.getUser().getEmail(),
             "Invoice Received — " + ref,
             html(invoice.getUser().getFullName(),
                  "📤 Invoice Received",
                  "We have received your invoice <strong>" + ref + "</strong> and AI processing has started.",
                  "#1677ff",
                  row("File", invoice.getOriginalFileName()) +
                  row("Uploaded At", now()) +
                  row("Status", "Uploaded — AI extraction queued")));

        // Notify all admins
        notifyAdmins(
             "📥 New Invoice Uploaded — " + ref,
             "A new invoice has been uploaded by <strong>" + invoice.getUser().getEmail() + "</strong> and is queued for AI extraction.",
             "#1677ff",
             row("Invoice Ref", ref) +
             row("File", invoice.getOriginalFileName()) +
             row("Uploaded By", invoice.getUser().getEmail()) +
             row("Uploaded At", now()));
    }

    @Async
    public void sendInvoiceExtracting(Invoice invoice) {
        if (!notificationsEnabled || invoice.getUser() == null) return;
        String ref = ref(invoice);
        send(invoice.getUser().getEmail(),
             "Invoice AI Extraction Started — " + ref,
             html(invoice.getUser().getFullName(),
                  "🔍 AI Extraction In Progress",
                  "Your invoice <strong>" + ref + "</strong> is currently being extracted by AWS Textract.",
                  "#722ed1",
                  row("File", invoice.getOriginalFileName()) +
                  row("Started At", now()) +
                  row("Status", "Extracting text & data from document")));
    }

    @Async
    public void sendProcessingCompleted(Invoice invoice) {
        if (!notificationsEnabled || invoice.getUser() == null) return;
        String ref = ref(invoice);
        send(invoice.getUser().getEmail(),
             "Invoice Processed Successfully — " + ref,
             html(invoice.getUser().getFullName(),
                  "✅ AI Processing Complete",
                  "Your invoice <strong>" + ref + "</strong> has been successfully processed by AI.",
                  "#52c41a",
                  row("AI Confidence", invoice.getAiConfidenceScore() != null
                          ? invoice.getAiConfidenceScore() + "%" : "N/A") +
                  row("Amount", invoice.getAmount() != null ? "$" + invoice.getAmount() : "—") +
                  row("Vendor", invoice.getVendorName() != null ? invoice.getVendorName() : "—") +
                  row("Completed At", now()) +
                  row("Status", "COMPLETED — Awaiting admin review")));
    }

    @Async
    public void sendManualReviewRequired(Invoice invoice) {
        if (!notificationsEnabled || invoice.getUser() == null) return;
        String ref = ref(invoice);
        // Notify uploader
        send(invoice.getUser().getEmail(),
             "Invoice Requires Manual Review — " + ref,
             html(invoice.getUser().getFullName(),
                  "⚠️ Manual Review Required",
                  "Your invoice <strong>" + ref + "</strong> has been processed but requires manual review due to low AI confidence score.",
                  "#faad14",
                  row("AI Confidence", invoice.getAiConfidenceScore() != null
                          ? invoice.getAiConfidenceScore() + "%" : "N/A") +
                  row("Flagged At", now()) +
                  row("Reason", "Confidence below threshold — admin will review shortly")));

        // Alert admins to take action
        notifyAdmins(
             "⚠️ Manual Review Required — " + ref,
             "Invoice <strong>" + ref + "</strong> uploaded by <strong>" + invoice.getUser().getEmail() +
             "</strong> requires manual review due to low AI confidence score.",
             "#faad14",
             row("Invoice Ref", ref) +
             row("File", invoice.getOriginalFileName()) +
             row("Uploaded By", invoice.getUser().getEmail()) +
             row("AI Confidence", invoice.getAiConfidenceScore() != null
                     ? invoice.getAiConfidenceScore() + "%" : "N/A") +
             row("Flagged At", now()));
    }

    @Async
    public void sendDuplicateInvoiceDetected(Invoice invoice) {
        if (!notificationsEnabled || invoice.getUser() == null) return;
        String ref = ref(invoice);
        // Notify uploader
        send(invoice.getUser().getEmail(),
             "Duplicate Invoice Detected — " + ref,
             html(invoice.getUser().getFullName(),
                  "⚠️ Duplicate Invoice Detected",
                  "Your invoice <strong>" + ref + "</strong> appears to be a duplicate of an existing invoice in the system.",
                  "#ff7a00",
                  row("Invoice Ref", ref) +
                  row("File", invoice.getOriginalFileName()) +
                  row("Status", "DUPLICATE") +
                  row("Detected At", now()) +
                  row("Action", "Please contact support if you believe this is an error.")));

        // Alert admins
        notifyAdmins(
             "⚠️ Duplicate Invoice Detected — " + ref,
             "A duplicate invoice <strong>" + ref + "</strong> was detected during AI extraction for user <strong>" +
             invoice.getUser().getEmail() + "</strong>.",
             "#ff7a00",
             row("Invoice Ref", ref) +
             row("File", invoice.getOriginalFileName()) +
             row("Uploaded By", invoice.getUser().getEmail()) +
             row("Detected At", now()));
    }

    @Async
    public void sendStatusChanged(Invoice invoice, InvoiceStatus newStatus) {
        if (!notificationsEnabled || invoice.getUser() == null) return;
        String ref = ref(invoice);
        String colour;
        String icon;
        String message;
        switch (newStatus) {
            case APPROVED -> { colour = "#52c41a"; icon = "✅ Invoice Approved";
                message = "Your invoice <strong>" + ref + "</strong> has been <strong>approved</strong>."; }
            case REJECTED -> { colour = "#ff4d4f"; icon = "❌ Invoice Rejected";
                message = "Your invoice <strong>" + ref + "</strong> has been <strong>rejected</strong>. "
                        + "Please check the comments below for details."; }
            case PAID     -> { colour = "#1677ff"; icon = "💰 Invoice Marked as Paid";
                message = "Your invoice <strong>" + ref + "</strong> has been marked as <strong>paid</strong>."; }
            default       -> { colour = "#8c8c8c"; icon = "ℹ️ Invoice Status Updated";
                message = "Your invoice <strong>" + ref + "</strong> status has been updated to <strong>"
                        + newStatus + "</strong>."; }
        }
        String comments = (invoice.getComments() != null && !invoice.getComments().isBlank())
                ? row("Admin Comments", invoice.getComments()) : "";
        String reviewer = (invoice.getReviewedBy() != null)
                ? row("Reviewed By", invoice.getReviewedBy().getEmail()) : "";

        send(invoice.getUser().getEmail(), icon + " — " + ref,
             html(invoice.getUser().getFullName(), icon, message, colour,
                  row("Invoice", ref) +
                  row("New Status", newStatus.name()) +
                  row("Updated At", now()) +
                  comments + reviewer));
    }

    @Async
    public void sendProcessingFailed(Invoice invoice) {
        if (!notificationsEnabled || invoice.getUser() == null) return;
        String ref = ref(invoice);
        // Notify uploader
        send(invoice.getUser().getEmail(),
             "Invoice Processing Failed — " + ref,
             html(invoice.getUser().getFullName(),
                  "❌ Processing Failed",
                  "Unfortunately, AI processing for your invoice <strong>" + ref + "</strong> has failed. "
                  + "You can retry processing from the invoice detail page.",
                  "#ff4d4f",
                  row("File", invoice.getOriginalFileName()) +
                  row("Failed At", now()) +
                  row("Error", invoice.getProcessingError() != null ? invoice.getProcessingError() : "Unknown error")));

        // Alert admins
        notifyAdmins(
             "❌ Invoice Processing Failed — " + ref,
             "AI processing failed for invoice <strong>" + ref + "</strong> uploaded by <strong>" +
             invoice.getUser().getEmail() + "</strong>.",
             "#ff4d4f",
             row("Invoice Ref", ref) +
             row("File", invoice.getOriginalFileName()) +
             row("Uploaded By", invoice.getUser().getEmail()) +
             row("Failed At", now()) +
             row("Error", invoice.getProcessingError() != null ? invoice.getProcessingError() : "Unknown error"));
    }

    // ── User account ──────────────────────────────────────────────────────────

    @Async
    public void sendWelcome(String toEmail, String fullName) {
        if (!notificationsEnabled) return;
        send(toEmail,
             "Welcome to Invoice Processing System",
             html(fullName,
                  "👋 Welcome Aboard!",
                  "Your account has been successfully created. You can now upload and track your invoices.",
                  "#1677ff",
                  row("Email", toEmail) +
                  row("Role", "User") +
                  row("Registered At", now()) +
                  row("Next Step", "Log in and upload your first invoice")));
    }

    @Async
    public void sendLoginAlert(String toEmail, String fullName) {
        if (!notificationsEnabled) return;
        send(toEmail,
             "New Login to Your Account",
             html(fullName,
                  "🔐 New Login Detected",
                  "A new login was detected on your Invoice Processing System account.",
                  "#1677ff",
                  row("Email", toEmail) +
                  row("Login At", now()) +
                  row("Note", "If this wasn't you, please change your password immediately and contact support.")));
    }

    @Async
    public void sendProfileUpdated(String toEmail, String fullName) {
        if (!notificationsEnabled) return;
        send(toEmail,
             "Your Profile Has Been Updated",
             html(fullName,
                  "✏️ Profile Updated",
                  "Your profile information has been successfully updated.",
                  "#1677ff",
                  row("Email", toEmail) +
                  row("Updated At", now()) +
                  row("Note", "If you did not make this change, please contact support immediately.")));
    }

    @Async
    public void sendPasswordChanged(String toEmail, String fullName) {
        if (!notificationsEnabled) return;
        send(toEmail,
             "Your Password Has Been Changed",
             html(fullName,
                  "🔒 Password Changed",
                  "Your account password has been successfully changed.",
                  "#faad14",
                  row("Changed At", now()) +
                  row("Note", "If you did not make this change, please contact support immediately.")));
    }

    @Async
    public void sendRoleChanged(String toEmail, String fullName, String newRole) {
        if (!notificationsEnabled) return;
        send(toEmail,
             "Your Account Role Has Been Updated",
             html(fullName,
                  "🔑 Role Updated",
                  "An administrator has updated your account role.",
                  "#722ed1",
                  row("New Role", newRole) +
                  row("Updated At", now()) +
                  row("Note", "Your access permissions have been updated accordingly.")));
    }

    @Async
    public void sendAccountDeleted(String toEmail, String fullName) {
        if (!notificationsEnabled) return;
        send(toEmail,
             "Your Account Has Been Deleted",
             html(fullName,
                  "🗑️ Account Deleted",
                  "Your account on the Invoice Processing System has been permanently deleted by an administrator.",
                  "#ff4d4f",
                  row("Email", toEmail) +
                  row("Deleted At", now()) +
                  row("Note", "If you believe this was a mistake, please contact support.")));
    }

    // ── Admin broadcast helper ────────────────────────────────────────────────

    private void notifyAdmins(String subject, String message, String accentColor, String tableRows) {
        List<String> adminEmails = userRepository.findAllByRole(UserRole.ADMIN)
                .stream().map(User::getEmail).toList();
        if (adminEmails.isEmpty()) {
            log.debug("No admins found to notify for: {}", subject);
            return;
        }
        adminEmails.forEach(adminEmail ->
            send(adminEmail, "[ADMIN ALERT] " + subject,
                 html("Admin", subject, message, accentColor, tableRows))
        );
    }

    // ── Core send via AWS SES ─────────────────────────────────────────────────

    private void send(String to, String subject, String htmlBody) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromEmail)
                    .replyToAddresses(replyToEmail)
                    .destination(Destination.builder().toAddresses(to).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).charset("UTF-8").build())
                            .body(Body.builder()
                                    .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                    .build())
                            .build())
                    .build();
            sesClient.sendEmail(request);
            log.info("SES email sent → {} : {}", to, subject);
        } catch (SesException ex) {
            log.warn("SES send failed to {}: {} — {}", to, subject, ex.awsErrorDetails().errorMessage());
        } catch (Exception ex) {
            log.warn("Email send failed to {}: {}", to, ex.getMessage());
        }
    }

    // ── HTML template helpers ─────────────────────────────────────────────────

    private String ref(Invoice invoice) {
        return invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "#" + invoice.getInvoiceId();
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss"));
    }

    private String row(String label, String value) {
        return "<tr>" +
               "<td style='padding:6px 12px;color:#666;font-size:13px;width:140px'>" + label + "</td>" +
               "<td style='padding:6px 12px;font-size:13px;font-weight:500'>" + (value != null ? value : "—") + "</td>" +
               "</tr>";
    }

    private String html(String name, String title, String message, String accentColor, String tableRows) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#f5f5f5;font-family:Arial,sans-serif'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding:40px 20px'>" +
               "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:8px;" +
               "box-shadow:0 2px 8px rgba(0,0,0,.08);overflow:hidden'>" +

               // Header
               "<tr><td style='background:" + accentColor + ";padding:28px 32px'>" +
               "<h2 style='margin:0;color:#fff;font-size:20px'>" + title + "</h2></td></tr>" +

               // Body
               "<tr><td style='padding:28px 32px'>" +
               "<p style='margin:0 0 20px;font-size:15px;color:#333'>Dear <strong>" + name + "</strong>,</p>" +
               "<p style='margin:0 0 24px;font-size:15px;color:#444;line-height:1.6'>" + message + "</p>" +

               // Details table
               (tableRows.isBlank() ? "" :
               "<table width='100%' style='background:#fafafa;border:1px solid #e8e8e8;border-radius:6px;" +
               "border-collapse:collapse;margin-bottom:24px'><tbody>" + tableRows + "</tbody></table>") +

               "<p style='margin:24px 0 0;font-size:13px;color:#888'>Log in to the " +
               "<a href='#' style='color:" + accentColor + "'>Invoice Processing System</a> to view full details.</p>" +
               "</td></tr>" +

               // Footer
               "<tr><td style='background:#fafafa;border-top:1px solid #f0f0f0;padding:16px 32px;text-align:center'>" +
               "<p style='margin:0;font-size:12px;color:#aaa'>© 2025 Invoice Processing System · " +
               "<a href='mailto:" + replyToEmail + "' style='color:#aaa'>" + replyToEmail + "</a></p>" +
               "</td></tr>" +

               "</table></td></tr></table></body></html>";
    }
}
