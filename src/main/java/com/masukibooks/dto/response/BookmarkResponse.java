package com.masukibooks.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookmarkResponse {

    private UUID bookmarkId;
    private UUID productId;
    private Integer pageNumber;
    private String title;
    private String note;
    private String color;
    private LocalDateTime createdAt;
}
