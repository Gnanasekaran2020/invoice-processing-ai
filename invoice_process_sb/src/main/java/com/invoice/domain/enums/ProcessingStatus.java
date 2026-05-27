package com.invoice.domain.enums;

public enum ProcessingStatus {
    UPLOADED,       // File received, queued
    EXTRACTING,     // OCR/PDF text extraction in progress
    AI_PROCESSING,  // AI model extracting structured data
    COMPLETED,      // AI extraction successful
    FAILED,         // Processing failed — error stored
    MANUAL_REVIEW   // Low confidence — flagged for human review
}
