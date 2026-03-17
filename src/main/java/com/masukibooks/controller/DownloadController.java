package com.masukibooks.controller;

import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.dto.response.DownloadTokenResponse;
import com.masukibooks.entity.User;
import com.masukibooks.service.DownloadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/downloads")
@RequiredArgsConstructor
public class DownloadController {

    private final DownloadService downloadService;

    @PostMapping("/{bookId}/token")
    public ResponseEntity<ApiResponse<DownloadTokenResponse>> generateToken(
            @PathVariable UUID bookId,
            @AuthenticationPrincipal User user) {
        DownloadTokenResponse token = downloadService.generateDownloadToken(user.getUserId(), bookId);
        return ResponseEntity.ok(ApiResponse.success("Download token generated", token));
    }

    @GetMapping("/file/{token}")
    public ResponseEntity<StreamingResponseBody> downloadFile(
            @PathVariable String token,
            HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        DownloadService.DownloadContext ctx = downloadService.processDownload(token, ipAddress);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(ctx.contentType()));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + ctx.fileName() + "\"");
        if (ctx.fileSize() != null) {
            headers.setContentLength(ctx.fileSize());
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(ctx.body());
    }
}
