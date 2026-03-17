package com.masukibooks.controller;

import com.masukibooks.dto.request.BookmarkRequest;
import com.masukibooks.dto.request.SaveProgressRequest;
import com.masukibooks.dto.response.*;
import com.masukibooks.entity.User;
import com.masukibooks.service.DigitalContentService;
import com.masukibooks.service.ReadingProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reader")
@RequiredArgsConstructor
public class ReaderController {

    private final DigitalContentService digitalContentService;
    private final ReadingProgressService readingProgressService;

    @GetMapping("/{bookId}/metadata")
    public ResponseEntity<ApiResponse<BookReaderMetadata>> getBookMetadata(
            @PathVariable UUID bookId,
            @AuthenticationPrincipal User user) {
        UUID userId = user != null ? user.getUserId() : null;
        BookReaderMetadata metadata = digitalContentService.getBookMetadata(bookId, userId);
        return ResponseEntity.ok(ApiResponse.success("Book metadata retrieved", metadata));
    }

    @GetMapping(value = "/{bookId}/page/{pageNumber}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getPage(
            @PathVariable UUID bookId,
            @PathVariable int pageNumber,
            @AuthenticationPrincipal User user) {
        UUID userId = user != null ? user.getUserId() : null;
        byte[] content = digitalContentService.getPageContent(bookId, pageNumber, userId);
        return ResponseEntity.ok()
                .header("X-Page-Number", String.valueOf(pageNumber))
                .body(content);
    }

    @GetMapping("/{bookId}/pages")
    public ResponseEntity<ApiResponse<List<PageContentResponse>>> getPageRange(
            @PathVariable UUID bookId,
            @RequestParam int start,
            @RequestParam int end,
            @AuthenticationPrincipal User user) {
        UUID userId = user != null ? user.getUserId() : null;
        List<PageContentResponse> pages = digitalContentService.getPageRange(bookId, start, end, userId);
        return ResponseEntity.ok(ApiResponse.success("Pages retrieved", pages));
    }

    @PostMapping("/{bookId}/progress")
    public ResponseEntity<ApiResponse<ReadingProgressResponse>> saveProgress(
            @PathVariable UUID bookId,
            @Valid @RequestBody SaveProgressRequest request,
            @AuthenticationPrincipal User user) {
        long readingTime = request.getReadingTimeSeconds() != null ? request.getReadingTimeSeconds() : 0L;
        ReadingProgressResponse progress = readingProgressService.saveProgress(
                user.getUserId(), bookId, request.getCurrentPage(), readingTime);
        return ResponseEntity.ok(ApiResponse.success("Progress saved", progress));
    }

    @GetMapping("/{bookId}/progress")
    public ResponseEntity<ApiResponse<ReadingProgressResponse>> getProgress(
            @PathVariable UUID bookId,
            @AuthenticationPrincipal User user) {
        ReadingProgressResponse progress = readingProgressService.getProgress(user.getUserId(), bookId);
        return ResponseEntity.ok(ApiResponse.success("Progress retrieved", progress));
    }

    @GetMapping("/{bookId}/bookmarks")
    public ResponseEntity<ApiResponse<List<BookmarkResponse>>> getBookmarks(
            @PathVariable UUID bookId,
            @AuthenticationPrincipal User user) {
        List<BookmarkResponse> bookmarks = readingProgressService.getBookmarks(user.getUserId(), bookId);
        return ResponseEntity.ok(ApiResponse.success("Bookmarks retrieved", bookmarks));
    }

    @PostMapping("/{bookId}/bookmarks")
    public ResponseEntity<ApiResponse<BookmarkResponse>> createBookmark(
            @PathVariable UUID bookId,
            @Valid @RequestBody BookmarkRequest request,
            @AuthenticationPrincipal User user) {
        BookmarkResponse bookmark = readingProgressService.createBookmark(user.getUserId(), bookId, request);
        return ResponseEntity.ok(ApiResponse.success("Bookmark created", bookmark));
    }

    @PutMapping("/{bookId}/bookmarks/{bookmarkId}")
    public ResponseEntity<ApiResponse<BookmarkResponse>> updateBookmark(
            @PathVariable UUID bookId,
            @PathVariable UUID bookmarkId,
            @Valid @RequestBody BookmarkRequest request,
            @AuthenticationPrincipal User user) {
        BookmarkResponse bookmark = readingProgressService.updateBookmark(user.getUserId(), bookmarkId, request);
        return ResponseEntity.ok(ApiResponse.success("Bookmark updated", bookmark));
    }

    @DeleteMapping("/{bookId}/bookmarks/{bookmarkId}")
    public ResponseEntity<ApiResponse<Void>> deleteBookmark(
            @PathVariable UUID bookId,
            @PathVariable UUID bookmarkId,
            @AuthenticationPrincipal User user) {
        readingProgressService.deleteBookmark(user.getUserId(), bookmarkId);
        return ResponseEntity.ok(ApiResponse.success("Bookmark deleted", null));
    }
}
