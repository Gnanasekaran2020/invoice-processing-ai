package com.invoice.ai;

/**
 * Replaced by {@link com.invoice.service.LambdaTextractService}.
 *
 * Invoice text extraction now runs inside an AWS Lambda function via
 * the AWS Textract AnalyzeExpense API.  This class is retained as an
 * empty placeholder so existing git history remains navigable; it can
 * be deleted once all branches referencing it are merged.
 *
 * @deprecated Use LambdaTextractService + Textract callback flow instead.
 */
@Deprecated(since = "2.0", forRemoval = true)
public class InvoiceAiService {
    private InvoiceAiService() { }
}
