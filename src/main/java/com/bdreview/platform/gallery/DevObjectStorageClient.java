package com.bdreview.platform.gallery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Local-disk object storage for the dev/test profile (spec §13). Bytes are
 * written to and served from {@code app.storage.local-dir} via
 * {@link StorageController} — a real deployment should implement
 * {@link ObjectStorageClient} against S3/R2 instead.
 */
@Component
@Profile("!prod")
public class DevObjectStorageClient implements ObjectStorageClient {

    private final String baseUrl;

    public DevObjectStorageClient(@Value("${app.storage.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String buildObjectKey(String folder, String ownerId, String filename) {
        // Folder convention from spec §13: business/{business_id}/..., review/{review_id}/..., nid/{user_id}/...
        return folder + "/" + ownerId + "/" + UUID.randomUUID() + "-" + filename;
    }

    @Override
    public String presignPutUrl(String objectKey) {
        return baseUrl + "/api/v1/storage/upload/" + objectKey;
    }

    @Override
    public String cdnUrlFor(String objectKey) {
        return baseUrl + "/api/v1/storage/files/" + objectKey;
    }
}
