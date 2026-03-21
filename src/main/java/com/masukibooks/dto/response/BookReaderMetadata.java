package com.masukibooks.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookReaderMetadata {

    private UUID productId;
    private String title;
    private String author;
    private String fileFormat;
    private Integer totalPages;
    private Integer previewPages;
    private String coverImageUrl;

    private ProgressInfo currentProgress;
    private List<BookmarkInfo> bookmarks;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProgressInfo {
        private Integer currentPage;
        private BigDecimal percentage;
        private LocalDateTime lastReadAt;
        private Long readingTimeSeconds;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BookmarkInfo {
        private UUID bookmarkId;
        private Integer pageNumber;
        private String title;
        private String note;
        private String color;
    }
}
