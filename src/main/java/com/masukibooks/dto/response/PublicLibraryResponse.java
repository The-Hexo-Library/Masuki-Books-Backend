package com.masukibooks.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class PublicLibraryResponse {
    private UUID publicLibraryId;
    private UUID productId;
    private String title;
    private String author;
    private String fileUrl;
    private String visibility;
    private Boolean isFeatured;
    private String notes;
    private Boolean editable;
}
