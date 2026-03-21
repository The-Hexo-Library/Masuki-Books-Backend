package com.masukibooks.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookmarkRequest {

    @NotNull
    @Min(1)
    private Integer pageNumber;

    private String title;

    private String note;

    private String color;
}
