package com.invoice.service;

import com.invoice.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final S3Client   s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.aws.s3.bucket}")
    private String bucket;

    @Value("${app.aws.s3.presigned-url-expiry-minutes:60}")
    private int presignedUrlExpiryMinutes;

    public String uploadFile(MultipartFile file, String folder) {
        String extension = getExtension(file.getOriginalFilename());
        String objectKey  = folder + "/" + UUID.randomUUID() + "." + extension;
        try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            log.info("File uploaded to S3: bucket={}, key={}", bucket, objectKey);
            return objectKey;
        } catch (Exception ex) {
            throw new FileStorageException("Failed to upload file to S3: " + ex.getMessage(), ex);
        }
    }

    public byte[] downloadFile(String objectKey) {
        try {
            return s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(objectKey).build()
            ).asByteArray();
        } catch (Exception ex) {
            throw new FileStorageException("Failed to download file from S3: " + ex.getMessage(), ex);
        }
    }

    public String generatePresignedUrl(String objectKey) {
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(presignedUrlExpiryMinutes))
                    .getObjectRequest(r -> r.bucket(bucket).key(objectKey))
                    .build();
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception ex) {
            throw new FileStorageException("Failed to generate presigned URL: " + ex.getMessage(), ex);
        }
    }

    public void deleteFile(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
            log.info("File deleted from S3: key={}", objectKey);
        } catch (Exception ex) {
            log.warn("Failed to delete file from S3: {}", ex.getMessage());
        }
    }

    public String getBucket() {
        return bucket;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
