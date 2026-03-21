package com.masukibooks.controller;

import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.dto.response.LibraryResponse;
import com.masukibooks.dto.response.RecentReadResponse;
import com.masukibooks.entity.User;
import com.masukibooks.service.UserLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/library")
@RequiredArgsConstructor
public class LibraryController {

    private final UserLibraryService userLibraryService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<LibraryResponse>>> getUserLibrary(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String accessType,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<LibraryResponse> library = userLibraryService.getUserLibrary(
                user.getUserId(), status, accessType, pageable);
        return ResponseEntity.ok(ApiResponse.success("Library retrieved", library));
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<ApiResponse<LibraryResponse>> getLibraryRecord(
            @PathVariable UUID bookId,
            @AuthenticationPrincipal User user) {
        LibraryResponse record = userLibraryService.getLibraryRecord(user.getUserId(), bookId);
        return ResponseEntity.ok(ApiResponse.success("Library record retrieved", record));
    }

    @PostMapping("/{bookId}")
    public ResponseEntity<ApiResponse<LibraryResponse>> addToLibrary(
            @PathVariable UUID bookId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal User user) {
        String accessType = (body != null && body.containsKey("accessType"))
                ? body.get("accessType")
                : "sample";
        LibraryResponse record = userLibraryService.addToLibrary(
                user.getUserId(), bookId, accessType, null);
        return ResponseEntity.ok(ApiResponse.success("Book added to library", record));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<Page<RecentReadResponse>>> getRecentlyRead(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<RecentReadResponse> recent = userLibraryService.getRecentlyRead(user.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Recently read books", recent));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<ApiResponse<Void>> removeFromLibrary(
            @PathVariable UUID bookId,
            @AuthenticationPrincipal User user) {
        userLibraryService.removeFromLibrary(user.getUserId(), bookId);
        return ResponseEntity.ok(ApiResponse.success("Book removed from library", null));
    }
}
