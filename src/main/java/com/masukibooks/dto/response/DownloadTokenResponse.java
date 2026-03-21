package com.masukibooks.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DownloadTokenResponse {

    private String token;
    private LocalDateTime expiresAt;
    private Long downloadsUsed;
    private Integer maxDownloads;
}
