package com.masukibooks.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PageContentResponse {

    private Integer pageNumber;
    private String content;  // base64 for PDF pages, HTML string for EPUB chapters
    private String contentType;  // application/pdf, text/html
}
