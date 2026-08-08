package com.bdreview.platform.bookmark;

import com.bdreview.platform.common.ForbiddenException;
import com.bdreview.platform.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Spec §9: Favorites extended with named Collections; sharing/following other users' collections is out of MVP scope. */
@Service
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final CollectionRepository collectionRepository;

    public BookmarkService(BookmarkRepository bookmarkRepository, CollectionRepository collectionRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.collectionRepository = collectionRepository;
    }

    @Transactional
    public Collection createCollection(UUID userId, String name) {
        return collectionRepository.save(Collection.builder().userId(userId).name(name).build());
    }

    @Transactional
    public void renameCollection(UUID userId, UUID collectionId, String name) {
        int updated = collectionRepository.rename(collectionId, userId, name);
        if (updated == 0) {
            throw new ResourceNotFoundException("Collection not found or not owned by you");
        }
    }

    @Transactional
    public void deleteCollection(UUID userId, UUID collectionId) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));
        if (!collection.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not own this collection");
        }
        bookmarkRepository.unassignCollection(collectionId); // fall back to default bucket, not orphaned
        collectionRepository.delete(collection);
    }

    public List<Collection> myCollections(UUID userId) {
        return collectionRepository.findByUserId(userId);
    }

    @Transactional
    public void addBookmark(UUID userId, AddBookmarkRequest request) {
        if (bookmarkRepository.findByUserIdAndBusinessId(userId, request.businessId()).isPresent()) {
            return; // idempotent
        }
        bookmarkRepository.save(Bookmark.builder()
                .userId(userId).businessId(request.businessId()).collectionId(request.collectionId()).build());
    }

    @Transactional
    public void removeBookmark(UUID userId, UUID businessId) {
        bookmarkRepository.deleteByUserIdAndBusinessId(userId, businessId);
    }

    public List<Bookmark> myBookmarks(UUID userId) {
        return bookmarkRepository.findByUserId(userId);
    }

    public List<Bookmark> bookmarksInCollection(UUID collectionId) {
        return bookmarkRepository.findByCollectionId(collectionId);
    }
}
