package com.bdreview.platform.gallery;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("!prod")
public class DevObjectStorageClient implements ObjectStorageClient {

    @Override
    public String buildObjectKey(String folder, String ownerId, String filename) {
        // Folder convention from spec §13: business/{business_id}/..., review/{review_id}/..., nid/{user_id}/...
        return folder + "/" + ownerId + "/" + UUID.randomUUID() + "-" + filename;
    }

    @Override
    public String presignPutUrl(String objectKey) {
        return "https://dev-bucket.local/presigned-put/" + objectKey;
    }

    @Override
    public String cdnUrlFor(String objectKey) {
        return "https://cdn.dev.local/" + objectKey;
    }
}
