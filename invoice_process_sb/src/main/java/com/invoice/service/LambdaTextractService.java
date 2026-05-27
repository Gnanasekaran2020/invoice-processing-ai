package com.invoice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoice.exception.AiProcessingException;
import com.invoice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.Map;

/**
 * Triggers the AWS Lambda function that runs Textract AnalyzeExpense on the
 * uploaded invoice.  Invocation type EVENT means fire-and-forget (HTTP 202);
 * the Lambda posts the parsed result back via the extraction-callback endpoint.
 *
 * Expected Lambda input payload:
 * {
 *   "invoiceId":      123,
 *   "s3Bucket":       "invoice-documents",
 *   "s3Key":          "invoices/user@example.com/uuid.pdf",
 *   "callbackUrl":    "https://app.example.com/api/invoices/123/extraction-callback",
 *   "callbackToken":  "Bearer <signed-jwt>"   ← real JWT, sent back as Authorization header
 * }
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LambdaTextractService {

    private final LambdaClient       lambdaClient;
    private final ObjectMapper       objectMapper;
    private final JwtTokenProvider   jwtTokenProvider;

    @Value("${app.aws.lambda.function-name}")
    private String functionName;

    @Value("${app.extraction.callback-base-url}")
    private String callbackBaseUrl;

    public void invokeLambda(Integer invoiceId, String s3Bucket, String s3Key) {
        try {
            // Generate a real signed JWT for the lambda-service identity.
            // The Lambda will send this back as:  Authorization: Bearer <token>
            String jwt = jwtTokenProvider.generateToken("lambda-service@internal");

            Map<String, Object> payload = Map.of(
                    "invoiceId",     invoiceId,
                    "s3Bucket",      s3Bucket,
                    "s3Key",         s3Key,
                    "callbackUrl",   callbackBaseUrl + "/api/invoices/" + invoiceId + "/extraction-callback",
                    "callbackToken", "Bearer " + jwt
            );

            InvokeRequest request = InvokeRequest.builder()
                    .functionName(functionName)
                    .invocationType(InvocationType.EVENT)   // async, no wait for result
                    .payload(SdkBytes.fromUtf8String(objectMapper.writeValueAsString(payload)))
                    .build();

            InvokeResponse response = lambdaClient.invoke(request);
            // 202 = event accepted; Lambda runs independently
            log.info("Lambda invoked for invoice {}: statusCode={}", invoiceId, response.statusCode());

        } catch (Exception ex) {
            throw new AiProcessingException(
                    "Failed to invoke Textract Lambda for invoice " + invoiceId + ": " + ex.getMessage(), ex);
        }
    }
}
