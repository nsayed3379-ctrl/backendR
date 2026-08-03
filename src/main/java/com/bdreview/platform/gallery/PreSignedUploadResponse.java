package com.bdreview.platform.gallery;

/**
 * Spec §13: backend issues a pre-signed S3/R2 upload URL; frontend uploads
 * directly to object storage; backend only ever stores the resulting
 * metadata/URL (see ConfirmUploadRequest). Actual S3/R2 SDK wiring is
 * infra-specific and left as an integration point (objectStorageClient below).
 */
public record PreSignedUploadResponse(String uploadUrl, String objectKey, String cdnUrlAfterUpload) {
}
