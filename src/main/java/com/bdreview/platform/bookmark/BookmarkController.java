package com.bdreview.platform.bookmark;

import com.bdreview.platform.common.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @PostMapping("/bookmarks")
    public ResponseEntity<Void> add(@Valid @RequestBody AddBookmarkRequest request) {
        bookmarkService.addBookmark(CurrentUser.id(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bookmarks/{businessId}")
    public ResponseEntity<Void> remove(@PathVariable UUID businessId) {
        bookmarkService.removeBookmark(CurrentUser.id(), businessId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bookmarks")
    public ResponseEntity<List<Bookmark>> mine() {
        return ResponseEntity.ok(bookmarkService.myBookmarks(CurrentUser.id()));
    }

    @PostMapping("/collections")
    public ResponseEntity<Collection> createCollection(@RequestBody CreateCollectionRequest request) {
        return ResponseEntity.ok(bookmarkService.createCollection(CurrentUser.id(), request.name()));
    }

    @PutMapping("/collections/{id}")
    public ResponseEntity<Void> renameCollection(@PathVariable UUID id, @RequestBody CreateCollectionRequest request) {
        bookmarkService.renameCollection(CurrentUser.id(), id, request.name());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/collections/{id}")
    public ResponseEntity<Void> deleteCollection(@PathVariable UUID id) {
        bookmarkService.deleteCollection(CurrentUser.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/collections")
    public ResponseEntity<List<Collection>> myCollections() {
        return ResponseEntity.ok(bookmarkService.myCollections(CurrentUser.id()));
    }

    @GetMapping("/collections/{id}/bookmarks")
    public ResponseEntity<List<Bookmark>> collectionBookmarks(@PathVariable UUID id) {
        return ResponseEntity.ok(bookmarkService.bookmarksInCollection(id));
    }
}
